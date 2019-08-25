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

package glade.grammar.synthesize;

import java.util.ArrayList;
import java.util.List;

import glade.grammar.GrammarUtils.ConstantNode;
import glade.grammar.GrammarUtils.Context;
import glade.grammar.GrammarUtils.MultiAlternationNode;
import glade.grammar.GrammarUtils.MultiConstantNode;
import glade.grammar.GrammarUtils.Node;
import glade.grammar.GrammarUtils.NodeData;
import glade.grammar.GrammarUtils.NodeMerges;
import glade.grammar.GrammarUtils.RepetitionNode;
import glade.util.Log;
import glade.util.OracleUtils.DiscriminativeOracle;
import glade.util.Utils.Maybe;
import glade.util.Utils.Pair;

public class GrammarSynthesis {
	public static Pair<Node,NodeMerges> getGrammarConstant(String example) {
		return new Pair<Node,NodeMerges>(new ConstantNode(new NodeData(example, new Context())), new NodeMerges());
	}
	
	private static Node getNode(String example, DiscriminativeOracle oracle, boolean useConstantTransformer, boolean useCharacterClasses) {
		return GrammarTransformer.getTransform(RegexSynthesis.getNode(example, oracle), oracle, useConstantTransformer, useCharacterClasses);
	}
	
	public static Pair<Node,NodeMerges> getGrammarSingle(String example, DiscriminativeOracle oracle, boolean useConstantTransformer, boolean useCharacterClasses) {
		long time = System.currentTimeMillis();
		if(!oracle.query(example)) {
			throw new RuntimeException("Invalid example: " + example);
		}
		Node node = getNode(example, oracle, useConstantTransformer, useCharacterClasses);
		Log.info("SINGLE REGEX TIME: " + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();
		Pair<Node,NodeMerges> pair = new Pair<Node,NodeMerges>(node, MergesSynthesis.getMergesSingle(node, node, oracle));
		Log.info("SINGLE MERGE TIME: " + (System.currentTimeMillis() - time));
		return pair;
	}
	
	public static Pair<Node,NodeMerges> getGrammarMultipleFromRoots(List<Node> roots, DiscriminativeOracle oracle) {
		long time = System.currentTimeMillis();
		Pair<Node,NodeMerges> pair = new Pair<Node,NodeMerges>(new MultiAlternationNode(new NodeData(null, new Context()), roots), MergesSynthesis.getMergesMultiple(roots, oracle));
		Log.info("MULTIPLE MERGE TIME: " + (System.currentTimeMillis() - time));
		return pair;
	}
	
	public static Pair<Node,NodeMerges> getGrammarMultiple(List<String> examples, DiscriminativeOracle oracle, boolean useConstantTransformer, boolean useCharacterClasses) {
		List<Node> roots = new ArrayList<Node>();
		for(String example : examples) {
			roots.add(getNode(example, oracle, useConstantTransformer, useCharacterClasses));
		}
		return getGrammarMultipleFromRoots(roots, oracle);
	}
	
	public static Pair<Node,NodeMerges> getRegularGrammarMultipleFromRoots(List<Node> roots, DiscriminativeOracle oracle) {
		long time = System.currentTimeMillis();
		Pair<Node,NodeMerges> pair = new Pair<Node,NodeMerges>(new MultiAlternationNode(new NodeData(null, new Context()), roots), new NodeMerges());
		Log.info("MULTIPLE MERGE TIME: " + (System.currentTimeMillis() - time));
		return pair;
	}
	
	public static Pair<Node,NodeMerges> getRegularGrammarMultiple(List<String> examples, DiscriminativeOracle oracle, boolean useConstantTransformer, boolean useCharacterClasses) {
		List<Node> roots = new ArrayList<Node>();
		for(String example : examples) {
			roots.add(getNode(example, oracle, useConstantTransformer, useCharacterClasses));
		}
		return getRegularGrammarMultipleFromRoots(roots, oracle);
	}
	
	public static boolean getCheck(DiscriminativeOracle oracle, Context context, Iterable<String> examples) {
		for(String example : examples) {
			if(!oracle.query(context.pre + example + context.post) || (context.useExtra() && !oracle.query(context.extraPre + example + context.extraPost))) {
				return false;
			}
		}
		return true;
	}
	
	public static Maybe<List<Node>> getMultiAlternationRepetitionConstantChildren(Node node, boolean isParentRep) {
		if(!isParentRep) {
			return new Maybe<List<Node>>();
		}
		if(!(node instanceof MultiAlternationNode)) {
			return new Maybe<List<Node>>();
		}
		List<Node> constantChildren = new ArrayList<Node>();
		for(Node child : node.getChildren()) {
			if(child instanceof RepetitionNode) {
				RepetitionNode repChild = (RepetitionNode)child;
				if(!(repChild.start instanceof ConstantNode) && !(repChild.start instanceof MultiConstantNode)) {
					return new Maybe<List<Node>>();
				}
				if(!(repChild.rep instanceof ConstantNode) && !(repChild.rep instanceof MultiConstantNode)) {
					return new Maybe<List<Node>>();
				}
				if(!(repChild.end instanceof ConstantNode) && !(repChild.end instanceof MultiConstantNode)) {
					return new Maybe<List<Node>>();
				}
				if(!repChild.start.getData().example.equals("") || !repChild.end.getData().example.equals("")) {
					return new Maybe<List<Node>>();
				}
				constantChildren.add(repChild.rep);
			} else if(child instanceof ConstantNode) {
				constantChildren.add((ConstantNode)child);
			} else if(child instanceof MultiConstantNode) {
				constantChildren.add((MultiConstantNode)child);
			} else {
				return new Maybe<List<Node>>();
			}
		}
		return new Maybe<List<Node>>(constantChildren);
	}
}
