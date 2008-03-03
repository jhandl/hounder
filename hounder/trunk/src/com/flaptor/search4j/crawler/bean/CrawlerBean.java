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
package com.flaptor.search4j.crawler.bean;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flaptor.util.Config;


public class CrawlerBean {

    private File dir;
    private File confDir;
    private ClassLoader loader;
    private Config config;
    private Map<String,PatternFileBean> patternFiles;
    private static Map<String,ArrayList<String>> mapping;

    static {
        mapping = new HashMap<String,ArrayList<String>>();
        addToMap(mapping, "PatternClassifierModule", new String[]{"patterns.file"});
        addToMap(mapping, "MatchUrlModule", new String[]{"url.pattern.file"});
        addToMap(mapping, "BoostModule", new String[]{"condition.url.patterns.file","method.keyword.patterns.file"});
    }

    private static void addToMap(Map<String,ArrayList<String>> map, String key, String[] values) {
        ArrayList<String> valueList = new ArrayList<String>();
        for (String value : values) {
            valueList.add(value);
        }
        map.put(key,valueList);
    }
    
    public CrawlerBean(File dir, File libDir){
        this.dir = dir;
        
        try { 
            String[] libs = libDir.list();
            List<URL> urls = new ArrayList<URL>(libs.length);
            confDir = new File(dir.getAbsolutePath() + "/conf");
            urls.add(confDir.toURI().toURL());
            for (String lib:libs) {
                if (lib.matches(".*jar")) {
                    urls.add(new File(libDir.getAbsolutePath() + "/" + lib).toURI().toURL());
                }
            }
  
            loader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
            config = Config.getConfig("crawler.properties",loader);
            patternFiles = new HashMap<String,PatternFileBean>();

            // Parses crawler.properties modules lines, adding 
            // all files that use patters to the list
            String modules = config.getString("modules");
            String[] moduleLines = modules.split("\\|");
            for (String module: moduleLines) {
                String[] parts = module.split(",");
                if (parts.length == 2) {
                    createPatternBean(parts[0],parts[1]);
                }
            }
            PatternFileBean hotspotsBean = getPatternFileBean(config.getString("hotspot.file"));
            patternFiles.put("hotspots",hotspotsBean);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            // FIXME log
            loader = new URLClassLoader(new URL[0]);
            config = null;
        }
    }

    public boolean stop() {
        try {
            Process p = Runtime.getRuntime().exec("touch stop",null,dir);
            p.waitFor();
            return (0 == p.exitValue() );
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    public boolean start() {
        try {
            Process p = Runtime.getRuntime().exec("sh start.sh",null,dir);
            p.waitFor();
            return (0 == p.exitValue() );
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    public CrawlerStatusBean status() {
        try {
            Process p = Runtime.getRuntime().exec("sh status.sh", null,dir);
            StringBuffer buffer = new StringBuffer();
            p.waitFor();
            InputStream is = p.getInputStream();

            int character;
            while (-1 != ( character = is.read()) ) {
                buffer.append((char)character);
            }

            return new CrawlerStatusBean(buffer.toString());
        } catch (Exception e) {
            // FIXME log error
            return new CrawlerStatusBean("");
        }
    }

    private PatternFileBean getPatternFileBean(String filename) {
        File file = null;
        if (filename.startsWith("conf/")) {
            file = new File(dir, filename);
        } else {    
            file = new File(confDir, filename);
        }
        try {
            return new PatternFileBean(file);
        } catch (IOException e) {
            // FIXME add logging
            System.out.println(e);
            return null;
        }
    }

    public InjectorBean getInjectorBean() {
        String injectedPath = config.getString("injected.pagedb.dir");
        return new InjectorBean(new File(dir + File.separator + injectedPath));
    }

    public PageDBExportBean getPageDBExportBean() {
        try {
            String pagedbPath = config.getString("pagedb.dir");
            String pagedbExportDestination = config.getString("pagedb.export.destination");
            return new PageDBExportBean(new File(dir + File.separator + pagedbPath),pagedbExportDestination);
        } catch (Exception e) {
            return null;
        }
    }

    public ClassLoader getClassLoader() {
        return loader;
    }



    private void createPatternBean(String className, String moduleName) {
        String prefix = "com.flaptor.search4j.crawler.modules.";
        if (!className.startsWith(prefix)) return;
        className = className.substring(prefix.length());

        PatternFileBean bean = null;
        ArrayList<String> properties = mapping.get(className);
        if (null != properties) {
            for (String configProperty : properties) {
                if (null != configProperty) {
                    // FIXME: The code below incorrectly assumes that a module can have no more than one pattern file.
                    // This forces to use many instances of a module, each with one pattern file, otherwise the admin
                    // webapp wouldn't show all the pattern files.

                    // Get module config. this has to be in sync with what modulesmanager does to load.
                    Config moduleConfig = Config.getConfig(moduleName + "Module.properties",loader);
                    String filename = moduleConfig.getString(configProperty);
                    if (null != filename && filename.trim().length() > 0) {
                        bean = getPatternFileBean(filename);
                        patternFiles.put(moduleName,bean);
                    }
                }
            }
        }
    }


    public Map<String,PatternFileBean> getPatternFileBeans() {
        return new HashMap<String,PatternFileBean>(patternFiles);
    }

}
