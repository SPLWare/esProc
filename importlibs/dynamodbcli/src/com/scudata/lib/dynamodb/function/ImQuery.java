package com.scudata.lib.dynamodb.function;


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementResult;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.sql.SQLUtil;

//dyna_query(fp, sql; p1,...)
public class ImQuery extends ImFunction {
	private int m_nCheckSize = 100; //获取字段时所需要的最大记录数
	
	public Object doExec(Object[] objs){		
		try {
			ExecuteStatementResult result = null;
			if (objs[0] instanceof Sequence){
				Table table = null;
				
				Sequence seq = (Sequence)objs[0];				
				Sequence ssql = (Sequence)SQLUtil.parse(objs[1].toString(), null);
				
				// Sql参数个数
				int nCnt = getSqlParamSize(ssql);
				Object[] os = new Object[nCnt+1];
				os[0] = objs[1];
				
				for(int i=1; i<=seq.length(); i+=nCnt){
					for(int j=0; j<nCnt; j++){
						os[1+j] = seq.get(i+j);
					}
					result = (ExecuteStatementResult)super.doExec(os);
					if (result!=null && result.getItems().size()>0){						
						Table tbl = resultToTable(result, ssql.get(1).toString());
						if (table==null){
							table = tbl;
						}else{
							table.addAll(tbl);
						}
					}
				}
				//BatchExecuteStatementResult result2 = batchExecuteStatementRequest(m_db.m_client, objs[1].toString(), seq, nCnt);
				return table;
			}else{
				result = (ExecuteStatementResult)super.doExec(objs);
				if (result!=null){
					Object o = SQLUtil.parse(objs[0].toString(), null);
					return resultToTable(result, ((Sequence)o).get(1).toString());
				}	
			}
		}catch(Exception e) {
			Logger.error("doExec: "+e.getMessage());
		}
		return null;
	}
	
//	 public static BatchExecuteStatementResult batchExecuteStatementRequest(
//			 AmazonDynamoDB client, String sql, Sequence seq, int paramSize ) {
//    	try{
//			BatchExecuteStatementRequest requests = new BatchExecuteStatementRequest();
//			List<BatchStatementRequest> statements = new ArrayList<BatchStatementRequest>();
//			
//			for(int i=1; i<=seq.length(); i+=paramSize){
//				List<AttributeValue> parameters = new ArrayList<AttributeValue>();
//				for(int j=0; j<paramSize; j++){
//					Object o = seq.get(i+j);
//					AttributeValue a = new AttributeValue();
//					if (o instanceof String){
//						a.setS(o.toString());
//					}else if(o instanceof Integer){
//						a.setN(o.toString());
//					}
//				    parameters.add(a);
//				}
//				statements.add(new BatchStatementRequest()
//						.withStatement(sql)
//						.withParameters(parameters));
//				break;
//			}
//	        requests.withStatements(statements);       
//	        return client.batchExecuteStatement(requests);
//    	}catch(Exception e){
//    		Logger.error(e.getMessage());
//    	}
//    	return null;
//    }
	
	private int getSqlParamSize(Sequence ssql){
		int nCnt = 1;
		if (ssql.length()>3){
			String sWhere = ssql.get(3).toString();
			String sMark = sWhere.replace("?", "");
			nCnt = sWhere.length() - sMark.length();
		}
		if (nCnt<1){
			throw new RQException("Sql Param Error");
		}
		return nCnt;
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
			}else if(a.getNULL()!=null){
	    		ret = a.getNULL();
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
			Logger.error("parseNumber: "+e.getStackTrace());
		}
		return ret;
	}

}
