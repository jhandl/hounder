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
package com.flaptor.hounder.crawler.modules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.Link;
import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.util.HtmlParser;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.flaptor.util.TextSignature;

/**
 * 
 * @author Flaptor Development Team
 */
public class FetchDocument {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private Page page;
    private HashSet<String> tags;
    private HashMap<String,Object> attributes;
    private HashMap<String,Object> indexableAttributes;
    private HashSet<String> categories;
    private static HtmlParser parser= null;

    static{
        Config conf = Config.getConfig("crawler.properties");
        String removedXPathElements = conf.getString("HtmlParser.removedXPath");
        String[] separatorTags = conf.getStringArray("HtmlParser.separatorTags");
        parser = new com.flaptor.hounder.util.HtmlParser(removedXPathElements,separatorTags);
    }

    private String origUrl;
    private String text;
    private String title;
    private Link[] links;
    private boolean success;
    private boolean recoverable;
    private boolean changed; // fetcher informs the server told it the page changed and has thus been refetched.
    private boolean textChanged; // the page text has significantly changed (according to the text signature).
    private byte[] content;
    private Map<String,String> header;
    private boolean alreadyParsed;


    public FetchDocument(Page page, String origUrl, String text, String title, Link[] links, byte[] content, Map<String,String> header, boolean success, boolean recoverable, boolean changed) {
        this(page);
        this.origUrl = origUrl;
        this.text = text;
        this.title = title;
        this.links = links;
        this.content = content;
        this.header = header;
        this.success = success;
        this.recoverable = recoverable;
        this.changed = changed;
        this.alreadyParsed = true;
        checkTextChanges();
    }


    // LAZY PARSE CONSTRUCTOR
    public FetchDocument(Page page, String origUrl, byte[] content, Map<String,String> header, boolean success, boolean recoverable, boolean changed) {
        this(page);
        this.origUrl = origUrl;
        this.content = content;
        this.header = header;
        this.success = success;
        this.recoverable = recoverable;
        this.changed = changed;
        this.alreadyParsed = false;
    }


    public FetchDocument (Page page) {
        this.page = page;
        tags = new HashSet<String>();
        categories = new HashSet<String>();
        attributes = new HashMap<String,Object>();
        indexableAttributes = new HashMap<String,Object>();
        attributes.put("boost",1d);
        page.setLastAttempt(System.currentTimeMillis());
    }

    public Page getPage () {
        return page;
    }

    public String getMimeType () {
        return "text/html";
    }

    private void parse () {
        try {
            HtmlParser.Output out = parser.parse(page.getUrl(), new String(content,getEncoding()));
            this.text = out.getText();
            this.title = out.getTitle();
            List<Pair<String,String>> ol = out.getLinks();
            links = new Link[ol.size()];
            int i = 0;
            for (Pair<String,String> lnk : ol) {
                links[i++] = new Link(lnk.first(), lnk.last());
            }
            checkTextChanges();
        } catch (Exception e) {
            logger.error("Parsing html content",e);
        }
        alreadyParsed = true;
    }

    /**
     * Determine if the page changed by analysing the text signature.
     */
    private void checkTextChanges() {
        textChanged = false;
        if (null != text) {
            TextSignature newSig = new TextSignature(text);
            textChanged = ! page.hasSimilarSignature(newSig);
            page.setSignature(newSig);
            if (textChanged) {
                page.setLastChange(System.currentTimeMillis());
            }
        }
    }


    public String getTitle () {
        if (!alreadyParsed) parse();
        return title;
    }   

    public String getTitle (int lengthLimit) {
        String tmpTitle = getTitle();
        if (null != tmpTitle && tmpTitle.length() > lengthLimit) {
            tmpTitle = tmpTitle.substring(0,lengthLimit);
        }
        return tmpTitle;
    }

    public String getText () {
        if (!alreadyParsed) parse();
        return text;
    }

    public String getText (int lengthLimit) {
        String tmpText = getText();
        if (null != tmpText && tmpText.length() > lengthLimit) {
            tmpText = tmpText.substring(0,lengthLimit);
        }
        return tmpText;
    }

