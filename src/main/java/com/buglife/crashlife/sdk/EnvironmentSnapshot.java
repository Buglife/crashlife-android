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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

/**
 * Represents a "snapshot" of the current environment the moment that the bug reporter is invoked.
 */
public final class EnvironmentSnapshot implements Parcelable {
    private static final String BATTERY_LEVEL = "battery_level";
    private static final String FREE_MEMORY_BYTES = "free_memory_bytes";
    private static final String TOTAL_MEMORY_BYTES = "total_memory_bytes";
    private static final String FREE_CAPACITY_BYTES = "free_capacity_bytes";
    private static final String TOTAL_CAPACITY_BYTES = "total_capacity_bytes";
    private static final String TOTAL_CAPACITY_BYTES1 = "total_capacity_bytes";
    private static final String CARRIER_NAME = "carrier_name";
    private static final String ANDROID_MOBILE_NETWORK_SUBTYPE = "android_mobile_network_subtype";
    private static final String WIFI_CONNECTED = "wifi_connected";
    private static final String LOCALE = "locale";
    private static final String LOCATION = "location";

    private final float mBatteryLevel;
    private final long mFreeMemoryBytes;
    private final long mTotalMemoryBytes;
    private final long mFreeCapacityBytes;
    private final long mTotalCapacityBytes;
    @Nullable private final String mCarrierName;
    private final int mMobileNetworkSubtype;
    private final boolean mWifiConnected;
    @Nullable private final String mLocale;
    @Nullable private final Location mLocation;

    public EnvironmentSnapshot(Context mContext) {
        mBatteryLevel = EnvironmentUtils.getBatteryLevel(mContext);
        ActivityManager.MemoryInfo memoryInfo = EnvironmentUtils.getMemoryInfo(mContext);
        mFreeMemoryBytes = memoryInfo.availMem;
        mTotalMemoryBytes = memoryInfo.totalMem;

        StatFs externalStats = new StatFs(Environment.getExternalStorageDirectory().getPath());
        mTotalCapacityBytes = externalStats.getBlockSizeLong() * externalStats.getBlockCountLong();
        mFreeCapacityBytes = externalStats.getBlockSizeLong() * externalStats.getAvailableBlocksLong();

        Connectivity connectivity = new Connectivity.Builder(mContext).build();
        mCarrierName = connectivity.getCarrierName();
        mMobileNetworkSubtype = connectivity.getMobileConnectionSubtype();
        mWifiConnected = connectivity.isConnectedToWiFi();
        mLocale = EnvironmentUtils.getLocale(mContext).toString();

        mLocation = fetchLocation(mContext);

    }

    private EnvironmentSnapshot(float mBatteryLevel, long mFreeMemoryBytes, long mTotalMemoryBytes,
                                long mFreeCapacityBytes, long mTotalCapacityBytes, @Nullable String mCarrierName,
                                int mMobileNetworkSubtype, boolean mWifiConnected, @Nullable String mLocale, @Nullable Location mLocation) {
        this.mBatteryLevel = mBatteryLevel;
        this.mFreeMemoryBytes = mFreeMemoryBytes;
        this.mTotalMemoryBytes = mTotalMemoryBytes;
        this.mFreeCapacityBytes = mFreeCapacityBytes;
        this.mTotalCapacityBytes = mTotalCapacityBytes;
        this.mCarrierName = mCarrierName;
        this.mMobileNetworkSubtype = mMobileNetworkSubtype;
        this.mWifiConnected = mWifiConnected;
        this.mLocale = mLocale;
        this.mLocation = mLocation;
    }

