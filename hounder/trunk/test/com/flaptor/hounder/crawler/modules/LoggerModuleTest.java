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
package com.flaptor.search4j.crawler.modules;

import java.util.HashSet;
import java.util.Set;

import com.flaptor.search4j.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class LoggerModuleTest extends TestCase {

    public LoggerModuleTest(String arg0) {
        super(arg0);
    }
    
    private Config globalConfig;
    private Config moduleConfig;
    private String moduleName;
    private String logFileName="loggerModule.out";
    private String tmpDir;

    public void setUp() {
        filterOutputRegex("[0-9 ]*(parsing|Using|impl:|not including|logging|No FS|Plugins|status|[a-z\\.]* = ).*");
        tmpDir = FileUtil.createTempDir("loggermoduletest",".tmp").getAbsolutePath();
        globalConfig = Config.getConfig("crawler.properties");
        moduleName = "logger";
        moduleConfig = Config.getConfig(moduleName + "Module.properties");
        moduleConfig.set("attributes.to.log","*");
        moduleConfig.set("tags.to.log","*");
        moduleConfig.set("categories.to.log","*");
        moduleConfig.set("log.text","false");
        moduleConfig.set("log.emmited","false");
        moduleConfig.set("log.file.name",tmpDir+"/"+logFileName);
    }
    
    public void tearDown(){
        FileUtil.deleteFile(tmpDir);
    }
    
    private boolean checkTagOrCat(String val, Set<String> set, boolean isATag){
        String what= isATag? LoggerModule.TAGS_STR:LoggerModule.CAT_STR;
        for (String ln: set){
            if (ln.startsWith(what)){
                return ln.matches(".* " + val + " , .*");                
            }
        }
        return false;
    }
    
    private int countTagOrCat(Set<String> set,boolean isATag){
        String what= isATag? LoggerModule.TAGS_STR:LoggerModule.CAT_STR;
        for (String ln: set){
            if (ln.startsWith(what)){
                return ln.split(",").length -1;                
            }
        }
        return 0;
    }
    

    private boolean checkAttribs(String attr, String val, Set<String> set){
        for (String ln: set){
            if (ln.startsWith(LoggerModule.ATTR_STR)){
                return ln.matches(".* " + attr + "=" + val + " , .*");                
            }
        }
        return false;
    }
    
    @SuppressWarnings("unused")
    private int countAttribs(Set<String> set){
        for (String ln: set){
            if (ln.startsWith(LoggerModule.ATTR_STR)){
                return ln.split(",").length -1;                
            }
        }
        return 0;
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testInternalProcess() throws Exception {
        LoggerModule lm= new LoggerModule(moduleName,globalConfig);
        Page page= new Page("http://loggerModuleTest.url.com", 0);
        FetchDocument doc= new FetchDocument(page);
        doc.addAttribute("attr1", "val1");
        doc.addTag("tag1");
        doc.addCategory("cat1");        
        lm.internalProcess(doc);

        Set<String> set= new HashSet<String>(); 
        FileUtil.fileToSet(tmpDir, logFileName, set);
        assertEquals(1, countTagOrCat(set, true));
        assertTrue(checkTagOrCat("tag1", set,true));
        assertEquals(1, countTagOrCat(set,false));
        assertTrue(checkTagOrCat("cat1", set, false));
        // not countig as some attribs are added by default (boost=1.0)
        //assertEquals(1, countAttribs(set));        
        assertTrue(checkAttribs("attr1", "val1", set));

    }    
}
