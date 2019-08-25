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
import java.util.List;
import java.util.Map;

import glade.grammar.GrammarUtils.AlternationNode;
import glade.grammar.GrammarUtils.ConstantNode;
import glade.grammar.GrammarUtils.MultiAlternationNode;
import glade.grammar.GrammarUtils.MultiConstantNode;
import glade.grammar.GrammarUtils.Node;
import glade.grammar.GrammarUtils.NodeMerges;
import glade.grammar.GrammarUtils.RepetitionNode;
import glade.grammar.MultiGrammarUtils.MultiGrammar;
import glade.grammar.MultiGrammarUtils.MultiProduction;
import glade.grammar.MultiGrammarUtils.NormalGrammar;
import glade.grammar.MultiGrammarUtils.SymbolTable;
import glade.util.Utils.Pair;

public class GrammarToNormalGrammar {
	public static NormalGrammar transform(Pair<Node,NodeMerges> pair) {
		SymbolTable symbols = new SymbolTable();
		List<MultiProduction> productions = new ArrayList<MultiProduction>();
		transformHelper(pair.getX(), GrammarUtils.getPartition(pair), symbols, productions);
		return new NormalGrammar(new MultiGrammar(productions, symbols.get(pair.getX())));
	}

	private static void transformHelper(Node node, Map<Node,Node> representatives, SymbolTable symbols, List<MultiProduction> productions) {
		int symbol = symbols.get(representatives.get(node));
		if(node instanceof RepetitionNode) {
			RepetitionNode repNode = (RepetitionNode)node;
			int intermediateSymbol = symbols.get();
			productions.add(new MultiProduction(intermediateSymbol, new Object[]{symbols.get(representatives.get(repNode.start))}));
			productions.add(new MultiProduction(intermediateSymbol, new Object[]{intermediateSymbol, symbols.get(representatives.get(repNode.rep))}));
			productions.add(new MultiProduction(symbol, new Object[]{intermediateSymbol, symbols.get(representatives.get(repNode.end))}));
			transformHelper(repNode.start, representatives, symbols, productions);
			transformHelper(repNode.rep, representatives, symbols, productions);
			transformHelper(repNode.end, representatives, symbols, productions);
		} else if(node instanceof MultiConstantNode) {
			MultiConstantNode mconstNode = (MultiConstantNode)node;
			Object[] characterSymbols = new Object[mconstNode.characterOptions.size()];
			for(int i=0; i<mconstNode.characterOptions.size(); i++) {
				characterSymbols[i] = symbols.get();
				for(char c : mconstNode.characterOptions.get(i)) {
					productions.add(new MultiProduction(characterSymbols[i], new Object[]{c}));
				}
			}
			productions.add(new MultiProduction(symbol, characterSymbols));
		} else if(node instanceof AlternationNode) {
			AlternationNode altNode = (AlternationNode)node;
			productions.add(new MultiProduction(symbol, new Object[]{symbols.get(representatives.get(altNode.first))}));
			productions.add(new MultiProduction(symbol, new Object[]{symbols.get(representatives.get(altNode.second))}));
			transformHelper(altNode.first, representatives, symbols, productions);
			transformHelper(altNode.second, representatives, symbols, productions);
		} else if(node instanceof ConstantNode) {
			ConstantNode constNode = (ConstantNode)node;
			char[] chars = constNode.getData().example.toCharArray();
			Object[] objs = new Object[chars.length];
			for(int i=0; i<chars.length; i++) {
				objs[i] = chars[i]; // OK since Characters (used here) do not equal Integers (used in the symbol table)
			}
			productions.add(new MultiProduction(symbol, objs));
		} else if(node instanceof MultiAlternationNode) {
			MultiAlternationNode altNode = (MultiAlternationNode)node;
			for(Node child : altNode.getChildren()) {
				productions.add(new MultiProduction(symbol, new Object[]{symbols.get(representatives.get(child))}));
				transformHelper(child, representatives, symbols, productions);
			}
		}
	}
}
