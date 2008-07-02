package com.flaptor.hounder.clusterfest;

import com.flaptor.clusterfest.monitoring.node.MonitoreableImplementation;
import com.flaptor.util.Statistics;

/**
 * send all statistics by default
 * @author Martin Massera
 */
public class HounderMonitoreable extends MonitoreableImplementation{

    public void updateProperties() {
        super.updateProperties();
        setProperty("statistics", Statistics.getStatistics());
    }
}
