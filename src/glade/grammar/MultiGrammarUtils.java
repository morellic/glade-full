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

package glade.grammar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import glade.util.OracleUtils.DiscriminativeOracle;
import glade.util.Utils.MultivalueMap;

public class MultiGrammarUtils {
	public static class MultiProduction {
		public final Object target;
		public final Object[] inputs;
		public MultiProduction(Object target, Object[] inputs) {
			this.target = target;
			this.inputs = inputs;
		}
	}
	
	public static class MultiGrammar {
		private final List<MultiProduction> productions;
		private final MultivalueMap<Object,MultiProduction> productionsByTarget = new MultivalueMap<Object,MultiProduction>();
		private final Object startSymbol;
		
		public MultiGrammar(List<MultiProduction> productions, Object startSymbol) {
			this.productions = productions;
			for(MultiProduction production : productions) {
				this.productionsByTarget.add(production.target, production);
			}
			this.startSymbol = startSymbol;
		}
		
		public List<MultiProduction> getProductions() {
			return this.productions;
		}
		
		public Object getStartSymbol() {
			return this.startSymbol;
		}

		public Set<MultiProduction> getProductionsByTarget(Object symbol) {
			return this.productionsByTarget.get(symbol);
		}
	}
	
	public static class SymbolTable {
		private final Map<Object,Integer> symbols = new HashMap<Object,Integer>();
		private int cur = 0;
		public int get() {
			return this.cur++;
		}
		public int get(Object t) {
			if(!this.symbols.containsKey(t)) {
				this.symbols.put(t, this.cur++);
			}
			return this.symbols.get(t);
		}
		public int num() {
			return this.cur;
		}
		public boolean contains(Object t) {
			return this.symbols.containsKey(t);
		}
	}
	
	public static interface Production {}
	
	public static class BinaryProduction implements Production {
		public final int target;
		public final int firstInput;
		public final int secondInput;
		private BinaryProduction(int target, int firstInput, int secondInput) {
			this.target = target;
			this.firstInput = firstInput;
			this.secondInput = secondInput;
		}
	}
	
	public static class UnaryProduction implements Production {
		public final int target;
		public final int input;
		private UnaryProduction(int target, int input) {
			this.target = target;
			this.input = input;
		}
	}
	
	public static class EmptyProduction implements Production {
		public final int target;
		private EmptyProduction(int target) {
			this.target = target;
		}
	}
	
