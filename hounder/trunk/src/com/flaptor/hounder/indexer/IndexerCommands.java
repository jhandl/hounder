package com.flaptor.hounder.indexer;

public class IndexerCommands {
    
    public static void optimize(Indexer indexer) {
        indexer.getIndexManager().scheduleOptimize();
    }

    public static void close(Indexer indexer) {
        indexer.requestStop();
    }

    public static void checkpoint(Indexer indexer) {
        indexer.getIndexManager().makeDirectoryCheckpoint();
    }
}
