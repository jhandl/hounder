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

/**
 * This class implements a crawler module to collect stats
 * about documents, such as how many have been crawled,
 * how many are indexable, etc. This module must be at
 * the end of the pipeline
 * @author Flaptor Development Team
 */
import java.io.File;
import java.io.FileWriter;

import org.apache.log4j.Logger;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;

public class CrawlStatsModule extends AProcessorModule {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    File outDir;
    boolean writeStats = true;
    String outputfilename = "stats.txt";

    //stats since the beginning of times
    int totalDocsSeen = 0;
    int totalIndexable = 0;
    int totalHotspots = 0;

    //stats since last dumpStats command received
    //a dumpStats command marks the end of a cycle as far as this
    //module is concerned
    int cycleDocsSeen = 0;
    int cycleIndexable = 0;
    int cycleHotspots = 0;

    public CrawlStatsModule(String moduleName, Config globalConfig) {
        super(moduleName, globalConfig);
        outDir = new File (config.getString("output.directory"));
        if (!outDir.isDirectory()) {
            if (!outDir.mkdirs()) {
                logger.warn("could not create directory " + outDir + ", will log stats as warnings instead of dumping them to a file");
                writeStats = false;
            }
        }

    }

    public synchronized void internalProcess(FetchDocument doc) {
        totalDocsSeen++;
        cycleDocsSeen++;

        if (doc.hasTag(IS_HOTSPOT_TAG)) {
                totalHotspots++;
                cycleHotspots++;
        }

        if (doc.hasTag(EMIT_DOC_TAG)) {
                totalIndexable++;
                cycleIndexable++;
        }
        if (totalDocsSeen % 1000 == 0) {
            logger.debug("total docs seen: " + totalDocsSeen + ", indexable: " + totalIndexable + ", hotspots: " + totalHotspots);
        }
    }

    /**
     * This module knows only one command, the only requirement is that
     * its toString() representation must start with "dumpStats"
     */
    public synchronized void applyCommand(Object command) {
        String cmdstr = command.toString();
        if (cmdstr.startsWith("endCycle")) {
            String toWrite = "---" + new java.util.Date().toString() + "\ntotal docs seen: " + totalDocsSeen + "\ntotal indexable: " + totalIndexable
                + "\ntotal hotspots: " + totalHotspots + "\n--since last dump--\ndocs seen: " + cycleDocsSeen + "\nindexable: " + cycleIndexable +
                "\nhotspots: " + cycleHotspots + "\n---";
            if (writeStats) {
                try {
                    FileWriter outfilewriter = new FileWriter(new File(outDir, outputfilename) , true /* append if exists */);
                    outfilewriter.write(toWrite);
                    outfilewriter.close();
                }
                catch (java.io.IOException e) {
                    logger.error(e);
                }
            }
            else {
                logger.warn(toWrite);
            }
        }

        
        //reset the cycle variables
        cycleDocsSeen = 0;
        cycleIndexable = 0;
        cycleHotspots = 0;
    }

}
