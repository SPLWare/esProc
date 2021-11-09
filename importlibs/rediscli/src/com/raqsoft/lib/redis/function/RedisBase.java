package com.raqsoft.lib.redis.function;

import java.util.List;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class RedisBase extends Function {
	protected int m_paramSize = 0;	//参数个数
	protected RedisTool m_jedisTool = null;
	protected String m_colNames[];
	protected String m_paramTypes[]; //除去redis句柄参数类型
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}
	
	protected void setParamSize(int paramSize){
		m_paramSize = paramSize;
	}

	public Object calculate(Context ctx) {
		Object cli = null;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("redis " + mm.getMessage("function.missingParam"));
		}else if(param.isLeaf()){
			cli = param.getLeafExpression().calculate(ctx);
			if (!(cli instanceof RedisTool)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("redis " + mm.getMessage("function.paramTypeError"));
			}else{
				m_jedisTool = (RedisTool)cli;
				return doQuery(null);
			}
		}

		int size = param.getSubSize();
		
		Object objs[] = new Object[size-1];
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("redis " + mm.getMessage("function.invalidParam"));
			}
			
			if (i==0){
				cli = param.getSub(i).getLeafExpression().calculate(ctx);
				if (!(cli instanceof RedisTool)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("redis " + mm.getMessage("function.paramTypeError"));
				}else{
					m_jedisTool = (RedisTool)cli;
				}
			}else{
				if (m_paramTypes.length>i-1){
					if ((objs[i-1] = Utils.checkValidDataType(param.getSub(i), ctx, m_paramTypes[i-1]))==null){
						throw new RQException("redis  param " + objs[i-1] + " type is not " + m_paramTypes[i-1]);
					}
				}else{
					objs[i-1] = param.getSub(i).getLeafExpression().calculate(ctx);
				}
			}
		}
		if (objs.length<m_paramTypes.length){
			throw new RQException("redis param error!");
		}
		if (m_jedisTool == null){
			throw new RQException("redis_connect is null");
		}
		if (objs.length<1){
			throw new RQException("redis_param is empty");
		}
		
		return doQuery(objs);//new RedisDriverCli((String)objs[0], Integer.parseInt((String)objs[1]),(String)objs[2]);
	}
	
	public Object doQuery( Object[] objs){
		m_colNames = new String[objs.length];
		if (objs.length == 1){
			m_colNames[0] = objs[0].toString();
		}else{
			m_colNames = Utils.objectArrayToStringArray(objs);		
		}
		
		return null;
	}
	
	protected Table toTable(Object[] row){
		if (row == null) return null;

		Table table = new Table(m_colNames);
		
//		for(Object o:row){
//			System.out.println("redisBase toTable = " + o);
//		}
		table.newLast(row);
		
		return table;
	}
	
	protected Table toTable(List<Object[]> rows){
		if (rows == null) return null;

		Table table = new Table(m_colNames);
		
		for(Object[] row:rows){
			table.newLast(row);
		}
		
		return table;
	}
}
