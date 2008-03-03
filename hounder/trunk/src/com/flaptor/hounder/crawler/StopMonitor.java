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
package com.flaptor.search4j.crawler;

import java.io.File;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;



/**
 * This class implements a stop mechanism.
 * @author Flaptor Development Team
 */
//TODO FIXME why all static methods and constructor? why would i construct an object?
public class StopMonitor {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static boolean running = true;
    private static File stopFile;

    /**
     * Class initializer.
     * @param stopFileName file name that stops the system if present.
     */
    public StopMonitor (String stopFileName) {
        stopFile = new File(stopFileName);
        reset();
        StopMonitorThread mon = new StopMonitorThread();
        mon.start();
    }

    /**
     * Resets the stop signal.
     */
    public static void reset () {
        if (stopFile.exists()) {
            stopFile.delete();
        }
        running = true;
    }

    // This thread checks if the stop signal is present.
    private class StopMonitorThread extends Thread {
        public void run () {
            while (running) {
                running = !stopFile.exists();
                try { sleep(10000); } catch (Exception e) { /*ignore*/ }
            }
            logger.info("STOP file detected");
        }
    }

    /**
     * Returns if the stop signal is present
     * @return true if the stop signal is present.
     */
    public static boolean stopRequested () {
        return !running;
    }

    /**
     * Stops the system by internal request.
     */
    public static void stop () {
        running = false;
        logger.info("STOP requested internaly");
    }

}

