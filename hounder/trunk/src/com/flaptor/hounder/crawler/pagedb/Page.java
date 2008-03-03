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
package com.flaptor.search4j.crawler.pagedb;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Arrays;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.TextSignature;
import com.flaptor.util.TranscodeUtil;

/**
 * @author Flaptor Development Team
 */
public class Page implements Serializable {
    private static final int CURRENT_VERSION = 5;
    private String url;
    private float score;
    private float priority;
    private int distance;
    private int retries;
    private long lastAttempt;
    private long lastSuccess;
    private long lastChange;
    private boolean isLocal;
    private boolean emitted;
    private int numInlinks;
    private byte[] urlHash;
    private HashSet<String> anchors;
    private HashSet<String> parents;
    private TextSignature signature;
    private static boolean configured = false;
    private static float similarityThreshold;

    /**
     * Read the configuration parameters.
     */
    private synchronized void readConfig() {
        if (!configured) {
            Config config = Config.getConfig("crawler.properties");
            similarityThreshold = config.getFloat("page.similarity.threshold");
            configured = true;
        }
    }


    /**
     * Initialize all fields.
     */
    private void init() {
        url = "";
        score = 0f;
        priority = 0;
        distance = 0;
        retries = 0;
        lastAttempt = 0;
        lastSuccess = 0;
        lastChange = 0;
        isLocal = true;
        emitted = false;
        numInlinks = 0;
        urlHash = new byte[16];
        anchors = new HashSet<String>();
        parents = new HashSet<String>();
        signature = new TextSignature("");
    }


    /**
     * Dafault constructor.
     */
    private Page() {
        init();
    }


    /**
     * Page constructor.
     * Used by Crawler when adding outlinks.
     * Also used by PageDB.main() and PageDBTest.
     * @param url the url of this page.
     * @param score the score of this page.
     */
    public Page(String url, float score) throws MalformedURLException {
        this();
        setUrl(url);
        setScore(score);
        readConfig();
    }


    /**
     * Page constructor.
     * Used by CacheBean for the Learning application.
     * This constructor provides aditional data to avoid reading a config file.
     * @param url the url of this page.
     * @param score the score of this page.
     * @param pageSimilarityThreshold the similarity threshold of 2 pages.
     */
    public Page (String url, float score, float similarityThreshold) throws MalformedURLException {
        this(); // don't call this(url,score); we want to avoid calling readConfig().
        setUrl(url);
        setScore(score);
        Page.similarityThreshold = similarityThreshold;
    }



    /**
     * Sets the url for this page.
     * @param url the url for this page.
     */
    public void setUrl (String url) throws MalformedURLException {
        this.url = url;
        try {
            urlHash = MessageDigest.getInstance("MD5").digest(url.getBytes());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Security algorithm MD5 not implemented");
        }
    }

    /**
     * Return the url of this page.
     * @return the url of this page.
     */
    public String getUrl () {
        return url;
    }


    /**
     * Return the anchors of the links pointing to this page.
     * @return the anchors of the links pointing to this page.
     */
    public String[] getAnchors () {
        return anchors.toArray(new String[0]);
    }

    /**
     * Sets the anchors of this page to the provided set.
     * @param anchors set of anchors.
     */
    public void setAnchors (String[] anchors) {
        this.anchors.clear();
        addAnchors(anchors);
    }

    /**
     * Adds an anchor to the anchor list.
     * @param anchor the anchor to be added.
     */
    public void addAnchor (String anchor) {
        if (null != anchor) {
            this.anchors.add(anchor);
        }
    }

    /**
     * Add an array of anchors to the anchor list of this page.
     * @param anchors array of anchors.
     */
    public void addAnchors (String[] anchors) {
        if (null != anchors) {
            this.anchors.addAll(Arrays.asList(anchors));
        }
    }

    /**
     * Return the urls of the pages linking to this page.
     * @return the urls of the pages linking to this page.
     */
    public String[] getParents () {
        return parents.toArray(new String[0]);
    }

    /**
     * Sets the parents of this page to the provided set.
     * @param urls set of parent urls.
     */
    public void setParents (String[] urls) {
        parents.clear();
        addParents(urls);
    }

