package com.raqsoft.lib.olap4j.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class ImFunction extends Function {
	protected int m_paramSize = 0;	//参数个数
	protected MdxQueryUtil m_mdx = null;
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
			throw new RQException("olap" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();

		Object cli = new Object();
		Object objs[] = new Object[size-1];
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("olap_client" + mm.getMessage("function.invalidParam"));
			}
			
			if (i==0){
				cli = param.getSub(i).getLeafExpression().calculate(ctx);
				if (!(cli instanceof MdxQueryUtil)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("olap_client" + mm.getMessage("function.paramTypeError"));
				}else{
					m_mdx = (MdxQueryUtil)cli;
				}
			}else{
				objs[i-1] = param.getSub(i).getLeafExpression().calculate(ctx);
				if (objs[i-1]!=null && objs[i-1] instanceof String){
					//skip;
				}else{
					throw new RQException("olap param " + objs[i-1] + " type is not String");
				}				
			}
		}

		if (m_mdx == null){
			throw new RQException("mdx is null");
		}
		if (objs.length<1){
			throw new RQException("olap_param is empty");
		}

		return doQuery(objs);
	}
	
	public Object doQuery( Object[] objs){
		
		return null;
	}
	 
}