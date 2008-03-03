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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.util.Config;
import com.flaptor.util.JdbcConnectionFactory;
import com.flaptor.util.Execute;

/**
 * implementation of ILogger that fills a database with the data
 * 
 * @author Martin Massera
 */
public class LogSubmitterDB implements ILogSubmitter {
	
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	private static JdbcConnectionFactory db = new JdbcConnectionFactory(Config.getConfig("loganalysis.properties"));

	public long submitQuery(String query, int queryResults, Date time, String userIP, List<Result> firstResults) {
        Connection con = null;
		try {
			con = db.connect();
			String sqlQuery = "INSERT INTO queries (query, results, ip, time) VALUES (?,?,?,?)";
	        PreparedStatement prepQuery = con.prepareStatement(sqlQuery);
	        prepQuery.setString(1, query);
	        prepQuery.setInt(2, queryResults);
	        prepQuery.setString(3, userIP);
	        prepQuery.setTimestamp(4, new Timestamp(time.getTime()));
			prepQuery.executeUpdate();
	        ResultSet keys = prepQuery.getGeneratedKeys();
	        keys.first();
	        long id = keys.getLong(1);
	        if (firstResults != null) {
				for (Result result : firstResults) {
					insertResult(con, id, result, db);
				}
	        }
			return id;
		} catch (Exception e) {
			logger.error(e);
			throw new RuntimeException(e);
		} finally {
            if (con != null) Execute.close(con);
		}
	}

	public void submitClickedResult(long queryId, Result result) {
        Connection con = null;
	    try{
			db.connect();
			insertResult(con, queryId, result, db);
		} catch (Exception e) {
			logger.error(e);
			throw new RuntimeException(e);
		} finally {
            if (con != null) Execute.close(con);
		}
	}

	/**
	 * inserts a result with all the tags in the list
	 */
	private static void insertResult(Connection con, long queryId, Result res, JdbcConnectionFactory db) throws SQLException {
		con.setAutoCommit(false);
		
    	String sqlResults = "INSERT INTO results (link, clicked, distance, query_id) VALUES(?,?,?,?)";
        PreparedStatement prepResults = con.prepareStatement(sqlResults);

        prepResults.setString(1, res.getUrl());
        prepResults.setInt(2, res.isClicked() ? 1 : 0);
        prepResults.setInt(3, res.getDistance());
        prepResults.setLong(4, queryId);
    	prepResults.executeUpdate();
    	
		ResultSet rs = prepResults.getGeneratedKeys();
		rs.first();
		long resultId = rs.getLong(1);

		insertTags(con, resultId, res.getTags(), db);
    	
    	con.commit();
		con.setAutoCommit(true);
	}

	private static void insertTags(Connection con, long resultId, List<String> tags, JdbcConnectionFactory db) throws SQLException {
		PreparedStatement stmt;
		stmt = con.prepareStatement("INSERT INTO tag_lists (result_id, tag_id) VALUES (?,?)");	
		for (String tag : tags) {
			long tagId = getTagId(con, tag);
			stmt.setLong(1, resultId);
			stmt.setLong(2, tagId);
			stmt.executeUpdate();
		}
	}

	private static Map<String, Long> tagIds = new HashMap<String, Long>();
	
	private static long getTagId(Connection con, String tag) throws SQLException {
		Long id = tagIds.get(tag);
		if (id == null) {
			logger.debug("tag " + tag + " not in tag cache, fetching");
			PreparedStatement stmt = con.prepareStatement("SELECT tag_id FROM tags WHERE name = ?");
			stmt.setString(1, tag);
			ResultSet rs = stmt.executeQuery();
			if (!rs.first()) {
				throw new RuntimeException("no tag in database called " + tag);
			}
			id = rs.getLong(1);
			tagIds.put(tag, id);
		}
		return id;
	}

}
