package com.example.dispositivotracker;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DeviceStartWorker extends Worker {

    private static final String TAG = "DEVICE_START_WORKER";

    public DeviceStartWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🕒 Esperando 30 segundos para iniciar servicio...");
        SystemClock.sleep(30000); // Espera real antes de iniciar el servicio

        try {
            Intent serviceIntent = new Intent(getApplicationContext(), DeviceTrackerService.class);

            // ✅ Llamamos startService() para evitar la restricción de Android 13/14
            getApplicationContext().startService(serviceIntent);

            Log.d(TAG, "🚀 DeviceTrackerService iniciado desde DeviceStartWorker");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error al iniciar servicio desde DeviceStartWorker", e);
            return Result.failure();
        }
    }
}
