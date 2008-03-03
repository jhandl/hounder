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

import java.util.Vector;

import org.apache.lucene.document.Document;

import com.flaptor.util.Pair;
import com.flaptor.util.TextSignature;

/**
 * @author Flaptor Development Team
 */
public class TextSignatureResultsGrouper extends AResultsGrouper {

    private Vector<TextSignature> signatures = null;
    private float threshold = 0.95f ; //TODO FIXME hardcoded
    private String criteria;

    public TextSignatureResultsGrouper(DocumentProvider provider, String criteria){
        super(provider);
        this.criteria = criteria;
        signatures = new Vector<TextSignature>();
    };



    protected Pair<String,Integer> findGroup(Document doc) {
        
        TextSignature signature = new TextSignature(doc.get("text"));

        if (signatures.size() == 0 ) {
            signatures.add(signature);
            return new Pair<String,Integer>(criteria,0);
        }

        // else, check from back to front, against all head signatures
        for (int i = signatures.size() - 1; i >= 0 ; i--) {

            if (signatures.get(i).compareTo(signature) > threshold) {
                return new Pair<String,Integer>(criteria,i);
            }
        }

        // if we got here, none matched. insert it at the tail
        signatures.add(signature);
        return new Pair<String,Integer>(criteria,signatures.size() - 1);
    }
}
