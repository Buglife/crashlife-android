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

final class PostEventSerializer {
    private int mStackFramePayloadCounter = 0;
    @NonNull private final Event mEvent;

    PostEventSerializer(@NonNull Event event) {
        mEvent = event;
    }

    JSONObject generateJSON() {
        JSONObject result = new JSONObject();

        JsonUtils.tryPut(result, "uuid", mEvent.getUuid());
        JsonUtils.tryPut(result, "occurred_at", mEvent.getTimestamp());

        //HACK: check if the message is a tombstone. If so, say so.
        String message = mEvent.getMessage();
        if (message != null) {
            if (message.startsWith("*** *** *** *** *** *** *** ***")) {
                JsonUtils.tryPut(result, "android_tombstone", message);
            } else {
                //note: this was missing, previously.
                JsonUtils.tryPut(result, "message", message);
            }
        }

        // Collect the exceptions
        JSONArray exceptionsJson = new JSONArray();

        List<ExceptionData> exceptionDatas = mEvent.getExceptionDatas();
        if (exceptionDatas != null) {
            for (ExceptionData exceptionData : exceptionDatas) {
                JSONObject exceptionJson = new JSONObject();

                JsonUtils.tryPut(exceptionJson, "name", exceptionData.getExceptionClass());
                JsonUtils.tryPut(exceptionJson, "message", exceptionData.getMessage());

                JSONArray stackframesJSON = new JSONArray();

                for (StackFrame stackFrame : exceptionData.getStackframes()) {
                    final int payloadId = getNextStackframePayloadId();
                    JSONObject stackframeJson = stackFrame.toApiJson(payloadId);
                    stackframesJSON.put(stackframeJson);
                }

                JsonUtils.tryPut(exceptionJson, "stack_frames", stackframesJSON);
                exceptionsJson.put(exceptionJson);
            }
        }

        JSONArray threadsJson = new JSONArray();

        List<ThreadData> threadDatas = mEvent.getThreadDatas();
        if (threadDatas != null) {
            for (ThreadData threadData : threadDatas) {
                JSONObject threadJson = new JSONObject();

                JsonUtils.tryPut(threadJson, "thread_id", threadData.getId());
                JsonUtils.tryPut(threadJson, "name", threadData.getName());

                JSONArray stackframesJson = new JSONArray();

                for (StackFrame stackFrame : threadData.getStackframes()) {
                    final int payloadId = getNextStackframePayloadId();
                    JSONObject stackframeJson = stackFrame.toApiJson(payloadId);
                    stackframesJson.put(stackframeJson);
                }

                JsonUtils.tryPut(threadJson, "stack_frames", stackframesJson);
                threadsJson.put(threadJson);
            }
        }

        JSONArray footprintsJson = JsonUtils.listToCacheJson(mEvent.getFootprints());

        AttributeMap attributeMap = mEvent.getAttributeMap();
        JSONArray attributeMapJson = attributeMap.toCacheJson();

        JsonUtils.tryPut(result, "attributes", attributeMapJson);
        JsonUtils.tryPut(result, "footprints", footprintsJson);
        JsonUtils.tryPut(result, "exceptions", exceptionsJson);
        JsonUtils.tryPut(result, "threads", threadsJson);
        return result;
    }

    private int getNextStackframePayloadId() {
        mStackFramePayloadCounter += 1;
        return mStackFramePayloadCounter;
    }
}
