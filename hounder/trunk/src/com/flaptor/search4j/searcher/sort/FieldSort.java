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
package com.flaptor.search4j.searcher.sort;

import java.util.LinkedList;
import java.util.List;
import java.util.Comparator;

import org.apache.lucene.document.Document;

import org.apache.lucene.search.SortField;


/**
 * ResultSet sort criteria.
 * This class contains the information to sort the results of a search by string, float
 * or int fields.
 * @author Flaptor Development Team
 */
public final class FieldSort extends ASort {
	private final String fieldName;
	private final OrderType orderType;

	/**
	 * Constructor.
	 * @param fieldName the name of the indexed field to use for sorting.
	 * @param reversed if true, the sort order is reversed.
	 * @param orderType the type of order to apply. It will depend on the data expected to be
	 * 		found in	the specified field.
	 */
	public FieldSort(final boolean reversed, final String fieldName, final OrderType orderType) {
		this(reversed, fieldName, orderType, null);
	}

	/**
	 * Constructor.
	 * @param fieldName the name of the indexed field to use for sorting.
	 * @param reversed if true, the sort order is reversed.
	 * @param orderType the type of order to apply. It will depend on the data expected to be
	 * 		found in	the specified field.
	 * @param baseSort a sort to use in case of two documents that are equally ranqued according
	 * 		to this Sort.
	 */
	public FieldSort(final boolean reversed, final String fieldName, final OrderType orderType, final ASort baseSort) {
        super(reversed, baseSort);
		this.fieldName = fieldName;
		this.orderType = orderType;
	}

	/**
	 * Returns a SortField list.
	 * @return the list of sortFields that represent all the chain of filters this Sort implements.
	 */
    @Override
	protected List<SortField> getSortFields() {
		List<SortField> list;
		if (null != baseSort) {
			list = baseSort.getSortFields();
		} else {
			list = new LinkedList<SortField>();
		}
		list.add(0, getSortField());
		return list;
	}

	/**
	 * Return a single SortField.
	 * @return a single Sort field that represents this single sort, but no all the ones chained
	 *		after it.
	 */
	private SortField getSortField() {
		SortField sf = null;
		switch (orderType) {
			case FLOAT:
				sf = new SortField(fieldName, SortField.FLOAT, reversed.booleanValue());
				break;
			case STRING:
				sf = new SortField(fieldName, SortField.STRING, reversed.booleanValue());
				break;
			case INT:
				sf = new SortField(fieldName, SortField.INT, reversed.booleanValue());
				break;
		}
		return sf;
	}
	
    @Override
	public boolean equals(final Object obj)
	{
	    if(this == obj)
	        return true;
	    if((obj == null) || (obj.getClass() != this.getClass()))
	        return false;
	    FieldSort f = (FieldSort) obj;
	    return fieldName.equals(f.fieldName) && super.equals(obj);
    }
	
    @Override
	public int hashCode()
	{
	    int hash = 7;
	    hash = 31 * hash + orderType.hashCode();
        hash = 31 * hash + fieldName.hashCode();
        hash = 31 * hash + super.hashCode();
	    return hash;
	}
   
    public Comparator<Document> getComparator() {
        return new FieldSortDocumentComparator(this);
    }

	//========================================================================================
	//INTERNAL CLASSES
	public static enum OrderType { FLOAT, INT, STRING};


    private static class FieldSortDocumentComparator implements Comparator<Document> {

        private final FieldSort sort;
        public FieldSortDocumentComparator(FieldSort sort) {
            this.sort = sort;
        }

        public int compare(Document d1, Document d2) {
            if (null == d1 && null == d2) return 0;
            if (null == d1 ) return 1;
            if (null == d2 ) return -1;

            String str1 = d1.get(sort.fieldName);
            String str2 = d2.get(sort.fieldName);

            if (null == str1 && null == str2) return 0;
            if (null == str1) return 1;
            if (null == str2) return -1;

            int myComparation = 0;

            if (sort.orderType == OrderType.FLOAT) {
                try {
                    Float f1 = Float.parseFloat(str1);
                    Float f2 = Float.parseFloat(str2);
                    myComparation = f1.compareTo(f2);
                } catch (NumberFormatException nfe) {
                    throw new RuntimeException(nfe);
                }
            }

            if (sort.orderType == OrderType.INT) {
                try {
                    Integer i1 = Integer.parseInt(str1);
                    Integer i2 = Integer.parseInt(str2);
                    myComparation = i1.compareTo(i2);
                } catch (NumberFormatException nfe) {
                    throw new RuntimeException(nfe);
                }
            }

            if (sort.orderType == OrderType.STRING) {
                myComparation = str1.compareTo(str2);
            }

            if (myComparation == 0 && null != sort.baseSort && sort.baseSort instanceof FieldSort) {
                myComparation = sort.baseSort.getComparator().compare(d1,d2);
            }

            if (!sort.reversed) {
                myComparation = myComparation * -1;
            }

            return myComparation;
        }
    }


	
}

