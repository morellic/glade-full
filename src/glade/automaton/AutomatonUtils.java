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

package glade.automaton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.libalf.BasicAutomaton;
import de.libalf.BasicTransition;
import de.libalf.Knowledgebase;
import de.libalf.LearningAlgorithm;
import de.libalf.LibALFFactory.Algorithm;
import de.libalf.jni.JNIFactory;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import glade.grammar.MultiGrammarUtils.MultiGrammar;
import glade.grammar.MultiGrammarUtils.MultiProduction;
import glade.program.ProgramDataUtils.ProgramExamples;
import glade.util.CharacterUtils;
import glade.util.Log;
import glade.util.IteratorUtils.SampleIterable;
import glade.util.IteratorUtils.Sampler;
import glade.util.LearnerUtils.DiscriminativeLearner;
import glade.util.LearnerUtils.InteractiveLearner;
import glade.util.OracleUtils.DiscriminativeOracle;
import glade.util.OracleUtils.HybridComplementOracle;
import glade.util.OracleUtils.HybridOracle;
import glade.util.OracleUtils.InteractiveOracle;
import glade.util.Utils.MultivalueMap;
import glade.util.Utils.Pair;

public class AutomatonUtils {
	public static void init() {
		System.load(new File("lib/libalf.so").getAbsolutePath());
		System.load(new File("lib/libjalf.so").getAbsolutePath());
	}
	
	public static int[] queryToQueryLibALF(String query) {
		if(query == null) {
			return null;
		}
		int[] queryLibALF = new int[query.length()];
		for(int i=0; i<query.length(); i++) {
			queryLibALF[i] = (int)query.charAt(i);
		}
		return queryLibALF;
	}
	
	public static String queryLibALFToQuery(int[] queryLibALF) {
		StringBuilder query = new StringBuilder();
		for (int i : queryLibALF) {
			query.append((char)i);
		}
		return query.toString();
	}
	
	// e.g. [2,4,1,3]
	public static String queryLibALFToString(int[] queryLibALF) {
		StringBuilder result = new StringBuilder();
		result.append("[");
		for(int i : queryLibALF) {
			result.append(i).append(",");
		}
		if(queryLibALF.length == 0) {
			result.append(",");
		}
		result.delete(result.length() - ",".length(), result.length()).append("]");
		return result.toString();
	}
	
	public static Automaton automatonLibALFToAutomaton(BasicAutomaton automatonLibALF) {
		// STEP 0: Initialize automaton
		Automaton automaton = new Automaton();
		
		// STEP 1: Set up states
		State[] states = new State[automatonLibALF.getNumberOfStates()];
		for(int i=0; i<states.length; i++) {
			states[i] = new State();
		}
		for(int i : automatonLibALF.getFinalStates()) {
			states[i].setAccept(true);
		}
		
		// STEP 2: Set up transitions
		for (BasicTransition transition : automatonLibALF.getTransitions()) {
			states[transition.source].addTransition(new Transition((char)transition.label, states[transition.destination]));
		}
		
		// STEP 3: Set initial states (first use of automaton)
		if (automatonLibALF.getInitialStates().size() != 1) {
			throw new RuntimeException("Multiple initial states!");
		}
		automaton.setInitialState(states[automatonLibALF.getInitialStates().iterator().next()]);
		
		// STEP 4: Finalize automaton
		automaton.setDeterministic(automatonLibALF.isDFA());
		automaton.restoreInvariant();
		
		return automaton;
	}
	
	public static class AutomatonOracle implements DiscriminativeOracle, InteractiveOracle<Automaton> {
		private final Automaton automaton;
		
		public AutomatonOracle(Automaton automaton) {
			this.automaton = automaton;
		}
		
		public Automaton getAutomaton() {
			return this.automaton;
		}
		
		@Override
		public boolean query(String query) {
			return automaton.run(query);
		}

		@Override
		public String equivalenceQuery(Automaton proposedAutomaton) {
			Automaton diff1 = proposedAutomaton.minus(this.automaton);
			if(!diff1.isEmpty()) {
				return diff1.getShortestExample(true);
			}
			Automaton diff2 = this.automaton.minus(proposedAutomaton);
			if(!diff2.isEmpty()) {
				return diff2.getShortestExample(true);
			}
			return null;
		}
	}
	
