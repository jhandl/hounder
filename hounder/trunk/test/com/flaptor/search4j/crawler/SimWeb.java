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

import java.util.Random;

/**
 * @author Flaptor Development Team
 */
public class SimWeb {

    Random rnd = null;
    public int size;
    public int[][] link;
    public int[] status;
    public boolean[] reached;
    public static int FAILURE = 0;
    public static int SUCCESS = 1;

    public SimWeb (int size) {
        rnd = new Random(System.currentTimeMillis());
        this.size = size;
        link = new int[size][size];
        reached = new boolean[size];
        status = new int[size];
        for (int i=0; i<size; i++) {
            status[i] = SUCCESS;
        }

    }

    public int getSize () {
        return size;
    }

    public void addLink (int from, int to) {
        link[from][to] = 1;
    }

    public void randomLinks () {
        int links = rnd.nextInt(size*size);
        for (int n=0; n<links; n++) {
            int from = rnd.nextInt(size);
            int to = rnd.nextInt(size);
            addLink(from, to);
        }
    }

    public void fullLinks () {
        for (int from=0; from<size; from++) {
            for (int to=0; to<size; to++) {
                addLink(from, to);
            }
        }
    }

    public int[] getChildren (int parent) {
        int count = 0;
        for (int child=0; child<size; child++) {
            if (link[parent][child] != 0) {
                count++;
            }
        }
        int[] children = new int[count];
        count = 0;
        for (int child=0; child<size; child++) {
            if (link[parent][child] != 0) {
                children[count++] = child;;
            }
        }
        return children;
    }

    public void setStatus (int page, int st) {
        if (page >= 0 && page < size) {
            status[page] = st;
        }
    }

    public int getStatus (int page) {
        int st = FAILURE;
        if (page >= 0 && page < size) {
            st = status[page];
        }
        return st;
    }



    private int countReachable (int from, int[] path) {
        int reached = 1;
        path[from] = 1;
        for (int to=0; to<size; to++) {
            if (link[from][to] != 0) {
                if (path[to] == 0) {
                    path[to] = 1;
                    reached += countReachable(to,path);
                }
            }
        }
        return reached;
    }

    public void markAsReached (int page) {
        reached[page] = true;
    }

    public int countReached () {
        int count = 0;
        for (int i=0; i<size; i++) {
            if (reached[i]) {
                count++;
            }
        }
        return count;
    }

    public int getStartPage () {
        boolean found = false;
        int startPage = -1;
        int lastFrom = -1;
        for (int from=0; from<size && !found; from++) {
                int reached = countReachable(from, new int[size]);
                if (reached == size) { // check to see if all pages are reachable from here
                    startPage = from;
                    found = true;
                } else {
                    if (lastFrom != -1) {
                        addLink(from, lastFrom); // add a link to the root of the last subtree
                        reached = countReachable(from, new int[size]);
                        if (reached == size) {
                            startPage = from;
                            found = true;
                        }
                    }
                }
                lastFrom = from;
        }
        return startPage;
    }

    public static String pageToUrl (int page) {
        return "http://page." + page + ".test.com/";
    }

    public static int urlToPage (String url) {
        String parts[] = url.split("\\.");
        return Integer.parseInt(parts[1]);
    }

    public void show () {
        System.out.println("---- Test web: " + size + " pages");
        for (int i=0; i<size; i++) {
            System.out.print(reached[i] ? "+" : "-");
            System.out.print("Page " + i);
            System.out.print(status[i]==SUCCESS ? " " : "*");
/*
            System.out.print("   Inbound: ");
            for (int j=0; j<size; j++) {
                if (link[j][i] != 0) {
                    System.out.print(j + " ");
                }
            }
*/
            System.out.print("  Outbound: ");
            for (int j=0; j<size; j++) {
                if (link[i][j] != 0) {
                    System.out.print(j + " ");
                }
            }
            System.out.println("");
        }
        System.out.println("----");
    }

}   

