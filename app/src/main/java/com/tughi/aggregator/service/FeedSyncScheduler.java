package com.tughi.aggregator.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import com.tughi.aggregator.BuildConfig;
import com.tughi.aggregator.content.FeedColumns;
import com.tughi.aggregator.content.Uris;

/**
 * Helper methods for scheduling th next feeds sync.
 */
public class FeedSyncScheduler {

    private static final String TAG = FeedSyncScheduler.class.getName();

    private static final String[] USER_FEED_PROJECTION = {
            FeedColumns.NEXT_SYNC
    };
    private static final int USER_FEED_NEXT_SYNC = 0;

    private static final String USER_FEED_SELECTION = FeedColumns.NEXT_SYNC + " > CAST(? AS INTEGER)";

    private static final String USER_FEED_SORT_ORDER = FeedColumns.NEXT_SYNC;

    /**
     * Invoked from a background thread of an {@link AsyncTask} to schedule the next alarm.
     */
    public static void scheduleAlarm(Context context, long poll) {
        if (BuildConfig.DEBUG) {
            // check current thread
            Looper looper = Looper.myLooper();
            if (looper != null && looper == Looper.getMainLooper()) {
                // running on the main thread
                throw new IllegalStateException("Invoked on the main thread");
            }
        }

        ContentResolver contentResolver = context.getContentResolver();

        // find next sync
        final String[] selectionArgs = {Long.toString(poll)};
        Cursor cursor = contentResolver.query(Uris.newUserFeedsUri(), USER_FEED_PROJECTION, USER_FEED_SELECTION, selectionArgs, USER_FEED_SORT_ORDER);
        if (cursor.moveToFirst()) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(context, FeedsSyncReceiver.class);
            PendingIntent syncPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            long triggerAtMillis = cursor.getLong(USER_FEED_NEXT_SYNC);
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, syncPendingIntent);

            Log.i(TAG, String.format("next sync: %tc", triggerAtMillis));
        }
        cursor.close();
    }
}
