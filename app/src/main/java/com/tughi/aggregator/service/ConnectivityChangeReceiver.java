package com.tughi.aggregator.service;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Handles the {@link ConnectivityManager#CONNECTIVITY_ACTION} by starting the
 * {@link FeedsSyncService}.
 */
public class ConnectivityChangeReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            // connection available

            startWakefulService(context, new Intent(context, FeedsSyncService.class));
        }
    }

}
