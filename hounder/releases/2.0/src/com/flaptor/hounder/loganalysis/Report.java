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
package com.flaptor.hounder.loganalysis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.JdbcConnectionFactory;
import com.flaptor.util.Pair;

/**
 * these are the methods that calculate the reports on log data
 * @author Flaptor Development Team
 */
public class Report {

	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	private static JdbcConnectionFactory db = new JdbcConnectionFactory(Config.getConfig("loganalysis.properties"));

	public static JdbcConnectionFactory getDB() {
		return db;
	}
	
    /**
     * Returns the list of most frequent queries.
     * @param n the number of items in the report.
     * @return a list of queries with their frecuencies.
     */
    public static synchronized ArrayList<Pair<Integer,String>> mostSearchedQueries(int n, Date from, Date to, List<String> tags) {
        ArrayList<Pair<Integer,String>> res = new ArrayList<Pair<Integer,String>>();
        Connection con = null;
        try {
            con = db.connect();
            String sql = 
                "select count(*) as cant, query "
                + "from queries "
                + "where ? <= time and time <= ? "
                + "group by query order by cant desc, query limit ?";
            PreparedStatement prep = con.prepareStatement(sql);
            
            if (from == null) from = new Date(0);
            if (to == null) to = new Date(Long.MAX_VALUE);
            prep.setTimestamp(1, new Timestamp(from.getTime()));
            prep.setTimestamp(2, new Timestamp(to.getTime()));
            prep.setInt(3, n);
           
            ResultSet rs = prep.executeQuery();
            while (rs.next()) {
                res.add(new Pair<Integer,String>(rs.getInt(1),rs.getString(2)));
            }
            rs.close();
        } catch (Exception e) {
            logger.error("Reading data from database",e);
        } finally {
            if (con != null) Execute.close(con);
        }
        return res;
    }

    /**
     * Returns the list of most frequent queries that where unsuccessful (resulted in no clicked links).
     * @param n the number of items in the report.
     * @return a list of queries with their frecuencies.
     */
    public static synchronized ArrayList<Pair<Integer,String>> mostUnsuccessfulQueries(int n, Date from, Date to, List<String> tags) {
        ArrayList<Pair<Integer,String>> res = new ArrayList<Pair<Integer,String>>();
        Connection con = null;
        try {
            con = db.connect();
            String sql =
            	"select count(*) as cant, query "
            	+ "from queries q "
                + "where ? <= time AND time <= ? "
            	+ "and not exists (select * from results r where r.query_id = q.query_id and clicked = true) "
            	+ "group by query order by cant desc, query limit ?";
            PreparedStatement prep = con.prepareStatement(sql);

            if (from == null) from = new Date(0);
            if (to == null) to = new Date(Long.MAX_VALUE);
            prep.setTimestamp(1, new Timestamp(from.getTime()));
            prep.setTimestamp(2, new Timestamp(to.getTime()));
            prep.setInt(3, n);

            ResultSet rs = prep.executeQuery();
            while (rs.next()) {
                res.add(new Pair<Integer,String>(rs.getInt(1),rs.getString(2)));
            }
            rs.close();
        } catch (Exception e) {
            logger.error("Reading data from database",e);
        } finally {
            if (con != null) Execute.close(con);
        }
        return res;
    }

    /**
     * Returns the list of most frequent queries that where unsuccessful (resulted in no clicked links).
     * @param n the number of items in the report.
     * @return a list of results with their positions.
     */
    public static synchronized ArrayList<Pair<Integer,String>> worstPlacedResults(int n, Date from, Date to, List<String> tags) {
        ArrayList<Pair<Integer,String>> res = new ArrayList<Pair<Integer,String>>();
        Connection con = null;
        try {
            con = db.connect();
            String sql;
            String tagSpec = getTagIdSpec(con, tags);
            if (null != tagSpec) {
                sql = 
            	    "select avg(distance) as dist, link "
                    + "from results r, queries q, tag_lists t "
            	    + "where clicked = true "
            	    + "and r.query_id = q.query_id "
            	    + "and ? <= time and time <= ? "
                    + "and r.result_id = t.result_id "
                    + "and t.tag_id in ("+tagSpec+") "
            	    + "group by link order by dist desc, link limit ?";
            } else {
                sql = 
            	    "select avg(distance) as dist, link "
                    + "from results r, queries q "
            	    + "where clicked = true "
            	    + "and r.query_id = q.query_id "
            	    + "and ? <= time and time <= ? "
            	    + "group by link order by dist desc, link limit ?";
            }
            PreparedStatement prep = con.prepareStatement(sql);

            if (from == null) from = new Date(0);
            if (to == null) to = new Date(Long.MAX_VALUE);
            prep.setTimestamp(1, new Timestamp(from.getTime()));
            prep.setTimestamp(2, new Timestamp(to.getTime()));
            prep.setInt(3, n);

            ResultSet rs = prep.executeQuery();
            while (rs.next()) {
                res.add(new Pair<Integer,String>(rs.getInt(1),rs.getString(2)));
            }
            rs.close();
        } catch (Exception e) {
            logger.error("Reading data from database",e);
        } finally {
            if (con != null) Execute.close(con);
        }
        return res;
    }

