package com.scudata.expression.mfn.string;

import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

class StrProperties extends Properties{
	private static final long serialVersionUID = 1L;
	String connector = "=";
	String seperator = " ";
	
	public StrProperties() {
	}
	
	public void setConnector(String c) {
		connector = c;
	}
	
	public void setSeperator(String s) {
		seperator = s;
	}
	
    public synchronized void load(String str) {
    	if (seperator!=" ") {
    		if(str.indexOf(",")>0) {
    			seperator=",";
    		} else {
    			seperator=";";
    		}
    	}
    	
    	StringTokenizer st = new StringTokenizer(str, seperator );
    	while (st.hasMoreTokens()) {
    		String tmp = st.nextToken().trim();
    		StringTokenizer seg = new StringTokenizer( tmp, connector );
    		String key = seg.nextToken().trim();
    		String val = "";
    		if(seg.hasMoreElements() ) val = seg.nextToken().trim();
    		put(key, val);
    	}
    }
	
    public String getString() {
    	StringBuffer sb = new StringBuffer();
    	Set<String> keys = stringPropertyNames();
    	Iterator<String> it = keys.iterator();
    	while (it.hasNext()) {
    		if (sb.length()>0) {
    			sb.append(seperator);
    		}
    		
    		String key = it.next();
    		String val = getProperty(key);
    		sb.append(key);
    		sb.append(connector);
    		sb.append(val);
    	}
    	
    	return sb.toString();
    }
}

