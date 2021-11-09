package com.raqsoft.expression.fn.math;

import com.raqsoft.expression.Function;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;


/**
 * 把参数变成二进制序列
 * @author yanjing
 *
 */
public class Bin extends Function {

	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("bin" + mm.getMessage("function.invalidParam"));
		}

		Object result = param.getLeafExpression().calculate(ctx);
		String result1="";
		
		if (result != null && result instanceof Number) {
			result1=Long.toBinaryString(((Number)result).longValue());
		}
		
		int l=result1.length();
		char[] charArray=result1.toCharArray();
		Sequence seq=new Sequence(l);
		for(int i=0;i<l;i++){
			Integer tmp=Integer.valueOf(String.valueOf(charArray[i]));
			seq.add(tmp);
		}
		return seq;
	}
}
