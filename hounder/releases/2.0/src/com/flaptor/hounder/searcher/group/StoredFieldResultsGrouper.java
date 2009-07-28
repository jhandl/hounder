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
package com.flaptor.hounder.searcher.group;

import java.util.Vector;

import org.apache.lucene.document.Document;

import com.flaptor.util.Pair;

/**
 * @author Flaptor Development Team
 */
public class StoredFieldResultsGrouper extends AResultsGrouper {

    private Vector<String> values = null;
    private String criteria;

    public StoredFieldResultsGrouper(DocumentProvider provider, String criteria){
        super(provider);
        this.criteria = criteria;
        values = new Vector<String>();
    };



    protected Pair<String,Integer> findGroup(Document doc) {
        String fieldValue = doc.get(criteria);
        if (values.size() == 0 ) {
            values.add(fieldValue);
            return new Pair<String,Integer>(fieldValue,0);
        }

        // else, check from back to front, against all head signatures
        for (int i = values.size() - 1; i >= 0 ; i--) {

            if (values.get(i).equals(fieldValue)) {
                return new Pair<String,Integer>(fieldValue,i);
            }
        }

        // if we got here, none matched. insert it at the tail
        values.add(fieldValue);
        return new Pair<String,Integer>(fieldValue,values.size() - 1);
    }
}
