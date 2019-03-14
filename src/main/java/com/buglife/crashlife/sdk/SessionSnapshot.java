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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

@SuppressWarnings("WeakerAccess")
public class SessionSnapshot implements Parcelable {
    private final String mPlatform;
    private final String mSDKVersion;
    private final String mSDKName;
    private final String mUserIdentifier;
    private final String mBundleIdentifier;
    private final String mBundleName;
    @Nullable private final String mBundleShortVersion;
    @Nullable private final String mBundleVersion;
    private final boolean mDebugBuild;

    public SessionSnapshot(Context context, String userIdentifier) {
        mPlatform = "android";
        mSDKVersion = com.buglife.crashlife.sdk.BuildConfig.VERSION_NAME;
        mSDKName = "Crashlife Android";
        mUserIdentifier = userIdentifier;
        mBundleIdentifier = context.getPackageName();

        ApplicationInfo applicationInfo = context.getApplicationInfo();
        mDebugBuild = ((applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        int stringId = applicationInfo.labelRes;
        mBundleName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);

        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(mBundleIdentifier, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Unable to get version information / package information", e);
        }

        if (packageInfo != null) {
            mBundleVersion = Integer.toString(packageInfo.versionCode);
            mBundleShortVersion = packageInfo.versionName;
        } else {
            mBundleVersion = null;
            mBundleShortVersion = null;
        }
    }

    private SessionSnapshot(String mPlatform, String mSDKVersion, String mSDKName, String mUserIdentifier,
                            String mBundleIdentifier, String mBundleName, @Nullable String mBundleShortVersion,
                            @Nullable String mBundleVersion, boolean mDebugBuild) {
        this.mPlatform = mPlatform;
        this.mSDKVersion = mSDKVersion;
        this.mSDKName = mSDKName;
        this.mUserIdentifier = mUserIdentifier;
        this.mBundleIdentifier = mBundleIdentifier;
        this.mBundleName = mBundleName;
        this.mBundleShortVersion = mBundleShortVersion;
        this.mBundleVersion = mBundleVersion;
        this.mDebugBuild = mDebugBuild;
    }

    private String getPlatform() {
        return mPlatform;
    }

    String getSDKVersion() {
        return mSDKVersion;
    }

    String getSDKName() {
        return mSDKName;
    }

    String getUserIdentifier() {
        return mUserIdentifier;
    }

    @Nullable
    String getBundleShortVersion() {
        return mBundleShortVersion;
    }

    @Nullable
    String getBundleVersion() {
        return mBundleVersion;
    }

    private String getBundleIdentifier() {
        return mBundleIdentifier;
    }

    private String getBundleName() {
        return mBundleName;
    }

    private boolean getDebugBuild() { return mDebugBuild; }

    /* Parcelable */

    SessionSnapshot(Parcel in) {
        mPlatform = in.readString();
        mSDKVersion = in.readString();
        mSDKName = in.readString();
        mUserIdentifier = in.readString();
        mBundleShortVersion = in.readString();
        mBundleVersion = in.readString();
        mBundleIdentifier = in.readString();
        mBundleName = in.readString();
        mDebugBuild = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPlatform);
        dest.writeString(mSDKVersion);
        dest.writeString(mSDKName);
        dest.writeString(mUserIdentifier);
        dest.writeString(mBundleShortVersion);
        dest.writeString(mBundleVersion);
        dest.writeString(mBundleIdentifier);
        dest.writeString(mBundleName);
        dest.writeInt(mDebugBuild ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SessionSnapshot> CREATOR = new Creator<SessionSnapshot>() {
        @Override
        public SessionSnapshot createFromParcel(Parcel in) {
            return new SessionSnapshot(in);
        }

        @Override
        public SessionSnapshot[] newArray(int size) {
            return new SessionSnapshot[size];
        }
    };
    @NonNull
    JSONObject toCacheJson() {
        JSONObject result = new JSONObject();

        JsonUtils.safePut(result,"platform", getPlatform());
        JsonUtils.safePut(result,"sdk_version", getSDKVersion());
        JsonUtils.safePut(result,"sdk_name", getSDKName());
        JsonUtils.safePut(result,"user_identifier", getUserIdentifier());
        JsonUtils.safePut(result,"bundle_identifier", getBundleIdentifier());
        JsonUtils.safePut(result,"bundle_name", getBundleName());
        JsonUtils.safePut(result,"bundle_short_version", getBundleShortVersion());
        JsonUtils.safePut(result,"bundle_version", getBundleVersion());
        JsonUtils.safePut(result,"is_debug", getDebugBuild());
        return result;
    }

    @NonNull static SessionSnapshot fromCacheJson(JSONObject jsonObject) {
        String platform = JsonUtils.safeGetString(jsonObject,"platform");
        String sdkVersion = JsonUtils.safeGetString(jsonObject,"sdk_version");
        String sdkName = JsonUtils.safeGetString(jsonObject,"sdk_name");
        String userIdentifier = JsonUtils.safeGetString(jsonObject,"user_identifier");
        String bundleIdentifier = JsonUtils.safeGetString(jsonObject,"bundle_identifier");
        String bundleName = JsonUtils.safeGetString(jsonObject,"bundle_name");
        String bundleShortVersion = JsonUtils.safeGetString(jsonObject,"bundle_short_version");
        String bundleVersion = JsonUtils.safeGetString(jsonObject,"bundle_version");
        boolean debugBuild = JsonUtils.safeGetBoolean(jsonObject,"is_debug");

        return new SessionSnapshot(platform, sdkVersion, sdkName, userIdentifier,
                bundleIdentifier, bundleName, bundleShortVersion, bundleVersion, debugBuild);
    }

}
