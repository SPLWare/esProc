package com.raqsoft.lib.salesforce.function;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.raqsoft.dm.Table;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.SearchRecord;
import com.sforce.soap.enterprise.SearchResult;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.bind.XMLizable;

public class ImWsdlEnterpriceFind {
	private EnterpriseConnection m_conn = null;
	private Map<String, List<String>> m_colMap = new HashMap<String, List<String>>(); //用于记录表及其字段
	private Map<String, Map<String, Method>> m_map; //记录表及其方法
	
	public ImWsdlEnterpriceFind(EnterpriseConnection c) {
		m_conn = c;
		m_map = new HashMap<String, Map<String, Method>>();
	}

	/*****功能：执行查询*****
	 * 1、返回returning给定的字段
	 * 2、若Table无对应的字段则返回Id,但Id可能重复.
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
				
			List<Table> ls = new ArrayList<Table>();		//记录要返回的多个Table	
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
					//根据函数方法查询对应的value
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
			e.printStackTrace();
		}
		
	    return tbl;
	}
	
	//通过SearchRecord获取N个表对应的方法
	public void doMapMethod( SearchRecord[] rds) {
		try {
			m_map.clear();
			Map<String, SObject> omap = new HashMap<String, SObject>(); 
			SObject so = null;
			//获取表与对应的SObject对象
			for (SearchRecord s: rds){
				so = s.getRecord();
				String sName = ImWsdlCommon.getClassName(so);
				if (!omap.containsKey(sName)){
					omap.put(sName, so);
					//无字段的表处理
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
		        // 循环查找想要的方法
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
			e.printStackTrace();
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
