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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import glade.afl.AflOracleUtils.AflCoverageOracle;
import glade.afl.AflOracleUtils.AflOracle;
import glade.afl.AflOracleUtils.ShellAflCoverageOracle;
import glade.afl.AflOracleUtils.ShellAflOracle;
import glade.afl.AflOracleUtils.WrappedAflCoverageOracle;
import glade.afl.AflOracleUtils.WrappedAflOracle;
import glade.program.ProgramOracleUtils.CoverageOracle;
import glade.program.ProgramOracleUtils.WrappedCoverageOracle;
import glade.util.Utils;
import glade.util.OracleUtils.DiscriminativeOracle;
import glade.util.OracleUtils.WrappedDiscriminativeOracle;
import glade.util.OracleUtils.Wrapper;
import glade.util.ShellUtils.CommandFactory;
import glade.util.ShellUtils.ShellCoverageOracle;
import glade.util.ShellUtils.ShellDiscriminativeOracle;
import glade.util.ShellUtils.ShellOracle;
import glade.util.ShellUtils.SimpleCommandFactory;

public class ProgramDataUtils {
	public static class FileParameters {
		public final String queryProg;
		public final String gcovProg;
		public final String aflProg;
		public final String filename;
		public final long timeout;
		public final String exampleTrainPath;
		public final String exampleTestPath;
		public FileParameters(String queryProg, String gcovProg, String aflProg, String filename, long timeout, String exampleTrainPath, String exampleTestPath) {
			this.queryProg = queryProg;
			this.gcovProg = gcovProg;
			this.aflProg = aflProg;
			this.filename = filename;
			this.timeout = timeout;
			this.exampleTrainPath = exampleTrainPath;
			this.exampleTestPath = exampleTestPath;
		}
	}
	
	public static interface ProgramData {
		public abstract DiscriminativeOracle getQueryOracle();
		public abstract CoverageOracle getCoverageOracle();
		public abstract AflCoverageOracle getAflCoverageOracle();
		public abstract AflOracle getAflOrigOracle();
	}
	
	public static interface ProgramExamples {
		public abstract List<String> getTrainExamples();
		public abstract List<String> getTestExamples();
		public abstract List<String> getEmptyExamples();
	}
	
	public static class ShellProgramData implements ProgramData {
		private final FileParameters file;
		private final CommandFactory factory;
		private final String exePath;
		private final String gcovPath;
		private final boolean isError;
		
		public ShellProgramData(FileParameters file, CommandFactory factory, String exePath, String gcovPath, boolean isError) {
			this.file = file;
			this.factory = factory;
			this.exePath = exePath;
			this.gcovPath = gcovPath;
			this.isError = isError;
		}
		
		public ShellProgramData(FileParameters file, String exePath, String gcovPath, boolean isError) {
			this(file, new SimpleCommandFactory(), exePath, gcovPath, isError);
		}
		
		@Override
		public DiscriminativeOracle getQueryOracle() {
			return new ShellDiscriminativeOracle(new ShellOracle(this.file.filename, this.factory.getCommand(this.file.filename, this.file.queryProg + File.separator + this.exePath), this.isError, this.file.timeout));
		}

		@Override
		public CoverageOracle getCoverageOracle() {
			return new ShellCoverageOracle(new ShellOracle(this.file.filename, this.factory.getCommand(this.file.filename, this.file.gcovProg + File.separator + this.exePath), this.isError, this.file.timeout), this.file.gcovProg + File.separator + this.gcovPath);
		}

		@Override
		public AflCoverageOracle getAflCoverageOracle() {
			return new ShellAflCoverageOracle(new ShellAflOracle(this.factory, this.file.aflProg + File.separator + this.exePath));
		}

		@Override
		public AflOracle getAflOrigOracle() {
			return new ShellAflOracle(this.factory, this.file.aflProg + File.separator + this.exePath);
		}
	}
	
	public static class WrappedProgramData implements ProgramData {
		private final ProgramData data;
		private final Wrapper wrapper;
		
		public WrappedProgramData(ProgramData data, Wrapper wrapper) {
			this.data = data;
			this.wrapper = wrapper;
		}
		
		@Override
		public DiscriminativeOracle getQueryOracle() {
			return new WrappedDiscriminativeOracle(this.data.getQueryOracle(), this.wrapper);
		}

		@Override
		public CoverageOracle getCoverageOracle() {
			return new WrappedCoverageOracle(this.data.getCoverageOracle(), this.wrapper);
		}

		@Override
		public AflCoverageOracle getAflCoverageOracle() {
			return new WrappedAflCoverageOracle(this.data.getAflCoverageOracle(), this.wrapper);
		}
		
		@Override
		public AflOracle getAflOrigOracle() {
			return new WrappedAflOracle(this.data.getAflOrigOracle(), this.wrapper);
		}
	}
	
	public static class SingleFileProgramExamples implements ProgramExamples {
		private final FileParameters file;
		private final String name;
		private final String filename;
		private final String emptyExample;
		private final Wrapper exampleProcessor;
		
		public SingleFileProgramExamples(FileParameters file, String name, String filename, String emptyExample, Wrapper exampleProcessor) {
			this.file = file;
			this.name = name;
			this.filename = filename;
			this.emptyExample = emptyExample;
			this.exampleProcessor = exampleProcessor;
		}
		
		private List<String> getExamples(String path) {
			try {
				List<String> examples = new ArrayList<String>();
				BufferedReader br = new BufferedReader(new FileReader(path + File.separator + this.name + File.separator + this.filename));
				String line;
				while((line = br.readLine()) != null) {
					examples.add(this.exampleProcessor.wrap(line));
				}
				br.close();
				return examples;
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public List<String> getTrainExamples() {
			return this.getExamples(this.file.exampleTrainPath);
		}
		
		@Override
		public List<String> getTestExamples() {
			return this.getExamples(this.file.exampleTrainPath);
		}
		
		@Override
		public List<String> getEmptyExamples() {
			return Utils.getList(this.emptyExample);
		}
	}
	
	public static class MultiFileProgramExamples implements ProgramExamples {
		private final FileParameters file;
		private final String name;
		private final String extension;
		private final String emptyExample;
		private final Wrapper exampleProcessor;
		
		public MultiFileProgramExamples(FileParameters file, String name, String extension, String emptyExample, Wrapper exampleProcessor) {
			this.name = name;
			this.extension = extension;
			this.emptyExample = emptyExample;
			this.exampleProcessor = exampleProcessor;
			this.file = file;
		}
		
		private List<String> getExamples(String path) {
			List<String> examples = new ArrayList<String>();
			for(File file : new File(path, this.name).listFiles()) {
				if(!file.getName().endsWith(this.extension)) {
					continue;
				}
				try {
					StringBuilder sb = new StringBuilder();
					BufferedReader br = new BufferedReader(new FileReader(file));
					String line;
					while((line = br.readLine()) != null) {
						sb.append(line).append("\n");
					}
					br.close();
					examples.add(this.exampleProcessor.wrap(sb.toString()));
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}
			return examples;
		}
		
		@Override
		public List<String> getTrainExamples() {
			return this.getExamples(this.file.exampleTrainPath);
		}
		
		@Override
		public List<String> getTestExamples() {
			return this.getExamples(this.file.exampleTestPath);
		}

		@Override
		public List<String> getEmptyExamples() {
			return Utils.getList(this.emptyExample);
		}
	}
}
