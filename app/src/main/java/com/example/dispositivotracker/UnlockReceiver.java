package com.example.dispositivotracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UnlockReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_USER_PRESENT.equals(action)) {
            Log.d("UNLOCK_RECEIVER", "🔓 Dispositivo desbloqueado: iniciando servicio");
        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            Log.d("UNLOCK_RECEIVER", "💡 Pantalla encendida: iniciando servicio");
        } else {
            return;
        }

        Intent serviceIntent = new Intent(context, DeviceTrackerService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}