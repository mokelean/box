package com.example.dispositivotracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BOOT_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "🚀 BOOT_COMPLETED recibido, programando DeviceStartWorker...");

            WorkRequest startWorkerRequest = new OneTimeWorkRequest.Builder(DeviceStartWorker.class)
                    .setInitialDelay(30, TimeUnit.SECONDS) // ⏱️ Delay real manejado por el WorkManager
                    .build();

            WorkManager.getInstance(context).enqueue(startWorkerRequest);

            Log.d(TAG, "✅ DeviceStartWorker encolado para arrancar tras delay");
        }
    }
}

