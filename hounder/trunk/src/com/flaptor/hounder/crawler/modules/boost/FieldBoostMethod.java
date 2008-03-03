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

import java.util.HashMap;
import java.util.Map;

import com.flaptor.search4j.crawler.modules.FetchDocument;
import com.flaptor.util.Config;

/**
 * Boosts a field, or group of fields, changing its boost value
 * 
 * @author Flaptor Development Team
 */
public class FieldBoostMethod extends ABoostMethod {
    
    private final String[] fields;

    public FieldBoostMethod(Config config) {
        super(config);
        fields = config.getStringArray("field.boost.method.fields");
    }


    /**
     * Multiplies boost of each of the fields by "value".
     */
    @SuppressWarnings("unchecked")
    public void applyBoost(FetchDocument doc, double value) {
        Map<String,Double> boostMap = (Map<String,Double>)doc.getAttribute("field_boost");
        if (null == boostMap) {
            boostMap = new HashMap<String,Double>();
        }
        
        for (String field: fields) {
            Double oldBoost = boostMap.get(field);
            if (null == oldBoost) oldBoost = 1.0d;
            boostMap.put(field,oldBoost*value);
        }

        doc.setAttribute("field_boost",boostMap);
    }

}

