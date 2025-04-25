package com.example.dispositivotracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BOOT_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "🔁 BOOT_COMPLETED recibido");

            // ⛔️ NO usar startForegroundService aquí (prohibido por Android)
            Log.d(TAG, "🚀 Iniciando DeviceTrackerService en background desde BootReceiver");
            Intent serviceIntent = new Intent(context, DeviceTrackerService.class);
            context.startService(serviceIntent);  // solo startService
        }
    }
}
