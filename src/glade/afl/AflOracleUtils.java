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

package glade.afl;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import glade.program.ProgramOracleUtils.Oracle;
import glade.util.ShellUtils;
import glade.util.OracleUtils.Wrapper;
import glade.util.ShellUtils.CommandFactory;
import glade.util.Utils.Pair;

public class AflOracleUtils {
	private static final String AFL_INPUT = "afl/input/seed";
	private static final String AFL_OUTPUT = "afl/output";
	
	private static final String AFL_DIR = "afl";
	private static final int AFL_DEFAULT_SAMPLES = 0;
	
	public static String getAflCommand(CommandFactory factory, String exePath, int numSamples) {
		return AFL_DIR + File.separator + "afl-fuzz -i afl/input/ -o afl/output/ -e " + numSamples + " " + factory.getCommand("@@", exePath);
	}
	
	public static void writeAfl(String query) {
		ShellUtils.write(query, AFL_INPUT);
	}
	
	public static void deleteAfl() {
		ShellUtils.delete(AFL_INPUT);
	}

	public static String executeForStreamAfl(String query, CommandFactory factory, String exePath, int numSamples) {
		writeAfl(query);
		String result = ShellUtils.executeForStream(getAflCommand(factory, exePath, numSamples), false, -1);
		deleteAfl();
		return result;
	}
	
	public static List<String> getAflQueue(File queueFile) {
		List<String> queue = new ArrayList<String>();
		for(File file : queueFile.listFiles()) {
			if(file.isDirectory()) {
				continue;
			}
			try {
				FileInputStream fis = new FileInputStream(file);
				String sample = ShellUtils.read(fis);
				queue.add(sample);
				fis.close();
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		return queue;
	}
	
	public static interface AflOracle {
		public abstract Pair<List<String>,String> executeForQueue(String query, int numSamples);
	}
	
	public static class WrappedAflOracle implements AflOracle {
		private final AflOracle oracle;
		private final Wrapper wrapper;
		
		public WrappedAflOracle(AflOracle oracle, Wrapper wrapper) {
			this.wrapper = wrapper;
			this.oracle = oracle;
		}
		@Override
		public Pair<List<String>,String> executeForQueue(String query, int numSamples) {
			return this.oracle.executeForQueue(this.wrapper.wrap(query), numSamples);
		}
	}
	
	public static class ShellAflOracle implements AflOracle, Oracle {
		private final CommandFactory factory;
		private final String exePath;
		
		public ShellAflOracle(CommandFactory factory, String exePath) {
			this.factory = factory;
			this.exePath = exePath;
		}
		
		@Override
		public Pair<List<String>,String> executeForQueue(String query, int numSamples) {
			cleanup();
			String result = executeForStreamAfl(query, this.factory, this.exePath, numSamples);
			List<String> queue = getAflQueue(new File(AFL_OUTPUT, "queue_all"));
			cleanup();
			return new Pair<List<String>,String>(queue, result);
		}
		
		@Override
		public String execute(String query) {
			return this.executeForQueue(query, AFL_DEFAULT_SAMPLES).getY();
		}
	}
	
	private static void cleanup(File dir) {
		if(!dir.exists()) {
			return;
		}
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				cleanup(file);
			}
			file.delete();
		}
	}
	
	private static void cleanup() {
		cleanup(new File(AFL_OUTPUT));
	}
	
	public static int getCoverageAfl(String query, Oracle oracle) {
		for(String line : oracle.execute(query).split("\n")) {
			if(line.startsWith("SCORE:")) {
				return Integer.parseInt(line.split(":")[1]);
			}
		}
		throw new RuntimeException("No score detected!");
	}
	
	public static int[] getFullCoverageAfl(String query, Oracle oracle) {
		int[] trace = null;
		for(String line : oracle.execute(query).split("\n")) {
			if(line.startsWith("TOT:")) {
				trace = new int[Integer.parseInt(line.split(":")[1])];
			} else if(line.startsWith("TRACE:")) {
				try {
					String str = line.split(":")[1];
					DataInputStream dis = new DataInputStream(new ByteArrayInputStream(str.getBytes(StandardCharsets.US_ASCII)));
					for(int i=0; i<trace.length; i++) {
						trace[i] = dis.readInt();
					}
					return trace;
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		throw new RuntimeException("No trace detected!");
	}
	
	public static interface AflCoverageOracle {
		public abstract int getCoverage(String query);
		public abstract int[] getFullCoverage(String query);
	}
	
	public static class ShellAflCoverageOracle implements AflCoverageOracle {
		private final Oracle oracle;
		
		public ShellAflCoverageOracle(Oracle oracle) {
			this.oracle = oracle;
		}
		
		@Override
		public int getCoverage(String query) {
			return getCoverageAfl(query, this.oracle);
		}
		
		@Override
		public int[] getFullCoverage(String query) {
			return getFullCoverageAfl(query, this.oracle);
		}
	}
	
	public static class WrappedAflCoverageOracle implements AflCoverageOracle {
		private final AflCoverageOracle oracle;
		private final Wrapper wrapper;
		
		public WrappedAflCoverageOracle(AflCoverageOracle oracle, Wrapper wrapper) {
			this.oracle = oracle;
			this.wrapper = wrapper;
		}
		
		@Override
		public int getCoverage(String query) {
			return this.oracle.getCoverage(this.wrapper.wrap(query));
		}
		
		@Override
		public int[] getFullCoverage(String query) {
			return this.oracle.getFullCoverage(this.wrapper.wrap(query));
		}
	}
}
