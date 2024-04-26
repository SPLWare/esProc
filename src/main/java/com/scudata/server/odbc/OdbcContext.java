package com.scudata.server.odbc;

import java.io.File;
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
import com.scudata.common.ScudataLogger;
import com.scudata.common.StringUtils;
import com.scudata.common.ScudataLogger.FileHandler;
import com.scudata.parallel.UnitClient;
import com.scudata.parallel.UnitContext;
import com.scudata.parallel.XmlUtil;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.ConnectionProxyManager;
import com.scudata.server.unit.UnitServer;

/**
 * ODBC服务器上下文
 */
public class OdbcContext extends ConfigWriter {
	public static final String ODBC_CONFIG_FILE = "OdbcServer.xml";

	private String host = UnitContext.getDefaultHost();//"127.0.0.1";
	private int port = 8501, timeOut = 2; // 临时文件存活时间，小时为单位，0为不检查超时

	// Connection
	private int conMax = 10;
	private int conTimeOut = 2;// 连接存活时间，小时为单位，0为不检查超时
	private int conPeriod = 5; // 检查代理或者临时文件过期的时间间隔，0为不检查过期。文件以及游标代理的过期时间,单位秒
	private boolean autoStart=false;
	
	private List<User> users = null;

	/**
	 * 创建odbc服务器上下文
	 */
	public OdbcContext(){
		try {
			InputStream inputStream = UnitContext.getUnitInputStream(ODBC_CONFIG_FILE);
			if (inputStream != null) {
				load(inputStream);
			}
		}catch (Exception x) {
			x.printStackTrace();
		}
	}

	/**
	 * 加载配置
	 * @param is 配置文件输入流
	 * @throws Exception
	 */
	public void load(InputStream is) throws Exception {
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
			throw new Exception(ParallelMessage.get().getMessage("UnitConfig.errorxml"));
		}

		// Server 配置
		String buf = XmlUtil.getAttribute(root, "host");
		if (StringUtils.isValidString(buf)) {
			host = buf;
		}

		buf = XmlUtil.getAttribute(root, "port");
		if (StringUtils.isValidString(buf)) {
			port = Integer.parseInt(buf);
		}

		buf = XmlUtil.getAttribute(root, "autostart");
		if (StringUtils.isValidString(buf)) {
			autoStart = Boolean.parseBoolean(buf);
		}

		// 固定输出日志到控制台， 和 start.home/nodes/[ip_port]/log 目录下
		String home = UnitServer.getHome();
		String file = "odbc/" + UnitClient.getHostPath(host) + "_" + port + "/log/log.txt";
		File f = new File(home, file);
		File fp = f.getParentFile();
		if (!fp.exists()) {
			fp.mkdirs();
		}
		String logFile = f.getAbsolutePath();
		FileHandler lfh = ScudataLogger.newFileHandler(logFile);
		ScudataLogger.addFileHandler(lfh);
		
		buf = XmlUtil.getAttribute(root, "timeout");
		if (StringUtils.isValidString(buf)) {
			timeOut = Integer.parseInt(buf);
		}

		Node conNode = XmlUtil.findSonNode(root, "Connection");
		Node subNode = XmlUtil.findSonNode(conNode, "Max");
		buf = XmlUtil.getNodeValue(subNode);
		if (StringUtils.isValidString(buf)) {
			int t = Integer.parseInt(buf);
			if (t > 0)
				conMax = t;
		}

		subNode = XmlUtil.findSonNode(conNode, "Timeout");
		buf = XmlUtil.getNodeValue(subNode);
		if (StringUtils.isValidString(buf)) {
			int t = Integer.parseInt(buf);
			if (t > 0)
				conTimeOut = t;
		}

		subNode = XmlUtil.findSonNode(conNode, "Period");
		buf = XmlUtil.getNodeValue(subNode);
		if (StringUtils.isValidString(buf)) {
			int t = Integer.parseInt(buf);
			if (t > 0)
				conPeriod = t;
		}

		Node usersNode = XmlUtil.findSonNode(root, "Users");
		NodeList userList = usersNode.getChildNodes();

