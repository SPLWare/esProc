package com.raqsoft.expression.fn.math;

import com.raqsoft.expression.Function;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;


/**
 * 求参数的十六进制串
 * @author yanjing
 *
 */
public class Hex extends Function {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hex" +
								  mm.getMessage("function.missingParam"));
		}
		String result1="";
		Object result = param.getLeafExpression().calculate(ctx);
		if (result != null && result instanceof Number) {
			result1=Long.toHexString(((Number)result).longValue());
		}
		
		int l=result1.length();
		char[] charArray=result1.toCharArray();
		Sequence seq=new Sequence(l);
		if(option==null || option.indexOf('s')<0){
			for(int i=0;i<l;i++){
				Integer tmp=Integer.valueOf(String.valueOf(charArray[i]),16);
				seq.add(tmp);
			}
		}else if(option.indexOf('s')>=0){
			for(int i=0;i<l;i++){
				seq.add(String.valueOf(charArray[i]));
			}
		}
		return seq;
	}
}
