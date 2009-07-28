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
package com.flaptor.hounder.indexer.util;

import java.security.MessageDigest;
import java.util.Scanner;

/**
 * This class implements a utility to calculate a uniform hash based on md5.
 * @author Flaptor Development Team
 * 
 */
public final class Hash {

	private final MessageDigest md;
    private final int buckets;
	
	public Hash(final int buckets) {
        if (buckets <=0) {
            throw new IllegalArgumentException("Buckets must be > 0.");
        }
        this.buckets = buckets;
		
        
        try {
			md = MessageDigest.getInstance("MD5");
		}
		catch (Exception e) {
			//should not happen unless the jvm is missing the MD5 algorithm somehow.
			throw new RuntimeException(e.getMessage());
		}
    }

	/** 
	 * This hash method generates a number between 0 and n-1. It works as follows:
	 * - it computes the md5 of the key
	 * - it takes the first two bytes of the md5 and generates a value between 0 and 65536
	 * - it returns that value modulo "buckets"
	 */
	public synchronized int hash(String key) {
		md.reset();
		byte[] dig = md.digest(key.getBytes());
		return ((128 + dig[0])* 256 + (128 + dig[1])) % buckets;
	}
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: Hash <numBuckets>\n Reads lines from std in and writes the bucket to" +
                    " wich they map to std out");
            return;
        }
        Hash hash = new Hash(Integer.parseInt(args[0]));
        Scanner s = new Scanner(System.in);
        while (true) {
            System.out.println(hash.hash(s.nextLine()));
        }
    }

}

