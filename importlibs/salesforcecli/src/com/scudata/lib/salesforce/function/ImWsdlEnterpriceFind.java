package com.scudata.lib.salesforce.function;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.scudata.dm.Table;
import com.scudata.common.Logger;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.SearchRecord;
import com.sforce.soap.enterprise.SearchResult;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.bind.XMLizable;

public class ImWsdlEnterpriceFind {
	private EnterpriseConnection m_conn = null;
	private Map<String, List<String>> m_colMap = new HashMap<String, List<String>>(); //鐢ㄤ簬璁板綍琛ㄥ強鍏跺瓧娆�
	private Map<String, Map<String, Method>> m_map; //璁板綍琛ㄥ強鍏舵柟姹�
	
	public ImWsdlEnterpriceFind(EnterpriseConnection c) {
		m_conn = c;
		m_map = new HashMap<String, Map<String, Method>>();
	}

	/*****鍔熻兘锛氭墽琛屾煡璀�*****
	 * 1銆佽繑鍥瀝eturning缁欏畾鐨勫瓧娆�
	 * 2銆佽嫢Table鏃犲搴旂殑瀛楁鍒欒繑鍥濱d,浣咺d鍙兘閲嶅.
	 * 
	 * */
	public Object find(String soqlQuery)  {
		Table tbl = null;
		try {
			SearchResult ret = m_conn.search(soqlQuery);
			if (ret==null) return null;
			
			m_colMap.clear();
			SearchRecord[] rds=ret.getSearchRecords();
			if (rds.length<1) return null;
			ImWsdlCommon.doReturning(soqlQuery, m_colMap);
			doMapMethod(rds);
				
			List<Table> ls = new ArrayList<Table>();		//璁板綍瑕佽繑鍥炵殑澶氫釜Table	
			String lastClsName = "";
			Table subTable = null;
			Map<String, Method> methods = null;
			
			for (SearchRecord s: rds){
				SObject res = s.getRecord();
				String clsName = ImWsdlCommon.getClassName(res);
				//System.out.println("table= "+clsName);
				String[] cols = ImWsdlCommon.getArrayCols(m_colMap, clsName);
				if (!lastClsName.equalsIgnoreCase(clsName) ){
					if (subTable!=null){
						ls.add(subTable);
					}
					subTable = new Table(cols);
					methods = getClassMethod(clsName);
					lastClsName = clsName;
				}
				
				int n = 0;
				Method md = null;
				Object[] line = new Object[cols.length];
				for(String col : cols) {
					String[] subs= col.split("\\.");
					if (subs.length==2) {
						line[n++] = ImWsdlCommon.getSubRecord(methods, subs, res);
						continue;
					}else {
						md = methods.get(col);
					}
					//鏍规嵁鍑芥暟鏂规硶鏌ヨ瀵瑰簲鐨剉alue
					if (md!=null) {
						Object val = md.invoke(res);
						//System.out.println(col+": " + val+"; ");
						if (val instanceof XMLizable) {
							line[n++]=ImWsdlCommon.getSubRecordOfClass(val);
						}else {
							line[n++]=val;
						}
					}else {
						System.out.println(col+": function not found");
					}
				}
				subTable.newLast(line);
			}
			if (subTable!=null){
				ls.add(subTable);
			}
			
			if (ls.size()==1){
				tbl = ls.get(0);
			}else{
				tbl = new Table(new String[]{"Values"});
				Table[] tbls = ls.toArray(new Table[0]);
				for(Table t:tbls){
					tbl.newLast(new Object[]{t});
				}
			}
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		
	    return tbl;
	}
	
	//閫氳繃SearchRecord鑾峰彇N涓〃瀵瑰簲鐨勬柟姹�
	public void doMapMethod( SearchRecord[] rds) {
		try {
			m_map.clear();
			Map<String, SObject> omap = new HashMap<String, SObject>(); 
			SObject so = null;
			//鑾峰彇琛ㄤ笌瀵瑰簲鐨凷Object瀵硅薄
			for (SearchRecord s: rds){
				so = s.getRecord();
				String sName = ImWsdlCommon.getClassName(so);
				if (!omap.containsKey(sName)){
					omap.put(sName, so);
					//鏃犲瓧娈电殑琛ㄥ鐜�
					if (!m_colMap.containsKey(sName)){
						List<String> vals = new ArrayList<String>(){{add("Id");}};
						m_colMap.put(sName, vals);
					}
				}
			}
			
			for(Map.Entry<String, SObject> entry : omap.entrySet()){
			    String mapKey = entry.getKey();
			    SObject mapValue = entry.getValue();

			    List<String> cols = ImWsdlCommon.getListCols(m_colMap, mapKey);
				Map<String, Method> map = new HashMap<>();
				Class<?> catClass = Class.forName(mapValue.getClass().getName());
		        Method[] methods = catClass.getMethods();
		        // 寰幆鏌ユ壘鎯宠鐨勬柟姹�
		        String key = null;
		        for(Method method : methods) {
		        	if (method.getName().startsWith("get")) {
		        		key = method.getName().substring(3);
		        		if (cols.contains(key)){
		        			map.put(key, method);
		        		}
		        	}
		        }
		        m_map.put(mapKey, map);
			}
	        
		}catch(Exception e) {
			Logger.error(e.getMessage());
		}
		//System.out.println();
	}
	
	private Map<String, Method> getClassMethod(String clsName){
		Map<String, Method> ret = null;
		for(String k : m_map.keySet()){
			if (k.equalsIgnoreCase(clsName)){
				ret = m_map.get(k);
				break;
			}
		}
		return ret;
	}
	
	public static void main2(String[] args){
		String sql = "FIND {United Oil*} IN ALL FIELDS  RETURNING Account, Contact( name,email ),Goods, Opportunity( name  , StageName )";
		Map<String, List<String>> map = new HashMap<String, List<String>>(); 
		ImWsdlEnterpriceFind cls = new ImWsdlEnterpriceFind(null);
		ImWsdlCommon.doReturning(sql, map);
	 }
}
