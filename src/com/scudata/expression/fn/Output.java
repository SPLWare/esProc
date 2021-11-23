package com.scudata.expression.fn;

import java.sql.Timestamp;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

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
