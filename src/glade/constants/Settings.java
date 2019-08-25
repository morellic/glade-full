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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dk.brics.automaton.Automaton;
import glade.automaton.AutomatonUtils;
import glade.automaton.AutomatonUtils.LStarLearner;
import glade.automaton.AutomatonUtils.RPNILearner;
import glade.constants.program.BisonData;
import glade.constants.program.FFJSData;
import glade.constants.program.FlexData;
import glade.constants.program.GraphvizData;
import glade.constants.program.GrepData;
import glade.constants.program.PythonData;
import glade.constants.program.RubyData;
import glade.constants.program.SedData;
import glade.constants.program.XmlData;
import glade.grammar.MultiGrammarUtils.MultiGrammar;
import glade.grammar.fuzz.GrammarFuzzer.CombinedMutationSampler;
import glade.grammar.fuzz.GrammarFuzzer.GrammarMutationSampler;
import glade.grammar.fuzz.GrammarFuzzer.GrammarSampler;
import glade.grammar.fuzz.GrammarFuzzer.SampleParameters;
import glade.grammar.fuzz.GrammarTraceFuzzer.Acceptor;
import glade.grammar.fuzz.GrammarTraceFuzzer.GrammarOptimizationIterable;
import glade.grammar.fuzz.GrammarTraceFuzzer.GrammarProblem;
import glade.grammar.fuzz.GrammarTraceFuzzer.LearnedGrammarProblem;
import glade.grammar.fuzz.GrammarTraceFuzzer.ParseTreeExample;
import glade.grammar.fuzz.MultiGrammarFuzzer.MultiGrammarSampler;
import glade.main.GrepFuzzer.GrepMutationSampler;
import glade.main.RunSynthetic.DiscriminativeAutomatonOracleLearner;
import glade.main.RunSynthetic.GrammarSynthesisOracleLearner;
import glade.main.RunSynthetic.InteractiveAutomatonOracleLearner;
import glade.main.RunSynthetic.NoConstantGrammarSynthesisOracleLearner;
import glade.main.RunSynthetic.OracleLearner;
import glade.main.RunSynthetic.RegularGrammarSynthesisOracleLearner;
import glade.main.XmlFuzzer.FuzzParameters;
import glade.main.XmlFuzzer.XmlMutationSampler;
import glade.main.XmlFuzzer.XmlSampler;
import glade.program.LearnerDataUtils;
import glade.program.LearnerDataUtils.LearnerDataParameters;
import glade.program.ProgramDataUtils.ProgramData;
import glade.program.ProgramDataUtils.ProgramExamples;
import glade.util.CharacterUtils;
import glade.util.IteratorUtils.BoundedIterable;
import glade.util.IteratorUtils.FilteredIterable;
import glade.util.IteratorUtils.MultiMutationSampler;
import glade.util.IteratorUtils.MultiRandomSampler;
import glade.util.IteratorUtils.SampleIterable;
import glade.util.IteratorUtils.Sampler;
import glade.util.OracleUtils.DiscriminativeOracle;
import glade.util.OracleUtils.DiscriminativeOracleFilter;
import glade.util.RandomUtils.RandomExtra;
import glade.util.Utils.Callback;
import glade.util.Utils.Filter;

public class Settings {
	public static class SyntheticSettings {
		public final int numTrainExamples;
		public final int numTestExamples;
		public final int maxLength;
		public final int boxSize;
		public SyntheticSettings(int numTrainExamples, int numTestExamples, int maxLength, int boxSize) {
			this.numTrainExamples = numTrainExamples;
			this.numTestExamples = numTestExamples;
			this.maxLength = maxLength;
			this.boxSize = boxSize;
		}
	}
	
	public static class LearnerSettings {
		public final int numSamples;
		public final int maxLength;
		public final boolean ignoreErrors;
		public final boolean useCharacterClasses;
		public LearnerSettings(int numSamples, int maxLength, boolean ignoreErrors, boolean useCharacterClasses) {
			this.numSamples = numSamples;
			this.maxLength = maxLength;
			this.ignoreErrors = ignoreErrors;
			this.useCharacterClasses = useCharacterClasses;
		}
	}
	
