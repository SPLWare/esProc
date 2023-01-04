package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;


/**
 * 求参数的十六进制串
 * @author yanjing
 *
 */
public class Hex extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hex" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hex" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
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
