package com.raqsoft.lib.elastic.function;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.elastic.helper.ImUtils;
import com.raqsoft.lib.elastic.helper.RestConn;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.JSONUtil;
import net.sf.json.JSONObject;

public class ImFunction extends Function {
	protected int m_paramSize = 0; // 参数个数
	protected RestConn m_restConn = null;
	protected String m_model;
	protected Context m_ctx;
	protected Map<String, Integer> m_colMap;

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		m_ctx = ctx;
		m_colMap = new LinkedHashMap<String, Integer>(); 
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("olap" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		// System.out.println("baseSize = " + size);
		if (size == 0) {
			Object o = param.getLeafExpression().calculate(ctx);
			if ((o instanceof RestConn)) {
				m_restConn = (RestConn) o;
				return doQuery(null);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ali_close" + mm.getMessage("function.paramTypeError"));
			}
		}

		Object cli = new Object();
		Object objs[] = new Object[size - 1];
		for (int i = 0; i < size; i++) {
			if (param.getSub(i) == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("client" + mm.getMessage("function.invalidParam"));
			}

			if (i == 0) {
				cli = param.getSub(i).getLeafExpression().calculate(ctx);
				if ((cli instanceof RestConn)) {
					m_restConn = (RestConn) cli;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("client" + mm.getMessage("function.paramTypeError"));
				}
			} else {
				objs[i - 1] = param.getSub(i).getLeafExpression().calculate(ctx);
			}
		}

		if (m_restConn == null) {
			throw new RQException("connect is null");
		}
		if (objs.length < 1) {
			throw new RQException("olap_param is empty");
		}else if(option!=null && option.indexOf("f")>-1){//最近一个参数为文件时，转换成对应的文本.
			try {
				if (objs.length>=2){
					String s = objs[1].toString();
					objs[1] = ImUtils.readJsonFile(s);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return doQuery(objs);
	}

	public Object doQuery(Object[] objs) {

		return null;
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
	
	protected int getSubModel(String method, String s){
		int nRet = 0;
		if (s==null) {
			
		}else if(s.equalsIgnoreCase("/_cat") &&	method.equalsIgnoreCase("GET")){ //1. "/_cat"
			nRet = 1; 
		}else if(s.equalsIgnoreCase("health") && method.equalsIgnoreCase("_cat")){ //2. "/_cat/health"
			nRet = 2;
		}else if(s.equalsIgnoreCase("nodes") &&	method.equalsIgnoreCase("_cat")){ //2. "/_cat/nodes"
			nRet = 3;
		}else if(s.equalsIgnoreCase("indices") && method.equalsIgnoreCase("_cat")){ //2. "/_cat/indices"
			nRet = 4;
		}else if(s.equalsIgnoreCase("_source") && method.equalsIgnoreCase("GET")){ //5. "/_GET"
			nRet = 5; 
		}
		return nRet;
	}
	
	protected void parseCatJson(String retStr, List<Object[]> ls){
		m_colMap.clear();
		m_colMap.put("value", 0);
		retStr = retStr.replace("=^.^=\n", "");
		retStr = retStr.replaceAll("/_cat/", "");
		String[] vs = retStr.split("\n");
		
		for(String s : vs){
			Object[] objs= new Object[1];
			objs[0] = s;
			ls.add(objs);
		}
	}
	
	protected List<Object[]> parseInfo(String retStr) {
		List<Object[]> ls = new ArrayList<Object[]>();
		do{			
			String[] lines = retStr.split("\n");
			if (lines==null){
				System.out.println("reponse result is not split \"\\n\"");
				break;
			}
			
			if (lines.length<2){
				parseStringInfo(retStr, ls);
			}else{
				parseKVInfo(retStr, ls);
			}
		}while(false);
		
		return ls;
	}
	
	private void parseStringInfo(String line, List<Object[]> ls ) {
		try {
			m_colMap.clear();
			m_colMap.put("value", 0);
	
			String[] vs = line.split("\\s+");
			for(int n=0; n<vs.length; n++){
				if (vs[n].isEmpty()) continue;
				
				Object[] objs=new Object[1];
				objs[0] = vs[n];
				ls.add(objs);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	// 根据k值找到每个字段的位置范围，解析取字段值再去掉空格
	private void parseKVInfo(String retStr, List<Object[]> ls ) {
		try {
		do{		
			m_colMap.clear();
			m_colMap.put("key", 0);
			m_colMap.put("value", 1);
	
			String[] lines = retStr.split("\n");
			String[] ks = lines[0].split("\\s+");
			String[] vs = lines[1].split("\\s+");
			
			int n=0; 
			if (vs.length!=ks.length){
				vs = new String[ks.length];
				int[] ns = new int[ks.length+1];
				for(n=0; n<ks.length;n++){//记录字段的位置.
					ns[n] = lines[0].indexOf(ks[n]);
				}
				ns[ks.length] = lines[1].length(); //字符串长度
				
				String val = "";
				for(n=0; n<ks.length;n++){
					val = lines[1].substring(ns[n], ns[n+1]);
					vs[n]=val.trim();
					//System.out.println("idx="+nIdx+" len="+nLen + " val="+vs[n]);
				}				
			}
			
			for(n=0; n<ks.length; n++){
				Object[] objs=new Object[2];
				objs[0] = ks[n];
				objs[1] = vs[n];
				ls.add(objs);
			}
		}while(false);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	// index[key,.............]
	//	[val,.............]
	//	[val,.............]
	protected List<Object[]> parseIndex(String retStr) {
		List<Object[]> ls = null;
		try {
		do{		
			int n=0; 
			m_colMap.clear();
			ls = new ArrayList<Object[]>();
			String[] lines = retStr.split("\n");
			String[] ks = lines[0].split("\\s+");
						
			for(String k: ks){
				m_colMap.put(k, n++);
			}
			
			int[] ns = new int[ks.length+1];
			for(n=0; n<ks.length;n++){//记录字段的位置.
				ns[n] = lines[0].indexOf(ks[n]);
			}
			for(int i=1; i<lines.length; i++){
				String[] vs = lines[i].split("\\s+");
				
				vs = new String[ks.length];
				ns[ks.length] = lines[1].length(); //字符串长度
				
				String val = "";
				for(n=0; n<ks.length;n++){
					val = lines[i].substring(ns[n], ns[n+1]);
					vs[n]=val.trim();
					//System.out.println("idx="+nIdx+" len="+nLen + " val="+vs[n]);
				}	
				ls.add(vs);
			}
		}while(false);
		}catch(	Exception e){
			e.printStackTrace();
		}
	
		return ls;
	}
	
	protected Object parseResponse(String result, int subModel) {
		
		if (result.length()>1024*1024){
			String ss = result.substring(0, 100);
			ss = ss.substring(0,ss.indexOf("item"));
			boolean bResult = (ss.indexOf("\"errors\":false"))>0;
			return bResult;
		}else if (ImUtils.isJson(result)){
			JSONObject jobs = new JSONObject(result);
			Iterator itr = jobs.keys();
			int len = jobs.length();
			
			if(len==1 && itr.hasNext()){
				Object line = itr.next();
				Object vals = jobs.get(line.toString());
				String vs = vals.toString();
				return JSONUtil.parseJSON(vs.toCharArray(), 0, vs.length()-1);
			}else{
				return JSONUtil.parseJSON(result.toCharArray(), 0, result.length()-1);
			}
		}else{	
			List<Object[]> ls = null;
			if (subModel == 1){
				ls = new ArrayList<Object[]>();
				parseCatJson(result,ls);
			}else if (subModel == 4){				
				ls = parseIndex(result);
			}else{
				ls = parseInfo(result);
			}
			Table tbl = toTable(ls);
			return tbl;
		}
	}
	
	protected Response doRun(String method, String endpoint, Object[] objs)
	{
		Response response = null;
		try {
			Map<String, String> map = new HashMap<String, String>();
			Header[] headers = null;
			Request request = new Request(method, endpoint);   
			
			if (objs.length>1){
				JSONObject job = null;
				Iterator iterator = null;
				if (objs.length>2){
					job = new JSONObject(objs[2].toString());
					iterator = job.keys();
					
					while (iterator.hasNext()) {
						String key = (String) iterator.next();
						String value = job.getString(key);
						map.put(key, value);
					}
				}
				
				if (objs.length>3){
					job = new JSONObject(objs[3].toString());
					iterator = job.keys();
					
					List<Header> header = new ArrayList<Header>();
					while (iterator.hasNext()) {
						String key = (String) iterator.next();
						String value = job.getString(key);
						BasicHeader head = new BasicHeader(key, value);
						header.add(head);
					}
					headers = new Header[header.size()];
					header.toArray(headers);
				}
				
				if (map.size()==0){			
					HttpEntity entity = new NStringEntity(objs[1].toString(), ContentType.APPLICATION_JSON); 
					request.setEntity(entity);
					response = m_restConn.m_restClient.performRequest(request);
				}else{
					HttpEntity entity = new NStringEntity(objs[1].toString(), ContentType.APPLICATION_JSON);
					request.addParameter("Content-Type", " application/x-ndjson");
					request.setEntity(entity);
					response = m_restConn.m_restClient.performRequest(request);
				}
			}else{
				response = m_restConn.m_restClient.performRequest(request);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return response;
	}
}