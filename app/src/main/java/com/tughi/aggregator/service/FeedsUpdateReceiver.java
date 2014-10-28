package com.tughi.aggregator.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A {@link BroadcastReceiver} used to start the {@link FeedsUpdateService}.
 */
public class FeedsUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, FeedsUpdateService.class));
    }

}
