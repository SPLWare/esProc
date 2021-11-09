package com.raqsoft.lib.salesforce.function;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Table;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.bind.XMLizable;

public class ImWsdlQuery extends ImFunction {
	private ImWsdlOpen m_wsdl = null;
	private Map<String, Method> m_map;
	
	public ImWsdlQuery(){
		m_map = new HashMap<String, Method>();
	}

	public Object doQuery(Object[] objs) {
		if (objs==null || objs.length!=2) {
			throw new RQException("WSDL query function.missingParam ");
		}
		if (objs[0] instanceof ImWsdlOpen) {
			m_wsdl = (ImWsdlOpen)objs[0];
		}
		String sql = null;
		if (objs[1] instanceof String) {
			 sql = (String)objs[1];
		}
		if (m_wsdl==null || sql==null) {
			throw new RQException("WSDL query function.type error ");
		}
		// for partner;
		if (m_wsdl.m_parterConn!=null) {
			ImWsdlPartner p = new ImWsdlPartner(m_wsdl.m_parterConn); 
			return p.doQuery( objs );
		}else {
			return query( sql );
		}
	}

	public Object query(String sql) {
		Table tbl = null;
		try {
			// "SELECT Id, FirstName, LastName, Account.Name " +
			// "FROM Contact WHERE AccountId != NULL ORDER BY CreatedDate DESC LIMIT 5"
			// query for the 5 newest contacts
			if (m_wsdl.m_enterpriseConn==null) return tbl;
			if (sql.toLowerCase().startsWith("find")){
				ImWsdlEnterpriceFind cls = new ImWsdlEnterpriceFind(m_wsdl.m_enterpriseConn);
				return cls.find(sql);
			}
			
			int n = 0;
			QueryResult queryResults = m_wsdl.m_enterpriseConn.query(sql);
			String[] cols = null;
			if (queryResults.getSize() > 0) {
				cols = ImUtils.getTableCols(sql);
				if (cols!=null) {
					tbl = new Table(cols);
				}
				
				SObject res = null;
				SObject[] results=  queryResults.getRecords();
				if (results.length<1)  return null;
				
				ImWsdlCommon.doMapMethod(m_map,results[0]);
				Method md = null;
				for (int i = 0; i < results.length; i++) {
					Object[] line = new Object[cols.length];
					res = results[i];
					n = 0;
					for(String col : cols) {
						String[] subs= col.split("\\.");
						if (subs.length==2) {
							line[n++] = ImWsdlCommon.getSubRecord(m_map,subs, res);
							continue;
						}else {
							md = m_map.get(col.toLowerCase());
						}
						//根据函数方法查询对应的value
						if (md!=null) {
							Object val = md.invoke(res);
							if (val instanceof XMLizable) {	//嵌套字记录
								line[n++]=ImWsdlCommon.getSubRecordOfClass(val);
							}else {
								line[n++]=val;
							}
						}else {
							System.out.println(col+": function not found");
						}
					}
					tbl.newLast(line);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return tbl;
	}
	
}
