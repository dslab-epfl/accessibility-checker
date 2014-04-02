import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import com.itextpdf.text.xml.*;

public class Test {
public static void main (String[] args) throws Exception {
String filename = args.length>=1? args[0] : "test.pdf";
Document doc = TaggedPDFToDOMDocument.process(filename);
XmlDomWriter writer = new XmlDomWriter();
writer.setOutput(System.out, "ISO-8859-1");
writer.write(doc);
}

}