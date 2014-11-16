package com.tughi.aggregator.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * A {@link BroadcastReceiver} used to enable/start the components which require a charging device.
 */
public class PowerConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final PackageManager packageManager = context.getPackageManager();
        final ComponentName faviconServiceStarter = new ComponentName(context, FaviconServiceStarter.class);

        int newState;
        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            // receive connectivity changes while charging
            newState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

            FaviconServiceStarter.startServiceIfWifi(context);
        } else {
            newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        }

        packageManager.setComponentEnabledSetting(faviconServiceStarter, newState, PackageManager.DONT_KILL_APP);
    }

}
