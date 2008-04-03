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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.hounder.installer.configs.CacheServerConfig;
import com.flaptor.hounder.installer.configs.ClusteringWebConfig;
import com.flaptor.hounder.installer.configs.CommonConfig;
import com.flaptor.hounder.installer.configs.ComponentConfig;
import com.flaptor.hounder.installer.configs.CrawlerConfig;
import com.flaptor.hounder.installer.configs.IndexerConfig;
import com.flaptor.hounder.installer.configs.SearcherConfig;
import com.flaptor.util.CommandUtil;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.PortUtil;
import com.flaptor.util.TarUtil;
import com.flaptor.util.Triad;
import com.flaptor.wizard.ProgressPageElement;
import com.flaptor.wizard.ui.UI;

/**
 * This class has the code to install or upgrade Hounder
 * 
 * if installing in multimachine, you should call install and then makeDist to copy or create the tgz distribution files
 * 
 * @author Martin Massera
 */
public class Installer {

	private static Logger logger = Logger.getLogger(Execute.whoAmI());

	private String distDir;
	private CommonConfig commonConfig;
	private IndexerConfig indexerConfig;
	private SearcherConfig searcherConfig;
	private CrawlerConfig crawlerConfig;
	private CacheServerConfig cacheServerConfig;
	private ClusteringWebConfig clusteringWebConfig;
	private ProgressPageElement installProgress;
	private UI ui;

    private Set<String> hosts; //localhost, otherhost, etc 
    private Map<String, File> hostDirs; //localhost -> tmpdir/localhost, otherhost -> tmpdir/localhost, etc
    private Map<File, String> componentDirs; //tmpdir/localhost/var/local/hounder->localhost, tmpdir/otherhost/var/local/hounder ->otherhost, etc
	private File crawlerBaseDir;
	
	/*
	 * @param distDir the directory that contains the TGZ files to be installed
	 * @param commonConfig the common configuration
	 * @param indexerConfig configuration for indexer
	 * @param searcherConfig configuration for searcher
	 * @param crawlerConfig configuration for crawler
	 * @param cacheServerConfig configuration for cacheServer
	 * @param clusteringWebConfig configuration for clustering webapp
	 * @param installProgress a progress page element for a graphical interface, can be null
	 * @param ui a UI for updating the progress, can be null if installProgress is null
	 */
	public Installer(
			String distDir,
			CommonConfig commonConfig,
			IndexerConfig indexerConfig,
			SearcherConfig searcherConfig,
			CrawlerConfig crawlerConfig,
			CacheServerConfig cacheServerConfig,
			ClusteringWebConfig clusteringWebConfig,
			ProgressPageElement installProgress,
			UI ui) {
		this.distDir = distDir; 
		this.commonConfig = commonConfig;
		this.indexerConfig = indexerConfig;
		this.searcherConfig = searcherConfig;
		this.crawlerConfig = crawlerConfig;
		this.cacheServerConfig = cacheServerConfig;
		this.clusteringWebConfig = clusteringWebConfig;
		this.installProgress = installProgress;
		this.ui = ui;
	}
	
	
	/**
	 * Installs Hounder
	 * 
	 * @param distDir the directory that contains the TGZ files to be installed
	 * @param commonConfig the common configuration
	 * @param indexerConfig configuration for indexer
	 * @param searcherConfig configuration for searcher
	 * @param crawlerConfig configuration for crawler
	 * @param cacheServerConfig configuration for cacheServer
	 * @param clusteringWebConfig configuration for clustering webapp
	 * @param installProgress a progress page element for a graphical interface, can be null
	 * @param ui a UI for updating the progress, can be null if installProgress is null
	 * @throws IOException
	 */
	public void install() throws IOException {
        logger.info("Starting installation");
		updateProgress(installProgress, 0, "making directories", ui);
        
		//create a temp dir
		//inside it there will be hosts:
		//    /tmp/tempdir/localhost/hounder
		//    /tmp/tempdir/otherhost/hounder
		File tempDestDir = FileUtil.createTempDir("hounder-installation", "");
		

        hosts = new HashSet<String>(); //localhost, otherhost, etc 
        hostDirs = new HashMap<String, File>(); //localhost -> tmpdir/localhost, otherhost -> tmpdir/localhost, etc
        componentDirs = new HashMap<File, String>(); //tmpdir/localhost/var/local/hounder->localhost, tmpdir/otherhost/var/local/hounder ->otherhost, etc

		if (commonConfig.installOnBaseDir != null) {
	        File installBasePath = FileUtil.createOrGetDir(commonConfig.installOnBaseDir.getPath(), true, true); 
	        logger.info("installing in " + installBasePath);
	        FileUtil.deleteDir(installBasePath);
	        installBasePath.mkdirs();
	      	installBase(installBasePath, distDir);
		}        
        updateProgress(installProgress, 10, "configuring searcher", ui);
        
        if (searcherConfig.install) {
        	File componentDir = createComponentDir(tempDestDir, distDir, commonConfig, searcherConfig, hosts, hostDirs, componentDirs);       
        	installSearcher(distDir, componentDir, searcherConfig, cacheServerConfig);
        }
        
        updateProgress(installProgress, 20, "configuring indexer", ui);        
        if (indexerConfig.install) {
        	File componentDir = createComponentDir(tempDestDir, distDir, commonConfig, indexerConfig, hosts, hostDirs, componentDirs);       
        	installIndexer(distDir, componentDir, indexerConfig, searcherConfig);
        }

        updateProgress(installProgress, 40, "configuring crawler", ui);
        if (crawlerConfig.install) {
        	File componentDir = createComponentDir(tempDestDir, distDir, commonConfig, crawlerConfig, hosts, hostDirs, componentDirs);
        	crawlerBaseDir = componentDir;
        	installCrawler(distDir, componentDir, crawlerConfig, indexerConfig);
        }

        updateProgress(installProgress, 60, "configuring cache server", ui);
        if (cacheServerConfig.install) {
        	File componentDir = createComponentDir(tempDestDir, distDir, commonConfig, cacheServerConfig, hosts, hostDirs, componentDirs);        	
        	installCacheServer(distDir, componentDir, cacheServerConfig, crawlerConfig);
        }

        updateProgress(installProgress, 80, "configuring clustering web", ui);

        if (clusteringWebConfig.install) {
        	File componentDir = createComponentDir(tempDestDir, distDir, commonConfig, clusteringWebConfig, hosts, hostDirs, componentDirs);        	
        	installClusteringWeb(distDir, componentDir, searcherConfig, indexerConfig, crawlerConfig, cacheServerConfig, clusteringWebConfig);
        }

        updateProgress(installProgress, 90, "finished installing in local dirs", ui);
	}

