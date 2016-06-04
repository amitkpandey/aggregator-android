package com.tughi.aggregator.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.tughi.aggregator.content.FeedColumns;
import com.tughi.aggregator.content.FeedUpdateModes;
import com.tughi.aggregator.content.Uris;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link IntentService} that syncs the feeds.
 */
public class FeedsSyncService extends IntentService {

    /**
     * Boolean extra used to force a sync, by ignoring the next sync value.
     */
    public static final String EXTRA_FORCE = "force";

    private static final String[] FEED_PROJECTION = {
            FeedColumns.ID,
            FeedColumns.URL,
            FeedColumns.UPDATE_MODE,
    };
    private static final int FEED_ID_INDEX = 0;
    private static final int FEED_URL_INDEX = 1;
    private static final int FEED_UPDATE_MODE = 2;

    private static final Executor EXECUTOR = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() + 1,
            Runtime.getRuntime().availableProcessors() * 2 + 1,
            1,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);

                public Thread newThread(@NonNull Runnable runnable) {
                    return new Thread(runnable, FeedsSyncService.class.getSimpleName() + "#" + counter.getAndIncrement());
                }
            }
    );

    public FeedsSyncService() {
        super(FeedsSyncService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long poll = System.currentTimeMillis();

        ContentResolver contentResolver = this.getContentResolver();

        // find feeds to sync
        Cursor cursor;
        if (intent.getBooleanExtra(EXTRA_FORCE, false)) {
            String selection = FeedColumns.ID + " > 0";
            cursor = contentResolver.query(Uris.newFeedsUri(), FEED_PROJECTION, selection, null, null);
        } else {
            String selection = FeedColumns.ID + " > 0 AND "
                    + FeedColumns.UPDATE_MODE + " != " + FeedUpdateModes.DISABLED + " AND "
                    + FeedColumns.NEXT_SYNC + " < CAST(? AS INTEGER)";
            String[] selectionArgs = {Long.toString(poll)};
            cursor = contentResolver.query(Uris.newFeedsUri(), FEED_PROJECTION, selection, selectionArgs, null);
        }
        assert cursor != null;
        if (cursor.moveToFirst()) {
            do {
                // sync feed on a separated thread
                new FeedSyncTask(this)
                        .executeOnExecutor(
                                EXECUTOR,
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
