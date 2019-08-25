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

package glade.grammar.fuzz;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import glade.afl.AflOracleUtils.AflCoverageOracle;
import glade.grammar.ParseTreeUtils;
import glade.grammar.GrammarUtils.Node;
import glade.grammar.GrammarUtils.NodeMerges;
import glade.grammar.ParseTreeUtils.ParseTreeNode;
import glade.grammar.fuzz.GrammarFuzzer.SampleParameters;
import glade.util.OracleUtils.DiscriminativeOracle;
import glade.util.Utils.Pair;

public class GrammarTraceFuzzer {
	public static interface Acceptor {
		public abstract boolean accept(double scoreOrig, double scoreNew);
	}
	
	public static class HillClimbAcceptor implements Acceptor {
		private final boolean newBreakTies;
		
		public HillClimbAcceptor(boolean newBreaksTies) {
			this.newBreakTies = false;
		}
		
		@Override
		public boolean accept(double scoreOrig, double scoreNew) {
			return scoreOrig == scoreNew ? this.newBreakTies : scoreOrig < scoreNew;
		}		
	}
	
	public static class MCMCAcceptor implements Acceptor {
		private final Random random;
		
		public MCMCAcceptor(Random random) {
			this.random = random;
		}

		@Override
		public boolean accept(double scoreOrig, double scoreNew) {
			return this.random.nextDouble() <= (scoreNew/scoreOrig);
		}
	}
	
	private static <T extends StructuredExample> double[][] getDefaultWeights(T seed, AflCoverageOracle oracle) {
		int numInts = oracle.getFullCoverage(seed.getExample()).length;
		double[][] weights = new double[numInts][32];
		for(int i=0; i<numInts; i++) {
			for(int j=0; j<32; j++) {
				weights[i][j] = 1.0;
			}
		}
		return weights;
	}
	
	private static <T extends StructuredExample> boolean[][] getDefaultCovered(T seed, AflCoverageOracle oracle) {
		int numInts = oracle.getFullCoverage(seed.getExample()).length;
		boolean[][] covered = new boolean[numInts][32];
		for(int i=0; i<numInts; i++) {
			for(int j=0; j<32; j++) {
				covered[i][j] = false;
			}
		}
		return covered;
	}
	
	public static interface StructuredExample {
		public abstract String getExample();
	}
	
	public static interface GrammarProblem<T extends StructuredExample> {
		public abstract T seed();
		public abstract T sample(T seed);
	}
	
	public static class ParseTreeExample implements StructuredExample {
		public final ParseTreeNode node;
		public ParseTreeExample(ParseTreeNode node) {
			this.node = node;
		}
		public String getExample() {
			return this.node.getExample();
		}
	}
	
	public static class LearnedGrammarProblem implements GrammarProblem<ParseTreeExample> {
		private final Pair<Node,NodeMerges> pair;
		private final DiscriminativeOracle oracle;
		private final SampleParameters parameters;
		private final int maxLength;
		private final Random random;
		
		public LearnedGrammarProblem(Pair<Node,NodeMerges> pair, DiscriminativeOracle oracle, SampleParameters parameters, int maxLength, Random random) {
			this.pair = pair;
			this.oracle = oracle;
			this.parameters = parameters;
			this.maxLength = maxLength;
			this.random = random;
		}
		
		@Override
		public ParseTreeExample seed() {
			ParseTreeExample seed = new ParseTreeExample(GrammarFuzzer.sample(this.pair.getX(), this.pair, this.parameters, this.random));
			return seed.getExample().length() <= this.maxLength && this.oracle.query(seed.getExample()) ? seed : this.seed();
		}
		
		@Override
		public ParseTreeExample sample(ParseTreeExample seed) {
			List<ParseTreeNode>[] descendants = ParseTreeUtils.getDescendantsByType(seed.node);
			int isMultiConstant = descendants[1].isEmpty() || (!descendants[0].isEmpty() && this.random.nextBoolean()) ? 0 : 1;
			int choice = this.random.nextInt(descendants[isMultiConstant].size());
			ParseTreeNode cur = descendants[isMultiConstant].get(choice);
			ParseTreeNode sub = GrammarFuzzer.sample(cur.getNode(), this.pair, this.parameters, this.random);
			ParseTreeNode result = ParseTreeUtils.getSubstitute(seed.node, cur, sub);
			return result.getExample().length() <= this.maxLength && this.oracle.query(result.getExample()) ? new ParseTreeExample(result) : this.sample(seed);
		}
	}
	
	public static class GrammarOptimizationIterable<T extends StructuredExample> implements Iterable<String> {
		private final GrammarProblem<T> problem;
		private final AflCoverageOracle oracle;
		private final Acceptor acceptor;
		private final int numSubIters;
		private final int numBeams;
		private final Random random;
		
		public GrammarOptimizationIterable(GrammarProblem<T> problem, AflCoverageOracle oracle,  Acceptor acceptor, int numSubIters, int numBeams, Random random) {
			this.problem = problem;
			this.oracle = oracle;
			this.acceptor = acceptor;
			this.numSubIters = numSubIters;
			this.numBeams = numBeams;
			this.random = random;
		}
		
