package com.raqsoft.lib.salesforce.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.raqsoft.dm.Table;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.SearchRecord;
import com.sforce.soap.partner.SearchResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.bind.XmlObject;

public class ImWsdlPartnerFind {
	private PartnerConnection m_conn = null;
	private Map<String, List<String>> m_ls = new HashMap<String, List<String>>(); //记录表及其字段
	
	public ImWsdlPartnerFind(PartnerConnection c) {
		m_conn = c;
	}

	/*****功能：执行查询*****
	 * 1、返回returning给定的字段
	 * 2、若Table无对应的字段则返回Id,但Id可能重复.
	 * 3.
	 * 
	 * */
	public Object find(String soqlQuery)  {
		Table tbl = null;
		try {
			ImWsdlCommon.doReturning(soqlQuery, m_ls);
			SearchResult ret = m_conn.search(soqlQuery);
			SearchRecord[] rds=ret.getSearchRecords();
			List<Table> ls = new ArrayList<Table>();		//记录要返回的多个Table		
			List<String> keys = new ArrayList<String>();	//字段
			List<Object> vals = new ArrayList<Object>();	//字段值, 注意：若用map则与keys顺序可能不对应
			
			String lastKey = "";
			String sKey = "";
			Table subTable = null;
			boolean bTable= false;
			List<String> lls = null; //要返回的字段
			
			for (SearchRecord s: rds){
				SObject o = s.getRecord();
				Iterator<XmlObject> iter = o.getChildren();
				while(iter.hasNext()){
					XmlObject xo = iter.next();
					sKey = xo.getName().getLocalPart();

					if (sKey.equals("type")){
						String val = xo.getValue().toString();
						if (!val.equals(lastKey)){
							vals .clear();
							keys.clear();
							if (subTable!=null){
								ls.add(subTable);
							}
							lastKey = val;
							bTable= true;
							lls = ImWsdlCommon.getListCols(m_ls, lastKey);
						}
					}else{
						// 记录值，并过滤掉不显示的字段
						if (lls!=null && lls.size()>0){
							if (lls.contains(sKey)){
								if (!keys.contains(sKey) ){
									vals.add(xo.getValue());
								}
							}
						}else{
							// 返回记录值可能存在字段相同的.
							if (!keys.contains(sKey) ){
								vals.add(xo.getValue());
							}
						}
						if (!keys.contains(sKey)){
							keys.add(sKey);
						}
					}
					
					//System.out.println(xo.getName().getLocalPart()+": "+xo.getValue());
				}
				if (bTable){	//绑定表字段
					String[] cols = null;					
					if (lls!=null && lls.size()>0){
						cols = lls.toArray(new String[0]);
					}else{
						cols = keys.toArray(new String[0]);
					}
					subTable = null;
					subTable = new Table(cols);
					bTable = false;
				}
				subTable.newLast(vals.toArray());
				keys.clear();
				vals.clear();				
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
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	    return tbl;
	}
		
}
