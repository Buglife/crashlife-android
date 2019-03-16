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

class Event implements JSONCaching {

    enum Severity {
        INFO,
        WARNING,
        ERROR,
        CRASH;
        static Severity fromLowercaseString(String sev) {
            switch (sev) {
                case "info":
                    return INFO;
                case "warning":
                    return WARNING;
                case "error":
                    return ERROR;
                case "crash":
                    return CRASH;
            }
            return null;
        }
    }

    private static final String UUID_FIELD = "uuid";
    private static final String THREADS = "threads";
    private static final String EXCEPTIONS = "exceptions";
    private static final String CRASHINGTHREAD = "crashing_thread";
    private static final String MESSAGE = "message";
    private static final String TOMBSTONE = "android_tombstone";
    private static final String ATTRIBUTEMAP = "attributes";
    private static final String FOOTPRINTS = "footprints";
    private static final String SEVERITY = "severity";
    private static final String TIMESTAMP = "occurred_at";

    @NonNull private final String mUuid;
    @Nullable private final List<ThreadData> mThreadDatas;
    @Nullable private final List<ExceptionData> mExceptionDatas;
    @Nullable private final ThreadData mCrashingThread;
    @Nullable private final String mMessage;
    @Nullable private final String mTombstone;
    @NonNull private final AttributeMap mAttributeMap;
    @NonNull private final List<Footprint> mFootprints;
    @Nullable private final Severity mSeverity;
    @NonNull private Date mTimestamp;

    private Event(@NonNull String uuid,
                  @Nullable List<ThreadData> threadDatas,
                  @Nullable List<ExceptionData> exceptionDatas,
                  @Nullable ThreadData crashingThread,
                  @Nullable String message,
                  @Nullable String tombstone,
                  @NonNull AttributeMap attributeMap,
                  @NonNull List<Footprint> footprints,
                  @Nullable Severity severity,
                  @NonNull Date timestamp) {
        mUuid = uuid;
        mThreadDatas = threadDatas;
        mExceptionDatas = exceptionDatas;
        mCrashingThread = crashingThread;
        mMessage = message;
        mTombstone = tombstone;
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
        mTombstone = null;
        mSeverity = Severity.ERROR;
        mAttributeMap = attributeMap;
        mFootprints = footprints;
        mTimestamp = new Date();
    }
    Event(@NonNull Severity severity, @NonNull String message, @NonNull AttributeMap attributeMap, List<Footprint> footprints) {
        this(generateUuid(), new ArrayList<ThreadData>(), new ArrayList<ExceptionData>(), null, message, null, attributeMap, footprints, severity, new Date());
    }
    private Event(Severity severity, String tombstone, AttributeMap attributeMap, List<Footprint> footprints, String uuid) {
        this(uuid, new ArrayList<ThreadData>(), new ArrayList<ExceptionData>(), null, null, tombstone, attributeMap, footprints, severity, new Date());
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
        mTombstone = null;
        mAttributeMap = attributeMap;
        mFootprints = footprints;
        mSeverity = Severity.CRASH;
        mTimestamp = new Date(0); // make it obvious we did something wrong, forgot to set the timestamp
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
    public synchronized JSONObject toCacheJson() {
        JSONObject result = new JSONObject();

        JsonUtils.safePut(result, UUID_FIELD, mUuid);
        if (mThreadDatas != null) {
            JsonUtils.safePut(result, THREADS, JsonUtils.listToCacheJson(mThreadDatas));
        }
        if (mExceptionDatas != null) {
            JsonUtils.safePut(result, EXCEPTIONS, JsonUtils.listToCacheJson(mExceptionDatas));
        }
        if (mCrashingThread != null) {
            JsonUtils.safePut(result, CRASHINGTHREAD, mCrashingThread.toCacheJson());
        }
        JsonUtils.safePut(result, MESSAGE, mMessage);
        JsonUtils.safePut(result, TOMBSTONE, mTombstone);
        JsonUtils.safePut(result, ATTRIBUTEMAP, mAttributeMap.toCacheJson());
        JsonUtils.safePut(result, FOOTPRINTS, JsonUtils.listToCacheJson(mFootprints));
        if (mSeverity != null) {
            JsonUtils.safePut(result, SEVERITY, mSeverity.toString().toLowerCase());
        }
        JsonUtils.safePut(result, TIMESTAMP, sdf.format(mTimestamp));
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
        String tombstone; // will likely stay null because we should only be in this method on a JVM crash
        AttributeMap attributeMap = new AttributeMap();
        List<Footprint> footprints = new ArrayList<>();
        String severityStr = "";
        Date timestamp;

        uuid = JsonUtils.safeGetString(jsonObject, UUID_FIELD);
        if (uuid == null) {
            uuid = generateUuid(); // better to fake one than break things.
        }

        JSONArray threadDatasJson = JsonUtils.safeGetJSONArray(jsonObject, THREADS);
        if (threadDatasJson != null) {
            threadDatas = ThreadData.listFromCacheJson(threadDatasJson);
        }

        JSONArray exceptionDatasJson = JsonUtils.safeGetJSONArray(jsonObject, EXCEPTIONS);
        if (exceptionDatasJson != null) {
            exceptionDatas = ExceptionData.listFromCacheJson(exceptionDatasJson);
        }

        JSONObject crashingThreadJson = JsonUtils.safeGetJSONObject(jsonObject, CRASHINGTHREAD);
        if (crashingThreadJson != null) {
            crashingThread = ThreadData.fromCacheJson(crashingThreadJson);
        }

        message = JsonUtils.safeGetString(jsonObject, MESSAGE);
        tombstone = JsonUtils.safeGetString(jsonObject, TOMBSTONE);

        JSONArray attributeMapJson = JsonUtils.safeGetJSONArray(jsonObject, ATTRIBUTEMAP);
        if (attributeMapJson != null) {
            attributeMap = AttributeMap.fromCacheJson(attributeMapJson);
        }

        JSONArray footprintsJson = JsonUtils.safeGetJSONArray(jsonObject, FOOTPRINTS);
        if (footprintsJson != null) {
            footprints = Footprint.listFromCacheJson(footprintsJson);
        }

        severityStr = JsonUtils.safeGetString(jsonObject, SEVERITY);
        Severity severity = null;
        if (severityStr != null) {
            severity = Severity.fromLowercaseString(severityStr);
        }

        try {
            timestamp = sdf.parse(JsonUtils.safeGetString(jsonObject, TIMESTAMP));
        } catch (ParseException e) {
            e.printStackTrace();
            timestamp = new Date();
        }

        return new Event(uuid, threadDatas, exceptionDatas, crashingThread, message, tombstone, attributeMap, footprints, severity, timestamp);
    }
}
