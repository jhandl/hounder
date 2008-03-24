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

/**
 * @author Flaptor Development Team
 */
@SuppressWarnings("serial")
public class TextSignatureGroup extends AGroup {
    
    private final String fieldName;

    public TextSignatureGroup() {
        this("text");
    }
    public TextSignatureGroup(String field) {
        this.fieldName = field;
    }

    public AResultsGrouper getGrouper(DocumentProvider provider){
        return new TextSignatureResultsGrouper(provider,fieldName);
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof TextSignatureGroup)) return false;
        TextSignatureGroup object = (TextSignatureGroup) obj;
        return object.fieldName.equals(fieldName);
    }

    public int hashCode() {
        return fieldName.hashCode();
    }
}
