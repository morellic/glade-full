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

import java.util.ArrayList;
import java.util.List;

import glade.constants.Files;
import glade.main.XmlFuzzer.DefaultFuzzParameters;
import glade.main.XmlFuzzer.FuzzParameters;
import glade.program.ProgramDataUtils.MultiFileProgramExamples;
import glade.program.ProgramDataUtils.ProgramData;
import glade.program.ProgramDataUtils.ProgramExamples;
import glade.program.ProgramDataUtils.ShellProgramData;
import glade.util.OracleUtils.IdentityWrapper;
import glade.util.Utils.Filter;

public class XmlData {
	public static final String XML_GCOV = "xml/libxml2-2.9.2";
	public static final String XML_EXE = "xml/libxml2-2.9.2/xmllint";
	public static final boolean XML_IS_ERROR = true;
	public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
	public static final String XML_EXTENSION = ".xml";
	public static final String XML_EMPTY = "<a/>";
	
	public static final String XML_NAME = "xml";
	public static final ProgramData XML_DATA = new ShellProgramData(Files.FILE_PARAMETERS, XML_EXE, XML_GCOV, XML_IS_ERROR);
	public static final ProgramExamples XML_EXAMPLES = new MultiFileProgramExamples(Files.FILE_PARAMETERS, XML_NAME, XML_EXTENSION, XML_EMPTY, new IdentityWrapper());
	public static final Filter<String> XML_FILTER = new Filter<String>() { public boolean filter(String s) { return !s.trim().contains("\n"); }};
	
	public static final String XML_EXAMPLE = "seed.xml";
	public static final FuzzParameters HANDWRITTEN_RESTRICTED_PARAMETERS = new DefaultFuzzParameters(2, 2, 1, "a", false, -1.0);
	public static final FuzzParameters HANDWRITTEN_PARAMETERS = new DefaultFuzzParameters(2, 2, 2, null, true, 2.0);
	public static final FuzzParameters HANDWRITTEN_ALL_PARAMETERS = new DefaultFuzzParameters(5, 5, 2, null, true, 0.1);
	
	public static List<String> getXmlChecks() {
		List<String> samples = new ArrayList<String>();
		samples.add("<a xy=\"xy\"><</a>");
		samples.add("<a xy=\"\"xy\"></a>");
		samples.add("<a =\"xy\"></a>");
		samples.add("<a x=\"x\">\"</a>");
		samples.add("<a x=\"x\"></a>");
		samples.add("<a><a><!--x--></a></a>");
		return samples;
	}
}
