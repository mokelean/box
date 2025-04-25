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

    private static final String CHANNEL_ID = "tracker_channel_debug";
    private static final String TAG = "DeviceTrackerService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🔧 onCreate llamado: creando canal de notificación");
        crearCanalNotificacion();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🚀 onStartCommand ejecutado");

        try {
            Log.d(TAG, "🔔 Creando notificación de foreground...");
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Tracker activo")
                    .setContentText("Reportando dispositivo en segundo plano")
                    .setSmallIcon(R.drawable.ic_stat_notify) // ✔️ icono vector blanco sin fondo
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setOngoing(true)
                    .build();

            Log.d(TAG, "⏩ Llamando a startForeground...");
            startForeground(1, notification);
            Log.d(TAG, "✅ Notificación enviada a foreground con éxito");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al iniciar servicio en foreground: " + e.getMessage(), e);
        }

        try {
            Log.d(TAG, "🔁 Cancelando trabajos anteriores del worker...");
            WorkManager.getInstance(this).cancelUniqueWork("device_info_worker");

            Log.d(TAG, "🛠️ Configurando nuevo DeviceInfoWorker...");
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    DeviceInfoWorker.class,
                    15, TimeUnit.MINUTES
            ).build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "device_info_worker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
            );
            Log.d(TAG, "📦 DeviceInfoWorker encolado correctamente");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al configurar el Worker: " + e.getMessage(), e);
        }

        return START_STICKY;
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "🧪 API >= 26: creando NotificationChannel...");
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Canal Debug Tracker",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Canal para seguimiento de Tracker en background");

            try {
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                    Log.d(TAG, "✅ Canal de notificación creado con éxito");
                } else {
                    Log.e(TAG, "❌ NotificationManager es null");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error creando canal: " + e.getMessage(), e);
            }
        } else {
            Log.d(TAG, "ℹ️ No se necesita canal para versiones < Android O");
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "🛑 onDestroy: Servicio detenido");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
