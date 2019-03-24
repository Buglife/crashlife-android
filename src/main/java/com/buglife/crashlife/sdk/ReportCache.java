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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class ReportCache {

    static final String ATTRIBUTES_FILE_SUFFIX = "-attributes.json";
    static final String FOOTPRINTS_FILE_SUFFIX = "-footprints.json";
    static final String SNAPSHOTS_FILE_SUFFIX  = "-snapshots.json";
    static final String SESSION_FILE_SUFFIX = "-session.json";

    private final File mCachedReportsDirectory;
    private final File mCachedNativeReportsDirectory;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    ReportCache(Context context) {
        File reportDir = new File(context.getFilesDir(), "crashlife_events");
        File nativeReportDir = new File(context.getFilesDir(), "crashlife_native");

        if (!reportDir.exists()) {
            reportDir.mkdir();
        }
        if (!nativeReportDir.exists()) {
            nativeReportDir.mkdir();
        }

        mCachedReportsDirectory = reportDir;
        mCachedNativeReportsDirectory = nativeReportDir;
    }

    String getReportsPath() {
        return mCachedReportsDirectory.getPath();
    }

    String getNativeReportsPath() {
        return mCachedNativeReportsDirectory.getPath();
    }

    void deleteCachedReports(List<Event> successfullyPostedEvents, List<Event> successfullyPostedNativeEvents) {
        for (Event e : successfullyPostedEvents) {
            long timestamp = e.getTimestamp().getTime();
            File cacheFile = new File(mCachedReportsDirectory, String.valueOf(timestamp) + ".json");
            // Two posts in short order with additional log events could cause overlap.
            if (cacheFile.exists() && !cacheFile.delete()) {
                Log.w("Unable to delete posted JVM crash file: " + String.valueOf(timestamp));
            }
        }
        for (Event e : successfullyPostedNativeEvents) {
            String uuid = e.getUuid();
            File cacheFile = new File(mCachedNativeReportsDirectory, uuid + ".txt");
            File attributesFile = new File(mCachedNativeReportsDirectory, uuid + ATTRIBUTES_FILE_SUFFIX);
            File footprintsFile = new File(mCachedNativeReportsDirectory, uuid + FOOTPRINTS_FILE_SUFFIX);
            File snapshotsFile = new File(mCachedNativeReportsDirectory, uuid + SNAPSHOTS_FILE_SUFFIX);
            File sessionFile = new File(mCachedNativeReportsDirectory, uuid + SESSION_FILE_SUFFIX);
            if (cacheFile.exists() && !(cacheFile.delete() && attributesFile.delete() && footprintsFile.delete()
                    && snapshotsFile.delete() && sessionFile.delete())) {
                Log.e("Unable to delete posted native crash file: " + uuid);
            }
        }
    }

    void cacheEvent(Event event) {
        JSONObject eventJson = event.toCacheJson();
        String eventString = eventJson.toString();
        String filename = System.currentTimeMillis() + ".json";
        File file = new File(mCachedReportsDirectory, filename);

        try {
            IOUtils.writeStringToFile(eventString, file);
        } catch (IOException e) {
            Log.e("Unable to cache logged Crashlife event. Please contact support@buglife.com if you see this message.");
            e.printStackTrace();
        }
    }

    List<Event> getCachedNativeEvents() {
        ArrayList<Event> events = new ArrayList<>();
        File[] cachedEvents = mCachedNativeReportsDirectory.listFiles();

        for (File cachedEvent : cachedEvents) {
            if (!cachedEvent.getName().endsWith(".txt")) {
                //Filter out all the auxiliary files (footprints and attributes), let's get only the crashes here.
                continue;
            }
            String cachedEventString;
            try {
                cachedEventString = IOUtils.readStringFromFile(cachedEvent);
            } catch (IOException e) {
                Log.w("Failed to load Crashlife cached native event from disk. It may have been deleted.");
                e.printStackTrace();
                continue;
            }

            String uuid = cachedEvent.getName().replace(".txt", "");

            File crashFolder = cachedEvent.getParentFile();

            // We could put the Snapshots into the attributes map here instead of after loading the attribute map.
            // We have decided not to, but if that causes problems down the line, this issue can be revisited.
            File attributesFile = new File(crashFolder, uuid + ATTRIBUTES_FILE_SUFFIX);
            AttributeMap attributeMap;
            try {
                String jsonString = IOUtils.readStringFromFile(attributesFile);
                JSONArray attributeMapJson = new JSONArray(jsonString);
                attributeMap = AttributeMap.fromCacheJson(attributeMapJson);
            } catch (Exception e) {
                // There may not be any attributes, so don't scream about it. No e.printStacktrace()!
                attributeMap = new AttributeMap();
            }

            File footprintsFile = new File(crashFolder, uuid + FOOTPRINTS_FILE_SUFFIX);
            List<Footprint> footprints;
            try {
                String jsonString = IOUtils.readStringFromFile(footprintsFile);
                JSONArray footprintsJson = new JSONArray(jsonString);
                footprints = Footprint.listFromCacheJson(footprintsJson);
            } catch (Exception e) {
                //There may not be any footprints. don't scream into the void about it.
                // finally, something useful!
                footprints = new ArrayList<>();
            }

            //OK, a bit more work to do then bed.
            //1. Load the environment and device snapshots from the snapshots file
            //2. Load the session snapshot from the session file
            //3. Add all 3 to the attribute map before putting it into the new Event
            File snapshotsFile = new File(crashFolder, uuid + SNAPSHOTS_FILE_SUFFIX);
            try {
                String jsonString = IOUtils.readStringFromFile(snapshotsFile);
                JSONObject snapshots = new JSONObject(jsonString);
                JSONObject environmentSnapshot = snapshots.optJSONObject("environment_snapshot");
                JSONObject deviceSnapshot = snapshots.optJSONObject("device_snapshot");
                JSONArray libFileIds = snapshots.optJSONArray("lib_file_ids");
                if (environmentSnapshot != null) {
                    attributeMap.putAll(JsonUtils.systemAttributesFromJsonObject(environmentSnapshot));
                }
                if (deviceSnapshot != null) {
                    attributeMap.putAll(JsonUtils.systemAttributesFromJsonObject(deviceSnapshot));
                }
                if (libFileIds != null) {
                    String yuuuuck = libFileIds.toString();
                    Attribute attr = new Attribute(yuuuuck, Attribute.ValueType.STRING, Attribute.FLAG_INTERNAL);
                    attributeMap.put("embedded_libs", attr);
                }
            } catch (Exception e) {
                e.printStackTrace(); // :/ not sure that's right at this hour.
            }
            File sessionFile = new File(crashFolder, uuid + SESSION_FILE_SUFFIX);
            try {
                String jsonString = IOUtils.readStringFromFile(sessionFile);
                JSONObject session = new JSONObject(jsonString);
                attributeMap.putAll(JsonUtils.systemAttributesFromJsonObject(session));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // So now the Event being created from the .crash static method has all the properties that the events created
            // by getCachedEvents() would. The only difference is at what time they get added to the JSON.
            // In the JVM case, it's at CrashCatcher.uncaughtException().
            // In the native case, it's here, because we can't alter the crash report getting saved out
            Event event = Event.crash(cachedEventString, attributeMap, footprints, cachedEvent.getName().replace(".txt", "").toLowerCase());
            long timestamp = cachedEvent.lastModified();
            event.setTimestamp(new Date(timestamp));
            events.add(event);
        }
        return events;
    }
    List<Event> getCachedEvents() {
        ArrayList<Event> events = new ArrayList<>();
        File[] cachedEvents = mCachedReportsDirectory.listFiles();

        for (File cachedEvent : cachedEvents) {
            String cachedEventString;
            try {
                cachedEventString = IOUtils.readStringFromFile(cachedEvent);
            } catch (IOException e) {
                Log.e("Unable to read cached Crashlife event from file.");
                e.printStackTrace();
                continue;
            }

            JSONObject cachedEventJson;

            try {
                cachedEventJson = new JSONObject(cachedEventString);
            } catch (JSONException e) {
                Log.e("Crashlife event was not a valid JSON object. If you're seeing this error, please contact support@buglife.com");
                e.printStackTrace();
                continue;
            }

            Event event = Event.fromCacheJson(cachedEventJson);
            event.setTimestamp(new Date(Long.parseLong(cachedEvent.getName().replace(".json", ""))));
            events.add(event);
        }
        return events;
    }

    void deleteAllCachedReports() {
        deleteCachedReports(getCachedEvents(), getCachedNativeEvents());
    }
}
