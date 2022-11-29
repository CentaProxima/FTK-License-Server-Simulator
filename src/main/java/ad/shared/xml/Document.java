package ad.shared.xml;

import ad.io.ByteArraysInputStream;
import ad.lang.StringToolkit;
import ad.utils.ADLogger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class Document {
    private static ADLogger mLogger = ADLogger.getRootLogger();

    public static class ContentHandler extends DefaultHandler {
        private Node mRoot = new Node();

        private Node mNode = this.mRoot;

        private StringBuffer mStrBuf = new StringBuffer();

        public void characters(char[] text, int offset, int length) {
            this.mStrBuf.append(text, offset, length);
        }

        public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes attributes) {
            if (this.mStrBuf.length() > 0) {
                this.mNode.addChild(this.mStrBuf.toString());
                this.mStrBuf = new StringBuffer();
            }
            this.mNode = this.mNode.addChild(localName, attributes);
        }

        public void endElement(String namespaceURI, String localName, String qualifiedName) {
            if (this.mStrBuf.length() > 0) {
                this.mNode.addChild(this.mStrBuf.toString());
                this.mStrBuf = new StringBuffer();
            }
            this.mNode = this.mNode.getParent();
        }

        public Node getRoot() {
            List<Node> nodes = this.mRoot.getNodes();
            assert nodes.size() == 1;
            return nodes.get(0);
        }
    }

    public static Node parse(String stream, boolean hasRoot, String encoding) throws SAXException, IOException {
        if (hasRoot && stream.length() < 1) {
            assert false;
            return null;
        }
        String xml = "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>" + (hasRoot ? stream : ("<ROOT>" + stream + "</ROOT>"));
        try {
            XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            ContentHandler contentHandler = new ContentHandler();
            parser.setContentHandler(contentHandler);
            parser.parse(new InputSource(new StringReader(xml)));
            return contentHandler.getRoot();
        } catch (SAXException se) {
            mLogger.warn("Document.parse: SAXException: " + se);
            mLogger.warn("Document.parse: xml: " + xml);
            mLogger.error(StringToolkit.stackTraceToString(se));
            throw se;
        }
    }

    public static Node parse(InputStream stream, boolean hasRoot, String encoding) throws SAXException, IOException {
        ByteArraysInputStream bais = new ByteArraysInputStream();
        bais.add(new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>").getBytes(encoding)));
        if (hasRoot) {
            bais.add(stream);
        } else {
            bais.add(new ByteArrayInputStream("<ROOT>".getBytes(encoding)));
            bais.add(stream);
            bais.add(new ByteArrayInputStream("</ROOT>".getBytes(encoding)));
        }
        try {
            XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            ContentHandler contentHandler = new ContentHandler();
            parser.setContentHandler(contentHandler);
            parser.parse(new InputSource((InputStream)bais));
            return contentHandler.getRoot();
        } catch (SAXException se) {
            mLogger.warn("Document.parse: SAXException: " + se);
            mLogger.error(StringToolkit.stackTraceToString(se));
            throw se;
        }
    }

    public static Node parse(String stream, boolean hasRoot) throws SAXException, IOException {
        return parse(stream, hasRoot, "UTF-8");
    }

    public static Node parse(InputStream stream, boolean hasRoot) throws SAXException, IOException {
        return parse(stream, hasRoot, "UTF-8");
    }
}
