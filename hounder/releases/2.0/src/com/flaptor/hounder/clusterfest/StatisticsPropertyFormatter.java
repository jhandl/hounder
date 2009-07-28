package com.flaptor.hounder.clusterfest;

import com.flaptor.clusterfest.ClusterManager;
import com.flaptor.clusterfest.NodeDescriptor;
import com.flaptor.clusterfest.monitoring.DefaultPropertyFormatter;
import com.flaptor.util.Statistics;

public class StatisticsPropertyFormatter extends DefaultPropertyFormatter {

	public String format(NodeDescriptor node, String name, Object value) {
		if (value instanceof Statistics) {
			return formatStatistics(node, (Statistics) value);
		} else {
			return super.format(node, name, value);
		}
    }
	
	private String formatStatistics(NodeDescriptor node, Statistics stats) {
		StringBuffer buf = new StringBuffer();
		buf.append("<table><tbody><tr><th>Event name</th><th>Max</th><th>Min</th><th>95% percentile</th><th>Mean</th><th>Correct samples</th><th>Errors</th></tr>");
		for (String eventName : stats.getEvents()) {
			buf.append("<tr><td>")
                .append("<a href='statistics.do?&id="+ ClusterManager.getInstance().getNodeIndex(node)+ "&eventName="+eventName+"'>")
				.append(eventName)
                .append("</a>")
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
