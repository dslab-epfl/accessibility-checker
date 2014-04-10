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
PdfDictionary kDic = root.getAsDict(PdfName.K);
PdfNumber mcid = root.getAsNumber(PdfName.K);
String name = null;
if (elname!=null) name = elname.toString().substring(1).toLowerCase();
if (name!=null) xmlBeginElement(name);
if (actualText!=null) xmlAddAttr("actualText", actualText.toUnicodeString());
if (alt!=null) xmlAddAttr("alt", alt.toUnicodeString());
if (ar!=null) for (int i=0, N=ar.size(); i<N; i++) {
PdfDictionary obj = ar.getAsDict(i);
PdfNumber mcid2 = ar.getAsNumber(i);
if (obj!=null) pdfWalkStructElem(obj);
else if (mcid2!=null) pdfWalkMCID(root, mcid2);
}
else if (kDic!=null) pdfWalkStructElem(kDic);
else if (mcid!=null) pdfWalkMCID(root, mcid);
if (name!=null) xmlEndElement(name);
}

private void pdfWalkStructElem (PdfDictionary obj) throws Exception {
PdfName type = obj.getAsName(PdfName.TYPE);
if (type==null) return;
else if (type.equals(PdfName.STRUCTELEM)) pdfWalkTree(obj);
else if (type.equals(PdfName.MCR)) pdfWalkMCID(obj, obj.getAsNumber(PdfName.MCID));
}

private void pdfWalkMCID (PdfDictionary root, PdfNumber mcid) throws Exception {
if (root==null || mcid==null) return;
String str = getMCIDNodeText(root.getAsDict(PdfName.PG), mcid);
xmlAddText(str);
}

private String streamToString (PdfStream stream) throws Exception {
return new String(PdfReader.getStreamBytes((PRStream)stream));
}

private String getPageStream (PdfDictionary page) throws Exception {
String str = pageStreams.get(page);
if (str!=null) return str;
if (page==null) return null;
PdfStream stream = page.getAsStream(PdfName.CONTENTS);
PdfArray ar = page.getAsArray(PdfName.CONTENTS);
if (stream!=null) str = streamToString(stream);
else if (ar!=null) {
StringBuilder sb = new StringBuilder();
for (int i=0, N=ar.size(); i<N; i++) {
stream = ar.getAsStream(i);
if (stream!=null) sb.append(streamToString(stream)).append("\n");
}
str = sb.toString();
}
pageStreams.put(page,str);
return str;
}

private String getMCIDNodeText (PdfDictionary page, PdfNumber mcid) throws Exception {
String data = getPageStream(page);
if (data==null) return null;
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
