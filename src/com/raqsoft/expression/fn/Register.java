package com.raqsoft.expression.fn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.FunctionLib;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/**
 * 将dfx文件登记为函数
 * register(f,dfx) 
 * 登记dfx文件为函数f，之后该函数便可以在网格脚本中使用，函数表达式写法为：f(xi,...)，其中xi,...为dfx文件中的参数，
 * 多个参数间用逗号分隔。
 * @author runqian
 *
 */
public class Register extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("register" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object name = param.getLeafExpression().calculate(ctx);
			if (!(name instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("register" + mm.getMessage("function.paramTypeError"));
			}
			
			FunctionLib.removeDFXFunction((String)name);
			return name;
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("register" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("register" + mm.getMessage("function.invalidParam"));
		}
		
		Object name = sub0.getLeafExpression().calculate(ctx);
		if (!(name instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("register" + mm.getMessage("function.paramTypeError"));
		}
		
		Object dfx = sub1.getLeafExpression().calculate(ctx);
		if (!(dfx instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("register" + mm.getMessage("function.paramTypeError"));
		}
		
		FunctionLib.addDFXFunction((String)name, (String)dfx);
		return name;
	}
}
