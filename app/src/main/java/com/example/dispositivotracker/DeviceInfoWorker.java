package com.example.dispositivotracker;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
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
        SharedPreferences prefs = context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE);

        String ip = getWifiIpAddress(context);

        int batteryLevel = -1;
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        }

        boolean isWifi = isConnectedViaWifi(context);

        // Obtener operador y número de teléfono
        String simOperator = "Desconocido";
        String phoneNumber = prefs.getString("numero_linea", "");

        if (phoneNumber.isEmpty()) {
            try {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    simOperator = tm.getSimOperatorName();
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        phoneNumber = tm.getLine1Number();
                    }
                }
            } catch (SecurityException e) {
                Log.e("WORKER", "⚠️ Permiso READ_PHONE_STATE no otorgado");
            }
        }

        Log.d("WORKER", "📞 Número de línea cargado desde prefs (o SIM): " + phoneNumber);

        DeviceInfo info = new DeviceInfo(
                Build.BRAND,
                Build.MODEL,
                Build.MANUFACTURER,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT,
                Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID),
                ip,
                batteryLevel,
                isWifi ? "Wi-Fi" : "Datos móviles",
                simOperator,
                phoneNumber
        );

        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.23.151:5000")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            Call<ResponseBody> call = apiService.sendDeviceInfo(info);
            Response<ResponseBody> response = call.execute();

            if (response.isSuccessful()) {
                Log.d("WORKER", "✅ Info enviada correctamente (se reprograma en 8 horas)");

                OneTimeWorkRequest nextSuccessRequest = new OneTimeWorkRequest.Builder(DeviceInfoWorker.class)
                        .setInitialDelay(8, TimeUnit.HOURS)
                        .build();
                WorkManager.getInstance(context).enqueue(nextSuccessRequest);

                return Result.success();
            } else {
                Log.e("WORKER", "❌ Error HTTP: " + response.code() + " (se reprograma en 1 hora)");

                OneTimeWorkRequest retryRequest = new OneTimeWorkRequest.Builder(DeviceInfoWorker.class)
                        .setInitialDelay(1, TimeUnit.HOURS)
                        .build();
                WorkManager.getInstance(context).enqueue(retryRequest);

                return Result.retry();
            }

        } catch (Exception e) {
            Log.e("WORKER", "🚨 Fallo al enviar: " + e.getMessage(), e);

            OneTimeWorkRequest retryRequest = new OneTimeWorkRequest.Builder(DeviceInfoWorker.class)
                    .setInitialDelay(1, TimeUnit.HOURS)
                    .build();
            WorkManager.getInstance(context).enqueue(retryRequest);

            return Result.retry();
        }
    }

    private String getWifiIpAddress(Context context) {
        try {
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ipInt = wifiManager.getConnectionInfo().getIpAddress();
            return String.format(
                    "%d.%d.%d.%d",
                    (ipInt & 0xff),
                    (ipInt >> 8 & 0xff),
                    (ipInt >> 16 & 0xff),
                    (ipInt >> 24 & 0xff)
            );
        } catch (Exception e) {
            Log.e("WORKER", "Error IP Wi-Fi: " + e.getMessage(), e);
            return "0.0.0.0";
        }
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