	public static class RandomAutomatonOracle implements HybridOracle, HybridComplementOracle {
		private final Automaton automaton;
		private final Automaton automatonComplement;
		private final double automatonTerminationProbability;
		private final double complementProbability;
		private final int maxLength;
		private final Random random;
		
		public RandomAutomatonOracle(Automaton automaton, double automatonTerminationProbability, double complementProbability, int maxLength, Random random) {
			this.automaton = automaton;
			this.automatonComplement = automaton.complement();
			this.automatonTerminationProbability = automatonTerminationProbability;
			this.complementProbability = complementProbability;
			this.maxLength = maxLength;
			this.random = random;
		}
		
		public RandomAutomatonOracle(Automaton automaton, Random random) {
			this(automaton, 0.1, 0.5, 100, random);
		}
		
		@Override
		public boolean query(String query) {
			return this.automaton.run(query);
		}
		
		private String sampleAutomaton(Automaton sampleAutomaton) {
			return new RandomAutomatonString(this.random).nextString(sampleAutomaton, this.automatonTerminationProbability, this.maxLength);
		}
		
		@Override
		public String sample() {
			return this.sampleAutomaton(this.automaton);
		}
		
		@Override
		public Pair<String,Boolean> sampleWithComplement() {
			boolean isComplement = this.random.nextDouble() < this.complementProbability;
			return new Pair<String,Boolean>(this.sampleAutomaton(isComplement ? this.automatonComplement : this.automaton), isComplement);
		}
		
		public Automaton getAutomaton() {
			return this.automaton;
		}
	}
	
	public static class RPNILearner implements DiscriminativeLearner<Automaton> {
		private final Map<String,Boolean> queries = new HashMap<String,Boolean>();

		@Override
		public Automaton getModel() {
			init();
			Knowledgebase base = JNIFactory.STATIC.createKnowledgebase();
			for(Map.Entry<String,Boolean> entry : this.queries.entrySet()) {
				base.add_knowledge(queryToQueryLibALF(entry.getKey()), entry.getValue());
			}
			Log.info("LEARNING AUTOMATON");
			long time = System.currentTimeMillis();
			Automaton aut = automatonLibALFToAutomaton((BasicAutomaton)JNIFactory.STATIC.createLearningAlgorithm(Algorithm.RPNI, new Object[]{base, 2}).advance());
			Log.info("DONE: " + (System.currentTimeMillis() - time));
			return aut;
		}

		@Override
		public void update(String query, boolean isMember) {
			this.queries.put(query, isMember);
		}
	}
	
	public static class LStarLearner implements InteractiveLearner<Automaton> {
		private final Knowledgebase base = JNIFactory.STATIC.createKnowledgebase();
		private final LearningAlgorithm algorithm = JNIFactory.STATIC.createLearningAlgorithm(Algorithm.ANGLUIN, new Object[] {this.base, 2});
		
		// Logic:
		// - getModel() (getQuery()) returns the currentModel (currentQuery) when isNextModel() is true (false), otherwise throws an exception
		// - step() is called after any update to set the parameters for the next query
		private LinkedList<int[]> queuedQueries = new LinkedList<int[]>();
		private int[] currentQuery = null;
		private BasicAutomaton currentModel = null;
		
		public LStarLearner() { step(); }
		
		private void step() {
			this.currentQuery = null;
			this.currentModel = null;
			if(this.queuedQueries.isEmpty()) {
				this.currentModel = (BasicAutomaton)this.algorithm.advance();
				if(this.currentModel == null) {
					this.queuedQueries = this.base.get_queries();
					this.currentQuery = this.queuedQueries.removeFirst();
				}
			} else {
				this.currentQuery = this.queuedQueries.removeFirst();
			}
		}

		@Override
		public boolean isNextQueryModel() {
			return this.currentModel != null;
		}

		@Override
		public String getQuery() {
			return queryLibALFToQuery(this.currentQuery);
		}

		@Override
		public Automaton getModel() {
			return automatonLibALFToAutomaton(this.currentModel);
		}

		@Override
		public void update(String query, boolean isMember) {
			this.base.add_knowledge(queryToQueryLibALF(query), isMember);
			this.step();
		}

