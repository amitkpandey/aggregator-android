package com.tughi.aggregator.service;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Handles the {@link Intent#ACTION_BOOT_COMPLETED} and {@link Intent#ACTION_MY_PACKAGE_REPLACED}
 * by starting the {@link FeedsSyncService}.
 */
public class FeedsSyncReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        startWakefulService(context, new Intent(context, FeedsSyncService.class));
    }

}
