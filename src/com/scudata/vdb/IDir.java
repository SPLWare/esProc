package com.scudata.vdb;

import com.scudata.util.Variant;

/**
 * 目录基类
 * @author RunQian
 *
 */
abstract class IDir {
	protected Object value; // 目录值
	protected String name; // 目录名，即字段名
		
	public IDir() {
	}
		
	public String getName() {
		return name;
	}
	
	public Object getValue() {
		return value;
	}
	
	abstract public ISection getParent();

	public boolean isEqualValue(Object val) {
		return Variant.isEquals(value, val);
	}
	
	public boolean isEqualName(String str) {
		if (str == null) {
			return name == null;
		} else {
			return str.equals(name);
		}
	}
	
	abstract public void releaseSubSection();
}
