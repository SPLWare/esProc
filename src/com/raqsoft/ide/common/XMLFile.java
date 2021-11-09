package com.raqsoft.ide.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.raqsoft.app.common.Section;
import com.raqsoft.common.MessageManager;
import com.raqsoft.ide.common.resources.IdeCommonMessage;

/**
 * Used to parse xml files
 */
public class XMLFile {

	/**
	 * XML file path
	 */
	public String xmlFile = null;

	/**
	 * XML Document
	 */
	private Document xmlDocument = null;

	/**
	 * Common MessageManager
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * Constructor
	 * 
	 * @param file
	 *            XML file path
	 * @throws Exception
	 */
	public XMLFile(String file) throws Exception {
		this(new File(file), "root");
	}

	/**
	 * Constructor
	 * 
	 * @param file
	 *            XML file path
	 * @param root
	 *            Root node name
	 * @throws Exception
	 */
	public XMLFile(File file, String root) throws Exception {
		if (file != null) {
			xmlFile = file.getAbsolutePath();
			if (file.exists() && file.isFile()) {
				loadXMLFile(new FileInputStream(file));
			} else {
				XMLFile.newXML(xmlFile, root);
			}
		} else {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			xmlDocument = docBuilder.newDocument();
			Element eRoot = xmlDocument.createElement(root);
			eRoot.normalize();
			xmlDocument.appendChild(eRoot);
		}
	}

	/**
	 * Constructor
	 * 
	 * @param is
	 *            File InputStream
	 * @throws Exception
	 */
	public XMLFile(InputStream is) throws Exception {
		loadXMLFile(is);
	}

	/**
	 * Load XML file from file input stream
	 * 
	 * @param is
	 * @throws Exception
	 */
	private void loadXMLFile(InputStream is) throws Exception {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		xmlDocument = docBuilder.parse(is);
	}

	/**
	 * Create a new XML file
	 *
	 * @param file
	 *            The name of the file to be created. If the file exists, it
	 *            will overwrite the original one.
	 * @param root
	 *            Root node name
	 * @return Return true if the creation is successful, otherwise false.
	 */
	public static XMLFile newXML(String file, String root) throws Exception {
		if (!isLegalXmlName(root)) {
			throw new Exception(IdeCommonMessage.get().getMessage(
					"xmlfile.falseroot", file, root));
		}

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document sxmlDocument = docBuilder.newDocument();
		Element eRoot = sxmlDocument.createElement(root);
		eRoot.normalize();
		sxmlDocument.appendChild(eRoot);

		writeNodeToFile(sxmlDocument, file);
		return new XMLFile(file);
	}

	/**
	 * List all sub-elements under the path path in the file file, including
	 * elements and attributes.
	 *
	 * @param path
	 *            Node path, use / as a separator, for example: "a/b/c"
	 * @return Return the section of the element and attribute under the path
	 *         node
	 */
	public Section listAll(String path) throws Exception {
		Section ss = new Section();
		Node tmpNode = getTerminalNode(path, Node.ELEMENT_NODE);
		ss.unionSection(getNodesName(tmpNode.getChildNodes()));
		ss.unionSection(getNodesName(tmpNode.getAttributes()));
		return ss;
	}

	/**
	 * List all sub-elements under path in the file
	 *
	 * @param path
	 *            Node path
	 * @return Return the section of the elements under the path node
	 */
	public Section listElement(String path) throws Exception {
		Node tmpNode = getTerminalNode(path, Node.ELEMENT_NODE);
		return getNodesName(tmpNode.getChildNodes());
	}

	/**
	 * List all attributes under path in file
	 *
	 * @param path
	 *            Node path
	 * @return Return the section of the attribute names under the path node
	 */
	public Section listAttribute(String path) throws Exception {
		Node tmpNode = getTerminalNode(path, Node.ELEMENT_NODE);
		return getNodesName(tmpNode.getAttributes());
	}

