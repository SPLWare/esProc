package com.raqsoft.app.config;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.raqsoft.app.common.Segment;
import com.raqsoft.common.DBConfig;
import com.raqsoft.common.JNDIConfig;
import com.raqsoft.common.StringUtils;
import com.raqsoft.parallel.UnitConfig;

/**
 * Used to write out raqsoftConfig.xml
 */
public class ConfigWriter {
	protected TransformerHandler handler = null;
	/**
	 * Element level, used to control XML indentation.
	 */
	protected int level = 0;
	/**
	 * Indent each level by 4 spaces (a tab).
	 */
	protected final String tab = "    ";
	/**
	 * System line separator
	 */
	protected final String separator = System.getProperties()
			.getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1 ? "\n"
			: System.getProperties().getProperty("line.separator");

	/**
	 * Constructor
	 */
	public ConfigWriter() {
		try {
			SAXTransformerFactory fac = (SAXTransformerFactory) SAXTransformerFactory
					.newInstance();
			handler = fac.newTransformerHandler();
			Transformer transformer = handler.getTransformer();
			/* Set the encoding method used for output */
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			/* Whether to automatically add extra blanks */
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			/* Whether to ignore the xml declaration */
			transformer
					.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Node start
	 * 
	 * @param objectElement
	 * @param attrs
	 * @throws SAXException
	 */
	protected void startElement(String objectElement, AttributesImpl attrs)
			throws SAXException {
		if (attrs == null) {
			attrs = new AttributesImpl();
		}
		appendTab();
		if (objectElement != null) {
			handler.startElement("", "", objectElement, attrs);
		}
	}

	/**
	 * End of node
	 * 
	 * @param objectElement
	 * @throws SAXException
	 */
	protected void endElement(String objectElement) throws SAXException {
		appendTab();
		if (objectElement != null) {
			handler.endElement("", "", objectElement);
		}
	}

	/**
	 * End of empty node
	 * 
	 * @param objectElement
	 * @throws SAXException
	 */
	protected void endEmptyElement(String objectElement) throws SAXException {
		handler.endElement("", "", objectElement);
	}

	/**
	 * Write out attribute
	 * 
	 * @param key
	 * @param value
	 * @throws SAXException
	 */
	protected void writeAttribute(String key, String value) throws SAXException {
		if (value == null)
			value = "";
		appendTab();
		handler.startElement("", "", key, new AttributesImpl());
		handler.characters(value.toCharArray(), 0, value.length());
		handler.endElement("", "", key);
	}

	/**
	 * Increase indentation based on level
	 * 
	 * @throws SAXException
	 */
	private void appendTab() throws SAXException {
		StringBuffer sb = new StringBuffer(separator);
		for (int i = 0; i < level; i++) {
			sb.append(tab);
		}
		String indent = sb.toString();
		handler.characters(indent.toCharArray(), 0, indent.length());
	}

	/**
	 * Write out the configuration file. Version 2 changes: logLevel is moved
	 * from Esproc to Runtime.
	 * 
	 * @param out
	 * @param config
	 * @throws Exception
	 */
	public void write(OutputStream out, RaqsoftConfig config) throws Exception {
		Result resultxml = new StreamResult(out);
		handler.setResult(resultxml);
		level = 0;
		handler.startDocument();
		/* Set the root node and version */
		handler.startElement("", "", ConfigConsts.CONFIG,
				getAttributesImpl(new String[] { ConfigConsts.VERSION, "2" }));
		writeRuntime(config);
		writeInit(config);
		writeServer(config);
		writeJDBC(config);
		handler.endElement("", "", ConfigConsts.CONFIG);
		handler.endDocument();
	}

	/**
	 * Write the runtime node
	 * 
	 * @param config
	 * @throws SAXException
	 */
	protected void writeRuntime(RaqsoftConfig config) throws SAXException {
		level = 1;
		startElement(ConfigConsts.RUNTIME, null);
		writeDBList(config);
		writeXmlaList(config);
		writeRuntimeEsproc(config);
		writeLogger(config);
		level = 1;
		endElement(ConfigConsts.RUNTIME);
	}

	protected void writeDBList(RaqsoftConfig config) throws SAXException {
		List<DBConfig> dbList = config.getDBList();
		List<String> autoConnectedList = config.getAutoConnectList();
		level = 2;
		startElement(ConfigConsts.DB_LIST, null);
		if (dbList != null) {
			DBConfig dbConfig;
			for (int i = 0, size = dbList.size(); i < size; i++) {
				dbConfig = dbList.get(i);
				level = 3;
				startElement(ConfigConsts.DB, getAttributesImpl(new String[] {
						ConfigConsts.NAME, dbConfig.getName() }));
				level = 4;
				writeNameValueElement(ConfigConsts.DB_URL, dbConfig.getUrl());
				writeNameValueElement(ConfigConsts.DB_DRIVER,
						dbConfig.getDriver());
				writeNameValueElement(ConfigConsts.DB_TYPE,
						dbConfig.getDBType() + "");
				writeNameValueElement(ConfigConsts.DB_USER, dbConfig.getUser());
				String pwd = dbConfig.getPassword();
				writeNameValueElement(ConfigConsts.DB_PASSWORD, pwd);
				writeNameValueElement(ConfigConsts.DB_BATCH_SIZE,
						dbConfig.getBatchSize() + "");
				writeNameValueElement(
						ConfigConsts.DB_AUTO_CONNECT,
						String.valueOf(autoConnectedList != null
								&& autoConnectedList.contains(dbConfig
										.getName())));
				writeNameValueElement(ConfigConsts.DB_USE_SCHEMA,
						String.valueOf(dbConfig.isUseSchema()));
				writeNameValueElement(ConfigConsts.DB_ADD_TILDE,
						String.valueOf(dbConfig.isAddTilde()));
				String extend = dbConfig.getExtend();
				if (extend != null && extend.trim().length() > 0) {
					Segment s = new Segment(extend);
					Iterator it = s.keySet().iterator();
					while (it.hasNext()) {
						String key = (String) it.next();
						String value = s.get(key);
						writeExtendedDBElement(key, value);
					}
				}
				if (StringUtils.isValidString(dbConfig.getDBCharset())) {
					writeNameValueElement(ConfigConsts.DB_CHARSET,
							dbConfig.getDBCharset());
				}
				if (StringUtils.isValidString(dbConfig.getClientCharset())) {
					writeNameValueElement(ConfigConsts.DB_CLIENT_CHARSET,
							dbConfig.getClientCharset());
				}
				writeNameValueElement(ConfigConsts.DB_TRANS_CONTENT,
						String.valueOf(dbConfig.getNeedTranContent()));
				writeNameValueElement(ConfigConsts.DB_TRANS_SENTENCE,
						String.valueOf(dbConfig.getNeedTranSentence()));
				writeNameValueElement(ConfigConsts.DB_CASE_SENTENCE,
						String.valueOf(dbConfig.isCaseSentence()));
				level = 3;
				endElement(ConfigConsts.DB);
			}
			level = 2;
			endElement(ConfigConsts.DB_LIST);
		} else {
			endEmptyElement(ConfigConsts.DB_LIST);
		}
	}

	/**
	 * Write the Xmla list node
	 * 
	 * @param config
	 * @throws SAXException
	 */
	protected void writeXmlaList(RaqsoftConfig config) throws SAXException {
		List<Xmla> xmlaList = config.getXmlaList();
		if (xmlaList == null)
			return;
		level = 2;
		startElement(ConfigConsts.XMLA_LIST, null);
		if (xmlaList != null) {
			Xmla xmla;
			for (int i = 0, size = xmlaList.size(); i < size; i++) {
				xmla = xmlaList.get(i);
				level = 3;
				startElement(ConfigConsts.XMLA, getAttributesImpl(new String[] {
						ConfigConsts.NAME, xmla.getName() }));
				level = 4;
				writeNameValueElement(ConfigConsts.XMLA_TYPE, xmla.getType());
				writeNameValueElement(ConfigConsts.XMLA_URL, xmla.getUrl());
				writeNameValueElement(ConfigConsts.XMLA_CATALOG,
						xmla.getCatalog());
				writeNameValueElement(ConfigConsts.XMLA_USER, xmla.getUser());
				writeNameValueElement(ConfigConsts.XMLA_PASSWORD,
						xmla.getPassword());
				level = 3;
				endElement(ConfigConsts.XMLA);
			}
			level = 2;
			endElement(ConfigConsts.XMLA_LIST);
		}
	}

	/**
	 * Write the esproc node
	 * 
	 * @param config
	 * @throws SAXException
	 */
	protected void writeRuntimeEsproc(RaqsoftConfig config) throws SAXException {
		level = 2;
		startElement(ConfigConsts.ESPROC, null);
		level = 3;
		writeAttribute(ConfigConsts.CHAR_SET, config.getCharSet());
		List<String> paths = config.getDfxPathList();
		if (paths != null && !paths.isEmpty()) {
			startElement(ConfigConsts.DFX_PATH_LIST, null);
			level = 4;
			for (int i = 0; i < paths.size(); i++) {
				writeAttribute(ConfigConsts.DFX_PATH, (String) paths.get(i));
			}
			level = 3;
			endElement(ConfigConsts.DFX_PATH_LIST);
		}
		writeAttribute(ConfigConsts.DATE_FORMAT, config.getDateFormat());
		writeAttribute(ConfigConsts.TIME_FORMAT, config.getTimeFormat());
		writeAttribute(ConfigConsts.DATE_TIME_FORMAT,
				config.getDateTimeFormat());

		writeAttribute(ConfigConsts.MAIN_PATH, config.getMainPath());
		writeAttribute(ConfigConsts.TEMP_PATH, config.getTempPath());
		writeAttribute(ConfigConsts.BUF_SIZE, config.getBufSize());
		writeAttribute(ConfigConsts.LOCAL_HOST, config.getLocalHost());
		writeAttribute(ConfigConsts.LOCAL_PORT, config.getLocalPort());
		writeAttribute(ConfigConsts.PARALLEL_NUM, config.getParallelNum());
		writeAttribute(ConfigConsts.CURSOR_PARALLEL_NUM,
				config.getCursorParallelNum());
		writeAttribute(ConfigConsts.BLOCK_SIZE, config.getBlockSize());
		writeAttribute(ConfigConsts.NULL_STRINGS, config.getNullStrings());
		writeAttribute(ConfigConsts.FETCH_COUNT, config.getFetchCount());
		writeAttribute(ConfigConsts.EXTLIBS, config.getExtLibsPath());
		writeImportLibList(config.getImportLibs());
		level = 2;
		endElement(ConfigConsts.ESPROC);
	}

	/**
	 * Write the importLibs node
	 * 
	 * @param importLibs
	 * @throws SAXException
	 */
	protected void writeImportLibList(List<String> importLibs)
			throws SAXException {
		if (importLibs == null || importLibs.isEmpty())
			return;
		level = 3;
		startElement(ConfigConsts.IMPORT_LIBS, null);
		level = 4;
		for (int i = 0, size = importLibs.size(); i < size; i++) {
			writeAttribute(ConfigConsts.LIB, importLibs.get(i));
		}
		level = 3;
		endElement(ConfigConsts.IMPORT_LIBS);
	}

	/**
	 * Write the logger node
	 * 
	 * @param config
	 * @throws SAXException
	 */
	protected void writeLogger(RaqsoftConfig config) throws SAXException {
		level = 2;
		startElement(ConfigConsts.LOGGER, null);
		level = 3;
		writeAttribute(ConfigConsts.LEVEL, config.getLogLevel());
		level = 2;
		endElement(ConfigConsts.LOGGER);
	}

	/**
	 * Write out the Server node
	 * 
	 * @param config
	 * @throws SAXException
	 */
	protected void writeServer(RaqsoftConfig config) throws SAXException {
		String defDataSource = config.getDefDataSource();
		if (!StringUtils.isValidString(defDataSource)) {
			if (config.getJNDIList() == null || config.getJNDIList().isEmpty()) {
				if (config.getServerProperties() == null
						|| config.getServerProperties().isEmpty())
					return;
			}
		}
		level = 1;
		startElement(ConfigConsts.SERVER, null);
		level = 2;
		if (StringUtils.isValidString(defDataSource)) {
			writeAttribute(ConfigConsts.DEF_DATA_SOURCE,
					config.getDefDataSource());
		}
		writeJNDIList(config.getJNDIList());
		writeServerProperties(config.getServerProperties());
		level = 1;
		endElement(ConfigConsts.SERVER);
	}

	/**
	 * Write out the JNDI list node
	 * 
	 * @param jndiList
	 * @throws SAXException
	 */
	private void writeJNDIList(List<JNDIConfig> jndiList) throws SAXException {
		if (jndiList == null || jndiList.isEmpty())
			return;
		level = 2;
		startElement(ConfigConsts.JNDI_LIST, null);
		for (JNDIConfig config : jndiList)
			writeJNDIConfig(config);
		level = 2;
		endElement(ConfigConsts.JNDI_LIST);
	}

	/**
	 * Write out the JNDI node
	 * 
	 * @param config
	 * @throws SAXException
	 */
	private void writeJNDIConfig(JNDIConfig config) throws SAXException {
		if (config == null)
			return;
		level = 3;
		startElement(ConfigConsts.JNDI, getAttributesImpl(new String[] {
				ConfigConsts.NAME, config.getName() }));
		writeNameValueElement(ConfigConsts.DB_TYPE, config.getDBType() + "");
		writeNameValueElement(ConfigConsts.BATCH_SIZE, config.getBatchSize()
				+ "");
		writeNameValueElement(ConfigConsts.LOOKUP, config.getJNDI());
		if (StringUtils.isValidString(config.getDBCharset()))
			writeNameValueElement(ConfigConsts.DB_CHARSET,
					config.getDBCharset());
		endElement(ConfigConsts.JNDI);
	}

	/**
	 * Write out the Server properties
	 * 
	 * @param props
	 * @throws SAXException
	 */
	private void writeServerProperties(Properties props) throws SAXException {
		if (props == null || props.isEmpty())
			return;
		level = 2;
		Iterator it = props.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Object value = props.getProperty(key);
			writeNameValueElement(key, value == null ? "" : value.toString());
		}
	}

	/**
	 * Write out the property node containing the name and value
	 * 
	 * @param name
	 * @param value
	 * @throws SAXException
	 */
	protected void writeNameValueElement(String name, String value)
			throws SAXException {
		writeNameValueElement(ConfigConsts.PROPERTY, name, value);
	}

	/**
	 * Write out the extended property node containing the name and value
	 * 
	 * @param name
	 * @param value
	 * @throws SAXException
	 */
	protected void writeExtendedDBElement(String name, String value)
			throws SAXException {
		writeNameValueElement(ConfigConsts.EXTENDED, name, value);
	}

	/**
	 * Write out the element containing the name and value
	 * 
	 * @param elementName
	 *            the element name
	 * @param name
	 * @param value
	 * @throws SAXException
	 */
	protected void writeNameValueElement(String elementName, String name,
			String value) throws SAXException {
		startElement(elementName, getAttributesImpl(new String[] {
				ConfigConsts.NAME, name, ConfigConsts.VALUE, value }));
		endEmptyElement(elementName);
	}

	/**
	 * Write out the Init node
	 * 
	 * @param config
	 * @throws SAXException
	 */
	protected void writeInit(RaqsoftConfig config) throws SAXException {
		if (!StringUtils.isValidString(config.getInitDfx())) {
			return;
		}
		level = 1;
		startElement(ConfigConsts.INIT, null);
		level = 2;
		writeAttribute(ConfigConsts.DFX, config.getInitDfx());
		level = 1;
		endElement(ConfigConsts.INIT);
	}

	/**
	 * Write out the JDBC node
	 * 
	 * @param config
	 * @throws SAXException
	 */
	protected void writeJDBC(RaqsoftConfig config) throws SAXException {
		boolean hasLoad = !new RaqsoftConfig().getJdbcLoad().equals(
				config.getJdbcLoad());
		boolean hasGateway = StringUtils.isValidString(config.getGateway());
		boolean hasUnit = config.getUnitList() != null
				&& !config.getUnitList().isEmpty();
		if (!config.isJdbcNode())
			if (!hasGateway && !hasUnit) {
				return;
			}
		level = 1;
		startElement(ConfigConsts.JDBC, null);

		level = 2;
		if (hasLoad) {
			writeAttribute(ConfigConsts.LOAD, config.getJdbcLoad());
		}
		if (hasGateway) {
			writeAttribute(ConfigConsts.GATEWAY, config.getGateway());
		}
		writeUnitList(config.getUnitList());
		level = 1;
		endElement(ConfigConsts.JDBC);
	}

	/**
	 * Write out the unit list
	 * 
	 * @param unitList
	 * @throws SAXException
	 */
	private void writeUnitList(List<String> unitList) throws SAXException {
		if (unitList == null || unitList.isEmpty())
			return;
		level = 2;
		startElement(ConfigConsts.UNITS, null);
		level = 3;
		for (String unit : unitList)
			if (StringUtils.isValidString(unit))
				writeAttribute(ConfigConsts.UNIT, unit);
		level = 2;
		endElement(ConfigConsts.UNITS);
	}

	/**
	 * Get the AttributesImpl object
	 * 
	 * @param attrs
	 * @return
	 */
	protected AttributesImpl getAttributesImpl(String[] attrs) {
		AttributesImpl attrImpl = new AttributesImpl();
		int size = attrs.length;
		for (int i = 0; i < size; i += 2) {
			if (attrs[i + 1] != null)
				attrImpl.addAttribute("", "", attrs[i], String.class.getName(),
						String.valueOf(attrs[i + 1]));
		}
		return attrImpl;
	}

	/**
	 * Write unit configuration
	 * 
	 * @param filePath
	 * @param config
	 * @throws SAXException
	 * @throws IOException
	 */
	public void writeUnitConfig(String filePath, UnitConfig config)
			throws SAXException, IOException {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		try {
			fos = new FileOutputStream(filePath);
			bos = new BufferedOutputStream(fos);
			writeUnitConfig(bos, config);
			bos.flush();
		} finally {
			if (fos != null)
				fos.close();
			if (bos != null)
				bos.close();
		}
	}

	/**
	 * Write unit configuration
	 * 
	 * @param out
	 * @param config
	 * @throws SAXException
	 */
	public void writeUnitConfig(OutputStream out, UnitConfig config)
			throws SAXException {
		Result resultxml = new StreamResult(out);
		handler.setResult(resultxml);
		level = 0;
		handler.startDocument();
		handler.startElement("", "", "SERVER", getAttributesImpl(new String[] {
				ConfigConsts.VERSION, "1" }));
		level = 1;
		writeAttribute("TempTimeOut", config.getTempTimeOut() + "");
		writeAttribute("Interval", config.getInterval() + "");
		writeAttribute("ProxyTimeOut", config.getProxyTimeOut() + "");
		handler.endElement("", "", "SERVER");
		handler.endDocument();
	}
}