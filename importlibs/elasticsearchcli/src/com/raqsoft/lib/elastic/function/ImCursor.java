package com.raqsoft.lib.elastic.function;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.lib.elastic.helper.ImUtils;
import com.raqsoft.lib.elastic.helper.RestConn;
import com.raqsoft.util.JSONUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ImCursor extends ICursor {
	private RestConn m_restConn = null;
	private List<Object[]> m_buffer;
	private String m_endpoint;
	private HttpEntity m_entity;
	private String m_method = "POST";
	private String m_sOldIndex="";
	private int m_nTotal;  //记录总数
	private int m_current; //当前位置
	protected Map<String, Integer> m_colMap;
	
	public ImCursor(Context c, RestConn conn, Object[] objs) {
		ctx = c;
		this.m_restConn = conn;
		ctx.addResource(this);	
		m_colMap = new LinkedHashMap<String, Integer>(); 
		init(objs);
	}
	
	private void init(Object[] objs) {
		try {
			m_endpoint=objs[0].toString()+"?scroll=1m";		
			String entity = "";
			if (objs.length==2){
				entity = objs[1].toString();
			}else{
				entity = "{ \"size\": 500," + " \"query\": { \"match_all\": {}}}";
			}
			m_method = "GET";			
			m_buffer = new ArrayList<Object[]>();
			m_entity = new NStringEntity(entity, ContentType.APPLICATION_JSON);
			Request request = new Request(m_method,  m_endpoint);   
			request.setEntity(m_entity);
			
			Response response = m_restConn.m_restClient.performRequest(request);
			String v = EntityUtils.toString(response.getEntity());
			Map<String, Object> map = new HashMap<String, Object>();
			parseHeaderInfo(v, map);
			m_nTotal = Integer.parseInt(map.get("total").toString());
			String scrollId = map.get("scroll_id").toString();

			if (objs.length==2){
				; //skip;
			}else if (m_restConn.m_version.equals("1.5")) {
				m_endpoint = "/_search/scroll?scroll=1m";
				m_entity = new NStringEntity(scrollId, ContentType.APPLICATION_JSON);
			} else {
				m_endpoint = "/_search/scroll";
				m_entity = new NStringEntity(
						"{\n" + " \"scroll\" : \"1m\"," + 
						"  \"scroll_id\": \"" + scrollId + "\"" + "}",
						ContentType.APPLICATION_JSON);
			}
		    m_current = Integer.parseInt( map.get("ret").toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected Sequence get(int n) {
		List<Object[]> ls = getData(n);
		if (ls.size()==0)
			return null;
		else{
			return toTable(ls);
		}
	}
	
	private List<Object[]> getData(int n) {
		List<Object[]> ls = new ArrayList<Object[]>();
		// 1。缓冲>n时
		if (n < m_buffer.size()) {
			Iterator<Object[]> iter = m_buffer.iterator();
			while (iter.hasNext() && n > 0) {
				ls.add(iter.next());
				iter.remove();
				n--;
			}
		} else if (n == m_buffer.size()) { // 2。缓冲==n时
			ls.addAll(m_buffer);
			m_buffer.clear();
		} else if (n > m_buffer.size()) { // 3。缓冲<n时
			if(m_current >= m_nTotal) {
				if (m_buffer.size()>0){
					ls.addAll(m_buffer);
					m_buffer.clear();;
				}
				return ls;
			}
			Iterator<Object[]> iter = m_buffer.iterator();
			while (iter.hasNext()) {
				ls.add(iter.next());
			}
			n = n - m_buffer.size();
			m_buffer.clear();

			try {
				Response response = null;
				String v = "";
				int nTotal = 0;
				Request request = new Request(m_method,  m_endpoint);   
				request.setEntity(m_entity);
				while (m_current < m_nTotal) {
					response = m_restConn.m_restClient.performRequest(request);
					v = EntityUtils.toString(response.getEntity());
					int len = searchData(v);
					m_current += n;
					nTotal += len;
					if (nTotal > n) {
						break;
					}
				}
				// 数据补充到ls中.
				iter = m_buffer.iterator();
				while (iter.hasNext() && n > 0) {
					ls.add(iter.next());
					iter.remove();
					n--;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return ls;
	}

	@Override
	protected long skipOver(long arg0) {
		long n = skipData(arg0);
		
		return n;
	}
	
	private long skipData(long n) {
		long org = n;
		if (m_buffer == null || n == 0) return 0;
		// 1。缓冲>n时
		if (n < m_buffer.size()) {
			Iterator<Object[]> iter = m_buffer.iterator();
			while (iter.hasNext() && n > 0) {
				iter.next();
				iter.remove();
				n--;
			}
		} else if (n == m_buffer.size()) { // 2。缓冲==n时
			n = 0;
			m_buffer.clear();
		} else if (n > m_buffer.size()) { // 3。缓冲<n时
			if(m_current >= m_nTotal) return 0;
			Iterator<Object[]> iter = null;
			n = n - m_buffer.size();
			m_buffer.clear();

			try {
				Response response = null;
				String v = "";
				int nTotal = 0;
				Request request = new Request(m_method,  m_endpoint);   
				request.setEntity(m_entity);
				while (m_current < m_nTotal) {
					response = m_restConn.m_restClient.performRequest(request);
					v = EntityUtils.toString(response.getEntity());
					int len = searchData(v);
					m_current += n;
					nTotal += len;
					if (nTotal > n) {
						break;
					}
				}
				// 数据补充到ls中.
				iter = m_buffer.iterator();
				while (iter.hasNext() && n > 0) {
					iter.next();
					iter.remove();
					n--;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return org - n;
	}
	
	//解析header信息
	private void parseHeaderInfo(String s, Map<String, Object> map) {
		try {
			JSONObject result = new JSONObject(s); // Convert String to JSON Object
			String v = result.getString("_scroll_id");
			map.put("scroll_id", v);
			
			if (result.has("aggregations")){
				parseHeaderAggs(result.getJSONObject("aggregations"), map);
			}else{
				parseHeaderHits(result.getJSONObject("hits"), map);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void parseHeaderHits(JSONObject result, Map<String, Object> map) {
		try {
			JSONArray tokenList = result.getJSONArray("hits");
			boolean b = result.has("total");
			if (result.has("total")){
				JSONObject o = (JSONObject)result.getJSONObject("total");
				Object nTotal = o.get("value"); 
				map.put("total", nTotal);
			}
			
			int nRet = tokenList.length();
			map.put("ret", nRet);
		    for(int i=0;i<tokenList.length();i++){
		      JSONObject jobs = tokenList.getJSONObject(i);  // 遍历 jsonarray 数组，把每一个对象转成 json 对象
		   
		      doParseLine(jobs, m_buffer, i);
		    }

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void parseHeaderAggs(JSONObject ret, Map<String, Object> map) {
		try {
			Iterator itr = ret.keys();
			int n = 0;
			while(itr.hasNext()){
				Object o = itr.next();
				JSONObject v = ret.getJSONObject(o.toString());
				JSONArray tokenList = v.getJSONArray("buckets");
				int nRet = tokenList.length();
				map.put("total", nRet);
				map.put("ret", nRet);
			    for(int i=0;i<tokenList.length();i++){
			      JSONObject jobs = tokenList.getJSONObject(i);  // 遍历 jsonarray 数组，把每一个对象转成 json 对象
			      doParseLine(jobs, m_buffer, n);
			      n++;
			    }
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private int searchData(String retSrc) {
		
		int nRet = 0;
		try {
			if (retSrc != null) {
				JSONObject result = new JSONObject(retSrc); // Convert String to JSON Object
				if (result.has("")){
					return searchAggs(result.getJSONObject("aggregations"));
				}else{
					return searchHits(result.getJSONObject("hits"));
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return nRet;
	}
	
	private int searchHits(JSONObject ret) {
		int nRet = 0;
		try {
			JSONArray tokenList = ret.getJSONArray("hits");
			nRet = tokenList.length();				
			for(int i=0;i<tokenList.length();i++){
			    JSONObject jobs = tokenList.getJSONObject(i);  // 遍历 jsonarray 数组
			    doParseLine(jobs, m_buffer, 1);
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return nRet;
	}
	
	private int searchAggs(JSONObject ret) {
		int nRet = 0;
		try {
			JSONArray tokenList = ret.getJSONArray("buckets");
			nRet = tokenList.length();				
			for(int i=0;i<tokenList.length();i++){
			    JSONObject jobs = tokenList.getJSONObject(i);  // 遍历 jsonarray 数组
			    doParseLine(jobs, m_buffer, 1);
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return nRet;
	}
	
	protected boolean doParseLine(JSONObject jobs, List<Object[]> ls, int i){
		boolean bRet = false;
		do{
			if( jobs.has("_index")){
				if (!m_sOldIndex.isEmpty() && !m_sOldIndex.equals(jobs.getString("_index"))){
					ls.clear();
					break;
				}
				m_sOldIndex = jobs.getString("_index");
			}
			if (jobs.has("_source")){
				JSONObject job = jobs.getJSONObject("_source");		
				if (!job.isEmpty()){
					doParseJoson(job, ls, i==0);
				}
			}else if(jobs.length()>0){
				doParseJoson(jobs, ls, i==0);
			}else{
				ls.clear();
				break;
			}
			bRet = true;
		}while(false);
		
		return bRet;
	}
	
	protected void doParseJoson(JSONObject job, List<Object[]> ls, Boolean bTitle){
		Iterator iterator = job.keys();
		Object[] objs = null;
		// 每个对象中的属性值
		int n = 0;
		if (bTitle) {
			objs = new Object[job.length()];
			m_colMap.clear();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				String value = job.getString(key);
				m_colMap.put(key, n);
				if (ImUtils.isJson(value)){
					objs[n++] = JSONUtil.parseJSON(value.toCharArray(), 0, value.length()-1);
				}else{
					objs[n++] = value;
				}
			}
		}else{
			int nSize = job.length();
			if(m_colMap.size()>job.length()){
				nSize = m_colMap.size();
			}
			objs = new Object[nSize];
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				String value = job.getString(key);	
				
				if ( !m_colMap.containsKey(key)){
					n = m_colMap.size();
					m_colMap.put(key, n);
				}else{
					n =m_colMap.get(key);
				}
				if (ImUtils.isJson(value)){
					objs[n] = JSONUtil.parseJSON(value.toCharArray(), 0, value.length()-1);
				}else{
					objs[n] = value;
				}
			}
		}
		
		ls.add(objs);
	}
	
	protected Table toTable(List<Object[]> rows) {
		String[] colNames=new String[m_colMap.size()];
		m_colMap.keySet().toArray(colNames);
		Table table = new Table(colNames);

		for (Object[] row : rows) {
			table.newLast(row);
		}

		return table;
	}
}
