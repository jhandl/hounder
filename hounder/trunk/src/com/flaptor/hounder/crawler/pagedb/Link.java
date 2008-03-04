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


/** 
 * This class represents a web page link, whith its target url and associated anchor.
 * @author Flaptor Development Team
 */
public class Link { 
    
    private String url;
    private String anchor;
    
    /**
     * Link constructor.
     */
    public Link (String url, String anchor) {
        setUrl(url);
        setAnchor(anchor);
    }

    /**
     * Set the url part of this link.
     */
    public void setUrl (String url) {
        this.url = url;
    }

    /**
     * Set the anchor part of this link.
     */
    public void setAnchor (String anchor) {
        this.anchor = anchor;
    }

    /**
     * Return the url in this link.
     * @return the url in this link.
     */
    public String getUrl () {
        return url;
    }

    /**
     * Return the anchor in this link.
     * @return the anchor in this link.
     */
    public String getAnchor () {
        return anchor;
    }

    /**
     * Return a string representation of this link.
     */
    public String toString () {
        return "Link(url=" + getUrl() + ",anchor=" + getAnchor() + ")";
    }

}

