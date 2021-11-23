package com.scudata.parallel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.scudata.app.config.ConfigConsts;
import com.scudata.app.config.ConfigWriter;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.parallel.XmlUtil;
import com.scudata.resources.ParallelMessage;

/**
 * 分机配置类
 * @author Joancy
 *
 */
public class UnitConfig extends ConfigWriter {
	// version 3
	private int tempTimeOut = 12; // 临时文件存活时间，秒为单位，0为永生，单位都是小时。
	private int proxyTimeOut = 12; // 文件以及游标代理的过期时间，秒为单位，0为永生，单位都是小时。
	private int interval = 30 * 60; // 检查代理或者临时文件过期的时间间隔，0为不检查过期。单位秒
	boolean autoStart=false;
	private List<Host> hosts = null;
	
//	客户端白名单
	private boolean checkClient = false;
	private List<String> enabledClientsStart = null;
	private List<String> enabledClientsEnd = null;
	
	MessageManager mm = ParallelMessage.get();
	
	/**
	 * 从配置文件流加载配置信息
	 * @param is 配置文件流
	 * @throws Exception 加载出错时抛出异常
	 */
	public void load(InputStream is) throws Exception {
		load(is, true);
	}
	
	/**
	 * 从配置文件的字节数据加载配置信息
	 * @param buf 配置文件字节数据
	 * @throws Exception 加载出错抛出异常
	 */
	public void load(byte[] buf) throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		load(bais, true);
		bais.close();
	}
	
	/**
	 * 根据ip和port产生Host对象，并将Host对象维护到hosts队列
	 * @param hosts hosts队列
	 * @param ip IP地址
	 * @param port 端口号
	 * @return 由ip跟port产生Host对象
	 */
	public static Host getHost(List<Host> hosts, String ip, int port) {
		Host h = null;
		for (int i = 0, size = hosts.size(); i < size; i++) {
			h = hosts.get(i);
			if (h.getIp().equals(ip) && h.getPort()==port) {
				return h;
			}
		}
		h = new Host(ip,port);
		hosts.add(h);
		return h;
	}

	/**
	 * 加载配置文件输入流
	 * @param is 配置文件流
	 * @param showDebug 是否打出加载配置的调试信息
	 * @throws Exception 文件格式错误是抛出异常
	 */
	public void load(InputStream is, boolean showDebug) throws Exception {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document xmlDocument = docBuilder.parse(is);
		NodeList nl = xmlDocument.getChildNodes();
		Node root = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getNodeName().equalsIgnoreCase("Server")) {
				root = n;
			}
		}
		if (root == null) {
			throw new Exception(mm.getMessage("UnitConfig.errorxml"));
		}
		String ver = XmlUtil.getAttribute(root, "Version");
		if (ver==null || Integer.parseInt( ver )<3) {
			throw new RuntimeException(mm.getMessage("UnitConfig.updateversion",UnitContext.UNIT_XML));
		}

		// version 3
		// Server 配置
		Node subNode = XmlUtil.findSonNode(root, "tempTimeout");
		String buf = XmlUtil.getNodeValue(subNode);
		if (StringUtils.isValidString(buf)) {
			tempTimeOut = Integer.parseInt(buf);
			if (tempTimeOut > 0) {
				if (showDebug)
					Logger.debug("Using TempTimeOut=" + tempTimeOut
							+ " hour(s).");
			}
		}

		subNode = XmlUtil.findSonNode(root, "interval");
		buf = XmlUtil.getNodeValue(subNode);
		if (StringUtils.isValidString(buf)) {
			int t = Integer.parseInt(buf);
			if (t > 0)
				interval = t;// 设置不正确时，使用缺省检查间隔
		}

		subNode = XmlUtil.findSonNode(root, "autostart");
		buf = XmlUtil.getNodeValue(subNode);
		if (StringUtils.isValidString(buf)) {
			autoStart = new Boolean(buf);
		}

		subNode = XmlUtil.findSonNode(root, "proxyTimeout");
		buf = XmlUtil.getNodeValue(subNode);
		if (StringUtils.isValidString(buf)) {
			proxyTimeOut = Integer.parseInt(buf);
			if (proxyTimeOut > 0) {
				if (showDebug)
					Logger.debug("Using ProxyTimeOut=" + proxyTimeOut
							+ " hour(s).");
			}
		}

		Node nodeHosts = XmlUtil.findSonNode(root, "Hosts");
		NodeList hostsList = nodeHosts.getChildNodes();
		hosts = new ArrayList<Host>();

		for (int i = 0; i < hostsList.getLength(); i++) {
			Node xmlNode = hostsList.item(i);
			if (!xmlNode.getNodeName().equalsIgnoreCase("Host")) {
				continue;
			}
			buf = XmlUtil.getAttribute(xmlNode, "ip");
			String sPort = XmlUtil.getAttribute(xmlNode, "port");
			Host host = new Host(buf,Integer.parseInt(sPort));
			
			buf = XmlUtil.getAttribute(xmlNode, "maxTaskNum");
			if(StringUtils.isValidString(buf)){
				host.setMaxTaskNum(Integer.parseInt(buf));
			}
			
			buf = XmlUtil.getAttribute(xmlNode, "preferredTaskNum");
			if(StringUtils.isValidString(buf)){
				host.setPreferredTaskNum(Integer.parseInt(buf));
			}
			
			hosts.add(host);
		}// hosts
		
		Node nodeECs = XmlUtil.findSonNode(root, "EnabledClients");
		buf = XmlUtil.getAttribute(nodeECs, "check");
		if (StringUtils.isValidString(buf)){
			checkClient = new Boolean(buf);
		}

		NodeList ecList = nodeECs.getChildNodes();
		enabledClientsStart = new ArrayList<String>();
		enabledClientsEnd = new ArrayList<String>();

		for (int i = 0; i < ecList.getLength(); i++) {
			Node xmlNode = ecList.item(i);
			if (!xmlNode.getNodeName().equalsIgnoreCase("Host"))
				continue;
			buf = XmlUtil.getAttribute(xmlNode, "start");
			if (!StringUtils.isValidString(buf))
				continue;
			enabledClientsStart.add(buf);
			buf = XmlUtil.getAttribute(xmlNode, "end");
			enabledClientsEnd.add(buf);
		}
	}

	/**
	 * 将配置文件转换为字节数据
	 * @return 字节数据
	 * @throws Exception 转换出错时抛出异常
	 */
	public byte[] toFileBytes() throws Exception{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		save(baos);
		return baos.toByteArray();
	}
	
	/**
	 * 将配置文件信息保存到输出流out
	 * @param out 输出流
	 * @throws SAXException 写出错时抛出异常
	 */
	public void save(OutputStream out) throws SAXException {
		Result resultxml = new StreamResult(out);
		handler.setResult(resultxml);
		level = 0;
		handler.startDocument();
		// 设置根节点和版本
		handler.startElement("", "", "SERVER", getAttributesImpl(new String[] {
				ConfigConsts.VERSION, "3" }));
		level = 1;
		writeAttribute("TempTimeOut", tempTimeOut + "");
		writeAttribute("Interval", interval + "");
		writeAttribute("AutoStart", autoStart + "");
		writeAttribute("ProxyTimeOut", proxyTimeOut + "");

		startElement("Hosts", null);
		if (hosts != null) {
			for (int i = 0, size = hosts.size(); i < size; i++) {
				level = 2;
				Host h = hosts.get(i);
				startElement("Host", getAttributesImpl(new String[] { "ip",
						h.ip,"port",h.port+"",
						"maxTaskNum",h.maxTaskNum+"","preferredTaskNum",h.preferredTaskNum+""}));
				endElement("Host");
			}
			level = 1;
			endElement("Hosts");
		} else {
			endEmptyElement("Hosts");
		}
		
		level = 1;
		startElement("EnabledClients", getAttributesImpl(new String[] { "check",
				checkClient+"" }));
		if (enabledClientsStart != null) {
			level = 2;
			for (int i = 0, size = enabledClientsStart.size(); i < size; i++) {
				String start = enabledClientsStart.get(i);
				String end = enabledClientsEnd.get(i);
				startElement("Host", getAttributesImpl(new String[] {
						"start",start,"end",end }));
				endElement("Host");
			}
			level = 1;
			endElement("EnabledClients");
		} else {
			endEmptyElement("EnabledClients");
		}
		
		handler.endElement("", "", "SERVER");
		// 文档结束,同步到磁盘
		handler.endDocument();
	}

	/**
	 * 获取临时文件超时时间，单位统一为小时
	 * 方法同getTempTimeOutHour，保留用于兼容
	 * @return 超时时间
	 */
	public int getTempTimeOut() {
		return tempTimeOut;
	}

	/**
	 * 获取临时文件超时时间，单位小时
	 * @return 超时时间
	 */
	public int getTempTimeOutHour() {
		return tempTimeOut;
	}

	/**
	 * 设置临时文件超时时间
	 * @param tempTimeOut 时间
	 */
	public void setTempTimeOut(int tempTimeOut) {
		this.tempTimeOut = tempTimeOut;
	}

	/**
	 * 按小时设置超时时间，方法同setTempTimeOut
	 * 保留用于代码兼容
	 * @param tempTimeOutHour 时间
	 */
	public void setTempTimeOutHour(int tempTimeOutHour) {
		this.tempTimeOut = tempTimeOutHour;
	}

	public int getProxyTimeOut() {
		return proxyTimeOut;
	}

	public boolean isAutoStart(){
		return autoStart;
	}
	public void setAutoStart(boolean as){
		autoStart = as;
	}
	/**
	 * 取代理对象生存时间（单位为小时）
	 * @return 代理超时时间
	 */
	public int getProxyTimeOutHour() {
		return proxyTimeOut;
	}

	/**
	 * 设置代理超时时间
	 * @param proxyTimeOut 超时时间
	 */
	public void setProxyTimeOut(int proxyTimeOut) {
		this.proxyTimeOut = proxyTimeOut;
	}

	/**
 * 方法同setProxyTimeOut
 * @param proxyTimeOutHour
 */
	public void setProxyTimeOutHour(int proxyTimeOutHour) {
		this.proxyTimeOut = proxyTimeOutHour;// * 3600;
	}

	/**
	 * 检查是否超时的时间间隔(单位为秒)
	 * @return 时间间隔
	 */
	public int getInterval() {
		return interval;
	}

	/**
	 * 设置检查超时的时间间隔
	 * @param interval 时间间隔
	 */
	public void setInterval(int interval) {
		this.interval = interval;
	}

	/**
	 * 列出当前分机下的所有进程地址
	 * @return 进程主机列表
	 */
	public List<Host> getHosts() {
		return hosts;
	}

	/**
	 * 设置进程主机列表
	 * @param hosts 进程主机列表
	 */
	public void setHosts(List<Host> hosts) {
		this.hosts = hosts;
	}

	/**
	 * 是否校验客户端
	 * @return
	 */
	public boolean isCheckClients() {
		return checkClient;
	}

	public void setCheckClients(boolean b) {
		this.checkClient = b;
	}

	public List<String> getEnabledClientsStart() {
		return enabledClientsStart;
	}

	public void setEnabledClientsStart(List<String> enHosts) {
		this.enabledClientsStart = enHosts;
	}
	
	public List<String> getEnabledClientsEnd() {
		return enabledClientsEnd;
	}

	public void setEnabledClientsEnd(List<String> enHosts) {
		this.enabledClientsEnd = enHosts;
	}


	public static class Host {
//		适合作业数缺省为CPU个数,可能被废弃
		int preferredTaskNum = Runtime.getRuntime().availableProcessors();
		
		int maxTaskNum = preferredTaskNum*2;
		String ip;
		int port;
		
		public Host(String ip, int port) {
			this.ip = ip;
			this.port = port;
		}

		public String getIp() {
			return ip;
		}
		
		public int getPort(){
			return port;
		}
		
		public int getMaxTaskNum(){
			return maxTaskNum;
		}
		public void setMaxTaskNum(int max){
			maxTaskNum = max;
		}
		
		public int getPreferredTaskNum(){
			return preferredTaskNum;
		}
		public void setPreferredTaskNum(int num){
			preferredTaskNum = num;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(ip);
			sb.append(":[");
			sb.append(port);
			sb.append("]");
			return sb.toString();
		}

	}

}
