package com.raqsoft.lib.salesforce.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.sql.SQLUtil;

public class ImQuery extends ImFunction {
	private ImOpen m_open = null;
	
	public Object doQuery(Object[] objs){
		if (objs==null || objs.length<2){
			throw new RQException("query function.missingParam ");
		}
		
		if (objs[0] instanceof ImOpen) {
			m_open = (ImOpen)objs[0];
		}
		
		if (m_open == null) {
			throw new RQException("query2 function.missingParam ");
		}
		if (objs[1] instanceof String ) {
			
		}else {
			throw new RQException("query3 function.missingParam ");
		}
		if ( objs.length>=3 && objs[2] instanceof String) {
			return getData(objs[1].toString(), objs[2].toString() );
		}else {
			return getData(objs[1].toString(), null );
		}		
	}
	
	/* 功能：将sql改为rest api格式格式,字符串之间用"+"连接
	 * 
	 */
	private String getSqlString(String val) {
		if (val == null || val.isEmpty()) return null;
		String ret = null;
		try {
			ret = "select";
			val = val.replaceAll("\n", " ");
			val = val.replaceAll("\r", " ");
			Object result = SQLUtil.parse(val, null);
			if (result instanceof Sequence) {
				Sequence seq = (Sequence)result;
				Object obj = seq.get(1);
				String ss = obj.toString();
				ss = ss.replaceAll(" +", " ");
				ss = ss.replaceAll(", ",",");
				ss = ss.replaceAll(",","+,+");
				ss = ss.replaceAll(" ", "+");
				ret += "+"+ss;	 								//field
				ss = seq.get(2).toString(); 		 			//tableName
				ss = ss.replaceAll(" +", " ");
				ss = ss.replaceAll(" ", "+");
				ret += "+from+"+ss;
				obj = seq.get(3);								//where
				if (obj!=null){
					ss = obj.toString().toLowerCase().replaceAll(" +", "+");
					ss = ss.replaceAll("%", "%25");
					ss = ss.replaceAll("/", "%2F");
					ss = ss.replaceAll("\\?", "%3F");
					ss = ss.replaceAll("#", "%23");
					ss = ss.replaceAll("&", "%26");
					//ss = "isactive+=+true+and+email+like+'auto'";
					ret += "+where+"+ss; 
				}
				obj = seq.get(4);								//group
				if (obj!=null){
					ss = obj.toString().replaceAll(" +", "");
					ret += "+group%20by+"+ss; 	 
				}
				obj = seq.get(5);								//having
				if (obj!=null){			
					ss = obj.toString().replaceAll(" +", "");
					ret += "+having+"+ss; 	 		
				}
				obj = seq.get(6);								//order
				if (obj!=null){
					ss = obj.toString().replaceAll(" +", "");
					ret += "+order+by+"+ss; 	 
				}				
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public Object getData(String url, String sVal) {
	    System.out.println("****************Case QUERY**************");
	    Table tbl = null;
	    try {
	    	Map<String, List<String>> tblMap = null; 	//记录子表名及其字段
	    	String finalURI = null;
	    	if(sVal!=null) {
		    	String val = sVal;
		    	if (ImUtils.isSampleSql(val)) {
		    		val = getSqlString(sVal);
		    		tblMap = ImWsdlCommon.doSubTableInfo(sVal);
		    		// query+sql
		    		finalURI = String.format("%s/%s?q=%s",m_open.m_instanceUrl, url, val);
		    	}else if(val.indexOf("=")>-1) {
		    		// apex api + param
		    		finalURI = String.format("%s/%s?%s",m_open.m_instanceUrl, url, val);
		    	}else {
		    		//sobject + id
		    		finalURI = String.format("%s/%s/%s",m_open.m_instanceUrl, url, val);
		    	}
	    	}else {
	    		// apex api
	    		finalURI = String.format("%s/%s",m_open.m_instanceUrl, url);
	    	}
	      
	      HttpClient httpClient = HttpClientBuilder.create().build();
	      	  
	      System.out.println("Query URL: " + finalURI);
	      HttpGet httpGet = new HttpGet(finalURI);
	      httpGet.addHeader(m_open.m_oAuthHeader);
	      httpGet.addHeader(m_open.m_printHeader);	 
	      HttpResponse httpResponse = httpClient.execute(httpGet);
	 
	      int statusCode = httpResponse.getStatusLine().getStatusCode();
	      if (statusCode == 200) {
	        String responseString = EntityUtils.toString(httpResponse.getEntity());
	        try {
	        	JSONObject jsonObject = null;
	        	JSONArray jsonArray = null;
	        	//System.out.println("responseString ="+responseString);
                 Object rspObj = new JSONTokener( responseString ).nextValue();
                 if (rspObj instanceof JSONArray) {
                 	jsonArray = new JSONArray(responseString);
                 }else if(rspObj instanceof JSONObject) {
                	 jsonObject = new JSONObject(responseString);
                	 if (jsonObject.has("records")) {
                		 jsonArray = jsonObject.getJSONArray("records");
       	          	 }
                 }
              Object line = null;
               //多条记录
	          if (jsonArray!=null && jsonArray.length()>0) {
	        	  String[] cols=JSONObject.getNames(jsonArray.getJSONObject(0));
	        	  cols = doFilterCols(cols, tblMap);
	        	  
	        	  Object[] os = null;
		          tbl = new Table(cols);
		          
		          for (int i = 0; i < jsonArray.length(); i++) {
		        	  JSONObject item = jsonArray.getJSONObject(i);
		        	  os = new Object[cols.length];
		        	  for(int j=0; j<cols.length; j++) {
		        		  line = item.get(cols[j]);
		        		  if (line instanceof JSONObject) {
		        			  JSONObject jLine = (JSONObject)line;
		        			  if (tblMap!=null ){ //子表记录
		        				  //System.out.println("tblMap =" + cols[j]);
		        				  if (tblMap.containsKey(cols[j])){
				        			  String[] subCols = tblMap.get(cols[j]).toArray(new String[0]);
				        			  Object subObj = ImUtils.getSubRecords( jLine, cols[j], subCols);
				        			  os[j] = subObj;
				        			  continue;
				        		  }
		        			  }
		        			  
		        			  os[j] = ImUtils.getSubRecords( ((JSONObject)line));
		        		  }else{
		        			  os[j] = line;
		        		  }
		        	  }
	        	 
		        	  tbl.newLast(os);
		          }
		      //单条记录
	          }else if(jsonObject.has("attributes")) {
	        	  String[] cols=JSONObject.getNames(jsonObject);
		          tbl = new Table(cols);
		          Object[] os = new Object[cols.length];
	        	  for(int j=0; j<cols.length; j++) {
	        		  line = jsonObject.get(cols[j]);
	        		  if (line instanceof JSONObject) {
	        			  os[j] = ImUtils.getSubRecords( ((JSONObject)line));
	        			  continue;
	        		  }
	        		  os[j] = line;
	        	  }
	        	  tbl.newLast(os);
	          }
	        } catch (JSONException jsonException) {
	          jsonException.printStackTrace();
	        }
	      } else {
	        System.out.print("Query was unsuccessful. Status code returned is " + statusCode);
	        System.out.println(httpResponse.getEntity().getContent());
	      }
	   } catch (Exception exception) {
	      exception.printStackTrace();
	    }
	    
	    return tbl;
	  }
		
	// 过滤表字段.
	private String[] doFilterCols(String[] cols, Map<String, List<String>> tblMap) {
		List<String> ls = new ArrayList<String>();
		List<String> mls = tblMap.get("_MainTable_");
		for (String c : cols) {
			if (mls.contains(c)) {
				ls.add(c);
			}
		}

		return ls.toArray(new String[0]);
	}
	
	//获取子记录，返回
	private Table getSubArray(JSONArray array) {
		Table subT = null;
		try {
			if (array.length()<1) return null;
			String[] sCols=JSONObject.getNames(array.get(0));
			  subT = new Table(sCols);
			  for(int n = 0;n<array.length();n++) {
				  JSONObject item = array.getJSONObject(n);
				  Object[] oSub = new Object[sCols.length];
				  for(String s: sCols) {		        				  
					  oSub[n++] = item.get(s);
				  }					 
				  subT.newLast(oSub);
			  }
		} catch (JSONException e) {
			e.printStackTrace();
		}
		  
		return subT;
	}
}
