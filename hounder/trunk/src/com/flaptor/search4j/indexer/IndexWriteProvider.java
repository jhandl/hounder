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
package com.flaptor.search4j.indexer;

import org.apache.lucene.document.Document;

/**
 * This interface abstracts the functionality to write to a lucene index.
 * @author Flaptor Development Team
 */
interface IndexWriteProvider {
    /**
     * Adds the provided Lucene Document to the index.
     * 
     * @param doc
     *            the document to add to the index
     */
    void addDocument(Document doc);

    /**
     * "Deletes" (probably schedules a delete) of all the documents matching the
     * id in the id field.
     * 
     * @param id
     *            the id of the document to delete.
     */
    void deleteDocument(String id);
}
