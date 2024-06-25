package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 对序列的序列做行列转换A.pivot0(g;v) A.pivot0@r(g;v;k)
 * 
 * @author RunQian
 *
 */
public class Pivot0 extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pivot0" + mm.getMessage("function.missingParam"));
		} else if (param.getType() != IParam.Semicolon) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pivot0" + mm.getMessage("function.invalidParam"));
		}
	}

	private Object pivotSequence(Context ctx) {
		if (option == null || option.indexOf('r') == -1) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pivot0" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pivot0" + mm.getMessage("function.invalidParam"));
			}
			
			Expression gexp = sub.getLeafExpression();
			sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pivot0" + mm.getMessage("function.invalidParam"));
			}
			
			Expression vexp = sub.getLeafExpression();
			return srcSequence.pivot0(gexp, vexp, ctx);
		} else {
			if (param.getSubSize() != 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pivot0" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pivot0" + mm.getMessage("function.invalidParam"));
			}
			
			Expression gexp = sub.getLeafExpression();
			sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pivot0" + mm.getMessage("function.invalidParam"));
			}
			
			Expression vexp = sub.getLeafExpression();
			sub = param.getSub(2);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pivot0" + mm.getMessage("function.invalidParam"));
			}
			
			Object value = sub.getLeafExpression().calculate(ctx);
			if (!(value instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pivot0" + mm.getMessage("function.paramTypeError"));
			}
			
			int k = ((Number)value).intValue();
			return srcSequence.unpivot0(gexp, vexp, k, ctx);
		}
	}
	
	private Object pivotTable(Context ctx) {
		Expression[] gexps;
		String []gnames;
		Expression[] vexps = null;
		String []newNames = null;
		
		if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pivot0" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pivot0" + mm.getMessage("function.invalidParam"));
			}
			
			ParamInfo2 pi = ParamInfo2.parse(sub, "pivot0", true, false);
			gexps = pi.getExpressions1();
			gnames = pi.getExpressionStrs2();
			
			sub = param.getSub(1);
			if (sub != null) {
				vexps = sub.toArray("pivot0", false);
			}
			
			if (size > 2 && param.getSub(2) != null) {
				sub = param.getSub(2);
				newNames = sub.toStringArray("pivot0", false);
			}
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "pivot0", true, false);
			gexps = pi.getExpressions1();
			gnames = pi.getExpressionStrs2();
		}
		
		if (option == null || option.indexOf('r') == -1) {
			return srcSequence.pivot0(gexps, gnames, vexps, newNames, ctx);
		} else {
			return srcSequence.unpivot0(gexps, gnames, vexps, newNames, ctx);
		}
	}

	public Object calculate(Context ctx) {
		if (srcSequence.ifn() instanceof Sequence) {
			return pivotSequence(ctx);
		} else {
			return pivotTable(ctx);
		}
	}
}
