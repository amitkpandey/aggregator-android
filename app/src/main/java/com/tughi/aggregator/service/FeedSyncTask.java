package com.tughi.aggregator.service;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;

import com.tughi.aggregator.BuildConfig;
import com.tughi.aggregator.content.DatabaseContentProvider;
import com.tughi.aggregator.content.EntryColumns;
import com.tughi.aggregator.content.FeedColumns;
import com.tughi.aggregator.content.FeedSyncStatsColumns;
import com.tughi.aggregator.content.FeedUpdateModes;
import com.tughi.aggregator.content.SyncLogColumns;
import com.tughi.aggregator.content.Uris;
import com.tughi.aggregator.feeds.FeedParser;
import com.tughi.aggregator.feeds.FeedParserException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

/**
 * Updates a feed on a background thread.
 */
/* local */ class FeedSyncTask extends AsyncTask<Object, Void, Void> {

    private static final String TAG = FeedSyncTask.class.getName();

    private Context context;
    private PowerManager.WakeLock wakeLock;

    public FeedSyncTask(Context context) {
        this.context = context;

        // acquire a partial lock
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();
    }

    @Override
    protected Void doInBackground(Object... objects) {
        final long feedId = (Long) objects[0];
        final String feedUrl = (String) objects[1];
        final int feedUpdateMode = (Integer) objects[2];
        final long poll = (Long) objects[3];

        final Uri feedEntriesUri = Uris.newFeedEntriesUri(feedId);

        final ContentResolver contentResolver = context.getContentResolver();

        Log.i(getClass().getName(), "Updating feed " + feedId + " on " + Thread.currentThread().getName());

        ContentValues syncLogValues = new ContentValues();
        syncLogValues.put(SyncLogColumns.FEED_ID, feedId);
        syncLogValues.put(SyncLogColumns.POLL, poll);

        try {
            // parse feed
            FeedParser.Result result;
            try {
                result = FeedParser.parse(feedUrl);
            } catch (FeedParserException exception) {
                throw new IOException(exception);
            }

            if (result.status != HttpURLConnection.HTTP_OK) {
                throw new IOException("unexpected HTTP response: " + result.status);
            }

            syncLogValues.put(SyncLogColumns.ENTRIES_TOTAL, result.feed.entries.size());

            // prepare content batch
            ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>(result.feed.entries.size() + 1);

            // update feed values
            ContentValues feedSyncValues = new ContentValues();
            feedSyncValues.put(FeedColumns.URL, result.url);
            feedSyncValues.put(FeedColumns.TITLE, result.feed.title);
            feedSyncValues.put(FeedColumns.LINK, result.feed.link);
            batch.add(
                    ContentProviderOperation
                            .newUpdate(Uris.newSyncFeedUri(feedId))
                            .withValues(feedSyncValues)
                            .build()
            );

            for (FeedParser.Result.Feed.Entry entry : result.feed.entries) {
                // insert/update feed entry
                ContentValues entryValues = new ContentValues();
                entryValues.put(EntryColumns.FEED_ID, feedId);
                entryValues.put(EntryColumns.GUID, entry.id);
                entryValues.put(EntryColumns.TITLE, entry.title);
                entryValues.put(EntryColumns.UPDATED, entry.updatedTimestamp);
                entryValues.put(EntryColumns.POLL, poll);
                entryValues.put(EntryColumns.DATA, entry.title);
                batch.add(
                        ContentProviderOperation
                                .newInsert(feedEntriesUri)
                                .withValues(entryValues)
                                .build()
                );
            }

            // execute batch
            try {
                contentResolver.applyBatch(DatabaseContentProvider.AUTHORITY, batch);
            } catch (Exception exception) {
                throw new IOException("batch failed", exception);
            }
        } catch (IOException exception) {
            syncLogValues.put(SyncLogColumns.ERROR, exception.getMessage());

            if (BuildConfig.DEBUG) {
                Log.w(getClass().getName(), "feed update failed", exception);
            } else {
                Log.w(getClass().getName(), "feed update failed: " + exception.getMessage());
            }
        } finally {
            // update sync log
            contentResolver.insert(Uris.newFeedsSyncLogUri(), syncLogValues);

            // update next sync
            ContentValues feedUserValues = new ContentValues();
            feedUserValues.put(FeedColumns.NEXT_SYNC, findNextSync(feedId, feedUpdateMode, poll));
            contentResolver.update(Uris.newUserFeedUri(feedId), feedUserValues, null, null);

            // schedule next sync
            FeedSyncScheduler.scheduleAlarm(context, poll);
        }

        contentResolver.notifyChange(Uris.newFeedsUri(), null);
        contentResolver.notifyChange(Uris.newSyncFeedsUri(), null);

        return null;
    }

    private long findNextSync(long feedId, int feedUpdateMode, long poll) {
        switch (feedUpdateMode) {
            case FeedUpdateModes.AUTO:
            default:
                return findNextAutoSync(feedId, poll);
        }
    }

    private static final long TIME_15_MINUTES = 15 * 60 * 1000;

    private long findNextAutoSync(long feedId, long poll) {
        long nextPollDelta;

        Uri feedSyncLogStatsUri = Uris.newFeedSyncLogStatsUri(feedId);
        final String[] projection = {
                FeedSyncStatsColumns.POLL_COUNT,
                FeedSyncStatsColumns.LAST_POLL,
                FeedSyncStatsColumns.LAST_ENTRIES_TOTAL,
                FeedSyncStatsColumns.LAST_ENTRIES_NEW,
                FeedSyncStatsColumns.POLL_DELTA_AVERAGE,
                FeedSyncStatsColumns.ENTRIES_NEW_AVERAGE,
                FeedSyncStatsColumns.ENTRIES_NEW_MEDIAN
        };
        // final int pollCountIndex = 0;
        // final int lastPollIndex = 1;
        final int lastEntriesTotalIndex = 2;
        final int lastEntriesNewIndex = 3;
        final int pollDeltaAverageIndex = 4;
        final int entriesNewAverageIndex = 5;
        final int entriesNewMedianIndex = 6;
        Cursor cursor = context.getContentResolver().query(feedSyncLogStatsUri, projection, null, null, null);
        if (cursor.moveToFirst()) {
            int lastEntriesTotal = cursor.getInt(lastEntriesTotalIndex);
            int lastEntriesNew = cursor.getInt(lastEntriesNewIndex);
            if (lastEntriesNew > lastEntriesTotal / 2) {
                // try to not loose any new entries if the feed gets updated very fast
                nextPollDelta = TIME_15_MINUTES;
            } else {
                long pollDeltaAverage = cursor.getLong(pollDeltaAverageIndex);
                if (pollDeltaAverage % TIME_15_MINUTES != TIME_15_MINUTES) {
                    // rounding
                    pollDeltaAverage = (pollDeltaAverage / TIME_15_MINUTES) * TIME_15_MINUTES + TIME_15_MINUTES;
                }
                int entriesNewAverage = cursor.getInt(entriesNewAverageIndex);
                int entriesNewMedian = cursor.getInt(entriesNewMedianIndex);
                if (entriesNewMedian == 0) {
                    // polls were made too frequent
                    nextPollDelta = pollDeltaAverage + TIME_15_MINUTES;
                } else if (entriesNewMedian > entriesNewAverage) {
                    // polls were made not frequent enough
                    nextPollDelta = pollDeltaAverage - TIME_15_MINUTES;
                } else {
                    // use the current poll delta
                    nextPollDelta = pollDeltaAverage;
                }
            }
        } else {
            // fallback
            nextPollDelta = TIME_15_MINUTES;
        }
        cursor.close();

        // TODO: nextPollDelta can be 24 hours at the max
        // TODO: if nextPollDelta > 24h then align the next sync with the last updated entry

        long nextSync = poll + nextPollDelta;

        // align to a 15 minutes update rate
        if (nextSync % TIME_15_MINUTES != 0) {
            nextSync = (nextSync / TIME_15_MINUTES) * TIME_15_MINUTES + TIME_15_MINUTES;
        }
        // next sync cannot be in the past
        long earliestSync = (System.currentTimeMillis() / TIME_15_MINUTES) * TIME_15_MINUTES + TIME_15_MINUTES;
        if (earliestSync > nextSync) {
            nextSync = earliestSync;
        }

        return nextSync;
    }

    @Override
    protected void onPostExecute(Void result) {
        wakeLock.release();
    }

}
