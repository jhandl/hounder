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
package com.flaptor.hounder.indexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.flaptor.hounder.IRemoteIndexUpdater;
import com.flaptor.hounder.Index;
import com.flaptor.hounder.searcher.IndexUpdatesListenerStub;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.flaptor.util.PortUtil;
import com.flaptor.util.Stoppable;


/**
 * An IndexLibrary is a repository for indexes, produced by an IndexManager.
 * It keeps a directory with index copies, and can receive a new index
 * or give the latest index. When it receives a new index, and if there is
 * a certain count of indexes (10), it deletes the oldest index, until there
 * are only that count. This is a house-keeping technique, to avoid using
 * much disk space.
 *
 * Also, when it  receives an index, and if it has IRemoteIndexUpdaters 
 * configured, it pushes the index to the IRemoteIndexUpdaters.
 *
 * There is no way to ask the IndexLibrary to flush all indexes.
 *
 * @author Flaptor Development Team
 */
public class IndexLibrary implements Stoppable {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());

    // How many copies to keep
    private static int MAX_COPIES = 10;
    // Seconds to wait for previous updater to finish.
    private static int UPDATER_WAITING_TIME = 10; 
    private Config config;
    // Where to push the received indexes.
    private IRemoteIndexUpdater[] updaters;
    // The number of updaters not yet finished updating.
    private volatile int updating;
    // The number of udpater threads that have started.
    private volatile int inited;
    // Directory to store index copies.
    private File copiesDirectory;
    // The string to use as remoteHost on indexDescriptors for index updaters.
    private String rsyncAccessString;
    // True if requested to stop.
    private boolean stopping;

    private Indexer indexer;
    
    /**
     * Constructor.
     * Configuration file: indexer.properties
     * Configuration variables:
     *      IndexLibrary.remoteIndexUpdaters
     *          A comma-separated list of host:port formatted strings, 
     *          indicating where the IRemoteIndexUpdater instances are 
     *          listening for index pushes.
     *      IndexLibrary.rsyncAccessString
     *          A user@host formatted string, that IRemoteIndexUpdaters will
     *          use to connect through ssh to rsync indexes. In case ALL
     *          IRemoteIndexUpdaters are local, it can be left as the empty
     *          string, as the rsync will be between directories.
     *
     * @todo check if IndexManager prefixed variable name is ok, or we should
     * make all it IndexLibrary prefixed, and have a way for the IndexManager
     * to ask this value. It is not so important that both index and copies 
     * under the same directory, but it is "nice"
     *
     */
    public IndexLibrary(Indexer indexer) {
        this.indexer = indexer;
        File baseDir = new File(Config.getConfig("common.properties").getString("baseDir"));
        config = Config.getConfig("indexer.properties");
        String copiesPath = config.getString("indexer.dir") + File.separator + "indexes" + File.separator + "copies";
        copiesDirectory = new File(baseDir, copiesPath);

        
        // Need to push indexes to searchers/Backup?
        String[] remoteIndexUpdaters = config.getStringArray("IndexLibrary.remoteIndexUpdaters");
        
        if (remoteIndexUpdaters.length == 0) logger.warn("no remote index updaters");
        	
        updaters = new IRemoteIndexUpdater[remoteIndexUpdaters.length];
        updating = 0;

        for (int i = 0; i < remoteIndexUpdaters.length; i++ ) {
            Pair<String, Integer> host = PortUtil.parseHost(remoteIndexUpdaters[i]);
            logger.info("Creating updater stub on " + host.first() + ":" + host.last() );
            updaters[i] = new IndexUpdatesListenerStub(host.last(),host.first());
        }
        
        rsyncAccessString = config.getString("IndexLibrary.rsyncAccessString"); 
        setCopiesDirectory();

        stopping = false;
    }

   

    /**
     * Adds an index to this library. The index will be copied, so the
     * parameter will not be modified. 
     *
     * @param index
     *          The index to add to this library
     * @return
     *          a boolean indicating if the operation succeded. The operation
     *          succeds iff the index has been copied to disk.
     *
     */
    public synchronized boolean addIndex(Index index) {

        if (stopping) {
            throw new IllegalStateException("The IndexLibrary is stopping, can't add a new index");
        }

        // Don't let it continue if there is a previous call still running.
        if (updating > 0) {
            logger.warn("The previous index update is still not finished, will wait. The index update interval may be too short.");
            while (updating > 0) {
                Execute.sleep(UPDATER_WAITING_TIME*1000, logger);
            }
        }

        // Make room for the new copy.
        while (getNumberOfCopies() >= MAX_COPIES) {
            com.flaptor.util.FileUtil.deleteDir(getFirstDirectoryCopy());
        }

        String newIndexName = "index-" + System.currentTimeMillis();
        File newCopyDir = new File(copiesDirectory, newIndexName);

        if (newCopyDir.exists()) {
            String s = "Destination directory exists. The main index should be in an uncorrupted state, so you may try to recover it.";
            logger.error(s);
            throw new IllegalStateException(s);
        } else {
            // Make a copy
            Index newIndexCopy = index.copyIndex(newCopyDir);
            // Set the hostname on the descriptor
            newIndexCopy.getIndexDescriptor().setRsyncAccessString(rsyncAccessString);
            // Close the copy
            newIndexCopy.close();

            // reset the variable that counts inited threads.
            synchronized(updaters) {
                inited = 0;
            }
            
            List<String> problems = new ArrayList<String>();
            // Now, send the index to the updaters.
            for (IRemoteIndexUpdater updater : updaters) {
                // Send the index with a thread so we don't have to serialize the sending process.
                new IndexUpdaterThread(updater, newIndexCopy, problems).start();
            }

            // Wait until all threads have started.
            while (inited < updaters.length) {
                Execute.sleep(50, logger);
            }
            if (indexer.getIndexerMonitoredNode() != null) {
                indexer.getIndexerMonitoredNode().setProperty("indexUpdateProblems", problems);
            }
        }

        return true;
    }

    /**
     * This thread handles one remote updater.
     */
    private class IndexUpdaterThread extends Thread {
        private IRemoteIndexUpdater updater;
        private Index index;
        private List<String> problems;
        public IndexUpdaterThread(IRemoteIndexUpdater updater, Index index, List<String> problems) {
            setName(Execute.whatIsMyName());
            this.updater = updater;
            this.index = index;
            this.problems = problems;
        }
        @Override
        public void run() {
            synchronized (updaters) {
                updating++;
                inited++;
            }
            try {
                boolean updated = updater.setNewIndex(index.getIndexDescriptor());
                if (!updated) {
                    String problem = "Could not set new index " + index.getIndexDescriptor().toString() + " on " + updater.toString();
                    logger.warn(problem);
                    synchronized (problems) {
                        problems.add(problem);
                    }
                }
            } catch (Exception e) {
                String problem = "While sending index (" +index.getIndexDescriptor().getRemotePath()+ ") : " +e;
                logger.error(problem,e);
                synchronized (problems) {
                    problems.add(problem);
                }
            } finally {
                synchronized (updaters) {
                    updating--;
                }
            }
        }
    }


    /**
     * Returns the absolute path of the last modified directory in the 
     * copies directory.
     *
     * @return 
     *      null if no directories are found, or the absolute path of the 
     *      latest modified directory.
     */
    public String getLatestDirectoryCopy() {
        String path = null;
        long latest = 0;
        String[] files = copiesDirectory.list();
        for (int i = 0; i < files.length; i++) {
            File file = new File(copiesDirectory, files[i]);
            if (file.isDirectory() && file.lastModified() > latest) {
                latest = file.lastModified();
                path = file.getAbsolutePath();
            }
        }
        return path;
    }





    /*----------------------*/
    // PRIVATE HELPERS

    /**
     * Sets up the copies directory.
     * Constructor helper method.
     * Verifies the existence or creates the copies directory inside the 
     * working directory.
     *
     * @throws IllegalArgumentException 
     *      if the index directory is not a directory, but a regular file.
     * @throws IOException 
     *      if there is a permissions problem with the directory.     
     */
    private void setCopiesDirectory() {
        boolean createdDirectory = false;
        if (!copiesDirectory.exists()) {
            logger.info("setCopiesDirectory: copies directory not found. Creating it.");
            createdDirectory = copiesDirectory.mkdirs();
            if (!createdDirectory) {
                String s = "asked to create copies directory ( " + copiesDirectory.getAbsolutePath() + " ), but could not create it.";
                logger.error(s);
                throw new RuntimeException(s);
            }
        } else {
            if (!copiesDirectory.isDirectory()) {
                String s = "setCopiesDirectory: copies present, but not a directory.";
                logger.error(s);
                throw new IllegalArgumentException(s);
            }
        }
        if (!copiesDirectory.canRead()) {
            String s = "setCopiesDirectory: don't have read permission over the copies directory.";
            // If the directory was created by us, give a more detailed error
            if (createdDirectory) {
                s += " It has been created by me, so the problem is likely to be with umask. Check default permissions for directories on your OS.";
            }
            logger.error(s);
            throw new IllegalStateException(s);
        }
        if (!copiesDirectory.canWrite()) {
            String s = "setCopiesDirectory: don't have write permission over the copies directory.";
            // If the directory was created by us, give a more detailed error
            if (createdDirectory) {
                s += " It has been created by me, so the problem is likely to be with umask. Check default permissions for directories on your OS.";
            }
            logger.error(s);
            throw new IllegalStateException(s);
        }
    } 

    /**
     * Returns the absolute path of the first modified directory in the 
     * copies directory.
     * 
     * @return 
     *      null if no directories are found, or the absolute path of the
     *      first modified directory.
     */
    private String getFirstDirectoryCopy() {
        String path = null;
        long latest = Long.MAX_VALUE;
        String[] files = copiesDirectory.list();
        for (int i = 0; i < files.length; i++) {
            File file = new File(copiesDirectory, files[i]);
            if (file.isDirectory() && file.lastModified() < latest) {
                latest = file.lastModified();
                path = file.getAbsolutePath();
            }
        }
        return path;
    }

    /**
     * Returns the number of directories in the copies directory.
     * @return the number of directories in the copies directory.
     */
    private int getNumberOfCopies() {
        int count = 0;
        String[] files = copiesDirectory.list();
        for (int i = 0; i < files.length; i++) {
            File file = new File(copiesDirectory, files[0]);
            if (file.isDirectory()) {
                count++;
            }
        }
        return count;
    }


    public synchronized void requestStop() {
        stopping = true;
    }

    public boolean isStopped() {
        return (stopping && (updating == 0));
    }


}
