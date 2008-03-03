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
package com.flaptor.search4j.crawler;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import com.flaptor.search4j.crawler.pagedb.Page;
import com.flaptor.search4j.crawler.pagedb.Link;
import com.flaptor.search4j.crawler.modules.FetchDocument;
import com.flaptor.util.TestUtils;

/**
 * @author Flaptor Development Team
 */
public class SimFetcher implements IFetcher {

    private Random rnd = new Random(new Date().getTime());
    private SimWeb web = null;

    public SimFetcher (SimWeb web) {
        super();
        this.web = web;
    }

    public FetchData fetch (FetchList fetchlist) throws Exception {
        FetchData fetchdata = new FetchData();
// System.out.println("Fetching:");
        for (Page page : fetchlist) {
            String url = page.getUrl();
// System.out.println("  url="+url);
            String text = "";
            String title = "";
            HashMap<String,String> header = new HashMap<String,String>();
            byte[] content = new byte[0];
            Link[] links = new Link[0];
            int pageid = SimWeb.urlToPage(url);
            boolean success = (web.getStatus(pageid) == SimWeb.SUCCESS);
            boolean recoverable = true;
            boolean changed = true;
            if (success) {
                web.markAsReached(pageid);
                int[] children = web.getChildren(pageid);
                links = new Link[children.length];
                for (int i=0; i<children.length; i++) {
                    String link = SimWeb.pageToUrl(children[i]);
// System.out.println("     outlink["+i+"]="+link);
                    String anchor = TestUtils.randomText(2,5);
                    links[i] = new Link(link, anchor);
                }
                text = TestUtils.randomText(5,50);
                title = TestUtils.randomText(2,5);
                content = text.getBytes();
                header.put("length",String.valueOf(content.length));
            }
            FetchDocument doc = new FetchDocument(page, url, text, title, links, content, header, success, recoverable, changed);
            fetchdata.addDoc(doc);
        }
        return fetchdata;
    }

}

