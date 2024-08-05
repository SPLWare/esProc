package com.scudata.util;

import java.util.ArrayList;

import org.xml.sax.Attributes;

import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;

/**
 * XML格式串的节点
 * @author RunQian
 *
 */
class XMLNode {
	private String name; // 名称
	private XMLNode parent; // 父节点
	private ArrayList<XMLNode> subList; // 子节点列表
	private String characters; // 节点值字符串
	//private Object value;
	
	private String []attrNames;
	private String []attrValues;
	
	public XMLNode(String name, XMLNode parent) {
		this.name = name;
		this.parent = parent;
	}
	
	public String getName() {
		return name;
	}
	
	public XMLNode getParent() {
		return parent;
	}
	
	/*public void setValue(Object val) {
		this.value = val;
	}*/
	
	public void addCharacters(String str) {
		if (characters == null) {
			characters = str;
		} else {
			characters += str;
		}
	}
	
	public void addSub(XMLNode sub) {
		if (subList == null) {
			subList = new ArrayList<XMLNode>();
		}
		
		subList.add(sub);
	}
	
	public boolean euqalName(String name) {
		return this.name.equals(name);
	}
	
	public Object getResult() {
		ArrayList<XMLNode> subList = this.subList;
		Object value;
		if (subList == null) {
			value = XMLUtil.parseText(characters);
		} else {
			ArrayList<String> nameList = new ArrayList<String>();
			int size = subList.size();
			for (XMLNode sub : subList) {
				if (!nameList.contains(sub.name)) {
					nameList.add(sub.name);
				}
			}
			
			int count = nameList.size();
			String []names = new String[count];
			nameList.toArray(names);
			DataStruct ds = new DataStruct(names);
			Record r = new Record(ds);
			value = r;
			
			if (count == size) {
				for (int i = 0; i < size; ++i) {
					XMLNode sub = subList.get(i);
					r.setNormalFieldValue(i, sub.getResult());
				}
			} else if (count == 1) {
				boolean hasAttributes = true;
				Sequence seq = new Sequence(size);
				for (int i = 0; i < size; ++i) {
					XMLNode sub = subList.get(i);
					seq.add(sub.getResult());
					if (!sub.hasAttributes()) {
						hasAttributes = false;
					}
				}
				
				if (hasAttributes) {
					value = seq;
				} else {
					r.setNormalFieldValue(0, seq);
				}
			} else {
				for (int i = 0; i < count; ++i) {
					String name = names[i];
					Sequence seq = new Sequence();
					for (XMLNode sub : subList) {
						if (sub.euqalName(name)) {
							seq.add(sub.getResult());
						}
					}
					
					if (seq.length() > 1) {
						r.setNormalFieldValue(i, seq);
					} else {
						r.setNormalFieldValue(i, seq.get(1));
					}
				}
			}
		}
		
		if (attrNames == null) {
			return value;
		} else {
			int attrCount = attrNames.length;
			String []names = new String[1 + attrCount];
			names[0] = name;
			System.arraycopy(attrNames, 0, names, 1, attrCount);
			DataStruct ds = new DataStruct(names);
			Record r = new Record(ds);
			r.setNormalFieldValue(0, value);
			r.setStart(1, attrValues);
			return r;
		}
	}
	
	public void setAttributes(Attributes attributes) {
		int len = attributes.getLength();
		if (len > 0) {
			attrNames = new String[len];
			attrValues = new String[len];
			for (int i = 0; i < len; ++i) {
				attrNames[i] = attributes.getQName(i);
				attrValues[i] = attributes.getValue(i);
			}
		}
	}
	
	private boolean hasAttributes() {
		return attrNames != null;
	}
}
