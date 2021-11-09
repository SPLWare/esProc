package com.raqsoft.expression.mfn.op;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.op.Regex;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.OperableFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 对游标或管道附加模式匹配运算
 * op.regex(rs,F) op是游标或管道
 * @author RunQian
 *
 */
public class AttachRegex extends OperableFunction {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("regex" + mm.getMessage("function.missingParam"));
		}
		
		int flags = 0;
		if (option != null) {
			if (option.indexOf('c') != -1) flags |= Pattern.CASE_INSENSITIVE;
			if (option.indexOf('u') != -1) flags |= Pattern.UNICODE_CASE;
		}

		String strPattern;
		String []names = null;
		Expression exp = null;
		IParam sub0;
		IParam sub1 = null;
		
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex" + mm.getMessage("function.invalidParam"));
			}
			
			sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex" + mm.getMessage("function.missingParam"));
			}
			
			sub1 = param.getSub(1);
		} else {
			sub0 = param;
		}
		
		if (sub0.isLeaf()) {
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex" + mm.getMessage("function.paramTypeError"));
			}

			strPattern = (String)obj;
		} else {
			if (sub0.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = sub0.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex" + mm.getMessage("function.paramTypeError"));
			}

			strPattern = (String)obj;
			sub = sub0.getSub(1);
			if (sub != null) exp = sub.getLeafExpression();
		}

		if (sub1 == null) {
		} else if (sub1.isLeaf()) {
			names = new String[1];
			names[0] = sub1.getLeafExpression().getIdentifierName();
		} else {
			int size = sub1.getSubSize();
			names = new String[size];
			for (int f = 0; f < size; ++f) {
				IParam sub = sub1.getSub(f);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("regex" + mm.getMessage("function.invalidParam"));
				}

				names[f] = sub.getLeafExpression().getIdentifierName();
			}
		}

		Pattern pattern = Pattern.compile(strPattern, flags);
		Matcher m = pattern.matcher("");
		int fcount = m.groupCount();
		if (fcount > 0) {
			if (names == null) {
				names = new String[fcount];
			} else if (names.length != fcount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex: " + mm.getMessage("engine.dsNotMatch"));
			}
		} else {
			names = null;
		}
		
		Regex regex = new Regex(this, pattern, names, exp);
		return operable.addOperation(regex, ctx);
	}
}

