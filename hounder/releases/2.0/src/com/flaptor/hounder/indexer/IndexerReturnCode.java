package com.flaptor.hounder.indexer;


public enum IndexerReturnCode {
    SUCCESS (0),
    PARSE_ERROR (1),
    RETRY_QUEUE_FULL (2),
    FAILURE (3); // INTERNAL ERROR

    private final int oldRetValue;
    
    IndexerReturnCode(int retVal) {
        this.oldRetValue = retVal;
    }

    public int getOldRetValue() {
        return oldRetValue;
    }
}