	public static enum SyntheticLearner {
		SYNTHESIS, REGULAR_SYNTHESIS, NO_CONSTANT_SYNTHESIS, RPNI, LSTAR;
		public OracleLearner<?> getOracleLearner(ProgramSettings program, LearnerSettings learner, FuzzSettings fuzz, Random random) {
			switch(this) {
			case SYNTHESIS:
				return new GrammarSynthesisOracleLearner(fuzz.sample, random);
			case REGULAR_SYNTHESIS:
				return new RegularGrammarSynthesisOracleLearner(fuzz.sample, random);
			case NO_CONSTANT_SYNTHESIS:
				return new NoConstantGrammarSynthesisOracleLearner(fuzz.sample, random);
			case RPNI:
				return new DiscriminativeAutomatonOracleLearner(new RPNILearner(), new RandomExtra(random).nextUniformLengthStrings(learner.maxLength, learner.numSamples), random);
			case LSTAR:
				return new InteractiveAutomatonOracleLearner(new LStarLearner(), learner.numSamples, learner.maxLength, random);
			default:
				throw new RuntimeException();
			}
		}
		public String getName() {
			switch(this) {
			case SYNTHESIS:
				return "synthesis";
			case REGULAR_SYNTHESIS:
				return "reg_synthesis";
			case NO_CONSTANT_SYNTHESIS:
				return "noconst_synthesis";
			case RPNI:
				return "rpni";
			case LSTAR:
				return "lstar";
			default:
				throw new RuntimeException();
			}
		}
	}
	
	public static enum SyntheticGrammar {
		URL, GREP, LISP, XML;
		public MultiGrammar getGrammar() {
			switch(this) {
			case URL:
				return SyntheticGrammarFiles.loadGrammar(SyntheticGrammarFiles.URL_GRAMMAR).getY();
			case LISP:
				return SyntheticGrammars.getLispGrammar();
			case XML:
				return SyntheticGrammars.getXmlGrammar();
			case GREP:
				return SyntheticGrammarFiles.loadGrammar(SyntheticGrammarFiles.GREPLIM_GRAMMAR).getY();
			default:
				throw new RuntimeException();
			}		
		}
		public Iterable<String> getSampler(int boxSize, Random random) {
			return new SampleIterable(new MultiGrammarSampler(this.getGrammar(), boxSize, random));
		}
		public String getName() {
			switch(this) {
			case URL:
				return SyntheticGrammarFiles.loadGrammar(SyntheticGrammarFiles.URL_GRAMMAR).getX();
			case LISP:
				return SyntheticGrammars.getLispGrammarName();
			case XML:
				return SyntheticGrammars.getXmlGrammarName();
			case GREP:
				return SyntheticGrammarFiles.loadGrammar(SyntheticGrammarFiles.GREP_GRAMMAR).getX();
			default:
				throw new RuntimeException();
			}
		}
	}
	
	public static class LearnerDataSettings {
		public final LearnerDataParameters learnerData;
		public LearnerDataSettings(LearnerDataParameters learnerData) {
			this.learnerData = learnerData;
		}
	}
	
	public static class LongRunningSettings {
		public final int numQueue;
		public final int numIters;
		public LongRunningSettings(int numQueue, int numIters) {
			this.numQueue = numQueue;
			this.numIters = numIters;
		}
	}
	
	public static class ProgramSettings {
		public final ProgramData data;
		public final ProgramExamples examples;
		public final String name;
		public final Filter<String> filter;
		public ProgramSettings(ProgramData data, ProgramExamples examples, String name, Filter<String> filter) {
			this.data = data;
			this.examples = examples;
			this.name = name;
			this.filter = filter;
		}
	}
	
	public static class FuzzSettings {
		public final int numMutations;
		public final int numIters;
		public final int recordIters;
		public final int maxLength;
		public final SampleParameters sample;
		public final FuzzParameters fuzz;
		public final int numBeams;
		public final int numSubIters;
		public final Acceptor acceptor;
		public final int maxAutomatonMutationLength;
		public final double pAutomatonMutationInsert;
		public FuzzSettings(int numMutations, int numIters, int recordIters, int maxLength, SampleParameters sample, FuzzParameters fuzz, int numBeams, int numSubIters, Acceptor acceptor, int maxAutomatonMutationLength, double pAutomatonMutationInsert) {
			this.numMutations = numMutations;
			this.numIters = numIters;
			this.recordIters = recordIters;
			this.maxLength = maxLength;
			this.sample = sample;
			this.fuzz = fuzz;
			this.numBeams = numBeams;
			this.numSubIters = numSubIters;
			this.acceptor = acceptor;
			this.maxAutomatonMutationLength = maxAutomatonMutationLength;
			this.pAutomatonMutationInsert = pAutomatonMutationInsert;
		}
	}
	
	public static class CompareSettings {
		public final int maxLength;
		public CompareSettings(int maxLength) {
			this.maxLength = maxLength;
		}
	}
	
