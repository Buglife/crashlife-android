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

final class StackFrame implements JSONCaching {
    private static final String FILE = "file";
    private static final String LINE_NUMBER = "line_number";
    private static final String CLASS_NAME = "class_name";
    private static final String METHOD_NAME = "method_name";
    private static final String IS_NATIVE = "is_native";
    private static final String IS_EXTERNAL = "is_external";
    @Nullable private final String mFileName;
    private final int mLineNumber;
    @Nullable private final String mClassName;
    @Nullable private final String mMethodName;
    private final boolean mIsNativeMethod;

    private StackFrame(@Nullable String fileName, int lineNumber, @Nullable String className, @Nullable String methodName, boolean isNativeMethod) {
        mFileName = fileName;
        mLineNumber = lineNumber;
        mClassName = className;
        mMethodName = methodName;
        mIsNativeMethod = isNativeMethod;
    }

    private StackFrame(@NonNull StackTraceElement stackTraceElement) {
        mFileName = stackTraceElement.getFileName();
        mLineNumber = stackTraceElement.getLineNumber();
        mClassName = stackTraceElement.getClassName();
        mMethodName = stackTraceElement.getMethodName();
        mIsNativeMethod = stackTraceElement.isNativeMethod();
    }

    static List<StackFrame> stackFrames(StackTraceElement[] stackTraceElements) {
        ArrayList<StackFrame> result = new ArrayList<>();

        for (StackTraceElement stackTraceElement : stackTraceElements) {
            StackFrame stackFrame = new StackFrame(stackTraceElement);
            result.add(stackFrame);
        }

        return result;
    }

    private String getFileName() {
        return mFileName;
    }

    private int getLineNumber() {
        return mLineNumber;
    }

    private String getClassName() {
        return mClassName;
    }

    private String getMethodName() {
        return mMethodName;
    }

    private boolean isNativeMethod() {
        return mIsNativeMethod;
    }

    private static String getAppPackageName() {
        return Crashlife.getClient().getContext().getPackageName();
    }

    private boolean isExternal() {
        String packageName = StackFrame.getAppPackageName();
        String[] packageComponents = packageName.split("\\.");
        String className = getClassName();
        if (className == null) {
            return true;
        }
        String[] frameClassComponents = className.split("\\.");
        if (frameClassComponents.length < 2 || packageComponents.length < 2) {
            return true; //probably dealing with C++
        }
        int countSame = 0;
        for (int i = 0; i < frameClassComponents.length && i < packageComponents.length; i++) {
            if (frameClassComponents[i].equals(packageComponents[i])) {
                countSame++;
            }
            else {
                break;
            }
        }
        if (countSame >= 2) {
            return false;
        }
        return true;
    }

    @NonNull
    JSONObject toApiJson(int payloadNumber) {
        JSONObject result = toCacheJson();

        try {
            result.put("payload_id", payloadNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    @NonNull
    public JSONObject toCacheJson() {
        JSONObject result = new JSONObject();


        JsonUtils.safePut(result, FILE, getFileName());
        JsonUtils.safePut(result, LINE_NUMBER, getLineNumber());
        JsonUtils.safePut(result, CLASS_NAME, getClassName());
        JsonUtils.safePut(result, METHOD_NAME, getMethodName());
        JsonUtils.safePut(result, IS_NATIVE, isNativeMethod());
        JsonUtils.safePut(result, IS_EXTERNAL, isExternal());
        return result;
    }

    @NonNull
    private static StackFrame fromCacheJson(JSONObject jsonObject) {
        String fileName;
        int lineNumber;
        String className;
        String methodName;
        boolean isNativeMethod;
        boolean isExternal;
         fileName = JsonUtils.safeGetString(jsonObject, FILE);
         lineNumber = JsonUtils.safeGetInt(jsonObject, LINE_NUMBER);
         className = JsonUtils.safeGetString(jsonObject, CLASS_NAME);
         methodName = JsonUtils.safeGetString(jsonObject, METHOD_NAME);
         isNativeMethod = JsonUtils.safeGetBoolean(jsonObject, IS_NATIVE);
         // we don't need to read is_external. That's computed at cache/submission time.

        return new StackFrame(fileName, lineNumber, className, methodName, isNativeMethod);
    }

    @NonNull
    static List<StackFrame> listFromCacheJson(JSONArray jsonArray) {
        ArrayList<StackFrame> result = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject;

            try {
                 jsonObject = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }

            StackFrame stackFrame = fromCacheJson(jsonObject);
            result.add(stackFrame);
        }

        return result;
    }
}