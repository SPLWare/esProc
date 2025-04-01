package com.scudata.lib.salesforce.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.scudata.dm.Table;
import com.scudata.common.Logger;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.SearchRecord;
import com.sforce.soap.partner.SearchResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.bind.XmlObject;

public class ImWsdlPartnerFind {
	private PartnerConnection m_conn = null;
	private Map<String, List<String>> m_ls = new HashMap<String, List<String>>(); //璁板綍琛ㄥ強鍏跺瓧娆�
	
	public ImWsdlPartnerFind(PartnerConnection c) {
		m_conn = c;
	}

	/*****鍔熻兘锛氭墽琛屾煡璀�*****
	 * 1銆佽繑鍥瀝eturning缁欏畾鐨勫瓧娆�
	 * 2銆佽嫢Table鏃犲搴旂殑瀛楁鍒欒繑鍥濱d,浣咺d鍙兘閲嶅.
	 * 3.
	 * 
	 * */
	public Object find(String soqlQuery)  {
		Table tbl = null;
		try {
			ImWsdlCommon.doReturning(soqlQuery, m_ls);
			SearchResult ret = m_conn.search(soqlQuery);
			SearchRecord[] rds=ret.getSearchRecords();
			List<Table> ls = new ArrayList<Table>();		//璁板綍瑕佽繑鍥炵殑澶氫釜Table		
			List<String> keys = new ArrayList<String>();	//瀛楁
			List<Object> vals = new ArrayList<Object>();	//瀛楁渚�, 娉ㄦ剰锛氳嫢鐢╩ap鍒欎笌keys椤哄簭鍙兘涓嶅甯�
			
			String lastKey = "";
			String sKey = "";
			Table subTable = null;
			boolean bTable= false;
			List<String> lls = null; //瑕佽繑鍥炵殑瀛楁
			
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
						// 璁板綍鍊硷紝骞惰繃婊ゆ帀涓嶆樉绀虹殑瀛楁
						if (lls!=null && lls.size()>0){
							if (lls.contains(sKey)){
								if (!keys.contains(sKey) ){
									vals.add(xo.getValue());
								}
							}
						}else{
							// 杩斿洖璁板綍鍊煎彲鑳藉瓨鍦ㄥ瓧娈电浉鍚岀殑.
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
				if (bTable){	//缁戝畾琛ㄥ瓧娆�
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
			Logger.error(e.getMessage());
		}
		
	    return tbl;
	}
		
}
