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
package com.flaptor.hounder.crawler;

import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.PropertyConfigurator;

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class PageRankTest extends TestCase {

    private int PAGES = 0;
    private float score[];
    private int link[][];
    private Random rnd;
    private PageRank pr;

    int repetitions = 100;

    public void setUp() {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
        rnd = new Random(new Date().getTime());
    }

    public void tearDown() {}

    private void initWeb (int size) {
        PAGES = size;
        pr = new PageRank(PAGES);
        score = new float[PAGES];
        link = new int[PAGES][PAGES];
    }
    
    private void initScores () {
        for (int i=0; i<PAGES; i++) {
            score[i] = PageRank.INITIAL_SCORE / PAGES;
        }
    }

    private void randomLinks (int links, boolean self) {
        for (int n=0; n<links; n++) {
            int i = rnd.nextInt(PAGES);
            int j = rnd.nextInt(PAGES);
            if (i != j || self) {
                link[i][j] = 1;
            }
        }
    }

    private void fullLinks (boolean self) {
        int coverage = 100;
        for (int i=0; i<PAGES; i++) {
            for (int j=0; j<PAGES; j++) {
                if (i != j || self) {
                    link[i][j] = 1;
                }
            }
        }
    }

    private void avoidDangling () {
        for (int i=0; i<PAGES; i++) {
            boolean hasChildren = false;
            for (int j=0; j<PAGES; j++) {
                if (link[i][j] != 0) {
                    hasChildren = true;
                }
            }
            if (! hasChildren) {
                link[i][i] = 1;
            }
        }
    }

    private void cycle () {
        // count how many dangling pages there are
        float correctionFactor = 0;
        for (int page=0; page<PAGES; page++) {
            boolean hasChildren = false;
            for (int child=0; child<PAGES; child++) {
                if (link[page][child] == 1) {
                    hasChildren = true;
                }
            }
            if (! hasChildren) {
                correctionFactor += 1.0f/PAGES;
            }
        }
        // calculate the pagerank for each page
        for (int page=0; page<PAGES; page++) {
            pr.reset();
            for (int parent=0; parent<PAGES; parent++) {
                if (link[parent][page] == 1) {
                    int parentLinks = 0;
                    for (int child=0; child<PAGES; child++) {
                        parentLinks += link[parent][child];
                    }
                    float contribution = PageRank.parentContribution (score[parent], parentLinks);
                    pr.addContribution (contribution);
                }
            }
            score[page] = pr.getPageScore(); // + correctionFactor;
        }
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    private float totalPageRank () {
        float sum = 0;
        for (int i=0; i<PAGES; i++) {
            sum += score[i];
        }
        return sum;
    }

    private float converge (int cycles) {
        cycle();
        float sum = totalPageRank();
        float dif = Float.MAX_VALUE;
        for (int c=1; c<cycles; c++) {
            cycle();
            float newSum = totalPageRank();
            float newDif = Math.abs(sum - newSum);
            if (newDif == 0) break;
            sum = newSum;
            dif = newDif;
        }
        return sum;
    }


    private boolean startsLoop (int i, int[] path) {
        boolean loop = false;
        for (int j=0; j<PAGES && !loop; j++) {
            if (link[i][j] != 0) {
                if (path[j] != 0) {
                    loop = true;
                } else {
                    path[j] = 1;
                    if (startsLoop(j,path)) {
                        loop = true;
                    }
                }
            }
        }
        return loop;
    }

    // test for nodes without children
    private boolean isScrewed () {
        boolean hasLoop = false;
        int[] path = new int[PAGES];
        for (int i=0; i<PAGES && !hasLoop; i++) {
            path[i] = 1;
            if (startsLoop(i,path)) {
                hasLoop = true;
            }
        }

        boolean hasOpas = false;
        for (int i=0; i<PAGES && !hasOpas; i++) {
            boolean hasParents = false;
            for (int j=0; j<PAGES && !hasParents; j++) {
                if (link[j][i] != 0) {
                    hasParents = true;
                }
            }
            if (!hasParents) {
                hasOpas = true;
            }
        }

        return hasLoop && hasOpas;
    }

    private void showCorrelation (int[][] event, String[] rowName, String[] colName) {
        for (int i = 0; i < event.length; i++) {
            for (int j = 0; j < event[i].length; j++) {
                System.out.println(event[i][j] + " --> " + rowName[i] + " and " + colName[j]);
            }
        }
    }

    private int countZeroValuePages (float limit) {
        int count = 0;
        for (int i=0; i<PAGES; i++) {
            if (score[i] < limit) {
                count++;
            }
        }
        return count;
    }

    private void showWeb (boolean ok, boolean screwed, float totalScore) {
        System.out.println("---- pages:" + PAGES + " screwed:" + screwed + " ok:" + ok + " score: " + totalScore + " ----");
        TreeMap<Float,TreeSet<Integer>> m = new TreeMap<Float,TreeSet<Integer>>();
        for (int i=0; i<PAGES; i++) {
            TreeSet<Integer> set = m.get(score[i]);
            if (null == set) {
                set = new TreeSet<Integer>();
            }
            set.add(i);
            m.put(score[i], set);
        }
        Iterator<TreeSet<Integer>> indexSet = m.values().iterator();
        while (indexSet.hasNext()) {
            Iterator<Integer> iter = indexSet.next().iterator();
            while (iter.hasNext()) {
                int i = iter.next().intValue();
                System.out.print("Page " + i + "  Score " + score[i] + " Inbound: ");
                for (int j=0; j<PAGES; j++) {
                    if (link[j][i] != 0) {
                        System.out.print(j + " ");
                    }
                }
                System.out.print("Outbound: ");
                for (int j=0; j<PAGES; j++) {
                    if (link[i][j] != 0) {
                        System.out.print(j + " ");
                    }
                }
                System.out.println("");
            }
        }
        System.out.println("----");
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testPageRank () throws Exception {
        int minOk = Integer.MAX_VALUE;
        int maxFail = 0;
        int tests = repetitions;
        int converged = 0;
        int event[][] = new int[2][2];
        int zeroOK = 0;
        int zeroNOK = 0;
        for (int i=0; i<tests; i++) {
            initWeb(2+rnd.nextInt(50));
            boolean self = rnd.nextBoolean();
            int links = rnd.nextInt(PAGES*PAGES);
            int coverage = 100*links/(PAGES*PAGES);
            randomLinks(links, self);
            avoidDangling();
            initScores();

            float totalScore = converge(1000);

            boolean ok = (Math.abs(totalScore-1.0f) < 0.1f);
            boolean screwed = isScrewed();
            int countzero = countZeroValuePages(0.001f);
            event[screwed?0:1][ok?0:1]++;
            if (ok) {
                zeroOK += countzero;
                converged++;
                if (coverage < minOk)   
                    minOk = coverage;
            } else {
                zeroNOK += countzero;
                if (coverage > maxFail)
                    maxFail = coverage;
            }
            if (!ok) {
                showWeb(ok,screwed,totalScore);
            }
        }
//        System.out.println(100*converged/tests + "% converged.");
        if (converged < tests) {
            System.out.println("All with a coverage of more than " + maxFail + " converged.");
            System.out.println("All with a coverage of less than " + minOk + " failed.");
            showCorrelation(event, new String[]{"screwed","not screwed"}, new String[]{"ok","not ok"});
            System.out.println(zeroOK + " zero valued pages when ok");
            System.out.println(zeroNOK + " zero valued pages when not ok");
        }
        assertTrue(tests-converged + " of " + tests + " tests failed to converge to 1", converged == tests);
    }

}

