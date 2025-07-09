package com.scudata.expression.mfn.op;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.OperableFunction;
import com.scudata.expression.ParamInfo2;
import com.scudata.resources.EngineMessage;

/**
 * 对游标或管道附加添加字段生成新序表运算
 * op.derive(xi:Fi,…) op.derive@x(xi:Fi,…;n) op是游标或管道
 * @author RunQian
 *
 */
public class AttachDerive extends OperableFunction {
	public Object calculate(Context ctx) {
		int level = 0;
		if (param != null && param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
			}
			
			Object val = sub.getLeafExpression().calculate(ctx);
			if (!(val instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.paramTypeError"));
			}
			
			level = ((Number)val).intValue();
			if (level < 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
			}
			
			param = param.getSub(0);
		} else if (option != null && option.indexOf('x') != -1) {
			level = 2;
		}

		//if (param == null && level < 2) {
		//	MessageManager mm = EngineMessage.get();
		//	throw new RQException("derive" + mm.getMessage("function.missingParam"));
		//}
		
		Expression []exps = null;
		String []names = null;
		if (param != null) {
			ParamInfo2 pi = ParamInfo2.parse(param, "derive", false, false);
			exps = pi.getExpressions1();
			names = pi.getExpressionStrs2();
		}
				
		//Derive derive = new Derive(this, exps, names, option, level);
		//if (cs != null) {
		//	derive.setCurrentCell(cs.getCurrent());
		//}
		
		//return operable.addOperation(derive, ctx);
		return operable.derive(this, exps, names, option, level, ctx);
	}
}
