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
package com.flaptor.search4j.classifier.bayes;

import java.io.Serializable;

/**
 * This class holds different token counters for a single category.
 * The following counters are instrumented:<br />
 * <ul>
 *   <li>count: total number of times the token appeared in the documents
 *   <li>countUnique: total number of documents the token appeared in
 * </ul>
 * <br />
 * This implementation is not thread safe!
 * @author Flaptor Development Team
 * 
 */
public class TokenCounter implements Serializable {

    private static final long serialVersionUID = 1L;
    
    // Count of token ocurrencies
    private long count = 0L;
    // Count of documents containing the token at least once
    private long countUnique = 0L;

    // Default counstructor
    public TokenCounter() { }

    /**
     * Updates the counters for a document.
     * This method must be called once by document.  It increments by one 
     * the unique counter in each invocation.
     *
     * @param toAdd the number of token occurrencies in the document
     */
    public void update(int toAdd) {
        if (toAdd == 0 ) return;
        count += toAdd;
        countUnique ++;
    }

    /**
     * Returns the current value of the countUnique counter.
     */
    public long getCountUnique() {
        return countUnique;
    }

    /**
     * Returns the current value of the count counter.
     */
    public long getCount() {
        return count;
    }

    /**
     * Returns a string representation of the values, suitable for logging.
     */
    public String toString() {
        return "unique: " +countUnique+ ", total: " +count;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TokenCounter) {
            TokenCounter otherTokenCounter = (TokenCounter)other;
            return ((otherTokenCounter.count == count) && (otherTokenCounter.countUnique == countUnique));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 17*(int)count + 29*(int)countUnique;
    }

}

