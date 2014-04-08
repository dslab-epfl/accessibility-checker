import org.w3c.dom.*;
import java.util.*;
import java.util.regex.*;

public class PDFXMLDocumentAnalyzer {
private Document doc;
private Map<String,Integer> msgs = new HashMap<String,Integer>();

public PDFXMLDocumentAnalyzer (Document d) {
doc = d;
analyze();
}

public PDFXMLDocumentAnalyzer (String filename) throws Exception {
doc = TaggedPDFToDOMDocument.process(filename);
analyze();
}

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

private void message (String msg) { message(msg,1); }
private void message (String msg, int n) {
Integer x = msgs.get(msg);
if (x==null) x=n;
else x+=n;
msgs.put(msg,x);
}

public Map<String,Integer> getResult () { return msgs; }

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

/*
Perform entire check of the document
*/
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

// Check correctness of lists
for (Element e: getElementsByTagName("l")) {
for (Node n: childNodes(e)) {
if (!isElement(n,"li")) message("BadLists"); // A list <l> must only contain list items <li>
}}

// other checks
}


public static void main (String[] args) throws Exception {
if (args.length<1) return;
for (Map.Entry<String,Integer> e: new PDFXMLDocumentAnalyzer(args[0]) .getResult().entrySet()) {
System.out.printf("%s: %d%n", e.getKey(), e.getValue());
}}

}