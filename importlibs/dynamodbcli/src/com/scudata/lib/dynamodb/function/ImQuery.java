package com.scudata.lib.dynamodb.function;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementRequest;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementResult;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.sql.SQLUtil;

//dyna_query(fp, sql; p1,...)
public class ImQuery extends ImFunction {
	private int m_nCheckSize = 100; //获取字段时所需要的最大记录数
	
	public Object doQuery(Object[] objs){
		try {
			if (objs.length<2){
				throw new RQException("dyna_query missingParam");
			}

			ImOpen imOpen = null;
			String strSql = null;
			if (objs[0] instanceof ImOpen){
				imOpen = (ImOpen)objs[0];
			}
			
			if (objs[1] instanceof String){
				strSql = objs[1].toString();
			}
			
			if (imOpen==null || strSql==null){
				throw new RQException("dyna_query paramTypeError");
			}
			Object o = SQLUtil.parse(strSql, null);
			
			List<AttributeValue> parameters = new ArrayList<AttributeValue>();
			for (int n=2; n<objs.length; n++){
				AttributeValue a = new AttributeValue();
				if (objs[n] instanceof String){
					a.setS(objs[n].toString());
				}else if(objs[n] instanceof Integer){
					a.setN(objs[n].toString());
				}
			    parameters.add(a);
			}
			
			ExecuteStatementResult result = executeStatementRequest(imOpen.m_client, strSql, parameters);			 
			return resultToTable(result, ((Sequence)o).get(1).toString());
		}catch(Exception e) {
			Logger.error(e.getStackTrace());
		}
		return null;
	}
	
	private Table resultToTable(ExecuteStatementResult result, String strCols){
		Table tbl = null;
		try{
			String[] cols = null;
			if (strCols.equals("*")){
				cols = getColsFromScanItem(result);
			}else{
				cols = strCols.split(",");
				for(int n=0; n<cols.length; n++){
					cols[n] = cols[n].trim();
				}
			}
			tbl = new Table (cols);
	        for (Map<String, AttributeValue> item : result.getItems()) {
	        	int n = 0;
		        Object objs[] =  new Object[cols.length];
	        	for(String key:cols){
	        		if (item.containsKey(key)){
	        			AttributeValue value = item.get(key);
	        			objs[n] = doAttributeValue(value);
	        		}
		        	n++;
		        }
	        	tbl.newLast(objs);
	        }
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		
		return tbl;
	}
	
	private String[] getColsFromScanItem(ExecuteStatementResult result){
		String[] fnames = null;
		Set<String> set = null, lastSet = null;
		Set<String> hset = new LinkedHashSet<String>();
		int nOffset = 0;
		
		for (Map<String, AttributeValue> item : result.getItems()) {
			set = item.keySet();
			
			if (!set.equals(lastSet)){
				lastSet = set; 
				hset.addAll(set);
			}
			if (++nOffset>=m_nCheckSize){
				break;
			}
		}

		if (hset.isEmpty()) {
			return null;
		}		

		fnames = new String[hset.size()];
		hset.toArray(fnames);

		return fnames;
	}
	
	private Object doAttributeValue(AttributeValue a){
		Object ret = null;
		
		try{
			if (a.getS()!=null){
				ret = a.getS();
			}else if(a.getBOOL()!=null){
	    		ret = a.getBOOL();
			}else if(a.getBOOL()!=null){
	    		ret = a.getBOOL();
			}else if(a.getN()!=null){				
	    		ret = parseNumber(a.getN());
			}else if(a.getL()!=null){
	    		List<AttributeValue> ls = a.getL();
	    		Sequence subSeq = new Sequence();
				for(AttributeValue o: ls){
					subSeq.add(doAttributeValue(o));
				}
	    		ret = subSeq;
			}else if(a.getSS()!=null ){
				Sequence subSeq = new Sequence();
	    		List<String> ls = a.getSS();
	    		for(String o: ls){
					subSeq.add(o);
				}
	    		ret = subSeq;
			}else if(a.getNS()!=null ){
				Sequence subSeq = new Sequence();
	    		List<String> ls = a.getNS();
	    		for(String o: ls){
					subSeq.add(parseNumber(o));
				}
	    		ret = subSeq;
	    	}else if(a.getM()!=null){
	    		 Map<String, AttributeValue> m = a.getM();
	    		 Table subTbl = new Table(new String[]{"key","value"});
	             for(String key:m.keySet()){
	            	 AttributeValue sub = m.get(key);
	            	 Object v = doAttributeValue(sub);
	            	 subTbl.newLast(new Object[]{key, v});
	             }
	             ret = subTbl;
	    	}else if(a.getB()!=null){
	    		ret = a.getB();
	        }
		}catch(Exception e){
			Logger.error(e.getStackTrace());
		}
		
		return ret;
	}
	
	private static ExecuteStatementResult executeStatementRequest(AmazonDynamoDB client, String statement, List<AttributeValue> parameters ) {
        ExecuteStatementRequest request = new ExecuteStatementRequest();
        request.setStatement(statement);
        if (parameters!=null){
        	request.setParameters(parameters);
        }
        return client.executeStatement(request);
    }
	
	private Object parseNumber(String val){
		Object ret = null;
		try{
			if (val==null) return ret;
			if (val.contains(".")){
				ret = Double.valueOf(val);
			}else{
				ret = Integer.valueOf(val);
			}
		}catch(Exception e){
			Logger.error(e.getStackTrace());
		}
		return ret;
	}

}
