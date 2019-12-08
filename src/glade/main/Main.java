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

package glade.main;

import java.util.Random;

import glade.automaton.AutomatonUtils;
import glade.constants.Files;
import glade.constants.Settings.CompareSettings;
import glade.constants.Settings.FuzzSettings;
import glade.constants.Settings.Fuzzer;
import glade.constants.Settings.LearnerDataSettings;
import glade.constants.Settings.LearnerSettings;
import glade.constants.Settings.LongRunningSettings;
import glade.constants.Settings.Processor;
import glade.constants.Settings.Program;
import glade.constants.Settings.ProgramSettings;
import glade.constants.Settings.SyntheticGrammar;
import glade.constants.Settings.SyntheticLearner;
import glade.constants.Settings.SyntheticSettings;
import glade.constants.program.XmlData;
import glade.extensions.Extensions.CustomGrammar;
import glade.grammar.fuzz.GrammarFuzzer.SampleParameters;
import glade.grammar.fuzz.GrammarTraceFuzzer.MCMCAcceptor;
import glade.main.XmlFuzzer.FuzzParameters;
import glade.program.ComputeStatistics;
import glade.program.LearnerDataUtils;
import glade.util.Log;
import glade.util.Utils;
import glade.util.OracleUtils.HybridOracle;
import glade.util.Utils.Callback;

public class Main {
	public static SyntheticSettings getDefaultSyntheticSettings(int numSamples) {
		return new SyntheticSettings(numSamples, 1000, 100, 500);
	}
	
	public static SyntheticSettings getDefaultSyntheticSettings() {
		return new SyntheticSettings(50, 1000, 100, 500);
	}
	
	public static LearnerDataSettings getDefaultLearnerDataSettings() {
		return new LearnerDataSettings(Files.LEARNER_DATA_PARAMETERS);
	}
	
	public static Callback getDefaultCallbackFilter() {
		return new Callback() {
			private int counter = 0;
			@Override
			public void call() {
				if((++this.counter)%100 == 0) {
					Log.info("UNFILTERED ITERATIONS: " + this.counter);
				}
			}
		};
	}
	
	public static LearnerSettings getDefaultLearnerSettings(int numSamples, int maxLength) {
		return new LearnerSettings(numSamples, maxLength, true, true);
	}
	
	public static LearnerSettings getDefaultLearnerSettings(int numSamples) {
		return new LearnerSettings(numSamples, 100, true, true);
	}
	
	public static LearnerSettings getDefaultLearnerSettings() {
		return new LearnerSettings(50, 100, true, true);
	}
	
	public static SampleParameters getDefaultSampleParameters() {
		return new SampleParameters(new double[]{0.2, 0.2, 0.2, 0.4}, 0.8, 0.1, 100, true);
	}
	
	public static FuzzParameters getDefaultFuzzParameters() {
		return XmlData.HANDWRITTEN_PARAMETERS;
	}
	
	public static FuzzSettings getDefaultFuzzSettings(Random random, int numMutations) {
		return new FuzzSettings(numMutations, 50000, 100, 1000, getDefaultSampleParameters(), getDefaultFuzzParameters(), 20, 100, new MCMCAcceptor(random), 100, 0.9);
	}
	
	public static FuzzSettings getDefaultFuzzSettings(Random random) {
		return new FuzzSettings(50, 50000, 100, 1000, getDefaultSampleParameters(), getDefaultFuzzParameters(), 20, 100, new MCMCAcceptor(random), 100, 0.9);
	}
	
	public static SampleParameters getLongRunningSampleParameters() {
		return new SampleParameters(new double[]{0, 0, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05}, 0.1, 1.0, 1000, true);
	}
	
	public static FuzzSettings getLongRunningFuzzSettings(Random random) {
		return new FuzzSettings(20, 1000, 10, 10000, getLongRunningSampleParameters(), getDefaultFuzzParameters(), 20, 100, new MCMCAcceptor(random), 100, 0.9);
	}
	
