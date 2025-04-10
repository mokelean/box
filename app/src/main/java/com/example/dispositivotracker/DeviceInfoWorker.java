package com.example.dispositivotracker;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

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

        String ip = getLocalIpAddress();

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int batteryLevel = -1;
        if (batteryStatus != null) {
            batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        }

        boolean isWifi = isConnectedViaWifi(context);

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
                    .baseUrl("http://10.0.2.2:5000") // Cambiar si estás en un dispositivo real
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);

            Call<ResponseBody> call = apiService.sendDeviceInfo(info);
            Response<ResponseBody> response = call.execute();

            if (response.isSuccessful()) {
                Log.d("WORKER", "✅ Info enviada correctamente");
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

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("WORKER", "Error IP: " + ex.getMessage());
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

