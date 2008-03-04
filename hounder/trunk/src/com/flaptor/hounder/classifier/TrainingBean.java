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
package com.flaptor.hounder.classifier;


/**
 * 
 * @author Flaptor Development Team
 *
 */
// TODO: use ConfigBean instead, or extend it. rafa
public class TrainingBean {
    protected boolean inited = false;
    protected ConfigBean config;

    protected boolean initialize(ConfigBean config) {
        this.config= config;
        return true;
    }

    public String getCacheDir() {
        return config.getCacheDir();
    }
    
    public String getBaseDir() {
        return config.getBaseDir();
    }
    
    public String getUrlFile() {
        return config.getUrlFile();
    }    

    
    public boolean isInited() {
        return inited;
    }
    
    public String[] getCategoryList(){
        return config.getCategoryList();
    }
}
