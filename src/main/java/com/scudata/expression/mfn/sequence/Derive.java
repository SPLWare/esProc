package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 为排列添加字段生成新序表
 * A.derive(xi:Fi,…) A.derive@x(xi:Fi,…;n)
 * @author RunQian
 *
 */
public class Derive extends SequenceFunction {
	public Object calculate(Context ctx) {
		IParam param = this.param;
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
		
		if (param == null && level < 2) {
			return srcSequence.derive(option);
		}
		
		Expression []exps = null;
		String []names = null;
		if (param != null) {
			ParamInfo2 pi = ParamInfo2.parse(param, "derive", false, false);
			exps = pi.getExpressions1();
			names = pi.getExpressionStrs2();
		}

		if (level < 2) {
			return srcSequence.derive(names, exps, option, ctx);
		} else {
			return srcSequence.derive(names, exps, option, ctx, level);
		}
	}
}