    /**
     * Adds a parent to the parents list.
     * @param url the url of a parent page.
     */
    public void addParent (String url) {
        if (null != url) {
            parents.add(url);
        }
    }

    /**
     * Add an array of urls to the list of parents of this page.
     * @param urls array of urls of parent pages.
     */
    public void addParents (String[] urls) {
        if (null != urls) {
            parents.addAll(Arrays.asList(urls));
        }
    }


    /**
     * Returns the signature of the page.
     * @return the signature of the page.
     */
    public TextSignature getSignature () {
        return signature;
    }

    /**
     * Sets the signature of the page.
     * @param sig the signature of the page.
     */
    public void setSignature (TextSignature sig) {
        signature = sig;
    }

    /**
     * Returns true if this page is similar to a specified page.
     * @param otherPage the specified page.
     * @return true if this page is similar to the other page.
     */
    public boolean isSimilar (Page otherPage) {
        return hasSimilarSignature(otherPage.getSignature());
    }

    /**
     * Returns true if this page has a similar signature to the one provided.
     * @param sig the signature to compare against.
     * @return true if this page has a similar signature to the one provided.
     */
    public boolean hasSimilarSignature (TextSignature sig) {
        float similarity = getSignature().compareTo(sig);
        return (similarity >= similarityThreshold);
    }

    /**
     * Getter for the urlHash metadata.
     * #return the hash of the page url
     */
    public String getUrlHash () {
        return TranscodeUtil.binToHex(urlHash);
    }

    /** 
     * Setter for the distance metadata.
     * @param distance the distance of this page to a hotspot.
     */
    public void setDistance (int distance) {
        this.distance = distance;
    }

    /** 
     * Getter for the distance metadata.
     * @return the distance of this page to a hotspot.
     */
    public int getDistance () {
        return distance;
    }

    /** 
     * Setter for the lastAttempt metadata.
     * @param lastAttempt the date and time of the last fetch attempt.
     */
    public void setLastAttempt (long lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    /** 
     * Getter for the lastAttempt metadata.
     * @return the date and time of the last fetch attempt.
     */
    public long getLastAttempt () {
        return lastAttempt;
    }

    /** 
     * Setter for the lastSuccess metadata.
     * @param lastSuccess the date and time of the last fetch success.
     */
    public void setLastSuccess (long lastSuccess) {
        this.lastSuccess = lastSuccess;
    }

    /** 
     * Getter for the lastSuccess metadata.
     * @return the date and time of the last fetch success.
     */
    public long getLastSuccess () {
        return lastSuccess;
    }

    /** 
     * Setter for the lastChange metadata.
     * @param lastChange the date and time of the last contents change.
     */
    public void setLastChange (long lastChange) {
        this.lastChange = lastChange;
    }

    /** 
     * Getter for the lastChange metadata.
     * @return the date and time of the last contents change.
     */
    public long getLastChange () {
        return lastChange;
    }

    /** 
     * Setter for the retries metadata.
     * @param retries number unsuccessfut fetch attempts.
     */
    public void setRetries (int retries) {
        this.retries = retries;
    }

    /** 
     * Getter for the retries metadata.
     * @return the number unsuccessfut fetch attempts.
     */
    public int getRetries () {
        return retries;
    }

    /** 
     * Setter for the score metadata.
     * @param score the score of the page.
     */
    public void setScore (float score) {
        this.score = score;
    }

    /** 
     * Getter for the score metadata.
     * @return the score of the page.
     */
    public float getScore () {
        return score;
    }

    /** 
     * Setter for the priority metadata.
     * @param priority the priority of the page.
     */
    public void setPriority (float priority) {
        this.priority = priority;;
    }

    /** 
     * Getter for the priority metadata.
     * @return the priority of the page.
     */
    public float getPriority () {
        return priority;
    }

    /**
     * Setter for the emitted metadata.
     * @param emitted true if the page has been emitted, false otherwise.
     */
    public void setEmitted (boolean emitted) {
        this.emitted = emitted;
    }

    /**
     * Returns true if this page has been emitted.
     * @return true if this page has been emitted.
     */
    public boolean isEmitted () {
        return emitted;
    }

    /**
     * Setter for the inlinks metadata.
     * #param inlinks the number of incomming links to this page.
     */
    public void setNumInlinks (int numInlinks) {
        this.numInlinks = numInlinks;
    }

    /**
     * Getter for the inlinks metadata.
     * #return the number of incomming links to this page.
     */
    public int getNumInlinks () {
        return numInlinks;
    }

    /**
     * Setter for the isLocal metadata.
     * @param isLocal true if the page is local to this node of a distributed pagedb, false otherwise.
     */
    public void setLocal (boolean isLocal) {
        this.isLocal = isLocal;
    }

    /**
     * Getter for the isLocal metadata.
     * @return true if the page is local to this node of a distributed pagedb, false otherwise.
     */
    public boolean isLocal () {
        return isLocal;
    }


    // Serialization method to write to a stream
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeByte(CURRENT_VERSION);
        oos.writeObject(url);
        oos.writeObject(urlHash);
        oos.writeFloat(score);
        oos.writeFloat(priority);
        oos.writeLong(lastAttempt);
        oos.writeLong(lastSuccess);
        oos.writeInt(retries);
        oos.writeInt(distance);
        oos.writeInt(numInlinks);
        oos.writeInt(parents.size());
        for (String parent: parents) {
            oos.writeObject(parent);
        }
        oos.writeInt(anchors.size());
        for (String anchor:anchors) {
            oos.writeObject(anchor);
        }
        if (0L != lastSuccess) { 
            oos.writeLong(lastChange);
            oos.writeBoolean(isLocal);
            oos.writeBoolean(emitted);
            oos.writeObject(signature);
        } 
    }

