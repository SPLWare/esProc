package com.raqsoft.lib.elastic.function;

import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.client.Response;
import com.raqsoft.common.RQException;

/*
 * ImHeader(index, type, doc)
 * 
 */
public class ImHead extends ImFunction {	 
	public Object doQuery(Object[] objs) {
		try {
			if (objs.length<1){
				throw new RQException("params should be endpoint");
			}
			
			String method = "HEAD";
			String endpoint = objs[0].toString();
			Response response = doRun( method, endpoint, objs);
			if(response!=null){
				List<Object[]> ls = new ArrayList<Object[]>();
				parseCatJson(response.toString(), ls);
			
				return toTable(ls);
			}		
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected void parseCatJson(String v, List<Object[]> ls){
		m_colMap.clear();
		m_colMap.put("key", 0);
		m_colMap.put("value", 1);
		
		v = v.substring(v.indexOf("{")+1, v.indexOf("}"));
		String[] line = v.split(",");

		for(String s: line){
			String[] kv = s.split("=");
			kv[0] = kv[0].trim();
			ls.add(kv);
		}
	}
	
}