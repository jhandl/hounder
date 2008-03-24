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
 * @author Flaptor Development Team
 */
public class AttributeBoostValue extends ABoostValue {
  
    private final String field;

    public AttributeBoostValue(Config config) {
        super(config);
        field = config.getString("value.attribute.name");
    }


    public double getValue(FetchDocument doc) {
        // First, check standard attributes
        Object o = doc.getAttribute(field);
        if (o != null && o instanceof Number) return (Float)o;

        // If it was not found, check on indexable attributes
        o = doc.getIndexableAttribute(field);
        if (o != null && o instanceof Number) return (Float)o;

        // asked something that is not present.
        throw new RuntimeException("Attribute " + field + " not found on doc. Did you check hasValue first?");
    }

    public boolean hasValue(FetchDocument doc) {
        // First, check standard attributes
        Object o = doc.getAttribute(field);
        if (o != null && o instanceof Number) return true;

        // If it was not found, check on indexable attributes
        o = doc.getIndexableAttribute(field);
        if (o != null && o instanceof Number) return true;

        // default
        return false;
    }

}

