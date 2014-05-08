import com.itextpdf.text.pdf.parser.*;
import com.itextpdf.text.pdf.parser.Vector;
import com.itextpdf.text.pdf.*;
import java.io.*;
import java.util.*;
import org.w3c.dom.*;

public class PDFTextExtractor implements RenderListener {

private static abstract class OperatorObserver implements ContentOperator {
private ContentOperator chainedOperator;
private void setChainedOperator (ContentOperator op) {  chainedOperator=op; }
public abstract boolean invoke (String op, List<PdfObject> args);
public void invoke (PdfContentStreamProcessor processor, PdfLiteral op, ArrayList<PdfObject> args) throws Exception {
if (invoke(op.toString(), args) && chainedOperator!=null) chainedOperator.invoke(processor, op, args);
}}

private class Info {
List<Node> nodes = new ArrayList<Node>();
Element curElement = null;
StringBuilder sb = new StringBuilder();
DocumentFont lastFont;
LineSegment lastLine;

public void flushText () {
if (sb.length()>0) {
Text text = doc.createTextNode(sb.toString());
sb.setLength(0);
if (curElement==null) nodes.add(text);
else curElement.appendChild(text);
}}
public void beginElement (String tagName) {
flushText();
Element e = doc.createElement(tagName);
if (curElement==null) nodes.add(e);
else curElement.appendChild(e);
curElement=e;
}
public void endElement (String tagName) {
if (curElement==null) return;
flushText();
List<Element> els = new ArrayList<Element>();
while (curElement!=null && !curElement.getTagName().equals(tagName)) {
els.add(curElement);
curElement = (Element)curElement.getParentNode();
}
if (curElement!=null) curElement = (Element)curElement.getParentNode();
for (Element e: els) beginElement(e.getTagName());
}
public void addEmptyElement (String tagName) {
flushText();
Element e = doc.createElement(tagName);
if (curElement==null) nodes.add(e);
else curElement.appendChild(e);
}
public boolean isInElement (String tagName) {
Element e = curElement;
while(e!=null) {
if (e.getTagName().equals(tagName)) return true;
e = (Element)e.getParentNode();
}
return false;
}
}//End class Info

Document doc;
PdfContentStreamProcessor processor;
Map<Integer,Info> texts = new TreeMap<Integer,Info>();
boolean watchStyles;

public PDFTextExtractor (Document d, boolean ws) {
watchStyles=ws;
doc = d;
processor = new PdfContentStreamProcessor(this);
}

public void registerOperator  (String operator, OperatorObserver observer) {
observer.setChainedOperator(processor.registerContentOperator(operator, observer));
}

public void process (PdfDictionary page) throws IOException {
if (page==null) return;
processor.reset();
PdfDictionary resources = page.getAsDict(PdfName.RESOURCES);
PdfStream stream = page.getAsStream(PdfName.CONTENTS);
PdfArray ar = page.getAsArray(PdfName.CONTENTS);
byte[] data = null;
if (stream!=null) data = getStreamData(stream);
else if (ar!=null) {
ByteArrayOutputStream out = new ByteArrayOutputStream();
for (int i=0, N=ar.size(); i<N; i++) {
stream = ar.getAsStream(i);
if (stream!=null) out.write(getStreamData(stream));
}
data = out.toByteArray();
}
if (data!=null) processor.processContent(data, resources);
}

public Map<Integer,Node[]> getAllMCID () {
Map<Integer,Node[]> m = new HashMap<Integer,Node[]>();
for (Map.Entry<Integer,Info> e: texts.entrySet()) {
Info info = e.getValue();
info.flushText();
m.put(e.getKey(), info.nodes.toArray(new Node[info.nodes.size()]));
}
return m;
}

private byte[] getStreamData (PdfStream stream) throws IOException {
return PdfReader.getStreamBytes((PRStream)stream);
}

private boolean isBold (DocumentFont font) {
return font.getFullFontName()[0][3].indexOf("Bold")>0;
}

private boolean isItalic (DocumentFont font) {
return font.getFullFontName()[0][3].indexOf("Italic")>0;
}

public void renderImage (ImageRenderInfo unused) {}
public void beginTextBlock () {}
public void endTextBlock () {}

public void renderText (TextRenderInfo tr) {
String text = tr.getText();
Integer mcid = tr.getMcid();
LineSegment line = tr.getBaseline();
DocumentFont font = tr.getFont();
if (mcid==null) return;
Info info = texts.get(mcid);
if (info==null) texts.put(mcid, info = new Info());
if (watchStyles && info.lastFont!=null && !info.lastFont.equals(font)) { // previous chunk of text had a different font than the current one; there might be a style change we are interested in
if (isBold(font) && !isBold(info.lastFont)) info.beginElement("b");
if (isItalic(font) && !isItalic(info.lastFont)) info.beginElement("i");
if (isBold(info.lastFont) && !isBold(font)) info.endElement("b");
if (isItalic(info.lastFont) && !isItalic(font)) info.endElement("i");
}
else if (watchStyles && info.lastFont==null) {
if (isBold(font)) info.beginElement("b");
if (isItalic(font)) info.beginElement("i");
}
if (info.lastLine!=null) {
Vector lastPoint = info.lastLine.getEndPoint(), newPoint = line.getStartPoint(); // we will compare end point of the previous chunk of text and start point of this chunk
float lastX = lastPoint.get(0), lastY = lastPoint.get(1), newX = newPoint.get(0), newY = newPoint.get(1), spaceWidth = tr.getSingleSpaceWidth();
if (newX-lastX >= 0.5*spaceWidth && info.sb.length()>0 && info.sb.charAt(info.sb.length()-1)!=' ') info.sb.append(' '); // if there is more than half the size of a space between now and before, it is very likely that spaces are encoded as move commands rather than actual spaces in the text; so we add a true space in the text if there isn't already one.
if (newY!=lastY) {
float diff=lastY-newY, fontSize = tr.getAscentLine().getStartPoint().get(1) - tr.getDescentLine().getStartPoint().get(1); // the difference between the top line and the bottom line of the text gives us more or less the font size
if (diff<0) {
if (watchStyles && info.isInElement("sub")) info.endElement("sub");
else if (watchStyles) info.beginElement("sup");
}
else if (diff>0) {
if (watchStyles && info.isInElement("sup")) info.endElement("sup");
else if (watchStyles && diff<fontSize) info.beginElement("sub");
if (diff>=fontSize && info.sb.length()>0) { // we are going down more than the font size; we have just gone to the next physical line of text
char c = info.sb.charAt(info.sb.length() -1);
if (c=='-') info.sb.setLength(info.sb.length() -1); // Yphan; in the perspective of text extraction or reflow, normally it should be an artifact; at least it is only presentational, so should be removed
else if (c!=' ' && watchStyles) info.addEmptyElement("br"); // very simple euristic: if the line ends with a space, it probably means that the line break is only presentational; if the line ends with another character, especially punctuation, it is most likely a forced line break that might have a semantic meaning
}}}}
info.lastLine = line;
info.lastFont = font;
info.sb.append(text);
}

}
