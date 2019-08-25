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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import glade.program.ProgramOracleUtils;
import glade.program.ProgramDataUtils.ProgramData;
import glade.program.ProgramOracleUtils.CoverInfo;
import glade.program.ProgramOracleUtils.CoverageOracle;
import glade.util.Log;
import glade.util.Utils.Pair;

public class RunProgram {
	public static List<Double> runCoverage(ProgramData data, Iterable<String> queries, Iterable<String> emptyQueries, int recordIters) {
		CoverageOracle oracle = data.getCoverageOracle();
		
		Map<Pair<String,Integer>,CoverInfo> emptyCoverage = ProgramOracleUtils.getFullCoverage(oracle, emptyQueries);
		Log.info("EMPTY COVERAGE: " + ProgramOracleUtils.getCoveragePercentage(emptyCoverage));
		
		int iter = 1;
		List<Double> results = new ArrayList<Double>();
		oracle.startCoverage();
		Iterator<String> queriesIter = queries.iterator();
		while(queriesIter.hasNext()) {
			String query = queriesIter.next();
			if(iter%recordIters == 0 || !queriesIter.hasNext()) {
				Map<Pair<String,Integer>,CoverInfo> curFullCoverage = oracle.curFullCoverage();
				double curCoverage = ProgramOracleUtils.getCoveragePercentage(curFullCoverage, ProgramOracleUtils.getRemoveLineFilter(emptyCoverage));
				double curUnfilteredCoverage = ProgramOracleUtils.getCoveragePercentage(curFullCoverage);
				results.add(curCoverage);
				Log.info("ITERATIONS: " + iter);
				Log.info("CUR COVERAGE: " + curCoverage);
				Log.info("CUR UNFILTERED COVERAGE: " + curUnfilteredCoverage);
			}
			oracle.runCoverage(query);
			iter++;
		}
		oracle.endCoverage();
		
		return results;
	}
	
	public static void runTiming(ProgramData data, Iterable<String> queries, Iterable<String> warmup, int numIters, int numQueue, int recordIters) {
		// STEP 0: Setup
		PriorityQueue<Pair<String,Long>> queue = new PriorityQueue<Pair<String,Long>>(
				new Comparator<Pair<String,Long>>() {
					public int compare(Pair<String,Long> p1, Pair<String,Long> p2) {
						return Long.compare(p1.getY(), p2.getY());
					}
				});
		
		// STEP 1: Warmup
		for(String query : warmup) {
			data.getQueryOracle().query(query);
		}
		
		// STEP 2: Run
		int iter = 1;
		Iterator<String> queriesIter = queries.iterator();
		Pair<String,Long> max = null;
		while(queriesIter.hasNext()) {
			// STEP 2a: Get next query
			String query = queriesIter.next();
			
			// STEP 2b: Get timing of current query
			long time = System.currentTimeMillis();
			for(int i=0; i<numIters; i++) {
				data.getQueryOracle().query(query);
			}
			long finalTime = System.currentTimeMillis() - time;
			queue.add(new Pair<String,Long>(query, finalTime));
			while(queue.size() > numQueue) {
				queue.remove();
			}
			
			// STEP 2c: Update max
			if(max == null || max.getY() < finalTime) {
				max = new Pair<String,Long>(query, finalTime);
			}
			
			// STEP 2d: Recording
			if(iter%recordIters == 0 || !queriesIter.hasNext()) {
				Log.info("ITERATIONS: " + iter);
				Log.info("MAX TIME: " + max.getY());
				Log.info("MAX QUERY:");				
				Log.info(max.getX());
			}
			
			// STEP 2e: Increment
			iter++;
		}
		
		// STEP 3: Top times
		max = null;
		for(Pair<String,Long> pair : queue) {
			long finalTime = 0L;
			for(int i=0; i<numIters; i++) {
				long time = System.currentTimeMillis();
				for(int j=0; j<numIters; j++) {
					data.getQueryOracle().query(pair.getX());
				}
				finalTime += System.currentTimeMillis() - time;
			}
			Log.info("TOP QUERY:");
			Log.info(pair.getX());
			Log.info("QUERY ESTIMATED TIME: " + pair.getY());
			Log.info("QUERY NEW ESTIMATED TIME: " + finalTime);
			if(max == null || finalTime > max.getY()) {
				max = new Pair<String,Long>(pair.getX(), finalTime);
			}
		}
		
		// STEP 4: Final time
		long finalTime = 0L;
		for(int i=0; i<numIters; i++) {
			long time = System.currentTimeMillis();
			for(int j=0; j<numIters; j++) {
				data.getQueryOracle().query(max.getX());
			}
			finalTime += (System.currentTimeMillis() - time);
		}
		Log.info("MAX QUERY:");
		Log.info(max.getX());
		Log.info("MAX ESTIMATED TIME: " + max.getY());
		Log.info("MAX NEW ESTIMATED TIME: " + finalTime);
	}
}
