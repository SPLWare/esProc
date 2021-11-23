package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 取字符串中指定位置字符的unicode值。
 * asc( string{, nPos} ) 取字符串string指定位置nPos的字符unicode值，如果是ascii字符则返回ascii码。
 * @author runqian
 *
 */
public class ToAsc extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("asc" + mm.getMessage("function.missingParam"));
		}
		
		String str;
		int pos = 0;
		if (param.isLeaf()) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("asc" + mm.getMessage("function.paramTypeError"));
			}
			
			str = (String)result1;
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("asc" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(0);
			if (sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("asc" + mm.getMessage("function.invalidParam"));
			}
			
			Object result1 = sub1.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("asc" + mm.getMessage("function.paramTypeError"));
			}
			
			str = (String)result1;
			IParam sub2 = param.getSub(1);
			if (sub2 != null) {
				Object result2 = sub2.getLeafExpression().calculate(ctx);
				if (!(result2 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("acs" + mm.getMessage("function.paramTypeError"));
				}
				
				pos = ((Number)result2).intValue() - 1;
			}
		}
		
		if (str.length() > pos && pos >= 0) {
			return new Integer(str.charAt(pos));
		} else {
			return null;
		}
	}
}