	public static class NormalGrammar {
		public final int stopSymbol;
		public final int numSymbols;
		public final Map<Character,Integer> characters = new HashMap<Character,Integer>();
		public final Map<Integer,Character> inverseCharacters = new HashMap<Integer,Character>();
		public final MultivalueMap<Integer,BinaryProduction> binaryProductionsByTarget = new MultivalueMap<Integer,BinaryProduction>();
		public final MultivalueMap<Integer,BinaryProduction> binaryProductionsByFirstInput = new MultivalueMap<Integer,BinaryProduction>();
		public final MultivalueMap<Integer,BinaryProduction> binaryProductionsBySecondInput = new MultivalueMap<Integer,BinaryProduction>();
		public final MultivalueMap<Integer,UnaryProduction> unaryProductionsByTarget = new MultivalueMap<Integer,UnaryProduction>();
		public final MultivalueMap<Integer,UnaryProduction> unaryProductionsByInput = new MultivalueMap<Integer,UnaryProduction>();
		public final MultivalueMap<Integer,EmptyProduction> emptyProductionsByTarget = new MultivalueMap<Integer,EmptyProduction>();
		public NormalGrammar(MultiGrammar multiGrammar) {
			SymbolTable symbols = new SymbolTable();
			for(MultiProduction production : multiGrammar.getProductions()) {
				this.add(production, symbols);
			}
			for(Map.Entry<Object,Integer> entry : symbols.symbols.entrySet()) {
				if(entry.getKey() instanceof Character) {
					this.characters.put((Character)entry.getKey(), entry.getValue());
					this.inverseCharacters.put(entry.getValue(), (Character)entry.getKey());
				}
			}
			this.numSymbols = symbols.num();
			this.stopSymbol = symbols.contains(multiGrammar.getStartSymbol()) ? symbols.get(multiGrammar.getStartSymbol()) : -1;
		}
		private void add(MultiProduction production, SymbolTable symbols) {
			if(production.inputs.length == 0) {
				this.add(new EmptyProduction(symbols.get(production.target)));
			} else if(production.inputs.length == 1) {
				this.add(new UnaryProduction(symbols.get(production.target), symbols.get(production.inputs[0])));
			} else {
				int firstInput = symbols.get(production.inputs[0]);
				int secondInput = symbols.get(production.inputs[1]);
				for(int index=2; index < production.inputs.length; index++) {
					int intermediateTarget = symbols.get();
					this.add(new BinaryProduction(intermediateTarget, firstInput, secondInput));
					firstInput = intermediateTarget;
					secondInput = symbols.get(production.inputs[index]);
				}
				this.add(new BinaryProduction(symbols.get(production.target), firstInput, secondInput));
			}
		}
		private void add(BinaryProduction binaryProduction) {
			this.binaryProductionsByTarget.add(binaryProduction.target, binaryProduction);
			this.binaryProductionsByFirstInput.add(binaryProduction.firstInput, binaryProduction);
			this.binaryProductionsBySecondInput.add(binaryProduction.secondInput, binaryProduction);
		}
		private void add(UnaryProduction unaryProduction) {
			this.unaryProductionsByTarget.add(unaryProduction.target, unaryProduction);
			this.unaryProductionsByInput.add(unaryProduction.input, unaryProduction);
		}
		private void add(EmptyProduction emptyProduction) {
			this.emptyProductionsByTarget.add(emptyProduction.target, emptyProduction);
		}		
		public List<Integer> serialize() {
			List<Integer> binary = new ArrayList<Integer>();
			binary.add(this.numSymbols); // 0
			binary.add(this.stopSymbol); // 1
			binary.add(this.characters.size()); // 2
			for(char c : this.characters.keySet()) {
				binary.add((int)c); // 3
				binary.add(this.characters.get(c)); // 4
			}
			for(int i=0; i<this.numSymbols; i++) {
				binary.add(this.emptyProductionsByTarget.get(i).size()); // 5
				for(EmptyProduction emptyProduction : this.emptyProductionsByTarget.get(i)) {
					binary.add(emptyProduction.target); // 6
				}
				binary.add(this.unaryProductionsByTarget.get(i).size()); // 7
				for(UnaryProduction unaryProduction : this.unaryProductionsByTarget.get(i)) {
					binary.add(unaryProduction.target); // 8
					binary.add(unaryProduction.input); // 9
				}
				binary.add(this.binaryProductionsByTarget.get(i).size()); // 10
				for(BinaryProduction binaryProduction : this.binaryProductionsByTarget.get(i)) {
					binary.add(binaryProduction.target); // 11
					binary.add(binaryProduction.firstInput); // 12
					binary.add(binaryProduction.secondInput); // 13
				}
			}
			return binary;
		}
		public NormalGrammar(List<Integer> binary) {
			int cur = 0;
			this.numSymbols = binary.get(cur++); // 0
			this.stopSymbol = binary.get(cur++); // 1
			int numCharacters = binary.get(cur++); // 2
			for(int i=0; i<numCharacters; i++) {
				char c = (char)(int)binary.get(cur++); // 3
				int symbol = binary.get(cur++); // 4
				this.characters.put(c, symbol);
			}
			for(int i=0; i<this.numSymbols; i++) {
				int numEmpty = binary.get(cur++); // 5
				for(int j=0; j<numEmpty; j++) {
					int target = binary.get(cur++); // 6
					this.add(new EmptyProduction(target));
				}
				int numUnary = binary.get(cur++); // 7
				for(int j=0; j<numUnary; j++) {
					int target = binary.get(cur++); // 8
					int input = binary.get(cur++); // 9
					this.add(new UnaryProduction(target, input));
				}
				int numBinary = binary.get(cur++); // 10
				for(int j=0; j<numBinary; j++) {
					int target = binary.get(cur++); // 11
					int firstInput = binary.get(cur++); // 12
					int secondInput = binary.get(cur++); // 13
					this.add(new BinaryProduction(target, firstInput, secondInput));
				}
			}
		}
	}
	
