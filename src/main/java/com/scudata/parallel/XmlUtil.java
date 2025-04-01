package com.scudata.parallel;

import org.w3c.dom.*;

/**
 * XML文件解析工具
 * 
 * @author Joancy
 *
 */
public class XmlUtil {

	/**
	 * 获取节点值
	 * @param node 节点
	 * @return 值
	 */
	public static String getNodeValue(org.w3c.dom.Node node) {
		if (node != null && node.getFirstChild() != null) {
			return node.getFirstChild().getNodeValue();
		}
		return null;
	}

	/**
	 * 根据名字sonName查找子节点
	 * @param pNode 父节点
	 * @param sonName 子节点名称
	 * @return 子节点，没找到时返回null
	 */
	public static Node findSonNode(Node pNode, String sonName) {
		NodeList list = pNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			org.w3c.dom.Node subNode = list.item(i);
			if (subNode.getNodeName().equalsIgnoreCase(sonName)) {
				return subNode;
			}
		}
		return null;
	}

	/**
	 * 获取节点的属性值
	 * @param node 节点对象
	 * @param attrName 属性名称
	 * @return 属性值
	 */
	public static String getAttribute(Node node, String attrName) {
		NamedNodeMap attrs = node.getAttributes();
		int i = attrs.getLength();
		for (int j = 0; j < i; j++) {
			Node tmp = attrs.item(j);
			String sTmp = tmp.getNodeName();
			if (sTmp.equalsIgnoreCase(attrName)) {
				return tmp.getNodeValue();
			}
		}
		return null;
	}

}
