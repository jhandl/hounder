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
package com.flaptor.search4j.crawler.bean;

import java.io.File;

/**
 * Bean to export a pagedb from a crawler to another.
 * 
 * @author Flaptor Development Team
 */
public class PageDBExportBean {

    private final File pagedb ;
    private String errorString;
    private String host;
    private String destination;

    public PageDBExportBean(File pagedb,String param) {
        this.pagedb = pagedb;
        String[] parts = param.split(":");
        if (parts.length == 1) {
            this.host = null;
            this.destination = parts[0];
        } else if (parts.length == 2) {
            this.host = parts[0];
            this.destination = parts[1];
        } else {
            throw new IllegalArgumentException("Can not export. Destination " + param + " is invalid.");
        }
        errorString = "";
    }


    public boolean export() {
        // First, check that the pagedb exists.
        if (!pagedb.exists()) {
            errorString = "The pagedb does not exist. Could not find file " + pagedb.getAbsolutePath();
            return false;
        }


        try {
            boolean success = false;
            // make tmp copy
            String remoteDestination = (null == host) ? destination : host +":"+destination;
            Process p = Runtime.getRuntime().exec("scp -r " + pagedb.getAbsolutePath() + " " + remoteDestination + ".tmp");
            p.waitFor();
            // check that tmp copy succeded
            if (0 == p.exitValue()) {
                if (null == host) {
                    p = Runtime.getRuntime().exec("mv " + destination + ".tmp " + destination);
                } else {
                    p = Runtime.getRuntime().exec("ssh " + host + " mv " + destination + ".tmp " + destination);
                }
                p.waitFor();
                success = ( 0 == p.exitValue());
                if (success ) {
                    errorString = "";
                } else {
                    errorString = "Could not move pagedb from temp to final destination on remote host";
                }
            } else {
                errorString = "Could not copy pagedb to remote host";
            }
            return success;
        } catch (Exception e) {
            errorString = e.getMessage();
            System.out.println(e);
        }
        return false;

    }

    public String getErrorString() {
        return errorString;
    }
}
