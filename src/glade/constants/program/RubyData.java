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

import java.io.File;

import glade.constants.Files;
import glade.program.ProgramDataUtils.MultiFileProgramExamples;
import glade.program.ProgramDataUtils.ProgramData;
import glade.program.ProgramDataUtils.ProgramExamples;
import glade.program.ProgramDataUtils.ShellProgramData;
import glade.program.ProgramDataUtils.WrappedProgramData;
import glade.util.OracleUtils.IdentityWrapper;
import glade.util.OracleUtils.Wrapper;
import glade.util.ShellUtils.CommandFactory;
import glade.util.Utils.ConstantFilter;
import glade.util.Utils.Filter;

public class RubyData {
	public static final String RUBY_GCOV = "ruby/ruby-2.3.0";
	public static final String RUBY_EXE = "ruby/ruby-2.3.0";
	public static final boolean RUBY_IS_ERROR = true;
	
	public static final String RUBY_EXTENSION = ".rb";
	public static final String RUBY_EMPTY = "";
	
	public static final String RUBY_NAME = "ruby";
	public static final String RUBY_WRAPPED_NAME = "ruby_wrapped";
	public static final ProgramData RUBY_DATA = new ShellProgramData(Files.FILE_PARAMETERS, new RubyCommandFactory(), RUBY_EXE, RUBY_GCOV, RUBY_IS_ERROR);
	public static final ProgramData RUBY_WRAPPED_DATA = new WrappedProgramData(RUBY_DATA, new RubyWrapper());
	public static final ProgramExamples RUBY_EXAMPLES = new MultiFileProgramExamples(Files.FILE_PARAMETERS, RUBY_NAME, RUBY_EXTENSION, RUBY_EMPTY, new IdentityWrapper());
	public static final Filter<String> RUBY_FILTER = new ConstantFilter<String>(true);
	public static final Filter<String> RUBY_WRAPPED_FILTER = new RubyFilter();
	
	public static class RubyWrapper implements Wrapper {
		@Override
		public String wrap(String input) {
			StringBuilder sb = new StringBuilder();
			
			// header
			sb.append("if false then\n");
			
			// query
			sb.append(input);
			
			// footer
			sb.append("\nend");
			
			return sb.toString();
		}
	}
	
	public static class RubyFilter implements Filter<String> {
		@Override
		public boolean filter(String sample) {
			return sample.trim().startsWith("if false then") && sample.trim().endsWith("end");
		}
	}
	
	public static class RubyCommandFactory implements CommandFactory {
		@Override
		public String getCommand(String filename, String exePath) {
			StringBuilder sb = new StringBuilder();
			sb.append(exePath + File.separator + "ruby ");
			sb.append("-I" + exePath + " ");
			sb.append("-I" + exePath + File.separator + "lib ");
			sb.append("-I" + exePath + File.separator + ".ext/x86_64-darwin13 ");
			sb.append(filename);
			return sb.toString();
		}
	}
}
