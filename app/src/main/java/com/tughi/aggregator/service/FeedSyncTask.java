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
import com.tughi.aggregator.content.Uris;
import com.tughi.aggregator.feeds.FeedParser;
import com.tughi.aggregator.feeds.FeedParserException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
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

        Log.i(getClass().getName(), "Updating feed " + feedId);

        try {
            // open connection
            URLConnection connection = new URL(feedUrl).openConnection();

            // parse feed
            FeedParser.Result result;
            try {
                result = FeedParser.parse(connection);
            } catch (FeedParserException exception) {
                throw new IOException(exception);
            }

            if (result.status == HttpURLConnection.HTTP_OK) {
                // prepare content batch
                ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>(result.feed.entries.size() + 1);

                // update feed values
                ContentValues feedSyncValues = new ContentValues();
                feedSyncValues.put(FeedColumns.URL, connection.getURL().toString());
                feedSyncValues.put(FeedColumns.TITLE, result.feed.title);
                feedSyncValues.put(FeedColumns.LINK, result.feed.link);
                feedSyncValues.put(FeedColumns.ENTRY_COUNT, result.feed.entries.size());
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
                    entryValues.put(EntryColumns.UPDATED, entry.updatedTimestamp != null ? entry.updatedTimestamp : poll);
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
                ContentResolver contentResolver = context.getContentResolver();
                try {
                    contentResolver.applyBatch(DatabaseContentProvider.AUTHORITY, batch);

                    contentResolver.notifyChange(Uris.newFeedsUri(), null);
                    contentResolver.notifyChange(Uris.newSyncFeedsUri(), null);
                } catch (Exception exception) {
                    throw new IOException("batch failed", exception);
                }

                // schedule next sync
                ContentValues feedUserValues = new ContentValues();
                feedUserValues.put(FeedColumns.NEXT_SYNC, findNextSync(feedId, feedUpdateMode, poll));
                contentResolver.update(Uris.newUserFeedUri(feedId), feedUserValues, null, null);

                FeedSyncScheduler.scheduleAlarm(context, poll);
            } else {
                Log.w(getClass().getName(), "feed update failed: " + result.status);
            }
        } catch (IOException exception) {
            if (BuildConfig.DEBUG) {
                Log.w(getClass().getName(), "feed update failed", exception);
            } else {
                Log.w(getClass().getName(), "feed update failed: " + exception.getMessage());
            }
        }

        return null;
    }

    private long findNextSync(long feedId, int feedUpdateMode, long poll) {
        switch (feedUpdateMode) {
            case FeedUpdateModes.AUTO:
            default:
                return findNextAutoSync(feedId, poll);
        }
    }

    private long findNextAutoSync(long feedId, long poll) {
        ContentResolver contentResolver = context.getContentResolver();

        Uri feedEntriesUri = Uris.newFeedEntriesUri(feedId);
        final String[] projection = {EntryColumns.ID};
        final String selection = EntryColumns.UPDATED + " >= ?";
        final String[] selectionArgs = new String[1];
        Cursor cursor;

        // get aggregated number of entries in the last 24 hours
        selectionArgs[0] = Long.toString(poll - 86400000);
        cursor = contentResolver.query(feedEntriesUri, projection, selection, selectionArgs, null);
        int day_entries = cursor.getCount();
        cursor.close();

        int poll_rate;
        if (day_entries > 0) {
            poll_rate = 86400000 / day_entries;
        } else {
            // get aggregated number of entries in the last 7 * 24 hours
            selectionArgs[0] = Long.toString(poll - 604800000);
            cursor = contentResolver.query(feedEntriesUri, projection, selection, selectionArgs, null);
            int week_entries = cursor.getCount();
            cursor.close();

            if (week_entries > 0) {
                poll_rate = 604800000 / week_entries;
            } else {
                poll_rate = 345600000;
            }
        }

        if (poll_rate < 1800000) {
            // schedule new poll in 15 minutes
            return poll + 900000;
        } else if (poll_rate < 3600000) {
            // schedule new poll in 30 minutes
            return poll + 1800000;
        } else if (poll_rate < 10800000) {
            // schedule new poll in 1 hour
            return poll + 3600000;
        } else if (poll_rate < 21600000) {
            // schedule new poll in 3 hours
            return poll + 10800000;
        } else if (poll_rate < 43200000) {
            // schedule new poll in 6 hours
            return poll + 21600000;
        } else if (poll_rate < 86400000) {
            // schedule new poll in 12 hours
            return poll + 43200000;
        } else if (poll_rate < 172800000) {
            // schedule new poll in 1 day
            return poll + 86400000;
        } else if (poll_rate < 259200000) {
            // schedule new poll in 2 day
            return poll + 172800000;
        } else if (poll_rate < 345600000) {
            // schedule new poll in 3 day
            return poll + 259200000;
        } else {
            // schedule new poll in 4 days
            return poll + 345600000;
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        wakeLock.release();
    }

}
