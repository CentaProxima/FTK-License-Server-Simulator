package ad.shared.xml;

import ad.lang.StringToolkit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.xml.sax.Attributes;

public class Node implements Comparable<Object> {
    public static final int UNKNOWN_NODE = 0;

    public static final int ELEMENT_NODE = 1;

    public static final int TEXT_NODE = 2;

    private String mName = "";

    private String mValue = "";

    private int mIntValue = -1;

    private int mType;

    private Map<String, Node> mAttributes = new TreeMap<>();

    private List<Node> mChildren = new ArrayList<>();

    private Node mParent;

    public Node() {
        this.mType = 0;
    }

    public Node(String value) {
        this.mName = "text";
        this.mType = 2;
        this.mValue = value;
        int i = value.length();
        if (i > 0 && i <= 9) {
            i--;
            for (; i >= 0; i--) {
                if (!Character.isDigit(value.charAt(i)))
                    return;
            }
            this.mIntValue = Integer.parseInt(value);
        }
    }

    public Node(int value) {
        this.mName = "text";
        this.mType = 2;
        this.mValue = "" + value;
        this.mIntValue = value;
    }

    public Node(String name, String content) {
        this.mType = 1;
        this.mName = name;
        Node child = new Node(content);
        addChild(child);
    }

    public Node(String name, Attributes attributes) {
        this.mName = name;
        this.mType = 1;
        for (int i = 0; i < attributes.getLength(); i++)
            this.mAttributes.put(attributes.getQName(i), new Node(attributes.getValue(i)));
    }

    public Node(Node nodeToClone) {
        this.mName = nodeToClone.mName;
        this.mValue = nodeToClone.mValue;
        this.mIntValue = nodeToClone.mIntValue;
        this.mType = nodeToClone.mType;
        this.mAttributes = nodeToClone.mAttributes;
        this.mChildren = nodeToClone.mChildren;
    }

    public String getName() {
        return this.mName;
    }

    public String getValue() {
        return this.mValue;
    }

    public int getSize() {
        return this.mChildren.size();
    }

    public Node getAttribute(String name) {
        return this.mAttributes.get(name);
    }

    public void setAttribute(String name, Node value) {
        this.mAttributes.put(name, value);
    }

    public Node getNode(String elementName) {
        for (Iterator<Node> i = this.mChildren.iterator(); i.hasNext(); ) {
            Node node = i.next();
            if (node.getName() != null && elementName.equals(node.getName()))
                return node;
        }
        return null;
    }

    public boolean hasNode(String elementName) {
        boolean retVal = false;
        for (Iterator<Node> i = this.mChildren.iterator(); i.hasNext(); ) {
            Node node = i.next();
            String nodeName = node.getName();
            if (nodeName != null && elementName.equals(nodeName)) {
                retVal = true;
                break;
            }
        }
        return retVal;
    }

    public List<Node> getNodes(String elementName) {
        if (elementName == null || elementName.length() == 0)
            return this.mChildren;
        List<Node> nodes = new ArrayList<>();
        for (Iterator<Node> i = this.mChildren.iterator(); i.hasNext(); ) {
            Node node = i.next();
            if (node.getName() != null && elementName.equals(node.getName()))
                nodes.add(node);
        }
        return nodes;
    }

    public List<Node> getNodes() {
        return getNodes(null);
    }

    public void removeNode(String elementName) {
        for (Iterator<Node> i = this.mChildren.iterator(); i.hasNext(); ) {
            Node node = i.next();
            if (node.getName() != null && elementName.equals(node.getName())) {
                this.mChildren.remove(node);
                break;
            }
        }
    }

    public void removeNode(Node node) {
        this.mChildren.remove(node);
    }

    public int integerFromChild() throws InvalidNodeTypeException {
        Node n = getNode("text");
        if (n == null)
            throw new InvalidNodeTypeException("no text node");
        try {
            return Integer.parseInt(n.getValue());
        } catch (Exception e) {
            throw new InvalidNodeTypeException("node cannot be converted to integer");
        }
    }

    public BigInteger bigIntegerFromChild() throws InvalidNodeTypeException {
        Node n = getNode("text");
        if (n == null)
            throw new InvalidNodeTypeException("no text node");
        try {
            return new BigInteger(n.getValue());
        } catch (Exception e) {
            throw new InvalidNodeTypeException("node cannot be converted to BigInteger");
        }
    }

    public long longFromChild() throws InvalidNodeTypeException {
        Node n = getNode("text");
        if (n == null)
            throw new InvalidNodeTypeException("no text node");
        try {
            return Long.parseLong(n.getValue());
        } catch (Exception e) {
            throw new InvalidNodeTypeException("node cannot be converted to long");
        }
    }

