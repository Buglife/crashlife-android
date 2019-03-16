/*
 * Copyright (C) 2019 Buglife, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.buglife.crashlife.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

public final class DeviceSnapshot implements Parcelable {

    private static final String OS_VERSION = "operating_system_version";
    private static final String DEVICE_MANUFACTURER = "device_manufacturer";
    private static final String DEVICE_MODEL = "device_model";
    private static final String DEVICE_BRAND = "device_brand";
    private static final String DEVICE_IDENTIFIER = "device_identifier";
    private final String mOSVersion;
    private final String mDeviceManufacturer;
    private final String mDeviceModel;
    private final String mDeviceBrand;
    @Nullable private final String mDeviceIdentifier;

    @SuppressLint("HardwareIds")
    public DeviceSnapshot(Context context) {
        mOSVersion = Build.VERSION.RELEASE;
        mDeviceManufacturer = Build.MANUFACTURER;
        mDeviceModel = Build.MODEL;
        mDeviceBrand = Build.BRAND;
        mDeviceIdentifier = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private DeviceSnapshot(String osVersion, String deviceManufacturer, String deviceModel, String deviceBrand, @Nullable String deviceIdentifier) {
        mOSVersion = osVersion;
        mDeviceManufacturer = deviceIdentifier;
        mDeviceModel = deviceModel;
        mDeviceBrand = deviceBrand;
        mDeviceIdentifier = deviceIdentifier;
    }

    private String getOSVersion() {
        return mOSVersion;
    }

    private String getDeviceManufacturer() {
        return mDeviceManufacturer;
    }

    private String getDeviceModel() {
        return mDeviceModel;
    }

    private String getDeviceBrand() {
        return mDeviceBrand;
    }

    @Nullable
    String getDeviceIdentifier() {
        return mDeviceIdentifier;
    }

    /* Parcelable */

    @SuppressWarnings("WeakerAccess")
    DeviceSnapshot(Parcel in) {
        mOSVersion = in.readString();
        mDeviceManufacturer = in.readString();
        mDeviceModel = in.readString();
        mDeviceBrand = in.readString();
        mDeviceIdentifier = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mOSVersion);
        dest.writeString(mDeviceManufacturer);
        dest.writeString(mDeviceModel);
        dest.writeString(mDeviceBrand);
        dest.writeString(mDeviceIdentifier);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DeviceSnapshot> CREATOR = new Creator<DeviceSnapshot>() {
        @Override
        public DeviceSnapshot createFromParcel(Parcel in) {
            return new DeviceSnapshot(in);
        }

        @Override
        public DeviceSnapshot[] newArray(int size) {
            return new DeviceSnapshot[size];
        }
    };

    @NonNull
    JSONObject toCacheJson() {
        JSONObject result = new JSONObject();

        JsonUtils.safePut(result, OS_VERSION, getOSVersion());
        JsonUtils.safePut(result, DEVICE_MANUFACTURER, getDeviceManufacturer());
        JsonUtils.safePut(result, DEVICE_MODEL, getDeviceModel());
        JsonUtils.safePut(result, DEVICE_BRAND, getDeviceBrand());
        JsonUtils.safePut(result, DEVICE_IDENTIFIER, getDeviceIdentifier());
        return result;
    }

    @NonNull static DeviceSnapshot fromCacheJson(JSONObject jsonObject) {
        String osVersion = JsonUtils.safeGetString(jsonObject, OS_VERSION);
        String deviceManufacturer = JsonUtils.safeGetString(jsonObject, DEVICE_MANUFACTURER);
        String deviceModel = JsonUtils.safeGetString(jsonObject, DEVICE_MODEL);
        String deviceBrand = JsonUtils.safeGetString(jsonObject, DEVICE_BRAND);
        String deviceIdentifier = JsonUtils.safeGetString(jsonObject, DEVICE_IDENTIFIER);

        return new DeviceSnapshot(osVersion, deviceManufacturer, deviceModel, deviceBrand, deviceIdentifier);
    }

}