	public static LongRunningSettings getDefaultLongRunningSettings() {
		return new LongRunningSettings(50, 10);
	}
	
	public static CompareSettings getDefaultCompareSettings() {
		return new CompareSettings(100);
	}
	
	public static enum RunCommand {
		// Learning
		LEARN,
		LEARN_RPNI,
		LEARN_LSTAR,
		BUILD_AFL,
		// Long running
		LONG_RUNNING,
		// Synthetic
		SYNTHETIC,
		// Custom grammar written as json
		CUSTOM,
		// Fuzzing
		FUZZER,
		// Statistics
		FALSE_NEGATIVE_RATE,
		GRAMMAR_SIZE,
		TRAIN_EXAMPLE_LINES,
		COVERABLE_LINES;
		public void run(FuzzSettings fuzz, Fuzzer fuzzer, Fuzzer remove, ProgramSettings program, SyntheticSettings synthetic, SyntheticLearner syntheticLearner, SyntheticGrammar syntheticGrammar, CustomGrammar customGrammar, LearnerDataSettings learnerData, CompareSettings compare, Processor processor, LearnerSettings learner, LongRunningSettings longRunning, Callback filterCallback, Random random) {
			switch(this) {
			case LEARN:
				LearnerDataUtils.learnAllGrammar(learnerData.learnerData, program.name, program.data, program.examples, learner.ignoreErrors, learner.useCharacterClasses);
				break;
			case LEARN_RPNI:
				LearnerDataUtils.learnRpni(learnerData.learnerData, learner, program.name, program.data, program.examples, random);
				break;
			case LEARN_LSTAR:
				LearnerDataUtils.learnLstar(learnerData.learnerData, learner, program.name, program.data, program.examples, random);
				break;
			case FUZZER:
				Iterable<String> fuzzSamples = processor.getFilteredSamples(program.data.getQueryOracle(), fuzz, fuzzer.getSamples(program, learner, fuzz, learnerData, random), filterCallback);
				RunProgram.runCoverage(program.data, fuzzSamples, remove.getSamples(program, learner, fuzz, learnerData, random), fuzz.recordIters);
				break;
			case LONG_RUNNING:
				Iterable<String> longRunningSamples = processor.getFilteredSamples(program.data.getQueryOracle(), fuzz, fuzzer.getSamples(program, learner, fuzz, learnerData, random), filterCallback);
				RunProgram.runTiming(program.data, longRunningSamples, program.examples.getTrainExamples(), longRunning.numIters, longRunning.numQueue, /*fuzz.recordIters*/10);
				break;
			case FALSE_NEGATIVE_RATE:
				ComputeStatistics.getFalseNegativeRate(LearnerDataUtils.loadAllGrammar(learnerData.learnerData, program.name), fuzzer.getSamples(program, learner, fuzz, learnerData, random), fuzz.maxLength);
				break;
			case GRAMMAR_SIZE:
				ComputeStatistics.getGrammarSize(LearnerDataUtils.loadAllGrammar(learnerData.learnerData, program.name));
				break;
			case TRAIN_EXAMPLE_LINES:
				ComputeStatistics.getExampleLinesOfCode(program.data.getQueryOracle(), program.examples.getTrainExamples());
				break;
			case COVERABLE_LINES:
				ComputeStatistics.getCoverableLinesOfCode(program.data, program.examples);
				break;
			case SYNTHETIC:
				Log.info("SIZE: " + ComputeStatistics.getMultiGrammarSize(syntheticGrammar.getGrammar()));
				HybridOracle oracle = RunSynthetic.getTrainSynthetic(syntheticLearner.getOracleLearner(program, learner, fuzz, random),  learnerData.learnerData, syntheticGrammar.getGrammar(), synthetic.boxSize, synthetic.numTrainExamples, synthetic.numTestExamples, fuzz.sample, synthetic.maxLength, filterCallback, random);
				RunSynthetic.getTestSynthetic(oracle, learnerData.learnerData, syntheticGrammar.getGrammar(), synthetic.boxSize, synthetic.numTrainExamples, synthetic.numTestExamples, fuzz.sample, synthetic.maxLength, filterCallback, random);
				break;
			case CUSTOM:
				Log.info("SIZE: " + ComputeStatistics.getMultiGrammarSize(customGrammar.getGrammar()));
				HybridOracle oracleCustom = RunSynthetic.getTrainSynthetic(syntheticLearner.getOracleLearner(program, learner, fuzz, random),  learnerData.learnerData, customGrammar.getGrammar(), synthetic.boxSize, synthetic.numTrainExamples, synthetic.numTestExamples, fuzz.sample, synthetic.maxLength, filterCallback, random);
				RunSynthetic.getTestSynthetic(oracleCustom, learnerData.learnerData, customGrammar.getGrammar(), synthetic.boxSize, synthetic.numTrainExamples, synthetic.numTestExamples, fuzz.sample, synthetic.maxLength, filterCallback, random);
				break;
			case BUILD_AFL:
				LearnerDataUtils.buildAflQueueAll(learnerData.learnerData, program.name, program.data, program.examples, fuzz.numIters + program.examples.getTrainExamples().size());
				break;
			default:
				throw new RuntimeException();
			}
		}


	}



