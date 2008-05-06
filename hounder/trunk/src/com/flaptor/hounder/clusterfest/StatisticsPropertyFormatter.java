package com.flaptor.hounder.clusterfest;

import com.flaptor.clusterfest.monitoring.DefaultPropertyFormatter;
import com.flaptor.util.Statistics;

public class StatisticsPropertyFormatter extends DefaultPropertyFormatter {

	public String format(String name, Object value) {
		if (value instanceof Statistics) {
			return formatStatistics((Statistics) value);
		} else {
			return super.format(name, value);
		}
    }
	
	private String formatStatistics(Statistics stats) {
		StringBuffer buf = new StringBuffer();
		buf.append("<table><tbody><tr><th>Event name</th><th>Max</th><th>Min</th><th>95% percentile</th><th>Mean</th><th>Correct samples</th><th>Errors</th></tr>");
		for (String eventName : stats.getEvents()) {
			buf.append("<tr><td>")
				.append(eventName)
				.append("</td><td>")
				.append(stats.getLastPeriodStats(eventName).getMax())
				.append("</td><td>")
				.append(stats.getLastPeriodStats(eventName).getMin())
				.append("</td><td>")
				.append(stats.getLastPeriodStats(eventName).getHistogram().getValueForPercentile(95))
				.append("</td><td>")
				.append(stats.getLastPeriodStats(eventName).getAvg())
				.append("</td><td>")
				.append(stats.getLastPeriodStats(eventName).numCorrectSamples)
				.append("</td><td>")
				.append(stats.getLastPeriodStats(eventName).numErrors)
				.append("</td></tr>");
		}
		buf.append("</tbody></table>");
		return buf.toString();
	}
	


}
