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

public class GraphvizData {
	public static final String GRAPHVIZ_GCOV = "graphviz/graphviz-2.38.0";
	public static final String GRAPHVIZ_EXE = "graphviz/graphviz/bin/dot";
	public static final boolean GRAPHVIZ_IS_ERROR = true;
	
	public static final String GRAPHVIZ_EXTENSION = ".dot";
	public static final String GRAPHVIZ_EMPTY = "graph g {}";
	
	public static final String GRAPHVIZ_NAME = "graphviz";
	public static final ProgramData GRAPHVIZ_DATA = new ShellProgramData(Files.FILE_PARAMETERS, GRAPHVIZ_EXE, GRAPHVIZ_GCOV, GRAPHVIZ_IS_ERROR);
	public static final ProgramExamples GRAPHVIZ_EXAMPLES = new MultiFileProgramExamples(Files.FILE_PARAMETERS, GRAPHVIZ_NAME, GRAPHVIZ_EXTENSION, GRAPHVIZ_EMPTY, new IdentityWrapper());
	public static final Filter<String> GRAPHVIZ_FILTER = new ConstantFilter<String>(true);
}
