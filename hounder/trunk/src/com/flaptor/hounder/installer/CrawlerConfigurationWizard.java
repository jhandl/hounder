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
package com.flaptor.hounder.installer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.flaptor.hounder.installer.configs.CrawlerConfig;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.IOUtil;
import com.flaptor.wizard.InputPageElement;
import com.flaptor.wizard.NextChoosingPage;
import com.flaptor.wizard.OptionPageElement;
import com.flaptor.wizard.Page;
import com.flaptor.wizard.PageElement;
import com.flaptor.wizard.TextPageElement;
import com.flaptor.wizard.Wizard;
import com.flaptor.wizard.ui.CLI;
import com.flaptor.wizard.ui.GUI;
import com.flaptor.wizard.ui.UI;


/**
 * Installer for Hounder product, that can be run in graphic mode or command line mode
 * 
 * @author Martin Massera
 */
public class CrawlerConfigurationWizard {
    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    
    private Page initial = new Page();

    private NextChoosingPage pageSeedsType = new NextChoosingPage("seedsType");
    private Page pageSeedsFile = new Page();    
    private Page pageSeedsManual = new Page();

    private NextChoosingPage hotspotsType = new NextChoosingPage("hotspotsType");
    private Page hotspotsFile = new Page();    
    private Page hotspotsManual = new Page();
    
    private Page configuring = new Page();

    private boolean graphicMode;
    private UI ui;
    private Wizard wizard;
    
    private CrawlerConfig crawlerConfig;
    
    private File crawlerDir; 
      
    public CrawlerConfigurationWizard(boolean graphicMode, File crawlerDir) {
        this.crawlerDir = crawlerDir;
        this.graphicMode = graphicMode;
        this.crawlerConfig = new CrawlerConfig();
        setup();
    }

    private void setup() {
    	ui = graphicMode ? new GUI("Crawler Configuration Wizard") : new CLI();
        
        initial
            .add(new PageElement("Welcome to Crawler Configuration Wizard"));
    
        pageSeedsType
        	.add(new OptionPageElement(
        		"Enter pagedb.seeds",
                "Pagedb seeds are the URLs where the crawler will start to crawl",
                "seedsType",
                "0",
                new String[] {"Enter seed URLs manually", "Use a pagedb.seeds file"}
                ));
        File seedsFile = new File(crawlerDir, "pagedb.seeds");
        String seeds = "http://www.flaptor.com\nhttp://www.cnn.com";
       	try {seeds = IOUtil.readAll(new FileReader(seedsFile));} catch (IOException e) {}//should exist
        pageSeedsManual
        	.add(new TextPageElement("Enter seed URLs", "one per line","seeds", seeds));
        pageSeedsFile
        	.add(new InputPageElement("Enter pagedb.seeds file", "pagedb.seeds is a file containing a URL per line","seeds", seedsFile.getAbsolutePath()));
        
        hotspotsType
    	.add(new OptionPageElement(
    		"Enter hotspots regular expressions",
            "Hotspots determine the documents that will be considered by the crawler. They are defined by regular expressions.",
            "hotspotsType",
            "0",
            new String[] {"Enter hotspots regular expressions manually", "Use a hotspots.regex file"}
            ));
	    File hotspotsRegexFile = new File(crawlerDir, "conf/hotspots.regex");
	    String hotspots = "*";
       	try {hotspots = IOUtil.readAll(new FileReader(hotspotsRegexFile)); } catch (IOException e) {}//should exist
       	hotspotsManual
	    	.add(new TextPageElement("Enter hotspots regex", "one per line","hotspots", hotspots));
       	hotspotsFile
	    	.add(new InputPageElement("Enter hotspots.regex file", "hotspots.regex is a file containing a regular expression per line","hotspots", hotspotsRegexFile.getAbsolutePath()));
        
        configuring
        	.add(new PageElement("Configuring..."));
        
        
//set ordering of pages
        initial.setNextPage(pageSeedsType);
        pageSeedsType.addNextPage(pageSeedsManual).addNextPage(pageSeedsFile);
        pageSeedsFile.setNextPage(hotspotsType);
        pageSeedsManual.setNextPage(hotspotsType);

        hotspotsType.addNextPage(hotspotsManual).addNextPage(hotspotsFile);
        hotspotsFile.setNextPage(configuring);
        hotspotsManual.setNextPage(configuring);
        
        configuring.setReadyToAdvance(false);
        configuring.setCanCancelOrBack(false);
        configuring.setPreRenderCallback(new Runnable() {
            public void run() {new Thread() {public void run() {configure(ui);}}.start();}});
        
        wizard = new Wizard(initial, ui);    	
    }
    
    public void start() {
        wizard.startWizard();
    }

    private void configure(UI ui) {
        PageElement errorReport = new PageElement("");
        PageElement finishReport = new PageElement("");
        configuring.add(errorReport);
        configuring.add(finishReport);

        try {
        	
        	if (pageSeedsType.getProperty("seedsType").equals("0")) {
            	crawlerConfig.pagedbSeeds = pageSeedsManual.getProperty("seeds");
            } else {
            	crawlerConfig.pagedbSeeds = IOUtil.readAll(new FileReader(pageSeedsFile.getProperty("seeds"))); 
            }

        	if (hotspotsType.getProperty("hotspotsType").equals("0")) {
            	crawlerConfig.hotspots = hotspotsManual.getProperty("hotspots");
            } else {
            	crawlerConfig.hotspots = IOUtil.readAll(new FileReader(hotspotsFile.getProperty("hotspots"))); 
            }

        	Installer.configureCrawler(crawlerDir, crawlerConfig);

        	finishReport.setText("Crawler configured correctly!");
        } catch (Throwable e) {
            errorReport.setText("There has been an error");
            errorReport.setExplanation(e.getMessage());
            ui.elementUpdated(errorReport);

            finishReport.setText("Configuration did not complete due to errors.");
            logger.error(e);
            e.printStackTrace();
        }
        
        ui.elementUpdated(finishReport);
    	configuring.setReadyToAdvance(true);
    }

    public static void main(String[] args) throws IOException {
        boolean graphics = false;
        String crawlerDir = null;
        if (args.length > 2 || args.length == 0) usage();
        if (args.length == 2) {
            if (!args[0].equals("-graphicMode")) usage();
            else graphics = true;
            
            crawlerDir = args[1];
        } else {
        	crawlerDir = args[0];
        }
        new CrawlerConfigurationWizard(graphics, FileUtil.getExistingFile(crawlerDir, true, true, true)).start();
    }
    
    private static void usage() {
        System.err.println("Arguments: CrawlerConfigurationWizard [-graphicMode] crawlerDir");
        System.exit(-1);
    }
}

