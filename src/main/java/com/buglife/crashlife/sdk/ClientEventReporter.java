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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public final class ClientEventReporter {
    private static ClientEventReporter sInstance;
    private final Context mContext;


    ClientEventReporter(Context context) {
        mContext = context;
    }
    public static synchronized ClientEventReporter getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ClientEventReporter(context);
        }

        return sInstance;

    }

    public void reportClientEvent(final String eventName, String apiKey)
    {
        JSONObject clientEventParams = new JSONObject();
        DeviceSnapshot deviceSnapshot = new DeviceSnapshot(mContext);
        SessionSnapshot sessionSnapshot = new SessionSnapshot(mContext, Crashlife.getUserIdentifier());
        try {
            if (deviceSnapshot.getDeviceIdentifier() != null) {
                clientEventParams.put("device_identifier", deviceSnapshot.getDeviceIdentifier());
            }
            clientEventParams.put("sdk_version", sessionSnapshot.getSDKVersion());
            clientEventParams.put("sdk_name", sessionSnapshot.getSDKName());
            clientEventParams.put("event_name", eventName);
            clientEventParams.put("bundle_short_version", sessionSnapshot.getBundleShortVersion());
            clientEventParams.put("bundle_version", sessionSnapshot.getBundleVersion());
            if (sessionSnapshot.getUserIdentifier() != null) {
                clientEventParams.put("user_identifier", sessionSnapshot.getUserIdentifier());
            }
        }
        catch (JSONException e) {
            //figure out what to do here
            e.printStackTrace();
        }

        Submitter submitter = new Submitter();
        submitter.submitClientEvent(apiKey, sessionSnapshot, clientEventParams, new Runnable() {
            @Override
            public void run() {
                Log.d("Error submitting client event: " + eventName);
            }
        }, new Runnable() {
            @Override
            public void run() {
                Log.d("Client Event posted successfully: " + eventName);
            }
        });
    }
}
