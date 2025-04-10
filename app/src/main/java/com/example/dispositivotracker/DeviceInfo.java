package com.example.dispositivotracker;

import com.google.gson.annotations.SerializedName;

public class DeviceInfo {
    @SerializedName("brand")
    public String brand;

    @SerializedName("model")
    public String model;

    @SerializedName("manufacturer")
    public String manufacturer;

    @SerializedName("android_version")
    public String androidVersion;

    @SerializedName("api_level")
    public int apiLevel;

    @SerializedName("device_id")
    public String deviceId;

    @SerializedName("ip_address")
    public String ipAddress;

    @SerializedName("battery_level")
    public int batteryLevel;

    @SerializedName("network_type")
    public String networkType;

    public DeviceInfo(String brand, String model, String manufacturer, String androidVersion,
                      int apiLevel, String deviceId, String ipAddress,
                      int batteryLevel, String networkType) {
        this.brand = brand;
        this.model = model;
        this.manufacturer = manufacturer;
        this.androidVersion = androidVersion;
        this.apiLevel = apiLevel;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.batteryLevel = batteryLevel;
        this.networkType = networkType;
    }
}