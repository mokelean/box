package com.example.dispositivotracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendButton = findViewById(R.id.sendButton);

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
}
