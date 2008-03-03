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
package com.flaptor.search4j.util;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Pattern;

import com.flaptor.search4j.indexer.RmiIndexerStub;

/**
 * @author Flaptor Development Team
 */
public class S4JDocumentGenerator {

    private Vector<String> words = new Vector<String>();
    private Random random = new Random();

    public S4JDocumentGenerator(String dictionaryPath) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(dictionaryPath));
            String str;
            while ((str = in.readLine()) != null) {
                words.add(str);
            }
            in.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }


    public String getDocument() {
        StringBuffer doc = new StringBuffer();
        String[] fields = { "name", "manu", "features", "includes" };
        doc.append("<documentAdd><documentId>" + random.nextInt(100000000) + "</documentId>");
        for (int i = 0; i < fields.length; i++) {
            doc.append("    <field name=\"" + fields[i] + "\" stored=\"true\" indexed=\"true\" tokenized=\"true\">");
            int numWords = random.nextInt(50);
            for (int j = 0; j < numWords; j++) {
                doc.append(randomWord() + " ");
            }
            doc.append("</field>");
        }
        doc.append("</documentAdd>");

        return doc.toString();
    }

    private String randomWord() {
        Pattern p = Pattern.compile("\\w*");
        while (true) {
            String word = (String)words.elementAt(random.nextInt(words.size()));
            if (p.matcher(word).matches()) return word;
        }

    }

    public static void main(String args[]) {
        S4JDocumentGenerator dg = new S4JDocumentGenerator(args[0]);
        try {
            RmiIndexerStub indexerStub = new RmiIndexerStub(9003, "localhost");

            for (int i=0; i < Integer.parseInt(args[1]); i++) {
                //String argstr[] = {"\'" + dg.getDocument() + "\'"};
                //System.out.println(dg.getDocument() + "\n\n");
                String doc = dg.getDocument();
                int result = indexerStub.index(doc);
                while (result == com.flaptor.search4j.indexer.Indexer.RETRY_QUEUE_FULL) {
                    System.out.println("queue full " + new Date());
                    try {
                        Thread.sleep(1000);
                    }
                    catch (Exception e) {
                        //ignore
                    }
                    result = indexerStub.index(doc);
                }
                if (i % 1000 == 0) {
                    System.out.println (i + " documents posted");
                    System.out.println(new Date());
                    //System.out.println(doc);
                }

            }

        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