    /**
     * This query returns the list of URLs that were shown. For each URL, it
     * returns if was clicked or not.
     * So if a URL was shown 3 times and clicked one of them, the array will 
     * have 4 positions with that URL: 3 with clicked= 0, and one with clicked=1
     * @param from Only URLs shown in queries between 'from' and 'to' will be return
     * @param to Only URLs shown in queries between 'from' and 'to' will be return
     * @param site  Only URLs from the specified site will be return. (ie: http://www.a.com)
     * @param tag Only URLs showns in querief for the specified tag will be return
     * @return a list of pairs <clicked,URL> that were shown under the requested
     * constrains.
     */
    public static synchronized ArrayList<Pair<Integer, String>> getClicks(Date from, Date to, 
            String site, List<String>  tags){
        ArrayList<Pair<Integer, String>> res = new ArrayList<Pair<Integer, String>>();
        Connection con = null;
        try {
            con = db.connect();
            StringBuffer sql;
            String tagSpec =  getTagIdSpec(con, tags);
            if (null != tagSpec && tagSpec.length() > 0) {
                sql= new StringBuffer (
                        "select distinct r.query_id, r.clicked, r.link "
                        + "from results r, queries q, tag_lists t "
                        + "where r.query_id = q.query_id "
                        + " and ? <= time and time <= ? "
                        + " and r.result_id = t.result_id "
                        + " and t.tag_id in ("+tagSpec+") ");
            }else {
                sql= new StringBuffer (
                        "select distinct r.query_id, r.clicked, r.link "
                        + "from results r, queries q "
                        + "where r.query_id = q.query_id "
                        + " and ? <= time and time <= ? ");
            }
            if (site != null && site.length() >0){
                sql.append(" and r.link LIKE '" + site + "%' ");
            }                
            sql.append("order by link");

            PreparedStatement prep = con.prepareStatement(sql.toString());
            if (null == from) from = new Date(0);
            if (null == to) to = new Date(Long.MAX_VALUE);
            prep.setTimestamp(1, new Timestamp(from.getTime()));
            prep.setTimestamp(2, new Timestamp(to.getTime()));

            ResultSet rs = prep.executeQuery();
            while (rs.next()) {
                res.add(new Pair<Integer, String>(rs.getInt(2),rs.getString(3)));
            }
            rs.close();

        } catch (Exception e) {
            logger.error("Reading data from database",e);
        } finally {
            if (con != null) Execute.close(con);
        }
        return res;
    }
    
