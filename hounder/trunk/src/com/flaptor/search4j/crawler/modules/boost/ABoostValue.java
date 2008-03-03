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
public abstract class ABoostValue extends ABoostModule{

    public static enum Type {None, Constant, Attribute};
    

    public ABoostValue(Config config) {
        super(config);
    }

    public static ABoostValue getBoostValue (Config config, Type type) {
        ABoostValue value = null;
        switch (type) {
            case None:
                value = new NoBoostValue(config);
                break;
            case Constant:
                value = new ConstantBoostValue(config);
                break;
            case Attribute:
                value = new AttributeBoostValue(config);
                break;
        }
        if (null == value) {
            throw new IllegalArgumentException("Unknown boost value: "+type);
        }

        return value;
    }
    public abstract double getValue (FetchDocument doc);
    public abstract boolean hasValue (FetchDocument doc);
}
