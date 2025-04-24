package com.example.dispositivotracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class DeviceTrackerService extends Service {

    private static final String CHANNEL_ID = "tracker_channel_v2";  // ← nuevo ID para forzar recreación
    private static final String TAG = "FOREGROUND_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🔧 Servicio en primer plano creado");

        crearCanalNotificacion();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tracker activo")
                .setContentText("Reportando dispositivo en segundo plano")
                .setSmallIcon(R.mipmap.ic_launcher) // ← tu ícono chico
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // prioridad adecuada
                .setOngoing(true) // evita que el usuario lo cierre por error
                .build();

        startForeground(1, notification);

        WorkManager.getInstance(this).cancelUniqueWork("device_info_worker");

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DeviceInfoWorker.class,
                15, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "device_info_worker",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
        );

        Log.d(TAG, "📦 DeviceInfoWorker reprogramado desde el servicio");
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Tracker en segundo plano",
                    NotificationManager.IMPORTANCE_DEFAULT  // ← prioridad visible
            );
            channel.setDescription("Muestra que la app TrackerBox está activa en segundo plano");
            channel.setShowBadge(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🚀 Servicio en primer plano iniciado");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 Servicio detenido");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
