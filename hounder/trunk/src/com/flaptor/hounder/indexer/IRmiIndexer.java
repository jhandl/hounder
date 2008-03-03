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
package com.flaptor.search4j.indexer;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.dom4j.Document;
/**
 * @author Flaptor Development Team
 */
public interface IRmiIndexer extends Remote {
    
    public int index(final Document doc) throws RemoteException;
    
    public int index(final String text)throws RemoteException;
    
}

