package com.scudata.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;

/**
 * 用于把排列转成XML格式串或者把XML格式串读成排列
 * @author RunQian
 *
 */
final public class XMLUtil {
	private static final String ID_Table = "xml"; // 表默认的标签名
	private static final String ID_Row = "row"; // 记录默认的标签名

	private static AttributesImpl attr = new AttributesImpl();

	// 字符串转换，如果obj本身是字符串则加上双引号
	private static String toTextNodeString(Object obj) {
		if (obj == null) {
			return ""; //"null";
		} else if (obj instanceof String) {
			return Escape.addEscAndQuote((String)obj);
		} else if (obj instanceof Sequence) {
			ListBase1 mems = ((Sequence)obj).getMems();
			StringBuffer sb = new StringBuffer(1024);
			sb.append('[');
			
			for (int i = 1, len = mems.size(); i <= len; ++i) {
				if (i > 1) sb.append(',');
				sb.append(toTextNodeString(mems.get(i)));
			}

			sb.append(']');
			
			return sb.toString();
		} else {
			return Variant.toString(obj);
		}
	}

	/**
	 * 把XML格式串读成多层记录或序表
	 * <>内的标识作为字段名，重复的同名标识生成为序表
	 * 将形如<K F=v F=v …>D</K>的XML串解析为以K,F,…为字段的记录，
	 * K取值为D，D是多层XML内容时解析为排列，<K …./K>时D解析为null，<K…></K>时D解析为空串
	 * @param src XML串
	 * @param levels 层标识，多层用/分隔 
	 * @return
	 */
	public static Object parseXml(String src, String levels) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser saxParser = spf.newSAXParser();
			SAXTableHandler handler = new SAXTableHandler();
			
			XMLReader xmlReader = saxParser.getXMLReader();
			xmlReader.setContentHandler(handler);
			StringReader reader = new StringReader(src);
			xmlReader.parse(new InputSource(reader));
			
			Object table = handler.getResult(parseLevels(levels));
			return table;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 把排列变成XML格式串
	 * @param sequence 排列
	 * @param charset 字符集
	 * @param levels 层标识，格式为"TableName/RecordName"，多层用/分隔，如果省略则用"xml/row"
	 * @return
	 */
	public static String toXml(Sequence sequence, String charset, String levels) {
		if (charset == null || charset.length() == 0) {
			charset = Env.getDefaultCharsetName();
		}

		DataStruct ds = sequence.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}

		try {
			SAXTransformerFactory fac = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
			TransformerHandler handler = fac.newTransformerHandler();
			Transformer transformer = handler.getTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, charset);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			
			StringWriter writer = new StringWriter(8192);
			Result resultxml = new StreamResult(writer);
			handler.setResult(resultxml);
			handler.startDocument();
			
			String []strs = parseLevels(levels);
			String idTable = ID_Row;
			int count = strs == null ? 0 : strs.length;
			
			if (count > 1) {
				idTable = strs[count - 1];
				for (int i = 0; i < count - 1; ++i) {
					handler.startElement("", "", strs[i], attr);
				}
			} else {
				if (count == 1) {
					handler.startElement("", "", strs[0], attr);
				} else {
					handler.startElement("", "", ID_Table, attr);
				}
			}
			
			toXml(handler, sequence, 0, idTable);
			
			if (count > 1) {
				for (int i = 0; i < count - 1; ++i) {
					handler.endElement("", "", strs[i]);
				}
			} else {
				if (count == 1) {
					handler.endElement("", "", strs[0]);
				} else {
					handler.endElement("", "", ID_Table);
				}
			}
			
