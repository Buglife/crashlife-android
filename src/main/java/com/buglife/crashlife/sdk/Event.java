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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

class Event {

    enum Severity {
        INFO,
        WARNING,
        ERROR,
        CRASH
    }

    @NonNull private final String mUuid;
    @Nullable private final List<ThreadData> mThreadDatas;
    @Nullable private final List<ExceptionData> mExceptionDatas;
    @Nullable private final ThreadData mCrashingThread;
    @Nullable private final String mMessage;
    @NonNull private final AttributeMap mAttributeMap;
    @NonNull private final List<Footprint> mFootprints;
    @Nullable private final Severity mSeverity;
    @NonNull private Date mTimestamp;

    private Event(@NonNull String uuid,
                  @Nullable List<ThreadData> threadDatas,
                  @Nullable List<ExceptionData> exceptionDatas,
                  @Nullable ThreadData crashingThread,
                  @Nullable String message,
                  @NonNull AttributeMap attributeMap,
                  @NonNull List<Footprint> footprints,
                  @Nullable Severity severity,
                  @NonNull Date timestamp) {
        mUuid = uuid;
        mThreadDatas = threadDatas;
        mExceptionDatas = exceptionDatas;
        mCrashingThread = crashingThread;
        mMessage = message;
        mAttributeMap = attributeMap;
        mFootprints = footprints;
        mSeverity = severity;
        mTimestamp = timestamp;
    }

    Event(@NonNull Throwable exception, @NonNull AttributeMap attributeMap, @NonNull List<Footprint> footprints) {
        mUuid = generateUuid();
        mExceptionDatas = new ArrayList<>();
        mExceptionDatas.add(new ExceptionData(exception));
        mThreadDatas = ThreadData.threadDatas(Thread.getAllStackTraces());
        mCrashingThread = null;
        mMessage = null;
        mSeverity = Severity.ERROR;
        mAttributeMap = attributeMap;
        mFootprints = footprints;
        mTimestamp = new Date();
    }
    Event(@NonNull Severity severity, @NonNull String message, @NonNull AttributeMap attributeMap, List<Footprint> footprints) {
        this(generateUuid(), new ArrayList<ThreadData>(), new ArrayList<ExceptionData>(), null, message, attributeMap, footprints, severity, new Date());
    }
    private Event(Severity severity, String tombstone, AttributeMap attributeMap, List<Footprint> footprints, String uuid) {
        this(uuid, new ArrayList<ThreadData>(), new ArrayList<ExceptionData>(), null, tombstone,  attributeMap, footprints, severity, new Date());
    }


    // Take this as a parameter because we want to grab the stack trace with as few Crashlife frames as possible.
    Event(Throwable originalException,
          Map<Thread, StackTraceElement[]> allThreadStackTraces,
          Thread crashedThread,
          @NonNull AttributeMap attributeMap,
          @NonNull List<Footprint> footprints) {
        mUuid = generateUuid();
        mThreadDatas = ThreadData.threadDatas(allThreadStackTraces);
        mExceptionDatas = ExceptionData.exceptionDatas(originalException);
        List<StackFrame> stackFrames = StackFrame.stackFrames(crashedThread.getStackTrace());
        mCrashingThread = new ThreadData(crashedThread, stackFrames);
        mMessage = originalException.getMessage();
        mAttributeMap = attributeMap;
        mFootprints = footprints;
        mSeverity = Severity.CRASH;
        mTimestamp = new Date();
    }

    public static Event info(@NonNull String message, @NonNull AttributeMap attributeMap, List<Footprint> footprints) {
        return new Event(Severity.INFO, message, attributeMap, footprints);
    }

    public static Event warning(@NonNull String message, @NonNull AttributeMap attributeMap, List<Footprint> footprints) {
        return new Event(Severity.WARNING, message, attributeMap, footprints);
    }

    public static Event error(@NonNull String message, @NonNull AttributeMap attributeMap, List<Footprint> footprints) {
        return new Event(Severity.ERROR, message, attributeMap, footprints);
    }
    static Event crash(@NonNull String tombstone, @NonNull AttributeMap attributeMap, List<Footprint> footprints, String uuid) {
        return new Event(Severity.CRASH, tombstone, attributeMap, footprints, uuid);
    }

    @NonNull String getUuid() {
        return mUuid;
    }

    @Nullable List<ThreadData> getThreadDatas() {
        return mThreadDatas;
    }

