package com.raqsoft.lib.informix.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.informix.helper.IfxConn;
import com.raqsoft.resources.EngineMessage;

public class ImFunction extends Function {
	protected int m_paramSize = 0;	//参数个数
	protected IfxConn m_ifxConn = null;
	protected String m_colNames[];
	protected String m_paramTypes[]; //除去句柄参数类型
	
	protected Context m_ctx;	
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		m_ctx = ctx;
		return this;
	}
	
	protected void setParamSize(int paramSize){
		m_paramSize = paramSize;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("informix " + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size==0){
			Object o = param.getLeafExpression().calculate(ctx);
			if ((o instanceof IfxConn)) {
				m_ifxConn = (IfxConn)o;
				return doQuery(null);
			}else{
				MessageManager mm = EngineMessage.get();
				throw new RQException("informix" + mm.getMessage("function.paramTypeError"));
			}			
		}
		
		Object cli = new Object();
		Object objs[] = new Object[size-1];
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("informix" + mm.getMessage("function.invalidParam"));
			}
			
			if (i==0){
				cli = param.getSub(i).getLeafExpression().calculate(ctx);
				if ((cli instanceof IfxConn)) {
					m_ifxConn = (IfxConn)cli;					
				}else{
					MessageManager mm = EngineMessage.get();
					throw new RQException("informix" + mm.getMessage("function.paramTypeError"));				
				}
			}else{
				objs[i-1] = param.getSub(i).getLeafExpression().calculate(ctx);
			}
		}

		if (m_ifxConn == null ){
			throw new RQException("read is null");
		}
		if (objs.length<1){
			throw new RQException("informix param is empty");
		}

		return doQuery(objs);
	}
	
	public Object doQuery( Object[] objs){
		
		return null;
	}
	 
}