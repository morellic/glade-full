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

public class FFJSData {
	public static final String FFJS_GCOV = "ffjs/mozjs-38.0.0/js/src/js/src";
	public static final String FFJS_EXE = "ffjs/mozjs-38.0.0/js/src/js/src/shell/js";
	public static final boolean FFJS_IS_ERROR = true;

	public static final String FFJS_EXTENSION = ".js";
	public static final String FFJS_EMPTY = "";
	
	public static final String FFJS_NAME = "ffjs";
	public static final String FFJS_WRAPPED_NAME = "ffjs_wrapped";
	public static final ProgramData FFJS_DATA = new ShellProgramData(Files.FILE_PARAMETERS, FFJS_EXE, FFJS_GCOV, FFJS_IS_ERROR);
	public static final ProgramData FFJS_WRAPPED_DATA = new WrappedProgramData(FFJS_DATA, new FFJSWrapper());
	public static final ProgramExamples FFJS_EXAMPLES = new MultiFileProgramExamples(Files.FILE_PARAMETERS, FFJS_NAME, FFJS_EXTENSION, FFJS_EMPTY, new IdentityWrapper());
	public static final Filter<String> FFJS_FILTER = new ConstantFilter<String>(true);
	public static final Filter<String> FFJS_WRAPPED_FILTER = new FFJSFilter();
	
	public static class FFJSWrapper implements Wrapper {
		@Override
		public String wrap(String query) {
			return "if(false){" + query + "}";
		}
	}
	
	public static class FFJSFilter implements Filter<String> {
		@Override
		public boolean filter(String example) {
			return example.startsWith("if(false){") && example.endsWith("}");
		}
	}
}