	public static void runFuzz(Random random, Fuzzer fuzzer, Processor processor, Program program) {
		RunCommand.FUZZER.run(getDefaultFuzzSettings(random), fuzzer, Fuzzer.TRAIN, program.getSettings(), getDefaultSyntheticSettings(), SyntheticLearner.SYNTHESIS, SyntheticGrammar.XML, null, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), processor, getDefaultLearnerSettings(), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}
	
	public static void runFuzz(Random random, Fuzzer fuzzer, Processor processor, Program program, int numMutations) {
		RunCommand.FUZZER.run(getDefaultFuzzSettings(random, numMutations), fuzzer, Fuzzer.TRAIN, program.getSettings(), getDefaultSyntheticSettings(), SyntheticLearner.SYNTHESIS, SyntheticGrammar.XML, null, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), processor, getDefaultLearnerSettings(), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}
	
	public static void runLearn(Program program, Random random) {
		RunCommand.LEARN.run(getDefaultFuzzSettings(random), Fuzzer.TRAIN, Fuzzer.TRAIN, program.getSettings(), getDefaultSyntheticSettings(), SyntheticLearner.SYNTHESIS, SyntheticGrammar.XML, null, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), Processor.BOUND_THEN_FILTER, getDefaultLearnerSettings(), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}
	
	public static void runSynthetic(Random random, SyntheticLearner syntheticLearner, SyntheticGrammar syntheticGrammar, int numSamples) {
		RunCommand.SYNTHETIC.run(getDefaultFuzzSettings(random), Fuzzer.TRAIN, Fuzzer.TRAIN, Program.XML.getSettings(), getDefaultSyntheticSettings(numSamples), syntheticLearner, syntheticGrammar, null, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), Processor.BOUND_THEN_FILTER, getDefaultLearnerSettings(numSamples), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}
	
	public static void runSynthetic(Random random, SyntheticLearner syntheticLearner, SyntheticGrammar syntheticGrammar) {
		RunCommand.SYNTHETIC.run(getDefaultFuzzSettings(random), Fuzzer.TRAIN, Fuzzer.TRAIN, Program.XML.getSettings(), getDefaultSyntheticSettings(), syntheticLearner, syntheticGrammar, null, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), Processor.BOUND_THEN_FILTER, getDefaultLearnerSettings(), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}

	public static void runCustom(Random random, SyntheticLearner syntheticLearner, CustomGrammar customGrammar, int numSamples) {
		RunCommand.CUSTOM.run(getDefaultFuzzSettings(random), Fuzzer.TRAIN, Fuzzer.TRAIN, Program.XML.getSettings(), getDefaultSyntheticSettings(numSamples), syntheticLearner, SyntheticGrammar.XML, customGrammar, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), Processor.BOUND_THEN_FILTER, getDefaultLearnerSettings(numSamples), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}

