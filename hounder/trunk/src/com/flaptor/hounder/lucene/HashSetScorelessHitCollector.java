package com.flaptor.hounder.lucene;

import java.util.HashSet;
import java.util.Set;

public class HashSetScorelessHitCollector extends ScorelessHitCollector {
    HashSet<Integer> set = new HashSet<Integer>();

    @Override
    public void collect(int doc, float score) {
        set.add(doc);
    }

    @Override
    public HashSet<Integer> getMatchingDocuments() {
        return set;
    }
}
