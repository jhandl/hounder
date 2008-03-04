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

import java.util.List;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class HtmlParserTest extends TestCase {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private HtmlParser.Output parse(String url, String text, String ignore) throws Exception {
        String ign= (null == ignore)? "": ignore;
        String ur= (null == url)? "http://domain.com/dir/test.html": url;
        HtmlParser parser = new HtmlParser(ign, new String[0]);
        HtmlParser.Output out = parser.parse(ur, text);
        return out;
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testTextExtractionXmlns() throws Exception {
        String text = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "+
                        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"+
                        " <html xmlns=\"http://www.w3.org/1999/xhtml\" >"+
                        "<sometag attr=\"no\"> one </sometag> " +
                        "<anothertag> two </anothertag> </html>";
        HtmlParser.Output out = parse(null, text, null);
        assertTrue("HtmlParser didn't produce expected output", "one two".equals(out.getText()));
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testTitleXmlns() throws Exception {
        String text = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "+
        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"+
        " <html xmlns=\"http://www.w3.org/1999/xhtml\" >"+
        "<head> <title> the title </title> </head> </html>";
        HtmlParser.Output out = parse(null, text, null);
        assertTrue("HtmlParser didn't extract the title", "the title".equals(out.getTitle()));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testIgnoreTagsXmlns() throws Exception {
        String text = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "+
        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"+
        " <html xmlns=\"http://www.w3.org/1999/xhtml\" >"+
        "<dontignore> right </dontignore> <ignorethis> wrong </ignorethis> </html>";
        HtmlParser.Output out = parse(null, text, "//IGNORETHIS");
        assertTrue("HtmlParser didn't produce expected output", "right".equals(out.getText()));
    }



    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testTextExtraction() throws Exception {
        String text = "<html> <sometag attr=\"no\"> one </sometag> <anothertag> two </anothertag> </html>";
        HtmlParser.Output out = parse("", text, null);
        assertTrue("HtmlParser didn't produce expected output", "one two".equals(out.getText()));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testTitle() throws Exception {
        String text = "<html> <head> <title> the title </title> </head> </html>";
        HtmlParser.Output out = parse("", text, null);
        assertTrue("HtmlParser didn't extract the title", "the title".equals(out.getTitle()));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testLinks() throws Exception {
        String text = "<html> <body> "+
            " <a href=\"http://domain.com/dir/first.html\"> first </a>"+
            " <a href=\"http://domain.com/dir/second page.html\"> second link </a> "+
            " <a href=\"http://domain.com/dir/three.html?one=1&two=2\"> three </a> "+
            " <a href=\"http://domain.com/dir/four.html?query=abc def\"> four </a> "+
            " <a href=\"http://domain.com/dir/five.html?query=abc#label\"> five </a> "+
            " <a href=\"/dir/six.html\"> six </a> "+
            " <a href=\"seven.html\"> seven </a> "+
            " </body> </html>";
        HtmlParser.Output out = parse("http://domain.com/dir/test.html", text, null);
        List<Pair<String,String>> links = out.getLinks();
        assertTrue("Didn't extract the right number or links", links.size() == 7);

        assertTrue("Didn't get the 1st link url right", "http://domain.com/dir/first.html".equals(links.get(0).first()));
        assertTrue("Didn't get the 1st link anchor right", "first".equals(links.get(0).last()));

        assertTrue("Didn't get the 2nd link url right", "http://domain.com/dir/second%20page.html".equals(links.get(1).first()));
        assertTrue("Didn't get the 2nd link anchor right", "second link".equals(links.get(1).last()));

        assertTrue("Didn't get the 3nd link url right", "http://domain.com/dir/three.html?one=1&two=2".equals(links.get(2).first()));
        assertTrue("Didn't get the 3nd link anchor right", "three".equals(links.get(2).last()));

        assertTrue("Didn't get the 4nd link url right", "http://domain.com/dir/four.html?query=abc+def".equals(links.get(3).first()));
        assertTrue("Didn't get the 4nd link anchor right", "four".equals(links.get(3).last()));

        assertTrue("Didn't get the 5nd link url right", "http://domain.com/dir/five.html?query=abc".equals(links.get(4).first()));
        assertTrue("Didn't get the 5nd link anchor right", "five".equals(links.get(4).last()));

        assertTrue("Didn't get the 6nd link url right", "http://domain.com/dir/six.html".equals(links.get(5).first()));
        assertTrue("Didn't get the 6nd link anchor right", "six".equals(links.get(5).last()));

        assertTrue("Didn't get the 7nd link url right", "http://domain.com/dir/seven.html".equals(links.get(6).first()));
        assertTrue("Didn't get the 7nd link anchor right", "seven".equals(links.get(6).last()));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testIgnoreTags() throws Exception {
        String text = "<html> <dontignore> right </dontignore> <ignorethis> wrong </ignorethis> </html>";
        HtmlParser.Output out = parse("", text, "//IGNORETHIS");
        assertTrue("HtmlParser didn't produce expected output", "right".equals(out.getText()));
    }

}

