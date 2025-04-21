package com.example.dispositivotracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BOOT_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "🔁 BOOT_COMPLETED recibido: cancelando y reprogramando DeviceInfoWorker");

            // Cancelar y reprogramar el Worker
            WorkManager.getInstance(context).cancelUniqueWork("device_info_worker");

            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    DeviceInfoWorker.class,
                    30, TimeUnit.MINUTES
            ).build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "device_info_worker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
            );

            Log.d(TAG, "✅ PeriodicWorkRequest reprogramado desde BootReceiver");

            // Iniciar el servicio en segundo plano
            Log.d(TAG, "🚀 Iniciando DeviceTrackerService desde BootReceiver");
            Intent serviceIntent = new Intent(context, DeviceTrackerService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