    @Nullable List<ExceptionData> getExceptionDatas() {
        return mExceptionDatas;
    }

    @Nullable ThreadData getCrashingThread() {
        return mCrashingThread;
    }

    @Nullable String getMessage() {
        return mMessage;
    }

    @NonNull AttributeMap getAttributeMap() {
        return mAttributeMap;
    }

    @Nullable Severity getSeverity() {
        return mSeverity;
    }

    Date getTimestamp() {
        return mTimestamp;
    }

    void setTimestamp(Date timestamp) {
        mTimestamp = timestamp;
    }

    @NonNull List<Footprint> getFootprints() { return mFootprints; }

    private static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    @NonNull
    synchronized JSONObject toCacheJson() {
        JSONObject result = new JSONObject();

        JsonUtils.safePut(result,"uuid", mUuid);
        if (mThreadDatas != null) {
            JsonUtils.safePut(result, "thread_datas", JsonUtils.listToCacheJson(mThreadDatas));
        }
        if (mExceptionDatas != null) {
            JsonUtils.safePut(result, "exception_datas", JsonUtils.listToCacheJson(mExceptionDatas));
        }
        if (mCrashingThread != null) {
            JsonUtils.safePut(result, "crashing_thread", mCrashingThread.toCacheJson());
        }
        JsonUtils.safePut(result,"message", mMessage);
        JsonUtils.safePut(result,"attributes_map", mAttributeMap.toCacheJson());
        JsonUtils.safePut(result,"footprints", JsonUtils.listToCacheJson(mFootprints));
        if (mSeverity != null) {
            JsonUtils.safePut(result, "severity_ordinal", mSeverity.ordinal());
        }
        JsonUtils.safePut(result, "occurred_at", sdf.format(mTimestamp));
        return result;
    }
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZ", Locale.US);
    @NonNull
    static synchronized Event fromCacheJson(JSONObject jsonObject) {
        String uuid;
        List<ThreadData> threadDatas = new ArrayList<>();
        List<ExceptionData> exceptionDatas = new ArrayList<>();
        ThreadData crashingThread = null;
        String message;
        AttributeMap attributeMap = new AttributeMap();
        List<Footprint> footprints = new ArrayList<>();
        int severityOrdinal = -1;
        Date timestamp;

        uuid = JsonUtils.safeGetString(jsonObject,"uuid");
        if (uuid == null) {
            uuid = generateUuid(); // better to fake one than break things.
        }

        JSONArray threadDatasJson = JsonUtils.safeGetJSONArray(jsonObject,"thread_datas");
        if (threadDatasJson != null) {
            threadDatas = ThreadData.listFromCacheJson(threadDatasJson);
        }

        JSONArray exceptionDatasJson = JsonUtils.safeGetJSONArray(jsonObject,"exception_datas");
        if (exceptionDatasJson != null) {
            exceptionDatas = ExceptionData.listFromCacheJson(exceptionDatasJson);
        }

        JSONObject crashingThreadJson = JsonUtils.safeGetJSONObject(jsonObject,"crashing_thread");
        if (crashingThreadJson != null) {
            crashingThread = ThreadData.fromCacheJson(crashingThreadJson);
        }

        message = JsonUtils.safeGetString(jsonObject,"message");

        JSONArray attributeMapJson = JsonUtils.safeGetJSONArray(jsonObject,"attributes_map");
        if (attributeMapJson != null) {
            attributeMap = AttributeMap.fromCacheJson(attributeMapJson);
        }

        JSONArray footprintsJson = JsonUtils.safeGetJSONArray(jsonObject,"footprints");
        if (footprintsJson != null) {
            footprints = Footprint.listFromCacheJson(footprintsJson);
        }

        try {
            severityOrdinal = jsonObject.getInt("severity_ordinal");
        } catch (JSONException e) {
            // this one in try/catch to maintain the non-0 default value
            // but don't spew about it.
        }

        Severity severity = null;
        if (severityOrdinal >= 0) {
            severity = Severity.values()[severityOrdinal];
        }

        try {
            timestamp = sdf.parse(JsonUtils.safeGetString(jsonObject, "occurred_at"));
        } catch (ParseException e) {
            e.printStackTrace();
            timestamp = new Date();
        }

        return new Event(uuid, threadDatas, exceptionDatas, crashingThread, message, attributeMap, footprints, severity, timestamp);
    }
}
