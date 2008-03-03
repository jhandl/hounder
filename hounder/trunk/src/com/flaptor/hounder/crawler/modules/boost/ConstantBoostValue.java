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
package com.flaptor.search4j.crawler.modules.boost;

import com.flaptor.search4j.crawler.modules.FetchDocument;
import com.flaptor.util.Config;

/**
 * Gives a constant boost value
 * 
 * @author Flaptor Development Team
 */
public class ConstantBoostValue extends ABoostValue {

    private final double boostValue;

    public ConstantBoostValue(Config config) {
        super(config);
        boostValue = config.getFloat("value.constant");
    }

    public boolean hasValue(FetchDocument doc) {
        return true;
    }

    public double getValue(FetchDocument doc) {
        return boostValue;
    }

}
