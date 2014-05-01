import com.itextpdf.text.pdf.parser.*;
import com.itextpdf.text.pdf.parser.Vector;
import com.itextpdf.text.pdf.*;
import java.io.*;
import java.util.*;

public class PDFTextExtractor implements RenderListener, ContentOperator {
private class Info {
StringBuilder sb = new StringBuilder();
DocumentFont lastFont;
LineSegment lastLine;
}

PdfContentStreamProcessor processor;
Map<Integer,Info> texts = new TreeMap<Integer,Info>();

public PDFTextExtractor () {
processor = new PdfContentStreamProcessor(this);
//chainedOperator = processor.registerContentOperator("Tf", this);
}

public void process (PdfDictionary page) throws IOException {
if (page==null) return;
processor.reset();
PdfDictionary resources = page.getAsDict(PdfName.RESOURCES);
PdfStream stream = page.getAsStream(PdfName.CONTENTS);
PdfArray ar = page.getAsArray(PdfName.CONTENTS);
if (stream!=null) processor.processContent(getStreamData(stream), resources);
else if (ar!=null) for (int i=0, N=ar.size(); i<N; i++) {
stream = ar.getAsStream(i);
if (stream!=null) processor.processContent(getStreamData(stream), resources);
}}

public Map<Integer,String> getAllMCID () {
Map<Integer,String> m = new HashMap<Integer,String>();
for (Map.Entry<Integer,Info> e: texts.entrySet()) {
m.put(e.getKey(), e.getValue().sb.toString());
}
return m;
}

private byte[] getStreamData (PdfStream stream) throws IOException {
return PdfReader.getStreamBytes((PRStream)stream);
}

public void renderImage (ImageRenderInfo unused) {}
public void beginTextBlock () {}
public void endTextBlock () {}

public void invoke (PdfContentStreamProcessor processor, PdfLiteral op, ArrayList<PdfObject> args) throws Exception {
System.out.println(op + ": " + args);
//chainedOperator.invoke(processor, op, args);
}

public void renderText (TextRenderInfo tr) {
String text = tr.getText();
Integer mcid = tr.getMcid();
LineSegment line = tr.getBaseline();
DocumentFont font = tr.getFont();
//System.out.println(text + " " + font.getFullFontName()[0][3]);
if (mcid==null) return;
Info info = texts.get(mcid);
if (info==null) texts.put(mcid, info = new Info());
if (info.lastLine!=null) {
Vector lastPoint = info.lastLine.getEndPoint(), newPoint = line.getStartPoint();
float lastX = lastPoint.get(0), lastY = lastPoint.get(1), newX = newPoint.get(0), newY = newPoint.get(1), spaceWidth = tr.getSingleSpaceWidth();
if (newX-lastX >= 0.5*spaceWidth && info.sb.length()>0 && info.sb.charAt(info.sb.length()-1)!=' ') info.sb.append(' ');
}
info.lastLine = line;
info.lastFont = font;
info.sb.append(text);
}

}
