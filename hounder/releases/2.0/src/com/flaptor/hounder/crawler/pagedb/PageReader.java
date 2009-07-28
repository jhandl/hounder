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
package com.flaptor.hounder.crawler.pagedb;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.flaptor.util.Execute;
import com.flaptor.util.sort.RecordReader;

/**
 * @author Flaptor Development Team
 */
public class PageReader implements RecordReader {

    private static final int BUFFERSIZE = 65535;

    private ObjectInputStream inputStream = null;

    public PageReader (File file) throws IOException {
        inputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file), BUFFERSIZE));
    }

    public PageRecord readRecord () throws IOException {
        PageRecord rec = null;
        try {
            Page page = Page.read(inputStream);
            rec = new PageRecord();
            rec.setPage(page);
        } catch (java.io.OptionalDataException e) {System.out.println("EXCEPTION: "+e+" eof="+e.eof+" length="+e.length);
        } catch (java.io.EOFException e) {/* ignore, just return null */}
        return rec;
    }

    public void close () throws IOException {
        Execute.close(inputStream);
    }

}

