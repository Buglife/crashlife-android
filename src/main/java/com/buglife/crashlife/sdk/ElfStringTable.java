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

package com.buglife.crashlife.sdk;

import java.io.IOException;

public final class ElfStringTable {

	/** The string table data. */
	private final byte data[];
	public final int numStrings;

	/** Reads all the strings from [offset, length]. */
	ElfStringTable(ElfParser parser, long offset, int length) throws ElfException, IOException {
		parser.seek(offset);
		data = new byte[length];
		int bytesRead = parser.read(data);
		if (bytesRead != length)
			throw new ElfException("Error reading string table (read " + bytesRead + "bytes - expected to " + "read " + data.length + "bytes)");

		int stringsCount = 0;
		for (int ptr = 0; ptr < data.length; ptr++)
			if (data[ptr] == '\0') stringsCount++;
		numStrings = stringsCount;
	}

	public String get(int index) {
		int startPtr = index;
		int endPtr = index;
		while (data[endPtr] != '\0')
			endPtr++;
		return new String(data, startPtr, endPtr - startPtr);
	}
}
