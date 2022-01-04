package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;


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
