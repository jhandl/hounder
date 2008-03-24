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
package com.flaptor.hounder.crawler.bean;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.flaptor.hounder.crawler.UrlPatterns;

/**
 * @author Flaptor Development Team
 */
public class PatternFileBean {

    private File patternsFile; 
    private List<List<String>> patterns =  null;
    private UrlPatterns urlPatterns = null;




    public PatternFileBean(File patternFile) throws IOException{
        this.patternsFile = patternFile;
        patterns = this.readPatterns(patternFile);
    }
        
    private List<List<String>> readPatterns (File patternFile) throws IOException{

        List<List<String>> patterns = new ArrayList<List<String>>(); 
        BufferedReader reader = new BufferedReader(new FileReader(patternFile)); 
        while (reader.ready()) { 
            String line = reader.readLine(); 
            if (line.length() > 0 && line.charAt(0) != '#') {  // ignore empty lines and comments

                String[] part = line.split("\\|\\|",2);  // the prefix and the pattern are separated by the '||' character of the tokens
                String tokens = part.length == 2 ? part[1]:"";
                part = part[0].split("\\|",2);
                String prefix = part[0]; 
                String pattern = part.length == 2 ? part[1] : ".*";


                List<String> list = new ArrayList<String>(3);
                list.add(prefix);
                list.add(pattern);
                list.add(tokens);
                patterns.add(list); 
            } 
        }

        try {
            this.urlPatterns = new UrlPatterns(patternFile.getAbsolutePath());
        } catch (Exception e) {
            this.urlPatterns = null;
        }
        
        return patterns;
    }


    private boolean validatePatterns(List<List<String>> lists) {
        boolean failed = false;
        for (List<String> list: lists) {
            try {
                Pattern.compile(list.get(1));
            } catch (Exception e) {
                list.add(3,e.getMessage());
                failed = true;
            }
            
        }
        return !failed;
    }


    public void writeToFile(List<List<String>> list) throws IOException, PatternFileBeanException{

        if (!validatePatterns(list)){
            throw new PatternFileBeanException(list);
        }

        // First of all, try to copy the file, for backup reasons
        File backup = new File(patternsFile.getAbsolutePath()+".bkp"); 
        File orig = new File(patternsFile.getAbsolutePath());

        patternsFile.renameTo(backup);

        // now, write the list to the orig file
        BufferedWriter writer = new BufferedWriter(new FileWriter(orig)); 
        int pipe = '|';
        int space = ' ';
        for (List<String> line: list) {
            writer.write(line.get(0).trim(),0,line.get(0).trim().length()); 
            writer.write(space);
            writer.write(pipe);
            writer.write(space);
            writer.write(line.get(1).trim(),0,line.get(1).trim().length()); 
            writer.write(space);
            writer.write(pipe);
            writer.write(pipe);
            writer.write(space);
            writer.write(line.get(2).trim(),0,line.get(2).trim().length()); 
            writer.newLine();
        }
        writer.close();

        patterns = list;
        urlPatterns = new UrlPatterns(orig.getAbsolutePath());
    }


    public List<List<String>> getPatterns() throws PatternFileBeanException{
        try {
            this.patterns = this.readPatterns(this.patternsFile);
            return this.patterns;
        } catch (IOException e) {  
            throw new PatternFileBeanException(patterns);
        }
    }

    public String getFilename() {
        return patternsFile.getAbsolutePath();
    }

    public class PatternFileBeanException extends Exception{
        private static final long serialVersionUID = 1L;
        public final List<List<String>> list;

        public PatternFileBeanException(List<List<String>> list) {
            this.list = list;
        }

        public List<List<String>> getList() {
            return list;
        }
    }

    public Set<String> getTokens(String url) throws PatternFileBeanException {
        if (null == urlPatterns) {
            throw new PatternFileBeanException(null); // HACK
        }

        return urlPatterns.getTokens(url);
    }

    public boolean matches(String url) throws PatternFileBeanException {
        if (null == urlPatterns) {
            throw new PatternFileBeanException(null); // HACK
        }
        
        return urlPatterns.match(url);
             
    }
}
