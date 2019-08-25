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
import glade.util.OracleUtils.IdentityWrapper;
import glade.util.Utils.ConstantFilter;
import glade.util.Utils.Filter;

public class BisonData {
	public static final String BISON_GCOV = "bison/bison-3.0/";
	public static final String BISON_EXE = "bison/bison-3.0/src/bison";
	public static final boolean BISON_IS_ERROR = true;
	
	public static final String BISON_EXTENSION = ".y";
	public static final String BISON_EMPTY = "%%\ninput:\n;\n%%";
	
	public static final String BISON_NAME = "bison";
	public static final ProgramData BISON_DATA = new ShellProgramData(Files.FILE_PARAMETERS, BISON_EXE, BISON_GCOV, BISON_IS_ERROR);
	public static final ProgramExamples BISON_EXAMPLES = new MultiFileProgramExamples(Files.FILE_PARAMETERS, BISON_NAME, BISON_EXTENSION, BISON_EMPTY, new IdentityWrapper());
	public static final Filter<String> BISON_FILTER = new ConstantFilter<String>(true);
}
