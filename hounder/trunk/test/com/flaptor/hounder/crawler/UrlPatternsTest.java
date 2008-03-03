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
package com.flaptor.search4j.crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.PropertyConfigurator;

import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class UrlPatternsTest extends TestCase {

    Random rnd = null;
    PrintStream stdOut;
    PrintStream stdErr;
    String patterns_filename = "/tmp/testpatterns";

    public void setUp() throws IOException {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
        rnd = new Random(new Date().getTime());
        stdOut = System.out;
        stdErr = System.err;
        try {
//            System.setOut(new PrintStream(new File("/tmp/test_stdout")));
            System.setErr(new PrintStream(new File("/tmp/test_stderr")));
        } catch (Exception e) {}

    }

    public void tearDown() {
        System.setOut(stdOut);
        System.setErr(stdErr);
        FileUtil.deleteFile(patterns_filename);
    }

    private void writeFile (String filename, String text) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        String[] lines = text.split("#");
        for (String line : lines) {
            writer.write(line);
            writer.newLine();
        }
        writer.close();
    }

    public void checkMatch (String spec, String[] match, String[] nomatch) {
        try {
            writeFile (patterns_filename, spec);
            UrlPatterns hs = new UrlPatterns(patterns_filename);
            for (String line : match) {
                assertTrue("Didn't match a line that should match ("+line+")", hs.match(line));
            }
            for (String line : nomatch) {
                assertFalse("Matched a line that should not match ("+line+")", hs.match(line));
            }
        } catch (IOException e) {
            assertTrue(e.toString(), false);
        }
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testUrlPatterns () {
        checkMatch("", new String[] {}, new String[] {"none","xxx"});
        checkMatch("*", new String[] {"any","xxx"}, new String[] {});
        checkMatch("abc", new String[] {"abc"}, new String[] {"ab","abcd","xxx"});
        checkMatch("abc#xyz", new String[] {"abc","xyz"}, new String[] {"xxx"});
        checkMatch("abc|.*", new String[] {"abc","abcd"}, new String[] {"xxx"});
        checkMatch("abc|[a-z]", new String[] {"abcd","abcz"}, new String[] {"abc1","xxx"});
        checkMatch("abc|[a-z]#abc|[1-9]", new String[] {"abcd","abc1","abcX"}, new String[] {"abc-","xxx"});
    }

}

