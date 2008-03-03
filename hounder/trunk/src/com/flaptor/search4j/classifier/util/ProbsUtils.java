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
package com.flaptor.search4j.classifier.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.flaptor.util.FileUtil;
import com.flaptor.util.Pair;
/**
 * @author Flaptor Development Team
 */
public class ProbsUtils {

    private static final String SUFFIX_INCLUDED="_included_urls";
    private static final String SUFFIX_NOT_INCLUDED="_notIncluded_urls";
    private static final String SUFFIX_UNKNOWN="_unknown_urls";

    public static final int UNKNOWN= 0;
    public static final int INCLUDED= 1;
    public static final int NOT_INCLUDED= 2;


    /**
     * Loads the data from cat_included_urls/cat_notIncluded_urls 
     */
    public static List<String> loadUrlsList(String baseDir, String catName, int type){
        String suffix;
        switch (type){
            case INCLUDED:     suffix= SUFFIX_INCLUDED; break;
            case NOT_INCLUDED: suffix= SUFFIX_NOT_INCLUDED; break;            
            case UNKNOWN: // no break here
            default:     suffix= SUFFIX_UNKNOWN; break;
        }

        String catUrlsFileName= catName + suffix;
        List<String> catUrlsList = new ArrayList<String>();        
        if (!FileUtil.fileToList(baseDir, catUrlsFileName, catUrlsList)){
            String msg="Cant load the category file " + catUrlsFileName + 
            " from " + baseDir + ". Creating an empty one";
            System.err.println(msg);
            catUrlsList= new ArrayList<String>();
        }
        return catUrlsList;
    }

    public static boolean saveUrlList(String baseDir, String catName, int type, List<String> urlsList ){
        String suffix;
        switch (type){
            case INCLUDED:     suffix= SUFFIX_INCLUDED; break;
            case NOT_INCLUDED: suffix= SUFFIX_NOT_INCLUDED; break;            
            case UNKNOWN: // no break here
            default:     suffix= SUFFIX_UNKNOWN; break;
        }
        String catUrlsFileName= catName + suffix;
        return FileUtil.listToFile(baseDir, catUrlsFileName, urlsList);
    }

    public static List<Pair<Double, String>> getSortedMapByVal(Map<String,Double> map){
        List<Pair<Double, String>> list= new ArrayList<Pair<Double,String>>();
        for (Map.Entry<String,Double> entry: map.entrySet()) {
            list.add(new Pair<Double, String>(entry.getValue(), entry.getKey()));
        }
        Collections.sort(list, Collections.reverseOrder());
        return list;       
    }
}