	/**
	 * @return the crawler component base dir, that could be a temp dir or the final dir if installed in this machine
	 */
	public File getCrawlerBaseDir() {
		return crawlerBaseDir;
	}
	
	/**
	 * copy or create tgz if necessary
	 * @throws IOException 
	 */
	public void makeDist() throws IOException {
        updateProgress(installProgress, 95, "", ui);
		for (Map.Entry<File, String> componentDir : componentDirs.entrySet()) {
			File dir = componentDir.getKey();
			String host = componentDir.getValue();
			File hostDir = hostDirs.get(host);
			
			File fromDir = new File(dir.getParent());
			
			String relativeToFromDir = fromDir.getAbsolutePath().substring(hostDir.getAbsolutePath().length());
			String relativeToDir = dir.getAbsolutePath().substring(hostDir.getAbsolutePath().length());
			
			File output = new File(commonConfig.outputDir); if (output.exists()) FileUtil.deleteDir(output); output.mkdirs();
	        
			updateProgress(installProgress, "creating .tgz for " + host, ui);
			
			String destName = host+"-uncompress_in_"+relativeToFromDir+".tgz";
			destName = destName.replace("/", ".");
			TarUtil.tarFile(new File(output, destName), new File(fromDir, dir.getName()), fromDir);

			if (commonConfig.copyViaSSH) {
				String dest = host + ":"+ relativeToFromDir;
				updateProgress(installProgress, "copying via SSH to " + host + " in folder " + dest, ui);
				Triad<Integer, String, String> ret = CommandUtil.execute("scp -B -r " + dir + " " + dest, null, logger);
				if (ret.first() != 0) {
					throw new IOException("problem while copying through SSH: " + ret.second() + " - " + ret.third());
				}
			}
		}
	}
	
	
	/**
	 * installs lib plugin and scripts in the dest dir
	 * @param destBaseDir
	 * @param distDir
	 * @throws IOException 
	 */
	private static void installBase(File destBaseDir, String distDir) throws IOException {
    	logger.info("installing all scripts in " + destBaseDir);
    	TarUtil.untarFile(FileUtil.getExistingFile(distDir+"/bin-all.tgz", true, false, false), destBaseDir);
    	logger.info("installing lib in " + destBaseDir);
    	TarUtil.untarFile(FileUtil.getExistingFile(distDir+"/lib.tgz", true, false, false), destBaseDir);
    	logger.info("installing plugins in " + destBaseDir);
    	TarUtil.untarFile(FileUtil.getExistingFile(distDir+"/plugin.tgz", true, false, false), destBaseDir);
	}
	
