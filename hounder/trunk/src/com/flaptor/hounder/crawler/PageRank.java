/*
Copyright 2008 Flaptor (flaptor.com) 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
*/
package com.flaptor.search4j.crawler;


/**
 * This class calculates the pagerank based on Sergey Brin and Lawrence Page's paper.
 * @see <a href="http://www.ams.org/featurecolumn/archive/pagerank.html"> How Google Finds Your Needle in the Web's Haystack </a>
 * @see <a href="http://www.webworkshop.net/pagerank.html"> Google's PageRank Explained </a>
 * @author Flaptor Development Team
 */
public class PageRank {

    public static float INITIAL_SCORE = 1.0f;
    private long webSize;
    private float damping;
    private float sum;

    /**
     * Initialize the class.
     * @param webSize the number of pages in the graph.
     */
    public PageRank (long webSize) {
        this.webSize = webSize;
        damping = 0.85f;
        sum = 0;
    }

    /**
     * Calculates the contribution of the page's parent to its score.
     * @param parentScore the score of the parent page.
     * @param parentNumOutlinks the number of outlinks of the parent page.
     * @return the contribution of the parent to the pages score.
     */
    public static float parentContribution (float parentScore, int parentNumOutlinks) {
        float contribution = 0;
        if (parentNumOutlinks > 0) {
            // this should always be true, since a node without parents cannot be reached.
            contribution =  parentScore / parentNumOutlinks;
        }
        return contribution;
    }

    /** 
     * Resets the score calculation for a page.
     * This has to be called before calculating the score of a page.
     */
    public void reset () {
        sum = 0;
    }

    /** 
     * Accumulates the contribution of a parent to the page's score. 
     * This has to be called for each parent of a page.
     * @param contribution the contribution of a parent page, as returned by the parentContribution method.
     */
    public void addContribution (float contribution) {
        sum += contribution;
    }

    /** 
     * Calculates the score of a page.
     * This has to be called after adding all the parent contributions.
     * @return the score of a page.
     */
    public float getPageScore () {
        return (1-damping)/webSize + damping * sum;
    }

}