	public static void runCustom(Random random, SyntheticLearner syntheticLearner, CustomGrammar customGrammar) {
		RunCommand.CUSTOM.run(getDefaultFuzzSettings(random), Fuzzer.TRAIN, Fuzzer.TRAIN, Program.XML.getSettings(), getDefaultSyntheticSettings(), syntheticLearner, SyntheticGrammar.XML, customGrammar, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), Processor.BOUND_THEN_FILTER, getDefaultLearnerSettings(), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}

	public static void runBuildAfl(Program program, Random random) {
		RunCommand.BUILD_AFL.run(getDefaultFuzzSettings(new Random()), Fuzzer.TRAIN, Fuzzer.TRAIN, program.getSettings(), getDefaultSyntheticSettings(), SyntheticLearner.SYNTHESIS, SyntheticGrammar.XML, null, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), Processor.BOUND_THEN_FILTER, getDefaultLearnerSettings(), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}
	
	public static void runLearnRpni(Program program, Random random) {
		RunCommand.LEARN_RPNI.run(getDefaultFuzzSettings(new Random()), Fuzzer.TRAIN, Fuzzer.TRAIN, program.getSettings(), getDefaultSyntheticSettings(), SyntheticLearner.SYNTHESIS, SyntheticGrammar.XML, null, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), Processor.BOUND_THEN_FILTER, getDefaultLearnerSettings(), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}
	
	public static void runLearnLstar(Program program, int numSamples, int maxLength, Random random) {
		RunCommand.LEARN_LSTAR.run(getDefaultFuzzSettings(new Random()), Fuzzer.TRAIN, Fuzzer.TRAIN, program.getSettings(), getDefaultSyntheticSettings(), SyntheticLearner.SYNTHESIS, SyntheticGrammar.XML, null, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), Processor.BOUND_THEN_FILTER, getDefaultLearnerSettings(numSamples, maxLength), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}
	
	public static void runLearnLstar(Program program, Random random) {
		RunCommand.LEARN_LSTAR.run(getDefaultFuzzSettings(new Random()), Fuzzer.TRAIN, Fuzzer.TRAIN, program.getSettings(), getDefaultSyntheticSettings(), SyntheticLearner.SYNTHESIS, SyntheticGrammar.XML, null, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), Processor.BOUND_THEN_FILTER, getDefaultLearnerSettings(), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}
	
	public static Program[] getAllPrograms() {
		return new Program[]{Program.XML, Program.GREP, Program.SED, Program.BISON, Program.FLEX, Program.PYTHON_WRAPPED, Program.RUBY_WRAPPED, Program.FFJS_WRAPPED};
	}

	public static int[] getAllGrammarNumMutations() {
		return new int[]{50, 50, 50, 10, 100, 20, 50, 20};
	}
	
	public static SyntheticGrammar[] getAllSyntheticGrammars() {
		return new SyntheticGrammar[]{SyntheticGrammar.URL, SyntheticGrammar.GREP, SyntheticGrammar.LISP, SyntheticGrammar.XML};
	}
	
	public static void runAllLearn(Random random) {
		for(Program program : getAllPrograms()) {
			runLearn(program, random);
		}
	}
	
	public static void runAllLearnRpni(Random random, int timeoutSeconds) {
		for(Program program : getAllPrograms()) {
			try {
				Utils.runForceTimeout(new Runnable() { public void run() { runLearnRpni(program, random); }}, 1000*timeoutSeconds);
			} catch(Exception e) {
				Log.info("TIMEOUT");
			}
		}
	}
	
	public static void runAllLearnLstar(Random random, int timeoutSeconds) {
		for(Program program : getAllPrograms()) {
			try {
				Utils.runForceTimeout(new Runnable() { public void run() { runLearnLstar(program, random); }}, 1000*timeoutSeconds);
			} catch(Exception e) {
				Log.info("TIMEOUT");
			}
		}
	}
	
	public static void runAllBuild(Random random) {
		for(Program program : getAllPrograms()) {
			runBuildAfl(program, random);
		}
	}
	