	/**
	 * creates the files, puts them in the collections, and returns the componentDir
	 * 
	 * if the componentDir didnt exist, copies the lib, plugin and scripts
	 * 
	 * @param tempDestDir
	 * @param commonConfig
	 * @param componentConfig
	 * @param hosts
	 * @param hostDirs
	 * @param componentDirs
	 * @return
	 * @throws IOException
	 */
	private static File createComponentDir(File tempDestDir, String distDir, CommonConfig commonConfig, ComponentConfig componentConfig, Set<String> hosts, Map<String, File> hostDirs, Map<File, String> componentDirs) throws IOException {
    	File componentDir;
    	if (componentConfig.installOnThisMachine) { 
    		//if localhost dont add it, just install where it will go  
    		componentDir = commonConfig.installOnBaseDir;
    		
    	} else {
    		//if not in this machine, install it in a temp dir
    		String host = componentConfig.installOnHost;
    		File hostDir = new File(tempDestDir, host);
            componentDir = new File(hostDir, componentConfig.installOnBaseDir.getPath());
        	hosts.add(host);
        	hostDirs.put(host, hostDir);
        	FileUtil.createIfDoesntExist(componentDir, true, true);
        	if (!componentDirs.containsKey(componentDir)) {
            	componentDirs.put(componentDir, host);
            	installBase(componentDir, distDir);
        	}
    	}
    	return componentDir;
	}

	private static void updateProgress(ProgressPageElement installProgress, String activity, UI ui) {
		installProgress.setExplanation(activity); ui.elementUpdated(installProgress);
	}

	private static void updateProgress(ProgressPageElement installProgress, int progress, String activity, UI ui) {
		if (installProgress != null) {
			installProgress.setExplanation(activity);
			installProgress.setProgress(progress); ui.elementUpdated(installProgress);
		}
	}

	/**
	 * sets common properties, and creates the logs directory
	 * should be called for all components
	 * @throws IOException
	 */
	private static void doCommonTasks(File componentDir, ComponentConfig componentConfig) throws IOException {
        File confDir = new File(componentDir, "conf");
        File logsDir = new File(componentDir, "logs");
	    FileUtil.createIfDoesntExist(logsDir, true, true);
		Config commonProperties = Config.getConfig("common.properties");
		commonProperties.set("port.base", String.valueOf(componentConfig.installOnBasePort));
		commonProperties.set("baseDir", componentConfig.installOnBaseDir.getPath());
		commonProperties.modifyOnDisk(new File(confDir,"common.properties"));
	}
	
	private static void installSearcher(String distDir, File installBasePath, SearcherConfig searcherConfig, CacheServerConfig cacheServerConfig) throws IOException {
        logger.info("installing searcher");
        TarUtil.untarFile(FileUtil.getExistingFile(distDir+"/searcher.tgz", true, false, false), installBasePath);
		doCommonTasks(new File(installBasePath,"/searcher"), searcherConfig);

		Config properties = Config.getConfig("searcher.properties");
		
		properties.set("searcher.cachedVersion.showLink", String.valueOf(cacheServerConfig.install));
		properties.set("searcher.cachedVersion.cacheServerHost", cacheServerConfig.externalHostName + ":" + cacheServerConfig.installOnBasePort);
		properties.modifyOnDisk(new File(installBasePath,"/searcher/conf/searcher.properties"));
		
	}

	private static void installCrawler(String distDir, File installBasePath, CrawlerConfig crawlerConfig, IndexerConfig indexerConfig) throws IOException {
        logger.info("installing crawler");
        TarUtil.untarFile(FileUtil.getExistingFile(distDir+"/crawler.tgz", true, false, false), installBasePath);
		doCommonTasks(new File(installBasePath,"/crawler"), crawlerConfig);

		Config indexerModuleProperties = Config.getConfig("indexerModule.properties");
		indexerModuleProperties.set("remoteRmiIndexer.host", indexerConfig.installOnHost + ":" + indexerConfig.installOnBasePort);
		indexerModuleProperties.modifyOnDisk(new File(installBasePath,"/crawler/conf/indexerModule.properties"));
		
		configureCrawler(new File(installBasePath, "crawler"), crawlerConfig);
	}

