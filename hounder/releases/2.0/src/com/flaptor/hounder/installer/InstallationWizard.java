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
import java.io.IOException;

import org.apache.log4j.Logger;

import com.flaptor.hounder.installer.configs.CacheServerConfig;
import com.flaptor.hounder.installer.configs.ClusteringWebConfig;
import com.flaptor.hounder.installer.configs.CommonConfig;
import com.flaptor.hounder.installer.configs.ComponentConfig;
import com.flaptor.hounder.installer.configs.CrawlerConfig;
import com.flaptor.hounder.installer.configs.IndexerConfig;
import com.flaptor.hounder.installer.configs.SearcherConfig;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.IOUtil;
import com.flaptor.util.PortUtil;
import com.flaptor.wizard.InputPageElement;
import com.flaptor.wizard.NextChoosingPage;
import com.flaptor.wizard.OptionPageElement;
import com.flaptor.wizard.Page;
import com.flaptor.wizard.PageElement;
import com.flaptor.wizard.ProgressPageElement;
import com.flaptor.wizard.Wizard;
import com.flaptor.wizard.YesNoNextChoosingPage;
import com.flaptor.wizard.YesNoPageElement;
import com.flaptor.wizard.ui.CLI;
import com.flaptor.wizard.ui.GUI;
import com.flaptor.wizard.ui.UI;


/**
 * Installer for Hounder product, that can be run in graphic mode or command line mode
 * 
 * @author Martin Massera
 */
public class InstallationWizard {
    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    
    private Page initial = new Page();
    private NextChoosingPage installationMethod = new NextChoosingPage("method");
    private Page thisMachineAllInstall = new Page();
    private Page thisMachineSomeInstall = new Page();
    private Page multiMachineInstall = new Page();
    
    private Page selectComponents		= new Page();
    private Page selectComponentsMulti	= new Page();
    private Page searcherInstall		= new Page(); private Page searcherInstallRemote		= new Page();
    private Page indexerInstall			= new Page(); private Page indexerInstallRemote			= new Page();
    private Page crawlerInstall			= new Page(); private Page crawlerInstallRemote			= new Page();
    private Page cacheServerInstall		= new Page(); private Page cacheServerInstallRemote		= new Page();
    private Page clusteringWebInstall	= new Page(); private Page clusteringWebInstallRemote	= new Page();
    private Page thisMachinesName		= new Page();

    private Page copySSH = new Page();
    
    private Page dirPortOptions = new Page();
    
//    private Page installBaseOptions = new Page();
	private YesNoNextChoosingPage confirmOverwrite = new YesNoNextChoosingPage("confirm");
    private Page installing = new Page();
    private Page summary = new Page();

    private String distDir;
    private String installDir;
    
    private boolean graphicMode;
    private UI ui;
    private Wizard wizard;
    
    
    //////////////////////////////////
    // installation state variables //
    private boolean multiMachine = false;
	
    //this two for multimachine
    private boolean oneRemote = false;
	private boolean oneLocal = false;
	
    private CommonConfig commonConfig = new CommonConfig();
    private CrawlerConfig crawlerConfig = new CrawlerConfig();
    private IndexerConfig indexerConfig = new IndexerConfig();
    private SearcherConfig searcherConfig = new SearcherConfig();
    private CacheServerConfig cacheServerConfig = new CacheServerConfig();
    private ClusteringWebConfig clusteringWebConfig = new ClusteringWebConfig();
	//////////////////////////////////

    private final String DEFAULT_INSTALLATION_DIR = "/var/local/hounder";
    
