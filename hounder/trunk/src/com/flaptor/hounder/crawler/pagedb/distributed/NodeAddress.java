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

import java.io.Serializable;

/**
 * This class represents the address of a pagedb node.
 * @author Flaptor Development Team
 */
public class NodeAddress implements Serializable {

    private static final long serialVersionUID = 1L;
    private String ip;
    private int port;

    /**
     * Creates a new NodeAddress.
     * @param ip the IP of this node address.
     * @param port the port of this node address.
     */
    public NodeAddress (String ip, Integer port) {
        if (null == port) port = 1099;
        this.ip = ip;
        this.port = port;
    }

    /**
     * Returns the IP of this node address.
     * @return the IP of this node address.
     */
    public String getIP () {
        return ip;
    }

    /**
     * Returns the port of this node address.
     * @return the port of this node address.
     */
    public int getPort () {
        return port;
    }

    /**
     * Compartes two node addresses.
     * @param other the node address to which this one will be compared.
     * @return true if both represent the same node address.
     */
    public boolean equals (Object other) {
        if (!(other instanceof NodeAddress)) return false;
        NodeAddress otherNode = (NodeAddress)other;
        return (getIP().equals(otherNode.getIP()) && getPort() == otherNode.getPort());
    }

    /**
     * Computes the hashcode of this object.
     * @return the hashcode of this object.
     */
    public int hashCode () {
        return ip.hashCode() ^ port;
    }

    /**
     * Return the node as a string.
     */
    public String toString () {
        return getIP()+":"+getPort();
    }


}