		@Override
		public void updateModel(Automaton model, String counterExample) {
			this.algorithm.add_counterexample(queryToQueryLibALF(counterExample));
			this.step();
		}
	}

	
	public static class WrappedLearner implements InteractiveLearner<AutomatonOracle> {
		private final InteractiveLearner<Automaton> learner;
		
		public WrappedLearner(InteractiveLearner<Automaton> learner) {
			this.learner = learner;
		}

		@Override
		public String getQuery() {
			return this.learner.getQuery();
		}

		@Override
		public AutomatonOracle getModel() {
			return new AutomatonOracle(this.learner.getModel());
		}

		@Override
		public void update(String string, boolean isMember) {
			this.learner.update(string, isMember);
		}

		@Override
		public boolean isNextQueryModel() {
			return this.learner.isNextQueryModel();
		}

		@Override
		public void updateModel(AutomatonOracle model, String counterExample) {
			this.learner.updateModel(model.getAutomaton(), counterExample);
		}
	}
	
	public static class RandomAutomatonString {
		public final Random random;
		public RandomAutomatonString(Random random) { this.random = random; }
		public RandomAutomatonString() { this(new Random()); }
		
		// In final state, end with given probability
		public String nextString(Automaton automaton, double terminationProbability, int maxLength) {
			Automaton autClone = automaton.clone();
			if(autClone.isEmpty()) {
				throw new RuntimeException();
			}
			State state = autClone.getInitialState();
			StringBuilder sample = new StringBuilder();
			for(int i=0; i<maxLength; i++) {
				// STEP 1: If end state, return with the given probability (or if it has no outgoing states)
				if(autClone.getAcceptStates().contains(state)) {
					if(state.getTransitions().size() == 0 || this.random.nextDouble() < terminationProbability) {
						return sample.toString();
					}
				}
				
				// STEP 2: Compute the target states
				MultivalueMap<State,Transition> targetMap = new MultivalueMap<State,Transition>();
				for(Transition transition : state.getTransitions()) {
					if(autClone.getLiveStates().contains(transition.getDest())) {
						targetMap.add(transition.getDest(), transition);
					}
				}
				// If there are no targets, then return null (unless it was an end state, see above)
				List<State> targets = new ArrayList<State>(targetMap.keySet());
				if(targets.size() == 0) {
					System.out.println("RECURSING");
					return nextString(autClone, terminationProbability, maxLength);
				}
				
				// STEP 3: Choose a uniformly random target state
				state = targets.get(this.random.nextInt(targets.size()));
				
				// STEP 4: Compute the transition symbols
				List<Character> symbols = new ArrayList<Character>();
				for(Transition transition : targetMap.get(state)) {
					for(char c=transition.getMin(); c<=transition.getMax(); c++) {
						if(CharacterUtils.isAsciiCharacter(c)) {
							symbols.add(c);
						}
					}
				}
				
				// STEP 5: Choose a uniformly random transition symbol
				sample.append(symbols.get(this.random.nextInt(symbols.size())));
			}
			
			autClone.setInitialState(state);
			sample.append(autClone.getShortestExample(true));
			return sample.toString();
		}
		
		public String nextString(Automaton automaton) {
			return this.nextString(automaton, 0.5, 100);
		}
	}
	
	public static class AutomatonSampler implements Sampler {
		private final Automaton automaton;
		private final RandomAutomatonString randomAutomaton;
		
		public AutomatonSampler(Automaton automaton, Random random) {
			this.automaton = automaton;
			this.randomAutomaton = new RandomAutomatonString(random);
		}
		
		@Override
		public String sample() {
			return this.randomAutomaton.nextString(automaton);
		}
	}
	
	public static MultiGrammar convertAutomaton(Automaton aut) {
		aut.minimize();
		List<MultiProduction> productions = new ArrayList<MultiProduction>();
		String startSymbol = "START";
		
		// q = q_init => L_q -> \epsilon
		productions.add(new MultiProduction(aut.getInitialState(), new Object[]{}));
		
		// q \in F => S -> L_q
		for(State state : aut.getAcceptStates()) {
			productions.add(new MultiProduction(startSymbol, new Object[]{state}));
		}
		
		// q' = \delta(q, \sigma) => L_{q'} -> L_q \sigma
		for(State state : aut.getStates()) {
			for(Transition transition : state.getTransitions()) {
				for(char c=transition.getMin(); c<=transition.getMax(); c++) {
					if(CharacterUtils.isAsciiCharacter(c)) {
						productions.add(new MultiProduction(transition.getDest(), new Object[]{state, (Character)c}));
					}
				}
			}
		}
		
		return new MultiGrammar(productions, startSymbol);
	}
	
