/*
Copyright (c) 2016-2017 Fredrik Fornwall.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*
 * additions Copyright (C) 2019 Buglife, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;

/** Package internal class used for parsing ELF files. */
class ElfParser {

    final ElfFile elfFile;
    private final ByteArrayInputStream fsFile;

    private final MappedByteBuffer mappedByteBuffer;
    private final long mbbStartPosition;

    ElfParser(ElfFile elfFile, ByteArrayInputStream fsFile) {
        this.elfFile = elfFile;
        this.fsFile = fsFile;
        mappedByteBuffer = null;
        mbbStartPosition = -1;
    }

    ElfParser(ElfFile elfFile, MappedByteBuffer byteBuffer, long mbbStartPos) {
        this.elfFile = elfFile;
        mappedByteBuffer = byteBuffer;
        mbbStartPosition = mbbStartPos;
        mappedByteBuffer.position((int)mbbStartPosition);
        fsFile = null;
    }

    public void seek(long offset) {
        if (fsFile != null) {
            fsFile.reset();
            if (fsFile.skip(offset) != offset) throw new ElfException("seeking outside file");
        }
        else if (mappedByteBuffer != null) {
            mappedByteBuffer.position((int)(mbbStartPosition + offset)); // we may be limited to sub-4GB APKs. big whoop
        }

    }

    /**
     * Signed byte utility functions used for converting from big-endian (MSB) to little-endian (LSB).
     */
    short byteSwap(short arg) {
        return (short) ((arg << 8) | ((arg >>> 8) & 0xFF));
    }

    int byteSwap(int arg) {
        return ((byteSwap((short) arg)) << 16) | (((byteSwap((short) (arg >>> 16)))) & 0xFFFF);
    }

    long byteSwap(long arg) {
        return ((((long) byteSwap((int) arg)) << 32) | (((long) byteSwap((int) (arg >>> 32))) & 0xFFFFFFFF));
    }

    short readUnsignedByte() {
        int val = -1;
        if (fsFile != null) {
            val = fsFile.read();
        } else if (mappedByteBuffer != null) {
            byte temp = mappedByteBuffer.get();
            val = temp & 0xFF; // bytes are signed in Java =_= so assigning them to a longer type risks sign extension.
        }
        if (val < 0) throw new ElfException("Trying to read outside file");
        return (short) val;
    }

    short readShort() throws ElfException {
        int ch1 = readUnsignedByte();
        int ch2 = readUnsignedByte();
        short val = (short) ((ch1 << 8) + (ch2 << 0));
        if (elfFile.encoding == ElfFile.DATA_LSB) val = byteSwap(val);
        return val;
    }

    int readInt() throws ElfException {
        int ch1 = readUnsignedByte();
        int ch2 = readUnsignedByte();
        int ch3 = readUnsignedByte();
        int ch4 = readUnsignedByte();
        int val = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));

        if (elfFile.encoding == ElfFile.DATA_LSB) val = byteSwap(val);
        return val;
    }

    long readLong() {
        int ch1 = readUnsignedByte();
        int ch2 = readUnsignedByte();
        int ch3 = readUnsignedByte();
        int ch4 = readUnsignedByte();
        int val1 = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        int ch5 = readUnsignedByte();
        int ch6 = readUnsignedByte();
        int ch7 = readUnsignedByte();
        int ch8 = readUnsignedByte();
        int val2 = ((ch5 << 24) + (ch6 << 16) + (ch7 << 8) + (ch8 << 0));

        long val = ((long) (val1) << 32) + (val2 & 0xFFFFFFFFL);
        if (elfFile.encoding == ElfFile.DATA_LSB) val = byteSwap(val);
        return val;
    }

    /** Read four-byte int or eight-byte long depending on if {@link ElfFile#objectSize}. */
    long readIntOrLong() {
        return elfFile.objectSize == ElfFile.CLASS_32 ? readInt() : readLong();
    }

    /** Returns a big-endian unsigned representation of the int. */
    long unsignedByte(int arg) {
        long val;
        if (arg >= 0) {
            val = arg;
        } else {
            val = (unsignedByte((short) (arg >>> 16)) << 16) | ((short) arg);
        }
        return val;
    }

    /**
     * Find the file offset from a virtual address by looking up the {@link ElfSegment} segment containing the
     * address and computing the resulting file offset.
     */
    long virtualMemoryAddrToFileOffset(long address) throws IOException {
        for (int i = 0; i < elfFile.num_ph; i++) {
            ElfSegment ph = elfFile.getProgramHeader(i);
            if (address >= ph.virtual_address && address < (ph.virtual_address + ph.mem_size)) {
                long relativeOffset = address - ph.virtual_address;
                if (relativeOffset >= ph.file_size)
                    throw new ElfException("Can not convert virtual memory address " + Long.toHexString(address) + " to file offset -" + " found segment " + ph
                            + " but address maps to memory outside file range");
                return ph.offset + relativeOffset;
            }
        }
        throw new ElfException("Cannot find segment for address " + Long.toHexString(address));
    }

    public int read(byte[] data) throws IOException {
        if (fsFile != null) {
            return fsFile.read(data);
        } else if (mappedByteBuffer != null) {
            mappedByteBuffer.get(data);
            return data.length;
        }
        throw new IOException("No way to read from file or buffer");
    }

}