	/**
	 * used by the installer and the configuration wizard
	 * @param crawlerDir
	 * @param crawlerConfig
	 * @throws IOException
	 */
	public static void configureCrawler(File crawlerDir, CrawlerConfig crawlerConfig) throws IOException {
        logger.info("creating pagedb seeds file");
    	FileUtil.writeFile(new File(crawlerDir, "pagedb.seeds"), crawlerConfig.pagedbSeeds);
    	FileUtil.writeFile(new File(crawlerDir, "conf/hotspots.regex"), crawlerConfig.hotspots);
        
    	logger.info("creating pagedb");
    	File commandFile = new File(crawlerDir, "createPageDb.sh");
        String pageDbcommand = "sh "+commandFile.getAbsolutePath();

        Triad<Integer, String, String> ret = CommandUtil.execute(pageDbcommand, crawlerDir, logger);
        if (0 != ret.first()) {
        	throw new IOException("problem while creating pagedb. command: " + pageDbcommand + " in dir " + crawlerDir + " - " + ret.second() + " - " + ret.third());
        }
        
	}

	private static void installIndexer(String distDir, File installBasePath, IndexerConfig indexerConfig, SearcherConfig searcherConfig) throws IOException {
		logger.info("installing indexer");
		TarUtil.untarFile(FileUtil.getExistingFile(distDir+"/indexer.tgz", true, false, false), installBasePath);
		doCommonTasks(new File(installBasePath,"/indexer"), indexerConfig);

		Config indexerProperties = Config.getConfig("indexer.properties");
		indexerProperties.set("IndexLibrary.rsyncAccessString", indexerConfig.installOnHost);
		indexerProperties.set("IndexLibrary.remoteIndexUpdaters", searcherConfig.installOnHost + ":" + searcherConfig.installOnBasePort);
		indexerProperties.modifyOnDisk(new File(installBasePath,"/indexer/conf/indexer.properties"));
	}

	private static void installCacheServer(String distDir, File installBasePath, CacheServerConfig cacheServerConfig, CrawlerConfig crawlerConfig) throws IOException {
        logger.info("installing cache-server");
        TarUtil.untarFile(FileUtil.getExistingFile(distDir+"/cache-server.tgz", true, false, false), installBasePath);
		doCommonTasks(new File(installBasePath,"/cache-server"), cacheServerConfig);
        
        Config indexerProperties = Config.getConfig("multiCache.properties");
		indexerProperties.set("multiCache.hosts", crawlerConfig.installOnHost + ":" + crawlerConfig.installOnBasePort);
		indexerProperties.modifyOnDisk(new File(installBasePath,"/cache-server/conf/multiCache.properties"));

	}
	
	private static void installClusteringWeb(String distDir,File installBasePath,SearcherConfig searcherConfig,IndexerConfig indexerConfig,CrawlerConfig crawlerConfig,CacheServerConfig cacheServerConfig,ClusteringWebConfig clusteringWebConfig) throws IOException {
        logger.info("installing clustering-web");
        TarUtil.untarFile(FileUtil.getExistingFile(distDir+"/clustering-web.tgz", true, false, false), installBasePath);	
		doCommonTasks(new File(installBasePath,"/clustering-web"), clusteringWebConfig);
		
		String hosts = "";
		if (searcherConfig.install) hosts += searcherConfig.installOnHost + ":" + (searcherConfig.installOnBasePort + PortUtil.getOffset("clustering.rpc.searcher")) + ":" + searcherConfig.installOnBaseDir+"/searcher";
		if (indexerConfig.install) hosts += (hosts.length() == 0?"":",") + indexerConfig.installOnHost + ":" + (indexerConfig.installOnBasePort + PortUtil.getOffset("clustering.rpc.indexer")) + ":" + searcherConfig.installOnBaseDir+"/indexer";
		if (crawlerConfig.install) hosts += (hosts.length() == 0?"":",") + crawlerConfig.installOnHost + ":" + (crawlerConfig.installOnBasePort + PortUtil.getOffset("clustering.rpc.crawler")) + ":" + searcherConfig.installOnBaseDir+"/crawler"; 
		if (cacheServerConfig.install) hosts += (hosts.length() == 0?"":",") + cacheServerConfig.installOnHost + ":" + (cacheServerConfig.installOnBasePort + PortUtil.getOffset("clustering.rpc.cacheServer")) + ":" + searcherConfig.installOnBaseDir+"/cache-server"; 
		
		Config clusteringProperties = Config.getConfig("clustering.properties");
		clusteringProperties.set("clustering.nodes", hosts);
		clusteringProperties.modifyOnDisk(new File(installBasePath,"/clustering-web/conf/clustering.properties"));
	}
}
