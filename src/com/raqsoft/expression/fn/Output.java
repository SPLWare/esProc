package com.raqsoft.expression.fn;

import java.sql.Timestamp;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 向控制台输出打印信息
 * output@g@t(x,…) 可输出一个或多个变量信息，多个时用逗号隔开，控制台输出的打印信息以Tab分隔。
 * @author runqian
 *
 */
public class Output extends Function {

	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("output" + mm.getMessage("function.invalidParam"));
		}

		boolean isInfo = false, isError = false, isTime = false, isLF = true;
		if (option != null) {
			if (option.indexOf('t') != -1) isTime = true;
			if (option.indexOf('g') != -1) isInfo = true;
			if (option.indexOf('e') != -1) isError = true;
			if (option.indexOf('s') != -1) isLF = false;
		}
		
		Expression []exps = getParamExpressions("output", false);
		String msg = null;
		for (int i = 0, size = exps.length; i < size; ++i) {
			Object obj = exps[i].calculate(ctx);
			if (i == 0) {
				msg = Variant.toString(obj);
			} else {
				msg += '\t' + Variant.toString(obj);
			}
		}
		
		if (isError) {
			Logger.error(msg);
		} else if (isInfo) {
			Logger.info(msg);
		} else {
			if (isTime) {
				Timestamp time = new Timestamp(System.currentTimeMillis());
				msg = Variant.toString(time) + '\t' + msg;
			}
	
			if (isLF) {
				System.out.println(msg);
			} else {
				System.out.print(msg);
			}
		}
		
		return null;
	}
}
