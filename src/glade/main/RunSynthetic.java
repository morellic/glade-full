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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import dk.brics.automaton.Automaton;
import glade.automaton.AutomatonUtils.AutomatonOracle;
import glade.automaton.AutomatonUtils.RandomAutomatonOracle;
import glade.automaton.AutomatonUtils.WrappedLearner;
import glade.grammar.GrammarToNormalGrammar;
import glade.grammar.GrammarUtils.Node;
import glade.grammar.GrammarUtils.NodeMerges;
import glade.grammar.MultiGrammarUtils.MultiGrammar;
import glade.grammar.MultiGrammarUtils.NormalGrammar;
import glade.grammar.MultiGrammarUtils.NormalGrammarOracle;
import glade.grammar.fuzz.GrammarFuzzer.GrammarSampler;
import glade.grammar.fuzz.GrammarFuzzer.SampleParameters;
import glade.grammar.fuzz.MultiGrammarFuzzer.MultiGrammarSampler;
import glade.grammar.synthesize.GrammarSynthesis;
import glade.program.LearnerDataUtils;
import glade.program.LearnerDataUtils.LearnerDataParameters;
import glade.util.LearnerUtils;
import glade.util.Log;
import glade.util.IteratorUtils.FilteredIterable;
import glade.util.IteratorUtils.RandomSampleIterator;
import glade.util.IteratorUtils.SampleIterable;
import glade.util.LearnerUtils.DiscriminativeLearner;
import glade.util.LearnerUtils.InteractiveLearner;
import glade.util.OracleUtils.DiscriminativeOracle;
import glade.util.OracleUtils.HybridOracle;
import glade.util.OracleUtils.InteractiveOracle;
import glade.util.OracleUtils.InteractiveOracleFromDiscriminative;
import glade.util.Utils.Callback;
import glade.util.Utils.Filter;
import glade.util.Utils.Pair;

public class RunSynthetic {
	public static String sample(Iterator<String> iterator) {
		try {
			return iterator.next();
		} catch(Exception e) {
			return sample(iterator);
		}
	}
	
	public static interface OracleLearner<T extends HybridOracle> {
		public T learn(List<String> examples, DiscriminativeOracle oracle);
	}
	
	public static class NodeGrammarOracle implements HybridOracle {
		private final Pair<Node,NodeMerges> pair;
		private final DiscriminativeOracle oracle;
		private final Iterator<String> iterator;
		public NodeGrammarOracle(Pair<Node,NodeMerges> pair, SampleParameters parameters, Random random) {
			this.pair = pair;
			this.oracle = new NormalGrammarOracle(GrammarToNormalGrammar.transform(pair));
			this.iterator = new RandomSampleIterator(new GrammarSampler(pair, parameters, random));
		}
		public boolean query(String query) {
			return this.oracle.query(query);
		}
		public Pair<Node,NodeMerges> getNodeGrammar() {
			return this.pair;
		}
		public String sample() {
			return RunSynthetic.sample(this.iterator);
		}
	}
	
	public static class GrammarSynthesisOracleLearner implements OracleLearner<NodeGrammarOracle> {
		private final SampleParameters parameters;
		private final Random random;
		public GrammarSynthesisOracleLearner(SampleParameters parameters, Random random) {
			this.parameters = parameters;
			this.random = random;
		}
		public NodeGrammarOracle learn(List<String> examples, DiscriminativeOracle oracle) {
			return new NodeGrammarOracle(GrammarSynthesis.getGrammarMultiple(examples, oracle, true, true), this.parameters, this.random);
		}
	}
	
	public static class RegularGrammarSynthesisOracleLearner implements OracleLearner<NodeGrammarOracle> {
		private final SampleParameters parameters;
		private final Random random;
		public RegularGrammarSynthesisOracleLearner(SampleParameters parameters, Random random) {
			this.parameters = parameters;
			this.random = random;
		}
		public NodeGrammarOracle learn(List<String> examples, DiscriminativeOracle oracle) {
			return new NodeGrammarOracle(GrammarSynthesis.getRegularGrammarMultiple(examples, oracle, true, true), this.parameters, this.random);
		}
	}

	public static class NoConstantGrammarSynthesisOracleLearner implements OracleLearner<NodeGrammarOracle> {
		private final SampleParameters parameters;
		private final Random random;
		public NoConstantGrammarSynthesisOracleLearner(SampleParameters parameters, Random random) {
			this.parameters = parameters;
			this.random = random;
		}
		public NodeGrammarOracle learn(List<String> examples, DiscriminativeOracle oracle) {
			return new NodeGrammarOracle(GrammarSynthesis.getGrammarMultiple(examples, oracle, false, true), this.parameters, this.random);
		}
	}
	
	public static class DiscriminativeAutomatonOracleLearner implements OracleLearner<RandomAutomatonOracle> {
		private final DiscriminativeLearner<Automaton> learner;
		private final List<String> examples = new ArrayList<String>();
		private final Random random;
		
		public DiscriminativeAutomatonOracleLearner(DiscriminativeLearner<Automaton> learner, Iterable<String> examples, Random random) {
			this.learner = learner;
			for(String example : examples) {
				this.examples.add(example);
			}
			this.random = random;
		}
		
		public DiscriminativeAutomatonOracleLearner(DiscriminativeLearner<Automaton> learner, Random random) {
			this.learner = learner;
			this.random = random;
		}

		@Override
		public RandomAutomatonOracle learn(List<String> examples, DiscriminativeOracle oracle) {
			List<String> newExamples = new ArrayList<String>();
			newExamples.addAll(this.examples);
			newExamples.addAll(examples);
			return new RandomAutomatonOracle(LearnerUtils.learnDD(oracle, this.learner, newExamples), this.random);
		}
	}
	
