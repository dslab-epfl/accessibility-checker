import org.w3c.dom.*;
import javax.xml.*;
import javax.xml.validation.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.exceptions.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;

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
private PdfDictionary catalog;
private List<PdfName> markedContentElements = new ArrayList<PdfName>();
private Map<PdfName,PdfName> roleMap;
private Map<String,Integer> msgs = new LinkedHashMap<String,Integer>();
private PrintStream out = System.out;
private int nPages;

static {
String[] inlineTags = { "span", "quote", "code", "link", "note", "reference", "bibentry", "annot", "figure", "formula", "ruby", "warichu"};
String[] baseBlockTags = {"p", "h", "h1", "h2", "h3", "h4", "h5", "h6", "caption"};
String[] allBlockTags = merge(baseBlockTags, "l", "div", "blockquote", "table", "form", "toc", "nonstruct", "private");
String[] inlineAndBlockTags = merge(allBlockTags, inlineTags);
Rule allowInlineOnly = new Rule(inlineTags, true);
for (String s: baseBlockTags) rules.put(s, allowInlineOnly);
for (String s: inlineTags) rules.put(s, allowInlineOnly);
for (String s: array("index")) rules.put(s, allowInlineOnly);
for (String s: array("blockquote", "figure", "formula", "private", "nonstruct")) rules.put(s, new Rule(inlineAndBlockTags, true));
for (String s: array("document", "part", "sect")) rules.put(s, new Rule(merge(inlineAndBlockTags, "part", "sect", "art"), true));
for (String s: array("rb", "rt", "rp")) rules.put(s, new Rule(inlineTags, array("ruby"), true));
for (String s: array("wt", "wp")) rules.put(s, new Rule(inlineTags, array("warichu"), true));
rules.put("caption", new Rule(merge(inlineTags, "p"), true));
rules.put("div", new Rule(merge(inlineAndBlockTags, "sect", "art", "part"), true));
rules.put("art", new Rule(merge(inlineAndBlockTags, "part", "sect"), true));
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
rules.put("toci", new Rule(array("lbl", "reference", "toc", "p", "index", "nonstruct"), array("toc"), true));
rules.put("ruby", new Rule(array("rb", "rt", "rp"), false));
rules.put("warichu", new Rule(array("wt", "wp"), false));
}

