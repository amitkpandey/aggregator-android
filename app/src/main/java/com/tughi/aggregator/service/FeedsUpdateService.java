package com.tughi.aggregator.service;

import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.tughi.aggregator.BuildConfig;
import com.tughi.aggregator.content.DatabaseContentProvider;
import com.tughi.aggregator.content.EntryColumns;
import com.tughi.aggregator.content.FeedColumns;
import com.tughi.aggregator.content.Uris;
import com.tughi.aggregator.feeds.FeedParser;
import com.tughi.aggregator.feeds.FeedParserException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * An {@link Service} that updates the scheduled feeds.
 */
public class FeedsUpdateService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // start the multi-threaded update
        new LoadFeedsTask().execute();

        // tell Android to restart the service if its process is killed
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // cannot bind to this service
        return null;
    }

    /**
     * An {@link AsyncTask} that gathers all feeds that should be updated.
     */
    private class LoadFeedsTask extends AsyncTask<Void, Void, List<Object[]>> {

        private final String[] FEED_PROJECTION = {
                FeedColumns.ID,
                FeedColumns.URL
        };
        private final int FEED_ID_INDEX = 0;
        private final int FEED_URL_INDEX = 1;

        /**
         * Selects all feeds that should be updated 'now'.
         */
        private final String FEED_SELECTION = FeedColumns.ID + " > 0 AND "
                + FeedColumns.NEXT_POLL + " < strftime('%s', 'now')";

        /**
         * Returns a {@link List} of {@link UpdateFeedTask#execute(Object[])} parameters.
         */
        @Override
        protected List<Object[]> doInBackground(Void... voids) {
            LinkedList<Object[]> result = new LinkedList<Object[]>();

            long poll = System.currentTimeMillis() / 1000;

            Cursor cursor = getContentResolver().query(Uris.newFeedsUri(), FEED_PROJECTION, FEED_SELECTION, null, null);
            if (cursor.moveToFirst()) {
                do {
                    Object[] parameters = {
                            cursor.getLong(FEED_ID_INDEX),
                            cursor.getString(FEED_URL_INDEX),
                            poll
                    };
                    result.add(parameters);
                } while (cursor.moveToNext());
            }
            cursor.close();

            return result;
        }

        /**
         * Starts {@link UpdateFeedTask}s for each result.
         */
        @Override
        protected void onPostExecute(List<Object[]> result) {
            for (Object[] parameters : result) {
                new UpdateFeedTask().executeOnExecutor(THREAD_POOL_EXECUTOR, parameters);
            }
        }

    }

    /**
     * Updates a feed on a background thread.
     */
    private class UpdateFeedTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... objects) {
            final long feedId = (Long) objects[0];
            final String feedUrl = (String) objects[1];
            final long poll = (Long) objects[2];

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

                    // TODO: update feed meta data

                    // insert/update feed entries
                    for (FeedParser.Result.Feed.Entry entry : result.feed.entries) {
                        // process entry values
                        ContentValues entryValues = new ContentValues();
                        entryValues.put(EntryColumns.FEED_ID, feedId);
                        entryValues.put(EntryColumns.GUID, entry.id);
                        entryValues.put(EntryColumns.TITLE, entry.title);
                        entryValues.put(EntryColumns.UPDATED, entry.updatedTimestamp);
                        entryValues.put(EntryColumns.POLL, poll);
                        entryValues.put(EntryColumns.DATA, entry.title);

                        // add insert operation
                        ContentProviderOperation operation = ContentProviderOperation
                                .newInsert(feedEntriesUri)
                                .withValues(entryValues)
                                .build();
                        batch.add(operation);
                    }

                    // execute batch
                    ContentResolver contentResolver = getContentResolver();
                    try {
                        contentResolver.applyBatch(DatabaseContentProvider.AUTHORITY, batch);

                        contentResolver.notifyChange(Uris.newFeedsUri(), null);
                    } catch (Exception exception) {
                        throw new IOException("batch failed", exception);
                    }
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
    }

}
