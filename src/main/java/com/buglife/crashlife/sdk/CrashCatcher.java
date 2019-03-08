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
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Map;

final class CrashCatcher implements Thread.UncaughtExceptionHandler {
    private final Context mContext;
    private final ReportCache mReportCache;
    private final Thread.UncaughtExceptionHandler mPreviousUncaughtExceptionHandler;

    CrashCatcher(@NonNull Context context, @NonNull ReportCache reportCache) {
        mContext = context;
        mReportCache = reportCache;
        mPreviousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        Log.e("Unhandled exception:" + throwable.getMessage() + "\n stack trace:\n" + Arrays.toString(thread.getStackTrace()));
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();

        AttributeMap attributeMap = Crashlife.getClient().getAttributes();
        DeviceSnapshot deviceSnapshot = new DeviceSnapshot(mContext);
        EnvironmentSnapshot environmentSnapshot = new EnvironmentSnapshot(mContext);
        SessionSnapshot sessionSnapshot = new SessionSnapshot(mContext, Crashlife.getUserIdentifier());
        attributeMap.putAll(JsonUtils.systemAttributesFromJsonObject(deviceSnapshot.toCacheJson()));
        attributeMap.putAll(JsonUtils.systemAttributesFromJsonObject(environmentSnapshot.toCacheJson()));
        attributeMap.putAll(JsonUtils.systemAttributesFromJsonObject(sessionSnapshot.toCacheJson()));
        Event crash = new Event(throwable, allStackTraces, thread, Crashlife.getClient().getAttributes(), Crashlife.getClient().getFootprints());
        mReportCache.cacheEvent(crash);

        if (mPreviousUncaughtExceptionHandler != null) {
            mPreviousUncaughtExceptionHandler.uncaughtException(thread, throwable);
        }
        else {
            System.exit(1);
        }

    }
}
