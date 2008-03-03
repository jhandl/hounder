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
package com.flaptor.search4j.classifier;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.search4j.classifier.util.ProbsUtils;
import com.flaptor.search4j.classifier.util.StateEnum;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;

/**
 * This Bean is in charge of the cat_{included,notIncluded,unknown} files
 * Other Beans can read those file (RO) but never write to them.
 * @author rafa
 *
 */
public class LearningBean extends TrainingBean{
    private static final Logger LOGGER = Logger.getLogger(Execute.whoAmI());
    private static final File TMP_DIR= FileUtil.createTempDir("training", Execute.whoAmI()); 



    private Map<String, List<String>> included;
    private Map<String, List<String>> notIncluded;
    private Map<String, List<String>> unknown;

    private List<String> inputData;

    public LearningBean() {
    }


    private void loadCategoryFiles(){
        included = new HashMap<String,List<String>>();
        notIncluded = new HashMap<String,List<String>>();
        unknown = new HashMap<String,List<String>>();

        for (String cat : config.getCategoryList()) {
            List<String> includedList= ProbsUtils.loadUrlsList(config.getBaseDir(), cat, ProbsUtils.INCLUDED);
            List<String> notIncludedList= ProbsUtils.loadUrlsList(config.getBaseDir(), cat, ProbsUtils.NOT_INCLUDED);
            List<String> unknownList= ProbsUtils.loadUrlsList(config.getBaseDir(), cat, ProbsUtils.UNKNOWN);
            if (null == includedList || null == notIncludedList || null == unknownList){
                throw new IllegalStateException("Couldn't initialize LearningBean");
            }

            // If included contains the url, it is marked as included.
            // notIncluded contains only urls that aren't already marked as included.
            // unknown contains only "actually unknown" urls.
            included.put(cat, includedList);
            notIncludedList.removeAll(includedList);
            notIncluded.put(cat, notIncludedList);
            unknownList.removeAll(includedList);
            unknownList.removeAll(notIncludedList);
            unknown.put(cat, unknownList);
        }
    }

    public boolean initialize(ConfigBean config) {
        LOGGER.info("Initializing LearningBean");
        if (!super.initialize(config)){
            inited= false;
            return false;
        }
        loadCategoryFiles();      
        inputData= UrlsBean.getUrls(config);
        inited= true;
        return inited;
    }

    /**
     * Return the url at por 'pos' from the file holding the input urls.
     * @param pos
     * @return
     */
    public synchronized String getUrl(int pos) {
        if (pos < inputData.size()) {
            return inputData.get(pos);
        } else {
            return null;
        }
    }

    public synchronized int getUrlId(String url){
        return inputData.indexOf(url);
    }

    /**
     * Return how many urls are in the file holding the input urls.
     * Used by the LearningBean to show/hide the 'Next' link
     * @return
     */
    public synchronized int getUrlCount() {
        return inputData.size();
    }

    /**
     * Called by the bean when the user clicks classify and a checkbox Y is 
     * marked
     * @param category
     * @param url
     * @return
     * note If the url was already added to the category, will NOT be added twice.
     */
    public synchronized boolean addToCategory(String category, String url) {
        LOGGER.debug("Adding " +url+ " to " +category);
        if (included.get(category).contains(url)){
            return true;
        }
        included.get(category).add(url);
        notIncluded.get(category).remove(url);
        unknown.get(category).remove(url);
        return true;
    }

    /**
     * Called by the bean when the user clicks classify and a checkbox N is 
     * marked
     * @param category
     * @param url
     * @return
     * note If the url was already added to the non-category, will NOT be added twice. 
     */
    public synchronized boolean removeFromCategory(String category, String url) {
        LOGGER.debug("Removing " +url+ " from " +category);
        if (notIncluded.get(category).contains(url)){
            return true;
        }
        included.get(category).remove(url);
        notIncluded.get(category).add(url);
        unknown.get(category).remove(url);
        return true;
    }

    /**
     * Called by the bean when the user clicks classify and no checkbox 
     * (Y nor N) is marked
     * @param category
     * @param url
     * @return
     * note If the url was already marked as unknown, will NOT be added twice. 
     */
    public synchronized boolean markAsUnknown(String category, String url) {
        LOGGER.debug("Marking " +url+ " as unknown for " +category);
        if (unknown.get(category).contains(url)){
            return true;
        }
        included.get(category).remove(url);
        notIncluded.get(category).remove(url);
        unknown.get(category).add(url);
        return true;
    }


    private synchronized boolean saveUrlFiles(String dirName) {
        boolean ok= true;
        for (String cat : config.getCategoryList()) {
            if (!ProbsUtils.saveUrlList(dirName, cat, 
                    ProbsUtils.INCLUDED, included.get(cat))){
                ok= false;
                break;
            }
            if (!ProbsUtils.saveUrlList(dirName, cat, 
                    ProbsUtils.NOT_INCLUDED, notIncluded.get(cat))){
                ok= false;
                break;
            }
            if (!ProbsUtils.saveUrlList(dirName, cat, 
                    ProbsUtils.UNKNOWN, unknown.get(cat))){
                ok= false;
                break;
            }
        }
        return ok;
    }

    /**
     * Saves all the files for each category
     * @return
     */
    public synchronized boolean saveData() {        
        LOGGER.debug("Saving data");        
        if (!saveUrlFiles(TMP_DIR.getAbsolutePath())){
            LOGGER.error("Error saving categories backup file. Exiting");
            System.exit(-1);            
        }
        if (!saveUrlFiles(config.getBaseDir())){
            LOGGER.error("Error saving categories file. Backup files are in " +
                    TMP_DIR.getAbsoluteFile() + ". Exiting.");
            System.exit(-1);            
        } else{
            LOGGER.debug("Saving data done.");
        }
        return true;
    }

    /**
     * Given a URL, says for each category if it's StateEnum (INCLUDED, 
     * REMOVED, UNKNOWN)
     * @param url
     * @return
     */
    public synchronized Map<String, StateEnum> getStates(String url) {
        Map<String, StateEnum> ret = new HashMap<String, StateEnum>(config.getCategoryList().length);
        for (String cat : config.getCategoryList()) {
            ret.put(cat, getState(cat, url));
        }
        return ret;
    }


    /**
     * Given an url and a category, saaty if the url belongs to that category.
     * Note, it doesnt made the categorization, it only checks if the URL was
     * already categorized and returns it category. 
     * @param category
     * @param url
     * @return StateEnum.INCLUDED, StateEnum.REMOVED or StateEnum.REMOVED
     */
    public synchronized StateEnum getState(String category, String url) {
        if (included.get(category).contains(url)) {
            return StateEnum.INCLUDED;
        } else if (notIncluded.get(category).contains(url)) {
            return StateEnum.REMOVED;
        } else if (unknown.get(category).contains(url)) {
            return StateEnum.UNKNOWN;
        } else {
            LOGGER.warn("Unknown url " +url);
            return StateEnum.UNKNOWN;
        }
    }
}

