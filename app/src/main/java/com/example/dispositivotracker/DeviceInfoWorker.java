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
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DeviceInfoWorker extends Worker {

    private static final String TAG = "WORKER";

    public DeviceInfoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔥 Ejecutando DeviceInfoWorker");

        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE);

        String ip = getWifiIpAddress(context);
        int batteryLevel = getBatteryLevel(context);
        boolean isWifi = isConnectedViaWifi(context);
        String ssid = getWifiSsid(context);

        String phoneNumber = prefs.getString("numero_linea", "");
        if (phoneNumber.isEmpty()) {
            phoneNumber = prefs.getString("confirmed_phone", "");
            if (!phoneNumber.isEmpty()) {
                prefs.edit().putString("numero_linea", phoneNumber).apply();
            }
        }

        String simOperator = !phoneNumber.isEmpty() ? "Manual" : "Desconocido";
        if (simOperator.equals("Desconocido")) {
            try {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    simOperator = tm.getSimOperatorName();
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        phoneNumber = tm.getLine1Number();
                    }
                }
            } catch (SecurityException e) {
                Log.w(TAG, "READ_PHONE_STATE no concedido", e);
            }
        }

        DeviceInfo info = new DeviceInfo(
                Build.BRAND,
                Build.MODEL,
                Build.MANUFACTURER,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT,
                Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID),
                ip,
                ssid,
                batteryLevel,
                isWifi ? "Wi-Fi" : "Datos móviles",
                simOperator,
                phoneNumber
        );

        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.25.59:5001")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            Call<ResponseBody> call = apiService.sendDeviceInfo(info);
            Response<ResponseBody> response = call.execute();

            if (response.isSuccessful()) {
                Log.i(TAG, "✅ Datos enviados correctamente");
                return Result.success();
            } else {
                Log.w(TAG, "❌ Fallo HTTP " + response.code());
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(TAG, "🚨 Error al enviar datos: " + e.getMessage(), e);
            return Result.retry();
        }
    }

    private int getBatteryLevel(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        return batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
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
            Log.w(TAG, "Error al obtener IP", e);
            return "0.0.0.0";
        }
    }

    private String getWifiSsid(Context context) {
        try {
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.getConnectionInfo() != null) {
                String ssid = wifiManager.getConnectionInfo().getSSID();
                if (ssid != null) {
                    return ssid.replace("\"", "");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error al obtener SSID", e);
        }
        return "Desconocido";
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
