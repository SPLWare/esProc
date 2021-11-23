package com.scudata.expression.fn.string;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * replace(src,a,b) 将字符串src的子字符串a变为字符串b
 * @author runqian
 *
 */
public class Replace extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.missingParam"));
		}
		
		if (param.getSubSize() != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		IParam sub3 = param.getSub(2);
		if (sub1 == null || sub2 == null || sub3 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.invalidParam"));
		}
		
		Object str1 = sub1.getLeafExpression().calculate(ctx);
		if (str1 == null) {
			return null;
		} else if (!(str1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
		}

		Object str2 = sub2.getLeafExpression().calculate(ctx);
		if (str2 == null) {
			return null;
		} else if (!(str2 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
		}

		Object str3 = sub3.getLeafExpression().calculate(ctx);
		if (str3 == null) {
			return null;
		} else if (!(str3 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("replace" + mm.getMessage("function.paramTypeError"));
		}
		
		int flag = Sentence.IGNORE_PARS;
		if (option != null) {
			if (option.indexOf('q') == -1) {
				//引号里面的字符也需要变换
				flag += Sentence.IGNORE_QUOTE;
			}
			
			if (option.indexOf('1') != -1) {
				// 只替换第一个
				flag += Sentence.ONLY_FIRST;
			}
			
			if (option.indexOf('c') != -1) {
				// 忽略大小写
				flag += Sentence.IGNORE_CASE;
			}
		} else {
			flag += Sentence.IGNORE_QUOTE;
		}
		
		return Sentence.replace((String)str1, 0, (String)str2, (String)str3, flag);
	}
}
