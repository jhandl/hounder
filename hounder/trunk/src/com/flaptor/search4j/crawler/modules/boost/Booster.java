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

/**
 * @author Flaptor Development Team
 */
public class Booster {
    private ABoostCondition condition;
    private ABoostValue value;
    private ABoostMethod method;
    public Booster (ABoostCondition condition, ABoostValue value, ABoostMethod method) {
        this.condition = condition;
        this.value = value;
        this.method = method;
    }

    public void applyBoost (FetchDocument doc) {
        boolean needed = condition.eval(doc);
        if (needed) {
            double val = 1;
            if (value.hasValue(doc)) {
                val = value.getValue(doc);
            } else if (condition.hasValue(doc) ) {
                val = condition.getValue(doc);
            }
            method.applyBoost(doc,val);
        }
    }


    public ABoostCondition getBoostCondition () {
        return condition;
    }
    public ABoostValue getBoostValue () {
        return value;
    }
    public ABoostMethod getBoostMethod () {
        return method;
    }
}
