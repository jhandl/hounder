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
package com.flaptor.hounder.crawler.pagedb.distributed;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;
import com.flaptor.util.remote.RmiUtil;


/**
 * This class allows remote nodes to synchronize their executions. 
 * Each node sends signals to all the others and waits until it
 * receives the signal from all the rest before returning.
 * @author Flaptor Development Team
 */
public class SyncPoint implements IRemoteSyncPoint {

    static Logger logger = Logger.getLogger(Execute.whoAmI());
    private ArrayList<IRemoteSyncPoint> remoteSyncPoints = null;
    private ArrayList<NodeAddress> remoteNodes = null;
    private NodeAddress localNode = null;
    private static HashSet<NodeAddress> received = new HashSet<NodeAddress>();

    /**
     * Initialize the class.
     * @param nodes list of nodes to synchronize with.
     * @param localNode local node, that has to be among the provided node list.
     */
    public SyncPoint (ArrayList<NodeAddress> nodes, NodeAddress localNode) {
        this.remoteNodes = nodes;
        this.localNode = localNode;
        exportService(localNode.getPort());
        remoteSyncPoints = getRemoteServices(nodes, localNode);
    }

    /**
     * Exports the RMI service so other nodes can send a signal to it.
     * @param port the port where the service will be listening.
     */
    private void exportService (int port) {
        try {
            RmiUtil.registerLocalService(port,"SyncPoint",this);
        } catch (Exception e) {
            logger.error(e,e);
            throw new RuntimeException("Registering the SyncPoint", e);
        }
    }

    /**
     * Obtains a list of all the remote SyncPoint RMI stubs.
     * @param nodes list of nodes to synchronize with.
     * @param localNode local node, that has to be among the provided node list.
     * @return the list of all the remote SyncPoint RMI stubs.
     */
    private ArrayList<IRemoteSyncPoint> getRemoteServices (ArrayList<NodeAddress> nodes, NodeAddress localNode) {
        ArrayList<IRemoteSyncPoint> services = new ArrayList<IRemoteSyncPoint>();
        IRemoteSyncPoint localService = null;
        for (NodeAddress node : nodes) {
            boolean connected = false;
            while (!connected) {
                IRemoteSyncPoint service = (IRemoteSyncPoint) RmiUtil.getRemoteService(node.getIP(), node.getPort(), "SyncPoint");
                if (null != service) {
                    services.add(service);
                    if (node.equals(localNode)) {
                        localService = service;
                    }
                    connected = true;
                } else {
                    Execute.sleep(1000);
                }
            }
        }
        if (null == localService) {
            throw new RuntimeException("Local address not found among the provided addresses");
        }
        return services;
    }

    /**
     * Synchronize with the remote nodes, with no timeout.
     * @return true.
     */
    public boolean sync () {
        return sync(0);
    }

    /**
     * Synchronize with the remote nodes with timeout.
     * @param timeout number of seconds before timeout.
     * @return true if synchronized, false if timed out.
     */
    public boolean sync (int timeout) {
        boolean couldSync = false;
        long remainingTime = timeout * 1000L;
        long startTime = System.currentTimeMillis();
        logger.debug("Node "+localNode+" sending sync signal to all nodes");
        for (IRemoteSyncPoint node : remoteSyncPoints) {
            boolean signalSent = false;
            while (!signalSent) {
                try {
                    node.signalFrom(localNode);
                    signalSent = true;
                } catch (RemoteException e) {
                    logger.warn("Sending sync signal to node "+node, e);
                    long now = System.currentTimeMillis();
                    remainingTime -= (now - startTime);
                    if (remainingTime <= 0) {
                        break;
                    }
                }
            }
        }
        int remainingSeconds = (int) (remainingTime / 1000L);
        if (remainingSeconds > 0 || timeout == 0) {
            couldSync = waitForNodes(remoteNodes, remainingSeconds);
        }
        return couldSync;
    }

    /**
     * Reseting a SynchPoint prepares it for a new use. 
     * Care must be taken to ensure that at the time of reset no signal is pending.
     */
    public void reset () {
        logger.debug("Reseting sync");
        received.clear();
    }


    /**
     * Send a signal to a remote node.
     * @param node remote node to send the signal to.
     */
    public void signalFrom (NodeAddress node) throws RemoteException {
        logger.debug("Sync signal received from node "+node);
        synchronized (received) {
            received.add(node);
            received.notifyAll();
        }
    }

    /**
     * Wait for the signal of a remote node with no timeout.
     * @param node remote node to wait for.
     * @return true.
     */
    public boolean waitForNode (NodeAddress node) {
        return waitForNode(node, 0);
    }

    /**
     * Wait for the signal of a remote node with timeout.
     * @param node remote node to wait for.
     * @param timeout number of seconds before timeout.
     * @return true if signal received, false if timed out.
     */
    public boolean waitForNode (NodeAddress node, int timeout) {
        ArrayList<NodeAddress> nodes = new ArrayList<NodeAddress>();
        nodes.add(node);
        return waitForNodes(nodes, timeout);
    }

    /**
     * Wait for the signal of a list of remote nodes with no timeout.
     * @param nodes list of remote nodes to wait for.
     * @return true.
     */
    public boolean waitForNodes (ArrayList<NodeAddress> nodes) {
        return waitForNodes(nodes, 0);
    }

    /**
     * Wait for the signal of a list of remote nodes with timeout.
     * @param nodes list of remote nodes to wait for.
     * @param timeout number of seconds before timeout.
     * @return true if all signals received, false if timed out.
     */
    public boolean waitForNodes (ArrayList<NodeAddress> nodes, int timeout) {
        logger.debug("Waiting for sync signal from nodes: "+nodes);
        long remainingTime = timeout * 1000L;
        long startTime = System.currentTimeMillis();
        synchronized (received) {
            while (!nodesReported(nodes)) {
                try {
                    received.wait(remainingTime);
                } catch (InterruptedException e) {
System.out.println("SYNCPOINT INTERRUPTED!!!");
                    long now = System.currentTimeMillis();
                    remainingTime -= (now - startTime);
                    if (remainingTime <= 0) {
                        break;
                    }
                }
            }
        }
        boolean ok = nodesReported(nodes);
        return ok;
    }

    /**
     * Check if the specified nodes sent their signal.
     * @param nodes list of remote nodes to check.
     * @return true if all the specified nodes sent their signal.
     */
    private boolean nodesReported (ArrayList<NodeAddress> nodes) {
        boolean reported = true;
        for (NodeAddress node : nodes) {    
            if (!received.contains(node)) {
                reported = false;
                break;
            }
        }
        return reported;
    }

    
    /**
     * For testing
     */
    public static class Syncer extends Thread {
        private ArrayList<NodeAddress> nodes;
        private NodeAddress node;
        public Syncer (ArrayList<NodeAddress> nodes, NodeAddress node) {
            this.nodes = nodes;
            this.node = node;
        }
        public void run () {
            SyncPoint sync = new SyncPoint(nodes, node);
            sync.sync();
            System.out.println("DONE "+node);
        }
    }

    /**
     * For testing
     */
    public static void main (String[] args) {
        NodeAddress node1 = new NodeAddress("127.0.0.1",1091);
        NodeAddress node2 = new NodeAddress("127.0.0.1",1092);
        ArrayList<NodeAddress> nodes = new ArrayList<NodeAddress>();
        nodes.add(node1);
        nodes.add(node2);
        Syncer s1 = new SyncPoint.Syncer(nodes, node1);
        Syncer s2 = new SyncPoint.Syncer(nodes, node2);
        s1.start();
        s2.start();
    }

}

