package com.example.dispositivotracker;

import com.google.gson.annotations.SerializedName;

public class DeviceInfo {

    @SerializedName("brand")
    private String brand;

    @SerializedName("model")
    private String model;

    @SerializedName("manufacturer")
    private String manufacturer;

    @SerializedName("android_version")
    private String androidVersion;

    @SerializedName("api_level")
    private int apiLevel;

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("ip_address")
    private String ipAddress;

    @SerializedName("battery_level")
    private int batteryLevel;

    @SerializedName("network_type")
    private String networkType;

    @SerializedName("sim_operator")
    private String simOperator;

    @SerializedName("phone_number")
    private String phoneNumber;

    public DeviceInfo(String brand, String model, String manufacturer, String androidVersion,
                      int apiLevel, String deviceId, String ipAddress, int batteryLevel,
                      String networkType, String simOperator, String phoneNumber) {
        this.brand = brand;
        this.model = model;
        this.manufacturer = manufacturer;
        this.androidVersion = androidVersion;
        this.apiLevel = apiLevel;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.batteryLevel = batteryLevel;
        this.networkType = networkType;
        this.simOperator = simOperator;
        this.phoneNumber = phoneNumber;
    }
}
