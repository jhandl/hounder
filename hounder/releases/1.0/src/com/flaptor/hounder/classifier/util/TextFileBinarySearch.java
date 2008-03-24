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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;


/**
 * Given a text file that is sorted, retrieves some 
 * @author rafa
 *
 */
public class TextFileBinarySearch {
    RandomAccessFile reader;

    public TextFileBinarySearch(String txtFile) throws FileNotFoundException {
        this(new File(txtFile));
    }
    
    public TextFileBinarySearch(File txtFile) throws FileNotFoundException {
        reader= new RandomAccessFile(txtFile, "r");
    }

    public Set<String> getToken(String token) {
        String[] tkns;
        Set<String> res= new HashSet<String>();
        try {
            tkns= binarySearch(token, 0, reader.length());
        } catch (IOException e) {
            return res;
        }
        for (int i=1; i<tkns.length; i++){
            res.add(tkns[i]);
        }
        return res;
    }
    
    private String[] binarySearch(String token, long low, long high) throws IOException {
        if (high < low)
            return new String[0];
        long mid = (low + high) / 2;        
//        System.err.println("Checking: low= " + low + ", mid= "+ mid + ", high=" + high);
        if (mid > 0) {
                reader.seek(mid-1);
                String tm= reader.readLine();
                if (null == tm) return new String[0]; //EOF
        } else {
            reader.seek(0);
        }
        String tknLine= reader.readLine();
        if (null == tknLine) return new String[0]; //EOF
        String[] tkns= tknLine.split("\\s+");
        int rel= tkns[0].compareTo(token);
        if (0 == rel){
            return tkns;
        } else if (rel >0){
            return binarySearch(token, low, mid-1);
        } else { 
            return binarySearch(token, mid+1, high);
        }
    }
}
