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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a thread at the time that a given event is captured.
 */
final class ThreadData implements JSONCaching {
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String STACK_FRAMES = "stack_frames";
    private static final String PRIORITY = "priority";
    private static final String STATE_ORDINAL = "state_ordinal";
    private static final String IS_ALIVE = "is_alive";
    private static final String IS_DAEMON = "is_daemon";
    private static final String IS_INTERRUPTED = "is_interrupted";
    private final long mId;
    @Nullable private final String mName;
    @NonNull private final List<StackFrame> mStackframes;
    private final int mPriority;
    @Nullable private final Thread.State mState;
    private final boolean mIsAlive;
    private final boolean mIsDaemon;
    private final boolean mIsInterrupted;

    ThreadData(@NonNull Thread thread, @NonNull List<StackFrame> stackFrames) {
        mId = thread.getId();
        mName = thread.getName();
        mPriority = thread.getPriority();
        mState = thread.getState();
        mIsAlive = thread.isAlive();
        mIsDaemon = thread.isDaemon();
        mIsInterrupted = thread.isInterrupted();
        mStackframes = stackFrames;
    }

    private ThreadData(long id,
                       @Nullable String name,
                       @NonNull List<StackFrame> stackFrames,
                       int priority,
                       @Nullable Thread.State state,
                       boolean isAlive,
                       boolean isDaemon,
                       boolean isInterrupted) {
        mId = id;
        mName = name;
        mStackframes = stackFrames;
        mState = state;
        mPriority = priority;
        mIsAlive = isAlive;
        mIsDaemon = isDaemon;
        mIsInterrupted = isInterrupted;
    }

    static List<ThreadData> threadDatas(Map<Thread, StackTraceElement[]> threadStackTraces) {
        ArrayList<ThreadData> result = new ArrayList<>();

        for (Map.Entry<Thread, StackTraceElement[]> entry : threadStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTraceElements = entry.getValue();
            List<StackFrame> stackFrames = StackFrame.stackFrames(stackTraceElements);
            ThreadData threadData = new ThreadData(thread, stackFrames);
            result.add(threadData);
        }

        return result;
    }

    long getId() {
        return mId;
    }

    @Nullable String getName() {
        return mName;
    }

    @NonNull
    List<StackFrame> getStackframes() {
        return mStackframes;
    }

    @NonNull
    public JSONObject toCacheJson() {
        JSONObject result = new JSONObject();

        JsonUtils.safePut(result, ID, mId);
        JsonUtils.safePut(result, NAME, mName);
        JsonUtils.safePut(result, STACK_FRAMES, JsonUtils.listToCacheJson(mStackframes));
        JsonUtils.safePut(result, PRIORITY, mPriority);

        int state = 0;
        if (mState != null) {
            state = mState.ordinal();
        }
        JsonUtils.safePut(result, STATE_ORDINAL, state);
        JsonUtils.safePut(result, IS_ALIVE, mIsAlive);
        JsonUtils.safePut(result, IS_DAEMON, mIsDaemon);
        JsonUtils.safePut(result, IS_INTERRUPTED, mIsInterrupted);
        return result;
    }

    @NonNull
    static ThreadData fromCacheJson(JSONObject jsonObject) {
        long id;
        String name;
        List<StackFrame> stackFrames = new ArrayList<>();
        int priority;
        int stateOrdinal = -1;
        boolean isAlive = true;
        boolean isDaemon;
        boolean isInterrupted;

        id = JsonUtils.safeGetLong(jsonObject, ID);
        name = JsonUtils.safeGetString(jsonObject, NAME);
        JSONArray stackframesJson = JsonUtils.safeGetJSONArray(jsonObject, STACK_FRAMES);
        if (stackframesJson != null) {
            stackFrames = StackFrame.listFromCacheJson(stackframesJson);
        }
        priority = JsonUtils.safeGetInt(jsonObject, PRIORITY);
        isDaemon = JsonUtils.safeGetBoolean(jsonObject, IS_DAEMON);
        isInterrupted = JsonUtils.safeGetBoolean(jsonObject, IS_INTERRUPTED);

        try {
            stateOrdinal = jsonObject.getInt(STATE_ORDINAL);
            isAlive = jsonObject.getBoolean(IS_ALIVE); // do these two ourselves to keep initial value
        } catch (JSONException e) {
            // Nothing to do here
        }

        Thread.State state = null;

        if (stateOrdinal >= 0) {
            state = Thread.State.values()[stateOrdinal];
        }

        return new ThreadData(id, name, stackFrames, priority, state, isAlive, isDaemon, isInterrupted);
    }

    @NonNull
    static List<ThreadData> listFromCacheJson(JSONArray jsonArray) {
        ArrayList<ThreadData> result = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject;

            try {
                jsonObject = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }

            ThreadData threadData = fromCacheJson(jsonObject);
            result.add(threadData);
        }

        return result;
    }
}
