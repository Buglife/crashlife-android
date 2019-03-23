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

import java.io.IOException;

class ElfNote {
    private int nameSize;
    private int descSize;
    private int type;
    private String name;
    private String desc;
    private byte[] descBytes;
    ElfNote(ElfParser parser, long offset, int size) throws ElfException, IOException {
        parser.seek(offset);
        nameSize = parser.readInt();
        descSize = parser.readInt();
        type = parser.readInt();
        byte nameBytes[] = new byte[nameSize];
        byte descBytes[] = new byte[descSize];
        int bytesRead = parser.read(nameBytes);
        if (bytesRead != nameSize) {
            throw new ElfException("Error reading note (read " + bytesRead + "bytes - expected to " + "read " + nameSize + "bytes)");
        }
        while (bytesRead % 4 != 0) { // finish reading the padding to the nearest 4 bytes
            parser.readUnsignedByte();
            bytesRead += 1;
        }
        bytesRead = parser.read(descBytes);
        if (bytesRead != descSize) {
            throw new ElfException("Error reading note (read " + bytesRead + "bytes - expected to " + "read " + descSize + "bytes)");
        }
        while (bytesRead % 4 != 0) { // finish reading the padding to the nearest 4 bytes
            parser.readUnsignedByte();
            bytesRead += 1;
        }
        name = new String(nameBytes, 0, nameSize-1); // unnecessary trailing 0
        desc = new String(descBytes, 0, descSize); // There's no trailing 0 on desc
    }

    String getName() {
        return name;
    }

    int getType() {
        return type;
    }

    String getDesc() {
        return desc;
    }

    byte[] getDescBytes() {
        return descBytes;
    }
}
