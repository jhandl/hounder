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
package com.flaptor.hounder.crawler;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.nutch.fetcher.FetcherOutput;
import org.apache.nutch.io.ArrayFile;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.protocol.Content;

import com.flaptor.util.Execute;


/** 
 * This class reads and holds the data of a segment record.
 * It assumes it is used after a fetch.
 * @author Flaptor Development Team
 */
public class SegmentRecord {

    static Logger logger = Logger.getLogger(Execute.whoAmI());
    private String dir;
    private ArrayFile.Reader fetcherReader = null;
    private ArrayFile.Reader contentReader = null;
    private ArrayFile.Reader dataReader = null;
    private FetcherOutput fetcherOutput = null;
    private ParseData parseData = null;
    private Content content = null;
    private org.apache.nutch.fs.NutchFileSystem lfs;


    /** 
     * Inilializes the class 
     * @param dir Directory where the segment is stored.
     */
    public SegmentRecord (String dir) {
        try {
            lfs = org.apache.nutch.fs.NutchFileSystem.getNamed("local");
            this.dir = dir;
            File f;
            f = new File(dir, FetcherOutput.DIR_NAME);
            if (f.exists()) fetcherReader = new ArrayFile.Reader(lfs, f.toString());
            f = new File(dir, Content.DIR_NAME);
            if (f.exists()) contentReader = new ArrayFile.Reader(lfs, f.toString());
            f = new File(dir, ParseData.DIR_NAME);
            if (f.exists()) dataReader = new ArrayFile.Reader(lfs, f.toString());
            fetcherOutput = new FetcherOutput();
            parseData = new ParseData();
            content = new Content();
        } catch (Exception e) {
            logger.error ("Initializing a segment record [" + dir + "]: " + e, e);
        }
    }

    /** Reads a new segment record, returns false if read past end. */
    public boolean next () {
        try {
            if (null == fetcherReader || null == fetcherReader.next(fetcherOutput)) return false;
            if (null == contentReader || null == contentReader.next(content)) return false;
            if (null == dataReader || null == dataReader.next(parseData)) return false;
            return true;
        } catch (Exception e) {
            logger.error ("next: " + e, e);
            return false;
        }
    }

    /** Returns the current fetched url */
    public String getFetchedUrl() {
        return content.getUrl();
    }

    /** Closes a reader */
    private void close (ArrayFile.Reader rdr) {
        try {
            if (null != rdr) {
                rdr.close();
                rdr = null;
            }
        } catch (Exception e) {
            logger.error ("close: " + e, e);
        }
    }

    /** Closes the segment readers */
    public void close() {
        close (fetcherReader);
        close (contentReader);
        close (dataReader);
    }

    // Attribute getters
    public String getDir () { return dir; }
    public ArrayFile.Reader getFetcherReader () { return fetcherReader; }
    public ArrayFile.Reader getContentReader () { return contentReader; }
    public FetcherOutput getFetcherOutput () { return fetcherOutput; }
    public ParseData getParseData () { return parseData; }
    public Content getContent () { return content; }

    /** Return an identifier of this instance */
    public String id() {
        int i = dir.lastIndexOf('/');
        return dir.substring(i+1) + " ";
    }

}

