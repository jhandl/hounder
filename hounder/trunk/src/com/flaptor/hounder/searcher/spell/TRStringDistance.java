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
package com.flaptor.hounder.searcher.spell;

/**
 * Edit distance  class
 * @author Flaptor Development Team
 */
final class TRStringDistance {

    final char[] sa;
    final int n;
    final float[][][] cache=new float[30][][];


    /**
     * Optimized to run a bit faster than the static getDistance().
     * In one benchmark times were 5.3sec using ctr vs 8.5sec w/ static method, thus 37% faster.
     */
    public TRStringDistance (String target) {
        sa=target.toCharArray();
        n=sa.length;
    }


    //*****************************
     // Compute Levenshtein distance
     //*****************************
      public final float getDistance (String other) {
          float d[][]; // matrix
          float cost; // cost

          // Step 1
          final char[] ta=other.toCharArray();
          final int m=ta.length;
          if (n==0) {
              return m;
          }
          if (m==0) {
              return n;
          }

          if (m>=cache.length) {
              d=form(sa, ta);
          }
          else if (cache[m]!=null) {
              d=cache[m];
          }
          else {
              d=cache[m]=form(sa, ta);

              // Step 3

          }
          for (int i=1; i<=n; i++) {
              final char s_i=sa[i-1];

              // Step 4

              for (int j=1; j<=m; j++) {
                  final char t_j=ta[j-1];

                  // Step 5
                  cost = distance (s_i,t_j);
                  // Step 6
                  d[i][j]=min3(d[i-1][j]+d[i][0], d[i][j-1]+d[0][j], d[i-1][j-1]+cost);

                  // Damerau-Levenshtein transposition
                  if(i > 1 && j > 1 && sa[i-1] == ta[j-2] && sa[i-2] == ta[j-1]) {
                      d[i][j] = Math.min( d[i][j], d[i-2][j-2] + cost);
                  }

              }

          }

          // Step 7
          return d[n][m];

      }


      /**
       *
       */
    private static float[][] form (char[] na, char[] ma) {
        int n = na.length,m=ma.length;
        float[][] d=new float[na.length+1][ma.length+1];
          // Step 2

        float cost = 0;
        for (int i=0; i<=n; i++) {
            if (i>0) { 
                cost = ('h' == na[i-1] ) ? 0.2f : 1;
            }
            d[i][0]=cost;
        }
        cost = 0;
        for (int j=0; j<=m; j++) {
            if (j>0) { 
                cost = ('h' == ma[j-1] ) ? 0.2f : 1;
            }
            d[0][j]=cost;
        }


        return d;
    }


    //****************************
    // Get minimum of three values
    //****************************
    private static float  min3 (float a, float b, float c) {
        float mi=a;
        if (b<mi) {
            mi=b;
        }
        if (c<mi) {
            mi=c;
        }
        return mi;

    }

    private static float distance (char one, char other) {
        // special case
        if (one == other ) return 0f;

        char min = (char)Math.min(one,other); 
        char max = (char)Math.max(one,other);

        switch (min) {
            case 's':
                if ('z' == max) return 0.4f;
                break;
            case 'm':
                if ('n' == max) return 0.6f;
                break;
            case 'c':
                if ('k' == max) return 0.6f;
                if ('s' == max) return 0.6f;
                break;
            case 'b': 
                if ('v' == max) return 0.4f;
                break;
            case 'g':
                if ('j' == max) return 0.6f;
                break;

        }
        // default
        return 1f;
    }


    public static void main(String[] args) {
        String target = args[0];
        String other = args[1];

        TRStringDistance std = new TRStringDistance(target);

        System.out.println(target + " and " + other + " are at " + std.getDistance(other) + " distance");
    }
}
