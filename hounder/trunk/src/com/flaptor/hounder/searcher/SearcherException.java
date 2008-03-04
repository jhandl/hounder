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
package com.flaptor.search4j.searcher;

import java.io.Serializable;

/**
 * For exceptions that we might expect from the Searcher
 * 
 * @author Martin Massera
 */
public class SearcherException extends Exception implements Serializable {

	public SearcherException() {
		super();
	}

	public SearcherException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public SearcherException(String arg0) {
		super(arg0);
	}

	public SearcherException(Throwable arg0) {
		super(arg0);
	}
}