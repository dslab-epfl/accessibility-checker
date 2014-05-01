import org.w3c.dom.*;
import javax.xml.*;
import javax.xml.validation.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import java.util.*;
import java.util.regex.*;

public class PDFXMLDocumentAnalyzer {
private static class Rule {
String[] allowedChilds=null, allowedParents=null;
boolean allowText=false;
public Rule (String[] c, String[] p, boolean t) { allowedParents=p; allowedChilds=c; allowText=t; }
public Rule (String[] c, boolean t) { this(c,null,t); }
public Rule (boolean t) { this(null,t); }
}
private static Map<String,Rule> rules = new HashMap<String,Rule>();

private Document doc;
private Map<String,Integer> msgs = new HashMap<String,Integer>();

static {
String[] inlineTags = { "span", "quote", "code", "link", "note", "reference", "bibentry", "annot", "ruby", "warichu"};
String[] baseBlockTags = {"p", "h", "h1", "h2", "h3", "h4", "h5", "h6", "caption"};
String[] allBlockTags = merge(baseBlockTags, "l", "div", "blockquote", "table", "figure", "formula", "form", "toc", "nonstruct", "private");
String[] inlineAndBlockTags = merge(allBlockTags, inlineTags);
Rule allowInlineOnly = new Rule(inlineTags, true);
for (String s: baseBlockTags) rules.put(s, allowInlineOnly);
for (String s: inlineTags) rules.put(s, allowInlineOnly);
for (String s: array("div", "blockquote", "art", "figure", "formula", "private", "nonstruct")) rules.put(s, new Rule(inlineAndBlockTags, true));
for (String s: array("document", "part", "sect")) rules.put(s, new Rule(merge(inlineAndBlockTags, "part", "sect", "art"), true));
for (String s: array("rb", "rt", "rp")) rules.put(s, new Rule(inlineTags, array("ruby"), true));
for (String s: array("wt", "wp")) rules.put(s, new Rule(inlineTags, array("warichu"), true));
rules.put("l", new Rule(array("li", "caption"), false));
rules.put("li", new Rule(array("lbl", "lbody"), array("l"), false));
rules.put("lbl", new Rule(inlineTags, array("li", "toci", "note"), true));
rules.put("lbody", new Rule(inlineAndBlockTags, array("li"), true));
rules.put("table", new Rule(array("thead", "tbody", "tfoot", "tr", "caption"), false));
rules.put("thead", new Rule(array("tr"), false));
rules.put("tbody", new Rule(array("tr"), false));
rules.put("tfoot", new Rule(array("tr"), false));
rules.put("tr", new Rule(array("td", "th"), array("table", "thead", "tbody", "tfoot"), false));
rules.put("td", new Rule(inlineAndBlockTags, array("tr"), true));
rules.put("th", new Rule(inlineAndBlockTags, array("tr"), true));
rules.put("toc", new Rule(array("toci", "toc"), false));
rules.put("toci", new Rule(array("lbl", "reference", "p", "index", "nonstruct"), array("toc"), true));
rules.put("ruby", new Rule(array("rb", "rt", "rp"), false));
rules.put("warichu", new Rule(array("wt", "wp"), false));
}

public PDFXMLDocumentAnalyzer (Document d) {
doc = d;
analyze();
}

public PDFXMLDocumentAnalyzer (String filename) throws Exception {
doc = TaggedPDFToDOMDocument.process(filename);
analyze();
}

@SuppressWarnings("unchecked") private static String[] merge (String[] a, String... b) { return merge2(a,b); }
@SuppressWarnings("unchecked") private static String[] merge2 (String[]... arrays) {
int i=0, len = 0;
for (String[] e: arrays) len+=e.length;
String[] ar = new String[len];
for (String[] e1: arrays) for(String e: e1) ar[i++]=e;
return ar;
}

@SuppressWarnings("unchecked") private static <E> E[] array (E... x) { return x; }

@SuppressWarnings("unchecked") private <E> List<E> toList (final NodeList list) {
return new AbstractList<E>(){
public E get (int i) { return (E) list.item(i); }
public int size () { return list.getLength(); }
};
}

private List<Element> getElementsByTagName (String tagName) {
return this.<Element>toList(doc.getElementsByTagName(tagName));
}

private List<Element> getElementsByTagName (Element el, String tagName) {
return this.<Element>toList(el.getElementsByTagName(tagName));
}

private List<Node> childNodes (Node node) {
return this.<Node>toList(node.getChildNodes());
}

private boolean isElement (Node node, String tagName) {
return node instanceof Element && ((Element)node).getTagName().equals(tagName);
}

private boolean isOneOf (Node node, String... tagNames) {
return node instanceof Element && indexOf( ((Element)node).getTagName(), tagNames)>=0;
}

private void message (String msg) { message(msg,1); }
private void message (String msg, int n) {
Integer x = msgs.get(msg);
if (x==null) x=n;
else x+=n;
msgs.put(msg,x);
}

public Map<String,Integer> getResult () { return msgs; }

private <T> int indexOf (T item, T[] array) {
for (int i=0, n=array.length; i<n; i++) {
if (
(item!=null && item.equals(array[i])) 
|| (item==null&&array[i]==null)
) return i;
}
return -1;
}

/** Validate the structure of an element */
private void validate (Element el) {
Rule rule = rules.get(el.getTagName());
if (rule==null) {
message("InvalidTagging"); // encountered an unknown element
System.err.println("Unknown element: " +el.getTagName());
}
Node parent = el.getParentNode();
if (parent!=null && (parent instanceof Element) && rule!=null && rule.allowedParents!=null && !isOneOf(parent, rule.allowedParents)) {
message("InvalidTagging"); // This element is within a disallowed parent
System.err.println(el.getTagName() + " under " + ((Element)parent).getTagName());
}
for (Node n: childNodes(el)) {
if (n instanceof Element) {
if (rule!=null && rule.allowedChilds!=null && !isOneOf(n, rule.allowedChilds)) {
message("InvalidTagging"); // the element contains a child element of a type that isn't allowed
System.err.println(((Element)n).getTagName() + " child of " + el.getTagName()  );
}
validate((Element)n);
}
else if ((n instanceof Text) && rule!=null && !rule.allowText) {
message("InvalidTagging"); // The element contains text nodes when it isn't allowed
System.err.println(el.getTagName() + " contains text");
}
}}

/** 
Check for presence and significance of an alt text.
Returns false if no alt text found, or if it is very likely to be unappropriate; returns true if an alt is present and seem to be OK.
Unappropriate alt text currently includes 
+ ending like file extension, e.g. ".png"; word and power point in particular automatically set the source file as alt text, but it is rarely appropriate
*/
private boolean checkAltText (Element e) {
if (!e.hasAttribute("alt")) return false;
String alt = e.getAttribute("alt");
return !Pattern.compile("\\.\\w{2,4}$").matcher(alt).find();
}


/* Analyze the whole document */
public void analyze () {
msgs.clear(); // Clear error messages

// Check presence of significant alt text in <figure> and <formula> elements
for (Element e: getElementsByTagName("figure")) {
if (!checkAltText(e)) message("BadAlt");
}
for (Element e: getElementsByTagName("formula")) {
if (!checkAltText(e)) message("BadAlt");
}

// Check presence and correct nesting of headings
boolean useH = false, useHn = false;
int lastHn = 0;
for (Element e: getElementsByTagName("*")) {
String tagName = e.getTagName();
if (tagName.equals("h")) useH=true; // use relative heading levels, nesting of <sect> determines the effective level
else if (tagName.matches("^h\\d+$")) { // use <hn> absolute heading levels
int level = Integer.parseInt(tagName.substring(1));
if (level<=0 || level>6) message("BadHeadings"); // Headings go from h1 to h6
if (level > lastHn+1) message("BadHeadings"); // an <hn> must be followed by an <hm> where m is between 1 and n+1, otherwise it means that levels have been missed, e.g. <h3> followed by <h1> without any <h2> inbetween. The later is not valid tagging.
lastHn = level;
useHn=true;
}}
if (useH&&useHn) message("BadHeadings"); // It is not allowed to mix <h> and <hn> tags, the standard forbids it
if (!useH&&!useHn) message("NoHeadings"); // The document don't contain any heading; this is not an error, but it isn't good either if the document has more than a few pages

// validate the whole document structure
validate(doc.getDocumentElement());

// other checks
}


public static void main (String[] args) throws Exception {
if (args.length<1) return;
for (Map.Entry<String,Integer> e: new PDFXMLDocumentAnalyzer(args[0]) .getResult().entrySet()) {
if (e.getValue()>0) System.out.printf("%s: %d%n", e.getKey(), e.getValue());
}}

}