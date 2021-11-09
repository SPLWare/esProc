package com.raqsoft.lib.salesforce.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Table;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.bind.XmlObject;

public class ImWsdlPartner {
	private PartnerConnection m_conn = null;
	
	public ImWsdlPartner(PartnerConnection c) {
		m_conn = c;
	}
	
	public Object query(String soqlQuery)  {
		Table tbl = null;
		try {
		    //soqlQuery = "SELECT FirstName, LastName FROM Contact limit 10";
			if (m_conn==null) return tbl;
			if (soqlQuery.toLowerCase().startsWith("find")){
				ImWsdlPartnerFind cls = new ImWsdlPartnerFind(m_conn);
				return cls.find(soqlQuery);
			}
			String[] cols = null;
			Object[] os = null;
			SObject record = null;
			List<String> keys = new ArrayList<String>();	//字段可能重复
			
			String sSql = soqlQuery.toLowerCase();
			int off = sSql.indexOf("fields");
			if (off!=-1){
				sSql = sSql.substring(off).replaceAll(" +", "");
			}
			if (sSql.matches("fields(.*)")){
				List<String> columns = null;
				QueryResult qr = m_conn.query(soqlQuery);
			    SObject[] recordList = qr.getRecords();
			    if (recordList.length>0){
			    	record = recordList[0];
			    	columns = new ArrayList<String>();		    	
			    	Iterator<XmlObject> iter = record.getChildren();
			    	while (iter.hasNext()){
			        	XmlObject xo = iter.next();
			        	String s = xo.getName().getLocalPart();
			        	if( s.equalsIgnoreCase("type") || s.equalsIgnoreCase("Id")){
			        		;
				    	}else{
				    		columns.add(s);
				    	}
			        	//System.out.println(xo.getValue() + "= " + xo.getName().getLocalPart());
			        }
			    	cols = columns.toArray(new String[columns.size()]);
			    	tbl = new Table(cols);
			    }
			    
			    for (int i = 0; i < recordList.length; i++) {
			    	int n = 0;
			    	os = new Object[cols.length];
			    	record = recordList[i];
			    			    	 
			        Iterator<XmlObject> iter = record.getChildren();
			        while (iter.hasNext()){
			        	XmlObject xo = iter.next();
			        	String s = xo.getName().getLocalPart();
			        	if( s.equalsIgnoreCase("type") || s.equalsIgnoreCase("Id")){
			        		;
				    	}else{
				    		if(!keys.contains(s)){
				    			os[n++]=xo.getValue();
				    			keys.add(s);
				    		}
				    	}
			        	//System.out.println(xo.getValue() + "= " + xo.getName().getLocalPart());
			        }
			        keys.clear();
			        tbl.newLast(os);
			    }
			}else{
			    cols = ImUtils.getTableCols(soqlQuery);
				if (cols!=null) {
					tbl = new Table(cols);
				}
				int n = 0;
				List<String> columns = Arrays.asList(cols);;
			    QueryResult qr = m_conn.query(soqlQuery);
			    SObject[] recordList = qr.getRecords();
			    if (recordList.length<1) return null;
				
			    for (int i = 0; i < recordList.length; i++) {
			    	n = 0;
			    	os = new Object[cols.length];
			    	record = recordList[i];
			    	Iterator<XmlObject> iter = record.getChildren();
			        while (iter.hasNext()){
			        	XmlObject xo = iter.next();
			        	String s = xo.getName().getLocalPart();
			        	if(columns.indexOf(s)!=-1){
			        		if(!keys.contains(s)){
			        			os[n++]=xo.getValue();
			        			keys.add(s);
			        		}
			        	}else if( s.equalsIgnoreCase("type") || s.equalsIgnoreCase("Id")){
			        		;
				    	}else{
				    		String[] fs=isSubRecord(columns, s);
				    		if(fs!=null && fs.length==2){
				    			if (fs[0].equalsIgnoreCase(s)){
				    				Iterator<XmlObject> it = xo.getChildren();
				    				while (it.hasNext()){
				    					XmlObject subX = it.next();
				    					String ss = subX.getName().getLocalPart();
				    					if (ss.equalsIgnoreCase(fs[1])){
				    						os[n++]=subX.getValue();
				    						break;
				    					}
				    				}
				    			}
				    		}
				    	}
			        	//System.out.println(xo.getValue() + "= " + xo.getName().getLocalPart());
			        }
			        keys.clear();
			        tbl.newLast(os);
			    }
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	    return tbl;
	}
	
	public Object doQuery(Object[] objs) {
		if (objs==null || objs.length!=2) {
			throw new RQException("WSDL query function.missingParam ");
		}

		return query( objs[1].toString() );
	}
	
	private String[] isSubRecord(List<String> columns, String val){
		 String line="";
		 for(int i=0; i<columns.size(); i++){
			 line = columns.get(i);
			 if (line.indexOf(val)!=-1){
				 return line.split("\\.");
			 }
		 }
		 return null;
	 }
	
}
