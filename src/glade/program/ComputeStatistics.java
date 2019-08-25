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

package glade.program;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import glade.constants.program.XmlData;
import glade.grammar.GrammarToNormalGrammar;
import glade.grammar.GrammarUtils;
import glade.grammar.GrammarUtils.Node;
import glade.grammar.GrammarUtils.NodeMerges;
import glade.grammar.MultiGrammarUtils.MultiGrammar;
import glade.grammar.MultiGrammarUtils.NormalGrammar;
import glade.grammar.MultiGrammarUtils.Solver;
import glade.grammar.fuzz.GrammarFuzzer;
import glade.grammar.fuzz.GrammarFuzzer.SampleParameters;
import glade.program.ProgramDataUtils.ProgramData;
import glade.program.ProgramDataUtils.ProgramExamples;
import glade.program.ProgramOracleUtils.CoverInfo;
import glade.util.Log;
import glade.util.OracleUtils.DiscriminativeOracle;
import glade.util.Utils.Pair;
import glade.util.Utils.Triple;

public class ComputeStatistics {
	public static int getGrammarSize(Pair<Node,NodeMerges> pair) {
		return GrammarUtils.getAllNodes(pair.getX()).size();
	}
	
	public static Triple<Integer,Integer,Integer> getMultiGrammarSize(MultiGrammar grammar) {
		NormalGrammar normal = new NormalGrammar(grammar);
		int counter = 0;
		for(int target : normal.binaryProductionsByTarget.keySet()) {
			counter += normal.binaryProductionsByTarget.get(target).size();
		}
		for(int target : normal.unaryProductionsByTarget.keySet()) {
			counter += normal.unaryProductionsByTarget.get(target).size();
		}
		for(int target : normal.emptyProductionsByTarget.keySet()) {
			counter += normal.emptyProductionsByTarget.get(target).size();
		}
		return new Triple<Integer,Integer,Integer>(normal.characters.size(), (normal.numSymbols - normal.characters.size()), counter);
	}
	
	public static int getCoverableLinesOfCode(ProgramData data, ProgramExamples examples) {
		Map<Pair<String,Integer>,CoverInfo> fullCoverage = ProgramOracleUtils.getFullCoverage(data.getCoverageOracle(), examples.getEmptyExamples());
		int counter = 0;
		for(CoverInfo info : fullCoverage.values()) {
			if(info.isCoverable()) {
				counter++;
			}
		}
		return counter;
	}
	
	public static int getExampleLinesOfCode(DiscriminativeOracle oracle, Iterable<String> examples) {	
		int counter = 0;
		for(String example : examples) {
			if(oracle.query(example)) {
				counter += example.trim().split("\n").length;
			}
		}
		return counter;
	}
	
	public static double getFalseNegativeRate(Pair<Node,NodeMerges> pair, Iterable<String> samples, int maxLength) {
		NormalGrammar grammar = GrammarToNormalGrammar.transform(pair);
		int count = 0;
		int totalCount = 0;
		for(String sample : samples) {
			if(sample.length() <= maxLength) {
				if(!(new Solver().solve(grammar, sample.toCharArray()))) {
					Log.info(sample);
					count++;
				}
				totalCount++;
			}
		}
		return (double)count/totalCount;
	}
	
	public static double getFalsePositiveRate(Pair<Node,NodeMerges> pair, DiscriminativeOracle oracle, SampleParameters parameters, int numSamples, Random random) throws Exception {
		int count = 0;
		for(int i=0; i<numSamples; i++) {
			String sample = GrammarFuzzer.sample(pair.getX(), pair, parameters, random).getExample();
			if(!oracle.query(sample)) {
				count++;
			}
		}
		return (double)count/numSamples;
	}
	
	public static List<String> getXmlCheckErrors(Pair<Node,NodeMerges> pair) {
		List<String> errors = new ArrayList<String>();
		NormalGrammar grammar = GrammarToNormalGrammar.transform(pair);
		for(String sample : XmlData.getXmlChecks()) {
			if(new Solver().solve(grammar, sample.toCharArray()) != XmlData.XML_DATA.getQueryOracle().query(sample)) {
				errors.add(sample);
			}
		}
		return errors;
	}
}