    // Serialization method to read from a stream
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        init();
        int version = ois.readByte();
        // Page got redesigned in version 5, making it incompatible with all prior versions.
        if (version < 5 || version > CURRENT_VERSION) {
            throw new IOException("Incompatible Page version: v"+version+", expected v5 through v"+CURRENT_VERSION);
        }
        url = (String)ois.readObject();
        urlHash = (byte[])ois.readObject();
        score = ois.readFloat();
        priority = ois.readFloat();
        lastAttempt = ois.readLong();
        lastSuccess = ois.readLong();
        retries = ois.readInt();
        distance = ois.readInt();
        numInlinks = ois.readInt();
        parents.clear();
        int parentSize = ois.readInt(); // not necessarily = numInlinks; parent tracking may be disabled.
        for (int i = 0; i < parentSize; i++) {
            String p = (String)ois.readObject();
            parents.add(p);
        }
        anchors.clear();
        int anchorSize = ois.readInt();
        for (int i = 0; i < anchorSize; i++) {
            String a = (String)ois.readObject();
            anchors.add(a);
        }
        if (0L != lastSuccess) {
            lastChange = ois.readLong();
            isLocal = ois.readBoolean();
            emitted = ois.readBoolean();
            signature = (TextSignature)ois.readObject();
        }
        readConfig();
    }

    public void write(ObjectOutputStream oos) throws IOException {
        writeObject(oos);
    }

    public static Page read(ObjectInputStream ois) throws IOException {
        Page page = new Page();
        try {
            page.readObject(ois);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
        return page;
    }

    public String toString () {
        return "Page("
            +"url="+getUrl()
            +",urlhash="+getUrlHash()
            +",distance="+getDistance()
            +",retries="+getRetries()
            +",lastAttempt="+getLastAttempt()
            +",lastSuccess="+getLastSuccess()
            +",lastChange="+getLastChange()
            +",score="+getScore()
            +",priority="+getPriority()
            +",islocal="+isLocal()
            +",emitted="+isEmitted()
            +",numinlinks="+getNumInlinks()
            +",anchors="+Arrays.asList(getAnchors())
            +",parents="+Arrays.asList(getParents())
            +",signature="+getSignature()
            +")";
    }


    public boolean equals(Object obj) {
        if (!(obj instanceof Page)) return false;
        if (!getUrlHash().equals(((Page)obj).getUrlHash())) return false;
        return true;
    }


    public int hashCode() {
        return urlHash.hashCode();
    }


}

