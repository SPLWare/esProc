package com.scudata.lib.dynamodb.function;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementResult;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.MemberFunction;
import com.scudata.expression.Node;

public class ImFunction extends MemberFunction {
	protected Context m_ctx;
	protected ImOpen  m_db;
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		m_ctx = ctx;
		return this;
	}

	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof ImOpen;
	}
	
	public void setDotLeftObject(Object obj) {
		m_db = (ImOpen)obj;
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			return doExec(null);
		}

		Object[] objs = null;
		if (param.isLeaf()) {
			objs = new Object[1];
			objs[0] = param.getLeafExpression().calculate(ctx);
			return doExec(objs);
		}

		int size = param.getSubSize();
		objs = new Object[size];
		IParam subPM = null;
		ArrayList<Expression> ls = new ArrayList<Expression>();

		for (int i = 0; i < size; i++) {
			if (param.getSub(i) == null) {
				objs[i] = null;
				continue;
			}

			subPM = param.getSub(i);
			if (subPM.isLeaf()) {
				if (this instanceof ImExecute && i>=2){
					objs[i] = subPM.getLeafExpression();
				}else{
					objs[i] = subPM.getLeafExpression().calculate(ctx);
				}
			} else {
				ls.clear();
				subPM.getAllLeafExpression(ls);
				Object[] os = new Object[ls.size()];
				for(int j=0; j<ls.size(); j++){
					if (ls.get(j)!=null){
						os[j] = ls.get(j).calculate(ctx);
					}else{
						os[j] = null;
					}
				}
				objs[i] = os;
			}
		}

		return doExec(objs);
	}

	protected Object doExec(Object[] objs) {
		if (this instanceof ImQuery){
			return doExecute(objs, "dyna_query");
		}else if (this instanceof ImExecute){
			return doExecEx(objs, "dyna_execute");
		}else{
			return null;
		}		
	}
	
	protected ExecuteStatementResult doExecute(Object[] objs, String info){
		ExecuteStatementResult result = null;
		try {
			if (objs.length<1){
				throw new RQException(info+" missingParam");
			}
			
			String sql = null;
			//check type of params
			if (objs[0] instanceof String){
				sql = objs[0].toString();
			}else{
				throw new RQException(info+" paramTypeError");
			}
			
			List<AttributeValue> parameters = null;
			if (objs.length>1){
				parameters = new ArrayList<AttributeValue>();
				for (int n=1; n<objs.length; n++){
					AttributeValue a = new AttributeValue();
					if (objs[n] instanceof String){
						a.setS(objs[n].toString());
					}else if(objs[n] instanceof Integer){
						a.setN(objs[n].toString());
					}
				    parameters.add(a);
				}
			}
			
			result = ImUtils.executeStatementRequest(m_db.m_client, sql, parameters);				
		}catch(Exception e) {
			Logger.error(e.getMessage());
		}
		
		return result;
	}
	
	protected Object doExecEx(Object[] objs, String info){
		boolean bRet = false;
		try {
			ExecuteStatementResult result = doExecute(objs, info);
			if (result!=null){
				bRet = (result.getSdkHttpMetadata().getHttpStatusCode()==200);
			}
		}catch(Exception e) {
			Logger.error(e.getMessage());
		}
		
		return bRet;
	}
}
