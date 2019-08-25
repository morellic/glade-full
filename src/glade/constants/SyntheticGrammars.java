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

import java.util.ArrayList;
import java.util.List;

import glade.grammar.MultiGrammarUtils.MultiGrammar;
import glade.grammar.MultiGrammarUtils.MultiProduction;
import glade.main.XmlFuzzer;
import glade.util.CharacterUtils;

public class SyntheticGrammars {
	public static String getSimpleParenthesesGrammarName() {
		return "simple";
	}
	
	public static MultiGrammar getSimpleParenthesesGrammar() {
		List<MultiProduction> productions = new ArrayList<MultiProduction>();
		productions.add(new MultiProduction("S", new Object[]{}));
		productions.add(new MultiProduction("S", new Object[]{'(', "S", ')'}));
		productions.add(new MultiProduction("S", new Object[]{"S", "S"}));
		return new MultiGrammar(productions, "S");
	}
	
	public static String getParenthesesGrammarName() {
		return "paren";
	}
	
	private static final int PAREN_NUM = 5;
	public static MultiGrammar getParenthesesGrammar() {
		List<MultiProduction> productions = new ArrayList<MultiProduction>();
		productions.add(new MultiProduction("S", new Object[]{}));
		productions.add(new MultiProduction("S", new Object[]{"S", "S"}));
		String values = "0123456789";
		for(int i=0; i<PAREN_NUM; i++) {
			productions.add(new MultiProduction("S", new Object[]{'(', values.charAt(i), "S", ')', values.charAt(i)}));
		}
		return new MultiGrammar(productions, "S");
	}
	
	/*
	 * 
	 * REGCHAR: a | 0 // [^()\+*]
	 * 
	 * BKSLCHAR: \
	 * LPCHAR: (
	 * RPCHAR: )
	 * PLCHAR: +
	 * ASCHAR: *
	 * 
	 * BKBKSLCHAR: \\
	 * BKLPCHAR: \(
	 * BKRPCHAR: \)
	 * BKPLCHAR: \+
	 * BKASCHAR: \*
	 * 
	 * REGTOK: REGCHAR | BKBKSLCHAR | BKLPCHAR | BKRPCHAR | BKPLCHAR | BKASCHAR
	 * 
	 * REGEX: REGTOK+
	 * REGEX: LPCHAR REGEX (PLCHAR REGEX)* RPCHAR
	 * REGEX: LPCHAR REGEX RPCHAR ASCHAR
	 * REGEX: REGEX REGEX
	 * 
	 */
	public static String getRegexGrammarName() {
		return "regex";
	}
	
	public static MultiGrammar getRegexGrammar() {
		List<MultiProduction> productions = new ArrayList<MultiProduction>();
		
		// STEP 1: Characters
		String REGCHAR = "<REGCHAR>";
		productions.add(new MultiProduction(REGCHAR, new Object[]{'a'}));
		productions.add(new MultiProduction(REGCHAR, new Object[]{'0'}));
		
		// STEP 2: Special characters
		String BKSLCHAR = "<BKSLCHAR>";
		String LPCHAR = "<LPCHAR>";
		String RPCHAR = "<RPCHAR>";
		String PLCHAR = "<PLCHAR>";
		String ASCHAR = "<ASCHAR>";
		String BKSLBKSLCHAR = "<BKBKSLCHAR>";
		
		productions.add(new MultiProduction(BKSLCHAR, new Object[]{'\\'}));
		productions.add(new MultiProduction(LPCHAR, new Object[]{'\\', '('}));
		productions.add(new MultiProduction(RPCHAR, new Object[]{'\\', ')'}));
		productions.add(new MultiProduction(PLCHAR, new Object[]{'\\', '+'}));
		productions.add(new MultiProduction(ASCHAR, new Object[]{'\\', '*'}));
		productions.add(new MultiProduction(BKSLBKSLCHAR, new Object[]{'\\', '\\'}));
		
		// STEP 3: Tokens
		String REGTOK = "<REGTOK>";
		String REGEX = "<REGEX>";
		String REGEXPRE = "<REGEXPRE>";
		
		productions.add(new MultiProduction(REGTOK, new Object[]{}));
		productions.add(new MultiProduction(REGTOK, new Object[]{REGCHAR}));
		productions.add(new MultiProduction(REGTOK, new Object[]{BKSLBKSLCHAR}));

		productions.add(new MultiProduction(REGEX, new Object[]{REGTOK}));
		productions.add(new MultiProduction(REGEX, new Object[]{REGTOK}));
		productions.add(new MultiProduction(REGEX, new Object[]{REGTOK}));
		productions.add(new MultiProduction(REGEX, new Object[]{REGTOK}));
		productions.add(new MultiProduction(REGEX, new Object[]{REGTOK}));
		productions.add(new MultiProduction(REGEX, new Object[]{REGEX, REGTOK}));
		productions.add(new MultiProduction(REGEXPRE, new Object[]{LPCHAR, REGEX}));
		productions.add(new MultiProduction(REGEXPRE, new Object[]{REGEXPRE, PLCHAR, REGEX}));
		productions.add(new MultiProduction(REGEX, new Object[]{REGEXPRE, RPCHAR}));
		productions.add(new MultiProduction(REGEX, new Object[]{LPCHAR, REGEX, RPCHAR, ASCHAR}));
		productions.add(new MultiProduction(REGEX, new Object[]{REGEX, REGEX}));

		return new MultiGrammar(productions, "<REGEX>");
	}
	
