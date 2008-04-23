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
package com.flaptor.hounder.crawler.modules;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * Dummy class that only echoes a message when internalProcess is called.
 * @author Flaptor Development Team
 */
public class LoggerModule extends AProcessorModule {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    // The following are 'package/default' for unit test
    static final String ATTR_STR="ATTRIBUTES";
    static final String CAT_STR="CATEGORIES";
    static final String TAGS_STR="TAGS";
    
    private PrintStream out;
    private Set<String> attributesToLog;
    private Set<String> tagsToLog;
    private Set<String> categoriesToLog;
    private boolean logText= false;
    private boolean logEmmited= false;

    public LoggerModule (String name, Config globalConfig) throws IOException{
        super(name, globalConfig);
        
        attributesToLog = loadTags("attributes.to.log");
        tagsToLog = loadTags("tags.to.log");
        categoriesToLog = loadTags("categories.to.log");
        Config mdlConfig = getModuleConfig();
        logText= mdlConfig.getBoolean("log.text");
        logEmmited= mdlConfig.getBoolean("log.emmited");

        String outputFileName= mdlConfig.getString("log.file.name");

        if (null == outputFileName || 0 == outputFileName.length() ||
                outputFileName.equalsIgnoreCase("stderr")){
            out= System.err;
        } else if (outputFileName.equalsIgnoreCase("stdout")){
            out= System.out;
        }
        out= new PrintStream(outputFileName);
    }
    

    
    private void logIt(FetchDocument doc){        
        out.print(ATTR_STR + ": ");
        Set<String> attributes=doc.getAttributes().keySet();
        for (String attr: attributes){
            if (attributesToLog.contains("*") || attributesToLog.contains(attr)){
                out.print(attr + "=" + doc.getAttribute(attr) + " , ");
            }
        }
        out.print("\n" + CAT_STR + ": ");
        Set<String> categories=doc.getCategories();
        for (String cat: categories){
            if (categoriesToLog.contains("*") || categoriesToLog.contains(cat)){
                out.print(cat + " , ");
            }
        }
        out.print("\n" + TAGS_STR + ": ");
        Set<String> tags=doc.getTags();
        for (String tag: tags){
            if (tagsToLog.contains("*") || tagsToLog.contains(tag)){
                out.println(tag + " , ");
            }            
        }
        out.println();
    }
    
    
    public synchronized void internalProcess (FetchDocument doc) {
        out.println("--------------------------------------------------");
        Page page = doc.getPage();
        if (null == page) {
            out.println("Null Page");
        } else {
            out.println("Url="+page.getUrl());
            logIt(doc);
            if (logText){
                out.println("TEXT="+doc.getText(80));
            }
            if (logEmmited){
                out.println("Emmited="+page.isEmitted());
            }
        }
        out.println("--------------------------------------------------");
        out.flush();
    }
}
