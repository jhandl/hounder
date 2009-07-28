package com.flaptor.hounder.lucene;

import java.util.Set;
import org.apache.lucene.search.HitCollector;


public abstract class ScorelessHitCollector extends HitCollector {
    public abstract Set<Integer> getMatchingDocuments();
}
