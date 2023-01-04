package com.scudata.lib.dynamodb.function;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchExecuteStatementRequest;
import com.amazonaws.services.dynamodbv2.model.BatchExecuteStatementResult;
import com.amazonaws.services.dynamodbv2.model.BatchStatementRequest;
import com.scudata.common.Logger;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.Cursor;
import com.scudata.expression.Expression;

//dyna_execute(sql; p1,...)
public class ImExecute extends ImFunction {
	
	protected Object doExecEx(Object[] objs, String info){
		boolean bRet = false;
		try {
			//batch execute
			if (objs[0] instanceof Sequence || objs[0] instanceof ICursor){
				BatchExecuteStatementRequest reqs = batchConverSql(objs);
				BatchExecuteStatementResult result = m_db.m_client.batchExecuteStatement(reqs);
				if (result!=null){
					bRet = (result.getSdkHttpMetadata().getHttpStatusCode()==200);
				}
			}else{
				String sql = objs[0].toString().trim(); 
				if (sql.toLowerCase().startsWith("insert")){
					objs[0] = ImSqlUtil.convertSqlJson(sql);
				}
				
				//sql = "insert into t (f,..) values (?)", [seq]
				if(objs.length>1 && objs[1] instanceof Sequence){
					Sequence seq = (Sequence)objs[1];					
					Object[] vs = new Object[seq.length()+1];
					vs[0] = objs[0];
					for(int i=1; i<=seq.length();i++){
						vs[i]=seq.get(i);
					}
					bRet = (boolean)super.doExecEx(vs, info);
				}else{
					bRet = (boolean)super.doExecEx(objs, info);
				}
			}
		}catch(Exception e) {
			Logger.error(e.getMessage());
		}
		
		return bRet;
	}
	
	private BatchExecuteStatementRequest batchConverSql(Object[] objs) {
		try{
			String sql = null;
			if (objs[1] instanceof String){
				sql = objs[1].toString();			
				if (sql.toLowerCase().startsWith("insert")){
					sql = ImSqlUtil.convertSqlJson(sql);
				}
			}
			
			BatchExecuteStatementRequest requests = new BatchExecuteStatementRequest();
			List<BatchStatementRequest> statements = new ArrayList<BatchStatementRequest>();
			
			if (objs[0] instanceof Table || objs[0] instanceof ICursor){
				Table table = null;
				if (objs[0] instanceof ICursor){
					ICursor c = (ICursor)objs[0];
					table = (Table)c.fetch();
				}else{
					table = (Table)objs[0];
				}
				
				for(int i=0; i<table.length(); i++){
					Object o = table.get(i+1);
					BaseRecord r = (BaseRecord)o;
					List<AttributeValue> parameters = new ArrayList<AttributeValue>();
					
					// for params
					for(int j=2; j<objs.length; j++){
						if (objs[j] instanceof Expression){
							o = r.calc((Expression)objs[j], m_ctx);						
						}else{
							o = objs[j];
						}
						
						AttributeValue a = new AttributeValue();
						if (o instanceof String){
							a.setS(o.toString());
						}else if(o instanceof Integer){
							a.setN(o.toString());
						}
					    parameters.add(a);
					}
					statements.add(new BatchStatementRequest().withStatement(sql)
									.withParameters(parameters));
				}
				requests.withStatements(statements);
			}else if (objs[0] instanceof Sequence){
				Sequence table = (Sequence)objs[0];
				String[] cols=ImSqlUtil.getCols();
				
				for(int i=0; i<table.length(); i+=cols.length){				
					List<AttributeValue> parameters = new ArrayList<AttributeValue>();
					// for params
					for(int j=0; j<cols.length; j++){
						Object o = table.get(j+i+1);
						
						AttributeValue a = new AttributeValue();
						if (o instanceof String){
							a.setS(o.toString());
						}else if(o instanceof Integer){
							a.setN(o.toString());
						}
					    parameters.add(a);
					}
					statements.add(new BatchStatementRequest().withStatement(sql)
									.withParameters(parameters));
				}
				requests.withStatements(statements);
			}
			
			return requests;
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		
		return null;
	}
	
    private BatchExecuteStatementResult batchExecuteStatementRequest(AmazonDynamoDB client, Sequence seq ) {
    	try{
			BatchExecuteStatementRequest requests = new BatchExecuteStatementRequest();
			List<BatchStatementRequest> statements = new ArrayList<BatchStatementRequest>();
			for(int i=1; i<=seq.length(); i++){
				String s = seq.get(i).toString();
				statements.add(new BatchStatementRequest().withStatement(s));
			}
	        requests.withStatements(statements);       
	        return client.batchExecuteStatement(requests);
    	}catch(Exception e){
    		Logger.error(e.getMessage());
    	}
    	return null;
    }
}
