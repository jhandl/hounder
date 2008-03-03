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
package com.flaptor.search4j.loganalysis;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.flaptor.util.Config;
import com.flaptor.util.EmbeddedSqlServer;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.Pair;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;
/**
 * @author Flaptor Development Team
 */
public class ReportTest extends TestCase {

    Logger logger = Logger.getLogger(Execute.whoAmI());
    Random rnd = null;
    Config config;
    String tmpDir;
    EmbeddedSqlServer server;
    Connection con;




    // counters used to accumulate the info used for report verification
    HashMap<String,Integer> queryMap = null;
    HashMap<String,Integer> unclickedQueryMap = null;
    HashMap<String,Integer> unclickedResultMap = null;
    HashMap<String,ArrayList<Integer>> clickedResultMap = null;
    HashMap<String,SessionData> sessionMap = null;

    @Override
    public void setUp() throws Exception {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
//        rnd = new Random(System.currentTimeMillis());
        rnd = new Random(0);
        tmpDir = FileUtil.createTempDir("reporttest",".tmp").getAbsolutePath();

        server = new EmbeddedSqlServer(new File("src/com/flaptor/search4j/loganalysis/tables.sql"), "hsql");
        con = server.getConnection();
        config = Config.getConfig("loganalysis.properties");
        config.set("database.driver",server.getDriverSpec());
        config.set("database.url",server.getDBUrl());
        config.set("database.user",server.getUser());
        config.set("database.pass",server.getPass());

        queryMap = new HashMap<String,Integer>();
        unclickedQueryMap = new HashMap<String,Integer>();
        unclickedResultMap = new HashMap<String,Integer>();
        clickedResultMap = new HashMap<String,ArrayList<Integer>>();
        sessionMap = new HashMap<String,SessionData>();

        // create test data
        int loglen = 50;
        int maxQueries = 30;
        int maxResults = 50;
        int maxSources = 10;
        int maxCategories = 3;
        int maxTags = 5;
        String[] queries = new String[maxQueries];
        String[] results = new String[maxResults];
        String[] sources = new String[maxSources];
        for (int i=0; i<maxQueries; i++) {
            queries[i] = TestUtils.randomText(1,3).trim();
        }
        for (int i=0; i<maxResults; i++) {
            results[i] = TestUtils.randomText(1,4).trim();
        }
        for (int i=0; i<maxSources; i++) {
            sources[i] = rnd.nextInt(256)+"."+rnd.nextInt(256)+"."+rnd.nextInt(256)+"."+rnd.nextInt(256);
        }

        // fill tables with test data
        int catId = 10;
        int tagId = 10;
        for (int i=0; i<maxCategories; i++) { // add tag types
        	PreparedStatement prep = con.prepareStatement("insert into tag_types (type_id, name) values (?,?)");
        	prep.setInt(1, catId);
        	prep.setString(2, "category "+catId);
        	prep.executeUpdate();
        	for (int j=0; j<maxTags; j++) { // add tags of that type
        		prep = con.prepareStatement("insert into tags (tag_id, type_id, name) values (?,?,?)");
        		prep.setInt(1, tagId);
        		prep.setInt(2, catId);
        		prep.setString(3, "tag " + tagId);
                prep.executeUpdate();
                tagId++;
            }
            catId++;
        }

        long time = System.currentTimeMillis();
        int rid = 1;
        for (int qid=1; qid<=loglen; qid++) {
            // add query data
            time += (rnd.nextFloat() < 0.2f) ? 1000*60*60 : rnd.nextInt(1000*60); // usually less than a minute and a few 1 hour jumps
            Timestamp ts = new Timestamp(time);
            String query = queries[rnd.nextInt(maxQueries)];
            String ip = sources[rnd.nextInt(maxSources)];
            String sql = "insert into queries (query_id,query,ip,time) values ("+qid+",'"+query+"','"+ip+"','"+ts+"');";
            PreparedStatement prep = con.prepareStatement(sql);
            prep.executeUpdate();
            countData(queryMap, query);
            addSessionData(sessionMap, ip, time, query);
            // add results data
            boolean click = rnd.nextBoolean();
            int dist = 1+rnd.nextInt(20); // position of clicked result
            int first = (dist > 3) ? 3 : dist;
            for (int f=1; f<=first; f++) { // add first unclicked results
                String result = results[rnd.nextInt(maxResults)];
                sql = "insert into results (result_id,query_id,link,clicked,distance) values ("+rid+","+qid+",'"+result+"',0,"+f+");";
                prep = con.prepareStatement(sql);
                prep.executeUpdate();
                countData(unclickedResultMap, result);
                addTagData(rid,maxCategories*maxTags);
                rid++;
            }
            if (click) { // add clicked result
                String result = results[rnd.nextInt(maxResults)];
                sql = "insert into results (result_id,query_id,link,clicked,distance) values ("+rid+","+qid+",'"+result+"',1,"+dist+");";
                prep = con.prepareStatement(sql);
                prep.executeUpdate();
                addData(clickedResultMap, result, dist);
                addTagData(rid,maxCategories*maxTags);
                rid++;
            } else {
                countData(unclickedQueryMap, query);
            }
        }

/* for debugging
        showTable("queries");
        showTable("results");
        showTable("tag_lists");
        showTable("tags");
        showTable("tag_types");
*/

        // disconnect from server
        con.close();
    }


