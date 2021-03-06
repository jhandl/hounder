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

import java.util.Set;

import junit.framework.Assert;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;
import com.flaptor.util.TestUtils;

/**
 * This module is used to test the {@link ATrueFalseModule}
 * @author Flaptor Development Team
 */
public class MockTrueModule extends ATrueFalseModule {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    /**
     * This tag will be set iff internalProcess was called
     */ 
    public static final String TAG_MOCKTRUE_INTERNAL_PROCESS_WAS_CALLED= 
        "tag_mocktrue_internal_process_was_called";

    private String echoMessage;
        
    public MockTrueModule (String name) throws Exception {
        super(name, TestUtils.getConfig());
    }
    
    public Set<String> getSetTag(){
        return onTrueSetTags;        
    }

    public Set<String> getUnsetTag(){
        return onTrueUnSetTags;        
    }

    @Override
    protected Boolean tfInternalProcess(FetchDocument doc) {
        logger.debug(moduleName +" " + echoMessage);
        try {
            doc.addTag(TAG_MOCKTRUE_INTERNAL_PROCESS_WAS_CALLED);
        } catch (Exception e) {            
            e.printStackTrace();
            Assert.fail("Unexpected exception");
        }
        return true;
    }


    
}
