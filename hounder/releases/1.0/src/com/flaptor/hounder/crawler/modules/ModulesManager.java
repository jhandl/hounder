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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * @author Flaptor Development Team
 */
public class ModulesManager implements IProcessorModule {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static ModulesManager instance = null;
    private ArrayList<IProcessorModule> modules;


    /**
     * Return an instance of this singleton class.
     */
    public static ModulesManager getInstance () {
        if (null == instance) {
            instance = new ModulesManager();
        }
        return instance;
    }

    /**
	 * @todo check that thrown runtime exceptions are the right ones.
	 * @todo decide the config name
	 * @todo review the way exceptions are handled.
	 */
    private ModulesManager() {

        modules = new ArrayList<IProcessorModule>();

        Config config = Config.getConfig("crawler.properties");
        String moduleNames = config.getString("modules");
        if (null == moduleNames || "".equals(moduleNames)) {
            logger.warn("No crawler modules defined");
            return;
        }
        String[] singleModules = moduleNames.split("\\|");

        // try block is outside the for loop, because it makes no sense to 
        // make it inside if the loop will be broken by throw statement in 
        // catch block. 
        // TODO: clarify the comment above.
        String className = null;
        String moduleName = null;
        try {

            // Load each of the modules.
            for (int i = 0; i < singleModules.length; i++) {
       
                String[] params = singleModules[i].split(",");
                // Check that each module has 2 parameters. Fail otherwise.
                if (params.length != 2) {
                    String error =  "Wrong configuration file. Module config " +
                                    "format is \"classname,modulename\".";
                    logger.error(error);
                    throw new IllegalArgumentException(error);
                }

                // So line has 2 parameters.
                className = params[0];
                moduleName = params[1];
                logger.debug("Instantiating module " + className + " as " + moduleName);
                // Instantiate it.
                modules.add((IProcessorModule)Class.forName(className).getConstructor(new Class[]{String.class,Config.class}).newInstance(new Object[]{moduleName,config}));
            } // end for


        } catch (NoSuchMethodException e) {
            logger.error("Could not find constructor, AProcessModule(String,Config) for class " + className + ". Is the provided class an AProcessModule ?", e);
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            logger.error("Class not found " + className + " with module name " + moduleName,e);
            throw new IllegalArgumentException(e);
        } catch (Exception e) {
            logger.error("Exception while instantiating class " + className + " with module name " + moduleName,e);
            throw new IllegalArgumentException(e);
        }            
        // TODO check if catching Exception is better.              
    }

    /**
     * Returns an array of module instances of the given module class name.
     * If there are no instances of the given class an empty array is returned.
     * @param moduleClassName the name of the module class.
     * @return an array or module instances of the given module class name.
     */
    public IProcessorModule[] getModuleInstances (String moduleClassName) {
        ArrayList<IProcessorModule> mods = new ArrayList<IProcessorModule>();
        Iterator<IProcessorModule> it = modules.iterator();
        while (it.hasNext()) {
            IProcessorModule mod = it.next();
            if (mod.getClass().getName().equals(moduleClassName)) {
                mods.add(mod);
            }
        }
        return mods.toArray(new IProcessorModule[0]);
    }

    /**
     * Removes a module from the module list.
     * @param module the module instance to be removed.
     */
    public void unloadModule (IProcessorModule module) {
        if (modules.contains(module)) {
            logger.debug("Removing module "+module);
            modules.remove(module);
        }
    }


    /**
     * Sends the document to all the modules in sequence.
     * @param doc the FetchDocument to be processed.
     */
    public void process (FetchDocument doc) {
        Iterator<IProcessorModule> it = modules.iterator();
        while (it.hasNext()) {
            IProcessorModule mod = it.next();
            logger.debug("Going to send doc to module " + mod);
            try {
                mod.process(doc);
            } catch (RuntimeException e) {
                logger.error("Unexpected exception processing module " + mod, e);
                throw e;
            }
        }
    }
    
    /**
     * Send a command to all the modules in sequence.
     * @param an object that represents the command. 
     * @see IProcessorModule#applyCommand(Object)
     */
    public void applyCommand(Object command) {
        Iterator<IProcessorModule> it = modules.iterator();
        while (it.hasNext()) {
            IProcessorModule mod = it.next();
            logger.debug("Going to send command " + command.toString() + " to module " + mod);
            mod.applyCommand(command);
        }
    }

}