	public static void runAllFuzz(Random random, Fuzzer fuzzer, Processor processor, String addendum, int[] numMutations) {
		Program[] programs = getAllPrograms();
		for(int i=0; i<programs.length; i++) {
			runFuzz(random, fuzzer, processor, programs[i], numMutations[i]);
		}
	}
	
	public static void runAllFuzz(Random random, Fuzzer fuzzer, Processor processor, String addendum) {
		for(Program program : getAllPrograms()) {
			try {
				runFuzz(random, fuzzer, processor, program);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void runXmlManual(Random random) {
		runFuzz(random, Fuzzer.XML_MANUAL, Processor.BOUND_THEN_FILTER, Program.XML);
		runFuzz(random, Fuzzer.XML_MANUAL_COMBINED, Processor.BOUND, Program.XML);
	}
	
	public static void runSynthetic(Random random, SyntheticGrammar grammar, SyntheticLearner learner, int numSamples, int timeoutSeconds) {
		Utils.runForceTimeout(new Runnable() { public void run() { runSynthetic(random, learner, grammar, numSamples); }}, 1000*timeoutSeconds);
	}
	
	public static void runSynthetic(Random random, SyntheticGrammar grammar, SyntheticLearner learner, int[] numSamples, int timeoutSeconds) {
		try {
			for(int i : numSamples) {
				runSynthetic(random, grammar, learner, i, timeoutSeconds);
			}
		} catch(Exception e) {
			Log.info("TIMEOUT");
		}
	}
	
	public static void runSyntheticAll(Random random, SyntheticLearner learner, int[] numSamples, int timeoutSeconds, String addendum) {
		for(SyntheticGrammar grammar : getAllSyntheticGrammars()) {
			runSynthetic(random, grammar, learner, numSamples, timeoutSeconds);
		}
	}

	public static void runCustom(Random random, CustomGrammar grammar, SyntheticLearner learner, int numSamples, int timeoutSeconds) {
		Utils.runForceTimeout(new Runnable() { public void run() { runCustom(random, learner, grammar, numSamples); }}, 1000*timeoutSeconds);
	}

	public static void runCustom(Random random, CustomGrammar grammar, SyntheticLearner learner, int[] numSamples, int timeoutSeconds) {
		try {
			for(int i : numSamples) {
				runCustom(random, grammar, learner, i, timeoutSeconds);
			}
		} catch(Exception e) {
			Log.info("TIMEOUT");
		}
	}


	public static void runLongRunning(Random random, Program program, Fuzzer fuzzer) {
		RunCommand.LONG_RUNNING.run(getLongRunningFuzzSettings(random), fuzzer, Fuzzer.EMPTY, program.getSettings(), getDefaultSyntheticSettings(), SyntheticLearner.SYNTHESIS, SyntheticGrammar.XML, null, getDefaultLearnerDataSettings(), getDefaultCompareSettings(), Processor.BOUND_THEN_FILTER_ASCII, getDefaultLearnerSettings(), getDefaultLongRunningSettings(), getDefaultCallbackFilter(), random);
	}
	
	public static void runMultipleFuzz(Random random, Program program) {
		for(int i=0; i<5; i++) {
			runFuzz(random, Fuzzer.COMBINED, Processor.BOUND, program);
			runFuzz(random, Fuzzer.NAIVE, Processor.BOUND, program);
		}
	}
	
	public static void usage() {
		System.out.println("Learn handwritten grammars:");
		System.out.println("  java -jar glade.jar learn-handwritten <algorithm> <grammar>");
		System.out.println("  <algorithm> = rpni, lstar, glade-p1, glade");
		System.out.println("  <grammar> = url, grep, lisp, xml");
		System.out.println();
		System.out.println("Learn custom json style written grammar:");
		System.out.println("  java -jar glade.jar learn-custom <path_to_file>");
		System.out.println();
		System.out.println("Learn program input grammar:");
		System.out.println("  java -jar glade.jar learn-program <program>");
		System.out.println("  <program> = sed, grep, flex, xml, python, js");
		System.out.println();
		System.out.println("Learn program input grammar:");
		System.out.println("  java -jar glade.jar fuzz-program <program>");
		System.out.println("  <program> = sed, grep, flex, xml, python, js");
		System.exit(0);
	}
	
	public static void main(String[] args) {
		Log.init("log.txt");
		// AutomatonUtils.init();
		
		if(args.length == 0) {
			usage();
		}
		
		if(args[0].equals("learn-handwritten")) {
			if(args.length != 3) {
				usage();
			}
			
			int[] numSamples = new int[]{5, 10, 15, 20, 25, 30, 35, 40, 45, 50};
			int timeoutSeconds = 300;
			
			SyntheticLearner learner = null;
			if(args[1].equals("rpni")) {
				learner = SyntheticLearner.RPNI;
			} else if(args[1].equals("lstar")) {
				learner = SyntheticLearner.LSTAR;
			} else if(args[1].equals("glade")) {
				learner = SyntheticLearner.SYNTHESIS;
			} else if(args[1].equals("glade-p1")) {
				learner = SyntheticLearner.REGULAR_SYNTHESIS;
			} else {
				usage();
			}
			
			SyntheticGrammar grammar = null;
			if(args[2].equals("url")) {
				grammar = SyntheticGrammar.URL;
			} else if(args[2].equals("grep")) {
				grammar = SyntheticGrammar.GREP;
			} else if(args[2].equals("lisp")) {
				grammar = SyntheticGrammar.LISP;
			} else if(args[2].equals("xml")) {
				grammar = SyntheticGrammar.XML;
			} else {
				usage();
			}
			
			runSynthetic(new Random(), grammar, learner, numSamples, timeoutSeconds);
			PostProcessor.processSynthetic();

		} else if(args[0].equals("learn-custom")) {
			if(args.length != 2) {
				usage();
			}
			int[] numSamples = new int[]{5, 10, 15, 20, 25, 30, 35, 40, 45, 50};
			int timeoutSeconds = 300;
			String filePath = args[1];

			SyntheticLearner learner = SyntheticLearner.SYNTHESIS;
			CustomGrammar customGrammar = new CustomGrammar(filePath);

			runCustom(new Random(), customGrammar, learner, numSamples, timeoutSeconds);
			PostProcessor.processSynthetic();

		} else if(args[0].equals("learn-program")) {
			if(args.length != 2) {
				usage();
			}

			Program program = null;
			if(args[1].equals("sed")) {
				program = Program.SED;
			} else if(args[1].equals("grep")) {
				program = Program.GREP;
			} else if(args[1].equals("flex")) {
				program = Program.FLEX;
			} else if(args[1].equals("xml")) {
				program = Program.XML;
			} else if(args[1].equals("python")) {
				program = Program.PYTHON_WRAPPED;
			} else if(args[1].equals("js")) {
				program = Program.FFJS;
			} else {
				usage();
			}

			runLearn(program, new Random());

		} else if(args[0].equals("fuzz-program")) {
			if(args.length != 2) {
				usage();
			}

			Processor processor = Processor.BOUND_THEN_FILTER_ASCII;

			Program program = null;
			if(args[1].equals("sed")) {
				program = Program.SED;
			} else if(args[1].equals("grep")) {
				program = Program.GREP;
			} else if(args[1].equals("flex")) {
				program = Program.FLEX;
			} else if(args[1].equals("xml")) {
				program = Program.XML;
			} else if(args[1].equals("python")) {
				program = Program.PYTHON_WRAPPED;
			} else if(args[1].equals("js")) {
				program = Program.FFJS;
			} else {
				usage();
			}

			runFuzz(new Random(), Fuzzer.NAIVE, processor, program);
			runFuzz(new Random(), Fuzzer.GRAMMAR, processor, program);
			PostProcessor.processFuzz();

		} else {
			usage();
		}
		
		System.exit(0);
	}
}
