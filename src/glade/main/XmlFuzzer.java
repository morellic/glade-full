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

package glade.main;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import glade.constants.program.XmlData;
import glade.grammar.fuzz.GrammarTraceFuzzer.GrammarProblem;
import glade.grammar.fuzz.GrammarTraceFuzzer.StructuredExample;
import glade.util.CharacterUtils;
import glade.util.CharacterUtils.CharacterGeneralization;
import glade.util.IteratorUtils.Sampler;

public class XmlFuzzer {
	public static class XmlSampler implements Sampler {
		private final FuzzParameters parameters;
		private final Random random;
		
		public XmlSampler(FuzzParameters parameters, Random random) {
			this.parameters = parameters;
			this.random = random;
		}

		@Override
		public String sample() {
			return nextSample(this.parameters, this.random);
		}
	}
	
	public static class XmlMutationSampler implements Sampler {
		private final List<String> examples = new ArrayList<String>();
		private final FuzzParameters parameters;
		private final Random random;
		private final int maxLength;
		
		public XmlMutationSampler(FuzzParameters parameters, Iterable<String> examples, int maxLength, Random random) {
			for(String example : examples) {
				this.examples.add(example);
			}
			this.parameters = parameters;
			this.random = random;
			this.maxLength = maxLength;
		}

		@Override
		public String sample() {
			int choice = this.random.nextInt(this.examples.size());
			Document document = getDocument(this.examples.get(choice));
			DocumentExample mutant = new DocumentExample(nextMutationSample(this.parameters, document, this.random));
			return mutant.getExample().length() <= this.maxLength ? mutant.getExample() : sample();
		}
	}
	
	public static class DocumentExample implements StructuredExample {
		private final Document document;
		
		public DocumentExample(Document document) {
			this.document = document;
		}
		
		@Override
		public String getExample() {
			return XmlFuzzer.toString(this.document);
		}
		
		@Override
		public String toString() {
			return getExample();
		}
	}
	
	public static class HandwrittenGrammarProblem implements GrammarProblem<DocumentExample> {
		private final FuzzParameters parameters;
		private final Random random;
		private final int maxLength;
		
		public HandwrittenGrammarProblem(FuzzParameters parameters, int maxLength, Random random) {
			this.parameters = parameters;
			this.maxLength = maxLength;
			this.random = random;
		}
		
		@Override
		public DocumentExample seed() {
			DocumentExample seed = new DocumentExample(nextDocumentLoop(this.parameters, this.random));
			return seed.getExample().length() <= this.maxLength ? seed : seed();
		}
		
		private DocumentExample sampleHelper(DocumentExample seed) {
			Document document = (Document)seed.document.cloneNode(true);
			DocumentExample mutant = new DocumentExample(nextMutationSample(this.parameters, document, this.random));
			return mutant.getExample().length() <= this.maxLength ? mutant : sampleHelper(seed);
		}
		
		@Override
		public DocumentExample sample(DocumentExample seed) {
			return sampleHelper(seed);
		}
	}
	
	public static interface FuzzParameters {
		public abstract String nextString(Random random, Set<Character> filter);
		public abstract int nextRepetition(Random random);
		public abstract int nextAttributeRepetition(Random random);
		public abstract int nextChoice(Random random, int numChoices);
		public abstract String nextElementName(Random random, Set<Character> filter);
	}
	
	public static final Set<Character> attrNameFilter = new HashSet<Character>();
	public static final Set<Character> attrValueFilter = new HashSet<Character>();
	public static final Set<Character> elementNameFilter = new HashSet<Character>();
	public static final Set<Character> textFilter = new HashSet<Character>();
	public static final Set<Character> dataFilter = new HashSet<Character>();
	public static final Set<Character> commentFilter = new HashSet<Character>();
	public static final Set<Character> piTargetFilter = new HashSet<Character>();
	public static final Set<Character> piDataFilter = new HashSet<Character>();
	public static final Set<Character> entityReferenceFilter = new HashSet<Character>();
	
	public static class DefaultFuzzParameters implements FuzzParameters {
		private final int maxRepetitions;
		private final int maxAttributes;
		private final int maxStringLength;
		private final String constantName; // if null, randomize
		private final boolean useNonAlphaNumeric;
		private final double pAllCharacters;
		
		public DefaultFuzzParameters(int maxStringLength, int maxRepetitions, int maxAttributes, String constantName, boolean useNonAlphaNumeric, double pAllCharacters) {
			this.maxRepetitions = maxRepetitions;
			this.maxAttributes = maxAttributes;
			this.maxStringLength = maxStringLength;
			this.constantName = constantName;
			this.useNonAlphaNumeric = useNonAlphaNumeric;
			this.pAllCharacters = pAllCharacters;
		}
		
