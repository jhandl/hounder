package com.flaptor.hounder.searcher;

public class SearchTimeoutException extends SearcherException {
    private static final long serialVersionUID = 1L;
    private final long timeout;

    public SearchTimeoutException(long timeout, String text) {
        super(text);
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }
}