    public float floatFromChild() throws InvalidNodeTypeException {
        Node n = getNode("text");
        if (n == null)
            throw new InvalidNodeTypeException("no text node");
        try {
            return Float.parseFloat(n.getValue());
        } catch (Exception e) {
            throw new InvalidNodeTypeException("node cannot be converted to float : n.getValue() : " + n.getValue());
        }
    }

    public double doubleFromChild() throws InvalidNodeTypeException {
        Node n = getNode("text");
        if (n == null)
            throw new InvalidNodeTypeException("no text node");
        try {
            return Double.parseDouble(n.getValue());
        } catch (Exception e) {
            throw new InvalidNodeTypeException("node cannot be converted to double : n.getValue() : " + n.getValue());
        }
    }

    public Node getParent() {
        return this.mParent;
    }

    public Node addChild(Node child) {
        child.mParent = this;
        this.mChildren.add(child);
        return child;
    }

    public Node addChild(String value) {
        Node child = new Node(value);
        child.mParent = this;
        this.mChildren.add(child);
        return child;
    }

    public Node addChild(String name, String content) {
        Node node = new Node();
        node.mType = 1;
        node.mName = name;
        Node child = new Node();
        child.mType = 2;
        child.mName = "text";
        child.mValue = content;
        node.addChild(child);
        addChild(node);
        return node;
    }

    public Node addChild(String name, Node content) {
        Node node = new Node();
        node.mType = 1;
        node.mName = name;
        node.addChild(content);
        addChild(node);
        return node;
    }

    public Node addChild(String child, Attributes attributes) {
        Node node = new Node(child, attributes);
        node.mParent = this;
        this.mChildren.add(node);
        return node;
    }

    public int compareTo(Object object) {
        Node node = (Node)object;
        if (this.mName.compareTo(node.mName) != 0)
            return this.mName.compareTo(node.mName);
        assert this.mIntValue != -1 && node.mIntValue != -1;
        if (this.mIntValue < node.mIntValue)
            return -1;
        if (this.mIntValue > node.mIntValue)
            return 1;
        int c;
        if ((c = this.mValue.compareTo(node.mValue)) != 0)
            return c;
        if (this.mType < node.mType)
            return -1;
        if (this.mType > node.mType)
            return 1;
        return 0;
    }

    private static void recursiveNodeToString(StringBuilder returnString, Node node, boolean escape, int depth) {
        Map<String, Node> attributes;
        Iterator<String> keys;
        Iterator<Node> values;
        List<Node> children;
        switch (node.mType) {
            case 2:
                returnString.append(escape ? XmlStrings.escapeString(node.mValue) : node.mValue);
                return;
            case 1:
                returnString.append(StringToolkit.indent(depth));
                returnString.append('<');
                returnString.append(node.getName());
                attributes = node.mAttributes;
                keys = attributes.keySet().iterator();
                values = attributes.values().iterator();
                while (keys.hasNext() && values.hasNext()) {
                    returnString.append(' ');
                    returnString.append(keys.next());
                    returnString.append("=\"");
                    String value = ((Node)values.next()).mValue;
                    returnString.append(escape ? XmlStrings.escapeString(value) : value);
                    returnString.append("\"");
                }
                children = node.mChildren;
                if (children.size() > 0) {
                    returnString.append('>');
                    for (Iterator<Node> n = children.iterator(); n.hasNext();)
                        recursiveNodeToString(returnString, n.next(), escape, depth + 1);
                    returnString.append("</");
                    returnString.append(node.getName());
                    returnString.append('>');
                } else {
                    returnString.append("/>");
                }
                return;
        }
        returnString.append("<UNKNOWN/>");
    }

    public void setChildString(String text) {
        Node n = getNode("text");
        if (n != null)
            n.mValue = text;
    }

    public String stringFromChild() {
        Node node = getNode("text");
        if (node == null)
            return "";
        return node.getValue();
    }

    public String childrenToString() {
        StringBuilder returnValue = new StringBuilder();
        for (Iterator<Node> i = this.mChildren.iterator(); i.hasNext();)
            recursiveNodeToString(returnValue, i.next(), false, 0);
        return returnValue.toString();
    }

    public String childrenToXML(int depth) {
        StringBuilder returnValue = new StringBuilder();
        for (Iterator<Node> i = this.mChildren.iterator(); i.hasNext();)
            recursiveNodeToString(returnValue, i.next(), true, depth);
        return returnValue.toString();
    }

    public String toString() {
        StringBuilder returnValue = new StringBuilder();
        recursiveNodeToString(returnValue, this, false, 0);
        return returnValue.toString();
    }

    public String toXML(int depth) {
        StringBuilder returnValue = new StringBuilder();
        recursiveNodeToString(returnValue, this, true, depth);
        return returnValue.toString();
    }
}
