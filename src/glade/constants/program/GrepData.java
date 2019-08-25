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

package glade.constants.program;

import glade.constants.Files;
import glade.program.ProgramDataUtils.ProgramData;
import glade.program.ProgramDataUtils.ProgramExamples;
import glade.program.ProgramDataUtils.ShellProgramData;
import glade.program.ProgramDataUtils.SingleFileProgramExamples;
import glade.util.OracleUtils.IdentityWrapper;
import glade.util.OracleUtils.Wrapper;
import glade.util.ShellUtils.CommandFactory;
import glade.util.Utils.ConstantFilter;
import glade.util.Utils.Filter;

public class GrepData {
	public static final String GREP_GCOV = "grep/grep-2.23";
	public static final String GREP_EXE = "grep/grep-2.23/src/grep";
	public static final boolean GREP_IS_ERROR = true;
	
	public static final String GREP_EXAMPLE_FILENAME = "tests.txt";
	public static final String GREP_ALL_EXAMPLE_FILENAME = "tests_all.txt";
	public static final String GREP_EMPTY = "";
	
	public static final String GREP_INPUT_FILE = "data/misc/grep/file.txt";
	
	public static final String GREP_NAME = "grep";
	public static final ProgramData GREP_DATA = new ShellProgramData(Files.FILE_PARAMETERS, new GrepCommandFactory(), GREP_EXE, GREP_GCOV, GREP_IS_ERROR);
	public static final ProgramExamples GREP_EXAMPLES = new SingleFileProgramExamples(Files.FILE_PARAMETERS, GREP_NAME, GREP_EXAMPLE_FILENAME, GREP_EMPTY, new IdentityWrapper());
	public static final Filter<String> GREP_FILTER = new ConstantFilter<String>(true);
	
	public static class GrepCommandFactory implements CommandFactory {
		@Override
		public String getCommand(String filename, String exePath) {
			return exePath + " -f " + filename + " " + GREP_INPUT_FILE;
		}
	}
	
	public static class GrepAllExampleProcessor implements Wrapper {
		@Override
		public String wrap(String input) {
			return input.split(":")[1];
		}
	}
}