    public Link[] getLinks () {
        if (!alreadyParsed) parse();
        return links;
    }

    public Map<String,String> getHeader () {
        return header;
    }

    public byte[] getHeaderBytes(){
        StringBuffer buf = new StringBuffer();
        for (String key: header.keySet()) {
            buf.append(key + ": " + header.get(key) + "\n");
        }
        return buf.toString().getBytes();

    }

    public byte[] getContent () {
        return content;
    }

    public String getEncoding () {
        String encoding = null;
        // find charset. http headers usually have a Content-Type line, but
        // as it may not be in the same case, all headers are stored lowercased.
        // Content-Type lines contain mime-type and charset, separated by ;
        // for example
        // Content-Type: text/html; charset=UTF-8
        if (header.containsKey("content-type")) {
            String[] tokens = header.get("content-type").split(";");
            for (String token: tokens) {
                if (token.toLowerCase().contains("charset") && token.contains("=")){
                    encoding = token.split("=")[1].trim().toUpperCase();
                    break;
                }
            }
        }
        // if not found, use default encoding
        if (null == encoding) {
            encoding = java.nio.charset.Charset.defaultCharset().name();
        }
        return encoding;
    }

    public String getLastModified () {
        if (null == header) return null;
        // Values on header are all lowercased
        return header.get("lastmodified");
    }

    public String getLastUpdated () {
        if (null == header) return null;
        // Values on header are all lowercased
        return header.get("lastupdated");
    }

    public String getOriginalUrl () {
        return origUrl;
    }

    public boolean success () {
        return success;
    }

    public boolean recoverable () {
        return recoverable;
    }

    public boolean pageChanged () {
        return changed;
    }

    public boolean pageTextChanged () {
        if (!alreadyParsed) parse();
        return textChanged;
    }


    /** GETTERS AND SETTERS FOR MODULES */

    public void setTag (String name) {
        tags.add(name);
    }

    public void addTag (String name) throws Exception {
        if (hasTag(name)) {
            throw new Exception("Tag " + name + " already set");
        } else {
            setTag(name);
        }
    }

    public void delTag (String name) {
        tags.remove(name);
    }

    public boolean hasTag (String name) {
        return tags.contains(name);
    }

    /** Get a set of tags. Modifying this set does not modify document. */
    public Set<String> getTags() {
        return new HashSet<String>(tags);
    }


    public void setAttribute (String name, Object value) {
        attributes.put(name, value);
    }

    public void addAttribute (String name, Object value) throws Exception {
        if (null != getAttribute(name)) {
            throw new Exception("Attribute " + name + " already set");
        } else {
            setAttribute(name, value);
        }
    }

    /** Get a map of attributes . Modifying this map does not modify document. */
    public Map<String,Object> getAttributes() {
        return new HashMap<String,Object>(attributes);
    }


    public Object getAttribute (String name) {
        return attributes.get(name);
    }


    public void setIndexableAttribute (String name, Object value) {
        if (null == value) {
            value = "";
        }
        indexableAttributes.put(name, value);
    }

    public void addIndexableAttribute (String name, Object value) throws Exception {
        if (null != getIndexableAttribute(name)) {
            throw new Exception("Attribute " + name + " already set");
        } else {
            setIndexableAttribute(name, value);
        }
    }

    /** Get a map of indexable attributes . Modifying this map does not modify document. */
    public Map<String,Object> getIndexableAttributes() {
        return new HashMap<String,Object>(indexableAttributes);
    }


    public Object getIndexableAttribute (String name) {
        return indexableAttributes.get(name);
    }



    public void addCategory(String category) {
        categories.add(category);
    }


    /**
     * Removes a category from this FetchDocument. If the FetchDocument
     * has the parameter category, it is deleted.
     * @param category
     *      The category to delete from the document.
     * @return
     *      true if the category was present and it has been deleted.
     *      false if the category was not present.
     */
    public boolean removeCategory(String category) {
        if (categories.contains(category)) {
            categories.remove(category);
            return true;
        } 
        return false;
    }

    /** Get a set of categories. Modifying this set does not modify document. */
    public Set<String> getCategories() {
        return new HashSet<String>(categories);
    }


}