    private void addTagData(int rid, int maxTagId) throws Exception {
        int tag_count = 1+rnd.nextInt(2);
        for (int t=0; t<tag_count; t++) { // add result-tag relationships
            int tagId = 10+rnd.nextInt(maxTagId);
            String sql = "insert into tag_lists (result_id,tag_id) values ("+rid+","+tagId+");";
            PreparedStatement prep = con.prepareStatement(sql);
            prep.executeUpdate();
        }
    }

    public void tearDown() {
        server.stop();
        FileUtil.deleteDir(tmpDir);
    }



    private void showTable(String tableName) throws Exception {
        System.out.println("\nTABLE: "+tableName);
        String sql = "select * from "+tableName+";";
        PreparedStatement prep = con.prepareStatement(sql);
        ResultSet rs = prep.executeQuery();
        ResultSetMetaData md = rs.getMetaData();
        int colCount = md.getColumnCount();
        String line = "---------------------------------------------------------------------------------------";
        line = line + line + line;
        int totalWidth = 0;
        int limit = 30;
        String title = "";
        String sep = " | ";
        for (int c=1; c<=colCount; c++) {
            String colName = md.getColumnName(c);
            int width = md.getColumnDisplaySize(c);
            if (width > limit) width = limit;
            String out = String.format("%"+width+"S",colName);
            if (out.length() > limit) out = out.substring(0,limit);
            title += sep+out;
            totalWidth += sep.length()+out.length();
        }
        title += sep;
        totalWidth += sep.length()-2;
        System.out.println(" "+line.substring(0,totalWidth));
        System.out.println(title);
        System.out.println(" "+line.substring(0,totalWidth));
        while (rs.next()) {
            for (int c=1; c<=colCount; c++) {
                int width = md.getColumnDisplaySize(c);
                if (width > limit) width = limit;
                String data = rs.getString(c);
                String out = String.format("%"+width+"s",data);
                if (out.length() > limit) out = out.substring(0,limit);
                System.out.print(sep+out);
            }
            System.out.println(sep);
        }
        System.out.println(" "+line.substring(0,totalWidth));
        rs.close();
    }


    // accumulate data
    private void countData(HashMap<String,Integer> dataMap, String text) {
        int count = (dataMap.containsKey(text)) ? dataMap.get(text) : 0;
        dataMap.put(text,count+1);
    }

    // store data
    private void addData(HashMap<String,ArrayList<Integer>> dataMap, String text, int num) {
        ArrayList<Integer> data = dataMap.containsKey(text) ? dataMap.get(text) : new ArrayList<Integer>();
        data.add(num);
        dataMap.put(text,data);
    }


    // struct for storing query data
    private class QueryData {
        int sessionId;
        String query;
        public QueryData(int sessionId, String query) {
            this.sessionId = sessionId;
            this.query = query;
        }
    }

    // struct for storing session data
    private class SessionData {
        long lastTime;
        ArrayList<QueryData> queries;
        public SessionData(long lastTime) {
            this.lastTime = lastTime;
            queries = new ArrayList<QueryData>();
        }
    }

    // store session data
    private void addSessionData(HashMap<String,SessionData> dataMap, String ip, long time, String query) {
        int sessionId = 1;
        SessionData data = null;
        if (dataMap.containsKey(ip)) {
            data = dataMap.get(ip);
            sessionId = data.queries.get(data.queries.size()-1).sessionId;
            if (time - data.lastTime > 30*60*1000) sessionId++;
            data.lastTime = time;
        } else {
            data = new SessionData(time);
            dataMap.put(ip, data);
        }
        data.queries.add(new QueryData(sessionId,query));
    }


