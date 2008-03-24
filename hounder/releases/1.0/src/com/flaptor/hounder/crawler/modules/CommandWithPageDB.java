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
package com.flaptor.hounder.crawler.modules;

import com.flaptor.hounder.crawler.pagedb.PageDB;

/**
 * This class implements a crawler module command that holds a PageDB as a payload. 
 * It can be used to send a command to a module with an associated PageDB.
 * @author Flaptor Development Team
 */
public class CommandWithPageDB {

    private String cmdName;
    private PageDB pagedb;
    
    /**
     * Class constructor.
     * @param cmdName the name of the command.
     * @param pagedb the associated PageDB.
     */
    public CommandWithPageDB (String cmdName, PageDB pagedb) {
        this.cmdName = cmdName;
        this.pagedb = pagedb;
    }

    /**
     * Get the command name.
     * @return the command name.
     */
    public String getName () {
        return cmdName;
    }
    
    /**
     * Get the pagedb associated with the command.
     * @return the pagedb associated with the command.
     */
    public PageDB getPageDB () {
        return pagedb;
    }

    /**
     * Produce a printable representation of this object.
     * Since this will be used by crawler modules that compare the
     * toString() of the command to a command name, it returns the
     * command name. This way this object can be used in place of
     * a simple command name string.
     * @return a string that represents this object.
     */
    public String toString () {
        return getName();
    }

}

