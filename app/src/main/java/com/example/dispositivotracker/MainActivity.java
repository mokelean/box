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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pedirPermisosSiEsNecesario();

        sendButton = findViewById(R.id.sendButton);
        phoneEditText = findViewById(R.id.phoneEditText);
        confirmPhoneButton = findViewById(R.id.confirmPhoneButton);

        // ✅ Cargar número guardado (si lo hay)
        SharedPreferences prefs = getSharedPreferences("tracker_prefs", MODE_PRIVATE);
        String savedNumber = prefs.getString("confirmed_phone", "");
        if (!savedNumber.isEmpty()) {
            phoneEditText.setText(savedNumber);
            phoneEditText.setEnabled(false);
            confirmPhoneButton.setEnabled(false);
        }

        confirmPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String numeroIngresado = phoneEditText.getText().toString().trim();
                if (!numeroIngresado.isEmpty()) {
                    SharedPreferences.Editor editor = getSharedPreferences("tracker_prefs", MODE_PRIVATE).edit();
                    editor.putString("confirmed_phone", numeroIngresado);
                    editor.apply();
                    phoneEditText.setEnabled(false);
                    confirmPhoneButton.setEnabled(false);
                    Toast.makeText(MainActivity.this, "✅ Número guardado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "⚠️ Ingrese un número válido", Toast.LENGTH_SHORT).show();
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MANUAL", "Botón tocado - ejecutando tarea manual");
                ejecutarTareaManual();
            }
        });

        // 🔁 Programar ejecución automática cada 3 minutos (modo prueba)
        programarTareaCada3Minutos();

        Log.d("WORKER", "⏱️ Tarea programada cada 3 minutos (modo prueba)");
    }

    private void ejecutarTareaManual() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DeviceInfoWorker.class).build();
        WorkManager.getInstance(this).enqueue(request);
    }

    private void programarTareaCada3Minutos() {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS},
                        1);
            }
        }
    }
}
