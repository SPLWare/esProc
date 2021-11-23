package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 按所指定的底数，返回一个数的对数。log(number,base), 若base省略则为10
 * @author yanjing
 *
 */
public class Loga extends Function {
	public Object calculate(Context ctx) {
		Object result1=null;
		Object result2=null;
		MessageManager mm = EngineMessage.get();
		if (param==null) {
			throw new RQException("lg" +
								  mm.getMessage("function.missingParam"));
		}else if(param.isLeaf()){
			result1 = param.getLeafExpression().calculate(ctx);
			if (result1 == null) {
				throw new RQException("lg" + mm.getMessage("function.invalidParam"));
			}
		}else{
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null) {
				throw new RQException("lg" + mm.getMessage("function.invalidParam"));
			}
			result1 = sub1.getLeafExpression().calculate(ctx);
			if (result1==null || !(result1 instanceof Number)) {
				throw new RQException("The first param of lg" + mm.getMessage("function.paramTypeError"));
			}
			if(sub2!=null){
				result2 = sub2.getLeafExpression().calculate(ctx);
				if (result2 != null && !(result2 instanceof Number)) {
					throw new RQException("The second param of lg" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		double n=Variant.doubleValue(result1);
		double b=10;
		if (result2 != null) {
			b=Variant.doubleValue(result2);
		}
		return new Double(Math.log(n)/Math.log(b));
	}

}
