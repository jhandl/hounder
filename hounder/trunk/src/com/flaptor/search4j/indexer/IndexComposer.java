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
package com.flaptor.search4j.indexer;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.search4j.Index;
import com.flaptor.search4j.IndexDescriptor;
import com.flaptor.search4j.MultiIndex;
import com.flaptor.search4j.searcher.LocalIndexUpdater;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;


/**
 * This class is intended to create a composition of a bunch of indexes.
 * It needs a baseDir (to store compositions) and an array of cluster names 
 * to know which to use to update, and what to push.
 * @author Flaptor Development Team
 */
public class IndexComposer implements LocalIndexUpdater {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private File baseDir;
    private Index currentIndex;
    private Map<String,Index> indexes;
    private Set<String> knownClusters;

    public IndexComposer() {
        Config config = Config.getConfig("composer.properties");
        baseDir = new File(config.getString("IndexComposer.baseDir"));
        String[] clusters = config.getStringArray("IndexComposer.clusters");
        indexes = new HashMap<String,Index>(clusters.length);
        knownClusters = new HashSet<String>(clusters.length);

        for (String cluster: clusters) {
           knownClusters.add(cluster) ;
        }
        currentIndex = null;

        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    public boolean addIndex(Index index) {
        IndexDescriptor desc = index.getIndexDescriptor();
        if (!knownClusters.contains(desc.getClusterName())) {
            return false;
        }

        // If there was an old copy of that index, delete it
        Index oldFromSameCluster = indexes.get(desc.getClusterName());
        if (null != oldFromSameCluster) {
            logger.debug("erasing old index for cluster \"" + desc.getClusterName() + "\".");
            oldFromSameCluster.eraseFromDisk();
        }


        // add it to current indexes
        indexes.put(desc.getClusterName(),index);

        // if we have a copy of each of this clusters, we can
        // create a multi index.
        if (indexes.size() == knownClusters.size()) {
            Index[] toMerge = new Index[knownClusters.size()];
            int i = 0;
            for (String cluster: knownClusters) {
                toMerge[i] = indexes.get(cluster);
                i++;
            }
            
            // Merge them
            File newIndexDirectory = new File(baseDir,"index-"+System.currentTimeMillis());
            newIndexDirectory.mkdirs();
            Index newIndex = MultiIndex.compose(newIndexDirectory,toMerge);

            // delete old reference, and push new index.
            if (null != currentIndex) {
            	currentIndex.eraseFromDisk();
            }
            currentIndex = newIndex;

        }
        return true;
    }

    public Index getCurrentIndex(){
        return currentIndex;
    }
}


