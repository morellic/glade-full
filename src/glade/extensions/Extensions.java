package glade.extensions;

import java.util.ArrayList;
import java.util.List;

import org.json.*;

import glade.grammar.MultiGrammarUtils.MultiGrammar;
import glade.grammar.MultiGrammarUtils.MultiProduction;
/*
{
  "<start>": "<expr>",
  "<expr>": ["<expr> + <expr>", "( <expr> )", "<digit>"],
  "<digit>": ["0", "1"],
}
*/

public class Extensions {
    public static class CustomGrammar {
        private String jsonString;
        private JSONObject jsonGrammar;
        private MultiGrammar multiGrammar;

        public CustomGrammar(String filePath) {
            this.jsonString = "{\"<start>\": \"<expr>\", \"<expr>\": [\"<expr> + <expr>\", \"( <expr> )\", \"<digit>\"], \"<digit>\": [\"0\", \"1\"]}";
            this.initJsonGrammar();
            this.initMultiGrammar();
        }

        private void initJsonGrammar() {
            this.jsonGrammar = new JSONObject(this.jsonString);
        }

        private void initMultiGrammar() {
            List<MultiProduction> productions = new ArrayList<MultiProduction>();

            for(String key : this.jsonGrammar.keySet()) {
                Object val = this.jsonGrammar.get(key);
                Object[] expansions;

                if (val instanceof String) {
                    expansions = new Object[1];
                    expansions[0] = val;
                }
                else if (val instanceof JSONArray) {
                    JSONArray arr = (JSONArray) val;
                    expansions = new Object[arr.length()];
                    for (int i = 0; i < arr.length(); i++) {
                        Object el = arr.get(i);
                        if (el instanceof String) {
                            expansions[i] = (String) el;
                        }
                        else {
                            throw new RuntimeException("JSON array element is not a string");
                        }
                    }
                }
                else {
                    throw new RuntimeException("Terminal is not a string or an array");
                }

                productions.add(new MultiProduction(key, expansions));
            }

            this.multiGrammar = new MultiGrammar(productions, "CustomJsonGrammar");
        }

        public JSONObject getJsonGrammar() {
            return this.jsonGrammar;
        }

        public MultiGrammar getMultiGrammar() {
            return this.multiGrammar;
        }

        public MultiGrammar getGrammar() {
            return this.getMultiGrammar();
        }
    }
}
