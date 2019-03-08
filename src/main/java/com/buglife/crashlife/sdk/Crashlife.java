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
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class Crashlife {
    @Nullable @SuppressLint("StaticFieldLeak")
    private static Client sClient = null;

    private Crashlife() {}

    public static void initWithApiKey(@NonNull Context context, @NonNull String apiKey) {
        boolean alreadyStarted = false;
        synchronized (Crashlife.class) {
            if (sClient == null) {
                sClient = new Client(context, apiKey);
            } else {
                alreadyStarted = true;
            }
        }
        if (alreadyStarted) {
            Log.e("You have attempted to start Crashlife twice. This is being ignored, but probably indicates an error in your application.");
        } else {
            sClient.postCachedEvents();
        }
    }

    static Client getClient() {
        if (sClient == null) {
            Log.e("Crashlife has not been initialized. Please call Crashlife.initWithApiKey(context, \"YOUR_API_KEY_HERE\") before using Crashlife APIs.");
        }
        return sClient;
    }

    public static void logException(Throwable exception) {
        if (getClient() != null) {
            getClient().logException(exception);
        }
    }

    public static void logError(String message) {
        if (getClient() != null) {
            getClient().logError(message);
        }
    }

    public static void logWarning(String message) {
        if (getClient() != null) {
            getClient().logWarning(message);
        }
    }

    public static void logInfo(String message) {
        if (getClient() != null) {
            getClient().logInfo(message);
        }
    }

    public static void log(Event.Severity severity, String message) {
        if (getClient() != null) {
            getClient().log(severity, message);
        }
    }
    public static void setUserIdentifier(@NonNull String identifier) {
        if (getClient() != null) {
            getClient().setUserIdentifier(identifier);
        }
    }
    public static String getUserIdentifier() {
        if (getClient() != null) {
            return getClient().getUserIdentifier();
        }
        return "";
    }

    public static void putAttribute(String attributeName, String attributeValue) {
        if (getClient() != null) {
            getClient().putAttribute(attributeName, attributeValue);
        }
    }

    public static void leaveFootprint(String name, Map<String, String> metadata) {
        if (getClient() != null) {
            getClient().leaveFootprint(name, metadata);
        }
    }
    public static void leaveFootprint(String name) {
        if (getClient() != null) {
            getClient().leaveFootprint(name, new HashMap<String, String>());
        }
    }


    @SuppressWarnings("WeakerAccess")
    public static class CrashlifeException extends RuntimeException {
        public CrashlifeException(String message) {
            super(message);
        }
    }

}