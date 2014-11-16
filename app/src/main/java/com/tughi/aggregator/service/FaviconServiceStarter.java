package com.tughi.aggregator.service;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Starts the {@link FaviconSyncService} when the active connection is switched to WiFi.
 */
public class FaviconServiceStarter extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            // connection available

            startServiceIfWifi(context);
        }
    }

    public static void startServiceIfWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null
                && networkInfo.isConnected()
                && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            // connected to WiFi
            startWakefulService(context, new Intent(context, FaviconSyncService.class));
        }
    }

}
