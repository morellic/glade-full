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

package glade.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import glade.program.ProgramOracleUtils.CoverInfo;
import glade.program.ProgramOracleUtils.CoverageOracle;
import glade.program.ProgramOracleUtils.Oracle;
import glade.util.OracleUtils.DiscriminativeOracle;
import glade.util.Utils.ConstantFilter;
import glade.util.Utils.Filter;
import glade.util.Utils.Pair;

public class ShellUtils {
	public static String escape(String query) {
		StringBuilder sb = new StringBuilder();
		for(char c : query.toCharArray()) {
			switch(c) {
			case '"':
				sb.append("\\\"");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return sb.toString();
	}
	
	public static void delete(String filename) {
		new File(filename).delete();
	}
	
	public static void write(String query, File file) {
		file.delete();
		try {
			FileWriter fw = new FileWriter(file);
			fw.write(query);
			fw.close();
		} catch(IOException e) {
			throw new Error(e);
		}
	}
	
	public static void write(String query, String filename) {
		write(query, new File(filename));
	}
	
	public static Filter<File> getExtensionFilter(final String extension) {
		return new Filter<File>() { public boolean filter(File file) { return file.getName().endsWith("." + extension); }};
	}
	
	public static Iterable<File> find(File root, Filter<File> dirFilter, Filter<File> fileFilter) {
		List<File> results = new ArrayList<File>();
		findHelper(root, dirFilter, fileFilter, results);
		return results;
	}
	
	public static Iterable<File> find(File root, String extension) {
		return find(root, getExtensionFilter(extension));
	}
	
	public static Iterable<File> find(File root, Filter<File> fileFilter) {
		return find(root, new ConstantFilter<File>(true), fileFilter);
	}
	
	private static void findHelper(File root, Filter<File> dirFilter, Filter<File> fileFilter, List<File> results) {
		if(root.isDirectory()) {
			if(!dirFilter.filter(root)) {
				return;
			}
			for(File file : root.listFiles()) {
				findHelper(file, dirFilter, fileFilter, results);
			}
		} else {
			if(!fileFilter.filter(root)) {
				return;
			}
			results.add(root);
		}
	}
	
	public static void deleteAll(File root, Filter<File> filter) {
		for(File file : find(root, filter)) {
			file.delete();
		}
	}
	
	public static void deleteAll(File root, String extension) {
		deleteAll(root, getExtensionFilter(extension));
	}
	
	public static String read(InputStream input) {
		try {
			StringBuilder result = new StringBuilder();
			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			String line;
			while((line = br.readLine()) != null) {
				result.append(line).append("\n");
			}
			br.close();
			return result.toString();
		} catch (IOException e) {
			throw new Error(e);
		}
	}
	
	public static Process execute(String command) {
		Process process = executeNoWait(command);
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return process;
	}
	
	private static Process executeNoWait(String command) {
		try {
			String[] shellCommand = {"/bin/sh", "-c", command};
			return Runtime.getRuntime().exec(shellCommand);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String executeForStream(final String command, final boolean isError, long timeoutMillis) {
		final Process process = executeNoWait(command);
		Callable<String> exec = new Callable<String>() {
			public String call() {
				String result = read(isError ? process.getErrorStream() : process.getInputStream());
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				return result;
			}
		};
		if(timeoutMillis == -1) {
			try {
				return exec.call();
			} catch (Exception e) {
				throw new Error(e);
			}
		} else {
			final ExecutorService executor = Executors.newSingleThreadExecutor();
			final Future<String> future = executor.submit(exec);
			executor.shutdown();
			String result;
			try {
				result = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
			} catch(Exception e) {
				process.destroy();
				result = "Timeout!";
			}
			if(!executor.isTerminated()) {
			    executor.shutdownNow();
			}
			return result;
		}
	}
	
	public static interface CommandFactory {
		public abstract String getCommand(String filename, String exePath);
	}
	
	public static class SimpleCommandFactory implements CommandFactory {
		@Override
		public String getCommand(String filename, String exePath) {
			return exePath + " " + filename;
		}
	}
	
	public static class ShellOracle implements Oracle {
		private final String command;
		private final String filename;
		private final boolean isError;
		private final long timeoutMillis;
		
		public ShellOracle(String filename, String command, boolean isError, long timeoutMillis) {
			this.filename = filename;
			this.command = command;
			this.isError = isError;
			this.timeoutMillis = timeoutMillis;
		}
		
		@Override
		public String execute(String query) {
			write(query, this.filename);
			String result = ShellUtils.executeForStream(this.command, this.isError, this.timeoutMillis);
			delete(this.filename);
			return result;
		}
	}
	
	public static class ShellDiscriminativeOracle implements DiscriminativeOracle {
		private final Oracle oracle;
		
		public ShellDiscriminativeOracle(Oracle oracle) {
			this.oracle = oracle;
		}

		@Override
		public boolean query(String query) {
			return this.oracle.execute(query).matches("\\s*");
		}
	}
	
	public static class ShellCoverageOracle implements CoverageOracle {
		private final Oracle oracle;
		private final File root;
		
		public ShellCoverageOracle(Oracle oracle, String root) {
			this.oracle = oracle;
			this.root = new File(root);
		}

		@Override
		public void startCoverage() {
			deleteAll(this.root, "gcda");
		}

		@Override
		public void runCoverage(String query) {
			this.oracle.execute(query);
		}

		@Override
		public void endCoverage() {
			deleteAll(this.root, "gcda");
		}
		
		@Override
		public Map<Pair<String, Integer>, CoverInfo> curFullCoverage() {
			return runGcovAll(this.root);
		}
	}
	
	public static Map<Pair<String,Integer>,CoverInfo> runGcovRead(File gcovFile) {
		Map<Pair<String,Integer>,CoverInfo> coverage = new HashMap<Pair<String,Integer>,CoverInfo>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(gcovFile));
			String line;
			while((line = br.readLine()) != null) {
				if(!line.contains(":")) {
					continue;
				}
				CoverInfo coverInfo = new ProgramCoverInfo(line.split(":")[0].trim());
				String lineNumber = line.split(":")[1].trim();
				coverage.put(new Pair<String,Integer>(gcovFile.getAbsolutePath(), Integer.parseInt(lineNumber)), coverInfo);
			}
			br.close();
			gcovFile.delete();
		} catch(IOException e) {}
		return coverage;
	}

	// maps (file name, line no.) -> coverage info (#####, -, or number)
	public static Map<Pair<String,Integer>,CoverInfo> runGcov(File gcnoFile) {
		//System.out.println("HERE: " + gcnoFile.getAbsolutePath());
		String[] result = executeForStream("gcov " + gcnoFile.getAbsolutePath(), false, -1).split("\n");
		List<File> gcovFiles = new ArrayList<File>();
		for(String str : result) {
			if(str.contains(":creating '")) {
				//System.out.println(str);
				String[] tokens = str.split(":creating '");
				if(tokens.length != 2) {
					throw new RuntimeException("Invalid inputs!");
				}
				gcovFiles.add(new File(tokens[1].substring(0, tokens[1].length()-1)));
			}
		}
		Map<Pair<String,Integer>,CoverInfo> coverage = new HashMap<Pair<String,Integer>,CoverInfo>();
		for(File gcovFile : gcovFiles) {
			coverage.putAll(runGcovRead(gcovFile));
		}
		return coverage;
	}
	
	public static Map<Pair<String,Integer>,CoverInfo> runGcovAll(File root) {
		Map<Pair<String,Integer>,CoverInfo> result = new HashMap<Pair<String,Integer>,CoverInfo>();
		for(File file : find(root, "gcno")) {
			result.putAll(runGcov(file));
		}
		return result;
	}
	
	public static Map<String,Pair<Integer,Integer>> runGcovSummary(File gcnoFile) {
		String[] lines = executeForStream("gcov -n " + gcnoFile.getAbsolutePath(), false, -1).split("\n");
		String curFile = null;
		Map<String,Pair<Integer,Integer>> result = new HashMap<String,Pair<Integer,Integer>>();
		for(String line : lines) {
			if(curFile != null && line.startsWith("Lines executed:")) {
				if(result.containsKey(line)) { throw new RuntimeException(); }
				String[] tokens = line.split(":")[1].split("% of ");
				if(tokens.length != 2) { throw new RuntimeException("Invalid: " + line); }
				double frac = Double.parseDouble(tokens[0]);
				int coverable = Integer.parseInt(tokens[1]);
				int covered = (int)Math.round(frac*coverable/100);
				result.put(curFile, new Pair<Integer,Integer>(covered, coverable));
				curFile = null;
			}
			if(line.startsWith("File")) {
				curFile = line;
			}
		}
		return result;
	}
	
	public static double runGcovSummaryAll(File root) {
		Map<String,Pair<Integer,Integer>> results = new HashMap<String,Pair<Integer,Integer>>();
		for(File file : find(root, "gcno")) {
			results.putAll(runGcovSummary(file));
		}
		int num = 0;
		int denom = 0;
		for(Pair<Integer,Integer> fraction : results.values()) {
			num += fraction.getX();
			denom += fraction.getY();
		}
		return (double)num/denom;
	}
	
	private static class ProgramCoverInfo implements CoverInfo {
		// should be #####, -, or a number
		private final String coverInfo;
		private ProgramCoverInfo(String coverInfo) {
			this.coverInfo = coverInfo;
			if(!this.coverInfo.equals("-") && !this.coverInfo.equals("#####") && !this.isCovered()) {
				Log.info("INVALID COVER INFO: " + this.coverInfo);
			}
		}
		
		@Override
		public boolean isCovered() {
			try {
				Long.parseLong(this.coverInfo);
				return true;
			} catch(NumberFormatException e) {
				return false;
			}
		}
		
		@Override
		public long getNumTimesCovered() {
			return this.isCovered() ? Long.parseLong(this.coverInfo) : 0;
		}
		
		@Override
		public boolean isCoverable() {
			return !this.coverInfo.equals("-");
		}
	}
}
