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

        Log.i(getClass().getName(), "Updating feed " + feedId);

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
    private static final long TIME_30_MINUTES = TIME_15_MINUTES * 2;
    private static final long TIME_60_MINUTES = TIME_30_MINUTES * 2;
    private static final long TIME_03_HOURS = TIME_60_MINUTES * 3;
    private static final long TIME_06_HOURS = TIME_60_MINUTES * 6;
    private static final long TIME_12_HOURS = TIME_60_MINUTES * 12;
    private static final long TIME_24_HOURS = TIME_60_MINUTES * 24;
    private static final long TIME_02_DAYS = TIME_24_HOURS * 2;
    private static final long TIME_03_DAYS = TIME_24_HOURS * 3;
    private static final long TIME_04_DAYS = TIME_24_HOURS * 4;
    private static final long TIME_07_DAYS = TIME_24_HOURS * 7;

    private long findNextAutoSync(long feedId, long poll) {
        ContentResolver contentResolver = context.getContentResolver();

        Uri feedEntriesUri = Uris.newFeedEntriesUri(feedId);
        final String[] projection = {EntryColumns.UPDATED};
        final String selection = EntryColumns.UPDATED + " >= CAST(? AS INTEGER)";
        final String[] selectionArgs = new String[1];
        final String sortOrder = EntryColumns.UPDATED + " DESC";
        Cursor cursor;

        long lastUpdated = poll;

        // get aggregated number of entries in the last 24 hours
        selectionArgs[0] = Long.toString(poll - TIME_24_HOURS);
        cursor = contentResolver.query(feedEntriesUri, projection, selection, selectionArgs, sortOrder);
        int day_entries = cursor.getCount();
        if (cursor.moveToFirst()) {
            lastUpdated = Math.min(lastUpdated, cursor.getLong(0));
        }
        cursor.close();

        long poll_rate;
        if (day_entries > 0) {
            poll_rate = TIME_24_HOURS / day_entries;
        } else {
            // get aggregated number of entries in the last 7 * 24 hours
            selectionArgs[0] = Long.toString(poll - TIME_07_DAYS);
            cursor = contentResolver.query(feedEntriesUri, projection, selection, selectionArgs, sortOrder);
            int week_entries = cursor.getCount();
            if (cursor.moveToFirst()) {
                lastUpdated = Math.min(lastUpdated, cursor.getLong(0));
            }
            cursor.close();

            if (week_entries > 0) {
                poll_rate = TIME_07_DAYS / week_entries;
            } else {
                poll_rate = TIME_04_DAYS;
            }
        }

        long nextSync;
        if (poll_rate < TIME_30_MINUTES) {
            // schedule new poll in 15 minutes
            nextSync = lastUpdated + TIME_15_MINUTES;
        } else if (poll_rate < TIME_60_MINUTES) {
            // schedule new poll in 30 minutes
            nextSync = lastUpdated + TIME_30_MINUTES;
        } else if (poll_rate < TIME_03_HOURS) {
            // schedule new poll in 1 hour
            nextSync = lastUpdated + TIME_60_MINUTES;
        } else if (poll_rate < TIME_06_HOURS) {
            // schedule new poll in 3 hours
            nextSync = lastUpdated + TIME_03_HOURS;
        } else if (poll_rate < TIME_12_HOURS) {
            // schedule new poll in 6 hours
            nextSync = lastUpdated + TIME_06_HOURS;
        } else if (poll_rate < TIME_24_HOURS) {
            // schedule new poll in 12 hours
            nextSync = lastUpdated + TIME_12_HOURS;
        } else if (poll_rate < TIME_02_DAYS) {
            // schedule new poll in 1 day
            nextSync = lastUpdated + TIME_24_HOURS;
        } else if (poll_rate < TIME_03_DAYS) {
            // schedule new poll in 2 day
            nextSync = lastUpdated + TIME_02_DAYS;
        } else if (poll_rate < TIME_04_DAYS) {
            // schedule new poll in 3 day
            nextSync = lastUpdated + TIME_03_DAYS;
        } else {
            // schedule new poll in 4 days
            nextSync = lastUpdated + TIME_04_DAYS;
        }

        // align to a 15 minutes update rate
        if (nextSync % TIME_15_MINUTES != 0) {
            nextSync = (nextSync / TIME_15_MINUTES) * TIME_15_MINUTES + TIME_15_MINUTES;
        }

        return nextSync;
    }

    @Override
    protected void onPostExecute(Void result) {
        wakeLock.release();
    }

}
