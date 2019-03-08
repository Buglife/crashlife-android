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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

final class Attribute implements Parcelable {
    private String mValue;
    final static int FLAG_CUSTOM     = 1;
    final static int FLAG_SYSTEM     = 1 << 1;
    @SuppressWarnings("WeakerAccess")
    final static int FLAG_PUBLIC     = 1 << 2;
    @SuppressWarnings("WeakerAccess")
    final static int FLAG_INTERNAL   = 1 << 3;

    private String getNameForFlag(int flag) {
        switch (flag) {
            case FLAG_CUSTOM:
                return null;
            case FLAG_SYSTEM:
                return "system";
            case FLAG_PUBLIC:
                return "public";
            case FLAG_INTERNAL:
                return "internal";
            default:
                return Integer.toString(flag);
        }
    }
    enum ValueType {
        STRING(0),
        INT(1),
        FLOAT(2),
        BOOL(3);
        private int mValue;
        ValueType(int value) {
            mValue = value;
        }
        @SuppressLint("UseSparseArrays")
        private static final Map<Integer, ValueType> map = new HashMap<>();
        static {
            for (ValueType valueType : ValueType.values()) {
                map.put(valueType.mValue, valueType);
            }
        }
        public static ValueType valueOf(int typeValue) {
            return map.get(typeValue);
        }

        public int getValue() {
            return mValue;
        }

    }
    private final ValueType mValueType;
    private final int mFlags;

    Attribute(String value, ValueType valueType, int flags) {
        mValue = value;
        mValueType = valueType;
        mFlags = flags;
    }

    // valueType should really be removed on both platforms,
    // or other constructors added here, but as long as we're using the Buglife backend, no rush

    Attribute(Attribute other) {
        mValue = other.mValue;
        mValueType = other.mValueType;
        mFlags = other.mFlags;
    }
    @SuppressWarnings("WeakerAccess")
    Attribute(Parcel source) {
        mValue = source.readString();
        mValueType = ValueType.valueOf(source.readInt());
        mFlags = source.readInt();
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mValue);
        dest.writeInt(mValueType.getValue());
        dest.writeInt(mFlags);
    }
    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Attribute> CREATOR = new Creator<Attribute>() {
        @Override
        public Attribute createFromParcel(Parcel in) {
            return new Attribute(in);
        }

        @Override
        public Attribute[] newArray(int size) {
            return new Attribute[size];
        }
    };
    @SuppressWarnings("WeakerAccess")
    String getValue() {
        return mValue;
    }
    public void setValue(String value) {
        mValue = value;
    }
    public ValueType getValueType() {
        return mValueType;
    }
    @SuppressWarnings("WeakerAccess")
    int getFlags() {
        return mFlags;
    }

    @NonNull
    JSONObject toCacheJson() {
        JSONObject dict = new JSONObject();
        JsonUtils.tryPut(dict,"value", getValue());
//        JsonUtils.tryPut(dict,"value_type", getValueType().getValue());
        JsonUtils.tryPut(dict,"flags", getNameForFlag(getFlags()));
        return dict;
    }

    @Nullable
    static Attribute fromCacheJson(JSONObject jsonObject) {
        String value;
        ValueType valueType;
        int flags;
        try {
            value = jsonObject.getString("value");
            valueType = ValueType.STRING; // everything is a string now
            flags = jsonObject.getInt("flags");
        } catch (JSONException e) {
            //Is this really useful?
            e.printStackTrace();
            return null;
        }
        return new Attribute(value, valueType, flags);
    }

}
