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
package com.flaptor.hounder.searcher.sort;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.SortField;

/**
 * Sort that sorts by the searche's score.
 * This is the default behavior when no sort is used, but it may be useful in combination with
 * other sorts.
 * @author Flaptor Development Team
 */
public final class ScoreSort extends ASort {
	
	public ScoreSort() {
		//This class won't need any information at all
		super(false, null);
	}

	public ScoreSort(final ASort baseSort) {
		//This class won't need any information but the base sort
		super(false, baseSort);
        throw new RuntimeException("not implemented");
	}

    @Override
    protected List<SortField> getSortFields() {
        List<SortField> list;
        if (null != baseSort) {
            list = baseSort.getSortFields();
        } else {
            list = new LinkedList<SortField>();
        }
        list.add(SortField.FIELD_SCORE);
        return list;
	}


    @Override
    public Comparator<Document> getComparator() {
        return null;
    }
}

