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
package com.flaptor.hounder.searcher;

import org.apache.log4j.Logger;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Statistics;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;

/**
 * A searcher for taking query times, and print individual query statistics
 * 
 * @author Martin Massera, Spike, DButhay
 */
public class LoggingSearcher implements ISearcher {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	private ISearcher searcher;
	
	public LoggingSearcher(ISearcher searcher) {
		this.searcher = searcher;
//        PropertyConfigurator.configureAndWatch("loggingsearcher.properties",60000);
        Properties props = new Properties();
        props.setProperty("log4j.rootLogger","INFO,rolling");
        props.setProperty("log4j.appender.rolling","org.apache.log4j.DailyRollingFileAppender");
        props.setProperty("log4j.appender.rolling.File","logs/queries.log");
        props.setProperty("log4j.appender.rolling.DatePattern","'.'yyyy-MM-dd");
        props.setProperty("log4j.appender.rolling.layout","org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.rolling.layout.ConversionPattern","%m");
        PropertyConfigurator.configure(props);
	}

	public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort)  throws SearcherException {
		GroupedSearchResults results = searcher.search(query, firstResult, count, group, groupSize, filter, sort);
        if (null != results && results.totalResults() > 0) {
            logger.info(query.toString());
        }
		return results;		
	}

    @Override
    public void requestStop() {
        searcher.requestStop();
    }

    @Override
    public boolean isStopped() {
        return searcher.isStopped();
    }

}
