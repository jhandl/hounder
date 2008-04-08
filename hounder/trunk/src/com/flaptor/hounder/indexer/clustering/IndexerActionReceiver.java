package com.flaptor.hounder.indexer.clustering;

import org.apache.log4j.Logger;

import com.flaptor.clusterfest.action.ActionReceiverImplementation;
import com.flaptor.hounder.indexer.Indexer;
import com.flaptor.hounder.indexer.IndexerCommands;
import com.flaptor.hounder.test.XmlIndexerClient;
import com.flaptor.util.PortUtil;

/**
 * Receives actions from clusterfest
 * @author Martin Massera
 */
public class IndexerActionReceiver extends ActionReceiverImplementation{
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private Indexer indexer;
    
    public IndexerActionReceiver(Indexer indexer) {
        super();
        this.indexer = indexer;
    }
    public boolean action(String action, Object[] params) throws Exception {
        logger.info("received action " + action + " params " + params);
        if ("close".equals(action)) {
            IndexerCommands.close(indexer);
        } else if ("checkpoint".equals(action)) {
            IndexerCommands.checkpoint(indexer);
        } else if ("optimize".equals(action)) {
            IndexerCommands.optimize(indexer);
        }
        return true;
    }
}