	/**
	 * List the attribute value of the path node in the file
	 *
	 * @param path
	 *            Node path
	 * @return The value of the attribute
	 */
	public String getAttribute(String path) {
		try {
			Node tmpNode = getTerminalNode(path, Node.ATTRIBUTE_NODE);
			return tmpNode.getNodeValue();
		} catch (Exception ex) {
			return "";
		}
	}

	/**
	 * Create a new element under the current path
	 *
	 * @param path
	 *            Node path
	 * @param element
	 *            The name of the element to be created
	 * @return The node path after the new element is successfully created
	 */
	public String newElement(String path, String element) throws Exception {
		Element newElement = null;
		if (!isLegalXmlName(element)) {
			throw new Exception(mm.getMessage("xmlfile.falseelement", xmlFile,
					element));
		}

		Node parent = getTerminalNode(path, Node.ELEMENT_NODE);
		NodeList nl = parent.getChildNodes();
		int i;
		for (i = 0; i < nl.getLength(); i++) {
			if (nl.item(i).getNodeName().equalsIgnoreCase(element)) {
				break;
			}
		}
		if (i >= nl.getLength()) {
			newElement = xmlDocument.createElement(element);
			parent.appendChild(newElement);
		}
		return path + "/" + element;
	}

	/**
	 * Create a new attribute under the current path
	 *
	 * @param path
	 *            Node path
	 * @param attr
	 *            The name of the attribute to be created
	 * @return Return 1 on success, -1 on failure (when the attribute with the
	 *         same name exists)
	 */
	public int newAttribute(String path, String attr) throws Exception {
		if (!isLegalXmlName(attr)) {
			throw new Exception(mm.getMessage("xmlfile.falseattr", xmlFile,
					attr));
		}

		Node parent = getTerminalNode(path, Node.ELEMENT_NODE);

		NamedNodeMap nl = parent.getAttributes();
		int i;
		for (i = 0; i < nl.getLength(); i++) {
			if (nl.item(i).getNodeName().equalsIgnoreCase(attr)) {
				break;
			}
		}
		if (i >= nl.getLength()) {
			((Element) parent).setAttribute(attr, "");
		} else {
			return -1;
		}
		return 1;
	}

	/**
	 * Delete the node specified by path
	 *
	 * @param path
	 *            Node path
	 */
	public void delete(String path) throws Exception {
		deleteAttribute(path);
		deleteElement(path);
	}

	/**
	 * Delete the element node specified by path
	 *
	 * @param path
	 *            Node path
	 */
	public void deleteElement(String path) throws Exception {
		Node nd = getTerminalNode(path, Node.ELEMENT_NODE);
		Node parent = nd.getParentNode();
		parent.removeChild(nd);
	}

