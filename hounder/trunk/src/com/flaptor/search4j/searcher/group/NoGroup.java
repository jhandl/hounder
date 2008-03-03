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


/**
 * @author Flaptor Development Team
 */
@SuppressWarnings("serial")
public class NoGroup extends AGroup {
    
    public NoGroup() {
    }

    public AResultsGrouper getGrouper(DocumentProvider provider){
        return new SimpleResultsGrouper(provider);
    }
    
    public boolean equals(Object obj) {
        return obj instanceof NoGroup;
    }

    public int hashCode() {
        return 0;
    }
}
