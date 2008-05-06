package com.flaptor.hounder.clusterfest;

import java.util.ArrayList;
import com.flaptor.clusterfest.monitoring.DefaultPropertyFormatter;
import com.flaptor.clusterfest.monitoring.PropertyFormatter;
import com.flaptor.hist4j.Cell;
import com.flaptor.util.Pair;
import com.flaptor.util.Triad;
import com.flaptor.util.Statistics;
import com.flaptor.util.Statistics.EventStats;


public class SearcherFormatter implements PropertyFormatter {

	@Override
	@SuppressWarnings("unchecked")
	public String format(String name, Object value) {
		StringBuffer output = new StringBuffer();
		if (name.equals("averageTimes")) {
			ArrayList<Pair<String,Float>> averageTimes = (ArrayList<Pair<String,Float>>)value;
			for (Pair<String,Float> averageTime : averageTimes) {
				String searcher = averageTime.first();
				Float time = averageTime.last();
				if (time == Float.NaN) {
					output.append(searcher+": &lt;waiting&gt;<br>");
				} else {
					output.append(searcher+": "+time+" ms<br>");
				}
			}
		} else if (name.equals("responseTimes")
					|| name.equals("cacheHit")
					|| name.equals("cacheMiss")) {
			Triad<EventStats,EventStats,EventStats> stats = (Triad<EventStats,EventStats,EventStats>)value;
			EventStats accum = stats.first();
			EventStats curr = stats.second();
			EventStats last = stats.third();
			addStats(output,"Current",curr);
			addStats(output,"Last",last);
			addStats(output,"Accumulated",accum);
		} else {
			output.append((new DefaultPropertyFormatter()).format(name,value));
		}
		return output.toString();
	}
	
	private void addStats(StringBuffer output, String name, EventStats stats) {
		output.append("<b>"+name+"</b>");
		float avg = stats.getAvg();
		float med = stats.getMedian();
		float min = stats.getMin();
		float max = stats.getMax();
		long total = stats.totalSamples();
		long good = stats.correctSamples();
		long bad = stats.errorSamples();
		float ratio = stats.getErrorRatio();
		output.append(" ("+total+" samples: "+good+" good, "+bad+" bad ["+ratio+"%])<br/>");
		output.append("&nbsp;&nbsp;&nbsp;&nbsp;median: "+med+"<br/>");
		output.append("&nbsp;&nbsp;&nbsp;&nbsp;min: "+min+"<br/>");
		output.append("&nbsp;&nbsp;&nbsp;&nbsp;avg: "+avg+"<br/>");
		output.append("&nbsp;&nbsp;&nbsp;&nbsp;max: "+max+"<br/>");
	}

}
