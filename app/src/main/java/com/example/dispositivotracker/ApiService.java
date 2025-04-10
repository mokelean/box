package com.example.dispositivotracker;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/report")
    Call<ResponseBody> sendDeviceInfo(@Body DeviceInfo info);
}