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

package glade.main;

import java.io.BufferedReader;
import java.io.FileReader;

import glade.util.Log;

public class PostProcessor {
	public static void processSynthetic() {
		double fn = 1.0;
		double fp = 1.0;
		double time = 0.0;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("log.txt"));
			String line;
			while((line = br.readLine()) != null) {
				if(line.startsWith("FN RATE: ")) {
					fn = Double.parseDouble(line.substring("FN RATE: ".length()));
				}
				if(line.startsWith("FP RATE: ")) {
					fp = Double.parseDouble(line.substring("FP RATE: ".length()));
				}
				if(line.startsWith("DONE IN: ")) {
					time = Double.parseDouble(line.substring("DONE IN: ".length()));
				}
				if(line.equals("TIMEOUT")) {
					time = 300.0;
				}
			}
			br.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		double f1;
		if(fn == 1.0 && fp == 1.0) {
			f1 = 0.0;
		} else {
			f1 = 2.0 * (1.0-fp) * (1.0-fn) / (2.0-fp-fn);
		}
		
		Log.output("F1-Score: " + f1);
		Log.output("Running time: " + time);
	}
	
	public static void processFuzz() {
		int iter = 0;
		double naive = 0.0;
		double glade = 0.0;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("log.txt"));
			String line;
			while((line = br.readLine()) != null) {
				if(line.startsWith("EMPTY COVERAGE: ")) {
					iter++;
				}
				if(iter == 1 && line.startsWith("CUR COVERAGE: ")) {
					naive = Double.parseDouble(line.substring("CUR COVERAGE: ".length()));
				}
				if(iter == 2 && line.startsWith("CUR COVERAGE: ")) {
					glade = Double.parseDouble(line.substring("CUR COVERAGE: ".length()));
				}
			}
			br.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		double cov = glade/naive;
		
		Log.output("Normalized incremental coverage: " + cov);
	}
}
