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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dk.brics.automaton.Automaton;
import glade.automaton.AutomatonUtils.LStarLearner;
import glade.automaton.AutomatonUtils.RPNILearner;
import glade.constants.Settings.LearnerSettings;
import glade.grammar.GrammarSerializer;
import glade.grammar.GrammarUtils.Node;
import glade.grammar.GrammarUtils.NodeMerges;
import glade.grammar.synthesize.GrammarSynthesis;
import glade.main.RunSynthetic.DiscriminativeAutomatonOracleLearner;
import glade.main.RunSynthetic.InteractiveAutomatonOracleLearner;
import glade.program.ProgramDataUtils.ProgramData;
import glade.program.ProgramDataUtils.ProgramExamples;
import glade.util.RandomUtils.RandomExtra;
import glade.util.Utils.Pair;

public class LearnerDataUtils {
	public static class LearnerDataParameters {
		public final String grammarPath;
		public final String aflQueuePath;
		public final String rpniPath;
		public final String lstarPath;
		public LearnerDataParameters(String grammarPath, String aflQueuePath, String rpniPath, String lstarPath) {
			this.grammarPath = grammarPath;
			this.aflQueuePath = aflQueuePath;
			this.rpniPath = rpniPath;
			this.lstarPath = lstarPath;
		}
		public String getAutomatonPath(AutomatonLearner learner) {
			switch(learner) {
			case RPNI:
				return this.rpniPath;
			case LSTAR:
				return this.lstarPath;
			default:
				throw new RuntimeException();
			}
		}
	}
	
	public static void clearGrammarDirectory(LearnerDataParameters learnerData, String name) {
		File dir = new File(learnerData.grammarPath + File.separator + name);
		if(dir.exists()) {
			for(File file : dir.listFiles()) {
				file.delete();
			}
		}
	}
	
	private static String getGrammarFilename(LearnerDataParameters learnerData, String name, int index) {
		return learnerData.grammarPath + File.separator + name + File.separator + "example" + index + ".gram";
	}
	
	private static String getAllGrammarFilename(LearnerDataParameters learnerData, String name) {
		return learnerData.grammarPath + File.separator + name + File.separator + "all.gram";
	}
	
