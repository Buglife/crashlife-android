package com.buglife.crashlife.sdk;

class IndexRange {
    private int INVALID = -1;
    long start;
    long end;

    IndexRange() {
        start = INVALID;
        end = INVALID;
    }

    boolean isValid() {
        return start != INVALID && end != INVALID && start <= end;
    }

    boolean containsIndex(long index) {
        return index >= start && index <= end && isValid();
    }

    @Override
    public String toString() {
        return "("+ start + ", " + end + ")";
    }
}