		public String nextString(Random random, Set<Character> filter) {
			boolean useAllCharacters = random.nextDouble() < this.pAllCharacters;
			List<Character> characters = new ArrayList<Character>();
			if(this.useNonAlphaNumeric) {
				for(CharacterGeneralization generalization : CharacterUtils.getGeneralizations()) {
					if(useAllCharacters) {
						for(char c : generalization.characters) {
							if(!filter.contains(c)) {
								characters.add(c);
							}
						}
					} else {
						for(char c : generalization.checks) {
							if(!filter.contains(c)) {
								characters.add(c);
							}
						}
					}
				}
			} else {
				for(char c : CharacterUtils.getAsciiCharacters()) {
					if(!CharacterUtils.isNonAlphaNumeric(c) && !filter.contains(c)) {
						characters.add(c);
					}
				}
			}
			StringBuilder sb = new StringBuilder();
			while(sb.length() < this.maxStringLength) {
				sb.append(characters.get(random.nextInt(characters.size())));
			}
			return sb.toString();
		}
		
		public int nextRepetition(Random random) {
			return random.nextInt(this.maxRepetitions);
		}
		
		public int nextAttributeRepetition(Random random) {
			return random.nextInt(this.maxAttributes);
		}
		
		public int nextChoice(Random random, int numChoices) {
			return random.nextInt(numChoices);
		}
		
		public String nextElementName(Random random, Set<Character> filter) {
			return this.constantName != null ? this.constantName : this.nextString(random, filter);
		}
	}
	
	public static Attr randAttr(Document doc, FuzzParameters params, Random random) {
		Attr attr = doc.createAttribute(params.nextString(random, attrNameFilter));
		attr.setValue(params.nextString(random, attrValueFilter));
		return attr;
	}
	
	public static Element nextElement(Document doc, FuzzParameters params, Random random, boolean isRoot) {
		Element element = doc.createElement(params.nextElementName(random, elementNameFilter));
		
		// Attributes
		int numAttrs = params.nextAttributeRepetition(random);
		for(int i=0; i<numAttrs; i++) {
			element.setAttributeNode(randAttr(doc, params, random));
		}

		// Children
		int numChildren = isRoot && random.nextDouble() < 0.9 ? 10 : params.nextRepetition(random);
		for(int i=0; i<numChildren; i++) {
			element.appendChild(nextChild(doc, params, random));
		}
		
		return element;
	}
	
	public static Text nextText(Document doc, FuzzParameters params, Random random) {
		return doc.createTextNode(params.nextString(random, textFilter));
	}
	
	public static CDATASection nextData(Document doc, FuzzParameters params, Random random) {
		return doc.createCDATASection(params.nextString(random, dataFilter));
	}
	
	public static Comment nextComment(Document doc, FuzzParameters params, Random random) {
		return doc.createComment(params.nextString(random, commentFilter));
	}
	
	public static ProcessingInstruction nextPI(Document doc, FuzzParameters params, Random random) {
		return doc.createProcessingInstruction(params.nextString(random, piTargetFilter), params.nextString(random, piDataFilter));
	}
	
	public static EntityReference nextEntityReference(Document doc, FuzzParameters params, Random random) {
		return doc.createEntityReference(params.nextString(random, entityReferenceFilter));
	}
	
	public static Node nextChild(Document doc, FuzzParameters params, Random random) {
		int choice = params.nextChoice(random, 6);
		switch(choice) {
		case 0:
			return nextElement(doc, params, random, false);
		case 1:
			return nextText(doc, params, random);
		case 2:
			return nextData(doc, params, random);
		case 3:
			return nextComment(doc, params, random);
		case 4:
			return nextPI(doc, params, random);
		case 5:
			return nextEntityReference(doc, params, random);
		default:
			throw new RuntimeException("Choice not recognized: " + choice);
		}
	}
	
	public static Node nextChildLoop(Document doc, FuzzParameters params, Random random) {
		try {
			return nextChild(doc, params, random);
		} catch(Exception e) {
			return nextChildLoop(doc, params, random);
		}
	}
	
	public static Document nextDocument(FuzzParameters params, Random random) throws Exception {
		//DOMImplementation impl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
		//Document doc = impl.createDocument(params.nextString(random), params.nextString(random), impl.createDocumentType(params.nextString(random), params.nextString(random), params.nextString(random)));
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		doc.appendChild(nextElement(doc, params, random, true));
		return doc;
	}
	
	public static Document nextDocumentLoop(FuzzParameters params, Random random) {
		try {
			return nextDocument(params, random);
		} catch(Exception e) {
			throw new Error(e);
			//return nextDocumentLoop(params, random);
		}
	}
	
