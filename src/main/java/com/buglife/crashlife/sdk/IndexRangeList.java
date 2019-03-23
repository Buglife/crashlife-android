package com.buglife.crashlife.sdk;

import java.util.ArrayList;
import java.util.List;

class IndexRangeList {
    private List<IndexRange> list;

    IndexRangeList() {
        list = new ArrayList<>();
    }

    void addIndexRange(IndexRange range) {
        if (!range.isValid()) {
            return;
        }
        int insertionIndex = 0;
        int reinsertionIndex = -1;
        for (IndexRange test : list) {
            if (test.containsIndex(range.start) && test.containsIndex(range.end)) {
                return; // nothing to do.
            } else if (test.containsIndex(range.start)) {
                test.end = range.end;
                reinsertionIndex = insertionIndex;
                break;
            } else if (test.containsIndex(range.end)) {
                test.start = range.start;
                reinsertionIndex = insertionIndex;
                break;
            } else if (range.end < test.start ) {
                break;
            } else if (range.start > test.end) {
                insertionIndex++;
            } else if (range.start < test.start && range.end > test.end) {
                test.start = range.start;
                test.end = range.end;
                reinsertionIndex = insertionIndex;
                break;
            }
        }
        if (reinsertionIndex != -1) {
            IndexRange ir = list.remove(reinsertionIndex);
            addIndexRange(ir);
        } else {
            list.add(insertionIndex, range);
        }
    }

    boolean containsIndex(long index) {
        for (IndexRange test : list) {
            if (test.containsIndex(index)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("IndexRangeList count ").append(list.size()).append("\n");
        for (IndexRange range : list) {
            builder.append(range).append("\n");
        }
        return builder.toString();
    }
}
