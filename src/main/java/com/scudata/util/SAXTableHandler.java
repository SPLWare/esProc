package com.scudata.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.scudata.array.IArray;
import com.scudata.common.RQException;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;

/**
 * 用于解析XML数据
 * @author RunQian
 *
 */
class SAXTableHandler extends DefaultHandler {
	private XMLNode root; // 根节点
	private XMLNode node;
	private boolean sopt; // 是否有s选项
	private boolean bopt; // 是否有b选项
	
	public SAXTableHandler(String opt) {
		if (opt != null) {
			sopt = opt.indexOf('s') != -1;
			bopt = opt.indexOf('b') != -1;
		}
	}

	/**
	 * 取XML的解析结果
	 * @param levels 结果集的路径，省略从根开始
	 * @return Object
	 */
	public Object getResult(String []levels) {
		if (root == null) {
			return null;
		}
		
		DataStruct ds = new DataStruct(new String[]{root.getName()});
		Record r = new Record(ds);
		r.setNormalFieldValue(0, root.getResult());
		
		if (levels == null) {
			return r;
		}
		
		Object obj = r;
		for (String level : levels) {
			if (obj instanceof Record) {
				Record record = (Record)obj;
				int f = record.getFieldIndex(level);
				if (f != -1) {
					obj = record.getNormalFieldValue(f);
				} else {
					return null;
				}
			} else if (obj instanceof Sequence) {
				obj = fieldValues((Sequence)obj, level);
			} else {
				return null;
			}
		}

		return obj;
	}

	private Sequence fieldValues(Sequence seq, String fieldName) {
		IArray mems = seq.getMems();
		int size = mems.size();
		Sequence result = new Sequence(size);

		int col = -1; // 字段在上一条记录的索引
		Record prevRecord = null; // 上一条记录

		for (int i = 1; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof Record) {
				Record cur = (Record)obj;
				if (prevRecord == null || !prevRecord.isSameDataStruct(cur)) {
					col = cur.getFieldIndex(fieldName);
					if (col < 0) {
						continue;
						//MessageManager mm = EngineMessage.get();
						//throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
					}
					
					prevRecord = cur;
				}
				
				result.add(cur.getFieldValue(col));
			} else if (obj instanceof Sequence) {
				Sequence tmp = fieldValues((Sequence)obj, fieldName);
				result.addAll(tmp);
			}
		}

		return result;
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		for (int i = 0; i < length; ++i) {
			if (!Character.isWhitespace(ch[start + i])) {
				if (node == null) {
					throw new RQException();
				}
				
				String str = new String(ch, start, length);
				node.addCharacters(str);
				break;
			}
		}
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		XMLNode newNode = new XMLNode(qName, node, bopt);
		if (sopt) {
			newNode.setAttributes(attributes);
		}
		
		if (node == null) {
			if (root != null) {
				throw new RQException(qName);
			}
			
			node = newNode;
			root = node;
		} else {
			node.addSub(newNode);
			node = newNode;
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (node == null || !node.euqalName(qName)) {
			throw new RQException(qName);
		}
		
		node = node.getParent();
	}
}
