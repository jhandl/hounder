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
package com.flaptor.hounder.crawler.bean;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Flaptor Development Team
 */
public class ConfigBean {
    private String crawlerBaseDir;
    private String libDir;
    private Map<String,CrawlerBean> crawlers;
    private boolean inited;

    public String getCrawlerBaseDir() {
        return crawlerBaseDir;
    }

    public String getLibDir() {
        return libDir;
    }

    public boolean initialize(String crawlerBaseDir, String libDir) {
        this.crawlerBaseDir = crawlerBaseDir;
        this.libDir = libDir;
        inited = findCrawlers();
        return inited;
    }

    public ConfigBean(){
        
    }

    private boolean findCrawlers() {
        this.crawlers = new HashMap<String,CrawlerBean>();
        File baseDir = new File(crawlerBaseDir);
        File jarDir = new File(libDir);
        if (!baseDir.isDirectory()) return false;

        File[] crawlerDirs = baseDir.listFiles();

        for (File  crawlerFile: crawlerDirs) {
            if (crawlerFile.isDirectory()) {
                CrawlerBean bean = new CrawlerBean(crawlerFile,jarDir);
                crawlers.put(crawlerFile.getName(),bean);
            }
        }

        return true;
    }


    public CrawlerBean getCrawlerBean(String name) {
        return crawlers.get(name);
    }

    public String[] getCrawlerNames() {
        return crawlers.keySet().toArray(new String[]{});
    }

    public boolean isInited() {
        return inited;
    }

}