	private static class Edge {
		private final int start;
		private final int end;
		private final int symbol;
		private Edge(int start, int end, int symbol) {
			this.start = start;
			this.end = end;
			this.symbol = symbol;
		}
		@Override
		public int hashCode() {
			return this.start + 37*(this.end + 37*this.symbol);
		}
		@Override
		public boolean equals(Object obj) {
			Edge other = (Edge)obj;
			return this.start == other.start && this.end == other.end && this.symbol == other.symbol;
		}
	}
	
	public static class Solver {
		private Set<Edge> edges;
		private LinkedList<Edge> worklist;
		private LinkedList<Edge>[][] outgoingEdges;
		private LinkedList<Edge>[][] incomingEdges;
		
		@SuppressWarnings("unchecked")
		public boolean solve(NormalGrammar grammar, char[] string) {
			// setup
			if(grammar.stopSymbol == -1) {
				return false;
			}
			this.edges = new HashSet<Edge>();
			this.worklist = new LinkedList<Edge>();
			this.outgoingEdges = new LinkedList[string.length+1][grammar.numSymbols];
			this.incomingEdges = new LinkedList[string.length+1][grammar.numSymbols];
			for(int i=0; i<string.length+1; i++) {
				for(int j=0; j<grammar.numSymbols; j++) {
					this.outgoingEdges[i][j] = new LinkedList<Edge>();
					this.incomingEdges[i][j] = new LinkedList<Edge>();
				}
			}
			
			// initial edges
			for(int i=0; i<string.length; i++) {
				if(!grammar.characters.containsKey(string[i])) {
					return false;
				}
				this.add(new Edge(i, i+1, grammar.characters.get(string[i])));
			}
			
			// empty productions
			for(int i=0; i<string.length+1; i++) {
				for(int j : grammar.emptyProductionsByTarget.keySet()) {
					this.add(new Edge(i, i, j));
				}
			}
			
			// processing
			while(!this.worklist.isEmpty()) {
				Edge edge = this.worklist.removeFirst();
				for(BinaryProduction bp : grammar.binaryProductionsByFirstInput.get(edge.symbol)) {
					for(Edge secondInput : this.outgoingEdges[edge.end][bp.secondInput]) {
						this.add(new Edge(edge.start, secondInput.end, bp.target));
					}
				}
				for(BinaryProduction bp : grammar.binaryProductionsBySecondInput.get(edge.symbol)) {
					for(Edge firstInput : this.incomingEdges[edge.start][bp.firstInput]) {
						this.add(new Edge(firstInput.start, edge.end, bp.target));
					}
				}
				for(UnaryProduction up : grammar.unaryProductionsByInput.get(edge.symbol)) {
					this.add(new Edge(edge.start, edge.end, up.target));
				}
			}
			
			// result
			return this.edges.contains(new Edge(0, string.length, grammar.stopSymbol));
		}
		
		private void add(Edge edge) {
			if(!this.edges.contains(edge)) {
				this.outgoingEdges[edge.start][edge.symbol].add(edge);
				this.incomingEdges[edge.end][edge.symbol].add(edge);
				this.edges.add(edge);
				this.worklist.add(edge);
			}
		}
	}
	
	public static class NormalGrammarOracle implements DiscriminativeOracle {
		private final NormalGrammar grammar;
		
		public NormalGrammarOracle(NormalGrammar grammar) {
			this.grammar = grammar;
		}
		
		@Override
		public boolean query(String query) {
			return new Solver().solve(this.grammar, query.toCharArray());
		}
	}
}
