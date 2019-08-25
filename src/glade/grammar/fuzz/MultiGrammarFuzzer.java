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
import java.util.List;
import java.util.Random;

import glade.grammar.MultiGrammarUtils.MultiGrammar;
import glade.grammar.MultiGrammarUtils.MultiProduction;
import glade.grammar.fuzz.GrammarFuzzer.IntBox;
import glade.grammar.fuzz.GrammarTraceFuzzer.StructuredExample;
import glade.util.IteratorUtils.Sampler;

public class MultiGrammarFuzzer {
	public static interface MultiGrammarParseTreeNode extends StructuredExample {
		public abstract List<MultiGrammarParseTreeNode> getChildren();
		public abstract Object getSymbol();
	}
	
	public static class MultiGrammarInternalNode implements MultiGrammarParseTreeNode {
		private final Object symbol;
		private final String example;
		private final List<MultiGrammarParseTreeNode> children;
		public MultiGrammarInternalNode(Object symbol, String example, List<MultiGrammarParseTreeNode> children) {
			this.symbol = symbol;
			this.example = example;
			this.children = children;
		}
		public List<MultiGrammarParseTreeNode> getChildren() {
			return this.children;
		}
		public Object getSymbol() {
			return this.symbol;
		}
		public String getExample() {
			return this.example;
		}
	}
	
	public static class MultiGrammarLeafNode implements MultiGrammarParseTreeNode {
		private final Object symbol;
		private final String example;
		public MultiGrammarLeafNode(Object symbol, String example) {
			this.symbol = symbol;
			this.example = example;
		}
		public List<MultiGrammarParseTreeNode> getChildren() {
			return new ArrayList<MultiGrammarParseTreeNode>();
		}
		public Object getSymbol() {
			return this.symbol;
		}
		public String getExample() {
			return this.example;
		}
	}
	
	private static MultiGrammarParseTreeNode sampleHelper(MultiGrammar grammar, Object symbol, IntBox box, Random random) {
		if(box.value() < 0) {
			throw new RuntimeException();
		}
		box.decrement();
		
		// STEP 0: Handle base case
		if(symbol instanceof Character) {
			return new MultiGrammarLeafNode(symbol, "" + (Character)symbol);
		}
		
		// STEP 1: Get random production
		List<MultiProduction> productions = new ArrayList<MultiProduction>(grammar.getProductionsByTarget(symbol));
		int choice = random.nextInt(productions.size());
		MultiProduction production = productions.get(choice);
		
		// STEP 2: Sample children
		List<MultiGrammarParseTreeNode> children = new ArrayList<MultiGrammarParseTreeNode>();
		StringBuilder sb = new StringBuilder();
		for(Object child : production.inputs) {
			MultiGrammarParseTreeNode node = sampleHelper(grammar, child, box, random);
			sb.append(node.getExample());
			children.add(node);
		}
		return new MultiGrammarInternalNode(symbol, sb.toString(), children);
	}
	
	public static MultiGrammarParseTreeNode sample(MultiGrammar grammar, int boxSize, Random random) {
		try {
			return sampleHelper(grammar, grammar.getStartSymbol(), new IntBox(boxSize), random);
		} catch(Exception e) {
			return sample(grammar, boxSize, random);
		}
	}
	
	public static class MultiGrammarSampler implements Sampler {
		private final MultiGrammar grammar;
		private final int boxSize;
		private final Random random;
		
		public MultiGrammarSampler(MultiGrammar grammar, int boxSize, Random random) {
			this.grammar = grammar;
			this.boxSize = boxSize;
			this.random = random;
		}
		
		@Override
		public String sample() {
			return MultiGrammarFuzzer.sample(this.grammar, this.boxSize, this.random).getExample();
		}
	}
}