		@Override
		public Iterator<String> iterator() {
			return new GrammarOptimizationIterator<T>(this);
		}
	}
	
	public static class GrammarOptimizationIterator<T extends StructuredExample> implements Iterator<String> {
		private final GrammarOptimizationIterable<T> optimization;
		
		private final double[][] weights;
		private final boolean[][] covered;
		private final List<T> nodes;
		private int curCovered;
		
		private Pair<List<Pair<T,int[]>>,Double> cur;
		private Pair<List<Pair<T,int[]>>,Double> max;
		private int iter;
		
		public GrammarOptimizationIterator(GrammarOptimizationIterable<T> optimization) {
			this.optimization = optimization;
			this.iter = 1;
			
			// STEP 0: Setup
			this.weights = GrammarTraceFuzzer.getDefaultWeights(this.optimization.problem.seed(), this.optimization.oracle);
			this.covered = GrammarTraceFuzzer.getDefaultCovered(this.optimization.problem.seed(), this.optimization.oracle);
			this.nodes = new ArrayList<T>();
			this.curCovered = 0;
			
			// STEP 1: Seed
			this.cur = this.seed();
			this.max = cur;
		}
		
		@Override
		public String next() {
			// STEP 0: Return if out of iterations
			if(!this.hasNext()) {
				return null;
			}
			
			// STEP 1: Sample
			int choice = this.optimization.random.nextInt(this.cur.getX().size());
			T tree = this.optimization.problem.sample(this.cur.getX().get(choice).getX());
			List<Pair<T,int[]>> newSeed = new ArrayList<Pair<T,int[]>>(this.cur.getX());
			newSeed.set(choice, new Pair<T,int[]>(tree, this.optimization.oracle.getFullCoverage(tree.getExample())));
			Pair<List<Pair<T,int[]>>,Double> proposal = this.scoreAndUpdate(newSeed, choice);
			
			// STEP 2: Acceptance
			if(proposal.getY() > this.max.getY()) {
				this.max = proposal;
			}
			if(this.optimization.acceptor.accept(cur.getY(), proposal.getY())) {
				this.cur = proposal;
			}
			
			// STEP 3: Update if super iteration
			if(this.iter%this.optimization.numSubIters == 0) {
				this.updateWeights(this.max.getX());
				this.max = scoreAndUpdate(this.max.getX(), -1);
				this.cur = this.max;
			}
			
			// STEP 4: Return
			this.iter++;
			return tree.getExample();
		}
		
		@Override
		public boolean hasNext() {
			return true;
		}
		
		@Override
		public void remove() {
			throw new RuntimeException();
		}
		
		private Pair<List<Pair<T,int[]>>,Double> seed() {
			T tree = this.optimization.problem.seed();
			Pair<T,int[]> seedSingle = new Pair<T,int[]>(tree, this.optimization.oracle.getFullCoverage(tree.getExample()));
			List<Pair<T,int[]>> seed = new ArrayList<Pair<T,int[]>>();
			for(int i=0; i<this.optimization.numBeams; i++) {
				seed.add(seedSingle);
			}
			return this.scoreAndUpdate(seed, -1);
		}
		
		private Pair<List<Pair<T,int[]>>,Double> scoreAndUpdate(List<Pair<T,int[]>> sample, int choice) {
			int numInts = sample.get(0).getY().length;
			int[] allBits = new int[numInts];
			for(Pair<T,int[]> pair : sample) {
				int[] curBits = pair.getY();
				for(int j=0; j<curBits.length; j++) {
					allBits[j] |= curBits[j];
				}
			}
			double score = 0.0;
			boolean coveredNew = false;
			for(int i=0; i<numInts; i++) {
				int bits = allBits[i];
				for(int j=0; j<32; j++) {
					if(bits%2 == 1) {
						score += this.weights[i][j];
						if(!this.covered[i][j]) {
							this.covered[i][j] = true;
							this.curCovered++;
							coveredNew = true;
						}
					}
					bits >>= 1;
				}
			}
			if(coveredNew && choice != -1) {
				this.nodes.add(sample.get(choice).getX());
			}
			return new Pair<List<Pair<T,int[]>>,Double>(sample, score);
		}
		
		private void updateWeights(List<Pair<T,int[]>> beams) {
			int numInts = beams.get(0).getY().length;
			int[][] counts = new int[numInts][32];
			for(Pair<T,int[]> beam : beams) {
				for(int i=0; i<numInts; i++) {
					int bits = beam.getY()[i];
					for(int j=0; j<32; j++) {
						counts[i][j] += bits%2;
						bits >>= 1;
					}
				}
			}
			for(int i=0; i<numInts; i++) {
				for(int j=0; j<32; j++) {
					this.weights[i][j] = Math.pow(2.0, -(double)counts[i][j]);
				}
			}
		}
	}
}
