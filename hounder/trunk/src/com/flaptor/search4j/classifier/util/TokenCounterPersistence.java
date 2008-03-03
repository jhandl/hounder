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
package com.flaptor.search4j.classifier.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.flaptor.search4j.classifier.bayes.BayesCalculator;
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
public class TokenCounterPersistence implements RecordInformation {
    /******** RecordWriter ************/
    public class TCRecordWriter implements RecordWriter{
        DataOutputStream out;
        
        public TCRecordWriter(File fout) throws FileNotFoundException{
            this (new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fout))));
        }
        
        public TCRecordWriter(DataOutputStream out){
            this.out= out;
        }
        public void close() throws IOException {
            out.close();
        }

        public void writeRecord(Record rec) throws IOException {
            TCRecord r= (TCRecord )rec;
            out.writeUTF(r.getToken());
            out.writeShort(r.getCatVal());
            out.writeShort(r.getNonCatVal());
//            out.writeUTF(new Integer(r.getCatVal()).toString());
//            out.writeUTF(new Integer(r.getNonCatVal()).toString());        
        }
    }

    /******** RecordReader ************/
    public class TCRecordReader implements RecordReader{
        DataInputStream in;
        
        public TCRecordReader(File in) throws FileNotFoundException{
            this (new DataInputStream(new BufferedInputStream(new FileInputStream(in))));            
        }
        
        public TCRecordReader(DataInputStream in){
            this.in= in;
        }
        
        public void close() throws IOException {
            in.close();
        }

        public Record readRecord() throws IOException {
            try {
                String tk= in.readUTF();
                int cat= in.readShort();
                int nCat= in.readShort();              
//              int cat= Integer.parseInt(in.readUTF());
//              int nCat= Integer.parseInt(in.readUTF());
                return new TCRecord(tk, cat, nCat);
            } catch (EOFException e) {
                return null;
            }
        }
    }

    /******** Record ************/
    public class TCRecord implements Record {
        private String token;
        private int catVal;
        private int nonCatVal;
        
        public TCRecord(String token, int catVal, int nonCatVal) {
            super();
            this.token = token;
            this.catVal = catVal;
            this.nonCatVal = nonCatVal;
        }
        
        public int getCatVal() {
            return catVal;
        }
        
        public int getNonCatVal() {
            return nonCatVal;
        }
        
        public String getToken() {
            return token;
        }
    }

    /******** Comparator ***********/
    public class TCComparator implements Comparator {
        public int compare(Object a, Object b) {
            if (!(a instanceof TCRecord && b instanceof TCRecord)) {
                throw new RuntimeException("Trying to compare objects that are not of type TokenCounterRecord.");
            }
            TCRecord ra= (TCRecord) a;
            TCRecord rb= (TCRecord) b;
            return ra.getToken().compareTo(rb.getToken());
        }
    }

    
    
    public TCRecord newRecord(String token, int catVal, int nonCatVal){
        return new TCRecord(token,catVal,nonCatVal);
    }
    
    public Comparator getComparator() {
        return new TCComparator();
     }

     public RecordReader newRecordReader(File filein) throws IOException {
         return new TCRecordReader(filein);
     }

     public RecordWriter newRecordWriter(File fileout) throws IOException {
         return new TCRecordWriter(fileout);
     }
}
