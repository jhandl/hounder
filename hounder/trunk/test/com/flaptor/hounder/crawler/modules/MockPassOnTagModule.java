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
 * This module is used to test the AProcessorModule
 * @author Flaptor Development Team
 */
public class MockPassOnTagModule extends AProcessorModule {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private String echoMessage;
    
    Set<String> getPassThroughOnTags(){
        return passThroughOnTags;
    }
    
    public MockPassOnTagModule (String name) throws Exception {
        super(name, TestUtils.getConfig());
    }
    
    public void internalProcess (FetchDocument doc) {
        logger.debug(moduleName +" " + echoMessage);
        Assert.fail("I should have not been called because of passThroughOnTags");
    }

}