    // extract the N most common query sequences
    private ArrayList<Pair<Integer,String>> filterSessionData(HashMap<String,SessionData> dataMap, int n, int order) {
        HashMap<String,Integer> sequenceMap = new HashMap<String,Integer>();
        for (SessionData sessionData : dataMap.values()) {
            String seq = "";
            int lastId = 0;
            int len = 0;
            for (QueryData queryData : sessionData.queries) {
                if (queryData.sessionId == lastId) {
                    seq += " --> " + queryData.query; // TODO: fix this, it should be a struct
                    len++;
                } else {
                    if (len > 1) {
                        countData(sequenceMap, seq);
                    }
                    lastId = queryData.sessionId;
                    seq = queryData.query;
                    len = 1;
                }
            }
            if (len > 1) {
                countData(sequenceMap, seq);
            }
        }
        return filterData(sequenceMap,n,order);
    }


    // sort the data by the number stored and returns the first N items.
    private ArrayList<Pair<Integer,String>> filterData(HashMap<String,Integer> dataMap, int n, final int order) {
        ArrayList<Pair<Integer,String>> dataArray = new ArrayList<Pair<Integer,String>>();
        for (Map.Entry<String,Integer> entry : dataMap.entrySet()) {
            dataArray.add(new Pair<Integer,String>(entry.getValue(),entry.getKey()));
        }
        Collections.sort(dataArray, new Comparator<Pair<Integer,String>>(){
                    public int compare(Pair<Integer,String> o1, Pair<Integer,String> o2) {
                        int cmp = order * o1.first().compareTo(o2.first());
                        if (0 == cmp) cmp = o1.last().compareTo(o2.last());
                        return cmp;
                    }
                }
            );
        while (dataArray.size() > n) {
            dataArray.remove(dataArray.size()-1);
        }
        return dataArray;
    }

    // same as above but for averaged data
    private ArrayList<Pair<Integer,String>> filterAvgData(HashMap<String,ArrayList<Integer>> dataMap, int n, final int order) {
        HashMap<String,Integer> avgMap = new HashMap<String,Integer>();
        for (Map.Entry<String,ArrayList<Integer>> entry : dataMap.entrySet()) {
            int avg = 0;
            ArrayList<Integer> data = entry.getValue();
            if (data.size() > 0) {
                for (int num : data) {
                    avg += num;
                }
                avg /= data.size();
            }
            avgMap.put(entry.getKey(), avg);
        }
        return filterData(avgMap, n, order);
        
    }

    private void show(String msg, ArrayList<Pair<Integer,String>> list) {
        System.out.println("\n"+msg);
        for (Pair<Integer,String> item : list) {
            System.out.println(item.first()+" "+item.last());
        }
        System.out.println("-----------");
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testReports() throws Exception {
        ArrayList<Pair<Integer,String>> report, control;

        // test the mostSearchedQueries report
        report = Report.mostSearchedQueries(10, null, null, null);
        control = filterData(queryMap,10,-1);
//show("mostSearchedQueries",report);
        assertTrue("mostSearchedQueries report did not match the expected results", report.equals(control));

        // test the worstPlacedResults report
        report = Report.worstPlacedResults(10, null, null, null);
        control = filterAvgData(clickedResultMap,10,-1);
//show("worstPlacedResults",report);
        assertTrue("worstPlacedResults report did not match the expected results", report.equals(control));
        
        // test the bestPlacedSpam report
        report = Report.bestPlacedSpam(10, null, null, null);
        control = filterData(unclickedResultMap,10,-1);
//show("bestPlacedSpam",report);
        assertTrue("bestPlacedSpam report did not match the expected results", report.equals(control));
        
        // test the mostUnsuccessfulQueries report
        report = Report.mostUnsuccessfulQueries(10, null, null, null);
        control = filterData(unclickedQueryMap,10,-1);
//show("mostUnsuccessfulQueries",report);
        assertTrue("mostUnsuccessfulQueries report did not match the expected results", report.equals(control));

        // test the mostCommonQuerySequences report
        report = Report.mostCommonQuerySequences(10, null, null, null);
        control = filterSessionData(sessionMap,10,-1);
//show("mostCommonQuerySequences",report);
        assertTrue("mostCommonQuerySequences report did not match the expected results", report.equals(control));
    }

}