	/**
	 * Whether the node path exists
	 * 
	 * @param path
	 *            Node path
	 * @return Exists when true, and does not exist when false.
	 */
	public boolean isPathExists(String path) {
		try {
			getTerminalNode(path, Node.ELEMENT_NODE);
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	/**
	 * Delete the attribute node specified by path
	 *
	 * @param path
	 *            Node path
	 */
	public void deleteAttribute(String path) throws Exception {
		int i = path.lastIndexOf('/');
		Node parent;
		if (i > 0) {
			String p = path.substring(0, i);
			parent = getTerminalNode(p, Node.ELEMENT_NODE);
		} else {
			return;
		}
		((Element) parent).removeAttribute(path.substring(i + 1));
	}

	/**
	 * Change the name of the attribute node of the path
	 *
	 * @param path
	 *            Node path
	 * @param newName
	 *            New name
	 * @throws Exception
	 */
	public void renameAttribute(String path, String newName) throws Exception {
		int i;
		i = path.lastIndexOf('/');
		if (i == -1) {
			throw new Exception(mm.getMessage("xmlfile.falsepath", xmlFile,
					path));
		}

		String value = getAttribute(path);
		deleteAttribute(path);
		setAttribute(path.substring(0, i) + "/" + newName, value);
	}

	/**
	 * Change the name of the element node of the path
	 *
	 * @param path
	 *            Node path
	 * @param newName
	 *            New name
	 * @throws Exception
	 */
	public void renameElement(String path, String newName) throws Exception {
		if (path.lastIndexOf('/') == -1) {
			throw new Exception(mm.getMessage("xmlfile.renameroot", xmlFile));
		}
		Node nd = getTerminalNode(path, Node.ELEMENT_NODE);
		Node pp = nd.getParentNode();
		NodeList childNodes = pp.getChildNodes();
		int i;
		if (childNodes != null) {
			for (i = 0; i < childNodes.getLength(); i++) {
				if (newName.equalsIgnoreCase(childNodes.item(i).getNodeName())) {
					throw new Exception(mm.getMessage("xmlfile.existnode",
							xmlFile, newName));
				}
			}
		}

		Element newNode = xmlDocument.createElement(newName);

		childNodes = nd.getChildNodes();
		if (childNodes != null) {
			for (i = 0; i < childNodes.getLength(); i++) {
				newNode.appendChild(childNodes.item(i));
			}
		}
		NamedNodeMap childAttr = nd.getAttributes();
		Node tmpNode;
		if (childAttr != null) {
			for (i = 0; i < childAttr.getLength(); i++) {
				tmpNode = childAttr.item(i);
				newNode.setAttribute(tmpNode.getNodeName(),
						tmpNode.getNodeValue());
			}
		}
		pp.replaceChild(newNode, nd);
	}

	/**
	 * Set the attribute value of the attribute node of the path
	 *
	 * @param path
	 *            Node path
	 * @param value
	 *            The attribute value to be set, null means delete the
	 *            attribute.
	 */
	public void setAttribute(String path, String value) throws Exception {
		if (path == null) {
			throw new Exception(mm.getMessage("xmlfile.nullpath", xmlFile));
		}
		if (path.trim().length() == 0) {
			throw new Exception(mm.getMessage("xmlfile.nullpath", xmlFile));
		}
		if (value == null) {
			deleteAttribute(path);
			return;
		}
		Node nd;
		int i = path.lastIndexOf('/');
		if (i > 0) {
			String p = path.substring(0, i);
			String v = path.substring(i + 1);
			newAttribute(p, v);
		}

		nd = getTerminalNode(path, Node.ATTRIBUTE_NODE);
		nd.setNodeValue(value);
	}

	/**
	 * Save XML file
	 * 
	 * @throws Exception
	 */
	public void save() throws Exception {
		save(xmlFile);
	}

	/**
	 * Save XML file
	 * 
	 * @param file
	 *            XML file path
	 * @throws Exception
	 */
	public void save(String file) throws Exception {
		writeNodeToFile(xmlDocument, file);
	}

	/**
	 * Get the byte array of the XML file
	 * 
	 * @return
	 */
	public byte[] getFileBytes() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			outputDocument(xmlDocument, baos);
		} catch (Exception ex) {
		}
		return baos.toByteArray();
	}

	/**
	 * Save XML file
	 * 
	 * @param os
	 *            File OutputStream
	 * @throws Exception
	 */
	public void save(OutputStream os) throws Exception {
		outputDocument(xmlDocument, os);
	}

	/**
	 * Write document to file
	 * 
	 * @param node
	 *            Document
	 * @param file
	 *            XML file path
	 * @throws Exception
	 */
	public static void writeNodeToFile(Document node, String file)
			throws Exception {
		outputDocument(node, file);
	}

	/**
	 * Output document
	 * 
	 * @param node
	 *            Document
	 * @param out
	 *            The object to output to. It can be:
	 *            String,OutputStream,Writer.
	 * @throws Exception
	 */
	public static void outputDocument(Document node, Object out)
			throws Exception {
		StreamResult result;
		if (out instanceof String) {
			result = new StreamResult((String) out);
		} else if (out instanceof OutputStream) {
			result = new StreamResult((OutputStream) out);
		} else if (out instanceof Writer) {
			result = new StreamResult((Writer) out);
		} else {
			return;
		}
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();

		Properties properties = transformer.getOutputProperties();
		properties.setProperty(OutputKeys.ENCODING, "UTF-8");
		properties.setProperty(OutputKeys.METHOD, "xml");
		properties.setProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperties(properties);

		DOMSource source = new DOMSource(node);
		transformer.transform(source, result);
	}

