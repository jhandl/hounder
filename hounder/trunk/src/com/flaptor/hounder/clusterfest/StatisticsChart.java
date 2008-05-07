package com.flaptor.hounder.clusterfest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.flaptor.clusterfest.ClusterManager;
import com.flaptor.clusterfest.NodeDescriptor;
import com.flaptor.clusterfest.chart.ChartModule;
import com.flaptor.clusterfest.monitoring.MonitorModule;
import com.flaptor.clusterfest.monitoring.MonitorNodeDescriptor;
import com.flaptor.hist4j.AdaptiveHistogram;
import com.flaptor.hist4j.HistogramNode;
import com.flaptor.util.DateUtil;
import com.flaptor.util.Pair;
import com.flaptor.util.Statistics;

public class StatisticsChart extends ChartModule{
    public StatisticsChart() {
        super("statistics");
    }
    
    public Pair<List<String>, Map<Date, List<Number>>> getChartData(HttpServletRequest request) {
        ClusterManager cluster= ClusterManager.getInstance();
        NodeDescriptor node = cluster.getNodes().get(Integer.parseInt(request.getParameter("id")));
        String eventName = request.getParameter("eventName");
        MonitorModule monitor = (MonitorModule)cluster.getModule("monitor");
        MonitorNodeDescriptor mnode = monitor.getModuleNode(node);
        Statistics statistics = (Statistics)mnode.getLastState().getProperties().get("statistics");
        AdaptiveHistogram hist = statistics.getLastPeriodStats(eventName).getHistogram();
        Calendar c = DateUtil.getCanonicalDayFromToday(0);
        
        Map<Date, List<Number>> ret = new HashMap<Date, List<Number>>(); 
        float last = 0;
        for (int i = 0; i<100; i += 10) {
            float value = hist.getValueForPercentile(i) - last; 
            last = hist.getValueForPercentile(i);
            List<Number> numbers = new ArrayList<Number>();
            numbers.add(last);
            numbers.add(last);
            ret.put(c.getTime(), numbers);
            c = DateUtil.getCanonicalDayFrom(c, 1);
        }
        return new Pair<List<String>, Map<Date, List<Number>>>(Arrays.asList(new String[]{eventName}),ret);
    }
}
