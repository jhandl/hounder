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
package com.flaptor.hounder.crawler.pagedb;

import java.io.IOException;
import java.util.Iterator;

/**
 * This interface defines the page storage service.
 * @author Flaptor Development Team
 */
public interface IPageStore extends Iterable<Page> {

    /** 
     * Add a page to the store. 
     * @param page the page to add.
     */
    public void addPage(Page page) throws IOException;


    /** 
     * Get an iterator of the stored pages. 
     * @return an iterator of the stored pages.
     */
    public Iterator<Page> iterator();

} 

