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
package com.flaptor.hounder.util;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;

/**
 * @author Flaptor Development Team
 */
public class Idx {

    private static void check (boolean ok, String msg) {
        if (!ok) {
            String usage = "\n    Idx create <idxDir> <inputfile>\n" +
            "    Idx list <idxDir> [<limit>]\n" +
            "    Idx search <idxDir> <field> <value>\n" +
            "    Idx optimize <idxDir>\n" +
            "    Idx delete <idxDir> <field> <value>\n" +
            "    Idx merge <idxDirDest> <idxDirSource>\n" +
            "    Idx term-count <idxDir> <field>\n" +
            "    Idx hit-count <idxDir> <field> <value>\n" +
            "    Idx terms <idxDir> <field>\n" +
            "    Idx uncompound <idxDir>\n" +
            "    Idx compound <idxDir>\n" +
            "\n    The input and output line format for create and list is:   <field>[,s(tore)][,i(ndex)][,t(oken)]: <value>\n";
            if (null != msg) {
                System.err.println("\n  ERROR: " + msg);
            }
            System.err.println(usage);
            System.exit(0);
        }
    }


    public static void main (String arg[]) throws Exception {
        check(arg.length > 1, null);
        String cmd = arg[0];
        File idx = new File(arg[1]);
        if ("list".equals(cmd)) {
            int num = (arg.length > 2) ? Integer.parseInt(arg[2]) : -1;
            check(idx.exists(), "Index dir not found");
            IndexReader reader = IndexReader.open(idx);
            int docs = reader.numDocs();
            int max = reader.maxDoc();
            System.err.println ("Index contains " + docs + " documents plus " + (max-docs) + " deleted.");
            if (num > -1) {
                if (num == 0) num = docs;
                for (int i=0; i < max && i < num; i++) {
                    System.out.println("----------------------------------------");
                    if (!reader.isDeleted(i)) {
                        Document doc = reader.document(i);
                        List flds = doc.getFields();
                        Iterator iter = flds.iterator();
                        while (iter.hasNext()) {
                            Field fld = (Field)iter.next(); 
                            String attr = (fld.isIndexed() ? ",i" : "") + 
                                          (fld.isStored() ? ",s" : "") + 
                                          (fld.isTokenized() ? ",t" : "");
                            System.out.println (fld.name() + attr + ": " + fld.stringValue());
                        }
                    }
                }
                reader.close();
                System.out.println();
            }
        } else if ("search".equals(cmd)) {
            check(idx.exists(), "Index dir not found");
            check(arg.length>3, "Not enough arguments");
            String field = arg[2];
            String value = arg[3];
            IndexSearcher searcher = new IndexSearcher(IndexReader.open(idx));
            Hits hits = searcher.search(new TermQuery(new Term(field, value)));
            System.out.println("\nNumber of hits: "+hits.length()+"\n");
            Iterator it = hits.iterator();
            while (it.hasNext()) {
                Hit hit = (Hit)it.next();
                Document doc = hit.getDocument();
                List flds = doc.getFields();
                Iterator iter = flds.iterator();
                while (iter.hasNext()) {
                    Field fld = (Field)iter.next();
                    System.out.println (fld.name() + ": " + fld.stringValue());
                }
            }
            searcher.close();
            System.out.println();
        } else if ("delete".equals(cmd)) {
            check(idx.exists(), "Index dir not found");
            check(arg.length>3, "Not enough arguments");
            String field = arg[2];
            String value = arg[3];
            IndexReader reader = IndexReader.open(idx);
            reader.deleteDocuments(new Term(field,value));
            reader.close();
        } else if ("optimize".equals(cmd)) {
            IndexWriter writer = new IndexWriter(idx, new StandardAnalyzer(), false);
            writer.optimize();
            writer.close();
        } else if ("merge".equals(cmd)) {
            check(arg.length == 3, "not enough parameters");
            File idx2 = new File(arg[2]);
            check(idx.exists(), "Index dir 1 not found");
            check(idx2.exists(), "Index dir 2 not found");
            IndexReader reader = IndexReader.open(idx2);
            IndexWriter writer = new IndexWriter(idx, new StandardAnalyzer(), false);
            writer.addIndexes(new IndexReader[] {reader});
            writer.close();
            reader.close();
        } else if ("term-count".equals(cmd)) {
            check(arg.length == 3, "not enough parameters");
            check(idx.exists(), "Index dir not found");
            IndexReader reader = IndexReader.open(idx);
            String field = arg[2];
            int count = 0;
            TermEnum terms = reader.terms();
            while (terms.next()) {
                Term term = terms.term();
                if (term.field().equals(field)) count++;
            }
            terms.close();
            reader.close();
            System.out.println("Found " + count + " different values for field " + field);
        } else if ("hit-count".equals(cmd)) {
            check(arg.length > 3, "Not enough arguments");
            check(idx.exists(), "Index dir not found");
            String field = arg[2];
            String value = arg[3];
            IndexSearcher searcher = new IndexSearcher(IndexReader.open(idx));
            Hits hits = searcher.search(new TermQuery(new Term(field, value)));
            System.out.println("\nNumber of hits: "+hits.length()+"\n");
            searcher.close();
        } else if ("uncompound".equals(cmd)) {
            IndexWriter writer = new IndexWriter(idx, new StandardAnalyzer(), false);
            writer.setUseCompoundFile(false);
            writer.optimize();
            writer.close();
        } else if ("compound".equals(cmd)) {
            IndexWriter writer = new IndexWriter(idx, new StandardAnalyzer(), false);
            writer.setUseCompoundFile(true);
            writer.optimize();
            writer.close();
        } else if ("terms".equals(cmd)) {
            check(arg.length == 3, "not enough parameters");
            check(idx.exists(), "Index dir not found");
            String field = arg[2];
            IndexReader reader = IndexReader.open(idx);
            TermEnum terms = reader.terms();
            while (terms.next()) {
                Term t = terms.term();
                if (t.field().equals(field)) {
                    System.out.println(t.text());
                }
            }

        }

    }

}

