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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
final class JsonUtils {
    static void tryPut(JSONObject jsonObject, String key, JSONObject val) {
        try {
            jsonObject.put(key, val);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static void tryPut(JSONObject jsonObject, String key, String val) {
        try {
            jsonObject.put(key, val);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static void tryPut(JSONObject jsonObject, String key, JSONArray val) {
        try {
            jsonObject.put(key, val);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("WeakerAccess")
    static void tryPut(JSONObject jsonObject, String key, int val) {
        try {
            jsonObject.put(key, val);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static void tryPut(JSONObject jsonObject, String key, long val) {
        try {
            jsonObject.put(key, val);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    @SuppressWarnings("WeakerAccess")
    static void tryPut(JSONObject jsonObject, String key, boolean val) {
        try {
            jsonObject.put(key, val);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    static void tryPut(JSONObject jsonObject, String key, float val) {
        try {
            jsonObject.put(key, val);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static void safePut(JSONObject jsonObject, String key, JSONObject val) {
        if (val == null) { return; }
        tryPut(jsonObject, key, val);
    }
    static void safePut(JSONObject jsonObject, String key, String val) {
        if (val == null) { return; }
        tryPut(jsonObject, key, val);
    }
    static void safePut(JSONObject jsonObject, String key, JSONArray val) {
        if (val == null) { return; }
        tryPut(jsonObject, key, val);
    }
    static void safePut(JSONObject jsonObject, String key, int val) {
        tryPut(jsonObject, key, val);
    }
    static void safePut(JSONObject jsonObject, String key, long val) {
        tryPut(jsonObject, key, val);
    }
    static void safePut(JSONObject jsonObject, String key, boolean val) {
        tryPut(jsonObject, key, val);
    }
    public static void safePut(JSONObject jsonObject, String key, float val) {
        tryPut(jsonObject, key, val);
    }


    //NSDictionary this ain't.
    static JSONObject safeGetJSONObject(JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.getJSONObject(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    static String safeGetString(JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.getString(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // returns 0 by default
    static int safeGetInt(JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.getInt(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    // returns 0 by default
    static long safeGetLong(JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.getLong(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return 0L;
    }
    static JSONArray safeGetJSONArray(JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.getJSONArray(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // returns false by default
    static boolean safeGetBoolean(JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.getBoolean(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false; // hmmm, this doesn't seem quite right. Maybe delete the scalar methods here...
    }

    static double safeGetDouble(JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.getDouble(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return 0.0;
    }

    // Warning: only for use with non-compound types. This was written with the Snapshot classes in mind
    // use at your own risk.
    static AttributeMap systemAttributesFromJsonObject(JSONObject jsonObject) {
        // jsonObject keys -> attributeMap Keys
        // jsonObject values -> attribute values
        // SYSTEM -> attribute flag
        // ??? -> attribute value type
        AttributeMap out = new AttributeMap();
        Iterator<String> itr = jsonObject.keys();
        while (itr.hasNext()) {
            String key = itr.next();
            Object obj = jsonObject.opt(key);
            if (obj == null) {
                continue;
            }
            Attribute attribute = new Attribute(obj.toString(), Attribute.ValueType.STRING, Attribute.FLAG_SYSTEM);
            out.put(key, attribute);
        }
        return out;
    }

    static JSONArray listToCacheJson(List<? extends JSONCaching> objects) {
        JSONArray result = new JSONArray();

        for (JSONCaching cachingObj : objects) {
            JSONObject object = cachingObj.toCacheJson();
            result.put(object);
        }

        return result;

    }
}
