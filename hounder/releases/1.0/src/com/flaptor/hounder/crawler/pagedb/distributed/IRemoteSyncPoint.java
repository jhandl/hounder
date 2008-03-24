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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Flaptor Development Team
 */
public interface IRemoteSyncPoint extends Remote {

    // pagedbs call signal on each remote pagecatcher passing their own ids.
    // the page catchers collect the ids recieved and can report on the signals received.
    // each pagedb synch method sends its signal and waits until the local catcher got all the signals before returning.
    public void signalFrom (NodeAddress node) throws RemoteException;


}

