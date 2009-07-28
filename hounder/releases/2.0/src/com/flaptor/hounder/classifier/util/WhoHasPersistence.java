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
package com.flaptor.hounder.classifier.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import com.flaptor.hounder.classifier.bayes.BayesCalculator;
import com.flaptor.util.sort.Comparator;
import com.flaptor.util.sort.Record;
import com.flaptor.util.sort.RecordInformation;
import com.flaptor.util.sort.RecordReader;
import com.flaptor.util.sort.RecordWriter;


/**
 * In previous versions, we added the data to a Map(token->TokenCounter) 
 * in memory. However such aproach consumes too much RAM and was discarded.
 * In this version we add the data to a file that is lately transformed 
 * into a Map backed by a file.
 * This class implements a file where partial data is saved, for late sort and
 * add to the map
 * @author rafa
 * @see BayesCalculator#computeProbabilities()
 * @see BayesCalculator#addData(java.util.Map, boolean, String)
 */
public class WhoHasPersistence implements RecordInformation {
    /******** RecordWriter ************/
    public class WHRecordWriter implements RecordWriter{
        Writer out;
        
        public WHRecordWriter(File fout) throws FileNotFoundException{
            this (fout, false);
        }
        
        public WHRecordWriter(File fout, boolean append) throws FileNotFoundException{
            this (new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fout, append)))); 
        }
        
        public WHRecordWriter(BufferedWriter out){
            this.out= out;
        }
        
        public void close() throws IOException {
            out.close();
        }

        public void writeRecord(Record rec) throws IOException {
            WHRecord r= (WHRecord )rec;
            WhoHasPersistence.writeWhoHasElement(out, r.getToken(), r.getUrls());
        }        
    }

    // Sadly we can not have a static method on a inner class. Hece this
    // method is here instead of inside WHRecordWriter
    public static  void writeWhoHasElement(Writer whFile, 
            String token, Set<String> urls) throws IOException{
        whFile.write(token);
        whFile.write(" ");
        for (String ur: urls){
            whFile.write(ur);
            whFile.write(" ");
        }
        whFile.write("\n");                        
    }

    /******** RecordReader ************/
    public class WHRecordReader implements RecordReader{
        BufferedReader in;
        
        public WHRecordReader(File in) throws FileNotFoundException{
            this (new BufferedReader(new InputStreamReader(new FileInputStream(in))));            
        }
        
        public WHRecordReader(BufferedReader in){
            this.in= in;
        }
        
        public void close() throws IOException {
            in.close();
        }

        public Record readRecord() throws IOException {
            String ln= in.readLine();
            if (null == ln) 
                return null; // EOF
            //System.err.println("READ=" + ln );
            String[] tkns= ln.split("\\s+");
            String tk= tkns[0];
            Set<String> s= new HashSet<String>();            
            for (String url: tkns){
                s.add(url);
            }
            s.remove(tk);
            return new WHRecord(tk, s);
        }
    }

    /******** Record ************/
    public class WHRecord implements Record {
        private String token;
        private Set<String> urls;
        
        public WHRecord(String token, String url) {
            this.token = token;
            this.urls= new HashSet<String>();
            urls.add(url);
        }
        
        public WHRecord(String token, Set<String>  urls) {
            this.token = token;
            this.urls= urls;
        }
        
        public String getToken() {
            return token;
        }
        public Set<String>  getUrls() {
            return urls;
        }
    }

    /******** Comparator ***********/
    public class WHComparator implements Comparator {
        public int compare(Object a, Object b) {
            if (!(a instanceof WHRecord && b instanceof WHRecord)) {
                throw new RuntimeException("Trying to compare objects that are not of type TokenCounterRecord.");
            }
            WHRecord ra= (WHRecord) a;
            WHRecord rb= (WHRecord) b;
            return ra.getToken().compareTo(rb.getToken());
        }
    }

    
    
    public WHRecord newRecord(String token, String url){
        return new WHRecord(token, url);
    }
    
    public Comparator getComparator() {
        return new WHComparator();
     }

     public RecordReader newRecordReader(File filein) throws IOException {
         return new WHRecordReader(filein);
     }

     public RecordWriter newRecordWriter(File fileout) throws IOException {
         return newRecordWriter(fileout, false);
     }

     public RecordWriter newRecordWriter(File fileout, boolean append) throws IOException {
         return new WHRecordWriter(fileout, append);
     }

}
