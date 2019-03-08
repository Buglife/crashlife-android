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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class Footprint implements Parcelable, JSONCaching {
    private final String mName;
    private final long mTimeStamp;
    private final AttributeMap mMetadata;

    Footprint(String name, AttributeMap metadata) {
        mName = name;
        mTimeStamp = System.currentTimeMillis();
        mMetadata = metadata;
    }
    private Footprint(String name, AttributeMap metadata, long timeStamp) {
        mName = name;
        mTimeStamp = timeStamp;
        mMetadata = metadata;
    }

    @SuppressWarnings("WeakerAccess")
    Footprint(Parcel source) {
        mName = source.readString();
        mTimeStamp = source.readLong();
        mMetadata = source.readParcelable(AttributeMap.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeLong(mTimeStamp);
        dest.writeParcelable(mMetadata, 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Footprint> CREATOR = new Creator<Footprint>() {
        @Override
        public Footprint createFromParcel(Parcel in) {
            return new Footprint(in);
        }

        @Override
        public Footprint[] newArray(int size) {
            return new Footprint[size];
        }
    };


    @NonNull
    public JSONObject toCacheJson() {
        JSONObject result = new JSONObject();

        JsonUtils.safePut(result,"name", mName);
        JsonUtils.safePut(result,"metadata", mMetadata.toCacheJson());
        JsonUtils.safePut(result,"timestamp", mTimeStamp);
        return result;
    }
    @NonNull
    private static Footprint fromCacheJson(JSONObject jsonObject) {
        String name;
        long timestamp;
        AttributeMap attributeMap = null;

        name = JsonUtils.safeGetString(jsonObject,"name");
        timestamp = JsonUtils.safeGetLong(jsonObject,"timestamp");
        JSONObject attributeMapJson = JsonUtils.safeGetJSONObject(jsonObject,"metadata");
        if (attributeMapJson != null) {
            attributeMap = AttributeMap.fromCacheJson(attributeMapJson);
        }

        return new Footprint(name, attributeMap, timestamp);
    }

    @NonNull
    static List<Footprint> listFromCacheJson(JSONArray jsonArray) {
        ArrayList<Footprint> result = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject;

            try {
                jsonObject = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }

            Footprint footprint = fromCacheJson(jsonObject);
            result.add(footprint);
        }

        return result;
    }

}
