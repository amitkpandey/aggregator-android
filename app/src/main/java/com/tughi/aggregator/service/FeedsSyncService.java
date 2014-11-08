package com.tughi.aggregator.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.tughi.aggregator.content.FeedColumns;
import com.tughi.aggregator.content.FeedUpdateModes;
import com.tughi.aggregator.content.Uris;

/**
 * A {@link IntentService} that syncs the feeds.
 */
public class FeedsSyncService extends IntentService {

    private static final String[] FEED_PROJECTION = {
            FeedColumns.ID,
            FeedColumns.URL,
            FeedColumns.UPDATE_MODE,
    };
    private static final int FEED_ID_INDEX = 0;
    private static final int FEED_URL_INDEX = 1;
    private static final int FEED_UPDATE_MODE = 2;

    private static final String FEED_SELECTION = FeedColumns.ID + " > 0 AND "
            + FeedColumns.UPDATE_MODE + " != " + FeedUpdateModes.DISABLED + " AND "
            + FeedColumns.NEXT_SYNC + " < :poll:";

    public FeedsSyncService() {
        super(FeedsSyncService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long poll = System.currentTimeMillis();

        // find feeds to sync
        String selection = FEED_SELECTION.replace(":poll:", Long.toString(poll));
        ContentResolver contentResolver = this.getContentResolver();
        Cursor cursor = contentResolver.query(Uris.newFeedsUri(), FEED_PROJECTION, selection, null, null);
        if (cursor.moveToFirst()) {
            do {
                // sync feed on a separated thread
                new FeedSyncTask(this)
                        .executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR,
                                cursor.getLong(FEED_ID_INDEX),
                                cursor.getString(FEED_URL_INDEX),
                                cursor.getInt(FEED_UPDATE_MODE),
                                poll
                        );
            } while (cursor.moveToNext());
        } else {
            // nothing to sync
            FeedSyncScheduler.scheduleAlarm(this, poll);
        }
        cursor.close();

        // release the wake lock
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

}
