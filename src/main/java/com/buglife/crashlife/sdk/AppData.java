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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

@SuppressWarnings("SameReturnValue")
final class AppData {
    @Nullable private final String mBundleIdentifier;
    @Nullable private final String mBundleName;
    @Nullable private final String mBundleShortVersion; // TODO: Move this into Events
    @Nullable private final String mBundleVersion;      // TODO: Move this into Events

    AppData(Context context) {
        mBundleIdentifier = context.getPackageName();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        mBundleName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);

        PackageInfo packageInfo = null;

        try {
            packageInfo = context.getPackageManager().getPackageInfo(mBundleIdentifier, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (packageInfo != null) {
            mBundleVersion = Integer.toString(packageInfo.versionCode);
            mBundleShortVersion = packageInfo.versionName;
        } else {
            mBundleVersion = null;
            mBundleShortVersion = null;
        }
    }

    @SuppressWarnings("SameReturnValue")
    @NonNull private String getSdkName() {
        return "Crashlife Android";
    }

    @NonNull private String getPlatform() {
        return "android";
    }

    JSONObject toJSON() {
        JSONObject result = new JSONObject();
        JsonUtils.tryPut(result, "platform", getPlatform());
        JsonUtils.tryPut(result, "sdk_name", getSdkName());
        JsonUtils.tryPut(result, "bundle_identifier", mBundleIdentifier);
        JsonUtils.tryPut(result, "bundle_name", mBundleName);
        JsonUtils.tryPut(result, "bundle_short_version", mBundleShortVersion);
        JsonUtils.tryPut(result, "bundle_version", mBundleVersion);
        return result;
    }
}
