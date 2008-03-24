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
 * A composite of two boost conditions, where both must be true for the composite to be true.
 * 
 * @author Flaptor Development Team
 */
public class CompositeBoostCondition extends ABoostCondition {

    private ABoostCondition cond1, cond2;

    public CompositeBoostCondition (ABoostCondition cond1, ABoostCondition cond2) {
        super((Config)null);
        this.cond1 = cond1;
        this.cond2 = cond2;
    }

    public boolean eval (FetchDocument doc) {
        return cond1.eval(doc) && cond2.eval(doc);
    }

    public boolean hasValue (FetchDocument doc) {
        return cond1.hasValue(doc) || cond2.hasValue(doc);
    }

    public double getValue (FetchDocument doc) {
        double val = 1;
        if (cond1.hasValue(doc)) {
            val = cond1.getValue(doc);
        } else if (cond2.hasValue(doc)) {
            val = cond2.getValue(doc);
        } else {
            throw new IllegalStateException("Trying to get a value from a composite boost condition: None of the conditions can return a value.");
        }
        return val;
    }

}

