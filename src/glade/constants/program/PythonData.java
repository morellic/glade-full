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
import glade.program.ProgramDataUtils.MultiFileProgramExamples;
import glade.program.ProgramDataUtils.ProgramData;
import glade.program.ProgramDataUtils.ProgramExamples;
import glade.program.ProgramDataUtils.ShellProgramData;
import glade.program.ProgramDataUtils.WrappedProgramData;
import glade.util.OracleUtils.IdentityWrapper;
import glade.util.OracleUtils.Wrapper;
import glade.util.Utils.ConstantFilter;
import glade.util.Utils.Filter;

public class PythonData {
	public static final String PYTHON_GCOV = "python/Python-2.7.10";
	public static final String PYTHON_EXE = "python/Python-2.7.10/python";
	public static final boolean PYTHON_IS_ERROR = true;
	
	public static final String PYTHON_EXTENSION = ".py";
	public static final String PYTHON_EMPTY = "";
	
	public static final String PYTHON_NAME = "python";
	public static final String PYTHON_WRAPPED_NAME = "python_wrapped";
	public static final ProgramData PYTHON_DATA = new ShellProgramData(Files.FILE_PARAMETERS, PYTHON_EXE, PYTHON_GCOV, PYTHON_IS_ERROR);
	public static final ProgramData PYTHON_WRAPPED_DATA = new WrappedProgramData(PYTHON_DATA, new PythonWrapper());
	public static final ProgramExamples PYTHON_EXAMPLES = new MultiFileProgramExamples(Files.FILE_PARAMETERS, PYTHON_NAME, PYTHON_EXTENSION, PYTHON_EMPTY, new IdentityWrapper());
	public static final Filter<String> PYTHON_FILTER = new ConstantFilter<String>(true);
	public static final Filter<String> PYTHON_WRAPPED_FILTER = new PythonFilter();
	
	public static class PythonWrapper implements Wrapper {
		@Override
		public String wrap(String input) {
			StringBuilder sb = new StringBuilder();
			
			// header
			sb.append("if False:\n");
			
			// query
			boolean isEmpty = true;
			for(String line : input.split("\n")) {
				sb.append("    ").append(line).append("\n");
				isEmpty = false;
			}
			
			// pass if needed
			if(isEmpty) {
				sb.append("    pass");
			}
			
			return sb.toString();
		}
	}
	
	public static class PythonFilter implements Filter<String> {
		@Override
		public boolean filter(String sample) {
			return sample.startsWith("if False:");
		}
	}
}