	private static Map<Pair<State,Character>,State> getTransitionTable(Automaton aut) {
		Map<Pair<State,Character>,State> transitionTable = new HashMap<Pair<State,Character>,State>(); // {(qCur, character) -> qNext}
		for(State state : aut.getStates()) {
			for(Transition transition : state.getTransitions()) {
				for(char c=transition.getMin(); c<=transition.getMax(); c++) {
					if(CharacterUtils.isAsciiCharacter(c)) {
						transitionTable.put(new Pair<State,Character>(state, c), transition.getDest());
					}
				}
			}
		}
		return transitionTable;
	}
	
	private static Map<State,Pair<State,Transition>> getReverseSpanningTree(Automaton aut, State qStart) {
		// STEP 0: Set up
		Map<State,Pair<State,Transition>> st = new HashMap<State,Pair<State,Transition>>(); // reverse spanning tree {child -> (parent, edge)}
		LinkedList<State> worklist = new LinkedList<State>(); // worklist
		worklist.add(qStart);
		
		// STEP 1: Breadth-first search
		while(!worklist.isEmpty()) {
			State qCur = worklist.removeFirst();
			for(Transition transition : qCur.getTransitions()) {
				// Get next state
				State qNext = transition.getDest();
				
				// Skip if visited
				if(qNext.equals(qStart) || st.containsKey(qNext)) {
					continue;
				}
				
				// Update worklist
				worklist.addLast(qNext);
				
				// Update spanning tree
				st.put(qNext, new Pair<State,Transition>(qCur, transition));
			}
		}
		
		return st;
	}
	
	private static Map<State,Map<State,Pair<State,Transition>>> getReverseSpanningTrees(Automaton aut) {
		Map<State,Map<State,Pair<State,Transition>>> rsts = new HashMap<State,Map<State,Pair<State,Transition>>>();
		for(State qStart : aut.getStates()) {
			rsts.put(qStart, getReverseSpanningTree(aut, qStart));
		}
		return rsts;
	}
	
	// states that can reach qEnd
	private static Set<State> getReverseReachable(Map<State,Map<State,Pair<State,Transition>>> reverseSpanningTrees, State qEnd) {
		Set<State> rr = new HashSet<State>();
		rr.add(qEnd);
		for(Map.Entry<State,Map<State,Pair<State,Transition>>> entry : reverseSpanningTrees.entrySet()) {
			if(entry.getValue().containsKey(qEnd)) {
				rr.add(entry.getKey());
			}
		}
		return rr;
	}
	
	private static char getRandomCharacter(Transition transition, Random random) {
		List<Character> choices = new ArrayList<Character>();
		for(char c=transition.getMin(); c<=transition.getMax(); c++) {
			if(CharacterUtils.isAsciiCharacter(c)) {
				choices.add(c);
			}
		}
		return choices.get(random.nextInt(choices.size()));
	}
	
	private static List<Transition> getPathFromRoot(Map<State,Pair<State,Transition>> reverseSpanningTree, State qStart) {
		List<Transition> path = new ArrayList<Transition>();
		State qCur = qStart;
		while(reverseSpanningTree.containsKey(qCur)) {
			Pair<State,Transition> pair = reverseSpanningTree.get(qCur);
			path.add(pair.getY());
			qCur = pair.getX();
		}
		Collections.reverse(path);;
		return path;
	}
	
	private static String getRandomStringToRoot(Map<State,Pair<State,Transition>> reverseSpanningTree, State qStart, Random random) {
		List<Transition> pathToRoot = getPathFromRoot(reverseSpanningTree, qStart);
		StringBuilder sb = new StringBuilder();
		for(Transition transition : pathToRoot) {
			sb.append(getRandomCharacter(transition, random));
		}
		return sb.toString();
	}
	
