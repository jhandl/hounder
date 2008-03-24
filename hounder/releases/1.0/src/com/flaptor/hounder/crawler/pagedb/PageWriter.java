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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import com.flaptor.util.sort.Record;
import com.flaptor.util.sort.RecordWriter;

/**
 * @author Flaptor Development Team
 */
public class PageWriter implements RecordWriter {

    private static final int BUFFERSIZE = 65535;

    private ObjectOutputStream outputStream = null;

    public PageWriter (File file) throws IOException {
        outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file), BUFFERSIZE));
    }

    public void writeRecord (Record r) throws IOException {
        PageRecord rec = (PageRecord) r;
        Page page = rec.getPage();
        page.write(outputStream);
        outputStream.reset();
    }

    public void close () throws IOException {
        outputStream.close();
    }

}