	public static class InteractiveAutomatonOracleLearner implements OracleLearner<RandomAutomatonOracle> {
		private final InteractiveLearner<Automaton> learner;
		private final int numSamples;
		private final int maxLength;
		private final Random random;
		
		public InteractiveAutomatonOracleLearner(InteractiveLearner<Automaton> learner, int numSamples, int maxLength, Random random) {
			this.learner = learner;
			this.numSamples = numSamples;
			this.maxLength = maxLength;
			this.random = random;
		}
		
		@Override
		public RandomAutomatonOracle learn(List<String> examples, DiscriminativeOracle oracle) {
			InteractiveOracle<AutomatonOracle> interactiveOracle =  new InteractiveOracleFromDiscriminative<AutomatonOracle>(oracle, examples, this.numSamples, this.maxLength, this.random);
			return new RandomAutomatonOracle(LearnerUtils.learnII(interactiveOracle, new WrappedLearner(this.learner)).getAutomaton(), this.random);
		}
	}
	
	public static interface GrammarSaver<T> {
		public void save(T t, String name);
		public T load(String name);
	}
	
	public static class NodeGrammarSaver implements GrammarSaver<NodeGrammarOracle> {
		private final LearnerDataParameters grammar;
		private final SampleParameters parameters;
		private final Random random;
		public NodeGrammarSaver(LearnerDataParameters grammar, SampleParameters parameters, Random random) {
			this.grammar = grammar;
			this.parameters = parameters;
			this.random = random;
		}
		public void save(NodeGrammarOracle node, String name) {
			LearnerDataUtils.saveGrammar(this.grammar, name, 0, node.getNodeGrammar());
		}
		public NodeGrammarOracle load(String name) {
			return new NodeGrammarOracle(LearnerDataUtils.loadGrammar(this.grammar, name, 0), this.parameters, this.random);
		}
	}
	
	public static <T extends HybridOracle> T getTrainSynthetic(OracleLearner<T> learner, LearnerDataParameters dataParameters, MultiGrammar grammar, int boxSize, int numTrainSamples, int numTestSamples, SampleParameters sampleParameters, final int maxLength, Callback filterCallback, Random random) {
		Log.info("TRAINING SYNTHETIC");
		long time = System.currentTimeMillis();
		
		// STEP 1: Set up oracle grammar sampler
		Iterator<String> oracleIterator = new FilteredIterable<String>(new SampleIterable(new MultiGrammarSampler(grammar, boxSize, random)), new Filter<String>() { public boolean filter(String s) { return s.length() < maxLength; }}, filterCallback).iterator();
		
		// STEP 2: Set up oracle
		DiscriminativeOracle oracle = new NormalGrammarOracle(new NormalGrammar(grammar));
		
		// STEP 3: List of examples to learn from
		List<String> examples = new ArrayList<String>();
		
		// STEP 4: Initial learning of pair
		T t = learner.learn(examples, oracle);
		
		// STEP 5: Iteratively learn and get counter examples
		int index = 0;
		for(int i=0; i<numTrainSamples; i++) {
			// STEP 5a: Get sample
			String sample = sample(oracleIterator);
			if(!t.query(sample)) {
				// STEP 5b: Add sample as counter example if necessary
				examples.add(sample);
				
				// STEP 5c: Re-learn pair if necessary
				t = learner.learn(examples, oracle);
				
				// STEP 5d: Update index
				index = i+1;
			}
			
			// STEP 5e: Printing
			Log.info("NUM TRAIN: " + i);
			Log.info("INDEX: " + index);
			Log.info("EXAMPLES: " + examples.size());			
		}
		
		Log.info("DONE IN: " + (System.currentTimeMillis() - time));
		
		return t;
	}

	public static Pair<Double,Double> getTestSynthetic(HybridOracle learner, LearnerDataParameters dataParameters, MultiGrammar grammar, int boxSize, int numTrainSamples, int numTestSamples, SampleParameters sampleParameters, final int maxLength, Callback filterCallback, Random random) {
		// STEP 1: Set up oracle grammar sampler
		Iterator<String> oracleIterator = new FilteredIterable<String>(new SampleIterable(new MultiGrammarSampler(grammar, boxSize, random)), new Filter<String>() { public boolean filter(String s) { return s.length() < maxLength; }}, filterCallback).iterator();
		
		// STEP 2: Set up oracle
		DiscriminativeOracle oracle = new NormalGrammarOracle(new NormalGrammar(grammar));
		
		// STEP 3: Get false negative rate
		int fn = 0;
		for(int i=0; i<numTestSamples; i++) {
			// STEP 5a: Get sample
			String sample = sample(oracleIterator);
			
			// STEP 5b: Check sample
			if(!learner.query(sample)) {
				fn++;
			}
			Log.info("CUR: " + fn + ", " + i);
		}
		
		// STEP 7: Get false positive/negative rate
		int fp = 0;
		for(int i=0; i<numTestSamples; i++) {
			// STEP 7a: Get sample
			String sample = learner.sample();
			
			// STEP 7b: Check sample
			if(!oracle.query(sample)) {
				fp++;
			}
			Log.info("CUR: " + fp + ", " + i);
		}
		Pair<Double,Double> pair = new Pair<Double,Double>((double)fn/numTestSamples, (double)fp/numTestSamples);
		Log.info("FN RATE: " + pair.getX());
		Log.info("FP RATE: " + pair.getY());
		
		return pair;
	}
}
