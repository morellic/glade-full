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

package glade.constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import glade.grammar.MultiGrammarUtils.MultiGrammar;
import glade.grammar.MultiGrammarUtils.MultiProduction;
import glade.util.CharacterUtils;
import glade.util.Utils.Pair;

public class SyntheticGrammarFiles {
	public static String GREP_GRAMMAR = "data/handwritten/grep.gram";
	public static String GREPLIM_GRAMMAR = "data/handwritten/greplim.gram";
	public static String URL_GRAMMAR = "data/handwritten/url.gram";
	public static String NUM_GRAMMAR = "data/handwritten/num.gram";
	
	// lower = [a-z]
	// upper = [A-Z]
	// alpha = [a-zA-Z]
	// num = [1-9]
	// numall = [0-9]
	// alphanum = [a-zA-Z0-9]
	// nonalphanum = [^a-zA-Z0-9]
	// ascii = ...
	// s = ' '
	// t = '\t'
	// n = '\n'
	// p = '|'
	// e = ''
	
	private static List<MultiProduction> defaultProductions = new ArrayList<MultiProduction>();
	static {
		for(char c : CharacterUtils.getAlphaLowerCaseCharacters()) {
			defaultProductions.add(new MultiProduction("lower", new Object[]{(Character)c}));
			defaultProductions.add(new MultiProduction("alpha", new Object[]{(Character)c}));
			defaultProductions.add(new MultiProduction("alphanum", new Object[]{(Character)c}));
		}
		for(char c : CharacterUtils.getAlphaUpperCaseCharacters()) {
			defaultProductions.add(new MultiProduction("upper", new Object[]{(Character)c}));
			defaultProductions.add(new MultiProduction("alpha", new Object[]{(Character)c}));
			defaultProductions.add(new MultiProduction("alphanum", new Object[]{(Character)c}));
		}
		for(char c : CharacterUtils.getNumericCharacters()) {
			if(c != '0') {
				defaultProductions.add(new MultiProduction("num", new Object[]{(Character)c}));
			}
			defaultProductions.add(new MultiProduction("numall", new Object[]{(Character)c}));
			defaultProductions.add(new MultiProduction("alphanum", new Object[]{(Character)c}));
		}
		for(char c : CharacterUtils.getNonAlphaNumericCharacters()) {
			defaultProductions.add(new MultiProduction("nonalphanum", new Object[]{(Character)c}));
		}
		for(char c : CharacterUtils.getAsciiCharacters()) {
			defaultProductions.add(new MultiProduction("ascii", new Object[]{(Character)c}));
		}
		defaultProductions.add(new MultiProduction("s", new Object[]{(Character)' '}));
		defaultProductions.add(new MultiProduction("t", new Object[]{(Character)'\t'}));
		defaultProductions.add(new MultiProduction("n", new Object[]{(Character)'\n'}));
		defaultProductions.add(new MultiProduction("p", new Object[]{'|'}));
		defaultProductions.add(new MultiProduction("e", new Object[]{}));
	}
	public static List<MultiProduction> getDefaultProductions() {
		return defaultProductions;
	}
	
	private static boolean isEqual(MultiProduction p1, MultiProduction p2) {
		if(!p1.target.equals(p2.target) || p1.inputs.length != p2.inputs.length) {
			return false;
		}
		for(int i=0; i<p1.inputs.length; i++) {
			if(!p1.inputs[i].equals(p2.inputs[i])) {
				return false;
			}
		}
		return true;
	}
	private static boolean isDefaultProduction(MultiProduction production) {
		for(MultiProduction defaultProduction : defaultProductions) {
			if(isEqual(production, defaultProduction)) {
				return true;
			}
		}
		return false;
	}
	
	private static String toString(MultiProduction production) {
		StringBuilder sb = new StringBuilder();
		sb.append(production.target.toString()).append(" ::=");
		if(production.inputs.length == 0) {
			sb.append(" e");
		} else {
			for(int i=0; i<production.inputs.length; i++) {
				if(production.inputs[i] instanceof Character) {
					sb.append(" '").append(production.inputs[i].toString()).append("'");
				} else {
					sb.append(" ").append(production.inputs[i].toString());
				}
			}
		}
		return sb.toString();
	}
	
	private static String toString(MultiGrammar grammar) {
		StringBuilder sb = new StringBuilder();
		sb.append("@").append(grammar.getStartSymbol().toString()).append("\n");
		for(MultiProduction production : grammar.getProductions()) {
			if(isDefaultProduction(production)) {
				continue;
			}
			sb.append(toString(production)).append("\n");
		}
		return sb.toString();
	}
	
	public static Pair<String,MultiGrammar> loadGrammar(String filename) {
		return loadGrammar(new File(filename));
	}
	
	public static Pair<String,MultiGrammar> loadGrammar(File file) {
		try {
			List<MultiProduction> productions = new ArrayList<MultiProduction>(getDefaultProductions());
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			String name = null;
			while((line = br.readLine()) != null) {
				if(line.startsWith("#")) {
					continue;
				}
				if(line.startsWith("@")) {
					name = line.substring(1);
					continue;
				}
				if(line.trim().isEmpty()) {
					continue;
				}
				String[] tokens = line.split("::=");
				if(tokens.length != 2) {
					throw new RuntimeException();
				}
				String target = tokens[0].trim();
				for(String production : tokens[1].trim().split("\\|")) {
					String[] strInputs = production.trim().split("\\s+");
					Object[] inputs = new Object[strInputs.length];
					for(int i=0; i<strInputs.length; i++) {
						String strInput = strInputs[i].trim();
						if(strInput.startsWith("'")) {
							if(!strInput.endsWith("'")) {
								throw new RuntimeException();
							}
							if(strInput.length() != 3) {
								throw new RuntimeException();
							}
							inputs[i] = (Character)strInput.charAt(1);
						} else {
							inputs[i] = strInput;
						}
					}
					productions.add(new MultiProduction(target, inputs));
				}
			}
			br.close();
			if(name == null) {
				throw new RuntimeException();
			}
			return new Pair<String,MultiGrammar>(name, new MultiGrammar(productions, name));
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void saveGrammar(MultiGrammar grammar, String filename) {
		saveGrammar(grammar, new File(filename));
	}
	
	public static void saveGrammar(MultiGrammar grammar, File file) {
		try {
			PrintWriter pw = new PrintWriter(new FileWriter(file));
			pw.println(toString(grammar));
			pw.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
