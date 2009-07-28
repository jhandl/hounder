package com.flaptor.hounder.lucene;

import org.apache.lucene.search.HitCollector;


public class CountingHitCollector extends HitCollector {
    private int docCount = 0;

    @Override
    public void collect(int doc, float score) {
        docCount++;
    }

    public int getDocCount() {
        return docCount;
    }
}