	public static String toString(Document doc) {
		try {
			StringWriter writer = new StringWriter();
			TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(writer));
			return writer.getBuffer().toString().substring("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>".length());
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String nextSample(FuzzParameters parameters, Random random) {
		try {
			buildFilters();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return toString(nextDocumentLoop(parameters, random));
	}
	
	public static Document nextMutationSample(FuzzParameters parameters, Document document, Random random) {
		try {
			buildFilters();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		try {
			return nextMutationSampleHelper(parameters, document, random);
		} catch(Exception e) {
			return nextMutationSample(parameters, document, random);
		}
	}
	
	private static Document nextMutationSampleHelper(FuzzParameters parameters, Document document, Random random) {
		List<Node>[] descendants = getDescendantsByType(document);
		int isMultiConstant = descendants[1].isEmpty() || (!descendants[0].isEmpty() && random.nextBoolean()) ? 0 : 1;
		int choice = random.nextInt(descendants[isMultiConstant].size());
		Node cur = descendants[isMultiConstant].get(choice);
		if(cur != document) {
			Node sub = XmlFuzzer.nextChildLoop(document, parameters, random);
			getSubstitute(document, cur, sub);
			return document;
		} else {
			return XmlFuzzer.nextDocumentLoop(parameters, random);
		}
	}
	
	private static void checkDocument(Document document) {
		if(!XmlData.XML_DATA.getQueryOracle().query(new DocumentExample(document).getExample())) {
			throw new RuntimeException();
		}
	}
	
	private static Document getDocumentWithElement() {
		Document document;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			document.appendChild(document.createElement("a"));
			return document;
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static boolean isBuilt = false;
	private static void buildFilters() throws Exception {
		if(!isBuilt) {
			for(char c=0; c<128; c++) {
				try {
					Document document = getDocumentWithElement();
					document.getDocumentElement().appendChild(document.createElement("" + c));
					checkDocument(document);
				} catch(Exception e) {
					elementNameFilter.add(c);
				}
				elementNameFilter.add(':');
				try {
					Document document = getDocumentWithElement();
					document.getDocumentElement().setAttribute("" + c, "a");;
					checkDocument(document);
				} catch(Exception e) {
					attrNameFilter.add(c);
				}
				attrNameFilter.add(':');
				try {
					Document document = getDocumentWithElement();
					document.getDocumentElement().setAttribute("a", "" + c);
					checkDocument(document);
				} catch(Exception e) {
					attrValueFilter.add(c);
				}
				try {
					Document document = getDocumentWithElement();
					document.getDocumentElement().appendChild(document.createProcessingInstruction("" + c, "a"));
					checkDocument(document);
				} catch(Exception e) {
					piTargetFilter.add(c);
				}
				piTargetFilter.add(':');
				try {
					Document document = getDocumentWithElement();
					document.getDocumentElement().appendChild(document.createProcessingInstruction("a", "" + c));
					checkDocument(document);
				} catch(Exception e) {
					piDataFilter.add(c);
				}
				try {
					Document document = getDocumentWithElement();
					document.getDocumentElement().appendChild(document.createComment("" + c));
					checkDocument(document);
				} catch(Exception e) {
					commentFilter.add(c);
				}
				try {
					Document document = getDocumentWithElement();
					document.getDocumentElement().appendChild(document.createTextNode("" + c));
					checkDocument(document);
				} catch(Exception e) {
					textFilter.add(c);
				}
				try {
					Document document = getDocumentWithElement();
					document.getDocumentElement().appendChild(document.createCDATASection("" + c));
					checkDocument(document);
				} catch(Exception e) {
					dataFilter.add(c);
				}
				try {
					Document document = getDocumentWithElement();
					document.getDocumentElement().appendChild(document.createEntityReference("" + c));
					checkDocument(document);
				} catch(Exception e) {
					entityReferenceFilter.add(c);
				}
			}
		}
		isBuilt = true;
	}
	
	private static void getDescendantsHelper(Node node, List<Node> descendants) {
		descendants.add(node);
		NodeList children = node.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			getDescendantsHelper(children.item(i), descendants);
		}
	}
	
	public static List<Node> getDescendants(Node node) {
		List<Node> descendants = new ArrayList<Node>();
		getDescendantsHelper(node, descendants);
		return descendants;
	}
	
	private static void getDescendantsByTypeHelper(Node node, List<Node>[] descendants) {
		(node instanceof Text ? descendants[0] : descendants[1]).add(node);
		NodeList children = node.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			getDescendantsByTypeHelper(children.item(i), descendants);
		}
	}

	public static List<Node>[] getDescendantsByType(Node node) {
		@SuppressWarnings("unchecked")
		List<Node>[] descendants = new List[2];
		for(int i=0; i<2; i++) {
			descendants[i] = new ArrayList<Node>();
		}
		getDescendantsByTypeHelper(node, descendants);
		return descendants;
	}
	
	private static boolean getSubstituteHelper(Node node, Node cur, Node sub) {
		NodeList children = node.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if(child == cur) {
				node.replaceChild(sub, cur);
				return true;
			} else if(getSubstituteHelper(child, cur, sub)) {
				return true;
			}
		}
		return false;
	}
	
	public static void getSubstitute(Document document, Node cur, Node sub) {
		if(document == cur) {
			throw new RuntimeException("Not a child!");
		}
		if(!getSubstituteHelper(document, cur, sub)) {
			throw new RuntimeException("Substitute failed!");
		}
	}
	
	public static Document getDocument(String example) {
		InputStream stream = new ByteArrayInputStream(example.getBytes(StandardCharsets.US_ASCII));
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
