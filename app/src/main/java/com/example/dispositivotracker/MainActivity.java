package com.example.dispositivotracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.graphics.Color;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
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
    private Button editPhoneButton;
    private static final String TAG = "MAIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🔄 onCreate iniciado");
        setContentView(R.layout.activity_main);

        iniciarServicioSiPermisos();

        pedirPermisosSiEsNecesario();
        verificarYAdvertirUbicacion();

        sendButton = findViewById(R.id.sendButton);
        phoneEditText = findViewById(R.id.phoneEditText);
        confirmPhoneButton = findViewById(R.id.confirmPhoneButton);
        editPhoneButton = findViewById(R.id.editPhoneButton);


        SharedPreferences prefs = getSharedPreferences("tracker_prefs", MODE_PRIVATE);
        String savedNumber = prefs.getString("numero_linea", "");
        Log.d(TAG, "📂 Número recuperado de prefs: " + savedNumber);

        if (!savedNumber.isEmpty()) {
            phoneEditText.setText(savedNumber);
            phoneEditText.setEnabled(false);
            phoneEditText.setTextColor(Color.parseColor("#999999"));
            phoneEditText.setAlpha(0.5f);
            phoneEditText.setTextColor(Color.GRAY);
            phoneEditText.setBackgroundColor(Color.parseColor("#EEEEEE"));
            confirmPhoneButton.setEnabled(false);
            editPhoneButton.setVisibility(View.VISIBLE);
            Log.d(TAG, "✅ Número ya confirmado previamente, desactivando input");
        } else {
            editPhoneButton.setVisibility(View.GONE);
        }

        confirmPhoneButton.setOnClickListener(v -> {
            String numeroIngresado = phoneEditText.getText().toString().trim();
            Log.d(TAG, "📥 Botón confirmar clickeado. Número ingresado: " + numeroIngresado);

            if (!numeroIngresado.isEmpty()) {
                SharedPreferences.Editor editor = getSharedPreferences("tracker_prefs", MODE_PRIVATE).edit();
                editor.putString("numero_linea", numeroIngresado);
                editor.apply();

                phoneEditText.setEnabled(false);
                phoneEditText.setAlpha(0.5f); // 🔘 visualmente desactivado
                confirmPhoneButton.setEnabled(false);
                editPhoneButton.setVisibility(View.VISIBLE);

                Toast.makeText(MainActivity.this, "✅ Número guardado", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "💾 Número guardado en SharedPreferences y campos desactivados");
            } else {
                Toast.makeText(MainActivity.this, "⚠️ Ingrese un número válido", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "❌ Intento de guardar número vacío");
            }
        });

        editPhoneButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Autenticación requerida");
            builder.setMessage("Ingrese la clave para editar el número");

            final EditText input = new EditText(MainActivity.this);
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);

            builder.setPositiveButton("Aceptar", (dialog, which) -> {
                String enteredPassword = input.getText().toString().trim();
                if (enteredPassword.equals("box4321")) {
                    phoneEditText.setEnabled(true);
                    phoneEditText.setAlpha(1.0f); // 🔘 visualmente activo
                    confirmPhoneButton.setEnabled(true);
                    editPhoneButton.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "✏️ Editá el número y volvé a confirmar", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "❌ Clave incorrecta", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
            builder.show();
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

    private void pedirPermisosSiEsNecesario() {
        Log.d(TAG, "🔐 Verificando permisos necesarios");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {

                Log.w(TAG, "📛 Permisos no otorgados, solicitando...");
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.READ_PHONE_NUMBERS,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.FOREGROUND_SERVICE_LOCATION
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

    private void iniciarServicioSiPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED) {

            Intent intent = new Intent(this, DeviceTrackerService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            Log.d(TAG, "🚀 Servicio DeviceTrackerService iniciado");
        } else {
            Log.w(TAG, "⛔ No se puede iniciar el servicio, permisos faltantes");
        }
    }

}