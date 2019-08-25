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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import glade.grammar.GrammarUtils;
import glade.grammar.ParseTreeUtils;
import glade.grammar.GrammarUtils.ConstantNode;
import glade.grammar.GrammarUtils.MultiAlternationNode;
import glade.grammar.GrammarUtils.MultiConstantNode;
import glade.grammar.GrammarUtils.Node;
import glade.grammar.GrammarUtils.NodeMerges;
import glade.grammar.GrammarUtils.RepetitionNode;
import glade.grammar.ParseTreeUtils.ParseTreeConstantNode;
import glade.grammar.ParseTreeUtils.ParseTreeMultiConstantNode;
import glade.grammar.ParseTreeUtils.ParseTreeNode;
import glade.grammar.ParseTreeUtils.ParseTreeRepetitionNode;
import glade.util.IteratorUtils.Sampler;
import glade.util.RandomUtils.RandomExtra;
import glade.util.Utils.Pair;

public class GrammarFuzzer {
	public static class SampleParameters {
		private final double[] pRepetition;
		private final double pRecursion;
		private final double pAllCharacters;
		private final int boxSize;
		private final boolean omitPound;
		public SampleParameters(double[] pRepetition, double pRecursion, double pAllCharacters, int boxSize, boolean omitPound) {
			this.pRepetition = pRepetition;
			this.pRecursion = pRecursion;
			this.pAllCharacters = pAllCharacters;
			this.boxSize = boxSize;
			this.omitPound = omitPound;
		}
		public boolean randRecursion(Random random) {
			return this.pRecursion >= random.nextDouble();
		}
		public boolean randAllCharacters(Random random) {
			return this.pAllCharacters >= random.nextDouble();
		}
		public int randRepetition(Random random) {
			double sample = random.nextDouble();
			double sum = 0.0;
			for(int i=0; i<this.pRepetition.length; i++) {
				sum += this.pRepetition[i];
				if(sum >= sample) {
					return i;
				}
			}
			return this.pRepetition.length;
		}
		public int randAlternation(Random random) {
			return random.nextInt(3);
		}
		public int randMultiAlternation(Random random, int numChoices) {
			return random.nextInt(numChoices);
		}
		public int getBoxSize() {
			return this.boxSize;
		}
		public boolean isOmitPound() {
			return this.omitPound;
		}
	}
	
	public static class IntBox {
		private int value;
		public IntBox(int value) {
			this.value = value;
		}
		public void decrement() {
			this.value--;
		}
		public int value() {
			return this.value;
		}
	}
	
	private static ParseTreeNode sampleHelper(Node grammar, NodeMerges recursiveNodes, SampleParameters parameters, Random random, Map<Node,ParseTreeNode> backup, IntBox length) {
		if(length.value() == 0) {
			return backup.get(grammar);
		}
		length.decrement();
		if(!recursiveNodes.get(grammar).isEmpty() && parameters.randRecursion(random)) {
			int choice = parameters.randMultiAlternation(random, recursiveNodes.get(grammar).size());
			return sampleHelper(new ArrayList<Node>(recursiveNodes.get(grammar)).get(choice), recursiveNodes, parameters, random, backup, length);
		} else if(grammar instanceof MultiAlternationNode) {
			MultiAlternationNode node = (MultiAlternationNode)grammar;
			int choice = parameters.randMultiAlternation(random, node.getChildren().size());
			return sampleHelper(node.getChildren().get(choice), recursiveNodes, parameters, random, backup, length);
		} else if(grammar instanceof RepetitionNode) {
			ParseTreeNode start = sampleHelper(((RepetitionNode)grammar).start, recursiveNodes, parameters, random, backup, length);
			List<ParseTreeNode> rep = new ArrayList<ParseTreeNode>();
			int reps = parameters.randRepetition(random);
			for(int i=0; i<reps; i++) {
				rep.add(sampleHelper(((RepetitionNode)grammar).rep, recursiveNodes, parameters, random, backup, length));
			}
			ParseTreeNode end = sampleHelper(((RepetitionNode)grammar).end, recursiveNodes, parameters, random, backup, length);
			return new ParseTreeRepetitionNode((RepetitionNode)grammar, start, rep, end);
		} else if(grammar instanceof MultiConstantNode) {
			MultiConstantNode mconstNode = (MultiConstantNode)grammar;
			StringBuilder sb = new StringBuilder();
			boolean useAllCharacters = parameters.randAllCharacters(random);
			for(Set<Character> characterOption : useAllCharacters ? mconstNode.characterOptions : mconstNode.characterChecks) {
				List<Character> characterOptionList = new ArrayList<Character>();
				if(parameters.omitPound) {
					for(Character c : characterOption) {
						if(characterOption.size() == 1 || ((char)c) != '#') {
							characterOptionList.add(c);
						}
					}
				} else {
					characterOptionList.addAll(characterOption);
				}
				int choice = parameters.randMultiAlternation(random, characterOptionList.size());
				sb.append(characterOptionList.get(choice));
			}
			return new ParseTreeMultiConstantNode(mconstNode, sb.toString());
		} else if(grammar instanceof ConstantNode) {
			return new ParseTreeConstantNode((ConstantNode)grammar, grammar.getData().example);
		} else {
			throw new RuntimeException("Invalid program node: " + grammar);
		}
	}
	
