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

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ru.ivanarh.jndcrash.NDCrash;
import ru.ivanarh.jndcrash.NDCrashError;
import ru.ivanarh.jndcrash.NDCrashUnwinder;

import static ru.ivanarh.jndcrash.NDCrashError.ok;


final class Client {
    @NonNull
    private final Context mContext;
    @NonNull
    private final String mApiKey;
    @NonNull
    private final ReportCache mReportCache;
    @NonNull
    private final AttributeMap mAttributes;
    @NonNull
    private final SessionSnapshot mWorkingSessionSnapshot;
    @NonNull
    private final ArrayList<Footprint> mFootprints;
    @NonNull
    private final Handler mHandler;
    @NonNull
    private final File mFootprintsFile;
    @NonNull
    private final File mAttributesFile;
    @NonNull
    private final File mSessionSnapshotFile;
    @NonNull
    private String mUserIdentifier;
    @NonNull
    private final Object mAttributesAndFootprintsLock;
    @NonNull
    private final Object mUserIdentifierLock;

    Client(@NonNull Context context, @NonNull final String apiKey) {
        mReportCache = new ReportCache(context);
        CrashCatcher mCrashCatcher = new CrashCatcher(context, mReportCache);
        Thread.setDefaultUncaughtExceptionHandler(mCrashCatcher);

        mContext = context;
        mApiKey = apiKey;
        mAttributes = new AttributeMap();
        mWorkingSessionSnapshot = new SessionSnapshot(context, "");
        mFootprints = new ArrayList<>();
        mUserIdentifier = "";
        mAttributesAndFootprintsLock = new Object();
        mUserIdentifierLock = new Object();
        UUID fileName = UUID.randomUUID();
        NDCrashError error = NDCrash.initializeOutOfProcess(mContext, mReportCache.getNativeReportsPath() + "/" + fileName.toString() + ".txt", NDCrashUnwinder.libunwind, CrashService.class);
        if (error != ok) {
            Log.w("error initializing NDK Crash reporter" + error.toString());
        }
        mFootprintsFile = new File(mReportCache.getNativeReportsPath(), fileName.toString() + ReportCache.FOOTPRINTS_FILE_SUFFIX);
        mAttributesFile = new File(mReportCache.getNativeReportsPath(), fileName.toString() + ReportCache.ATTRIBUTES_FILE_SUFFIX);
        mSessionSnapshotFile = new File(mReportCache.getNativeReportsPath(), fileName.toString() + ReportCache.SESSION_FILE_SUFFIX);
        HandlerThread mPersisterThread = new HandlerThread("com.buglife.crashlife.persistence");
        mPersisterThread.start();
        mHandler = new Handler(mPersisterThread.getLooper());

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ClientEventReporter.getInstance(mContext).reportClientEvent("app_launch", apiKey);
            }
        }, 10000);

    }

    void postCachedEvents() {
        final List<Event> cachedEvents = mReportCache.getCachedEvents();
        final List<Event> cachedNativeEvents = mReportCache.getCachedNativeEvents();
        List<Event> allEvents = new ArrayList<>(cachedEvents);
        allEvents.addAll(cachedNativeEvents);
        Runnable deletionRunnable = new Runnable() {
            @Override
            public void run() {
                mReportCache.deleteCachedReports(cachedEvents, cachedNativeEvents);
            }
        };
        Runnable failure = new Runnable() {
            @Override
            public void run() {
                Log.e("Posting cached events failed, will try again on next launch");
            }
        };
        if (allEvents.size() > 0) {
            Submitter submitter = new Submitter();
            submitter.submitEvents(mApiKey, mWorkingSessionSnapshot, allEvents, deletionRunnable, failure);
        }
    }

    private void postEvent(@NonNull Event event) {
        Submitter submitter = new Submitter();
        final ArrayList<Event> events = new ArrayList<>();
        events.add(event);
        Runnable success = new Runnable() {
            @Override
            public void run() {
                mReportCache.deleteCachedReports(events, events); // Ugly. Should probably unify this with a "native" flag
            }
        };
        Runnable failure = new Runnable() {
            @Override
            public void run() {
                Log.e("Posting event failed, will try again on next launch");
            }
        };
        submitter.submitEvents(mApiKey, mWorkingSessionSnapshot, events, success, failure);

    }

    void deleteCachedEvents(List<Event> mEvents, List<Event> mNativeEvents) {
        mReportCache.deleteCachedReports(mEvents, mNativeEvents);
    }
    void deleteAllCachedEvents() {
        mReportCache.deleteAllCachedReports();
    }

    void logException(Throwable exception) {
        Event event;
        synchronized (mAttributesAndFootprintsLock) {
            event = new Event(exception, new AttributeMap(mAttributes), new ArrayList<>(mFootprints));
        }
        mReportCache.cacheEvent(event);
        postEvent(event);
    }

    void logError(String message) {
        log(Event.Severity.ERROR, message);
    }

    void logWarning(String message) {
        log(Event.Severity.WARNING, message);
    }

    void logInfo(String message) {
        log(Event.Severity.INFO, message);
    }

    void log(Event.Severity severity, String message) {
        Event event;
        synchronized (mAttributesAndFootprintsLock) {
            event = new Event(severity, message, new AttributeMap(mAttributes), new ArrayList<>(mFootprints));
        }
        mReportCache.cacheEvent(event);
        postEvent(event);
    }

    void setUserIdentifier(@NonNull String userIdentifier) {
        synchronized (mUserIdentifierLock) {
            mUserIdentifier = userIdentifier;
        }
        final SessionSnapshot snapshot = new SessionSnapshot(mContext, userIdentifier);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    IOUtils.writeStringToFile(snapshot.toCacheJson().toString(), mSessionSnapshotFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void putAttribute(String attributeName, String attributeValue) {
        final AttributeMap attributes;
        synchronized (mAttributesAndFootprintsLock) {
            _putAttribute_unsafe(attributeName, attributeValue);
            attributes = new AttributeMap(mAttributes);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    IOUtils.writeStringToFile(attributes.toCacheJson().toString(), mAttributesFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private void _putAttribute_unsafe(String attributeName, String attributeValue) {
        if (attributeValue == null) {
            mAttributes.put(attributeName, null);
        }
        mAttributes.put(attributeName, new Attribute(attributeValue, Attribute.ValueType.STRING, Attribute.FLAG_CUSTOM));

    }


    void leaveFootprint(String name, Map<String, String> metadata) {
        _leaveFootprint(name, metadata);
    }
    private void _leaveFootprint(String name, Map<String, String> metadata) {
        AttributeMap map = new AttributeMap();
        for (String key : metadata.keySet()) {
            Attribute attr = new Attribute(metadata.get(key), Attribute.ValueType.STRING, Attribute.FLAG_CUSTOM);
            map.put(key, attr);
        }
        Footprint toAdd = new Footprint(name, map);
        final List<Footprint> footprints;
        synchronized (mAttributesAndFootprintsLock) {
            leaveFootprint_unsafe(toAdd);
            footprints = new ArrayList<>(mFootprints);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    IOUtils.writeStringToFile(JsonUtils.listToCacheJson(footprints).toString(), mFootprintsFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void leaveFootprint_unsafe(Footprint footprint) {
        mFootprints.add(footprint);
    }

    // This is for use by the crash service. You're probably doing it wrong if you're using it otherwise.
    AttributeMap getAttributes() {
        synchronized (mAttributesAndFootprintsLock) {
            return mAttributes;
        }
    }

    String getAPIKey() {
        return mApiKey;
    }

    Context getContext() { return mContext; }

    List<Footprint> getFootprints() {
        synchronized (mAttributesAndFootprintsLock) {
            return mFootprints;
        }
    }

    SessionSnapshot getWorkingSessionSnapshot() {
        return mWorkingSessionSnapshot;
    }

    int getCachedEventCount() {
        final List<Event> cachedEvents = mReportCache.getCachedEvents();
        final List<Event> cachedNativeEvents = mReportCache.getCachedNativeEvents();
        return cachedEvents.size() + cachedNativeEvents.size();
    }

    String getUserIdentifier() {
        synchronized (mUserIdentifierLock) {
            return mUserIdentifier;
        }
    }

    static String getProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
        if (infos != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : infos) {
                if (processInfo.pid == pid) {
                    return processInfo.processName;
                }
            }
        }
        return null;
    }

}