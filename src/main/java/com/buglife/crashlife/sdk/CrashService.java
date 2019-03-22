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

import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ru.ivanarh.jndcrash.NDCrashService;

public class CrashService extends NDCrashService {
    
    @Override
    public void onCrash(String reportPath) {
        Log.d("In onCrash in a service");
        EnvironmentSnapshot environmentSnapshot = new EnvironmentSnapshot(this);
        DeviceSnapshot deviceSnapshot = new DeviceSnapshot(this);
        //SessionSnapshot will have to be managed like the footprints/attributemaps files.
        JSONObject libIds = new JSONObject();

        try {
            Set<String> libs = new HashSet<>();
            String mapsFile = "/proc/" + android.os.Process.myPid() + "/maps";
            BufferedReader reader = new BufferedReader(new FileReader(mapsFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".so")) {
                    int n = line.lastIndexOf(" ");
                    String lib = line.substring(n + 1);
                    if (!lib.startsWith("/system")) {
                        libs.add(lib);
                    }
                }
            }
            for (String lib : libs) {
                String buildId = buildIdForLibraryAtPath(lib);
                Log.d("got build id " + buildId + " for lib " + lib);
                if (buildId != null) {
                    JsonUtils.safePut(libIds, lib, buildId);
                }
            }

        } catch (FileNotFoundException e) {
            // Do some error handling...
        } catch (IOException e) {
            // Do some error handling...
        }

        JSONObject metadata = new JSONObject();
        JsonUtils.safePut(metadata, "environment_snapshot", environmentSnapshot.toCacheJson());
        JsonUtils.safePut(metadata, "device_snapshot", deviceSnapshot.toCacheJson());
        JsonUtils.safePut(metadata, "lib_build_ids", libIds);

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

    @Nullable
    String buildIdForLibraryAtPath(String path) {
        try {
            ElfFile elf = ElfFile.fromFile(new File(path));
            for (int i = 0; i < elf.num_sh; i++) {
                ElfSection sectionHeader = elf.getSection(i);
                if (sectionHeader.type != ElfSection.SHT_NOTE) {
                    continue;
                }
                if (!sectionHeader.getName().equals(".note.gnu.build-id")) {
                    continue;
                }
                ElfNote note = sectionHeader.getNote();
                boolean littleEndian = elf.encoding == ElfFile.DATA_LSB;
                return computeDebugId(note.getDescBytes(), littleEndian);
            }
        } catch (ElfException e) {
            Log.e("Failed to get build id from library: " + path);
        } catch (IOException e) {
            Log.e("Couldn't read library at path " + path + " because " + e);
        }
        return null;
    }

    // 9229abee-e36c-a8f9-af13-c4eb8b7ec1aa
    // 04000000 14000000 03000000 474E5500 EEAB2992 6CE3 F9A8 AF13 C4EB8B7EC1AA8 729E0A7
    // This follows what Breakpad does
    // *sigh*
    private String computeDebugId(byte[] descBytes, boolean littleEndian) {
        byte[] sized = Arrays.copyOfRange(descBytes, 16, 32); // sizeof UUID, truncated or padded with 0s
        if (littleEndian) {
            byte tmp = sized[0];
            sized[0] = sized[3];
            sized[3] = tmp;
            tmp = sized[1];
            sized[1] = sized[2];
            sized[2] = tmp;

            tmp = sized[4];
            sized[4] = sized[5];
            sized[5] = tmp;

            tmp = sized[6];
            sized[6] = sized[7];
            sized[7] = tmp;
        }
        byte[] lowerHalf = Arrays.copyOfRange(sized, 8, sized.length);
        long hi = bytesToLong(sized);
        long lo = bytesToLong(lowerHalf);
        UUID uuid = new UUID(hi, lo);
        return uuid.toString().toLowerCase();
    }
    private static long bytesToLong(byte[] b) {
        long result = 0;
        for (int i = 0; i < Long.SIZE/Byte.SIZE; i++) {
            result <<= Byte.SIZE;
            result |= (b[i] & 0xFF);
        }
        return result;
    }
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
