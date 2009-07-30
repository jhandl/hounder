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
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Execute;
import java.util.Enumeration;
import org.apache.log4j.Appender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LevelMatchFilter;

/**
 * A searcher for registering search queries
 * 
 * @author jhandl
 */
public class LoggingSearcher implements ISearcher {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	private ISearcher searcher;
	
	public LoggingSearcher(ISearcher searcher) {
        this.searcher = searcher;

        // We add a special logger to write to the file the log consumers expect to find.
        DailyRollingFileAppender appender = new DailyRollingFileAppender();
        appender.setName("QueriesAppender");
        appender.setFile("logs/queries.log");
        appender.setDatePattern("'.'yyyy-MM-dd");
        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern("%m%n");
        appender.setLayout(layout);
        appender.activateOptions();
        logger.setLevel(Level.INFO);    
        logger.addAppender(appender);
        logger.setAdditivity(true);
        
        // We need to filter this class out of any root appenders, otherwise the queries are logged through all of them.
        ExcludeClassFilter filter = new ExcludeClassFilter(Execute.whoAmI());
        for (Enumeration appenders = Logger.getRootLogger().getAllAppenders(); appenders.hasMoreElements();) {
            Appender rootAppender = (Appender)appenders.nextElement();
            rootAppender.addFilter(filter);
        }
        
    }

    private class ExcludeClassFilter extends Filter {
        private String className = null;
        public ExcludeClassFilter(String className) {
            this.className = className;
        }
        @Override
        public int decide(LoggingEvent event) {
            if (className.equals(event.getLoggerName())) {
                return Filter.DENY;
            }
            return Filter.NEUTRAL;
        }
    }
    

    @Override
    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort) throws SearcherException{
        GroupedSearchResults results = searcher.search(query, firstResult, count, group, groupSize, filter, sort);
        if (null != results && results.totalResults() > 0) {
            LazyParsedQuery lazyParsedQuery = LazyParsedQuery.findLazyParsedQuery(query);
            if (null != lazyParsedQuery) {
                logger.info(lazyParsedQuery.getQueryString().toLowerCase().trim());
            }
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
