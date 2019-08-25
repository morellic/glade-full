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

import java.util.Random;

import glade.util.RandomUtils.RandomExtra;
import glade.util.Utils.Filter;
import glade.util.Utils.Pair;

public class OracleUtils {
	public interface DiscriminativeOracle {
		public abstract boolean query(String query);
	}
	
	public interface GenerativeOracle {
		public String sample();
	}
	
	public interface GenerativeComplementOracle {
		public Pair<String,Boolean> sampleWithComplement();
	}
	
	public interface HybridOracle extends GenerativeOracle, DiscriminativeOracle {}
	public interface HybridComplementOracle extends GenerativeComplementOracle, DiscriminativeOracle {}
	
	public interface InteractiveOracle<T> extends DiscriminativeOracle {
		public String equivalenceQuery(T model);
	}
	
	public static class DiscriminativeOracleFilter implements Filter<String> {
		private final DiscriminativeOracle oracle;
		public DiscriminativeOracleFilter(DiscriminativeOracle oracle) {
			this.oracle = oracle;
		}
		@Override
		public boolean filter(String query) {
			return this.oracle.query(query);
		}
	}
	
	public static interface Wrapper {
		public abstract String wrap(String input);
	}
	
	public static class IdentityWrapper implements Wrapper {
		@Override
		public String wrap(String input) {
			return input;
		}
	}
	
	public static class WrappedDiscriminativeOracle implements DiscriminativeOracle {
		private final DiscriminativeOracle oracle;
		private final Wrapper wrapper;
		
		public WrappedDiscriminativeOracle(DiscriminativeOracle oracle, Wrapper wrapper) {
			this.oracle = oracle;
			this.wrapper = wrapper;
		}
		
		@Override
		public boolean query(String query) {
			return this.oracle.query(this.wrapper.wrap(query));
		}
	}
	
	public static class InteractiveOracleFromDiscriminative<T extends DiscriminativeOracle> implements InteractiveOracle<T> {
		private final DiscriminativeOracle oracle;
		private final Iterable<String> queries;
		private final int numSamples;
		private final int maxLength;
		private final Random random;
		
		public InteractiveOracleFromDiscriminative(DiscriminativeOracle oracle, Iterable<String> queries, int numSamples, int maxLength, Random random) {
			this.oracle = oracle;
			this.queries = queries;
			this.numSamples = numSamples;
			this.maxLength = maxLength;
			this.random = random;
		}
		
		@Override
		public boolean query(String query) {
			return this.oracle.query(query);
		}

		@Override
		public String equivalenceQuery(T model) {
			for(String query : this.queries) {
				Log.info("CHECKING: " + query);
				if(model.query(query) != this.query(query)) {
					Log.info("FAILED: " + query);
					return query;
				}
			}
			for(int i=0; i<this.numSamples; i++) {
				String sample = new RandomExtra(this.random).nextUniformLengthString(this.maxLength);
				if(model.query(sample) != this.query(sample)) {
					return sample;
				}
			}
			return null;
		}		
	}
	
	public static class InteractiveOracleForGenerative<T extends HybridOracle> implements InteractiveOracle<T> {
		private final DiscriminativeOracle oracle;
		private final int numSamples;
		private final int numMutants;
		private final int numMutations;
		private final Random random;
		
		public InteractiveOracleForGenerative(DiscriminativeOracle oracle, int numSamples, int numMutants, int numMutations, Random random) {
			this.oracle = oracle;
			this.numSamples = numSamples;
			this.numMutants = numMutants;
			this.numMutations = numMutations;
			this.random = random;
		}
		
		@Override
		public boolean query(String query) {
			return this.oracle.query(query);
		}

		@Override
		public String equivalenceQuery(T model) {
			for(int i=0; i<this.numSamples; i++) {
				String sample = model.sample();
				if(!this.oracle.query(sample)) {
					return sample;
				}
				for(int j=0; j<this.numMutants; j++) {
					String mutatedSample = new RandomExtra(this.random).nextStringMutant(sample, this.numMutations);
					if(this.oracle.query(mutatedSample) != model.query(mutatedSample)) {
						return mutatedSample;
					}
				}
			}
			return null;
		}
	}
	
	public static class InteractiveOracleForGenerativeComplement<T extends HybridComplementOracle> implements InteractiveOracle<T> {
		private final DiscriminativeOracle oracle;
		private final int numSamples;
		
		public InteractiveOracleForGenerativeComplement(DiscriminativeOracle oracle, int numSamples) {
			this.oracle = oracle;
			this.numSamples = numSamples;
		}
		
		@Override
		public boolean query(String query) {
			return this.oracle.query(query);
		}

		@Override
		public String equivalenceQuery(T model) {
			for(int i=0; i<this.numSamples; i++) {
				Pair<String,Boolean> pair = model.sampleWithComplement();
				if(this.oracle.query(pair.getX()) != pair.getY()) {
					return pair.getX();
				}
			}
			return null;
		}
	}
}