	/*
	 * 
	 * ATTRNAMECHAR
	 * ATTRVALCHAR
	 * TEXTCHAR
	 * DATACHAR
	 * CMTCHAR
	 * PITGTCHAR
	 * PIDATACHAR
	 * ENTREFCHAR
	 * 
	 * WSCHAR: [ \t]
	 * LACHAR: <
	 * RACHAR: >
	 * SLCHAR: /
	 * NLCHAR: \n
	 * QTCHAR: "
	 * EQCHAR: =
	 * QMCHAR: ?
	 * DSCHAR: -
	 * EMCHAR: !
	 * LSQCHAR: [
	 * RSQCHAR: ]
	 * CCHAR: C
	 * DCHAR: D
	 * ACHAR: A
	 * TCHAR: T
	 * AMPCHAR: &
	 * SCCHAR: ;
	 * PDCHAR: #
	 * NUMCHAR: [0-9]
	 * HEXCHAR: [0-9A-E]
	 * 
	 * WSTOK: (WSCHAR | NLCHAR)+
	 * ATTRNAMETOK: ATTRNAMECHAR+
	 * ATTRVALTOK: QTCHAR ATTRVALCHAR* QTCHAR
	 * DATATOK: LACHAR EMCHAR LSQCHAR CCHAR DCHAR 'a' TCHAR ACHAR LSQCHAR DATACHAR* RSQCHAR RSQCHAR RACHAR
	 * CMTTOK: LACHAR EMCHAR DSCHAR DCHAR CMTCHAR* DSCHAR DSCHAR RACHAR
	 * PITOK: LACHAR QMCHAR PITGTCHAR* WSTOK PIDATACHAR* QMCHAR
	 * ENTREFTOK: AMCHAR 'a' 'm' 'p' SCCHAR | AMCHAR 'l' 't' SCHAR | AMCHAR 'g' 't' SCCHAR
	 *            | AMPCHAR 'q' 'u' 'o' 't' SCCHAR | AMPCHAR 'a' 'p' 'o' 's' SCCHAR
	 *            | AMPCHAR PDCHAR NUMCHAR SCCHAR | AMPCHAR PDCHAR NUMCHAR NUMCHAR SCCHAR | AMPCHAR PDCHAR NUMCHAR NUMCHAR NUMCHAR SCCHAR
	 *            | AMPCHAR PDCHAR 'x' HEXCHAR SCCHAR | AMPCHAR PDCHAR 'x' HEXCHAR HEXCHAR SCCHAR
	 * LTAGTOK: LACHAR 'a' (WSTOK ATTRNAMETOK EQCHAR ATTRVALTOK)* RACHAR
	 * RTAGTOK: LACHAR SLCHAR ACHAR RACHAR
	 * ELEMTOK: LTAGTOK (WSTOK | TEXTCHAR | DATATOK | CMTTOK | PITOK | ENTREFTOK | ELEMTOK)* RTAGTOK
	 * 
	 */
	public static String getXmlGrammarName() {
		return "xml";
	}
	
