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

package glade.program;

import java.util.Map;

import glade.util.OracleUtils.Wrapper;
import glade.util.Utils.AndFilter;
import glade.util.Utils.ConstantFilter;
import glade.util.Utils.Filter;
import glade.util.Utils.NotFilter;
import glade.util.Utils.Pair;

public class ProgramOracleUtils {
	public interface Oracle {
		public String execute(String query);
	}
	
	public static interface CoverInfo {
		public abstract boolean isCovered();
		public abstract boolean isCoverable();
		public abstract long getNumTimesCovered();
	}
	
	public static interface CoverageOracle {
		public abstract void startCoverage();
		public abstract void runCoverage(String query);
		public abstract void endCoverage();
		public abstract Map<Pair<String,Integer>,CoverInfo> curFullCoverage();
	}
	
	public static class WrappedOracle implements Oracle {
		private final Oracle oracle;
		private final Wrapper wrapper;
		
		public WrappedOracle(Oracle oracle, Wrapper wrapper) {
			this.oracle = oracle;
			this.wrapper = wrapper;
		}

		@Override
		public String execute(String query) {
			return this.oracle.execute(this.wrapper.wrap(query));
		}
	}
	
	public static class WrappedCoverageOracle implements CoverageOracle {
		private final CoverageOracle oracle;
		private final Wrapper wrapper;
		
		public WrappedCoverageOracle(CoverageOracle oracle, Wrapper wrapper) {
			this.oracle = oracle;
			this.wrapper = wrapper;
		}

		@Override
		public void startCoverage() {
			this.oracle.startCoverage();
		}

		@Override
		public void runCoverage(String query) {
			this.oracle.runCoverage(this.wrapper.wrap(query));
		}

		@Override
		public void endCoverage() {
			this.oracle.endCoverage();
		}
		
		@Override
		public Map<Pair<String,Integer>,CoverInfo> curFullCoverage() {
			return this.oracle.curFullCoverage();
		}
	}
	
	public static Map<Pair<String,Integer>,CoverInfo> getFullCoverage(CoverageOracle oracle, Iterable<String> queries) {
		oracle.startCoverage();
		for(String query : queries) {
			oracle.runCoverage(query);
		}
		Map<Pair<String,Integer>,CoverInfo> result = oracle.curFullCoverage();
		oracle.endCoverage();
		return result;
	}
	
	public static double getCoverage(CoverageOracle oracle, Iterable<String> queries, Iterable<String> emptyQueries) {
		Filter<Pair<String,Integer>> emptyFilter = getRemoveLineFilter(getFullCoverage(oracle, emptyQueries));
		oracle.startCoverage();
		for(String query : queries) {
			oracle.runCoverage(query);
		}
		double result = getCoveragePercentage(oracle.curFullCoverage(), emptyFilter);
		oracle.endCoverage();
		return result;
	}
	
	public static double getCoveragePercentage(Map<Pair<String,Integer>,CoverInfo> coverage) {
		return getCoveragePercentage(coverage, new ConstantFilter<Pair<String,Integer>>(true));
	}
	
	public static double getCoveragePercentage(Map<Pair<String,Integer>,CoverInfo> coverage, Filter<Pair<String,Integer>> filter) {
		int covered = 0;
		int coverable = 0;
		for(Map.Entry<Pair<String,Integer>,CoverInfo> entry : coverage.entrySet()) {
			if(!filter.filter(entry.getKey())) {
				continue;
			}
			if(entry.getValue().isCovered()) {
				covered++;
			}
			if(entry.getValue().isCoverable()) {
				coverable++;
			}
		}
		return (double)covered/coverable;
	}
	
	public static class CoverageFilter implements Filter<Pair<String,Integer>> {
		private final Map<Pair<String,Integer>,CoverInfo> coverage;
		public CoverageFilter(Map<Pair<String,Integer>,CoverInfo> coverage) {
			this.coverage = coverage;
		}
		public boolean filter(Pair<String,Integer> pair) {
			return this.coverage.get(pair).getNumTimesCovered() > 0;
		}
	}
	
	public static Filter<Pair<String,Integer>> getRemoveLineFilter(Map<Pair<String,Integer>,CoverInfo> emptyCoverage) {
		return new NotFilter<Pair<String,Integer>>(new CoverageFilter(emptyCoverage));
	}
	
	public static Filter<Pair<String,Integer>> getRestrictLineFilter(Map<Pair<String,Integer>,CoverInfo> fullCoverage) {
		return new CoverageFilter(fullCoverage);
	}
	
	public static Filter<Pair<String,Integer>> getJointLineFilter(Map<Pair<String,Integer>,CoverInfo> emptyCoverage, Map<Pair<String,Integer>,CoverInfo> fullCoverage) {
		return new AndFilter<Pair<String,Integer>>(getRemoveLineFilter(emptyCoverage), getRestrictLineFilter(fullCoverage));
	}
	
	public static double getRemoveCoverage(CoverageOracle oracle, Iterable<String> queries, Iterable<String> emptyQueries) {
		return getCoveragePercentage(getFullCoverage(oracle, queries), getRemoveLineFilter(getFullCoverage(oracle, emptyQueries)));
	}
	
	public static double getRestrictCoverage(CoverageOracle oracle, Iterable<String> queries, Iterable<String> fullQueries) {
		return getCoveragePercentage(getFullCoverage(oracle, queries), getRestrictLineFilter(getFullCoverage(oracle, fullQueries)));
	}

	public static double getJointCoverage(CoverageOracle oracle, Iterable<String> queries, Iterable<String> emptyQueries, Iterable<String> fullQueries) {
		return getCoveragePercentage(getFullCoverage(oracle, queries), getJointLineFilter(getFullCoverage(oracle, emptyQueries), getFullCoverage(oracle, fullQueries)));
	}
}
