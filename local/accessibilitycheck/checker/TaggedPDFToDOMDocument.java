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
private Map<PdfDictionary,String> pageStreams = new HashMap<>();

private void pdfWalkTree (PdfDictionary root) throws Exception {
if (root==null) return;
PdfName elname = root.getAsName(PdfName.S);
PdfString alt = root.getAsString(PdfName.ALT);
PdfString actualText = root.getAsString(PdfName.ACTUALTEXT);
PdfArray ar = root.getAsArray(PdfName.K);
PdfNumber mcid = root.getAsNumber(PdfName.K);
String name = null;
if (elname!=null) name = elname.toString().substring(1).toLowerCase();
if (name!=null) xmlBeginElement(name);
if (actualText!=null) xmlAddAttr("actualText", actualText.toUnicodeString());
if (alt!=null) xmlAddAttr("alt", alt.toUnicodeString());
if (ar!=null) for (int i=0, N=ar.size(); i<N; i++) {
PdfDictionary obj = ar.getAsDict(i);
PdfNumber mcid2 = ar.getAsNumber(i);
if (obj!=null) {
PdfName type = obj.getAsName(PdfName.TYPE);
if (!type.equals(PdfName.STRUCTELEM)) continue;
pdfWalkTree(obj);
}
else if (mcid2!=null) {
String str = getMCIDNodeText(root.getAsDict(PdfName.PG), mcid2);
xmlAddText(str);
}}
else if (mcid!=null) {
String str = getMCIDNodeText(root.getAsDict(PdfName.PG), mcid);
xmlAddText(str);
}
if (name!=null) xmlEndElement(name);
}

private String getPageStream (PdfDictionary page) throws IOException {
String str = pageStreams.get(page);
if (str!=null) return str;
PRStream stream = (PRStream)(page.getAsStream(PdfName.CONTENTS));
str = new String(PdfReader.getStreamBytes(stream));
pageStreams.put(page,str);
return str;
}

private String getMCIDNodeText (PdfDictionary page, PdfNumber mcid) throws IOException {
String data = getPageStream(page);
int startPos = data.indexOf("<</MCID " + mcid.intValue() + "");
if (startPos<0) return null;
int endPos = data.indexOf("<</MCID", startPos+1);
if (endPos<0) endPos = data.length();
//System.out.println("Content={\r\n"+data.substring(startPos,endPos)+"\r\n}");
Matcher m = Pattern.compile("^([^\r\n]+)\\s*Tj$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(data);
if (!m.find(startPos)) return null;
StringBuilder sb = new StringBuilder();
do {
parseTj(sb, m.group(1));
} while (m.find() && m.start()<endPos);
return sb.toString();
}

private void parseTj (StringBuilder sb, String tj) {
tj = tj
.replace("\\(", "\u0001")
.replace("\\)", "\u0002");
Matcher m = Pattern.compile("\\(([^()]+)\\)").matcher(tj);
while (m.find()) {
String s = m.group(1);
s = s
.replace("\u0001", "(")
.replace("\u0002", ")");
sb.append(s);
}}

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

private void xmlAddText (String str) throws Exception {
curEl.appendChild(doc.createTextNode(str));
}

public Document getDocument () { return doc; }

public TaggedPDFToDOMDocument readPDF (String filename) throws Exception {
doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
pdf = new PdfReader(filename);
PdfDictionary root = pdf.getCatalog().getAsDict(PdfName.STRUCTTREEROOT);
pdfWalkTree(root);
return this;
}

public static Document process (String filename) throws Exception {
return new TaggedPDFToDOMDocument() .readPDF(filename) .getDocument();
}

}
