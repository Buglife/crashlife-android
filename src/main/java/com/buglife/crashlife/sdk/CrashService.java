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

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import ru.ivanarh.jndcrash.NDCrashService;

public class CrashService extends NDCrashService {
    
    @Override
    public void onCrash(String reportPath) {
        Log.d("In onCrash in a service");
        EnvironmentSnapshot environmentSnapshot = new EnvironmentSnapshot(this);
        DeviceSnapshot deviceSnapshot = new DeviceSnapshot(this);
        //SessionSnapshot will have to be managed like the footprints/attributemaps files.

        JSONObject metadata = new JSONObject();
        JsonUtils.safePut(metadata, "environment_snapshot", environmentSnapshot.toCacheJson());
        JsonUtils.safePut(metadata, "device_snapshot", deviceSnapshot.toCacheJson());

        File reportFile = new File(reportPath);
        String uuid = reportFile.getName().replace(".txt", "");
        File crashFolder = reportFile.getParentFile();
        File metadataFile = new File(crashFolder, uuid + ReportCache.SNAPSHOTS_FILE_SUFFIX);
        try {
            IOUtils.writeStringToFile(metadata.toString(), metadataFile);
        } catch (IOException e) {
            Log.e("Unable to write Crashlife native crash to disk. If you see this message, please contact support@buglife.com.");
            e.printStackTrace();
        }
    }
}