    public InstallationWizard(boolean graphicMode, String distDir) {
        
    	this.graphicMode = graphicMode;
        ui = graphicMode ? new GUI("Hounder Installation Wizard") : new CLI();
        this.distDir =  distDir;
        
        Config searcherProperties = Config.getConfig("searcher.properties");
    
//        try {installDir = FileUtil.getDir(".") + "/hounder/";} catch (IOException e) {installDir = DEFAULT_INSTALLATION_DIR;}
        installDir = DEFAULT_INSTALLATION_DIR;

//create the pages
        initial
            .add(new PageElement("Welcome to Hounder installation Wizard"));
        installationMethod
            .add(new OptionPageElement(
            		"Choose installation type",
            		"Hounder can run in one machine or distributed in several machines.",
            "method",
            "0",
            new String[] {"Install all components in this machine", "Install some components in this machine", "Multimachine install"}
            ));
        
        thisMachineAllInstall
            .add(new PageElement("You have selected a complete installation of Hounder in this machine."));
        thisMachineSomeInstall
	        .add(new PageElement("You have selected to install Hounder in this machine, but only some components."))
	        .add(new PageElement("You will select which components to install"));       
        multiMachineInstall
            .add(new PageElement("You have selected to install Hounder in several machines."))
            .add(new PageElement("You will select which components to install on this machine.", "also, you will enter the hosts where you will install other components."))
            .add(new PageElement("After you install on this machine, you have to run the installer on the other hosts"));
		selectComponents            
	        .add(new PageElement("You will now select which components to install."))
	        .add(new YesNoPageElement("Install searcher?",null,"installSearcher", "y"))
	        .add(new YesNoPageElement("Install indexer?",null,"installIndexer", "y"))
	        .add(new YesNoPageElement("Install crawler?",null,"installCrawler", "y"))
	        .add(new YesNoPageElement("Install cacheServer?",null,"installCacheServer", "y"))
	        .add(new YesNoPageElement("Install monitorWeb?",null,"installMonitorWeb", "y"));
		selectComponentsMulti            
	        .add(new PageElement("You will now select which components to install.", "Select whether you want to install a component, and if the component will be installed on this machine or on other."))
	        .add(new OptionPageElement("Install searcher?",null,"installSearcher", "0", new String[] {"yes, in this machine",  "yes, in a remote machine", "no"}))
	        .add(new OptionPageElement("Install indexer?",null,"installIndexer", "0", new String[] {"yes, in this machine",  "yes, in a remote machine", "no"}))
	        .add(new OptionPageElement("Install crawler?",null,"installCrawler", "0", new String[] {"yes, in this machine",  "yes, in a remote machine", "no"}))
	        .add(new OptionPageElement("Install cacheServer?",null,"installCacheServer", "0", new String[] {"yes, in this machine",  "yes, in a remote machine", "no"}))
	        .add(new OptionPageElement("Install monitorWeb?",null,"installMonitorWeb", "0", new String[] {"yes, in this machine",  "yes, in a remote machine", "no"}));       
		searcherInstallRemote
			.add(new PageElement("Searcher remote configuration", "Please tell us where you will install the searcher."))
        	.add(new InputPageElement("host", null, "host", "localhost"))
        	.add(new InputPageElement("base port", null, "basePort", String.valueOf(PortUtil.getBasePort())))
			.add(new InputPageElement("base dir", null, "baseDir", DEFAULT_INSTALLATION_DIR));
		indexerInstallRemote
			.add(new PageElement("Indexer remote configuration", "Please tell us where you will install the indexer."))
        	.add(new InputPageElement("host", null, "host", "localhost"))
        	.add(new InputPageElement("base port", null, "basePort", String.valueOf(PortUtil.getBasePort())))
			.add(new InputPageElement("base dir", null, "baseDir", DEFAULT_INSTALLATION_DIR));        	
		crawlerInstallRemote
			.add(new PageElement("Crawler remote configuration", "Please tell us where you will install the crawler."))
        	.add(new InputPageElement("host", null, "host", "localhost"))
        	.add(new InputPageElement("base port", null, "basePort", String.valueOf(PortUtil.getBasePort())))
			.add(new InputPageElement("base dir", null, "baseDir", DEFAULT_INSTALLATION_DIR));
		cacheServerInstallRemote
			.add(new PageElement("Cache Server remote configuration", "Please tell us where you will install the crawler."))
	    	.add(new InputPageElement("host", null, "host", "localhost"))
	    	.add(new InputPageElement("base port", null, "basePort", String.valueOf(PortUtil.getBasePort())))
			.add(new InputPageElement("base dir", null, "baseDir", DEFAULT_INSTALLATION_DIR));
		clusteringWebInstallRemote
			.add(new PageElement("Monitor Web remote configuration", "Please tell us where you will install the monitor web."))
        	.add(new InputPageElement("host", null, "host", "localhost"))
        	.add(new InputPageElement("base port", null, "basePort", String.valueOf(PortUtil.getBasePort())))
			.add(new InputPageElement("base dir", null, "baseDir", DEFAULT_INSTALLATION_DIR));
//        searcherInstall
//        	.add(new PageElement("Searcher configuration"));
//        indexerInstall
//        	.add(new PageElement("Indexer configuration"));
//        crawlerInstall
//        	.add(new PageElement("Crawler configuration"));
        cacheServerInstall
			.add(new PageElement("Cache Server configuration"))
	    	.add(new InputPageElement("External host name", "users will access this through http, cannot be localhost", "host", "<replace>"));
        thisMachinesName
        	.add(new InputPageElement("What's the name of this machine?", "Enter the machine name or IP address", "localhostName", "<replace>"));
        copySSH
    		.add(new OptionPageElement(
				"How do you wish to copy files to the remote machines?",
				"You need to have SSH access without password to let the installer copy the files",
				"method",
				"0",
				new String[]{"let the installer copy them via SSH", "give me some .tgz that I'll copy and decompress myself"}));
    	dirPortOptions
    		.add(new PageElement("Local installation port and path"))
        	.add(new InputPageElement("Base port", "Hounder uses a range of ports starting in one base port", "basePort", String.valueOf(PortUtil.getBasePort())))
        	.add(new InputPageElement("Base dir", "This is where Hounder will be installed in this machine. If it exists, it will be overwritten.", "path",  installDir));    
//        installBaseOptions
//            .add(new InputPageElement("Local installation path", "This is where Hounder will be installed in this machine. If it exists, it will be overwritten.", "path",  installDir));
            

        thisMachineAllInstall.setPreNextCallback(new Runnable() {
			public void run() {
				multiMachine = false;
				crawlerConfig.install		= true;    
    			searcherConfig.install	 	= true;    
    			indexerConfig.install		= true;    
			    cacheServerConfig.install 	= true;    
    			clusteringWebConfig.install	= true;
				crawlerConfig.installOnThisMachine		= true;    
    			searcherConfig.installOnThisMachine 	= true;    
    			indexerConfig.installOnThisMachine 		= true;    
			    cacheServerConfig.installOnThisMachine 	= true;    
    			clusteringWebConfig.installOnThisMachine= true;

				cacheServerInstall.setNextPage(dirPortOptions);
			}
        });
        thisMachineSomeInstall.setPreNextCallback(new Runnable() {
			public void run() {
				multiMachine = false;
				crawlerConfig.installOnThisMachine		= true;    
    			searcherConfig.installOnThisMachine 	= true;    
    			indexerConfig.installOnThisMachine 		= true;    
			    cacheServerConfig.installOnThisMachine 	= true;    
    			clusteringWebConfig.installOnThisMachine= true;
			}
        });
        multiMachineInstall.setPreNextCallback(new Runnable() {
			public void run() {
				multiMachine = true;
			}
        });
        selectComponents.setPreNextCallback(new Runnable() {
            public void run() {
                crawlerConfig.install		= selectComponents.getProperty("installCrawler").equalsIgnoreCase("y");    
    			searcherConfig.install	 	= selectComponents.getProperty("installSearcher").equalsIgnoreCase("y");    
    			indexerConfig.install		= selectComponents.getProperty("installIndexer").equalsIgnoreCase("y");    
			    cacheServerConfig.install	= selectComponents.getProperty("installCacheServer").equalsIgnoreCase("y");    
    			clusteringWebConfig.install	= selectComponents.getProperty("installMonitorWeb").equalsIgnoreCase("y");
    			
    			if (cacheServerConfig.install) {
    				selectComponents.setNextPage(cacheServerInstall);
    				cacheServerInstall.setNextPage(dirPortOptions);
    			} else {
    				selectComponents.setNextPage(dirPortOptions);
    			}
            }
        });       
        selectComponentsMulti.setPreNextCallback(new Runnable() {
            public void run() {
                crawlerConfig.install		= Integer.parseInt(selectComponentsMulti.getProperty("installCrawler")) <= 1;    
    			searcherConfig.install	 	= Integer.parseInt(selectComponentsMulti.getProperty("installSearcher")) <= 1;
    			indexerConfig.install		= Integer.parseInt(selectComponentsMulti.getProperty("installIndexer")) <= 1;
			    cacheServerConfig.install	= Integer.parseInt(selectComponentsMulti.getProperty("installCacheServer")) <= 1;
    			clusteringWebConfig.install	= Integer.parseInt(selectComponentsMulti.getProperty("installMonitorWeb")) <= 1;	
                crawlerConfig.installOnThisMachine		= selectComponentsMulti.getProperty("installCrawler").equals("0");    
    			searcherConfig.installOnThisMachine		= selectComponentsMulti.getProperty("installSearcher").equals("0");   
    			indexerConfig.installOnThisMachine		= selectComponentsMulti.getProperty("installIndexer").equals("0");    
			    cacheServerConfig.installOnThisMachine	= selectComponentsMulti.getProperty("installCacheServer").equals("0");
    			clusteringWebConfig.installOnThisMachine= selectComponentsMulti.getProperty("installMonitorWeb").equals("0"); 
    			
				oneLocal = false;
				oneRemote = false;
				
				Page next = selectComponentsMulti;
				if (searcherConfig.install) {
					if (!searcherConfig.installOnThisMachine) { 
						next.setNextPage(searcherInstallRemote);
						next = next.getNextPage();
						oneRemote = true;
					} else {
						oneLocal = true;
					}
				}
				if (indexerConfig.install) {
					if (! indexerConfig.installOnThisMachine) { 
						next.setNextPage(indexerInstallRemote);
						next = next.getNextPage();
						oneRemote = true;
					} else {
						oneLocal = true;
					}
				}
				if (crawlerConfig.install) {
					if ( ! crawlerConfig.installOnThisMachine) { 
						next.setNextPage(crawlerInstallRemote);
						next = next.getNextPage();
						oneRemote = true;
					} else {
						oneLocal = true;
					}
				}
				if (cacheServerConfig.install) {
					if (!cacheServerConfig.installOnThisMachine) {
						next.setNextPage(cacheServerInstallRemote);
						oneRemote = true;
					} else {
						next.setNextPage(cacheServerInstall);
						oneLocal = true;
					}
					next = next.getNextPage();
				}
				if (clusteringWebConfig.install) {
					if (!clusteringWebConfig.installOnThisMachine) { 
						next.setNextPage(clusteringWebInstallRemote);
						next = next.getNextPage();
						oneRemote = true;
					} else {
						oneLocal = true;
					}
				}
				if (oneRemote) {
					next.setNextPage(copySSH);
					next = next.getNextPage();
				}
				if (oneLocal) {
					next.setNextPage(thisMachinesName);
				} else {//maybe there is no components in this machine, in which case we dont need to ask for the local path
					next.setNextPage(installing);
				}
            }
        });       
    	dirPortOptions.setPreNextCallback(new Runnable() {
			public void run() {
				if (new File(dirPortOptions.getProperty("path")).exists()) {
					dirPortOptions.setNextPage(confirmOverwrite);
				} else {
					dirPortOptions.setNextPage(installing);
				}
			}
    	});

        confirmOverwrite.add(new YesNoPageElement("Installation path exists and will be overwritten. Please Confirm.", null, "confirm","y"));
        confirmOverwrite.addNextPage(installing).addNextPage(dirPortOptions);
        
        installing
            .add(new PageElement("Installing Hounder"));
        summary
        	.add(new PageElement("Hounder installation summary"))
        	.setCanCancelOrBack(false);

        installing.setCanCancelOrBack(false);
        
//set ordering of pages
        initial.setNextPage(installationMethod);
        installationMethod.addNextPage(thisMachineAllInstall).addNextPage(thisMachineSomeInstall).addNextPage(multiMachineInstall);
		thisMachineAllInstall.setNextPage(cacheServerInstall);
		thisMachineSomeInstall.setNextPage(selectComponents).setNextPage(cacheServerInstall);
		
		cacheServerInstall.setNextPage(dirPortOptions);
		thisMachinesName.setNextPage(dirPortOptions);

		dirPortOptions.setNextPage(installing).setNextPage(summary);

		multiMachineInstall.setNextPage(selectComponentsMulti); selectComponentsMulti.setNextPage(dirPortOptions);

        installing.setReadyToAdvance(false);
        installing.setPreRenderCallback(new Runnable() {
            public void run() {new Thread() {public void run() {install(ui);}}.start();}});
        
        wizard = new Wizard(initial, ui);
    }
    
