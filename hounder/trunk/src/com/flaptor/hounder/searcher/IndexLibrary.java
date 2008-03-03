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
package com.flaptor.search4j.searcher;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.flaptor.search4j.Index;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.Stoppable;

/**
 * @author Flaptor Development Team
 */
public class IndexLibrary implements LocalIndexUpdater, Stoppable {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static TimeUnit tUnit = TimeUnit.MILLISECONDS;
    private final Long indexDeleteDelay;
    private DelayQueue<DeleteIndexTask> deletes;
    private final ReloadableIndexHandler rih;
    private Index currentIndex;
    private File invalidIndex;
    private IndexUpdatesListener iul;

    public IndexLibrary(ReloadableIndexHandler rih) {
        this.rih = rih;
        deletes = new DelayQueue<DeleteIndexTask>();
        Config config = Config.getConfig("searcher.properties");
        indexDeleteDelay = config.getLong("IndexLibrary.indexDeleteDelay");
        boolean cleanup = config.getBoolean("IndexLibrary.cleanupOnStartup");
        File baseDir = new File(Config.getConfig("common.properties").getString("baseDir"));
        File workingDir = null;
        try {
            workingDir = new File(baseDir,config.getString("searcher.dir") + File.separator + "indexes");
            workingDir = FileUtil.createOrGetDir(workingDir.getAbsolutePath(), true, true);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        } 
        File[] files = listFilesByDate_NewestFirst(workingDir);

        // find the newest valid index.
        currentIndex = null;
        File indexFile = null;
        for (File file : files) {
            try {
                if (!isLink(file)) {
                    currentIndex = new Index(file);
                    indexFile = file;
                    break; // if successful, this is the latest valid index.
                }
            } catch (Exception e) { 
                logger.error("Trying to open the file "+file.getAbsolutePath()+" as an index: "+e,e);
            } 
        }

        if (null == currentIndex) {
            logger.warn("Could not find a valid index in "+workingDir);
        } else {
            try {
                this.rih.setNewIndex(currentIndex);
                logger.info("Using newest un-corrupted index to start searcher.");
            } catch (IOException e) {
                logger.error("While initializing ReloadableIndexHandler with newest un-corrupted index: "+e,e);
            }

            // if cleanup enabled, delete the rest.
            if (cleanup) {
                for (File file : files) {
                    if (!file.equals(indexFile) && !isLink(file)) { // don't delete the current index or the symlink!
                        logger.info("CleanUp: Deleting the "+(file.isFile()?"file":"directory")+" "+file.getAbsolutePath());
                        FileUtil.deleteDir(file);
                    }
                }
            }
        }

        this.iul = new IndexUpdatesListener(this);

        // start thread to clean up
        new DeleteIndexTaskRunner().start();

    }
    
    public void requestStop() {
        iul.requestStop();
    }
    
    public boolean isStopped() {
        return iul.isStopped();
    }

    private boolean isLink(File file) {
        boolean islink = false;
        try {
            String fileName = file.getName();
            String targetName = file.getCanonicalFile().getName();
            islink = !fileName.equals(targetName);
        } catch (IOException e) {
            logger.error("Trying to determine if "+file.getPath()+" is a link",e);
        }
        return islink;
    }

    public boolean addIndex(Index index) {
        try {
            rih.setNewIndex(index);
            currentIndex = index;
            rih.flush();
            return true;
        } catch (IOException e) {
            logger.error(e,e);
            return false;
        }
    }


    public void discardIndex(Index index) {
        if (index.equals(currentIndex)) {
            // trying to discard the current index.
            // that is not the nicest thing, as the rih won't have an index
            logger.error("Trying to discard current index. Ignoring index discard");
            return;
        }
        if (index.exists()) {
            DeleteIndexTask task = new DeleteIndexTask(index,indexDeleteDelay);
            deletes.add(task);
        } else {
            logger.warn("Trying to discard index that no longer exists");
        }
    }


    public Index getCurrentIndex() {
        return currentIndex;
    }


    // PRIVATE CLASSES
    private class DeleteIndexTask implements Delayed {
        private long expireTime;
        private Index index;


        public DeleteIndexTask(Index index, long delay) {
            this.index = index;
            this.expireTime = System.currentTimeMillis() + delay;
        }

        public long getDelay(TimeUnit unit) {
            return tUnit.convert(expireTime - System.currentTimeMillis(),unit);
        }

        public int compareTo(Delayed del) {
            long myDelay = this.getDelay(tUnit);
            long otherDelay = del.getDelay(tUnit);
            return new Long(myDelay).compareTo(new Long(otherDelay));
        }

        public void deleteIndex() {
            if (index.exists()) {
                index.eraseFromDisk(); 
            } else {
                logger.warn("Trying to delete an index that no longer exists");
            }
        }

    }

    private class DeleteIndexTaskRunner extends Thread {

        public DeleteIndexTaskRunner(){
            this.setDaemon(true);
        }


        public void run() {
            while (true) {
                Execute.sleep(60000);
                DeleteIndexTask task = deletes.poll();
                if (null != task) {
                    task.deleteIndex();
                }
            }
        }
    }

    private static File[] listFilesByDate_NewestFirst(File dir) {
        File[] files = dir.listFiles();

        java.util.Arrays.sort(files,new Comparator<File>(){
            public int compare(File f1, File f2) {
                return new Long(f2.lastModified()).compareTo(f1.lastModified());
            }
        });
        return files;
    }

}
