import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import com.itextpdf.text.pdf.*;

public class TaggedPDFToDOMDocument {

private PdfReader pdf;
private Document doc;
private Element curEl;
private Map<PdfName,PdfName> roleMap = new HashMap<>();
private Map<PdfDictionary,Map<Integer,String>> pages = new HashMap<>();

private PdfName pdfMapRole (PdfName name) {
if (roleMap.containsKey(name)) return pdfMapRole(roleMap.get(name));
else return name;
}

private void pdfWalkRoleMap (PdfDictionary rm) {
if (rm==null) return;
for (PdfName key: rm.getKeys()) {
PdfName val = rm.getAsName(key);
if (val!=null) roleMap.put(key,val);
}}

private void pdfWalkTree (PdfDictionary root) throws Exception {
if (root==null) return;
PdfString 
alt = root.getAsString(PdfName.ALT),
actualText = root.getAsString(PdfName.ACTUALTEXT),
title = root.getAsString(PdfName.T),
abbr = root.getAsString(PdfName.E),
id = root.getAsString(PdfName.ID),
lang = root.getAsString(PdfName.LANG);
PdfName elname = root.getAsName(PdfName.S);
PdfDictionary aDic = root.getAsDict(PdfName.A);
PdfArray aAr = root.getAsArray(PdfName.A);
PdfArray kAr = root.getAsArray(PdfName.K);
PdfDictionary kDic = root.getAsDict(PdfName.K);
PdfNumber mcid = root.getAsNumber(PdfName.K);
String name = null;
if (elname!=null) name = pdfMapRole(elname).toString().substring(1).toLowerCase();
if (name!=null) xmlBeginElement(name);
if (aDic!=null) pdfWalkAttrs(aDic);
else if (aAr!=null) for (int i=0, N=aAr.size(); i<N; i++) {
aDic = aAr.getAsDict(i);
if (aDic!=null) pdfWalkAttrs(aDic);
}
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
if (actualText!=null) xmlAddText(actualText.toUnicodeString());
else {
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

private void pdfWalkAttrs (PdfDictionary dic) throws Exception {
if (dic==null) return;
for (PdfName name: dic.getKeys()) {
if (name.equals(PdfName.O) || name.equals(PdfName.R)) continue;
PdfObject obj = dic.getDirectObject(name);
xmlAddAttr(name.toString().substring(1).toLowerCase(), obj);
}}

private void pdfWalkStructElem (PdfDictionary obj) throws Exception {
PdfName type = obj.getAsName(PdfName.TYPE);
if (type==null) return;
else if (type.equals(PdfName.STRUCTELEM)) pdfWalkTree(obj);
else if (type.equals(PdfName.MCR)) pdfWalkMCID(obj, obj.getAsNumber(PdfName.MCID));
else if (type.equals(PdfName.OBJR)) pdfWalkObjRef(obj.getAsDict(PdfName.OBJ));
}

private void pdfWalkObjRef (PdfDictionary obj) throws Exception {
if (obj==null ) return;
PdfName type = obj.getAsName(PdfName.TYPE);
PdfName subtype = obj.getAsName(PdfName.SUBTYPE);
PdfString contents = obj.getAsString(PdfName.CONTENTS);
PdfString title = obj.getAsString(PdfName.T);
PdfDictionary action = obj.getAsDict(PdfName.A);
if (subtype.equals(PdfName.LINK) && action!=null) {
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

private void pdfWalkMCID (PdfDictionary root, PdfNumber mcid) throws Exception {
if (root==null || mcid==null) return;
String str = getMCIDNodeText(root.getAsDict(PdfName.PG), mcid);
xmlAddText(str);
}

private String getMCIDNodeText (PdfDictionary page, PdfNumber mcid) throws Exception {
Map<Integer,String> mcidMap = pages.get(page);
if (mcidMap==null) {
PDFTextExtractor te = new PDFTextExtractor();
te.process(page);
mcidMap = te.getAllMCID();
pages.put(page,mcidMap);
}
return mcidMap.get(mcid.intValue());
}

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

private void xmlBeginElement (String name) throws Exception {
Element el = doc.createElement(name);
if (curEl==null) doc.appendChild(el);
else curEl.appendChild(el);
curEl = el;
}

private void xmlEndElement (String name) throws Exception {
Node node = curEl.getParentNode();
curEl = node instanceof Element ? (Element)node : null;
}

private void xmlAddAttr (String name, String value) throws Exception {
curEl.setAttribute(name, value);
}

private void xmlAddAttr (String name, PdfObject value) throws Exception {
xmlAddAttr(name, xmlToString(value));
}

private void xmlAddText (String str) throws Exception {
curEl.appendChild(doc.createTextNode(str));
}

public Document getDocument () { return doc; }

public TaggedPDFToDOMDocument readPDF (String filename) throws Exception {
doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
pdf = new PdfReader(filename);
PdfDictionary root = pdf.getCatalog().getAsDict(PdfName.STRUCTTREEROOT);
PdfDictionary roleMap = root.getAsDict(PdfName.ROLEMAP);
pdfWalkRoleMap(roleMap);
pdfWalkTree(root);
return this;
}

public static Document process (String filename) throws Exception {
return new TaggedPDFToDOMDocument() .readPDF(filename) .getDocument();
}

}
