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
package com.flaptor.hounder.indexer;

/**
 * A module that requires access to Hounder's internal structure.
 * Some modules provided with Hounder require access to some of the Hounder internal classes. 
 * This package protected class allows to implement such modules.
 * 
 * @author Flaptor Development Team
 */
abstract class AInternalModule extends AModule {
	protected final Indexer indexer;
	
    /**
     * Mock constructor, do not use it.
     * This constructor is only here to comply with AModule's interface.
     */
	private AInternalModule() {
        throw new UnsupportedOperationException();
    }

    /**
     * Constructor.
     * Creates an internal module with a reference to an already created
     * indexer. This reference is saved to be available for the module during
     * normal execution.
     */
	public AInternalModule(final Indexer indexer) {
		this.indexer = indexer;
	}
    
}