	public static MultiGrammar getXmlGrammar() {
		List<MultiProduction> productions = new ArrayList<MultiProduction>();
		
		// STEP 1: Character classes
		String ATTRNAMECHAR = "<ATTRNAMECHAR>";
		String ATTRVALCHAR = "<ATTRVALCHAR>";
		String ELEMNAMECHAR = "<ELEMNAMECHAR>";
		String TEXTCHAR = "<TEXTCHAR>";
		String DATACHAR = "<DATACHAR>";
		String CMTCHAR = "<CMTCHAR>";
		String PITGTCHAR = "<PITGTCHAR>";
		String PIDATACHAR = "<PIDATACHAR>";
		String ENTREFCHAR = "<ENTREFCHAR>";
		String NUMCHAR = "<NUMCHAR>";
		String HEXCHAR = "<HEXCHAR>";
		for(char c : CharacterUtils.getAsciiCharacters()) {
			if(!XmlFuzzer.attrNameFilter.contains(c)) {
				productions.add(new MultiProduction(ATTRNAMECHAR, new Object[]{(Character)c}));
			}
			if(!XmlFuzzer.attrValueFilter.contains(c)) {
				productions.add(new MultiProduction(ATTRVALCHAR, new Object[]{(Character)c}));
			}
			if(!XmlFuzzer.elementNameFilter.contains(c)) {
				productions.add(new MultiProduction(ELEMNAMECHAR, new Object[]{(Character)c}));
			}
			if(!XmlFuzzer.textFilter.contains(c)) {
				productions.add(new MultiProduction(TEXTCHAR, new Object[]{(Character)c}));
			}
			if(!XmlFuzzer.dataFilter.contains(c)) {
				productions.add(new MultiProduction(DATACHAR, new Object[]{(Character)c}));
			}
			if(!XmlFuzzer.commentFilter.contains(c)) {
				productions.add(new MultiProduction(CMTCHAR, new Object[]{(Character)c}));
			}
			if(!XmlFuzzer.piTargetFilter.contains(c)) {
				productions.add(new MultiProduction(PITGTCHAR, new Object[]{(Character)c}));
			}
			if(!XmlFuzzer.piDataFilter.contains(c)) {
				productions.add(new MultiProduction(PIDATACHAR, new Object[]{(Character)c}));
			}
			if(!XmlFuzzer.entityReferenceFilter.contains(c)) {
				productions.add(new MultiProduction(ENTREFCHAR, new Object[]{(Character)c}));
			}
			if(CharacterUtils.isNumeric(c)) {
				productions.add(new MultiProduction(NUMCHAR, new Object[]{((Character)c)}));
			}
			if(CharacterUtils.isNumeric(c) || c >= 'A' || c <= 'C') {
				productions.add(new MultiProduction(HEXCHAR, new Object[]{((Character)c)}));
			}
		}
		
		// STEP 2: Special characters
		String WSCHAR = "<WSCHAR>";
		String LACHAR = "<LACHAR>";
		String RACHAR = "<RACHAR>";
		String SLCHAR = "<SLCHAR>";
		String NLCHAR = "<NLCHAR>";
		String QTCHAR = "<QTCHAR>";
		String EQCHAR = "<EQCHAR>";
		String QMCHAR = "<QMCHAR>";
		String DSCHAR = "<DSCHAR>";
		String EMCHAR = "<EMCHAR>";
		String LSQCHAR = "<LSQCHAR>";
		String RSQCHAR = "<RSQCHAR>";
		String CCHAR = "<CCHAR>";
		String DCHAR = "<DCHAR>";
		String ACHAR = "<ACHAR>";
		String TCHAR = "<TCHAR>";
		String AMPCHAR = "<AMPCHAR>";
		String SCCHAR = "<SCCHAR>";
		String PDCHAR = "<PDCHAR>";
		
		productions.add(new MultiProduction(WSCHAR, new Object[]{' '}));
		productions.add(new MultiProduction(WSCHAR, new Object[]{'\t'}));
		productions.add(new MultiProduction(LACHAR, new Object[]{'<'}));
		productions.add(new MultiProduction(RACHAR, new Object[]{'>'}));
		productions.add(new MultiProduction(SLCHAR, new Object[]{'/'}));
		productions.add(new MultiProduction(NLCHAR, new Object[]{'\n'}));
		productions.add(new MultiProduction(QTCHAR, new Object[]{'"'}));
		productions.add(new MultiProduction(EQCHAR, new Object[]{'='}));
		productions.add(new MultiProduction(QMCHAR, new Object[]{'?'}));
		productions.add(new MultiProduction(DSCHAR, new Object[]{'-'}));
		productions.add(new MultiProduction(EMCHAR, new Object[]{'!'}));
		productions.add(new MultiProduction(LSQCHAR, new Object[]{'['}));
		productions.add(new MultiProduction(RSQCHAR, new Object[]{']'}));
		productions.add(new MultiProduction(CCHAR, new Object[]{'C'}));
		productions.add(new MultiProduction(DCHAR, new Object[]{'D'}));
		productions.add(new MultiProduction(ACHAR, new Object[]{'A'}));
		productions.add(new MultiProduction(TCHAR, new Object[]{'T'}));
		productions.add(new MultiProduction(AMPCHAR, new Object[]{'&'}));
		productions.add(new MultiProduction(SCCHAR, new Object[]{';'}));
		productions.add(new MultiProduction(PDCHAR, new Object[]{'#'}));
		
		// STEP 3: Tokens
		String WSTOK = "<WSTOK>";
		String ATTRNAMETOK = "<ATTRNAMETOK>";
		String ATTRVALTOK = "<ATTRVALTOK>";
		String ATTRVALPRETOK = "<ATTRVALPRETOK>";
		String TEXTTOK = "<TEXTTOK>";
		String DATATOK = "<DATATOK>";
		String DATAPRETOK = "<DATAPRETOK>";
		String CMTTOK = "<CMTTOK>";
		String CMTPRETOK = "<CMTPRETOK>";
		String PITOK = "<PITOK>";
		String PIPRETOK1 = "<PIPRETOK1>";
		String PIPRETOK2 = "<PIPRETOK2>";
		String ENTREFTOK = "<ENTREFTOK>";
		String LTAGTOK = "<LTAGTOK>";
		String LTAGPRETOK = "<LTAGPRETOK>";
		String RTAGTOK = "<RTAGTOK>";
		String ELEMTOK = "<ELEMTOK>";
		String ELEMPRETOK = "<ELEMPRETOK>";
		
		productions.add(new MultiProduction(WSTOK, new Object[]{WSCHAR}));
		productions.add(new MultiProduction(WSTOK, new Object[]{WSTOK, WSCHAR}));
		productions.add(new MultiProduction(ATTRNAMETOK, new Object[]{ATTRNAMECHAR}));
		productions.add(new MultiProduction(ATTRNAMETOK, new Object[]{ATTRNAMETOK, ATTRNAMECHAR}));
		productions.add(new MultiProduction(ATTRVALPRETOK, new Object[]{QTCHAR}));
		productions.add(new MultiProduction(ATTRVALPRETOK, new Object[]{ATTRVALPRETOK, ATTRVALCHAR}));
		productions.add(new MultiProduction(ATTRVALTOK, new Object[]{ATTRVALPRETOK, QTCHAR}));
		productions.add(new MultiProduction(TEXTTOK, new Object[]{TEXTCHAR}));
		productions.add(new MultiProduction(TEXTTOK, new Object[]{TEXTTOK, TEXTCHAR}));
		productions.add(new MultiProduction(DATAPRETOK, new Object[]{LACHAR, EMCHAR, LSQCHAR, CCHAR, DCHAR, ACHAR, TCHAR, ACHAR, LSQCHAR}));
		productions.add(new MultiProduction(DATAPRETOK, new Object[]{DATAPRETOK, DATACHAR}));
		productions.add(new MultiProduction(DATATOK, new Object[]{DATAPRETOK, RSQCHAR, RSQCHAR, RACHAR}));
		productions.add(new MultiProduction(CMTPRETOK, new Object[]{LACHAR, EMCHAR, DSCHAR, DSCHAR}));
		productions.add(new MultiProduction(CMTPRETOK, new Object[]{CMTPRETOK, CMTCHAR}));
		productions.add(new MultiProduction(CMTTOK, new Object[]{CMTPRETOK, DSCHAR, DSCHAR, RACHAR}));
		productions.add(new MultiProduction(PIPRETOK1, new Object[]{LACHAR, QMCHAR}));
		productions.add(new MultiProduction(PIPRETOK1, new Object[]{PIPRETOK1, PITGTCHAR}));
		productions.add(new MultiProduction(PIPRETOK2, new Object[]{PIPRETOK1, WSTOK}));
		productions.add(new MultiProduction(PIPRETOK2, new Object[]{PIPRETOK2, PIDATACHAR}));
		productions.add(new MultiProduction(PITOK, new Object[]{PIPRETOK2, QMCHAR}));
		productions.add(new MultiProduction(ENTREFTOK, new Object[]{AMPCHAR, 'a', 'm', 'p', SCCHAR}));
		productions.add(new MultiProduction(ENTREFTOK, new Object[]{AMPCHAR, 'q', 'u', 'o', 't', SCCHAR}));
		productions.add(new MultiProduction(ENTREFTOK, new Object[]{AMPCHAR, 'l', 't', SCCHAR}));
		productions.add(new MultiProduction(ENTREFTOK, new Object[]{AMPCHAR, 'g', 't', SCCHAR}));
		productions.add(new MultiProduction(ENTREFTOK, new Object[]{AMPCHAR, 'a', 'p', 'o', 's', SCCHAR}));
		productions.add(new MultiProduction(ENTREFTOK, new Object[]{AMPCHAR, PDCHAR, NUMCHAR, SCCHAR}));
		productions.add(new MultiProduction(ENTREFTOK, new Object[]{AMPCHAR, PDCHAR, NUMCHAR, NUMCHAR, SCCHAR}));
		productions.add(new MultiProduction(ENTREFTOK, new Object[]{AMPCHAR, PDCHAR, NUMCHAR, NUMCHAR, NUMCHAR, SCCHAR}));
		productions.add(new MultiProduction(ENTREFTOK, new Object[]{AMPCHAR, PDCHAR, 'x', HEXCHAR, SCCHAR}));
		productions.add(new MultiProduction(ENTREFTOK, new Object[]{AMPCHAR, PDCHAR, 'x', HEXCHAR, HEXCHAR, SCCHAR}));
		productions.add(new MultiProduction(LTAGPRETOK, new Object[]{LACHAR, 'a'}));
		productions.add(new MultiProduction(LTAGPRETOK, new Object[]{LTAGPRETOK, WSTOK, ATTRNAMETOK, EQCHAR, ATTRVALTOK}));
		productions.add(new MultiProduction(LTAGTOK, new Object[]{LTAGPRETOK, RACHAR}));
		productions.add(new MultiProduction(RTAGTOK, new Object[]{LACHAR, SLCHAR, 'a', RACHAR}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{LTAGTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, WSTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, TEXTCHAR}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, DATATOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, CMTTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, PITOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, ENTREFTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{LTAGTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, WSTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, TEXTCHAR}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, DATATOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, CMTTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, PITOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, ENTREFTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{LTAGTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, WSTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, TEXTCHAR}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, DATATOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, CMTTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, PITOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, ENTREFTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{LTAGTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, WSTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, TEXTCHAR}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, DATATOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, CMTTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, PITOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, ENTREFTOK}));
		productions.add(new MultiProduction(ELEMPRETOK, new Object[]{ELEMPRETOK, ELEMTOK}));
		productions.add(new MultiProduction(ELEMTOK, new Object[]{ELEMPRETOK, RTAGTOK}));

