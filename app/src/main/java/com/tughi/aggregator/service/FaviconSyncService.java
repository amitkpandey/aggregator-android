package com.tughi.aggregator.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.tughi.aggregator.BuildConfig;
import com.tughi.aggregator.content.FeedColumns;
import com.tughi.aggregator.content.Uris;
import com.tughi.aggregator.feeds.FaviconFinder;

import java.io.IOException;

/**
 * An {@link IntentService} used to update the favicons.
 */
public class FaviconSyncService extends IntentService {

    private static final String TAG = FaviconSyncService.class.getName();

    private static final String[] FEED_PROJECTION = {
            FeedColumns.ID,
            FeedColumns.LINK
    };
    private static final int FEED_ID = 0;
    private static final int FEED_LINK = 1;

    private static final String FEED_SELECTION = FeedColumns.LINK + " NOT NULL";

    public FaviconSyncService() {
        super(FaviconSyncService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = contentResolver.query(Uris.newSyncFeedsUri(), FEED_PROJECTION, FEED_SELECTION, null, null);
            if (cursor.moveToFirst()) {
                final String updateSelection = FeedColumns.FAVICON + " IS NOT ?";
                final String[] updateSelectionArgs = new String[1];

                do {
                    long feedId = cursor.getLong(FEED_ID);
                    String feedLink = cursor.getString(FEED_LINK);

                    try {
                        FaviconFinder.Result result = FaviconFinder.find(feedLink);

                        if (result.faviconUrl != null) {
                            ContentValues userFeedValues = new ContentValues(1);
                            userFeedValues.put(FeedColumns.FAVICON, result.faviconUrl);

                            updateSelectionArgs[0] = result.faviconUrl;
                            contentResolver.update(Uris.newUserFeedUri(feedId), userFeedValues, updateSelection, updateSelectionArgs);
                        }
                    } catch (IOException exception) {
                        if (BuildConfig.DEBUG) {
                            Log.w(TAG, "Failed to find favicon: " + feedLink, exception);
                        } else {
                            Log.w(TAG, "Failed to find favicon: " + feedLink);
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        } finally {
            // release wake lock
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

}
