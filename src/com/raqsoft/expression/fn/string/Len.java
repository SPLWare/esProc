package com.raqsoft.expression.fn.string;

import java.io.UnsupportedEncodingException;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * len(s,cs) 计算字符串s的长度，字符集cs缺省为GB2312，省略cs时，计算s的unicode长度
 * @author runqian
 *
 */
public class Len extends Function {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("len" + mm.getMessage("function.missingParam"));
		}
		
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				String str = (String)obj;
				return str.length();
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("len" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("len" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				String str = (String)obj;
				String cs = "GB2312";
				IParam sub1 = param.getSub(1);
				if (sub1 != null) {
					obj = sub1.getLeafExpression().calculate(ctx);
					if (obj instanceof String) {
						cs = (String)obj;
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("len" + mm.getMessage("function.paramTypeError"));
					}
				}
				
				try {
					return str.getBytes(cs).length;
				} catch (UnsupportedEncodingException e) {
					throw new RQException(e.getMessage(), e);
				}
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("len" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("len" + mm.getMessage("function.invalidParam"));
		}
	}
}