	// if qStart = qEnd, returns a single loop
	private static String getRandomPath(Automaton aut, Map<State,Map<State,Pair<State,Transition>>> reverseSpanningTrees, State qStart, State qEnd, int maxLength, Random random) {
		StringBuilder sb = new StringBuilder();
		State qCur = qStart;
		
		Set<State> reverseReachable = getReverseReachable(reverseSpanningTrees, qEnd);
		
		for(int i=0; i<maxLength; i++) {
			// Step 1: Get valid transitions
			List<Transition> transitions = new ArrayList<Transition>();
			for(Transition transition : qCur.getTransitions()) {
				if(reverseReachable.contains(transition.getDest())) {
					transitions.add(transition);
				}
			}
			
			// STEP 2: Get random transition
			int transitionChoice = random.nextInt(transitions.size());
			Transition transition = transitions.get(transitionChoice);
			
			// STEP 3: Get random character for transition
			char c = getRandomCharacter(transition, random);
			
			// STEP 4: Update state
			qCur = transition.getDest();
			sb.append(c);
			
			// STEP 5: Break if done
			if(qCur.equals(qEnd)) {
				break;
			}
		}
		
		sb.append(getRandomStringToRoot(reverseSpanningTrees.get(qEnd), qCur, random));
		return sb.toString();
	}
	
	private static Pair<Integer,Integer> getUniformOrderedPair(int max, Random random) {
		int first = random.nextInt(max);
		int second = random.nextInt(max);
		return first < second ? new Pair<Integer,Integer>(first, second) : new Pair<Integer,Integer>(second, first);
	}
	
	private static State getState(Map<Pair<State,Character>,State> transitionTable, State qStart, String str) {
		State qCur = qStart;
		for(char c : str.toCharArray()) {
			qCur = transitionTable.get(new Pair<State,Character>(qCur, c));
		}
		return qCur;
	}
	
	private static Pair<Integer,Integer> getSingletonPair(int max, Random random) {
		int val = random.nextInt(max);
		return new Pair<Integer,Integer>(val, val);
	}
	
	private static String getSingleMutation(Automaton aut, Map<State,Map<State,Pair<State,Transition>>> reverseSpanningTrees, Map<Pair<State,Character>,State> transitionTable, String curExample, int maxMutationLength, double pInsert, Random random) {
		Pair<Integer,Integer> pair = random.nextDouble() < pInsert ? getSingletonPair(curExample.length(), random) : getUniformOrderedPair(curExample.length(), random);
		String prefix = curExample.substring(0, pair.getX());
		String infix = curExample.substring(pair.getX(), pair.getY());
		String postfix = curExample.substring(pair.getY());
		State qStart = getState(transitionTable, aut.getInitialState(), prefix);
		State qEnd = getState(transitionTable, qStart, infix);
		return prefix + getRandomPath(aut, reverseSpanningTrees, qStart, qEnd, maxMutationLength, random) + postfix;
	}
	
	public static class AutomatonMutationSampler implements Sampler {
		private final Automaton aut;
		private final Map<State,Map<State,Pair<State,Transition>>> reverseSpanningTrees;
		private final Map<Pair<State,Character>,State> transitionTable;
		private final List<String> examples = new ArrayList<String>();
		private final int numMutations;
		private final int maxMutationLength;
		private final double pInsert;
		private final Random random;
		
		public AutomatonMutationSampler(Automaton aut, Iterable<String> examples, int numMutations, int maxMutationLength, double pInsert, Random random) {
			this.aut = aut;
			this.reverseSpanningTrees = getReverseSpanningTrees(this.aut);
			this.transitionTable = getTransitionTable(this.aut);
			for(String example : examples) {
				this.examples.add(example);
			}
			this.numMutations = numMutations;
			this.maxMutationLength = maxMutationLength;
			this.pInsert = pInsert;
			this.random = random;
		}
		
		@Override
		public String sample() {
			int choice = this.random.nextInt(this.examples.size());
			String curExample = this.examples.get(choice);
			for(int i=0; i<this.numMutations; i++) {
				curExample = getSingleMutation(this.aut, this.reverseSpanningTrees, this.transitionTable, curExample, this.maxMutationLength, this.pInsert, this.random);
			}
			return curExample;
		}
	}

	public static Iterable<String> getAutomatonMutationSampler(Automaton automaton, ProgramExamples examples, int numMutations, int maxAutomatonMutationLength, double pAutomatonMutationInsert, Random random) {
		return new SampleIterable(new AutomatonMutationSampler(automaton, examples.getTrainExamples(), numMutations, maxAutomatonMutationLength, pAutomatonMutationInsert, random));
	}
}
