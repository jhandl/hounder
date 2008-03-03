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
 * @author Flaptor Development Team
 */
public abstract class ABoostMethod extends ABoostModule {
   
    public static enum Type  { Doc, Field, Keyword};
    

    public ABoostMethod(Config config) {
        super(config);
    }

    public static ABoostMethod getBoostMethod (Config config, Type type) {
        ABoostMethod method = null;
        switch (type) {
            case Doc:
                method = new DocBoostMethod(config);
                break;
            case Field:
                method = new FieldBoostMethod(config);
                break;
            case Keyword:
                method = new KeywordBoostMethod(config);
                break;
        }
        if (null == method) {
            throw new IllegalArgumentException("Unknown boost method: "+type);
        }

        return method;
    }

    public abstract void applyBoost (FetchDocument doc, double value);
}

