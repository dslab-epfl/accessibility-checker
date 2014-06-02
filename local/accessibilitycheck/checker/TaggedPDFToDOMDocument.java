import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.exceptions.*;

/** The goal of this class is to convert the complex PDF structure of a document into a much simpler DOM structure */
public class TaggedPDFToDOMDocument {

private PDFTextExtractor textExtractor = new PDFTextExtractor();
private PdfReader pdf;
private Document doc;
private Element curEl;
private Map<PdfName,PdfName> roleMap = new HashMap<>();
private Map<PdfDictionary,Map<Integer,Node[]>> pages = new HashMap<>();

/** Maps a registered role to another role, as defined in ISO32000-2008, role mapping chain. */
private PdfName pdfMapRole (PdfName name) {
int loopBreaker=0; // We need a look breaker because badly made PDF files can lead to inflinite loops (e.g. A mapped to B, B mapped to A again). This is forbidden by matterhorn protocol but many PDF files do it anyway.
while (roleMap.containsKey(name) && ++loopBreaker<1000) name = roleMap.get(name); // Follow role chain as long as we can
return name;
}

/** Read the role map from the PDF document */
private void pdfWalkRoleMap (PdfDictionary rm) {
if (rm==null) return;
for (PdfName key: rm.getKeys()) {
PdfName val = rm.getAsName(key);
if (val!=null) roleMap.put(key,val);
}}

/** Go through a node of the PDF structure tree */
private void pdfWalkTree (PdfDictionary root) throws IOException {
if (root==null) return;
PdfString 
alt = root.getAsString(PdfName.ALT), // alternate text
actualText = root.getAsString(PdfName.ACTUALTEXT), // actual text also known as replacement text
title = root.getAsString(PdfName.T),
abbr = root.getAsString(PdfName.E), // abbreviation expansion
id = root.getAsString(PdfName.ID),
lang = root.getAsString(PdfName.LANG);
PdfName elname = root.getAsName(PdfName.S);
PdfDictionary aDic = root.getAsDict(PdfName.A); // dictionary of all PDF attributes
PdfArray aAr = root.getAsArray(PdfName.A); // sometimes it is an array containing more than one dictionary instead of a single dictionary
PdfArray kAr = root.getAsArray(PdfName.K); // child nodes of PDF structure
PdfDictionary kDic = root.getAsDict(PdfName.K); // it may be a dictionary when there is a single child
PdfNumber mcid = root.getAsNumber(PdfName.K); // it may be a number when there is a single child and if it's a marked content reference ID
String name = null;
if (elname!=null) name = pdfMapRole(elname).toString().substring(1).toLowerCase(); // map the name found to the real role
if (name!=null) xmlBeginElement(name);

// Going through PDF attributes
if (aDic!=null) pdfWalkAttrs(aDic);
else if (aAr!=null) for (int i=0, N=aAr.size(); i<N; i++) {
aDic = aAr.getAsDict(i);
if (aDic!=null) pdfWalkAttrs(aDic);
}

// Checking for attributes that aren't in the A field (done above)
if (id!=null) xmlAddAttr("id", id.toUnicodeString());
if (alt!=null) xmlAddAttr("alt", alt.toUnicodeString());
if (title!=null) {
String str = title.toUnicodeString().trim();
if (str.length()>0) xmlAddAttr("title", str);
}
if (abbr!=null) {
String str = abbr.toUnicodeString().trim();
if (str.length()>0) xmlAddAttr("abbr", str);
}
if (lang!=null) xmlAddAttr("lang", lang.toUnicodeString());
if (actualText!=null) xmlAddText(actualText.toUnicodeString()); // if present, the actual text replaces all childs
else { // Going through child elements of the PDF structure
if (kAr!=null) for (int i=0, N=kAr.size(); i<N; i++) {
kDic = kAr.getAsDict(i);
mcid = kAr.getAsNumber(i);
if (kDic!=null) pdfWalkStructElem(kDic);
else if (mcid!=null) pdfWalkMCID(root, mcid);
}
else if (kDic!=null) pdfWalkStructElem(kDic);
else if (mcid!=null) pdfWalkMCID(root, mcid);
}
if (name!=null) xmlEndElement(name);
}

/** Going through PDF attributes */
private void pdfWalkAttrs (PdfDictionary dic) throws IOException {
if (dic==null) return;
for (PdfName name: dic.getKeys()) {
if (name.equals(PdfName.O) || name.equals(PdfName.R)) continue; // O and R aren't actual attributes, they have other purposes we aren't interested in
PdfObject obj = dic.getDirectObject(name);
xmlAddAttr(name.toString().substring(1).toLowerCase(), obj);
}}

/** Handling a particular PDF structure node */
private void pdfWalkStructElem (PdfDictionary obj) throws IOException {
PdfName type = obj.getAsName(PdfName.TYPE);
if (type==null || type.equals(PdfName.STRUCTELEM)) pdfWalkTree(obj);
else if (type.equals(PdfName.MCR)) pdfWalkMCID(obj, obj.getAsNumber(PdfName.MCID));
else if (type.equals(PdfName.OBJR)) pdfWalkObjRef(obj.getAsDict(PdfName.OBJ));
}

/** Going through object reference */
private void pdfWalkObjRef (PdfDictionary obj) throws IOException {
if (obj==null ) return;
PdfName type = obj.getAsName(PdfName.TYPE);
PdfName subtype = obj.getAsName(PdfName.SUBTYPE);
PdfString contents = obj.getAsString(PdfName.CONTENTS);
PdfString title = obj.getAsString(PdfName.T);
PdfDictionary action = obj.getAsDict(PdfName.A);
if (subtype.equals(PdfName.LINK) && action!=null) { // A clickable link
PdfString uri = action.getAsString(PdfName.URI);
if (uri!=null) xmlAddAttr("uri", uri.toUnicodeString());
if (title!=null) xmlAddAttr("title", title.toUnicodeString());
}
//other subtypes
else if (contents!=null) {
xmlBeginElement("annot");
xmlAddAttr("type", subtype.toString().substring(1).toLowerCase());
xmlAddText(contents.toUnicodeString());
xmlEndElement("annot");
}}

/** Going through a marked content refrence ID (MCID) */
private void pdfWalkMCID (PdfDictionary root, PdfNumber mcid) throws IOException {
if (root==null || mcid==null) return;
Node[] nodes = getNodesByMCID(root.getAsDict(PdfName.PG), mcid);
if (nodes!=null) xmlAddNodes(nodes);
}

/** Retriev extracted text corresponding to the MCID, in the form of XML nodes (to be able to support bold, italic and such) */
private Node[] getNodesByMCID (PdfDictionary page, PdfNumber mcid) throws IOException {
Map<Integer,Node[]> mcidMap = pages.get(page);
if (mcidMap==null) { // process the page only once if it isn't already done
textExtractor.process(page);
mcidMap = textExtractor.getAllMCID();
pages.put(page,mcidMap);
}
return mcidMap.get(mcid.intValue());
}

/** Utility method to be able to convert PDF values into XML attributes */
private String xmlToString (PdfObject o) {
if (o instanceof PdfString) return ((PdfString)o).toUnicodeString();
else if (o instanceof PdfName) return o.toString().substring(1).toLowerCase();
else if (o instanceof PdfNumber || o instanceof PdfBoolean || o instanceof PdfNull) return o.toString();
else if (o instanceof PdfArray) {
StringBuilder sb = new StringBuilder();
for (PdfObject obj: ((PdfArray)o)) {
if (sb.length()>0) sb.append(',');
sb.append(xmlToString(obj));
}
return sb.toString();
}
else return null;
}

/** Open an XML element and set the current element to point to it */
private void xmlBeginElement (String name) throws IOException {
Element el = doc.createElement(name);
if (curEl!=null) curEl.appendChild(el);
else if (doc.getDocumentElement()==null) doc.appendChild(el);
else {
Element el0 = doc.getDocumentElement();
doc.removeChild(el0);
Element root = doc.createElement("document");
root.appendChild(el0);
root.appendChild(el);
doc.appendChild(root);
}
curEl = el;
}

/** Close an XML element and make the current element pointing to the parent */
private void xmlEndElement (String name) throws IOException {
Node node = curEl.getParentNode();
curEl = node instanceof Element ? (Element)node : null;
}

/** Add an XML attribute to the current XML element */
private void xmlAddAttr (String name, String value) throws IOException {
curEl.setAttribute(name, value);
}

/** Add an XML attribute to the current XML element */
private void xmlAddAttr (String name, PdfObject value) throws IOException {
xmlAddAttr(name, xmlToString(value));
}

/** Add text to the current XML element */
private void xmlAddText (String str) throws IOException {
curEl.appendChild(doc.createTextNode(str));
}

/** Add multiple nodes at once to the current XML element */
private void xmlAddNodes (Node... nodes) throws IOException {
for (Node n: nodes) curEl.appendChild(n);
}

public Document getDocument () { return doc; }
public Map<PdfName,PdfName> getRoleMap () { return roleMap; }
public PdfDictionary getCatalog () { return pdf.getCatalog(); }
public int getPageCount () { return pdf.getNumberOfPages(); }
public PDFTextExtractor getTextExtractor () { return textExtractor; }

/** Read a PDF file and initialize the DOM document */
public void readPDF (String filename) throws IOException {
try {
doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
} catch (ParserConfigurationException e) { throw new IOException("Please install and configure a working DOM parser", e); }
textExtractor.setDocument(doc);
textExtractor.setWatchStyles(false);
pdf = new PdfReader(filename);
if (!pdf.isTagged()) throw new UnsupportedPdfException("Only tagged PDF files are supported");
PdfDictionary catalog = pdf.getCatalog();
PdfDictionary root = catalog.getAsDict(PdfName.STRUCTTREEROOT);
PdfDictionary roleMap = root.getAsDict(PdfName.ROLEMAP);
PdfDictionary outline = catalog.getAsDict(PdfName.OUTLINES);
pdfWalkRoleMap(roleMap);
pdfWalkTree(root);
}

}
