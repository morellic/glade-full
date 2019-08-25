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

import java.io.File;
import java.util.Iterator;
import java.util.Random;

import glade.constants.SyntheticGrammarFiles;
import glade.constants.Settings.FuzzSettings;
import glade.grammar.fuzz.MultiGrammarFuzzer.MultiGrammarSampler;
import glade.util.IteratorUtils.DefaultCallback;
import glade.util.IteratorUtils.FilteredIterable;
import glade.util.IteratorUtils.SampleIterable;
import glade.util.IteratorUtils.Sampler;
import glade.util.Utils.Filter;

public class GrepFuzzer {
	public static class GrepMutationSampler implements Sampler {
		private final Iterator<String> iterator;
		
		public GrepMutationSampler(FuzzSettings fuzz, Random random) {
			this.iterator = new FilteredIterable<String>(new SampleIterable(new MultiGrammarSampler(SyntheticGrammarFiles.loadGrammar(new File("data/handwritten/grep.gram")).getY(), fuzz.sample.getBoxSize(), random)), new Filter<String>() { public boolean filter(String s) { return s.length() < fuzz.maxLength; }}, new DefaultCallback()).iterator();
		}
		
		@Override
		public String sample() {
			return this.iterator.next();
		}
	}
}
