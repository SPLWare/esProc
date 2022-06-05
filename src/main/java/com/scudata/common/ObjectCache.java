package com.scudata.common;

final public class ObjectCache {
	private static final int MAX_INTEGER = 65535;
	private static final Integer []integers = new Integer[MAX_INTEGER + 1];
	private static final String []strings = new String[128];
	
	static {
		for (int i = 0, len = integers.length; i < len; ++i) {
			integers[i] = new Integer(i);
		}
		
		char buf[] = new char[1];
		for (int i = 0, len = strings.length; i < len; ++i) {
			buf[0] = (char) i;
			strings[i] = new String(buf);
		}
	}
	
	public static Integer getInteger(int i) {
		if (i >= 0 && i <= MAX_INTEGER) {
			return integers[i];
		} else {
			return new Integer(i);
		}
	}
	
	public static String getString(char[] buf) {
		if (buf[0] > 127) {
			return new String(buf, 0, 1);
		}
		return strings[buf[0]];
	}
	
	public static String getString(byte b) {
		return strings[b];
	}
}