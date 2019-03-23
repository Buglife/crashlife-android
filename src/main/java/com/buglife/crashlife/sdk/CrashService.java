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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashSet;
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
            IndexRangeList indexRangeList = new IndexRangeList();
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".so")) {
                    int n = line.lastIndexOf(" ");
                    String lib = line.substring(n + 1);
                    if (!(lib.startsWith("/system") || lib.startsWith("/vendor"))) {
                        libs.add(lib);
                    }
                }
                if (line.endsWith(".apk")) {
                    String[] fields = line.split(" ");
                    String addresses = fields[0];
                    String permissions = fields[1];
                    String offset = fields[2];
                    long longOffset = Long.valueOf(offset, 16);
                    int n = line.lastIndexOf(" ");
                    Log.d(line);
                    String apk = line.substring(n + 1);
                    if (!apk.startsWith("/vendor")) {
                        ElfFile elf = elfFileForZippedElf(apk, longOffset, indexRangeList);
                        if (elf != null) {
                            String buildId = buildIdForLibrary(elf);
                            // TODO: also get the name of the library. It's somewhere in there.
                        }
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
            e.printStackTrace();
        } catch (ElfException e) {
            e.printStackTrace();
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

    ElfFile elfFileForZippedElf(String apkPath, long startingOffset, IndexRangeList skipList) {

        // What's the deal with the skip list? Glad you asked.
        // We have an APK that looks a bit like this:
        // (.(*%4096==0)(so-file)*.(*%4096==0))*.*
        // So possibly multiple so-files surrounded by 0 or more pages of zipped binary data,
        // and then non-aligned data at the end.
        // We have multiple entries that could correspond to the same or different so-files inside the APK.
        // Each entry might point to the beginning of the so-file.
        // But if it doesn't, the naive approach is to walk backwards by pages until we find it
        // Problem is, the so-file might be *big*. And if this is the first time we're walking back through it,
        // well, that sucks, but it's got to be done. There's no call to do it a second time though,
        // so we should keep track of the ranges that we've traversed. If we ever see file that we've walked
        // before, we can bail early knowing we'll either end up at 0 or at the beginning of a so-file
        // we've already found before. So what's the funny business in the `if (foundElf)` block? Just in case
        // we found the ELF header in the so-file on the first try (or relatively soon after),
        // we should try to find the extent of the ELF within the APK (or something a reasonable distance from it)
        // so that we hopefully don't have to walk the rest of the so-file at all a second or third or nth time.
        // FORTUNATELY, it seems that the ELF is not edited to make the offsets be relative to the APK,
        // despite the addresses in the crash report being relative to the APK, not to the so-file.
        // The latter part is actually a good thing, as long as we send up the offsets within the APK
        // for each library; it means that we can subtract those offsets from the address. The smallest
        // non-negative effective address generated this way is the symbol we need to symbolicate
        // using the UUID we'll find with `buildIdForLibrary`. Whew.

        File apk = new File(apkPath);
        IndexRange skip = new IndexRange();
        skip.end = startingOffset; // we work backwards in this file
        long possibleStart = startingOffset - startingOffset%4096; // page size. TODO: don't hardcode that
        Log.d("startingOffset = " + startingOffset + "; startingOffset% 4096 = " + startingOffset%4096 + "; possibleStart = " + possibleStart);
        byte elfStart[] = new byte[4];
        boolean foundElf = false;
        try /*(RandomAccessFile file = new RandomAccessFile(apk, "r"))*/ {
            FileInputStream in = new FileInputStream(apk);
            FileChannel fileChannel = in.getChannel();
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, apk.length());
            // TODO: consider using MappedByteBuffer for the non-zipped case too.
            // should be (a lot) more memory-efficient than reading the ELF into memory
            // Need to test it on (much) larger so-file libs.
//            BufferedInputStream buff = new BufferedInputStream(in);
            while (!foundElf && possibleStart >= 0) {
//                Log.d("rewind and setting to " + possibleStart);
                skip.start = possibleStart;
                if (skipList.containsIndex(possibleStart)) {
                    skipList.addIndexRange(skip);
                    return null;
                }
                buffer.rewind();
                buffer.position((int) possibleStart);
                buffer.get(elfStart);
                Log.d("elfStart? " + elfStart[0] + "" + (char) elfStart[1] + "" + (char) elfStart[2] + "" + (char) elfStart[3]);
                if (elfStart[0] == 0x7f && elfStart[1] == 'E' && elfStart[2] == 'L' && elfStart[3] == 'F') {
                    foundElf = true;
                    skip.start = possibleStart;
                } else {
                    possibleStart -= 4096;
                }
            }
            if (foundElf) {
                ElfFile elf = new ElfFile(buffer, possibleStart);
                // Section Headers seem to be last?
                // This could probably be improved.
                skip.end = Math.max(skip.end, possibleStart + elf.sh_offset + elf.sh_entry_size*elf.num_sh);
                skipList.addIndexRange(skip);
                return elf;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    String buildIdForLibraryAtPath(String path) {
        try {
            ElfFile elf = ElfFile.fromFile(new File(path));
            return buildIdForLibrary(elf);
        } catch (ElfException e) {
            Log.e("Failed to get build id from library: " + path);
        } catch (IOException e) {
            Log.e("Couldn't read library at path " + path + " because " + e);
        }
        return null;
    }

    String buildIdForLibrary(ElfFile elf) throws ElfException, IOException {
        for (int i = 0; i < elf.num_sh; i++) {
            ElfSection sectionHeader = elf.getSection(i);
            if (sectionHeader.type != ElfSection.SHT_NOTE) {
                continue;
            }
            String sectionHeaderName = sectionHeader.getName();
            if (sectionHeaderName == null) {
                continue;
            }
            if (!sectionHeaderName.equals(".note.gnu.build-id")) {
                continue;
            }
            ElfNote note = sectionHeader.getNote();
            if (note == null) {
                continue;
            }
            boolean littleEndian = elf.encoding == ElfFile.DATA_LSB;
            return computeDebugId(note.getDescBytes(), littleEndian);
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
