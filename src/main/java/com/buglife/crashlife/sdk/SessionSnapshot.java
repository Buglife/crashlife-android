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
    private static final String PLATFORM = "platform";
    private static final String SDK_VERSION = "sdk_version";
    private static final String SDK_NAME = "sdk_name";
    private static final String USER_IDENTIFIER = "user_identifier";
    private static final String BUNDLE_IDENTIFIER = "bundle_identifier";
    private static final String BUNDLE_NAME = "bundle_name";
    private static final String BUNDLE_SHORT_VERSION = "bundle_short_version";
    private static final String BUNDLE_VERSION = "bundle_version";
    private static final String IS_DEBUG = "is_debug";
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

        JsonUtils.safePut(result, PLATFORM, getPlatform());
        JsonUtils.safePut(result, SDK_VERSION, getSDKVersion());
        JsonUtils.safePut(result, SDK_NAME, getSDKName());
        JsonUtils.safePut(result, USER_IDENTIFIER, getUserIdentifier());
        JsonUtils.safePut(result, BUNDLE_IDENTIFIER, getBundleIdentifier());
        JsonUtils.safePut(result, BUNDLE_NAME, getBundleName());
        JsonUtils.safePut(result, BUNDLE_SHORT_VERSION, getBundleShortVersion());
        JsonUtils.safePut(result, BUNDLE_VERSION, getBundleVersion());
        JsonUtils.safePut(result, IS_DEBUG, getDebugBuild());
        return result;
    }

    @NonNull static SessionSnapshot fromCacheJson(JSONObject jsonObject) {
        String platform = JsonUtils.safeGetString(jsonObject, PLATFORM);
        String sdkVersion = JsonUtils.safeGetString(jsonObject, SDK_VERSION);
        String sdkName = JsonUtils.safeGetString(jsonObject, SDK_NAME);
        String userIdentifier = JsonUtils.safeGetString(jsonObject, USER_IDENTIFIER);
        String bundleIdentifier = JsonUtils.safeGetString(jsonObject, BUNDLE_IDENTIFIER);
        String bundleName = JsonUtils.safeGetString(jsonObject, BUNDLE_NAME);
        String bundleShortVersion = JsonUtils.safeGetString(jsonObject, BUNDLE_SHORT_VERSION);
        String bundleVersion = JsonUtils.safeGetString(jsonObject, BUNDLE_VERSION);
        boolean debugBuild = JsonUtils.safeGetBoolean(jsonObject, IS_DEBUG);

        return new SessionSnapshot(platform, sdkVersion, sdkName, userIdentifier,
                bundleIdentifier, bundleName, bundleShortVersion, bundleVersion, debugBuild);
    }

}