	public static enum Program {
		XML, GREP, SED, BISON, FLEX, PYTHON, PYTHON_WRAPPED, FFJS, FFJS_WRAPPED, RUBY, RUBY_WRAPPED, GRAPHVIZ;
		public ProgramSettings getSettings() {
			switch(this) {
			case XML:
				return new ProgramSettings(XmlData.XML_DATA, XmlData.XML_EXAMPLES, XmlData.XML_NAME, XmlData.XML_FILTER);
			case FFJS:
				return new ProgramSettings(FFJSData.FFJS_DATA, FFJSData.FFJS_EXAMPLES, FFJSData.FFJS_NAME, FFJSData.FFJS_FILTER);
			case FFJS_WRAPPED:
				return new ProgramSettings(FFJSData.FFJS_WRAPPED_DATA, FFJSData.FFJS_EXAMPLES, FFJSData.FFJS_WRAPPED_NAME, FFJSData.FFJS_WRAPPED_FILTER);
			case PYTHON:
				return new ProgramSettings(PythonData.PYTHON_DATA, PythonData.PYTHON_EXAMPLES, PythonData.PYTHON_NAME, PythonData.PYTHON_FILTER);
			case PYTHON_WRAPPED:
				return new ProgramSettings(PythonData.PYTHON_WRAPPED_DATA, PythonData.PYTHON_EXAMPLES, PythonData.PYTHON_WRAPPED_NAME, PythonData.PYTHON_WRAPPED_FILTER);
			case GREP:
				return new ProgramSettings(GrepData.GREP_DATA, GrepData.GREP_EXAMPLES, GrepData.GREP_NAME, GrepData.GREP_FILTER);
			case SED:
				return new ProgramSettings(SedData.SED_DATA, SedData.SED_EXAMPLES, SedData.SED_NAME, SedData.SED_FILTER);
			case FLEX:
				return new ProgramSettings(FlexData.FLEX_DATA, FlexData.FLEX_EXAMPLES, FlexData.FLEX_NAME, FlexData.FLEX_FILTER);
			case RUBY:
				return new ProgramSettings(RubyData.RUBY_DATA, RubyData.RUBY_EXAMPLES, RubyData.RUBY_NAME, RubyData.RUBY_FILTER);
			case RUBY_WRAPPED:
				return new ProgramSettings(RubyData.RUBY_WRAPPED_DATA, RubyData.RUBY_EXAMPLES, RubyData.RUBY_WRAPPED_NAME, RubyData.RUBY_WRAPPED_FILTER);
			case BISON:
				return new ProgramSettings(BisonData.BISON_DATA, BisonData.BISON_EXAMPLES, BisonData.BISON_NAME, BisonData.BISON_FILTER);
			case GRAPHVIZ:
				return new ProgramSettings(GraphvizData.GRAPHVIZ_DATA, GraphvizData.GRAPHVIZ_EXAMPLES, GraphvizData.GRAPHVIZ_NAME, GraphvizData.GRAPHVIZ_FILTER);
			default:
				throw new RuntimeException();
			}
		}
	}
	
	public static enum Processor {
		NONE, BOUND, FILTER, FILTER_ASCII, BOUND_THEN_FILTER, FILTER_THEN_BOUND, BOUND_THEN_FILTER_ASCII, FILTER_ASCII_THEN_BOUND;
		private static Iterable<String> filterAscii(DiscriminativeOracle oracle, Iterable<String> samples, Callback filterCallback) {
			return new FilteredIterable<String>(samples, new Filter<String>() { public boolean filter(String s) { return oracle.query(s) && CharacterUtils.isAsciiOrNewlineOrTabString(s); }}, filterCallback);
		}
		private static Iterable<String> filter(DiscriminativeOracle oracle, Iterable<String> samples, Callback filterCallback) {
			return new FilteredIterable<String>(samples, new DiscriminativeOracleFilter(oracle), filterCallback);
		}
		private static Iterable<String> bound(Iterable<String> samples, int numIters) {
			return new BoundedIterable<String>(samples, numIters);
		}
		public Iterable<String> getFilteredSamples(DiscriminativeOracle oracle, FuzzSettings fuzz, Iterable<String> samples, Callback filterCallback) {
			switch(this) {
			case NONE:
				return samples;
			case BOUND:
				return bound(samples, fuzz.numIters);
			case FILTER:
				return filter(oracle, samples, filterCallback);
			case FILTER_ASCII:
				return filterAscii(oracle, samples, filterCallback);
			case BOUND_THEN_FILTER:
				return filter(oracle, bound(samples, fuzz.numIters), filterCallback);
			case FILTER_THEN_BOUND:
				return bound(filter(oracle, samples, filterCallback), fuzz.numIters);
			case BOUND_THEN_FILTER_ASCII:
				return filterAscii(oracle, bound(samples, fuzz.numIters), filterCallback);
			case FILTER_ASCII_THEN_BOUND:
				return bound(filterAscii(oracle, samples, filterCallback), fuzz.numIters);
			default:
				throw new RuntimeException();
			}
		}
	}
	
