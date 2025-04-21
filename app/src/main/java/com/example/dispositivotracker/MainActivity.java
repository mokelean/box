package com.example.dispositivotracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private Button sendButton;
    private EditText phoneEditText;
    private Button confirmPhoneButton;
    private static final String TAG = "MAIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🔄 onCreate iniciado");
        setContentView(R.layout.activity_main);

        pedirPermisosSiEsNecesario();
        verificarYAdvertirUbicacion();
        solicitarIgnorarOptimizaciones();
        solicitarPermisoAutoInicio();
        sugerirInicioAutomaticoParaTCL(); // ✅ Agregado para TCL

        WorkManager.getInstance(this).cancelUniqueWork("device_info_worker");
        Log.d(TAG, "🧹 Cancelando trabajos previos");

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DeviceInfoWorker.class,
                15, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "device_info_worker",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
        );

        Log.d(TAG, "⏱️ PeriodicWorkRequest limpio y registrado desde MainActivity");

        sendButton = findViewById(R.id.sendButton);
        phoneEditText = findViewById(R.id.phoneEditText);
        confirmPhoneButton = findViewById(R.id.confirmPhoneButton);

        SharedPreferences prefs = getSharedPreferences("tracker_prefs", MODE_PRIVATE);
        String savedNumber = prefs.getString("numero_linea", "");
        Log.d(TAG, "📂 Número recuperado de prefs: " + savedNumber);

        if (!savedNumber.isEmpty()) {
            phoneEditText.setText(savedNumber);
            phoneEditText.setEnabled(false);
            confirmPhoneButton.setEnabled(false);
            Log.d(TAG, "✅ Número ya confirmado previamente, desactivando input");
        }

        confirmPhoneButton.setOnClickListener(v -> {
            String numeroIngresado = phoneEditText.getText().toString().trim();
            Log.d(TAG, "📥 Botón confirmar clickeado. Número ingresado: " + numeroIngresado);

            if (!numeroIngresado.isEmpty()) {
                SharedPreferences.Editor editor = getSharedPreferences("tracker_prefs", MODE_PRIVATE).edit();
                editor.putString("numero_linea", numeroIngresado);
                editor.apply();

                phoneEditText.setEnabled(false);
                confirmPhoneButton.setEnabled(false);

                Toast.makeText(MainActivity.this, "✅ Número guardado", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "💾 Número guardado en SharedPreferences y campos desactivados");
            } else {
                Toast.makeText(MainActivity.this, "⚠️ Ingrese un número válido", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "❌ Intento de guardar número vacío");
            }
        });

        sendButton.setOnClickListener(v -> {
            Log.d(TAG, "📤 Botón 'Enviar reporte' clickeado - envío directo");

            SharedPreferences prefsLocal = getSharedPreferences("tracker_prefs", MODE_PRIVATE);
            String phoneNumber = prefsLocal.getString("numero_linea", "");

            DeviceInfo info = new DeviceInfo(
                    Build.BRAND,
                    Build.MODEL,
                    Build.MANUFACTURER,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT,
                    Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID),
                    getWifiIpAddress(),
                    getWifiSsid(),
                    getBatteryLevel(),
                    isConnectedViaWifi() ? "Wi-Fi" : "Datos móviles",
                    "Manual",
                    phoneNumber
            );

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.25.59:5001")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            Call<ResponseBody> call = apiService.sendDeviceInfo(info);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    Response<ResponseBody> response = call.execute();
                    if (response.isSuccessful()) {
                        Log.i(TAG, "✅ POST exitoso desde botón");
                    } else {
                        Log.e(TAG, "❌ Error HTTP: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "🚨 Error al enviar POST: " + e.getMessage(), e);
                }
            });
        });
    }

    private void solicitarIgnorarOptimizaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Toast.makeText(this, "Para que la app funcione siempre, se solicitará ignorar optimización de batería.", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                Log.d(TAG, "🔋 Ya se ignoran las optimizaciones de batería");
            }
        }
    }

    private void solicitarPermisoAutoInicio() {
        String fabricante = Build.MANUFACTURER.toLowerCase();
        Log.d(TAG, "📱 Fabricante detectado: " + fabricante);

        Intent intent = new Intent();
        switch (fabricante) {
            case "xiaomi":
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                break;
            case "huawei":
                intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
                break;
            case "oppo":
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"));
                break;
            case "vivo":
                intent.setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
                break;
            case "letv":
                intent.setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"));
                break;
            case "asus":
                intent.setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity"));
                break;
            default:
                Log.d(TAG, "ℹ️ Fabricante no requiere manejo especial de auto-inicio");
                return;
        }

        try {
            startActivity(intent);
            Toast.makeText(this, "Activá el inicio automático para esta app si es posible.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.w(TAG, "❌ No se pudo abrir la pantalla de auto-inicio: " + e.getMessage());
        }
    }

    private void sugerirInicioAutomaticoParaTCL() {
        if (Build.MANUFACTURER.toLowerCase().contains("tcl") || Build.BRAND.toLowerCase().contains("tcl")) {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Habilitar inicio automático")
                    .setMessage("Para que la app funcione después de reinicios, por favor activá el inicio automático para esta aplicación en la configuración del sistema.")
                    .setPositiveButton("Ir a configuración", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    private void pedirPermisosSiEsNecesario() {
        Log.d(TAG, "🔐 Verificando permisos necesarios");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.w(TAG, "📛 Permisos no otorgados, solicitando...");
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.READ_PHONE_NUMBERS,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        1);
            } else {
                Log.d(TAG, "✅ Todos los permisos ya otorgados");
            }
        }
    }

    private void verificarYAdvertirUbicacion() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            Log.e(TAG, "⚠️ Error al verificar GPS", ex);
        }

        if (!gpsEnabled) {
            new AlertDialog.Builder(this)
                    .setMessage("La ubicación está desactivada. Necesitamos que la actives para obtener el SSID de Wi-Fi.")
                    .setPositiveButton("Activar ubicación", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancelar", null)
                    .setCancelable(false)
                    .show();
        }
    }

    private String getWifiIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ipInt = wifiManager.getConnectionInfo().getIpAddress();
            return String.format("%d.%d.%d.%d",
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

    private String getWifiSsid() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⛔ Permiso de ubicación no otorgado, SSID desconocido");
            return "Desconocido";
        }
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

    private int getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        return batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
    }

    private boolean isConnectedViaWifi() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        return false;
    }
}
