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
package com.flaptor.search4j.classifier;

/**
 * @author Flaptor Development Team
 */
public class ConfigBean {
    private String[] categoryList;
    private String cacheDir;
    private String baseDir;
//    private double unknownTermsProbability= -1;
    private String urlFile;
    
    public String[] getCategoryList() {
        return categoryList;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public boolean initialize(String[] categoryList, String cacheDir, String baseDir, 
            String urlFile, double unknownTermsProbability) {
        this.categoryList = categoryList;
        this.cacheDir = cacheDir;
        this.baseDir = baseDir;
        this.urlFile= urlFile;
//        this.unknownTermsProbability= unknownTermsProbability;
        return true;
    }

    public ConfigBean(){
        
    }

/*    // TODO: Move it to config file "classifier.properties"
    public double getUnknownTermsProbability() {
        return unknownTermsProbability;
    }
*/
    public String getUrlFile() {
        return urlFile;
    }

    



}
