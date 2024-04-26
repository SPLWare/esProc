package com.scudata.server.http;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

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

import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.common.ScudataLogger.FileHandler;
import com.scudata.dm.Env;
import com.scudata.parallel.UnitClient;
import com.scudata.parallel.UnitContext;
import com.scudata.parallel.XmlUtil;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.unit.UnitServer;

import sun.net.util.IPAddressUtil;

/**
 * Http服务器的环境配置参数类
 * 
 * @author Joancy
 *
 */
public class HttpContext extends ConfigWriter {
	public static final String HTTP_CONFIG_FILE = "HttpServer.xml";
	public static String dfxHome;

	private String host = UnitContext.getDefaultHost();// "127.0.0.1";
	private int port = 8508;
	private int maxLinks = 50;
	private boolean autoStart=false;

	private ArrayList<String> sapPath = new ArrayList<String>();

	static MessageManager mm = ParallelMessage.get();

	/**
	 * 构造函数
	 * @param showException 是否将构造异常打印到控制台，否则忽略
	 */
	public HttpContext(boolean showException) {
		try {
			InputStream inputStream = UnitContext
					.getUnitInputStream(HTTP_CONFIG_FILE);
			if (inputStream != null) {
				load(inputStream);
			}
		} catch (Exception x) {
			if (showException) {
				x.printStackTrace();
			}
		}
	}

	/**
	 * 获取缺省的访问url地址
	 * @return url地址
	 */
	public String getDefaultUrl() {
		String tmp = host;
		if (IPAddressUtil.isIPv6LiteralAddress(host)) {
			int percentIndex = host.indexOf('%');
			if (percentIndex > 0) {
				tmp = tmp.substring(0, percentIndex);
			}
			tmp = "[" + tmp + "]";
		}

		return "http://" + tmp + ":" + port;
	}

	/**
	 * 从配置文件的输入流加载环境参数
	 * @param is 配置文件输入流
	 * @throws Exception 格式出错时抛出异常
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
			throw new Exception(mm.getMessage("UnitConfig.errorxml"));
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
		String file = "http/" + UnitClient.getHostPath(host) + "_" + port + "/log/log.txt";
		File f = new File(home, file);
		File fp = f.getParentFile();
		if (!fp.exists()) {
			fp.mkdirs();
		}
		String logFile = f.getAbsolutePath();
		FileHandler lfh = ScudataLogger.newFileHandler(logFile);
		ScudataLogger.addFileHandler(lfh);
		
		buf = XmlUtil.getAttribute(root, "parallelNum");
		if (StringUtils.isValidString(buf)) {
		}

		buf = XmlUtil.getAttribute(root, "maxlinks");
		if (StringUtils.isValidString(buf)) {
			maxLinks = Integer.parseInt(buf);
		}

		String mp = Env.getMainPath();
		if(!StringUtils.isValidString( mp )) {
			Logger.info("Main path is empty.");
		}else {
			File main = new File( mp );
			if( main.exists() ) {
				String mainPath = main.getAbsolutePath();
				addSubdir2Sappath( main, mainPath );//必须加装子目录，否则子目录下的splx文件，简写时，访问不到 xq 2023年9月6日
			}
		}
		/*buf = XmlUtil.getAttribute(root, "sapPath");//这个子目录不缓存到配置文件了，一旦量很大，解析时太慢，会影响到界面操作了
		if (StringUtils.isValidString(buf)) {
			ArgumentTokenizer at = new ArgumentTokenizer(buf, ',');
			while (at.hasMoreTokens()) {
				sapPath.add(at.nextToken().trim());
			}
		}*/
	}
	
	private void addSubdir2Sappath( File main, String mainPath ) {
		File[] fs = main.listFiles();
		if(fs==null) {
			return;
		}
		for( int i = 0; i < fs.length; i++ ) {
			if( !fs[i].isDirectory() ) continue;
			String path = fs[i].getAbsolutePath();
			path = path.substring( mainPath.length() );
			path = StringUtils.replace( path, "\\", "/" );
			sapPath.add( path );
			
			addSubdir2Sappath( fs[i], mainPath );
		}
	}

	public void save(OutputStream out) throws SAXException {
		Result resultxml = new StreamResult(out);
		handler.setResult(resultxml);
		level = 0;
		handler.startDocument();
		// 设置根节点和版本
		String paths = "";
		for (int i = 0; i < sapPath.size(); i++) {
			if (paths.length() > 0)
				paths += ",";
			paths += sapPath.get(i);
		}
		handler.startElement("", "", "Server", getAttributesImpl(new String[] {
				ConfigConsts.VERSION, "1", "host", host, "port", port + "", "autostart", autoStart + "",
				"maxlinks", maxLinks + "",  //parallelNum + "",
				"sapPath", paths }));

		handler.endElement("", "", "Server");
		// 文档结束,同步到磁盘
		handler.endDocument();
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public boolean isAutoStart() {
		return autoStart;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setAutoStart(boolean as) {
		this.autoStart = as;
	}

	public int getMaxLinks() {
		return maxLinks;
	}

	public void setMaxLinks(int m) {
		this.maxLinks = m;
	}

	public ArrayList<String> getSapPath() {
		return sapPath;
	}

	public void setSapPath(ArrayList<String> paths) {
		sapPath = paths;
	}

	public String toString() {
		return host + ":" + port;
	}
}