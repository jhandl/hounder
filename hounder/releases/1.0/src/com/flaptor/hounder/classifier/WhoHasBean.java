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
package com.flaptor.hounder.classifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.hounder.classifier.util.DocumentParser;
import com.flaptor.hounder.classifier.util.ProbsUtils;
import com.flaptor.hounder.classifier.util.TextFileBinarySearch;
import com.flaptor.hounder.classifier.util.WhoHasPersistence;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.cache.FileCache;
import com.flaptor.util.sort.MergeSort;
import com.flaptor.util.sort.RecordReader;
import com.flaptor.util.sort.RecordWriter;

/**
 * @author Flaptor Development Team
 */
public class WhoHasBean extends TrainingBean{

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private Config cfg; 
    
    private String TEMP_DIR;
    private String WHO_HAS_FILE_UNSORTED;
    private String WHO_HAS_FILE_SORTED;
    private String WHO_HAS_FILE_DONE;
    private RecordWriter whoHasFile= null;
    private static TextFileBinarySearch whoHasReader;
    private int maxTuple;
    public WhoHasBean(){}
    
    public boolean initialize(ConfigBean config) {
        if (!super.initialize(config)){
            return false;
        }
        cfg= Config.getConfig("classifier.properties"); 
        maxTuple= cfg.getInt("document.parser.tuples.size");
        inited= true;
        TEMP_DIR= cfg.getString("bayes.calculator.tmp.dir");
        try {
            FileUtil.createOrGetDir(TEMP_DIR, true, true);
        } catch (IOException e) {
            logger.error("Cant create directory " + TEMP_DIR, e);
            return false;
        }
        initialize("/whoHasUnsorted", "/whoHasSorted", TEMP_DIR + "/whoHasDone", TEMP_DIR);
        return inited;
    }
    
    void initialize(String unsortedFile, String sortedFile, 
            String doneFile, String tmpDir) {
        WHO_HAS_FILE_UNSORTED= tmpDir + unsortedFile;
        WHO_HAS_FILE_SORTED= tmpDir + sortedFile;
        WHO_HAS_FILE_DONE= doneFile;        
        resetFiles();
    }
    
    void addData(Map<String,int[]>documentTokenCount, 
            String url) throws IOException {
        Set<String> tokens = documentTokenCount.keySet();
        WhoHasPersistence.WHRecord whr;
        for (String token : tokens) {
            whr= new WhoHasPersistence().newRecord(token, url);
            whoHasFile.writeRecord(whr);
        }
    }    
    
    void computeWhoHas() throws FileNotFoundException, IOException{
        logger.info("Closing WhoHasPersistence...");        
        whoHasFile.close();        
        logger.info("Closing WhoHasPersistence.... done");
        
        logger.info("Sorting WhoHasPersistence....");
        File beforeSort= new File(WHO_HAS_FILE_UNSORTED);
        File afterSort= new File(WHO_HAS_FILE_SORTED);
        WhoHasPersistence whp= new WhoHasPersistence();
        MergeSort.sort(beforeSort, afterSort, whp);
        logger.info("Sorting WhoHasPersistence... done");

        logger.info("Writing WhoHas Total to ... " + WHO_HAS_FILE_DONE);
        RecordReader whrr= whp.newRecordReader(afterSort); 
        WhoHasPersistence.WHRecord whr= (WhoHasPersistence.WHRecord) whrr.readRecord();        
        if (null==whr) return;
        
//        FileUtil.deleteFile(WHO_HAS_FILE_DONE);
        FileWriter whDone= new FileWriter(WHO_HAS_FILE_DONE);

        HashSet<String> urls= new HashSet<String>();
        for (String url: whr.getUrls()){
            urls.add(url);
        }
        String prevToken= whr.getToken();
        while (null != (whr= (WhoHasPersistence.WHRecord) whrr.readRecord())){
            if (whr.getToken().equals(prevToken)){
                for (String url: whr.getUrls()){
                    urls.add(url);
                }
            } else {
                WhoHasPersistence.writeWhoHasElement(whDone, prevToken, urls);
                urls= new HashSet<String>();
                for (String url: whr.getUrls()){
                    urls.add(url);
                }
                prevToken= whr.getToken();
            }                        
        }
        WhoHasPersistence.writeWhoHasElement(whDone, prevToken, urls);
        whDone.flush();
        whDone.close();
        logger.info("Writing WhoHas ... done " + WHO_HAS_FILE_DONE);
    }

    public Set<String> getWhoHas(String url) throws IOException{
        if (null == whoHasReader){
            whoHasReader= new TextFileBinarySearch(WHO_HAS_FILE_DONE);
        }
        return whoHasReader.getToken(url);
    }
    
    private void resetFiles(){
        FileUtil.deleteFile(WHO_HAS_FILE_SORTED);
        FileUtil.deleteFile(WHO_HAS_FILE_UNSORTED);
        try {
            whoHasFile= new WhoHasPersistence().newRecordWriter(new File (
                    WHO_HAS_FILE_UNSORTED), true);  // This appends to existing file
        } catch (IOException e) {
            e.printStackTrace();
        }                        
    }
    
    public void calculate() throws IOException{
        resetFiles();
        Set<String> urls= new HashSet<String>();
        List<String> urlsList= null;
        for (String catName: getCategoryList()){            
            urlsList= ProbsUtils.loadUrlsList(config.getBaseDir(), catName, ProbsUtils.INCLUDED);
            urls.addAll(urlsList);
            urlsList= ProbsUtils.loadUrlsList(config.getBaseDir(), catName, ProbsUtils.NOT_INCLUDED);
            urls.addAll(urlsList);
        }
        FileCache<String>  cache = new FileCache<String> (config.getCacheDir() + "/text"); // TODO: softcode /text        
        for (String url: urls){
            String item=cache.getItem(url);
            if (null==item){
                logger.warn("Page " + url + "was not found in cache");
                continue;
            }
            addData(DocumentParser.parse(item, maxTuple), url);
        }  
        computeWhoHas();
    }
    
    public Date getWhoHasFileDate(){
        File f= new File(WHO_HAS_FILE_DONE);
        long t= f.lastModified();
        return new Date(t);        
    }
 
}