	/**
	 * Get the name Section of the nodes
	 * 
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	private Section getNodesName(Object obj) throws Exception {
		Section ss = new Section();
		if (obj == null) {
			return ss;
		}

		Node nd = null;
		if (obj instanceof NodeList) {
			NodeList nl = (NodeList) obj;
			for (int i = 0; i < nl.getLength(); i++) {
				nd = nl.item(i);
				if (nd.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				ss.unionSection(nd.getNodeName());
			}
		}

		if (obj instanceof NamedNodeMap) {
			NamedNodeMap nnm = (NamedNodeMap) obj;
			for (int i = 0; i < nnm.getLength(); i++) {
				nd = nnm.item(i);
				ss.unionSection(nd.getNodeName());
			}
		}
		return ss;
	}

	/**
	 * Locate node among nodes
	 * 
	 * @param obj
	 *            NodeList or NamedNodeMap
	 * @param nodeName
	 *            Node name
	 * @return
	 * @throws Exception
	 */
	private Node locateNode(Object obj, String nodeName) throws Exception {
		int i = 0, j = 0;
		String sTmp;
		Node tmpNode = null;
		NodeList nl;
		NamedNodeMap nnm;

		if (obj instanceof NodeList) {
			nl = (NodeList) obj;
			i = nl.getLength();
			for (j = 0; j < i; j++) {
				tmpNode = nl.item(j);
				sTmp = tmpNode.getNodeName();
				if (sTmp.equalsIgnoreCase(nodeName)) {
					break;
				}
			}
		}
		if (obj instanceof NamedNodeMap) {
			nnm = (NamedNodeMap) obj;
			i = nnm.getLength();
			for (j = 0; j < i; j++) {
				tmpNode = nnm.item(j);
				sTmp = tmpNode.getNodeName();
				if (sTmp.equalsIgnoreCase(nodeName)) {
					break;
				}
			}
		}
		if (j == i) {
			throw new Exception(mm.getMessage("xmlfile.falsenodename", xmlFile,
					nodeName));
		}
		return tmpNode;
	}

	/**
	 * Get terminal node
	 * 
	 * @param path
	 *            Node path
	 * @param type
	 *            Constants defined in org.w3c.dom.Node
	 * @return
	 * @throws Exception
	 */
	private Node getTerminalNode(String path, short type) throws Exception {
		Node tmpNode = null;
		String sNode, sLast;
		int i;
		if (path == null) {
			throw new Exception(mm.getMessage("xmlfile.nullpath1", xmlFile));
		}
		i = path.lastIndexOf('/');
		if (i == -1) {
			if (path.length() == 0) {
				return xmlDocument;
			}
			sNode = path;
			if (type == Node.ELEMENT_NODE) {
				tmpNode = locateNode(xmlDocument.getChildNodes(), sNode);
			}
		} else {
			sLast = path.substring(i + 1);
			path = path.substring(0, i);

			StringTokenizer st = new StringTokenizer(path, "/");
			NodeList childNodes = xmlDocument.getChildNodes();
			while (st.hasMoreTokens()) {
				sNode = st.nextToken();
				tmpNode = locateNode(childNodes, sNode);
				if (tmpNode == null) {
					throw new Exception(mm.getMessage("xmlfile.notexistpath",
							xmlFile, path));
				}
				childNodes = tmpNode.getChildNodes();
			}
			if (type == Node.ELEMENT_NODE) {
				tmpNode = locateNode(tmpNode.getChildNodes(), sLast);
			} else {
				tmpNode = locateNode(tmpNode.getAttributes(), sLast);
			}
		}
		if (tmpNode == null) {
			throw new Exception(mm.getMessage("xmlfile.notexistpath", xmlFile,
					path));
		}
		return tmpNode;
	}

	/**
	 * Is the legal XML name
	 * 
	 * @param input
	 *            Node name
	 * @return
	 */
	public static boolean isLegalXmlName(String input) {
		if (input == null || input.length() == 0) {
			return false;
		}
		return true;
	}
}
