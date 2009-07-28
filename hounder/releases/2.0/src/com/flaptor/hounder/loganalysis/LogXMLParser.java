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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML reader handler to insert log data in the database 
 * 
 * @author Martin Massera
 */
public class LogXMLParser {

	LogSubmitterDB submitter = new LogSubmitterDB();
	
	/**
	 * parses a xml read from the reader and inserts the read clicks in the database
	 * 
	 * @param reader the input with the XML
	 * @throws IOException
	 * @throws SAXException
	 */
	public void parse(Reader reader) throws IOException, SAXException {
		XMLReader parser = new SAXParser();
		parser.setContentHandler(new LogXMLHandler());
	    parser.parse(new InputSource(reader));
	}
	
	private void trackRead(Track track) {
    	long queryId = submitter.submitQuery(track.query, track.results, track.trackTime, track.userIP, null);
    	int distance = track.pageNo * 10 + track.indexOnPage; //TODO HOW MANY RESULTS PER PAGE 
    	submitter.submitClickedResult(queryId, new ILogSubmitter.Result(track.link, distance, track.clicked, track.tags));
	}
	
	private class LogXMLHandler extends DefaultHandler {
		private Track currentTrack;
		private String text;

		private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
		    text = new String (ch, start, length).trim();
		}
		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if ("track".equals(name)) currentTrack = new Track();
		}
		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			if ("track".equals(name)) {
		    	//TODO UNCLICKED results??
				currentTrack.clicked = true; //only clicked results are arriving now 
				trackRead(currentTrack);
			}
			if ("query".equals(name))		currentTrack.query = text;
			if ("results".equals(name))		currentTrack.results = Integer.parseInt(text);
			if ("link".equals(name))		currentTrack.link = text;
			if ("pageNo".equals(name))		currentTrack.pageNo = Integer.parseInt(text);
			if ("indexOnPage".equals(name))	currentTrack.indexOnPage = Integer.parseInt(text);
			if ("userId".equals(name))		currentTrack.userId = text;
			if ("userIP".equals(name))		currentTrack.userIP = text;
			if ("trackTime".equals(name))	{
				try {
					currentTrack.trackTime = (Date)dateFormatter.parse(text);
				} catch (ParseException e) {throw new SAXException(e);}
			}
			if ("subdomain".equals(name))	currentTrack.tags.add(text);
			if ("scope".equals(name))		currentTrack.tags.add(text);
			if ("category".equals(name))	currentTrack.tags.add(text);
		}
	}

	private static class Track {
		List<String> tags = new ArrayList<String>();

		String query;
		int results;
		String link;
		int pageNo;
		int indexOnPage;
		String userId;
		String userIP;
		Date trackTime;
		boolean clicked;
	}
	
	public static void main(String[] args) throws IOException, SAXException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><clicktrough><totalCount>95</totalCount><itemsPerPage>10</itemsPerPage><startIndex>0</startIndex><items><track><subdomain>slow.activeathletesearch.com</subdomain><query>velolalalala</query><category>running</category><scope>user</scope><results>420</results><link>http://www.eastcoastvelo.org/index.html</link><pageNo>1</pageNo><indexOnPage>9</indexOnPage><userId>ed379b77d066209d285d8870531a1e38</userId><userIP>192.168.1.129</userIP><trackTime>2007-10-07 14:18:41</trackTime></track><track><subdomain>slow.activeathletesearch.com</subdomain><query>velo</query><category>running</category><scope>user</scope><results>420</results><link>http://www.eastcoastvelo.org/index.html</link><pageNo>1</pageNo><indexOnPage>9</indexOnPage><userId>ed379b77d066209d285d8870531a1e38</userId><userIP>192.168.1.129</userIP><trackTime>2007-10-07 14:18:41</trackTime></track></items></clicktrough>";		
		new LogXMLParser().parse(new StringReader(xml));
	}
}
