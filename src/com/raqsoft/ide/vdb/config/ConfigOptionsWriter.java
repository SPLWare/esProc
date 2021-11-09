package com.raqsoft.ide.vdb.config;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class ConfigOptionsWriter {
	private TransformerHandler handler = null;
	// private String fileName;
	// private AttributesImpl atts;
	// 元素层次，用于控制XML缩进
	private int level = 0;
	// 每个层次父级缩进4个空格，即一个tab
	private final String tab = "    ";
	// 系统换行符，Windows为："\n"，Linux/Unix为："/n"
	private final String separator = System.getProperties().getProperty("os.name").toUpperCase()
			.indexOf("WINDOWS") != -1 ? "\n" : "/n";

	public ConfigOptionsWriter() {
		try {
			SAXTransformerFactory fac = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
			handler = fac.newTransformerHandler();
			Transformer transformer = handler.getTransformer();
			// 设置输出采用的编码方式
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			// 是否自动添加额外的空白
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			// 是否忽略xml声明
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			// outStream = new FileOutputStream(fileName);

			// level = 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 元素里面会嵌套子节点，因此元素的开始和结束分开写
	// 如：<a><b>bcd</b></a>
	private void startElement(String objectElement, AttributesImpl attrs) throws SAXException {
		if (attrs == null) {
			attrs = new AttributesImpl();
		}
		// level++;
		appendTab();
		if (objectElement != null) {
			// 注意，如果atts.addAttribute设置了属性，则会输出如：<a key="key"
			// value="value">abc</a>格式
			// 如果没有设置属性，则输出如：<a>abc</a>格式
			handler.startElement("", "", objectElement, attrs);
		}
	}

	// 正常元素结束标记，如：</a>
	private void endElement(String objectElement) throws SAXException {
		// level--;
		appendTab();
		if (objectElement != null) {
			handler.endElement("", "", objectElement);
		}
	}

	// 自封闭的空元素，如<a key="key" value="value"/>，不用换行，写在一行时XML自动会自封闭
	private void endEmptyElement(String objectElement) throws SAXException {
		// level--;
		handler.endElement("", "", objectElement);
	}

	// 无子节点的元素成为属性，如<a>abc</a>
	private void writeAttribute(String key, String value) throws SAXException {
		if (value == null)
			value = "";
		appendTab();
		handler.startElement("", "", key, new AttributesImpl());
		handler.characters(value.toCharArray(), 0, value.length());
		handler.endElement("", "", key);
		// level--;
	}

	// Tab缩进，SAX默认不自动缩进，因此需要手动根据元素层次进行缩进控制
	private void appendTab() throws SAXException {
		StringBuffer sb = new StringBuffer(separator);
		for (int i = 0; i < level; i++) {
			sb.append(tab);
		}
		String indent = sb.toString();
		handler.characters(indent.toCharArray(), 0, indent.length());
	}

	/**
	 * 把数据源和环境写到文件
	 * 
	 * @param dslm
	 * @throws SAXException
	 * @throws IOException
	 */
	public void write(String filePath) throws Exception {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		try {
			File f = new File(filePath);
			if(!f.exists()){
				File p = f.getParentFile();
				p.mkdirs();
			}
			fos = new FileOutputStream(filePath);
			bos = new BufferedOutputStream(fos);
			write(bos);
			bos.flush();
		} finally {
			if (fos != null)
				fos.close();
			if (bos != null)
				bos.close();
		}
	}

	public void write(OutputStream out) throws Exception {
		Result resultxml = new StreamResult(out);
		handler.setResult(resultxml);
		level = 0;
		handler.startDocument();
		// 设置根节点和版本
		handler.startElement("", "", ConfigFile.ROOT, getAttributesImpl(new String[] { ConfigFile.VERSION, "1" }));
		// 选项
		level = 1;
		startElement(ConfigFile.OPTIONS, null);
		Map<String, Object> options = ConfigOptions.options;
		Iterator<String> it = options.keySet().iterator();
		String key;
		while (it.hasNext()) {
			key = it.next();
			level = 2;
			startElement(ConfigFile.OPTION, getAttributesImpl(
					new String[] { ConfigFile.NAME, key, ConfigFile.VALUE, object2String(options.get(key)) }));
			endEmptyElement(ConfigFile.OPTION);
		}
		level = 1;
		endElement(ConfigFile.OPTIONS);
		
		// 窗口位置大小
		startElement(ConfigFile.DIMENSIONS, null);
		Map<String, String> dimensions = ConfigOptions.dimensions;
		it = dimensions.keySet().iterator();
		while (it.hasNext()) {
			key = it.next();
			level = 2;
			startElement(ConfigFile.DIMENSION,
					getAttributesImpl(new String[] { ConfigFile.NAME, key, ConfigFile.VALUE, dimensions.get(key) }));
			endEmptyElement(ConfigFile.DIMENSION);
		}
		level = 1;
		endElement(ConfigFile.DIMENSIONS);
		
		
		// 连接信息
		startElement(ConfigFile.CONNECTIONS, null);
		Map<String, String> connections = ConfigOptions.connections;
		it = connections.keySet().iterator();
		while (it.hasNext()) {
			key = it.next();
			level = 2;
			startElement(ConfigFile.CONNECTION,
					getAttributesImpl(new String[] { ConfigFile.NAME, key, ConfigFile.VALUE, connections.get(key) }));
			endEmptyElement(ConfigFile.CONNECTION);
		}
		level = 1;
		endElement(ConfigFile.CONNECTIONS);

		level = 0;
		handler.endElement("", "", ConfigFile.ROOT);
		// 文档结束,同步到磁盘
		handler.endDocument();
	}

	private String object2String(Object val) {
		if (val == null)
			return "";
		return val.toString();
	}

	private AttributesImpl getAttributesImpl(String[] attrs) {
		AttributesImpl attrImpl = new AttributesImpl();
		int size = attrs.length;
		for (int i = 0; i < size; i += 2) {
			if (attrs[i + 1] != null) // 空节点不写了
				attrImpl.addAttribute("", "", attrs[i], String.class.getName(), String.valueOf(attrs[i + 1]));
		}
		return attrImpl;
	}

}