	private static void getBackup(ParseTreeNode node, Map<Node,ParseTreeNode> backup) {
		backup.put(node.getNode(), node);
		for(ParseTreeNode child : node.getChildren()) {
			getBackup(child, backup);
		}
	}
	
	public static ParseTreeNode sample(Node program, Pair<Node,NodeMerges> grammar, SampleParameters parameters, Random random) {
		Map<Node,ParseTreeNode> backup = new HashMap<Node,ParseTreeNode>();
		if(grammar.getX() instanceof MultiAlternationNode) {
			for(ParseTreeNode parseTree : ParseTreeUtils.getParseTreeAlt((MultiAlternationNode)grammar.getX())) {
				getBackup(parseTree, backup);
			}
		} else {
			getBackup(ParseTreeUtils.getParseTree(grammar.getX()), backup);
		}
		for(Node node : grammar.getY().keySet()) {
			if(!backup.containsKey(node)) {
				throw new RuntimeException("Invalid node: " + node);
			}
			for(Node merge : grammar.getY().get(node)) {
				if(!backup.containsKey(merge)) {
					throw new RuntimeException("Invalid node: " + node);
				}
			}
		}
		for(Node descendant : GrammarUtils.getDescendants(program)) {
			if(!backup.containsKey(descendant)) {
				throw new RuntimeException("Invalid node: " + descendant);
			}
		}
		return sampleHelper(program, grammar.getY(), parameters, random, backup, new IntBox(parameters.getBoxSize()));
	}
	
	public static class GrammarSampler implements Sampler {
		private final Pair<Node,NodeMerges> pair;
		private final SampleParameters parameters;
		private final Random random;
		
		public GrammarSampler(Pair<Node,NodeMerges> pair, SampleParameters parameters, Random random) {
			this.pair = pair;
			this.parameters = parameters;
			this.random = random;
		}
		
		@Override
		public String sample() {
			return GrammarFuzzer.sample(this.pair.getX(), this.pair, this.parameters, this.random).getExample();
		}
	}
	
	public static class GrammarMutationSampler implements Sampler {
		private final Pair<Node,NodeMerges> pair;
		private final SampleParameters parameters;
		private final int maxLength;
		private final int numMutations;
		private final Random random;
		
		public GrammarMutationSampler(Pair<Node,NodeMerges> pair, SampleParameters parameters, int maxLength, int numMutations, Random random) {
			this.pair = pair;
			this.parameters = parameters;
			this.maxLength = maxLength;
			this.numMutations = numMutations;
			this.random = random;
		}
		
		private ParseTreeNode sampleHelper(ParseTreeNode seed) {
			List<ParseTreeNode>[] descendants = ParseTreeUtils.getDescendantsByType(seed);
			int isMultiConstant = descendants[1].isEmpty() || (!descendants[0].isEmpty() && this.random.nextBoolean()) ? 0 : 1;
			int choice = this.random.nextInt(descendants[isMultiConstant].size());
			ParseTreeNode cur = descendants[isMultiConstant].get(choice);
			ParseTreeNode sub = GrammarFuzzer.sample(cur.getNode(), this.pair, this.parameters, this.random);
			ParseTreeNode result = ParseTreeUtils.getSubstitute(seed, cur, sub);
			return result;
		}
		
		private ParseTreeNode sample(ParseTreeNode seed) {
			while(true) {
				ParseTreeNode result = sampleHelper(seed);
				if(result.getExample().length() <= this.maxLength) {
					return result;
				}
			}
		}
		
		public String sampleOne(Node node) {
			ParseTreeNode cur = ParseTreeUtils.getParseTree(node);
			int choice = this.random.nextInt(this.numMutations);
			for(int i=0; i<choice; i++) {
				cur = this.sample(cur);
			}
			return cur.getExample();
		}
		
		@Override
		public String sample() {
			Node node = this.pair.getX();
			if(node instanceof MultiAlternationNode) {
				MultiAlternationNode maltNode = (MultiAlternationNode)node;
				List<Node> children = maltNode.getChildren();
				int choice = this.random.nextInt(children.size());
				return this.sampleOne(children.get(choice));
			} else {
				return this.sampleOne(node);
			}
		}
	}
	
	public static class CombinedMutationSampler implements Sampler {
		private final Sampler sampler;
		private final int numMutations;
		private final Random random;
		
		public CombinedMutationSampler(Sampler sampler, int numMutations, Random random) {
			this.sampler = sampler;
			this.numMutations = numMutations;
			this.random = random;
		}
		
		@Override
		public String sample() {
			String sample = this.sampler.sample();
			return new RandomExtra(this.random).nextStringMutant(sample, this.random.nextInt(this.numMutations));
		}
	}
}
