package com.example.dispositivotracker;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.gson.annotations.SerializedName;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class MainActivity extends AppCompatActivity {

    // ==== MODELO Report ====
    public static class Report {
        @SerializedName("title")
        private String title;

        @SerializedName("description")
        private String description;

        public Report(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    // ==== INTERFAZ DE API ====
    public interface ApiService {
        @POST("report")  // ¡sin / inicial si baseUrl termina en /
        Call<ResponseBody> sendReport(@Body Report report);
    }

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String BASE_URL = "http://10.0.2.2:5000/"; // ← Con / final (para emulador)

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        Button sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarReporte();
            }
        });
    }

    private void enviarReporte() {
        Report report = new Report("Hola desde botón", "Esto es un POST desde Android");

        Log.d("API", "Enviando reporte...");

        apiService.sendReport(report).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("API", "✅ Reporte enviado correctamente");
                } else {
                    Log.e("API", "❌ Error en respuesta: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("API", "🚨 Fallo en conexión: " + t.getMessage(), t);
            }
        });
    }
}



