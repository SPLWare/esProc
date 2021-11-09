package com.raqsoft.expression.fn;

import com.raqsoft.cellset.ICellSet;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.ParamParser;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 从左到右计算表达式，如果有xi的返回值和x的相等则计算yi并返回
 * 如果没有表达式满足条件，则返回缺省值，没有缺省值则返回空
 * case(x,x1:y1,…,xk:yk;y)
 * @author RunQian
 *
 */
public class Case extends Function {
	public void setParameter(ICellSet cs, Context ctx, String param) {
		strParam = param;
		this.cs = cs;
		this.param = ParamParser.parse(param, cs, ctx, false, false);
	}

	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_UNKNOWN;
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("case" + mm.getMessage("function.missingParam"));
		}
		
		IParam defaultParam = null;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("case" + mm.getMessage("function.invalidParam"));
			}
			
			defaultParam = param.getSub(1);
			param = param.getSub(0);
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("case" + mm.getMessage("function.invalidParam"));
			}
		}
		
		if (param.getType() != IParam.Comma) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("case" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub = param.getSub(0);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("case" + mm.getMessage("function.invalidParam"));
		}

		Object val = sub.getLeafExpression().calculate(ctx);
		for (int i = 1, size = param.getSubSize(); i < size; ++i) {
			sub = param.getSub(i);
			if (sub.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("case" + mm.getMessage("function.invalidParam"));
			}
			
			IParam p = sub.getSub(0);
			Object condition = (p == null ? null : p.getLeafExpression().calculate(ctx));
			if (Variant.isEquals(val, condition)) {
				p = sub.getSub(1);
				return (p == null ? null : p.getLeafExpression().calculate(ctx));
			}
		}

		if (defaultParam != null) {
			return defaultParam.getLeafExpression().calculate(ctx);
		} else {
			return null;
		}
	}
}
