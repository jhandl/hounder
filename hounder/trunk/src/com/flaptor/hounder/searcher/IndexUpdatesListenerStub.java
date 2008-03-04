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
package com.flaptor.hounder.searcher;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.flaptor.hounder.IRemoteIndexUpdater;
import com.flaptor.hounder.IndexDescriptor;
import com.flaptor.util.PortUtil;
import com.flaptor.util.remote.ARmiClientStub;
import com.flaptor.util.remote.ExponentialFallbackPolicy;

/**
 * A client-side IRemoteSearcher that connects with the server
 * via rmi.
 * @author Flaptor Development Team
 */
public class IndexUpdatesListenerStub extends ARmiClientStub implements IRemoteIndexUpdater {
    private static Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private IRemoteIndexUpdater remoteUpdater;



    protected void setRemote(Remote remote) {
        this.remoteUpdater = (IRemoteIndexUpdater)remote;
    }

    public boolean setNewIndex(IndexDescriptor location) throws RemoteException{
        super.checkConnection();

        try {
            boolean retValue = remoteUpdater.setNewIndex(location);
            super.connectionSuccess();
            return retValue;
        } catch (RemoteException e) {
            logger.error(e,e);
            super.connectionFailure();
            throw e;
        }
    }


    /**
     * @todo the searcher should be able to specify the connection policy.
     */
    public IndexUpdatesListenerStub(final int basePort, final String host) {
        super(PortUtil.getPort(basePort,"post.new.index"), host, new ExponentialFallbackPolicy(),true);
    }


    public String toString() {
        return host + ":" + String.valueOf(port);
    }


    public static void main(String[] args) throws Exception {
        IRemoteIndexUpdater updater = new IndexUpdatesListenerStub(Integer.parseInt(args[1]), args[0]);
        IndexDescriptor idxDesc = new IndexDescriptor("0of1@defaultCluster");
        if (args[2].contains(":")) {
            idxDesc.setRsyncAccessString(args[2].split(":")[0]);
            idxDesc.setLocalPath(args[2].split(":")[1]);
        } else {
            idxDesc.setRsyncAccessString("");
            idxDesc.setLocalPath(args[2]);
        }
        System.out.println("Updating " + idxDesc.getRemotePath() +" : " + updater.setNewIndex(idxDesc));
    }

}