public void read (String filename) throws IOException {
msgs.clear(); // Clear error messages
TaggedPDFToDOMDocument cvt = new TaggedPDFToDOMDocument();
PDFTextExtractor.OperatorObserver bdcOp = new PDFTextExtractor.OperatorObserver(){ public boolean invoke (String op, List<PdfObject> args){ encounterBDC(args); return true; }};
cvt.getTextExtractor().registerOperator("BDC", bdcOp);
cvt.getTextExtractor().registerOperator("BMC", bdcOp);
cvt.getTextExtractor().registerOperator("EMC", new PDFTextExtractor.OperatorObserver(){ public boolean invoke (String op, List<PdfObject> args){ encounterEMC(args); return true; }});
cvt.readPDF(filename);
doc = cvt.getDocument();
roleMap = cvt.getRoleMap();
catalog = cvt.getCatalog();
nPages = cvt.getPageCount();
analyzeDocument();
analyzeRoleMap();
analyzeMisc();
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

private String getNearText (Element e) {
if (e==null) return "...";
String str = e.getTextContent();
if (str.length()<=0) return getNearText((Element)(e.getParentNode()));
if (str.length()>60) {
int n = str.indexOf(' ', 60);
if (n<0) n = str.indexOf('\n', 60);
if (n<0) n = str.indexOf('\t', 60);
if (n<0) n = str.indexOf('\u00A0', 60);
if (n>0) str = str.substring(0,n) + "...";
}
str = str.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').replaceAll(" +", " ");
return str;
}

private void message (String key, String detail) { message(key,detail,null); }
private void message (String key, String detail, Element el) {
if (out!=null) {
if (el!=null) out.println("- " + detail + ": near " + getNearText(el));
else out.println("- " + detail);
}
Integer x = msgs.get(key);
if (x==null) x=1;
else x++;
msgs.put(key,x);
}

public void setOut (PrintStream o) { out=o; }
public Document getDocument () { return doc; }
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

public void encounterBDC (List<PdfObject> l) {
if (l.size()<1 || !(l.get(0) instanceof PdfName)) return;
PdfName type = (PdfName)l.get(0);
boolean isArtifact = type.equals(PdfName.ARTIFACT),
isMarkedContent = l.size()>=2 && (l.get(1) instanceof PdfDictionary) && ((PdfDictionary)l.get(1)).get(PdfName.MCID)!=null,
containsArtifacts = markedContentElements.contains(PdfName.ARTIFACT), 
containsMarkedContents = markedContentElements.contains(PdfName.MCR),
notEmpty = markedContentElements.size()>0;
if (isMarkedContent) type = PdfName.MCR;
markedContentElements.add(type);
if (isArtifact && containsMarkedContents) message( "MixedArtifactAndTaggedContents", "MH01-003: artifacts present in tagged contents");
if (isMarkedContent && containsArtifacts) message( "MixedArtifactAndTaggedContents", "MH01-004: tagged contents present in artifacts");
}

public void encounterEMC (List<PdfObject> l) {
if (markedContentElements.size()<=0) message("BDCEMCMissmatch", "ISO-32000-2008: missmatched BDC/EMC tags");
else markedContentElements.remove(markedContentElements.size() -1);
}

/** Validate some other PDF aspects, outside of the XML document part */
private void analyzeMisc () {
}

/** Check the validity of the role map */
private void analyzeRoleMap () {
Set<PdfName> cycleCheck = new HashSet<PdfName>();
for (Map.Entry<PdfName,PdfName> e: roleMap.entrySet()) {
PdfName src = e.getKey(), dst = e.getValue();
cycleCheck.clear();
cycleCheck.add(src); cycleCheck.add(dst);
if (rules.containsKey(src.toString().substring(1).toLowerCase())) message("InvalidRoleMap", "MH02-004: standard element "+src+" has been remapped to "+dst); // Existing standard types shouldn't be remapped (MH02-004)
while(roleMap.containsKey(dst)) {
dst = roleMap.get(dst);
if (cycleCheck.contains(dst)) { message("InvalidRoleMap", "MH02-003: a cycle has been detected in the role map: " + src + " -> " + dst); break; } // very simple cycle detection! Cycling remappings aren't allowed (MH02-003)
cycleCheck.add(dst);
}
if (!rules.containsKey(dst.toString().substring(1).toLowerCase())) message("InvalidRoleMap", "MH02-001: role mapping must always end in a standard element; this is not the case for " + src + " -> " + dst); // remapped chain shouldn't end on a non-standard type (MH02-001)
}}

/** Validate the structure of an element */
private void validate (Element el) {
Rule rule = rules.get(el.getTagName());
if (rule==null) {
message("InvalidTagging", "ISO32000-2008: unknown structure element: " + el.getTagName(), el); // encountered an unknown element
}
Node parent = el.getParentNode();
if (parent!=null && (parent instanceof Element) && rule!=null && rule.allowedParents!=null && !isOneOf(parent, rule.allowedParents)) {
message("InvalidTagging", "ISO32000-2008: " + el.getTagName() + " not allowed within " + ((Element)parent).getTagName(), el); // This element is within a disallowed parent
}
for (Node n: childNodes(el)) {
if (n instanceof Element) {
if (rule!=null && rule.allowedChilds!=null && !isOneOf(n, rule.allowedChilds)) {
message("InvalidTagging", "ISO-32000-2008: " + ((Element)n).getTagName() + " within " + el.getTagName() + " not allowed", (Element)n); // the element contains a child element of a type that isn't allowed
}
validate((Element)n);
}
else if ((n instanceof Text) && rule!=null && !rule.allowText) {
message("InvalidTagging", "ISO-32000-2008: text not allowed within " + el.getTagName(), el); // The element contains text nodes when it isn't allowed
}
}
// Special case not covered by rules above: <art> not nestable
if (el.getTagName().equals("art") && el.getElementsByTagName("art").getLength()>0) message("InvalidTagging", "ISO-32000-2008: art tag is not nestable", el);
}

/** Check specific list structure (MH09-005) */
private void checkList (Element l) {
int nCaptions = l.getElementsByTagName("caption").getLength();
if (nCaptions>1) message("InvalidTagging", "MH09-005: a list can contain at most one caption", l);
else if (nCaptions==1) {
Node caption = l.getFirstChild();
if (!(caption instanceof Element) || !((Element)caption).getTagName().equals("caption")) message("InvalidTagging", "MH09-005: the caption of a list, if any, must be the first child element", l);
}
}

/** Check specific table structures (MH09-004) */
private void checkTable (Element table) {
List<Element> captions = getElementsByTagName(table, "caption");
List<Node> tc = childNodes(table);
if (captions.size()>1) message("InvalidTagging", "MH09-004: a table must contain at most a single caption element", captions.get(0)); // only a single <caption> is allowed per table 
else if (captions.size()==1) {
Element caption = captions.get(0);
if (!tc.get(0).equals(caption) && !tc.get(tc.size() -1).equals(caption)) message("InvalidTagging", "MH09-004: the caption element must be the first or the last one in the table element", caption); // if present, <caption> must be wheither the first or the last element of the table 
}

int nTr=0, nTbody=0, nThead=0, nTfoot=0;
for (Node n: tc) {
if (!(n instanceof Element)) continue;
Element e = (Element)n;
String tn = e.getTagName();
if (tn.equals("tr")) nTr++;
else if (tn.equals("thead")) {
if (nTfoot>0 || nTbody>0) message("InvalidTagging", "MH09-004: thead must appear before tbody and tfoot", e); // <thead> must appear before <tbody> and <tfoot>
nThead++;
}
else if (tn.equals("tbody")) nTbody++;
else if (tn.equals("tfoot")) nTfoot++;
}
if (nTr>0 && (nThead>0 || nTbody>0 || nTfoot>0)) message("InvalidTagging", "MH09-004: a table can contain wheither thead+tbody+tfoot or tr, but not both", table); // A table must consist of <tr> only, or <thead> + <tbody> + <tfoot> only, but not mix both

boolean hasTh=false;
boolean hasAttrHeaders=false, hasNoAttrScope=false;
int nCellsPerRow=0;
for (Element tr: getElementsByTagName(table, "tr")) {
int nTd = 0, nTh=0;
for (Element th: getElementsByTagName(tr, "th")) {
if (!th.hasAttribute("id") && !th.hasAttribute("scope")) message("tableThNoScope", "MH15-003: th must have wheither a scope or an id attribute", th);
if (!th.hasAttribute("scope")) hasNoAttrScope=true;
nTh++;
}
for (Element td: getElementsByTagName(tr, "td")) {
if (td.hasAttribute("headers")) hasAttrHeaders=true;
nTd++;
}
if (nTh>0) hasTh=true;
if (nCellsPerRow<=0) nCellsPerRow = nTd+nTh;
if (nTd+nTh!=nCellsPerRow && !hasAttrHeaders) message("TableIrregular", "Warning: Irregular tables that don't have a constant number of columns in each row aren't advisable and should be avoided", tr); // Irregular tables, i.e. not having always the same number of columns in each row aren't advisable
nCellsPerRow = nTd+nTh;
}
if (!hasTh) message("TableNoTh", "Warning: data tables should have header cells", table); // Data tables must have header cells; if they don't have, it probably means that they are presentational, in which case they shouldn't ahve been tagged as table at the first place. (MH15-004)
if (hasNoAttrScope && !hasAttrHeaders) message("TableMissingScopeOrHeaders", "MH15-003: header cells must all have a scope attributes, or cells headers must be explicitely specified with id/headers", table);
// other check to do in tables
}

/** 
Check for presence and significance of an alt text. (MH13-004 and MH17-002)
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
public void analyzeDocument () {
// Check presence of significant alt text in <figure> and <formula> elements (MH13-004 and MH17-002)
for (Element e: getElementsByTagName("figure")) {
if (!checkAltText(e)) message("BadAlt", "MH13-004: Absent or unappropriate alt text in figure", e);
}
for (Element e: getElementsByTagName("formula")) {
if (!checkAltText(e)) message("BadAlt", "MH17-002: absent or unappropriate alt text in formula", e);
}

// Check presence and correct nesting of headings (MH14-001, 14-002, 14-003 14-007)
boolean useH = false, useHn = false;
int lastHn = 0;
for (Element e: getElementsByTagName("*")) {
String tagName = e.getTagName();
if (tagName.equals("h")) useH=true; // use relative heading levels, nesting of <sect> determines the effective level
else if (tagName.matches("^h\\d+$")) { // use <hn> absolute heading levels
int level = Integer.parseInt(tagName.substring(1));
if (level<=0 || level>6) message("BadHeadings", "MH-14-00X: heading level must be between 1 and 6", e); // Headings go from h1 to h6
if (level > lastHn+1) message("BadHeadings", "MH14-002/3: heading structure is broken (some heading levels have been skipped)", e); // an <hn> must be followed by an <hm> where m is between 1 and n+1, otherwise it means that levels have been missed, e.g. <h3> followed by <h1> without any <h2> inbetween. The later is not valid tagging (MH14-002 and 3)
lastHn = level;
useHn=true;
}}
if (useH&&useHn) message("BadHeadings", "MH14-007: mixing relative and absolute headings levels isn't allowed"); // It is not allowed to mix <h> and <hn> tags (MH14-007)
if (!useH && !useHn && nPages>=3) message("NoHeadings", "MH14-001: the document doesn't contain any heading"); // The document don't contain any heading; this is not an error, but it isn't good either if the document has more than a few pages (suspect MH14-001); we tolerate it without error for documents that have less than 3 pages.

// Check table structures (MH09-004)
for (Element e: getElementsByTagName("table")) checkTable(e);

// Check list structures (MH09-005)
for (Element e: getElementsByTagName("l")) checkList(e);

// validate the whole document structure MH(09-001, 09-002 and 09-005 to 8)
validate(doc.getDocumentElement());

// Check that the document well specifies a language (MH11-001 and 11-002)
if (doc.getDocumentElement().getAttribute("lang") == null) message("NoLanguageDefined", "MH11-001/2: no language specified for the document"); 

// other checks
}


public static void main (String[] args) {
try {
if (args.length<1) return;
PDFXMLDocumentAnalyzer analyzer = new PDFXMLDocumentAnalyzer();
analyzer.read(args[0]);
Map<String,Integer> result = analyzer.getResult();
for (Map.Entry<String,Integer> e: result.entrySet()) {
if (e.getValue()<=0) continue;
System.out.printf("%s: %d%n", e.getKey(), e.getValue());
}
} catch (IOException e) { 
System.err.println("ERROR: " +e.getClass().getName() +": " +e.getMessage());
System.exit(1);
}
System.exit(0);
}

}