    /**
     * Returns the list of results that most frequently appeared near the top results but have not been clicked.
     * @param n the number of items in the report.
     * @return a list of results with their frequencies.
     */
    public static synchronized ArrayList<Pair<Integer,String>> bestPlacedSpam(int n, Date from, Date to, List<String> tags) {
        ArrayList<Pair<Integer,String>> res = new ArrayList<Pair<Integer,String>>();
        Connection con = null;
        try {
            con = db.connect();
            String sql;
            String tagSpec = getTagIdSpec(con, tags);
            if (null != tagSpec) {
                sql = 
            	    "select count(*) as cant, link "
                    + "from results r, queries q, tag_lists t "
            	    + "where r.query_id = q.query_id "
            	    + "and clicked = false "
            	    + "and ? <= time and time <= ? "
                    + "and r.result_id = t.result_id "
                    + "and t.tag_id in ("+tagSpec+") "
            	    + "group by link order by cant desc, link limit ?";
            } else {
                sql = 
            	    "select count(*) as cant, link "
                    + "from results r, queries q "
            	    + "where r.query_id = q.query_id "
            	    + "and clicked = false "
            	    + "and ? <= time and time <= ? "
            	    + "group by link order by cant desc, link limit ?";
            }
            PreparedStatement prep = con.prepareStatement(sql);
            
            if (null == from) from = new Date(0);
            if (null == to) to = new Date(Long.MAX_VALUE);
            prep.setTimestamp(1, new Timestamp(from.getTime()));
            prep.setTimestamp(2, new Timestamp(to.getTime()));
            prep.setInt(3, n);

            ResultSet rs = prep.executeQuery();
            while (rs.next()) {
                res.add(new Pair<Integer,String>(rs.getInt(1),rs.getString(2)));
            }
            rs.close();
        } catch (Exception e) {
            logger.error("Reading data from database",e);
        } finally {
            if (con != null) Execute.close(con);
        }
        return res;
    }

    /**
     * Returns the list query sequences within one session by the same user.
     * @param n the number of items in the report.
     * @return a list of queries with their frequencies.
     */
    public static synchronized ArrayList<Pair<Integer,String>> mostCommonQuerySequences(int n, Date from, Date to, List<String> tags) {
        ArrayList<Pair<Integer,String>> res = new ArrayList<Pair<Integer,String>>();
        Connection con = null;
        try {
            con = db.connect();
            String sql = 
                "select ip, time, query from queries "
            	+ "where ? <= time and time <= ? "
                + "order by ip, time";
            PreparedStatement prep = con.prepareStatement(sql);

            if (null == from) from = new Date(0);
            if (null == to) to = new Date(Long.MAX_VALUE);
            prep.setTimestamp(1, new Timestamp(from.getTime()));
            prep.setTimestamp(2, new Timestamp(to.getTime()));

            prep.setFetchSize(1000);
            ResultSet rs = prep.executeQuery();

            String lastIp = null;
            long lastTime = 0;
            int sequenceLen = 0;
            String querySequence = "";
            HashMap<String,Integer> sequenceMap = new HashMap<String,Integer>();
            while (rs.next()) {
                String ip = rs.getString(1);
                long time = rs.getTimestamp(2).getTime();
                String query = rs.getString(3);
                if (ip.equals(lastIp) && (time - lastTime < 30*60*1000)) {
                    querySequence += " --> "+query;
                    sequenceLen++;
                } else {
                    if (sequenceLen > 1) {
                        addOccurrence(sequenceMap, querySequence);
                    }
                    querySequence = query;
                    sequenceLen = 1;
                }
                lastIp = ip;
                lastTime = time;
            }
            rs.close();
            res = filterOccurrences(sequenceMap, n, -1);
        } catch (Exception e) {
            logger.error("Reading data from database",e);
        } finally {
            if (con != null) Execute.close(con);
        }
        return res;
    }


    private static String getTagIdSpec(Connection con, List<String> tagNames) throws SQLException {
        String tagIdList = null;
        if (null != tagNames && tagNames.size() > 0) {
            String tagList = "";
            String sep = "";
            for (String tag : tagNames) {
                if (tag.indexOf("'") == -1) {
                    tagList += sep + "'" + tag + "'";
                    if ("".equals(sep)) sep = ",";
                }
            }
            String sql =
                "select tag_id "
                + "from tags "
                + "where name in ("+tagList+")";
            PreparedStatement prep = con.prepareStatement(sql);
            sep = "";
            tagIdList = "";
            ResultSet rs = prep.executeQuery();
            while (rs.next()) {
                tagIdList += sep + rs.getInt(1);
                if ("".equals(sep)) sep = ",";
            }
            rs.close();
        }
        return tagIdList;
    }

    // counts occurrences of strings
    private static void addOccurrence(HashMap<String,Integer> dataMap, String data) {
        int counter = 0;
        if (dataMap.containsKey(data)) {
            counter = dataMap.get(data);
        }
        counter++;
        dataMap.put(data, counter);
    }


    // sorts the data by number of occurrences and returns only the first N
    private static ArrayList<Pair<Integer,String>> filterOccurrences(HashMap<String,Integer> dataMap, int n, final int order) {
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


}