			handler.endDocument();
			return writer.toString();
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	/**
	 * 把记录变成XML格式串
	 * @param r 记录
	 * @param charset 字符集
	 * @param levels 层标识，多层用/分隔
	 * @return
	 */
	public static String toXml(Record r, String charset, String levels) {
		if (charset == null || charset.length() == 0) {
			charset = Env.getDefaultCharsetName();
		}
		
		try {
			SAXTransformerFactory fac = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
			TransformerHandler handler = fac.newTransformerHandler();
			Transformer transformer = handler.getTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, charset);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			
			StringWriter writer = new StringWriter(8192);
			Result resultxml = new StreamResult(writer);
			handler.setResult(resultxml);
			handler.startDocument();
			
			String []strs = parseLevels(levels);
			int count = strs == null ? 0 : strs.length;
			for (int i = 0; i < count; ++i) {
				handler.startElement("", "", strs[i], attr);
			}
			
			toXml(handler, r, count);
			
			for (int i = 0; i < count; ++i) {
				handler.endElement("", "", strs[i]);
			}

			handler.endDocument();
			return writer.toString();
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	private static void appendTab(TransformerHandler handler, int level) throws SAXException {
		// 不需要对齐？
		/*if (false) {
			StringBuffer sb = new StringBuffer(ENTER);
			for (int i = 0; i < level; i++) {
				sb.append(TAB);
			}
			
			String indent = sb.toString();
			handler.characters(indent.toCharArray(), 0, indent.length());
		}*/
	}
	
	private static void toXml(TransformerHandler handler, Record r, int level) throws SAXException {
		Object []vals = r.getFieldValues();
		String []names = r.getFieldNames();
		for (int f = 0, fcount = vals.length; f < fcount; ++f) {
			appendTab(handler, level);

			Object val = vals[f];
			if (val instanceof Record) {
				handler.startElement("", "", names[f], attr);
				toXml(handler, (Record)val, level + 1);
				handler.endElement("", "", names[f]);
			} else if (val instanceof Sequence && ((Sequence)val).isPurePmt()) {
				toXml(handler, (Sequence)val, level + 1, names[f]);
			} else {
				handler.startElement("", "", names[f], attr);
				String valStr = toTextNodeString(val);
				handler.characters(valStr.toCharArray(), 0, valStr.length());
				handler.endElement("", "", names[f]);
			}
		}
	}
	
	private static void toXml(TransformerHandler handler, Sequence table, int level, String idTable) throws SAXException {
		if (level > 0) appendTab(handler, level);
		
		ListBase1 mems = table.getMems();
		for (int i = 1, len = mems.size(); i <= len; ++i) {
			handler.startElement("", "", idTable, attr);
			Record r = (Record)mems.get(i);
			toXml(handler, r, level + 1);
			handler.endElement("", "", idTable);
		}
	}
	
	private static String[] parseLevels(String levels) {
		if (levels == null || levels.length() == 0) {
			return null;
		}
	
		ArrayList<String> list = new ArrayList<String>();
		int s = 0;
		int len = levels.length();
		while (s < len) {
			int i = levels.indexOf('/', s);
			if (i < 0) {
				list.add(levels.substring(s));
				break;
			} else {
				list.add(levels.substring(s, i));
				s = i + 1;
			}
		}
		
		String []strs = new String[list.size()];
		list.toArray(strs);
		return strs;
	}
	
	/**
	 * 把XML格式串读成多层记录或序表
	 * <>内的标识作为字段名，重复的同名标识生成为序表
	 * 将形如<K F=v F=v …>D</K>的XML串解析为以K,F,…为字段的记录，
	 * K取值为D，D是多层XML内容时解析为排列，<K …./K>时D解析为null，<K…></K>时D解析为空串
	 * @param src XML串
	 * @return
	 */
	public static Object parseXmlString(String src) {
		try {
			SAXBuilder saxReader = new SAXBuilder();
			StringReader reader = new StringReader(src);
			InputSource is = new InputSource(reader);
			Document doc = saxReader.build(is);
			
			List<Element> list = doc.getContent();
			if (list == null || list.size() == 0) {
				return null;
			}
			
			int size = list.size();
			Sequence seq = new Sequence(size);
			for (Element e : list) {
				Object val = parseElement(e);
				seq.add(val);
			}
		
			return seq;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	private static Object parseElement(Element e) {
		List<Object> contents = e.getContent();
		List<Attribute> attrs = e.getAttributes();
		List<Element> childs = e.getChildren();
		
		int contentCount = contents == null ? 0 : contents.size();
		int attrCount = attrs == null ? 0 : attrs.size();
		int childCount = childs == null ? 0 : childs.size();
		
		String text = e.getTextTrim();
		String []names = new String[1 + attrCount];
		Object []vals = new Object[1 + attrCount];
		names[0] = e.getName();
		
		if (attrCount > 0) {
			for (int i = 1; i <= attrCount; ++i) {
				Attribute attr = attrs.get(i - 1);
				names[i] = attr.getName();
				vals[i] = Variant.parse(attr.getValue(), true);
			}
		}

		if (childCount > 0) {
			Sequence seq;
			if (contentCount > 0 && text.length() > 0) {
				seq = new Sequence(childCount + 1);
				Object val = Variant.parse(text, true);
				seq.add(val);
			} else {
				seq = new Sequence(childCount);
			}

			for (int i = 0; i < childCount; ++i) {
				Element sub = childs.get(i);
				Object val = parseElement(sub);
				seq.add(val);
			}
			
			vals[0] = seq;
		} else if (contentCount > 0) {
			vals[0] = Variant.parse(text, true);
		}
		
		DataStruct ds = new DataStruct(names);
		return new Record(ds, vals);
	}
}
