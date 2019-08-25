/*  Copyright 2015-2017 Stanford University                                                                                                                                       
 *                                                                                                                                                                               
 *  Licensed under the Apache License, Version 2.0 (the "License");                                                                                                               
 *  you may not use this file except in compliance with the License.                                                                                                              
 *  You may obtain a copy of the License at                                                                                                                                       
                                                                                                                                                                                
 *      http://www.apache.org/licenses/LICENSE-2.0                                                                                                                                
                                                                                                                                                                                
 *  Unless required by applicable law or agreed to in writing, software                                                                                                           
 *  distributed under the License is distributed on an "AS IS" BASIS,                                                                                                             
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.                                                                                                      
 *  See the License for the specific language governing permissions and                                                                                                           
 *  limitations under the License. 
 */

package glade.util;

public class BitUtils {
	public static int[] getBitMinus(int[] bits, int[] remove) {
		if(bits.length != remove.length) {
			throw new RuntimeException();
		}
		int[] difference = new int[bits.length];
		for(int i=0; i<bits.length; i++) {
			int curBits = bits[i];
			int curRemove = remove[i];
			difference[i] = curBits & (~curRemove);
		}
		return difference;
	}
	
	public static int[] getBitOr(Iterable<int[]> allBits) {
		int[] or = new int[allBits.iterator().next().length];
		for(int[] bits : allBits) {
			if(bits.length != or.length) {
				throw new RuntimeException();
			}
			for(int i=0; i<or.length; i++) {
				or[i] |= bits[i];
			}
		}
		return or;
	}
	
	public static int[] getBitAnd(Iterable<int[]> allBits) {
		int[] and = new int[allBits.iterator().next().length];
		for(int i=0; i<and.length; i++) {
			and[i] = -1;
		}
		for(int[] bits : allBits) {
			if(bits.length != and.length) {
				throw new RuntimeException();
			}
			for(int i=0; i<and.length; i++) {
				and[i] &= bits[i];
			}
		}
		return and;
	}
}
