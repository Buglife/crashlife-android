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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

final class Submitter {
    Submitter() {
    }

    void submitEvents(@NonNull String apiKey, @NonNull SessionSnapshot currentSession, @NonNull List<Event> events, @NonNull Runnable success, @NonNull Runnable failure) {
        JSONObject params = startingParams(apiKey, currentSession);

        JsonUtils.tryPut(params, "occurrences", JsonUtils.listToCacheJson(events));

        String paramString = params.toString();
        String endpoint = "/api/v1/events.json";
        AsyncHttpTask task = new AsyncHttpTask(paramString, success, failure, endpoint);
        task.execute();
    }

    void submitClientEvent(@NonNull String apiKey, @NonNull SessionSnapshot currentSession, @NonNull JSONObject clientEventParams, @NonNull Runnable success, @NonNull Runnable failure) {
        JSONObject params = startingParams(apiKey, currentSession);

        JsonUtils.tryPut(params, "client_event", clientEventParams);

        String paramString = params.toString();
        String endpoint = "/api/v1/client_events.json";
        AsyncHttpTask task = new AsyncHttpTask(paramString, success, failure, endpoint);
        task.execute();
    }

    private JSONObject startingParams(@NonNull String apiKey, @NonNull SessionSnapshot currentSession) {
        JSONObject params = new JSONObject();
        JsonUtils.tryPut(params, "api_key", apiKey);

        JSONObject appJson = currentSession.toCacheJson();
        JsonUtils.tryPut(params, "app", appJson);
        return params;
    }
}
