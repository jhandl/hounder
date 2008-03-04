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
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;
import com.flaptor.util.JdbcConnectionFactory;

/**
 * @author Flaptor Development Team
 */
public class ReportFrontend {

	private static final Logger logger = Logger.getLogger(Execute.whoAmI());

	private static JdbcConnectionFactory db = Report.getDB();
	
	/**
     * Returns the list of reports provided by this class.
     */
    public ArrayList<String> listReports() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("Most Searched Queries");
        list.add("Most Unsuccessful Queries");
        list.add("Worst Placed Results");
        list.add("Best Placed Spam");
        list.add("Most Common Query Sequences");
        return list;
    }
	
    /**
     * Returns a list of tag types.
     */
    public ArrayList<String> listTagTypes() {
        Connection con = null;
        ArrayList<String> list = new ArrayList<String>();
        try {
            con = db.connect();
            String sql = "select name from tag_types";
            PreparedStatement prep = con.prepareStatement(sql);
            ResultSet rs = prep.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            rs.close();
        } catch (Exception e) {
            logger.error("Reading data from database",e);
        } finally {
            if (con != null) Execute.close(con);
        }
        return list;
    }


    /**
     * Returns a list of tags for the provided tag type.
     * @param typeName the name of the tag type
     * @return a list of tags for the provided tag type.
     */
    public ArrayList<String> listTags(String typeName) {
        ArrayList<String> list = new ArrayList<String>();
        Connection con = null;
        try {
            con = db.connect();
            String sql = "select t.name from tags t, tag_types tt where tt.name = ? and t.type_id = tt.type_id";
            PreparedStatement prep = con.prepareStatement(sql);
            prep.setString(1, typeName);
            ResultSet rs = prep.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            rs.close();
        } catch (Exception e) {
            logger.error("Reading data from database",e);
        } finally {
            if (con != null) Execute.close(con);
        }
        return list;
    }

}
