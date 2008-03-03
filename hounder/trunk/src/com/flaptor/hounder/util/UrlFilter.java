/**
 * Copyright 2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Changes made by Flaptor, under the same license.
 */
package com.flaptor.search4j.util;

import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.Perl5Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.MalformedPatternException;

/**
 * Filters URLs based on a file of regular expressions. The file is named by
 * property "urlfilter.regex.file" in ./conf/crawler.properties, and
 *
 * <p>The format of this file is:
 * <pre>
 * [+-]<regex>
 * </pre>
 * where plus means go ahead and index it and minus means no.
 * @author Flaptor Development Team
 */
public class UrlFilter {


  private static class Rule {
    public Perl5Pattern pattern;
    public boolean sign;
    public String regex;
  }

  private List<Rule> rules;
  private PatternMatcher matcher = new Perl5Matcher();

  public UrlFilter() throws IOException, MalformedPatternException {
    String filename = com.flaptor.util.FileUtil.getFilePathFromClasspath("regex-urlfilter.txt");
    rules = readConfigurationFile(new FileReader(filename));
  }

  public synchronized String filter(String url) {
    Iterator i=rules.iterator();
    while(i.hasNext()) {
      Rule r=(Rule) i.next();
      if (matcher.contains(url,r.pattern)) {
        //System.out.println("Matched " + r.regex);
        return r.sign ? url : null;
      }
    };
        
    return null;   // assume no go
  }

  //
  // Format of configuration file is
  //    
  // [+-]<regex>
  //
  // where plus means go ahead and index it and minus means no.
  // 

  private static List<Rule> readConfigurationFile(Reader reader)
    throws IOException, MalformedPatternException {

    BufferedReader in=new BufferedReader(reader);
    Perl5Compiler compiler=new Perl5Compiler();
    List<Rule> rules=new ArrayList<Rule>();
    String line;
       
    while((line=in.readLine())!=null) {
      if (line.length() == 0)
        continue;
      char first=line.charAt(0);
      boolean sign=false;
      switch (first) {
      case '+' : 
        sign=true;
        break;
      case '-' :
        sign=false;
        break;
      case ' ' : case '\n' : case '#' :           // skip blank & comment lines
        continue;
      default :
        throw new IOException("Invalid first character: "+line);
      }

      String regex=line.substring(1);

      Rule rule=new Rule();
      rule.pattern=(Perl5Pattern) compiler.compile(regex);
      rule.sign=sign;
      rule.regex=regex;
      rules.add(rule);
    }

    return rules;
  }

  public static void main(String args[])
    throws IOException, MalformedPatternException {

    UrlFilter filter=new UrlFilter();
    BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
    String line;
    while((line=in.readLine())!=null) {
      String out=filter.filter(line);
      if(out!=null) {
        System.out.print("+");
        System.out.println(out);
      } else {
        System.out.print("-");
        System.out.println(line);
      }
    }
  }

}
