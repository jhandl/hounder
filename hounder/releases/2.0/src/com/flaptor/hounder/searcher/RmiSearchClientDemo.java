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

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.group.StoredFieldGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.remote.RpcException;


/*
 A minimalistic example about how to use a remote Searcher in java
*/

public class RmiSearchClientDemo {

    public static void main(String[] args) throws Exception {
        String host = args[0];
        String query = args[1];
        String groupByField = args[2];

        int basePort = 47000; // baseport of the search server. default value is 47000.

        IRemoteSearcher searcher = new RmiSearcherStub(basePort, host);


        //The simplest example.
        System.out.println("Simple search results:\n" + searcher.search(new LazyParsedQuery(query), 0, 10, new NoGroup(), 1, null, null).toString());


        //The same example, with some error management
        GroupedSearchResults gsr = null;
        try {
            gsr = searcher.search(new LazyParsedQuery(query), 0, 10, new NoGroup(), 1, null, null);
        } catch (RpcException e) {
            System.err.println("Error while executing the search." + e);
            System.exit(-1);
        }
        System.out.println("Search successfull.");
        System.out.println(gsr.toString());


        //Now the same example with grouping:
        AGroup group = new StoredFieldGroup(groupByField);
        System.out.println("Simple search successful.\n" + searcher.search(new LazyParsedQuery(query), 0, 10, group, 3, null, null).toString()); //up to 3 consecutive results within the same group.
    }
}