	public static void saveGrammar(String filename, Pair<Node,NodeMerges> pair) {
		try {
			File file = new File(filename);
			File parent = file.getParentFile();
			if(parent != null) {
				parent.mkdirs();
			}
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(filename));
			GrammarSerializer.serialize(pair.getX(), pair.getY(), dos);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Pair<Node,NodeMerges> loadGrammar(String filename) {
		try {
			DataInputStream dis = new DataInputStream(new FileInputStream(filename));
			return GrammarSerializer.deserializeNodeWithMerges(dis);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void saveGrammar(LearnerDataParameters learnerData, String name, int index, Pair<Node,NodeMerges> pair) {
		saveGrammar(getGrammarFilename(learnerData, name, index), pair);
	}
	
	public static void saveAllGrammar(LearnerDataParameters learnerData, String name, Pair<Node,NodeMerges> pair) {
		saveGrammar(getAllGrammarFilename(learnerData, name), pair);
	}
	
	public static Pair<Node,NodeMerges> loadGrammar(LearnerDataParameters learnerData, String name, int index) {
		return loadGrammar(getGrammarFilename(learnerData, name, index));
	}
	
	public static Pair<Node,NodeMerges> loadAllGrammar(LearnerDataParameters learnerData, String name) {
		return loadGrammar(getAllGrammarFilename(learnerData, name));
	}

	public static void learnGrammar(LearnerDataParameters learnerData, String name, ProgramData data, ProgramExamples examples, boolean ignoreErrors, boolean useCharacterClasses, int index) {
		String example = examples.getTrainExamples().get(index);
		Pair<Node,NodeMerges> pair = (data.getQueryOracle().query(example) || !ignoreErrors) ? GrammarSynthesis.getGrammarSingle(example, data.getQueryOracle(), true, useCharacterClasses) : GrammarSynthesis.getGrammarConstant(example);
		saveGrammar(learnerData, name, index, pair);
	}
	
	public static void mergeGrammar(LearnerDataParameters learnerData, String name, ProgramData data, ProgramExamples examples) {
		List<Node> roots = new ArrayList<Node>();
		for(int i=0; i<examples.getTrainExamples().size(); i++) {
			roots.add(LearnerDataUtils.loadGrammar(learnerData, name, i).getX());
		}
		Pair<Node,NodeMerges> pair = GrammarSynthesis.getGrammarMultipleFromRoots(roots, data.getQueryOracle());
		saveAllGrammar(learnerData, name, pair);
	}
	
	public static void learnAllGrammar(LearnerDataParameters learnerData, String name, ProgramData data, ProgramExamples examples, boolean ignoreErrors, boolean useCharacterClasses) {
		clearGrammarDirectory(learnerData, name);
		for(int i=0; i<examples.getTrainExamples().size(); i++) {
			learnGrammar(learnerData, name, data, examples, ignoreErrors, useCharacterClasses, i);
		}
		mergeGrammar(learnerData, name, data, examples);
	}
	
	public static void clearAflQueueDirectory(LearnerDataParameters learnerData, String name) {
		File dir = new File(learnerData.aflQueuePath + File.separator + name);
		if(dir.exists()) {
			for(File file : dir.listFiles()) {
				file.delete();
			}
		}
	}
	
	private static String getAflQueueFilename(LearnerDataParameters learnerData, String name, int index) {
		return learnerData.aflQueuePath + File.separator + name + File.separator + "example" + index + ".gram";
	}
	
	public static void saveAflQueue(String filename, List<String> queue) {
		try {
			File file = new File(filename);
			File parent = file.getParentFile();
			if(parent != null) {
				parent.mkdirs();
			}
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(filename));
			dos.writeInt(queue.size());
			for(String sample : queue) {
				dos.writeUTF(sample);
			}
			dos.close();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static List<String> loadAflQueue(String filename) {
		try {
			List<String> queue = new ArrayList<String>();
			DataInputStream dis = new DataInputStream(new FileInputStream(filename));
			int len = dis.readInt();
			for(int i=0; i<len; i++) {
				queue.add(dis.readUTF());
			}
			dis.close();
			return queue;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void saveAflQueue(LearnerDataParameters learnerData, String name, int index, List<String> queue) {
		saveAflQueue(getAflQueueFilename(learnerData, name, index), queue);
	}
	
	public static List<String> loadAflQueue(LearnerDataParameters learnerData, String name, int index) {
		return loadAflQueue(getAflQueueFilename(learnerData, name, index));
	}
	
	public static List<String> loadAflQueueAll(LearnerDataParameters learnerData, ProgramExamples examples, String name, int numIters) {
		List<String> queue = new ArrayList<String>();
		int sampleCounter = 0;
		int maxSamplesPerExample = (int)Math.ceil((double)numIters/examples.getTrainExamples().size());
		for(int i=0; i<examples.getTrainExamples().size(); i++) {
			List<String> curQueue = loadAflQueue(learnerData, name, i);
			for(int j=0; j<maxSamplesPerExample; j++) {
				queue.add(curQueue.get(j));
				if(++sampleCounter == numIters) {
					return queue;
				}
			}
		}
		throw new RuntimeException("Insufficient samples!");
	}

	public static void buildAflQueue(LearnerDataParameters learnerData, String name, ProgramData data, ProgramExamples examples, int numIters, int index) {
		List<String> queue = data.getAflOrigOracle().executeForQueue(examples.getTrainExamples().get(index), numIters/examples.getTrainExamples().size()).getX();
		saveAflQueue(learnerData, name, index, queue);
	}
	
	public static void buildAflQueueAll(LearnerDataParameters learnerData, String name, ProgramData data, ProgramExamples examples, int numIters) {
		for(int i=0; i<examples.getTrainExamples().size(); i++) {
			buildAflQueue(learnerData, name, data, examples, numIters, i);
		}
	}
	
	private static enum AutomatonLearner {
		RPNI, LSTAR
	}
	
	private static void clearDirectory(LearnerDataParameters learnerData, String name, AutomatonLearner learner) {
		String autPath = learnerData.getAutomatonPath(learner);
		File dir = new File(autPath + File.separator + name);
		if(dir.exists()) {
			for(File file : dir.listFiles()) {
				file.delete();
			}
		}
	}
	
	private static String getAutomatonFilename(LearnerDataParameters learnerData, String name, AutomatonLearner learner) {
		return learnerData.getAutomatonPath(learner) + File.separator + name + File.separator + "all.aut";
	}
	
	private static void saveAutomaton(String filename, Automaton aut) {
		try {
			File file = new File(filename);
			File parent = file.getParentFile();
			if(parent != null) {
				parent.mkdirs();
			}
			aut.store(new FileOutputStream(filename));
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Automaton loadAutomaton(String filename) {
		try {
			return Automaton.load(new FileInputStream(filename));
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void saveAutomaton(LearnerDataParameters learnerData, String name, AutomatonLearner learner, Automaton aut) {
		saveAutomaton(getAutomatonFilename(learnerData, name, learner), aut);
	}
	
	private static Automaton loadAutomaton(LearnerDataParameters learnerData, String name, AutomatonLearner learner) {
		return loadAutomaton(getAutomatonFilename(learnerData, name, learner));
	}
	
	public static void learnRpni(LearnerDataParameters learnerData, LearnerSettings learner, String name, ProgramData data, ProgramExamples examples, Random random) {
		List<String> samples = new ArrayList<String>();
		samples.addAll(examples.getTrainExamples());
		samples.addAll(new RandomExtra(random).nextUniformLengthStrings(learner.maxLength, learner.numSamples));
		Automaton aut = new DiscriminativeAutomatonOracleLearner(new RPNILearner(), random).learn(samples, data.getQueryOracle()).getAutomaton();
		saveAutomaton(learnerData, name, AutomatonLearner.RPNI, aut);
	}
	
	public static Automaton loadRpniAutomaton(LearnerDataParameters learnerData, String name) {
		return loadAutomaton(learnerData, name, AutomatonLearner.RPNI);
	}
	
	public static void clearRpniDirectory(LearnerDataParameters learnerData, String name) {
		clearDirectory(learnerData, name, AutomatonLearner.RPNI);
	}
	
	public static void learnLstar(LearnerDataParameters learnerData, LearnerSettings learner, String name, ProgramData data, ProgramExamples examples, Random random) {
		Automaton aut = new InteractiveAutomatonOracleLearner(new LStarLearner(), learner.numSamples, learner.maxLength, random).learn(examples.getTrainExamples(), data.getQueryOracle()).getAutomaton();
		saveAutomaton(learnerData, name, AutomatonLearner.LSTAR, aut);
	}
	
	public static Automaton loadLstarAutomaton(LearnerDataParameters learnerData, String name) {
		return loadAutomaton(learnerData, name, AutomatonLearner.LSTAR);
	}
	
	public static void clearnLstarDirectory(LearnerDataParameters learnerData, String name) {
		clearDirectory(learnerData, name, AutomatonLearner.LSTAR);
	}
}
