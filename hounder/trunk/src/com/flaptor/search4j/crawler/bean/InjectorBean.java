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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.flaptor.search4j.crawler.pagedb.PageDB;

/**
 * Bean to inject urls for a crawler
 * 
 * @author Flaptor Development Team
 */
public class InjectorBean {

    private final File pagedb ;
    private String errorString;

    public InjectorBean(File pagedb) {
        this.pagedb = pagedb;
        errorString = "";
    }


    // given a String[] of urls, tries to inject them to a crawler.
    // This method returns false if there is a problem writing the urls
    // to a file, or if there is a pagedb waiting to be injected.
    // It calls PageDB.main, and then returns true.
    // PageDB.main can fail, but as it returns void, there is no way to know.
    public boolean injectURLs(List<String> urls) {
        // First, check that there is no injected
        // db on the wd, waiting for injection
        if (pagedb.exists()) {
            errorString = "There is another db waiting to be injected. If this persists, check that the crawler is running (to inject the other db first)";
            return false;
        }

        File urlFile = null;
        boolean errorFound = false;
        try { 
            // so, there is no pagedb waiting .. create and inject.
            urlFile = new File(pagedb.getParent() + "/injectedurls");
            FileWriter fw = new FileWriter(urlFile);
            BufferedWriter bw = new BufferedWriter(fw);

            try { 
                for (String url: urls) {
                    // Try to fix urls that do not start with http
                    if (!url.startsWith("http://")) {
                        url = "http://" + url;
                    }
                    // verify that the url is a valid url.
                    new URL(url);
                    bw.write(url);
                    bw.newLine();
                }
            } catch (MalformedURLException ex ) {
                errorFound = true;
                errorString = ex.getMessage();
                return false;
            }
            bw.close(); 
        } catch (IOException e) {
            System.out.println(e);
            // FIXME logging
            errorString = "IO error while writing urls. " + e.getMessage();
            return false;
        }

        String tmpName = pagedb.getAbsolutePath() + ".tmp";
        PageDB.main(new String[]{"create",tmpName ,urlFile.getAbsolutePath()});

        return new File(tmpName).renameTo(new File(pagedb.getAbsolutePath()));

    }

    public String getErrorString() {
        return errorString;
    }
}
