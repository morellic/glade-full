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
import java.util.List;
import java.util.Random;

public class RandomUtils {
	public static class RandomExtra {
		public final Random random;
		public RandomExtra(Random random) { this.random = random; }
		public RandomExtra() { this(new Random()); }
		
		public double[] nextWeightVector(int length, double prior) {
			if(length <= 0) { throw new RuntimeException("Invalid length!"); }
			if(prior < 0.0) { throw new RuntimeException("Invalid prior!"); }
			double[] weights = new double[length];
			double normalizer = 0.0;
			for(int i=0; i<length; i++) {
				double randomWeight = this.random.nextDouble();
				weights[i] = prior + randomWeight;
				normalizer += weights[i];
			}
			if(normalizer == 0.0) {
				// recover from unlikely possibility of generating all 0.0's
				weights[0] = 1.0;
				return weights;
			}
			for(int i=0; i<length; i++) {
				weights[i] /= normalizer;
			}
			return weights;
		}
	
		public double[] nextWeightVector(int length) {
			return this.nextWeightVector(length, 0.0);
		}
		
		public double[] nextBooleanWeightVector(int length) {
			if(length <= 0) { throw new RuntimeException("Invalid length!"); }
			double[] selector = new double[length];
			int index = this.random.nextInt(length);
			selector[index] = 1.0;
			return selector;
		}
		
		// generates a random *non-control* ascii char
		public char nextAsciiChar() {
			int randSymbol = this.random.nextInt(94);
			randSymbol += 32;
			if(randSymbol >= 39) {
				randSymbol++;
			}
			return (char)randSymbol;
		}
		
		public String nextString(int length) {
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<length; i++) {
				sb.append(this.nextAsciiChar());
			}
			return sb.toString();
		}
		
		public List<String> nextStrings(int length, int numStrings) {
			List<String> strings = new ArrayList<String>();
			for(int i=0; i<numStrings; i++) {
				strings.add(this.nextString(length));
			}
			return strings;
		}
		
		// uniformly random string of length at most maxLength
		public String nextUniformLengthString(int maxLength) {
			return this.nextString(this.random.nextInt(maxLength));
		}
		
		public List<String> nextUniformLengthStrings(int maxLength, int numStrings) {
			List<String> strings = new ArrayList<String>();
			for(int i=0; i<numStrings; i++) {
				strings.add(this.nextUniformLengthString(maxLength));
			}
			return strings;
		}
		
		public String nextExpLengthString(double expProbability) {
			if(expProbability <= 0.0 || expProbability > 1.0) {
				throw new RuntimeException("Invalid probability!");
			}
			int length = 0;
			while(this.random.nextDouble() > expProbability) {
				length++;
			}
			return nextString(length);
		}
		
		public List<String> nextExpLengthStrings(double expProbability, int numSamples) {
			List<String> strings = new ArrayList<String>();
			for(int i=0; i<numSamples; i++) {
				strings.add(this.nextExpLengthString(expProbability));
			}
			return strings;
		}
		
		// performs a single edit to the string
		public String nextStringMutant(String string) {
			if(string.length() == 0) { return "" + this.nextAsciiChar(); }
			int randIndex = this.random.nextInt(string.length());
			boolean randOp = this.random.nextBoolean(); // false -> delete, true -> insert
			String head = string.substring(0, randIndex);
			String tail = string.substring(randIndex);
			if(randOp) {
				return head + this.nextAsciiChar() + tail;
			} else {
				return head + (tail.length() == 0 ? "" : tail.substring(1));
			}
		}
		
		public String nextStringMutant(String string, int numMutantions) {
			for(int i=0; i<numMutantions; i++) {
				string = this.nextStringMutant(string);
			}
			return string;
		}
	}
	
	public static class DiscreteDistribution<T> {
		private final List<T> ts = new ArrayList<T>();
		private final List<Double> weights = new ArrayList<Double>();
		private double normalizer = 0.0;
		
		public void add(T t, double weight) {
			if(weight <= 0.0) { throw new RuntimeException("Invalid weight!"); }
			this.ts.add(t);
			this.weights.add(this.normalizer);
			this.normalizer += weight;
		}
		
		public T sample(Random random) {
			return this.ts.get(binarySearch(this.weights, this.normalizer*random.nextDouble()));
		}
		
		// Precondition: Result guaranteed to be index between start and end (inclusive)
		// Precondition: Result is not the last element in the list
		private static int binarySearchHelper(List<Double> weights, double value, int start, int end) {
			int index = (start+end)/2;
			
			// STEP 1: Case w[i] > value
			if(weights.get(index) > value) {
				return binarySearchHelper(weights, value, start, index-1);
			}
			
			// STEP 2: Case w[i] <= value < w[i+1]
			if(weights.get(index) == value || weights.get(index+1) > value) {
				return index;
			}
			
			// STEP 3: Case w[i+1] < value
			return binarySearchHelper(weights, value, index+1, end);
		}
		
		private static int binarySearch(List<Double> weights, double value) {
			// STEP 1: Handle edge cases
			if(weights.size() == 0) {
				return -1;
			}
			if(value > weights.get(weights.size()-1)) {
				return weights.size()-1;
			}
			if(value < weights.get(0)) {
				return -1;
			}
			
			// STEP 2: Run helper
			return binarySearchHelper(weights, value, 0, weights.size()-2);
		}
	}
}