    @SuppressLint("MissingPermission")
    private static Location fetchLocation(Context mContext) {
        LocationManager locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        Location tempLocation = null;
        String[] fineLoc = {Manifest.permission.ACCESS_FINE_LOCATION};
        if (ActivityUtils.arePermissionsGranted(mContext, fineLoc)) {
            tempLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (tempLocation == null) {
                // Fall back to network provider
                tempLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }
        else {
            String[] coarseLoc = {Manifest.permission.ACCESS_COARSE_LOCATION};
            if (ActivityUtils.arePermissionsGranted(mContext, coarseLoc)) {
                tempLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }
        return tempLocation;
    }

    private float getBatteryLevel() {
        return mBatteryLevel;
    }

    private long getFreeMemoryBytes() {
        return mFreeMemoryBytes;
    }

    private long getTotalMemoryBytes() {
        return mTotalMemoryBytes;
    }

    private long getFreeCapacityBytes() {
        return mFreeCapacityBytes;
    }

    private long getTotalCapacityBytes() {
        return mTotalCapacityBytes;
    }

    @Nullable
    private String getCarrierName() {
        return mCarrierName;
    }

    private int getMobileNetworkSubtype() {
        return mMobileNetworkSubtype;
    }

    private boolean getWifiConnected() {
        return mWifiConnected;
    }

    @Nullable
    private String getLocale() {
        return mLocale;
    }

    @Nullable
    private Location getLocation() {
        return mLocation;
    }

    /* Parcelable */

    @SuppressWarnings("WeakerAccess")
    EnvironmentSnapshot(Parcel in) {
        mBatteryLevel = in.readFloat();
        mFreeMemoryBytes = in.readLong();
        mTotalMemoryBytes = in.readLong();
        mFreeCapacityBytes = in.readLong();
        mTotalCapacityBytes = in.readLong();
        mCarrierName = in.readString();
        mMobileNetworkSubtype = in.readInt();
        mWifiConnected = in.readByte() != 0;
        mLocale = in.readString();
        mLocation = in.readParcelable(Location.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(mBatteryLevel);
        dest.writeLong(mFreeMemoryBytes);
        dest.writeLong(mTotalMemoryBytes);
        dest.writeLong(mFreeCapacityBytes);
        dest.writeLong(mTotalCapacityBytes);
        dest.writeString(mCarrierName);
        dest.writeInt(mMobileNetworkSubtype);
        dest.writeByte((byte) (mWifiConnected ? 1 : 0));
        dest.writeString(mLocale);
        dest.writeParcelable(mLocation, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<EnvironmentSnapshot> CREATOR = new Creator<EnvironmentSnapshot>() {
        @Override
        public EnvironmentSnapshot createFromParcel(Parcel in) {
            return new EnvironmentSnapshot(in);
        }

        @Override
        public EnvironmentSnapshot[] newArray(int size) {
            return new EnvironmentSnapshot[size];
        }
    };


    @NonNull
    JSONObject toCacheJson() {
        JSONObject result = new JSONObject();

        JsonUtils.safePut(result, BATTERY_LEVEL, getBatteryLevel());
        JsonUtils.safePut(result, FREE_MEMORY_BYTES, getFreeMemoryBytes());
        JsonUtils.safePut(result, TOTAL_MEMORY_BYTES, getTotalMemoryBytes());
        JsonUtils.safePut(result, FREE_CAPACITY_BYTES, getFreeCapacityBytes());
        JsonUtils.safePut(result, TOTAL_CAPACITY_BYTES, getTotalCapacityBytes());
        JsonUtils.safePut(result, CARRIER_NAME, getCarrierName());
        JsonUtils.safePut(result, ANDROID_MOBILE_NETWORK_SUBTYPE, getMobileNetworkSubtype());
        JsonUtils.safePut(result, WIFI_CONNECTED, getWifiConnected());
        JsonUtils.safePut(result, LOCALE, getLocale());
        if (getLocation() != null) {
            JsonUtils.safePut(result, LOCATION, "" + getLocation().getLatitude() + "," + getLocation().getLongitude());
        }
        return result;
    }

    @NonNull static EnvironmentSnapshot fromCacheJson(JSONObject jsonObject) {
        float batteryLevel = (float) JsonUtils.safeGetDouble(jsonObject,BATTERY_LEVEL);
        long freeMemoryBytes= JsonUtils.safeGetLong(jsonObject, FREE_MEMORY_BYTES);
        long totalMemoryBytes= JsonUtils.safeGetLong(jsonObject, TOTAL_MEMORY_BYTES);
        long freeCapacityBytes= JsonUtils.safeGetLong(jsonObject, FREE_CAPACITY_BYTES);
        long totalCapacityBytes = JsonUtils.safeGetLong(jsonObject, TOTAL_CAPACITY_BYTES1);
        String carrierName= JsonUtils.safeGetString(jsonObject, CARRIER_NAME);
        int androidMobileNetworkSubtype = JsonUtils.safeGetInt(jsonObject, ANDROID_MOBILE_NETWORK_SUBTYPE);
        boolean wifiConnected = JsonUtils.safeGetBoolean(jsonObject, WIFI_CONNECTED);
        String locale = JsonUtils.safeGetString(jsonObject, LOCALE);
        String latlon = JsonUtils.safeGetString(jsonObject, LOCATION);
        Location location = null;
        if (latlon != null) {
            String lat_lon[] = latlon.split(",");
            double latitude = Double.parseDouble(lat_lon[0]);
            double longitude = Double.parseDouble(lat_lon[1]);
            location = new Location(""); // No provider needed when we'll just overwrite
            location.setLatitude(latitude);
            location.setLongitude(longitude);
        }

        return new EnvironmentSnapshot(batteryLevel, freeMemoryBytes, totalMemoryBytes, freeCapacityBytes,
                totalCapacityBytes, carrierName, androidMobileNetworkSubtype, wifiConnected, locale, location);
    }

}
