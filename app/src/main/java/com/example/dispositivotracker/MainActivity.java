package com.example.dispositivotracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

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

        confirmPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "📤 Botón 'Enviar reporte' clickeado");
                ejecutarTareaManual();
            }
        });

        programarTareaCada3Minutos();
        Log.d(TAG, "⏱️ Tarea automática programada cada 3 minutos");
    }

    private void ejecutarTareaManual() {
        Log.d(TAG, "🚀 Ejecutando tarea manual de envío");
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DeviceInfoWorker.class).build();
        WorkManager.getInstance(this).enqueue(request);
    }

    private void programarTareaCada3Minutos() {
        Log.d(TAG, "📅 Encolando tarea inicial con delay de 3 minutos");
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DeviceInfoWorker.class)
                .setInitialDelay(3, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "DeviceInfoTestWork",
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    private void pedirPermisosSiEsNecesario() {
        Log.d(TAG, "🔐 Verificando permisos READ_PHONE_STATE y READ_PHONE_NUMBERS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "📛 Permisos no otorgados, solicitando...");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS},
                        1);
            } else {
                Log.d(TAG, "✅ Permisos ya otorgados");
            }
        }
    }
}

