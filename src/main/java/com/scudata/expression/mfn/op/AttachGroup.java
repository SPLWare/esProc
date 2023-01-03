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
 * 对游标或管道附加有序分组运算
 * op.group(x,…) op.group(x:F,…;y:G,…) op是游标或管道
 * @author RunQian
 *
 */
public class AttachGroup extends OperableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("group" + mm.getMessage("function.missingParam"));
		}

		if (option != null && option.indexOf('q') != -1) {
			if (param.getType() != IParam.Semicolon) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}
			
			int size = param.getSubSize();
			if (size == 2) {
				IParam sub0 = param.getSub(0);
				IParam sub1 = param.getSub(1);
				if (sub0 == null || sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group" + mm.getMessage("function.invalidParam"));
				}
				
				Expression []exps = sub0.toArray("group", false);
				Expression []sortExps = sub1.toArray("group", false);
				return operable.group(this, exps, sortExps, option, ctx);
			} else if (size == 3) {
				IParam sub0 = param.getSub(0);
				IParam sub1 = param.getSub(1);
				IParam sub2 = param.getSub(2);
				if (sub0 == null || sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group" + mm.getMessage("function.invalidParam"));
				}

				ParamInfo2 pi0 = ParamInfo2.parse(sub0, "group", true, false);
				Expression []exps = pi0.getExpressions1();
				String []names = pi0.getExpressionStrs2();
				
				ParamInfo2 pi1 = ParamInfo2.parse(sub1, "group", true, false);
				Expression []sortExps = pi1.getExpressions1();
				String []sortNames = pi1.getExpressionStrs2();
				
				Expression []newExps = null;
				String []newNames = null;
				if (sub2 != null) {
					ParamInfo2 pi2 = ParamInfo2.parse(sub2, "group", true, false);
					newExps = pi2.getExpressions1();
					newNames = pi2.getExpressionStrs2();
				}
				
				return operable.group(this, exps, names, sortExps, sortNames, newExps, newNames, option, ctx);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			Expression []exps = new Expression[] {exp};
			return operable.group(this, exps, option, ctx);
		} else if (param.getType() == IParam.Comma) {
			if (option != null && option.indexOf('i') != -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}
			
			int size = param.getSubSize();
			Expression []exps = new Expression[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group" + mm.getMessage("function.invalidParam"));
				}
				exps[i] = sub.getLeafExpression();
			}
			
			return operable.group(this, exps, option, ctx);
		} else if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}

			ParamInfo2 pi0 = ParamInfo2.parse(sub0, "group", true, false);
			Expression []exps = pi0.getExpressions1();
			String []names = pi0.getExpressionStrs2();
			
			Expression []newExps = null;
			String []newNames = null;
			IParam sub1 = param.getSub(1);
			if (sub1 != null) {
				ParamInfo2 pi1 = ParamInfo2.parse(sub1, "group", true, false);
				newExps = pi1.getExpressions1();
				newNames = pi1.getExpressionStrs2();
			}
			
			if (option != null && option.indexOf('i') != -1 && exps.length != 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}

			return operable.group(this, exps, names, newExps, newNames, option, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("group" + mm.getMessage("function.invalidParam"));
		}
	}
}