    public void start() {
        wizard.startWizard();
    }

    private void install(UI ui) {
        ProgressPageElement installprogress = new ProgressPageElement("progress", null);
        PageElement errorReport = new PageElement("");
        PageElement finishReport = new PageElement("");
        installing.add(installprogress);
        installing.add(errorReport);
        installing.add(finishReport);
        
        commonConfig.localhostName = "localhost";
        if (multiMachine && oneLocal) commonConfig.localhostName = thisMachinesName.getProperty("localhostName").trim();
        if (!multiMachine || oneLocal) {
        	commonConfig.installOnBaseDir = new File(dirPortOptions.getProperty("path").trim());
        	commonConfig.installOnBasePort = Integer.parseInt(dirPortOptions.getProperty("basePort"));
        }
        commonConfig.copyViaSSH = copySSH.getProperty("method").equals("0");
        commonConfig.outputDir = new File("output").getAbsolutePath();
    
    	//init to localhost:baseport
    	crawlerConfig.installOnBasePort 		= commonConfig.installOnBasePort; crawlerConfig.installOnHost 		= commonConfig.localhostName; crawlerConfig.installOnBaseDir 		= commonConfig.installOnBaseDir;
    	indexerConfig.installOnBasePort 		= commonConfig.installOnBasePort; indexerConfig.installOnHost 		= commonConfig.localhostName; indexerConfig.installOnBaseDir 		= commonConfig.installOnBaseDir;
    	searcherConfig.installOnBasePort 		= commonConfig.installOnBasePort; searcherConfig.installOnHost 		= commonConfig.localhostName; searcherConfig.installOnBaseDir 		= commonConfig.installOnBaseDir;
    	cacheServerConfig.installOnBasePort 	= commonConfig.installOnBasePort; cacheServerConfig.installOnHost 	= commonConfig.localhostName; cacheServerConfig.installOnBaseDir 	= commonConfig.installOnBaseDir;
    	clusteringWebConfig.installOnBasePort 	= commonConfig.installOnBasePort; clusteringWebConfig.installOnHost = commonConfig.localhostName; clusteringWebConfig.installOnBaseDir = commonConfig.installOnBaseDir;

		//change for remote components
		if (!crawlerConfig.installOnThisMachine) {
			crawlerConfig.installOnBasePort 		= Integer.parseInt(crawlerInstallRemote.getProperty("basePort"));  
			crawlerConfig.installOnHost 			= crawlerInstallRemote.getProperty("host");
			crawlerConfig.installOnBaseDir			= new File(crawlerInstallRemote.getProperty("baseDir"));
		} 
		if (!searcherConfig.installOnThisMachine) {
			searcherConfig.installOnBasePort 		= Integer.parseInt(searcherInstallRemote.getProperty("basePort"));  
			searcherConfig.installOnHost 			= searcherInstallRemote.getProperty("host");		
			searcherConfig.installOnBaseDir			= new File(searcherInstallRemote.getProperty("baseDir"));
		} 
		if (!indexerConfig.installOnThisMachine) {
			indexerConfig.installOnBasePort 		= Integer.parseInt(indexerInstallRemote.getProperty("basePort"));  
			indexerConfig.installOnHost 			= indexerInstallRemote.getProperty("host");		
			indexerConfig.installOnBaseDir			= new File(indexerInstallRemote.getProperty("baseDir"));
		} 
		if (!cacheServerConfig.installOnThisMachine) {
			cacheServerConfig.installOnBasePort 	= Integer.parseInt(cacheServerInstallRemote.getProperty("basePort"));  
			cacheServerConfig.installOnHost 		= cacheServerInstallRemote.getProperty("host");		
			cacheServerConfig.installOnBaseDir		= new File(cacheServerInstallRemote.getProperty("baseDir"));
		}  else {
			cacheServerConfig.externalHostName	= cacheServerInstall.getProperty("host");			
		}
		if (!clusteringWebConfig.installOnThisMachine) {
			clusteringWebConfig.installOnBasePort 	= Integer.parseInt(clusteringWebInstallRemote.getProperty("basePort"));  
			clusteringWebConfig.installOnHost 		= clusteringWebInstallRemote.getProperty("host");		
			clusteringWebConfig.installOnBaseDir	= new File(clusteringWebInstallRemote.getProperty("baseDir"));
		} 

        try {
            crawlerConfig.pagedbSeeds = IOUtil.readAll(ClassLoader.getSystemClassLoader().getResourceAsStream("pagedb.seeds"));
            crawlerConfig.hotspots = IOUtil.readAll(ClassLoader.getSystemClassLoader().getResourceAsStream("hotspots.regex"));
        	Installer installer = new Installer(distDir, commonConfig, indexerConfig, searcherConfig, crawlerConfig, cacheServerConfig, clusteringWebConfig, installprogress, ui);
        	installer.install();
        	File crawlerDir = new File(installer.getCrawlerBaseDir(), "crawler");
        	if (crawlerConfig.install) new CrawlerConfigurationWizard(graphicMode, crawlerDir).start();
        	if (multiMachine) installer.makeDist();
        	
        	finishReport.setText("Hounder installed correctly!");
        	installprogress.setExplanation("");
        	installprogress.setProgress(100);
        	ui.elementUpdated(installprogress);
            if (searcherConfig.install)     addComponentSummary("Searcher", searcherConfig); 
            if (indexerConfig.install)      addComponentSummary("Indexer", indexerConfig); 
            if (crawlerConfig.install)      addComponentSummary("Crawler", indexerConfig); 
            if (cacheServerConfig.install)  addComponentSummary("Cache Server", cacheServerConfig); 
            if (clusteringWebConfig.install) {
                addComponentSummary("Clustering Web", clusteringWebConfig);
            }

            if (multiMachine && oneRemote) {
            	if (commonConfig.copyViaSSH) summary.add(new PageElement("Files have been copied via SSH"));
            	else summary.add(new PageElement(".TGZ files have been written to " + commonConfig.outputDir, "copy these files to the host:dir indicated and uncompress them there"));
            }
            summary.add(new PageElement("To start Hounder, go to the base dir and run start-all.sh", 
                    clusteringWebConfig.install ?
                            "Once the clustering web is started, you can administer Hounder by pointing your browser to http://" + clusteringWebConfig.installOnHost + ":" + PortUtil.getPort(clusteringWebConfig.installOnBasePort, "clustering.web") +"/"
                            :null));
        } catch (Throwable e) {
            errorReport.setText("There has been an error");
            errorReport.setExplanation(e.getMessage());
            ui.elementUpdated(errorReport);

            finishReport.setText("Installation did not complete due to errors.");
            logger.error(e);
            e.printStackTrace();

            summary
        		.add(new PageElement("Hounder installation didnt finish correctly."));
        }
        installing.setReadyToAdvance(true);
        
        ui.elementUpdated(finishReport);
        
    }

    private void addComponentSummary(String componentName, ComponentConfig componentConfig) {
        if (componentConfig.install) {
        	summary.add(new PageElement(componentName + " installed", componentConfig.installOnThisMachine ? ("in "+componentConfig.installOnBaseDir) : ("in " + componentConfig.installOnHost + ":" + componentConfig.installOnBaseDir)));
        }    	
    }
    
    public static void main(String[] args) throws IOException {
        boolean graphics = false;
        String distDir = null;
        if (args.length > 2 || args.length == 0) usage();
        if (args.length == 2) {
            if (!args[0].equals("-graphicMode")) usage();
            else graphics = true;
            
            distDir = args[1];
        } else {
            distDir = args[0];
        }
        
        distDir = FileUtil.getDir(distDir);
        new InstallationWizard(graphics, distDir).start();
    }
    
    private static void usage() {
        System.err.println("Arguments: [-graphicMode] distributionDir");
        System.exit(-1);
    }
}

