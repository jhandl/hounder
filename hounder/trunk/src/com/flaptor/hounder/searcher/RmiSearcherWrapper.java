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

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Execute;

/**
 * wraps a searcher into an RMI Searcher
 * @author Flaptor Development Team
 */
public class RmiSearcherWrapper implements IRmiSearcher {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	private ISearcher searcher;

	public RmiSearcherWrapper(ISearcher searcher) {
		this.searcher = searcher;
	}

	public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort) throws RemoteException {
		try {
			return searcher.search(query, firstResult, count, group, groupSize, filter, sort);
		} catch (Exception e) {
            logger.error("search: exception caught. I will re-throw it as a RemoteException. Original exception is: ", e);
			throw new RemoteException("Exception on remote searcher", e);
		}
	}
}
