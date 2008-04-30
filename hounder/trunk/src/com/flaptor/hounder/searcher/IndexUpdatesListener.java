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
package com.flaptor.hounder.searcher;

import java.io.File;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.flaptor.hounder.IRemoteIndexUpdater;
import com.flaptor.hounder.Index;
import com.flaptor.hounder.IndexDescriptor;
import com.flaptor.util.CommandUtil;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.PortUtil;
import com.flaptor.util.Stoppable;
import com.flaptor.util.remote.RmiServer;


/**
 * Listener for Remote Index Updates. It has the ability to copy a remote index
 * by request, and encapsulates rsync. It is an alternative to the good old
 * syncDirectoryIndexer.sh script.
 * 
 * When it receives an index, gives it to its LocalIndexUpdater, that will
 * perform something with it (Use it to search, compose it with something else
 * and push it, etc).
 * 
 * @author dbuthay
 *
 */
public class IndexUpdatesListener implements IRemoteIndexUpdater, Stoppable {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    // Where to store the indexes locally
    private final String indexDir; 
    // Whom to push the index retrieved.
    private final LocalIndexUpdater library;

    private boolean stopped = false;
    private final RmiServer server;

    /**
     * Constructor. Parameter is the "listener" of the 
     * retrieved indexes.
     * 
     * @param lib
     * 			A LocalIndexUpdater that will receive the retrieved indexes.
     */
    public IndexUpdatesListener(LocalIndexUpdater lib) {
        this.library = lib;

        Config config = Config.getConfig("searcher.properties");
        this.indexDir = Config.getConfig("common.properties").getString("baseDir") 
        					+ File.separator + config.getString("searcher.dir") 
        					+ File.separator + "indexes";
        int fetchIndexPort = PortUtil.getPort("post.new.index");
        server = new RmiServer(fetchIndexPort);
        server.addHandler(RmiServer.DEFAULT_SERVICE_NAME, this);
        server.start();
    }

    synchronized public void requestStop() {
        if (stopped) {
            throw new IllegalStateException("Server not running.");
        }
        stopped = true;
        server.requestStop();
    }
    
    synchronized public boolean isStopped() {
        return stopped;
    }


    /**
     * It retrieves the index in case the descriptor is ok, and forwards it to
     * @see IRemoteIndexUpdater#setNewIndex(IndexDescriptor) 
     */
    public synchronized boolean setNewIndex(IndexDescriptor descriptor) throws RemoteException {
        if (stopped) {
            throw new IllegalStateException("Server not running.");
        }
        logger.debug("setNewIndex: " + descriptor.toString() + " - " + descriptor.getRemotePath());
        try {
            String location = descriptor.getRemotePath();
            Index currentIndex = library.getCurrentIndex();
            File workingDir = new File(this.indexDir);

            if (location.endsWith("/")) location = location.substring(0,location.length() - 1);

            String newIndexName = location.substring(location.lastIndexOf("/"));

            // Make index copy, in case there is a current index.
            // otherwise, just create the directory
            if (null != currentIndex) {
                Index newIndex = currentIndex.copyIndex(new File(workingDir,newIndexName));
                newIndex.close();
            } else {
                new File(workingDir,newIndexName).mkdirs();
            }

            int exitValue;
            // So, we can now copy the index via rsync
            logger.info("executing rsync");
            exitValue = CommandUtil.execute("rsync -r -v -t -W --delete " + location + " " + workingDir.getCanonicalPath(), null, logger).first();
            logger.info("rsync finished with exit value = " + exitValue);

            if ( 0 != exitValue) {
                // already logged in Execute
                // TODO check what we want to do .. disk is probably full ..
                throw new RuntimeException("Could not make rsync");
            }

            // now, verify that the index did not shrink more than 10%
            File newIndexDir = new File(workingDir,newIndexName);
            if ((null != currentIndex) && (float)(currentIndex.length() * (0.1)) > (float)FileUtil.sizeOf(newIndexDir)) {
                String warning = "Trying to copy an index that shrinked more ";
                warning += "than 10%. It will not be erased nor used by the ";
                warning += "Searcher, and should be checked by a person. It is ";
                warning += " located at " + newIndexDir.getAbsolutePath();
                logger.warn(warning);
                return false;
            }

            // So, size constraints are fine .. Lets try check the index.
            // Read it
            Index index ;
            try {
                index = new Index(newIndexDir);
            } catch (Exception e) {
                logger.error(e,e);
                // Clean corrupted index
                logger.info("deleting directory: " + newIndexDir.getAbsolutePath());
                FileUtil.deleteDir(newIndexDir);
                return false;
            }
            library.addIndex(index);
            return true;
        } catch (Exception e) {
            logger.error(e,e);
            return false;
        }
    }

}
