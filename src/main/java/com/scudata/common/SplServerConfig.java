package com.scudata.common;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.scudata.parallel.XmlUtil;

/**	
<SERVER>
    <TempTimeOut>12</TempTimeOut>
    <Interval>0</Interval>
    <Backlog>10</Backlog>
    <ProxyTimeOut>12</ProxyTimeOut>
    <SplConfig>d:/path/raqsofCofig.xml</SplConfig>
    <LogPath>d:/sp.log</LogPath>
	<SplHome></SplHome>
	<JVMArgs></JVMArgs>
</SERVER>
**/
public class SplServerConfig {
	public String tempTimeOut,interval,backlog,proxyTimeOut,splConfig,logPath;
	public String splHome,JVMArgs;
	
	public static SplServerConfig getCfg(InputStream is) throws Exception {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document xmlDocument = docBuilder.parse(is);
		NodeList nl = xmlDocument.getChildNodes();
		Node root = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getNodeName().equalsIgnoreCase("SERVER")) {
				root = n;
			}
		}
		if (root == null) {
			throw new Exception( "Invalid config file." );
		}
		SplServerConfig ssc = new SplServerConfig();
		Node subNode = XmlUtil.findSonNode(root, "splHome");
		if(subNode!=null) {
			ssc.splHome = XmlUtil.getNodeValue(subNode);
		}else {
			throw new Exception("splHome is not specified.");
		}
		
		subNode = XmlUtil.findSonNode(root, "JVMArgs");
		if(subNode!=null) {
			ssc.JVMArgs = XmlUtil.getNodeValue(subNode);
		}
//server
		subNode = XmlUtil.findSonNode(root, "TempTimeOut");
		if(subNode!=null) {
			ssc.tempTimeOut = XmlUtil.getNodeValue(subNode);
		}
		subNode = XmlUtil.findSonNode(root, "ProxyTimeOut");
		if(subNode!=null) {
			ssc.proxyTimeOut = XmlUtil.getNodeValue(subNode);
		}
		subNode = XmlUtil.findSonNode(root, "Interval");
		if(subNode!=null) {
			ssc.interval = XmlUtil.getNodeValue(subNode);
		}
		subNode = XmlUtil.findSonNode(root, "Backlog");
		if(subNode!=null) {
			ssc.backlog = XmlUtil.getNodeValue(subNode);
		}
		subNode = XmlUtil.findSonNode(root, "LogPath");
		if(subNode!=null) {
			ssc.logPath = XmlUtil.getNodeValue(subNode);
		}
		subNode = XmlUtil.findSonNode(root, "SplConfig");
		if(subNode!=null) {
			ssc.splConfig = XmlUtil.getNodeValue(subNode);
		}
		return ssc;
	}

}
