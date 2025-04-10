package com.example.dispositivotracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
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

        // Programar tarea periódica cada 15 minutos
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                DeviceInfoWorker.class,
                15, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DeviceInfoWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );

        Log.d("WORKER", "Tarea periódica programada");
    }

    // Ejecutar el worker una sola vez manualmente al tocar el botón
    private void ejecutarTareaManual() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DeviceInfoWorker.class).build();
        WorkManager.getInstance(this).enqueue(request);
    }
}