		users = new ArrayList<User>();
		for (int i = 0, size = userList.getLength(); i < size; i++) {
			Node xmlNode = userList.item(i);
			if (!(xmlNode.getNodeName().equalsIgnoreCase("User")))
				continue;
			User user = new User();
			buf = XmlUtil.getAttribute(xmlNode, "name");
			user.name = buf;

			buf = XmlUtil.getAttribute(xmlNode, "password");
			user.password = buf;

			buf = XmlUtil.getAttribute(xmlNode, "admin");
			if (StringUtils.isValidString(buf)) {
				user.admin = Boolean.parseBoolean(buf);
			}

			users.add(user);
		}

	}

	/**
	 * 保存配置到输出流
	 * @param out 输出流
	 * @throws SAXException
	 */
	public void save(OutputStream out) throws SAXException {
		Result resultxml = new StreamResult(out);
		handler.setResult(resultxml);
		level = 0;
		handler.startDocument();
		// 设置根节点和版本
		handler.startElement("", "", "Server", getAttributesImpl(new String[] {
				ConfigConsts.VERSION, "1", "host", host, "port", port + "","autostart", autoStart + "",
				"timeout", timeOut + ""})); 

		level = 1;
		startElement("Connection", null);
		level = 2;
		writeAttribute("Max", conMax + "");
		writeAttribute("Timeout", conTimeOut + "");
		writeAttribute("Period", conPeriod + "");
		level = 1;
		endElement("Connection");

		startElement("Users", null);
		if (users != null) {
			for (int i = 0, size = users.size(); i < size; i++) {
				level = 2;
				User u = users.get(i);
				startElement(
						"User",
						getAttributesImpl(new String[] { "name", u.name,
								"password", u.password, "admin", u.admin + "" }));
				endElement("User");
			}
			level = 1;
			endElement("Users");
		} else {
			endEmptyElement("Users");
		}

		handler.endElement("", "", "Server");
		// 文档结束,同步到磁盘
		handler.endDocument();
	}

	/**
	 * 获取服务器IP
	 * @return IP地址
	 */
	public String getHost() {
		return host;
	}

	/**
	 * 获取端口号
	 * @return 端口号
	 */
	public int getPort() {
		return port;
	}

	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * 设置端口号
	 * @param port 端口
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 设置是否自动启动
	 * @param as 是否自动启动
	 */
	public void setAutoStart(boolean as) {
		this.autoStart = as;
	}

	/**
	 * 获取服务器是否自动启动的
	 * @return 是否自启动
	 */
	public boolean isAutoStart() {
		return autoStart;
	}
	
	/**
	 * 获取连接超时的时间
	 * @return 超时时间
	 */
	public int getTimeOut() {
		return timeOut;
	}

	/**
	 * 设置临时文件存活时间，小时为单位，0为不检查超时
	 * @param timeout 超时时间
	 */
	public void setTimeOut(int timeout) {
		this.timeOut = timeout;
	}

	/**
	 * 获取最大连接数目
	 * @return 最大连接数
	 */
	public int getConMax() {
		return conMax;
	}

	/**
	 * 设置最大连接数
	 * @param max 最大连接数
	 */
	public void setConMax(int max) {
		this.conMax = max;
	}

	/**
	 * 获取连接超时的时间
	 * @return 连接超时的时间
	 */
	public int getConTimeOut() {
		return conTimeOut;
	}
	/**
	 * 设置连接存活时间，小时为单位，0为不检查超时
	 * @param cto 时间
	 */
	public void setConTimeOut(int cto) {
		this.conTimeOut = cto;
	}

	/**
	 * 获取检查超时间隔
	 * @return 超时检查间隔 
	 */
	public int getConPeriod() {
		return this.conPeriod;
	}

	/**
	 * 设置检查代理或者临时文件过期的时间间隔，0为不检查过期。
	 * 文件以及游标代理的过期时间,单位秒
	 */
	public void setConPeriod(int period) {
		this.conPeriod = period;
	}

	/**
	 * 获取用户列表
	 * @return 用户列表
	 */
	public List<User> getUserList() {
		return users;
	}

	/**
	 * 设置用户列表
	 * @param users 用户列表
	 */
	public void setUserList(List<User> users) {
		this.users = users;
	}

	/**
	 * 实现toString接口
	 */
	public String toString() {
		return host + ":" + port;
	}

	/**
	 * 检查用户是否存在
	 * @param user 用户名
	 * @return  存在时返回true，否则返回false
	 */
	public boolean isUserExist(String user) {
		if (users == null) {
			return true;
		}
		for (User u : users) {
			if (u.getName().equalsIgnoreCase(user))
				return true;
		}
		return false;
	}

	/**
	 * 校验用户合法性
	 * @param user 用户名
	 * @param password 密码
	 * @return 校验通过返回true，否则返回false
	 * @throws Exception
	 */
	public boolean checkUser(String user, String password) throws Exception{
		ConnectionProxyManager cpm = ConnectionProxyManager.getInstance(); 
		if(cpm.size()>=conMax){
			throw new Exception("Exceed server's max connections, login user:"+user);
		}
		int size = users.size();
		for (int i = 0; i < size; i++) {
			User u = users.get(i);
			if (u.getName().equalsIgnoreCase(user)) {
				if (u.getPassword().equals(password)) {
					return true;
				} else {
					throw new Exception("Invalid password.");
				}
			}
		}
		throw new Exception("Invalid user name.");
	}

	// 返回1表示正确，其它错误
	public static class User {
		private String name = null;
		private String password = null;
		private boolean admin = false;

		public User() {
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public boolean isAdmin() {
			return admin;
		}

		public void setAdmin(boolean admin) {
			this.admin = admin;
		}
	}
}