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
package com.flaptor.search4j.crawler.modules;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.flaptor.search4j.crawler.pagedb.Link;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;

/**
 * This Module counts the amount of outlinks to REGEXP1 and compares it to the 
 * amount of outlinks to REGEXP2.
 * 
 * It is used to decide if a page 'belongs' to some country although may be 
 * hosted in another country or in global domain (ie: .com).
 * Note that only the host portiion of the URL is used.
 * 
 * As an example: 
 *    REGEXP1="^.*\\.ar$"  (ie: argentinean sites)
 *    REGEXP2="^.*\\.\\p{Alpha}{2}?$" (ie: any country site) 
 *    and threshold is 0.25, the page will be 
 *    considered argentinean if there are at least 3 outlinks to .ar for each
 *    outlink to any other country.
 *
 * The module also has a REGEXP3 to allow ignoring some sites. Also, the sites 
 * can be given as a list, not a only as a regexp, using a plain text file.
 * 
 * The evaluation order is as follow: 
 *  If a URL matches REGEXP_IGNORE or is in LIST_IGNORE, it is ignored
 *  If a URL matches REGEXP1 or is in LIST1 ==> SITE++
 *  If a URL matches REGEXP2 or is in LIST2 ==> OTHER++
 *  If the URL doesn't match anytthing,  it is ignored
 *  
 *  Note that because of this order, if a host matches REGEXP_IGNORE
 *   it will not be checked against REGEXP1/2. Hence do not add '.com' to 
 *   IGNORE if it appears in FILE1/2. Simply leaving IGNORE in blank will work 
 *   as no-matching urls are ignored. The IGNORE is checked first to allow 
 *   *.tv, etc sites be ignored before checking REGEXP1/2
 *   
 *  Finally the value SITE/OTHER is returned. (if SITE is 0, 0 is returned. 
 *  If OTHER is 0, Double.MAX_VALUE is returned);
 * 
 * 
 * @author rafa
 *
 */
@SuppressWarnings("unchecked")
public class OutLinksCountryModule extends AThresholdModule {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    private static final int OUTLINKS_IGNORE=0;
    private static final int OUTLINKS_1=1;
    private static final int OUTLINKS_2=2;
        
    private String regexp []= new String[3];
    private HashSet<String>[] knownSites= new HashSet[3]; 
    
    public OutLinksCountryModule(String name, Config globalConfig) {
        super(name, globalConfig);
        String sitesFile;
        
        regexp[OUTLINKS_1]= config.getString("outlinks.sites.regexp.1").trim();
        sitesFile= config.getString("outlinks.sites.file.1");
        knownSites[OUTLINKS_1]= new HashSet<String>();
        if (null == sitesFile || "".equals(sitesFile)) {
            logger.warn("There is no outlinks.sites.file.1 file defined.");
        } else {
            logger.info("Loading " + sitesFile + " as sites.file.1");
            FileUtil.fileToSet(null, sitesFile, knownSites[OUTLINKS_1],logger);
        } 

        regexp[OUTLINKS_2]= config.getString("outlinks.sites.regexp.2").trim();
        sitesFile= config.getString("outlinks.sites.file.2");
        knownSites[OUTLINKS_2]= new HashSet<String>();
        if (null == sitesFile || "".equals(sitesFile)) {
            logger.warn("There is no outlinks.sites.file.2 file defined.");
        } else {
            logger.info("Loading " + sitesFile + " as sites.file.2");
            FileUtil.fileToSet(null, sitesFile, knownSites[OUTLINKS_2],logger);
        }

        regexp[OUTLINKS_IGNORE]= config.getString("outlinks.sites.regexp.ignore").trim();
        sitesFile= config.getString("outlinks.sites.file.ignore");
        knownSites[OUTLINKS_IGNORE]= new HashSet<String>();
        if (null == sitesFile || "".equals(sitesFile)) {
            logger.warn("There is no outlinks.sites.file.ignore file defined.");
        } else {
            logger.info("Loading " + sitesFile + " as sites.file.ignore");
            FileUtil.fileToSet(null, sitesFile, knownSites[OUTLINKS_IGNORE],logger);
        }
    }

    
    private int getSiteCountry(String host){
        if (knownSites[OUTLINKS_IGNORE].contains(host) ||
                host.matches(regexp[OUTLINKS_IGNORE])){
            logger.debug("***************OUTLINKS_IGNORE SITE:" + host);
            return OUTLINKS_IGNORE;
        }
        if (knownSites[OUTLINKS_1].contains(host) ||
                host.matches(regexp[OUTLINKS_1])){
            logger.debug("***************OUTLINKS_1 SITE:" + host);
            return OUTLINKS_1;
        }
        if (knownSites[OUTLINKS_2].contains(host) ||
                host.matches(regexp[OUTLINKS_2])){
            logger.debug("***************OUTLINKS_2 SITE:" + host);
            return OUTLINKS_2;
        }
        logger.debug("Ignoring unmatched url: " + host);
        return OUTLINKS_IGNORE;

    }
    
    private int[] getNumOfOutLinks(FetchDocument doc){
        int outL[]= new int[3];
        Link[] outLinks= doc.getLinks();
        for(Link link: outLinks){
            URL url;
            try {
                url = new URL(link.getUrl());
            } catch (MalformedURLException e) {
                logger.debug(link.getUrl(), e);
                continue;
            }
            outL[getSiteCountry(url.getHost())]++;
        }
        int totL=outLinks.length;
        int outA=outL[OUTLINKS_1];
        int outG=outL[OUTLINKS_IGNORE];
        int outO=outL[OUTLINKS_2];
        logger.debug("TOTAL" + "=" + totL +  
                ", ARGENT=" + outA + "(" + ((float)outA/totL) +")" + 
                ", GLOBAL=" + outG + "(" + ((float)outG/totL) +")" +
                ", OTHER="  + outO + "(" + ((float)outO/totL) +")" +
                " outlinks in " + doc.getOriginalUrl());
        return outL;
    }
        
    @Override
    protected Double tInternalProcess(FetchDocument doc) {
        int outL[]= getNumOfOutLinks(doc);
        if (0==outL[OUTLINKS_1]) return (double)0;
        if (0==outL[OUTLINKS_2]) return Double.MAX_VALUE;
        return (double) outL[OUTLINKS_1] / outL[OUTLINKS_2] ;
        
    }

}
