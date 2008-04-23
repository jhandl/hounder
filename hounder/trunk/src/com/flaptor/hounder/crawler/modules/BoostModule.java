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

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.modules.boost.ABoostCondition;
import com.flaptor.hounder.crawler.modules.boost.ABoostMethod;
import com.flaptor.hounder.crawler.modules.boost.ABoostValue;
import com.flaptor.hounder.crawler.modules.boost.Booster;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * @todo generate categoryBoost
 * @author Flaptor Development Team
 */
public class BoostModule extends AProcessorModule {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());

    private final Booster booster;
    private final ABoostMethod method;
    private final ABoostValue value;
    private final ABoostCondition condition;

    public BoostModule (String moduleName, Config globalConfig) {
        super(moduleName, globalConfig);
            ABoostCondition.Type conditionType = null ;
            Config mdlConfig = getModuleConfig();
            String conditionString = mdlConfig.getString("boost.condition");
            if ("url".equals(conditionString)) conditionType = ABoostCondition.Type.UrlPattern;
            if ("keyword".equals(conditionString)) conditionType = ABoostCondition.Type.Keyword;
            if ("urlandkeyword".equals(conditionString)) conditionType = ABoostCondition.Type.UrlPatternAndKeyword;
            condition = ABoostCondition.getBoostCondition(mdlConfig,conditionType);

            ABoostMethod.Type methodType = null;
            String methodString = mdlConfig.getString("boost.method");
            if ("doc".equals(methodString)) methodType = ABoostMethod.Type.Doc;
            if ("field".equals(methodString)) methodType = ABoostMethod.Type.Field;
            if ("keyword".equals(methodString)) methodType = ABoostMethod.Type.Keyword;
            method = ABoostMethod.getBoostMethod(mdlConfig,methodType);

            ABoostValue.Type valueType = null;
            valueType = ABoostValue.Type.None;
            String valueString = mdlConfig.getString("boost.value");
            if ("constant".equals(valueString)) valueType = ABoostValue.Type.Constant;
            if ("attribute".equals(valueString)) valueType = ABoostValue.Type.Attribute;
            value = ABoostValue.getBoostValue(mdlConfig,valueType);

            booster = new Booster(condition,value,method);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void internalProcess (FetchDocument doc) {    
        
        booster.applyBoost(doc);

    }

}
