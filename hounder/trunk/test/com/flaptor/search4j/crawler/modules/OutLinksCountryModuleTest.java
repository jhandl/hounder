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

import java.io.IOException;

import com.flaptor.search4j.crawler.pagedb.Page;
import com.flaptor.search4j.crawler.pagedb.Link;
import com.flaptor.util.Config;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;
import com.flaptor.util.Execute;

/**
 * @author Flaptor Development Team
 */
public class OutLinksCountryModuleTest extends TestCase {

    private static final String MOD_NAME= "outLinksCountry";
    private static final String POS_PROP= "Module.properties";

    private OutLinksCountryModule module;

    // Argentinean sites
    private static final Link AR_1=new Link("http://www.com.ar",null);
    private static final Link AR_2=new Link("http://www.edu.ar",null);
    private static final Link AR_3=new Link("http://www.org.ar",null);
    private static final Link AR_4=new Link("http://fasdf.fasd.ar",null);

    // Other countries sites
    private static final Link OT_1=new Link("http://www.com.mx",null);
    private static final Link OT_2=new Link("http://www.ar.es",null);
    private static final Link OT_3=new Link("http://www.org.uk",null);
    private static final Link OT_4=new Link("http://fasdf.fasd.il",null);

    // Global sites
    private static final Link GL_1=new Link("http://www.com",null);
    private static final Link GL_2=new Link("http://www.edu",null);
    private static final Link GL_3=new Link("http://www.org",null);
    private static final Link GL_4=new Link("http://fasdf.fasd.ar.com",null);

    // Commonly global sites (belongs to a country, but are used globally)
    private static final Link OG_1=new Link("http://www.com.cc",null);
    private static final Link OG_2=new Link("http://www.edu.tv",null);
    private static final Link OG_3=new Link("http://www.org.info",null);
    private static final Link OG_4=new Link("http://fasdf.fasd.ar.ws",null);

    // Known argetniean sites
    private static final String SA_1="www.clarin.com";
    private static final String SA_2="argentinos.en.es";
    private static final Link KA_1=new Link("http://" + SA_1, null);
    private static final Link KA_2=new Link("http://" + SA_2, null);

    private String sitesArFile="sitesAr";
    private String tmpConfig = "tmp.properties";
    
    public void setUp() throws Exception {
        String sitesAr= SA_1 + "\n" + SA_2 + "\n";
        TestUtils.writeFile(sitesArFile, sitesAr);

        Config modcfg = Config.getConfig(MOD_NAME+POS_PROP);
        modcfg.set("pass.through.on.tags","");
        modcfg.set("pass.through.on.missing.tags","");
        modcfg.set("threshold.value","0.25");
        modcfg.set("on.above.threshold.set.tags","");
        modcfg.set("on.above.threshold.unset.tags","");
        modcfg.set("on.below.threshold.set.tags","");
        modcfg.set("on.below.threshold.unset.tags","");
        modcfg.set("outlinks.sites.regexp.1","^.*\\.ar$");
        modcfg.set("outlinks.sites.file.1",sitesArFile);
        modcfg.set("outlinks.sites.regexp.2","^.*\\.\\p{Alpha}{2}?$");
        modcfg.set("outlinks.sites.file.2","");
        modcfg.set("outlinks.sites.regexp.ignore","^.*\\.(tv|ws|cc)$");
        modcfg.set("outlinks.sites.file.ignore","");
        
        TestUtils.setConfig(tmpConfig,
                "page.text.max.length=250\n" +
                "hotspot.tag=TAG_IS_HOTSPOT\n" +
                "emitdoc.tag=TAG_IS_INDEXABLE");
        Config cfg = TestUtils.getConfig();
        module = new OutLinksCountryModule(MOD_NAME, cfg);
    }
    
    public void tearDown() {
        FileUtil.deleteFile(sitesArFile);
        FileUtil.deleteFile(tmpConfig);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testLinksAllOfOneType() throws Exception {
        FetchDocument doc;
        Double res;
        
        Link[] allAr={AR_1, AR_2, AR_3, AR_4};
        Link[] allOt={OT_1, OT_2, OT_3, OT_4};
        Link[] allGl={GL_1, GL_2, GL_3, GL_4};
        Link[] allOg={OG_1, OG_2, OG_3, OG_4};

        doc= new FetchDocument(new Page("",1f), null, null, null, allAr, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, Double.MAX_VALUE);

        doc= new FetchDocument(new Page("",1f), null, null, null, allOt, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 0.0);

        doc= new FetchDocument(new Page("",1f), null, null, null, allGl, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 0.0);

        doc= new FetchDocument(new Page("",1f), null, null, null, allOg, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 0.0);        
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testLinksArVsOthers() throws Exception {
        FetchDocument doc;
        Double res;
        
        Link[] a075={AR_1, OT_2, AR_3, AR_4};
        Link[] a050={AR_1, AR_2, OT_3, OT_4};
        Link[] a025={OT_2, AR_2, OT_3, OT_4};
        Link[] a000={OT_1, OT_2, OT_3, OT_4};

        doc= new FetchDocument(new Page("",1f), null, null, null, a075, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 3/1.0);

        doc= new FetchDocument(new Page("",1f), null, null, null, a050, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 2/2.0);

        doc= new FetchDocument(new Page("",1f), null, null, null, a025, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 1/3.0);

        doc= new FetchDocument(new Page("",1f), null, null, null, a000, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 0.0);        
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testLinksArVsOthersAndIgnore() throws Exception {
        FetchDocument doc;
        Double res;
        
        Link[] a075={AR_1, OT_2, AR_3, AR_4, GL_1};
        Link[] a050={AR_1, OG_3, OG_4, AR_2, OT_3, OT_4, GL_2, GL_1};
        Link[] a025={OT_2, GL_3, AR_2, OT_3, OT_4, GL_1};
        Link[] a000={GL_2, OT_1, OT_2, OT_3, OT_4, GL_4, OG_1, OG_2};

        doc= new FetchDocument(new Page("",1f), null, null, null, a075, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 3/1.0);

        doc= new FetchDocument(new Page("",1f), null, null, null, a050, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 2/2.0);

        doc= new FetchDocument(new Page("",1f), null, null, null, a025, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 1/3.0);

        doc= new FetchDocument(new Page("",1f), null, null, null, a000, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(res, 0.0);        
    }


    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testLinksArVsOthersAndIgnoreAndFile() throws Exception {
        FetchDocument doc;
        Double res;
        
        Link[] a075={AR_1, OT_2, KA_1, AR_4, GL_1};
        Link[] a050={KA_1, KA_2, OT_3, OT_4, GL_2, GL_1};
        Link[] a025={OT_2, GL_3, KA_2, OT_3, OT_4, GL_1};
        Link[] a000={GL_2, OT_1, OT_2, OT_3, OT_4, GL_4};

        doc= new FetchDocument(new Page("",1f), null, null, null, a075, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(3/1.0, res);

        doc= new FetchDocument(new Page("",1f), null, null, null, a050, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(2/2.0, res);

        doc= new FetchDocument(new Page("",1f), null, null, null, a025, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(1/3.0, res);

        doc= new FetchDocument(new Page("",1f), null, null, null, a000, null, null, true, false, true);
        res= module.tInternalProcess(doc);
        assertEquals(0.0, res);        
    }
}
