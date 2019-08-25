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

package glade.constants;

import glade.program.LearnerDataUtils.LearnerDataParameters;
import glade.program.ProgramDataUtils.FileParameters;

public class Files {
	public static final String QUERY_PROG = "prog-query";
	public static final String GCOV_PROG = "prog-gcov";
	public static final String AFL_PROG = "prog-afl";
	
	public static final String FILENAME = "seed";
	public static final long TIMEOUT = 400;
	
	public static final String EXAMPLE_TRAIN_PATH = "data/inputs-train";
	public static final String EXAMPLE_TEST_PATH = "data/inputs-test";
	
	public static final FileParameters FILE_PARAMETERS = new FileParameters(QUERY_PROG, GCOV_PROG, AFL_PROG, FILENAME, TIMEOUT, EXAMPLE_TRAIN_PATH, EXAMPLE_TEST_PATH);
	
	public static final String GRAMMAR_PATH = "data/grammars";
	public static final String AFL_QUEUE_PATH = "data/afl-queue";
	public static final String RPNI_PATH = "data/rpni";
	public static final String LSTAR_PATH = "data/lstar";
	public static final LearnerDataParameters LEARNER_DATA_PARAMETERS = new LearnerDataParameters(GRAMMAR_PATH, AFL_QUEUE_PATH, RPNI_PATH, LSTAR_PATH);
}