	public static enum Fuzzer {
		NONE, EMPTY, TRAIN, TEST, GRAMMAR, NAIVE, COMBINED, GRAMMAR_MCMC, GRAMMAR_NO_SEED, RPNI, LSTAR, XML_MANUAL_NO_SEED, XML_MANUAL, XML_MANUAL_COMBINED, GREP_MANUAL_NO_SEED, AFL;
		public Iterable<String> getSamples(ProgramSettings program, LearnerSettings learner, FuzzSettings fuzz, LearnerDataSettings learnerData, Random random) {
			switch(this) {
			case NONE:
				return new ArrayList<String>();
			case EMPTY:
				return program.examples.getEmptyExamples();
			case TRAIN:
				return program.examples.getTrainExamples();
			case TEST:
				return program.examples.getTestExamples();
			case NAIVE:
				return new SampleIterable(new MultiMutationSampler(program.examples.getTrainExamples(), fuzz.numMutations, random));
			case GRAMMAR:
				return new SampleIterable(new GrammarMutationSampler(LearnerDataUtils.loadAllGrammar(learnerData.learnerData, program.name), fuzz.sample, fuzz.maxLength, fuzz.numMutations, random));
			case GRAMMAR_NO_SEED:
				return new SampleIterable(new GrammarSampler(LearnerDataUtils.loadAllGrammar(learnerData.learnerData, program.name), fuzz.sample, random));
			case GRAMMAR_MCMC:
				GrammarProblem<ParseTreeExample> problem = new LearnedGrammarProblem(LearnerDataUtils.loadAllGrammar(learnerData.learnerData, program.name), program.data.getQueryOracle(), fuzz.sample, fuzz.maxLength, random);
				return new GrammarOptimizationIterable<ParseTreeExample>(problem, program.data.getAflCoverageOracle(), fuzz.acceptor, fuzz.numSubIters, fuzz.numBeams, random);
			case COMBINED:
				List<Sampler> combinedSamplers = new ArrayList<Sampler>();
				Sampler grammarMutationSampler = new GrammarMutationSampler(LearnerDataUtils.loadAllGrammar(learnerData.learnerData, program.name), fuzz.sample, fuzz.maxLength, fuzz.numMutations, random);
				combinedSamplers.add(grammarMutationSampler);
				combinedSamplers.add(new CombinedMutationSampler(grammarMutationSampler, fuzz.numMutations, random));
				combinedSamplers.add(new MultiMutationSampler(program.examples.getTrainExamples(), fuzz.numMutations, random));
				return new SampleIterable(new MultiRandomSampler(combinedSamplers, random));
			case RPNI:
				Automaton rpniAutomaton = LearnerDataUtils.loadRpniAutomaton(learnerData.learnerData, program.name);
				return AutomatonUtils.getAutomatonMutationSampler(rpniAutomaton, program.examples, fuzz.numMutations, fuzz.maxAutomatonMutationLength, fuzz.pAutomatonMutationInsert, random);
			case LSTAR:
				Automaton lstarAutomaton = LearnerDataUtils.loadLstarAutomaton(learnerData.learnerData, program.name);
				return AutomatonUtils.getAutomatonMutationSampler(lstarAutomaton, program.examples, fuzz.numMutations, fuzz.maxAutomatonMutationLength, fuzz.pAutomatonMutationInsert, random);
			case XML_MANUAL_NO_SEED:
				return new SampleIterable(new XmlSampler(fuzz.fuzz, random));
			case XML_MANUAL:
				return new SampleIterable(new XmlMutationSampler(fuzz.fuzz, program.examples.getTrainExamples(), fuzz.maxLength, random));
			case GREP_MANUAL_NO_SEED:
				return new SampleIterable(new GrepMutationSampler(fuzz, random));
			case XML_MANUAL_COMBINED:
				List<Sampler> xmlCombinedSamplers = new ArrayList<Sampler>();
				Sampler xmlMutationSampler = new XmlMutationSampler(fuzz.fuzz, program.examples.getTrainExamples(), fuzz.maxLength, random);
				xmlCombinedSamplers.add(xmlMutationSampler);
				xmlCombinedSamplers.add(new CombinedMutationSampler(xmlMutationSampler, fuzz.numMutations, random));
				xmlCombinedSamplers.add(new MultiMutationSampler(program.examples.getTrainExamples(), fuzz.numMutations, random));
				return new SampleIterable(new MultiRandomSampler(xmlCombinedSamplers, random));
			case AFL:
				return LearnerDataUtils.loadAflQueueAll(learnerData.learnerData, program.examples, program.name, fuzz.numIters);
			default:
				throw new RuntimeException();
			}
		}
	}
}
