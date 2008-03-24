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
package com.flaptor.hounder.crawler.modules.boost;

import com.flaptor.hounder.crawler.modules.FetchDocument;
import com.flaptor.util.Config;

/**
 * This boost value should be used when the value comes with the condition.
 * @author Flaptor Development Team
 */
public class NoBoostValue extends ABoostValue {
  
    public NoBoostValue (Config config) {
        super(config);
    }

    public double getValue(FetchDocument doc) {
        // asked something that is not present.
        throw new RuntimeException("NoBoostValue has no value. Did you check hasValue first?");
    }

    public boolean hasValue(FetchDocument doc) {
        return false;
    }

}

