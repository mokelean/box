package com.example.dispositivotracker;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DeviceInfoWorker extends Worker {

    public DeviceInfoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // ✅ Obtener IP solo si está en Wi-Fi
        String ip = getWifiIpAddress(context);

        // 🔋 Nivel de batería
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int batteryLevel = -1;
        if (batteryStatus != null) {
            batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        }

        boolean isWifi = isConnectedViaWifi(context);

        // 📦 Armar objeto con info del dispositivo
        DeviceInfo info = new DeviceInfo(
                Build.BRAND,
                Build.MODEL,
                Build.MANUFACTURER,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT,
                Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID),
                ip,
                batteryLevel,
                isWifi ? "Wi-Fi" : "Datos móviles"
        );

        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.23.151:5000") // Cambiar si usás otro host
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            Call<ResponseBody> call = apiService.sendDeviceInfo(info);
            Response<ResponseBody> response = call.execute();

            if (response.isSuccessful()) {
                Log.d("WORKER", "✅ Info enviada correctamente");

                // 🔁 Reprogramar en 3 minutos
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DeviceInfoWorker.class)
                        .setInitialDelay(3, TimeUnit.MINUTES)
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueue(request);

                return Result.success();
            } else {
                Log.e("WORKER", "❌ Error HTTP: " + response.code());
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e("WORKER", "🚨 Fallo al enviar: " + e.getMessage(), e);
            return Result.retry();
        }
    }

    // ✅ Obtener IP solo si está conectado por Wi-Fi
    private String getWifiIpAddress(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                int ipInt = wifiManager.getConnectionInfo().getIpAddress();
                return String.format(
                        "%d.%d.%d.%d",
                        (ipInt & 0xff),
                        (ipInt >> 8 & 0xff),
                        (ipInt >> 16 & 0xff),
                        (ipInt >> 24 & 0xff)
                );
            }
        } catch (Exception e) {
            Log.e("WORKER", "Error al obtener IP Wi-Fi: " + e.getMessage(), e);
        }
        return "0.0.0.0";
    }

    private boolean isConnectedViaWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        return false;
    }
}

