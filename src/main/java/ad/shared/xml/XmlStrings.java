package ad.shared.xml;

public class XmlStrings {
    public static String getStringData(Node rootNode, String name) {
        assert rootNode != null;
        Node node = rootNode.getNode(name);
        if (node != null)
            return unescapeString(node.childrenToString());
        assert false;
        return "";
    }

    public static String escapeString(String str) {
        if (str == null)
            return null;
        StringBuilder retval = new StringBuilder(str);
        int p = 0;
        while ((p = retval.indexOf("&", p)) != -1) {
            retval.replace(p, p + 1, "&amp;");
            p += 5;
        }
        while ((p = retval.indexOf("<")) != -1) {
            retval.replace(p, p + 1, "&lt;");
            p += 4;
        }
        while ((p = retval.indexOf(">")) != -1) {
            retval.replace(p, p + 1, "&gt;");
            p += 4;
        }
        while ((p = retval.indexOf("'")) != -1) {
            retval.replace(p, p + 1, "&apos;");
            p += 6;
        }
        while ((p = retval.indexOf("\"")) != -1) {
            retval.replace(p, p + 1, "&quot;");
            p += 6;
        }
        return retval.toString();
    }

    public static String unescapeString(String str) {
        if (str == null)
            return null;
        StringBuilder retval = new StringBuilder(str);
        int p;
        while ((p = retval.indexOf("&lt;")) != -1) {
            retval.replace(p, p + 4, "<");
            p++;
        }
        while ((p = retval.indexOf("&gt;")) != -1) {
            retval.replace(p, p + 4, ">");
            p++;
        }
        while ((p = retval.indexOf("&apos;")) != -1) {
            retval.replace(p, p + 6, "'");
            p++;
        }
        while ((p = retval.indexOf("&quot;")) != -1) {
            retval.replace(p, p + 6, "\"");
            p++;
        }
        while ((p = retval.indexOf("&amp;")) != -1) {
            retval.replace(p, p + 5, "&");
            p++;
        }
        return retval.toString();
    }

    public static String xml(String tag, String innerXML) {
        StringBuilder retval = new StringBuilder();
        retval.append("<").append(tag);
        if (innerXML != null && innerXML.length() > 0) {
            retval.append(">\n\t");
            retval.append(innerXML);
            retval.append("</");
            retval.append(tag);
            retval.append(">\n");
        } else {
            retval.append(" />\n");
        }
        return retval.toString();
    }
}
