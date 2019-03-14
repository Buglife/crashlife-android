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
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents a collection of custom attributes set by a user.
 */
final class AttributeMap implements Parcelable {
    private final HashMap<String, Attribute> mAttributes;

    AttributeMap() {
        mAttributes = new HashMap<>();
    }

    AttributeMap(AttributeMap attributeMap) {
        mAttributes = new HashMap<>(attributeMap.mAttributes);
    }

    AttributeMap(Parcel source) {
        mAttributes = new HashMap<>();

        final int size = source.readInt();

        for (int i = 0; i < size; i++) {
            String key = source.readString();
            Attribute attr = source.readParcelable(Attribute.class.getClassLoader());
            mAttributes.put(key, attr);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        final int size = mAttributes.size();
        dest.writeInt(size);

        if (size > 0) {
            for (Map.Entry<String, Attribute> entry : mAttributes.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeParcelable(entry.getValue(), 0);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<AttributeMap> CREATOR = new Parcelable.Creator<AttributeMap>() {
        @Override
        public AttributeMap createFromParcel(Parcel in) {
            return new AttributeMap(in);
        }

        @Override
        public AttributeMap[] newArray(int size) {
            return new AttributeMap[size];
        }
    };


    void put(@NonNull String key, @Nullable Attribute value) {
        mAttributes.put(key, value);
    }

    void putAll(@NonNull AttributeMap other) {
        mAttributes.putAll(other.mAttributes);
    }

    @Nullable Attribute get(@NonNull String key) {
        return mAttributes.get(key);
    }

    void clear() {
        mAttributes.clear();
    }

    Set<Map.Entry<String, Attribute>> entrySet() {
        return mAttributes.entrySet();
    }

    @NonNull
    JSONArray toCacheJson() {
        JSONArray attributesJSON = new JSONArray();
        for (String key : mAttributes.keySet()) {
            Attribute attr = mAttributes.get(key);
            if (attr != null) {
                JSONObject attrDict = attr.toCacheJson();
                JSONObject fullDict = new JSONObject();
                JsonUtils.tryPut(fullDict, "key", key);
                //add the entries of attrDict to fullDict
                Iterator it = attrDict.keys();
                while (it.hasNext()) {
                    String attrKey = (String) it.next();
                    JsonUtils.safePut(fullDict, attrKey, JsonUtils.safeGetString(attrDict, attrKey));
                }
                attributesJSON.put(fullDict);
            }
        }
        return attributesJSON;
    }

    @NonNull static AttributeMap fromCacheJson(JSONArray jsonArray) {
        AttributeMap attributes = new AttributeMap();

        if (jsonArray != null) {

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject fullDict = jsonArray.optJSONObject(i);
                if (fullDict != null) {
                    String key = fullDict.optString("key");
                    if (key == null) {
                        continue;
                    }
                    //create a shallow copy, remove the "key" entry, and create an attribute
                    JSONObject attrDict = new JSONObject();
                    for (Iterator<String> iterator = fullDict.keys(); iterator.hasNext(); ) {
                        String attrPartKey = iterator.next();
                        String value = fullDict.optString(attrPartKey);
                        JsonUtils.safePut(attrDict, attrPartKey, value);
                    }
                    attrDict.remove("key");
                    Attribute attr = Attribute.fromCacheJson(attrDict);
                    attributes.put(key, attr);
                }
            }
        }
        return attributes;
    }
}
