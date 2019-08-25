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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import glade.util.OracleUtils.DiscriminativeOracle;
import glade.util.OracleUtils.GenerativeComplementOracle;
import glade.util.OracleUtils.GenerativeOracle;
import glade.util.OracleUtils.InteractiveOracle;
import glade.util.Utils.Pair;

public class LearnerUtils {
	public interface DiscriminativeLearner<T> {
		public T getModel();
		public void update(String query, boolean isMember);
	}
	
	public interface GenerativeLearner<T> extends DiscriminativeLearner<T> {
		public String getQuery();
	}
	
	public interface InteractiveLearner<T> extends GenerativeLearner<T> {
		public boolean isNextQueryModel();
		public void updateModel(T model, String counterExample);
	}
	
	// DiscriminativeLearner + DiscriminativeOracle + Samples
	public static <T> T learnDD(DiscriminativeOracle oracle, DiscriminativeLearner<T> learner, Iterable<String> samples) {
		long time = System.currentTimeMillis();
		for(String query : samples) {
			learner.update(query, oracle.query(query));
		}
		T t = learner.getModel();
		Log.info("LEARNDD TIME: " + (System.currentTimeMillis() - time));
		return t;
	}

	// DiscriminativeLearner + GenerativeOracle
	public static <T> T learnDG(DiscriminativeOracle oracle, GenerativeLearner<T> learner, Iterable<String> seed, int numQueries) {
		long time = System.currentTimeMillis();
		for(String query : seed) {
			learner.update(query, oracle.query(query));
		}
		for(int i=0; i<numQueries; i++) {
			String query = learner.getQuery();
			learner.update(query, oracle.query(query));
		}
		T t = learner.getModel();
		Log.info("LEARNDG TIME: " + (System.currentTimeMillis() - time));
		return t;
	}
	
	public static <T> T learnDG(DiscriminativeOracle oracle, GenerativeLearner<T> learner, int numQueries) {
		return learnDG(oracle, learner, new ArrayList<String>(), numQueries);
	}
	
	// GenerativeLearner + DiscriminativeOracle
	public static <T> T learnGD(GenerativeOracle oracle, DiscriminativeLearner<T> learner, Iterable<String> seed, int numSamples) {
		long time = System.currentTimeMillis();
		for(String sample : seed) {
			learner.update(sample, true);
		}
		for(int i=0; i<numSamples; i++) {
			learner.update(oracle.sample(), true);
		}
		T t = learner.getModel();
		Log.info("LEARNGD TIME: " + (System.currentTimeMillis() - time));
		return t;
	}
	
	public static <T> T learnGD(GenerativeOracle oracle, DiscriminativeLearner<T> learner, int numSamples) {
		return learnGD(oracle, learner, new ArrayList<String>(), numSamples);
	}
	
	// GenerativeComplementLearner + DiscriminativeOracle
	public static <T> T learnGCD(GenerativeComplementOracle oracle, DiscriminativeLearner<T> learner, Map<String,Boolean> seed, int numSamples) {
		long time = System.currentTimeMillis();
		for(Map.Entry<String,Boolean> entry : seed.entrySet()) {
			learner.update(entry.getKey(), entry.getValue());
		}
		for(int i=0; i<numSamples; i++) {
			Pair<String,Boolean> pair = oracle.sampleWithComplement();
			learner.update(pair.getX(), pair.getY());
		}
		T t = learner.getModel();
		Log.info("LEARNGCD TIME: " + (System.currentTimeMillis() - time));
		return t;
	}
	
	public static <T> T learnGCD(GenerativeComplementOracle oracle, DiscriminativeLearner<T> learner, int numSamples) {
		return learnGCD(oracle, learner, new HashMap<String,Boolean>(), numSamples);
	}
	
	// LStarOracle<T> + LStarLearner<T>
	public static <T> T learnII(InteractiveOracle<T> oracle, InteractiveLearner<T> learner) {
		long time = System.currentTimeMillis();
		int numEquivalence = 0;
		while(true) {
			if(learner.isNextQueryModel()) {
				T model = learner.getModel();
				String counterExample = oracle.equivalenceQuery(model);
				if(counterExample != null) {
					Log.info("CUR TIME: " + (System.currentTimeMillis() - time));
					Log.info("CUR EQUIVALENCE: " + (++numEquivalence));
					learner.updateModel(model, counterExample);
				} else {
					Log.info("DONE!");
					Log.info("LEARNII TOTAL TIME: " + (System.currentTimeMillis() - time));
					Log.info("LEARNII TOTAL EQUIVALENCE: " + (++numEquivalence));
					return model;
				}
			} else {
				String query = learner.getQuery();
				learner.update(query, oracle.query(query));
			}
		}
	}
	
	public static class InteractiveLearnerFromDiscriminative<T> implements InteractiveLearner<T> {
		private final DiscriminativeLearner<T> learner;
		private final List<String> queries = new ArrayList<String>();
		private int index = 0;
		
		public InteractiveLearnerFromDiscriminative(DiscriminativeLearner<T> learner, Iterable<String> initialQueries) {
			this.learner = learner;
			for(String query : initialQueries) {
				this.queries.add(query);
			}
		}

		@Override
		public String getQuery() {
			return this.queries.get(this.index++);
		}

		@Override
		public T getModel() {
			return this.learner.getModel();
		}

		@Override
		public void update(String query, boolean isMember) {
			this.learner.update(query, isMember);
		}

		@Override
		public boolean isNextQueryModel() {
			return this.index >= this.queries.size();
		}

		@Override
		public void updateModel(T model, String counterExample) {
			this.queries.add(counterExample);
		}
	}
}
