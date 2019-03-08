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

final class ExceptionData implements JSONCaching {
    private final String mMessage;
    private final String mLocalizedMessage;
    private final String mExceptionClass;
    private final List<StackFrame> mStackframes;

    ExceptionData(@NonNull Throwable throwable) {
        mMessage = throwable.getMessage();
        mLocalizedMessage = throwable.getLocalizedMessage();
        mExceptionClass = throwable.getClass().getName();
        mStackframes = StackFrame.stackFrames(throwable.getStackTrace());
    }

    private ExceptionData(String message, String localizedMessage, String exceptionClass, List<StackFrame> stackFrames) {
        mMessage = message;
        mLocalizedMessage = localizedMessage;
        mExceptionClass = exceptionClass;
        mStackframes = stackFrames;
    }

    static List<ExceptionData> exceptionDatas(Throwable originalException) {
        ArrayList<ExceptionData> result = new ArrayList<>();
        result.add(new ExceptionData(originalException));
        Throwable causalChainCursor = originalException.getCause();

        while (causalChainCursor != null) {
            result.add(new ExceptionData(causalChainCursor));
            causalChainCursor = causalChainCursor.getCause();
        }

        return result;
    }

    @Nullable String getMessage() {
        return mMessage;
    }

    @Nullable
    private String getLocalizedMessage() {
        return mLocalizedMessage;
    }

    @Nullable String getExceptionClass() {
        return mExceptionClass;
    }

    @NonNull List<StackFrame> getStackframes() {
        return mStackframes;
    }

    @NonNull
    public JSONObject toCacheJson() {
        JSONObject result = new JSONObject();

        JsonUtils.safePut(result,"message", getMessage());
        JsonUtils.safePut(result,"localized_message", getLocalizedMessage());
        JsonUtils.safePut(result,"exception_class", getExceptionClass());
        JsonUtils.safePut(result,"stack_frames", JsonUtils.listToCacheJson(mStackframes));
        return result;
    }

    @NonNull
    private static ExceptionData fromCacheJson(JSONObject jsonObject) {
        String message;
        String localizedMessage;
        String exceptionClass;
        List<StackFrame> stackFrames = new ArrayList<>();

        message = JsonUtils.safeGetString(jsonObject,"message");
        localizedMessage = JsonUtils.safeGetString(jsonObject,"localized_message");
        exceptionClass = JsonUtils.safeGetString(jsonObject,"exception_class");

        JSONArray stackframesJson = JsonUtils.safeGetJSONArray(jsonObject,"stack_frames");
        if (stackframesJson != null) {
            stackFrames = StackFrame.listFromCacheJson(stackframesJson);
        }

        return new ExceptionData(message, localizedMessage, exceptionClass, stackFrames);
    }

    @NonNull
    static List<ExceptionData> listFromCacheJson(JSONArray jsonArray) {
        ArrayList<ExceptionData> result = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject;

            try {
                jsonObject = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }

            ExceptionData exceptionData = fromCacheJson(jsonObject);
            result.add(exceptionData);
        }

        return result;
    }
}