		return new MultiGrammar(productions, "<ELEMTOK>");
	}

	/*
	 * 
	 * CMTCHAR: a | 0 // .
	 * STRCHAR: a | 0 // [^"]
	 * NAMECHAR: a | 0 // [^()"; \t\]
	 * WSCHAR: [ \t]
	 * NLCHAR: \n
	 * SCCHAR: ;
	 * QTCHAR: "
	 * OPCHAR: (
	 * CPCHAR: )
	 * BKSLCHAR: \
	 * 
	 * NAMETOK: NAMECHAR+
	 * STRTOK: QTCHAR (STRCHAR | BKSLCHAR QTCHAR)* QTCHAR
	 * CMTTOK: SC CMTCHAR*
	 * WSTOK: (WSCHAR | NLCHAR | CMTTOK)
	 * 
	 * LISTEXPR: NAMETOK | STRTOK | SEXPR
	 * SEXPR -> OPCHAR NAMETOK (WSTOK LISTEXPR)* CPCHAR | SEXPR SEXPR
	 * 
	 */
	public static String getLispGrammarName() {
		return "lisp";
	}
	
	public static MultiGrammar getLispGrammar() {
		List<MultiProduction> productions = new ArrayList<MultiProduction>();
		
		// STEP 1: Character classes
		String CMTCHAR = "<CMTCHAR>";
		String STRCHAR = "<STRCHAR>";
		String NAMECHAR = "<NAMECHAR>";
		String WSCHAR = "<WSCHAR>";
		for(char c : CharacterUtils.getAsciiCharacters()) {
			if(c == '\n') {
				throw new RuntimeException();
			}
			if(c != 'a' && c != '0') {
				continue;
			}
			if(true) {
				productions.add(new MultiProduction(CMTCHAR, new Object[]{(Character)c}));
			}
			if(c != '"') {
				productions.add(new MultiProduction(STRCHAR, new Object[]{(Character)c}));
			}
			if(c == ' ' || c == '\t' ) {
				productions.add(new MultiProduction(WSCHAR, new Object[]{(Character)c}));
			}
			if(c != '(' && c != ')' && c != '"' && c != ';' && c != '\t' && c != '\\' && c != ' ') {
				productions.add(new MultiProduction(NAMECHAR, new Object[]{(Character)c}));
			}
		}
		
		// STEP 2: Special characters
		String NLCHAR = "<NLCHAR>";
		String SCCHAR = "<SCCHAR>";
		String QTCHAR = "<QTCHAR>";
		String OPCHAR = "<OPCHAR>";
		String CPCHAR = "<CPCHAR>";
		String BKSLCHAR = "<BKSLCHAR>";
		
		productions.add(new MultiProduction(NLCHAR, new Object[]{(Character)'\n'}));
		productions.add(new MultiProduction(SCCHAR, new Object[]{(Character)';'}));
		productions.add(new MultiProduction(QTCHAR, new Object[]{(Character)'"'}));
		productions.add(new MultiProduction(OPCHAR, new Object[]{(Character)'('}));
		productions.add(new MultiProduction(CPCHAR, new Object[]{(Character)')'}));
		productions.add(new MultiProduction(BKSLCHAR, new Object[]{(Character)'\\'}));
		
		// STEP 2: Tokens
		String NAMETOK = "<NAMETOK>";
		String STRPRETOK = "<STRPRETOK>";
		String STRTOK = "<STRTOK>";
		String CMTPRETOK = "<CMTPRETOK>";
		String CMTTOK = "<CMTTOK>";
		String WSTOK = "<WSTOK>";
		
		productions.add(new MultiProduction(NAMETOK, new Object[]{NAMECHAR}));
		productions.add(new MultiProduction(NAMETOK, new Object[]{NAMETOK, NAMECHAR}));
		
		productions.add(new MultiProduction(STRPRETOK, new Object[]{QTCHAR}));
		productions.add(new MultiProduction(STRPRETOK, new Object[]{STRPRETOK, STRCHAR}));
		productions.add(new MultiProduction(STRTOK, new Object[]{STRPRETOK, QTCHAR}));
		
		productions.add(new MultiProduction(CMTPRETOK, new Object[]{SCCHAR}));
		productions.add(new MultiProduction(CMTPRETOK, new Object[]{CMTPRETOK, CMTCHAR}));
		productions.add(new MultiProduction(CMTTOK, new Object[]{CMTPRETOK, NLCHAR}));
		
		productions.add(new MultiProduction(WSTOK, new Object[]{WSCHAR}));
		productions.add(new MultiProduction(WSTOK, new Object[]{WSTOK, WSCHAR}));
		productions.add(new MultiProduction(WSTOK, new Object[]{WSTOK, NLCHAR}));
		productions.add(new MultiProduction(WSTOK, new Object[]{WSTOK, CMTTOK}));
		
		// STEP 4: Programs
		String LISTEXPR = "<LISTEXPR>";
		String SPREEXPR = "<SPREEXPR>";
		String SEXPR = "<SEXPR>";
		
		for(int i=0; i<40; i++) {
			productions.add(new MultiProduction(LISTEXPR, new Object[]{NAMETOK}));
			productions.add(new MultiProduction(LISTEXPR, new Object[]{STRTOK}));
		}
		productions.add(new MultiProduction(LISTEXPR, new Object[]{SEXPR}));
		
		productions.add(new MultiProduction(SPREEXPR, new Object[]{OPCHAR, NAMETOK}));
		productions.add(new MultiProduction(SPREEXPR, new Object[]{SPREEXPR, WSTOK, LISTEXPR}));
		productions.add(new MultiProduction(SEXPR, new Object[]{SPREEXPR, CPCHAR}));
		productions.add(new MultiProduction(SEXPR, new Object[]{SEXPR, SEXPR}));
		
		return new MultiGrammar(productions, "<SEXPR>");
	}
}
