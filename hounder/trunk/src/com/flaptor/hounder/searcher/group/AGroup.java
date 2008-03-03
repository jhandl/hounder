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
package com.flaptor.search4j.searcher.group;

import java.io.Serializable;

/**
 * Represents a grouping scheme for results.
 * NOTE: subclasses must implement equals and hashCode
 * @author Flaptor Development Team
 */
public abstract class AGroup implements Serializable{

    /**
     * Gets a grouper, that groups results according to the criteria of 
     * this group, to group parameters.
     *
     * @param provider
     *          A DocumentProvider, to retrieve documents to group.
     *
     * @return An AResultsGrouper that can group parameters using 
     *         this AGroup's criteria.
     *
     */
    public abstract AResultsGrouper getGrouper(DocumentProvider provider);

    public boolean equals(Object obj) {
        throw new RuntimeException("unimplemented method");
    }

    public int hashCode() {
        throw new RuntimeException("unimplemented method");
    }
}
