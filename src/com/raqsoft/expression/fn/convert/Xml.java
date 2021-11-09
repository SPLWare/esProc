package com.raqsoft.expression.fn.convert;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.XMLUtil;

/**
 * xml(x,s) 当x为xml串时，解析为序表返回；当x为序表时解析为xml串返回。
 * @author runqian
 *
 */
public class Xml extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xml" + mm.getMessage("function.missingParam"));
		}

		Object val;
		String s = null;
		if (param.isLeaf()) {
			val = param.getLeafExpression().calculate(ctx);
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xml" + mm.getMessage("function.invalidParam"));
			}
			
			val = sub0.getLeafExpression().calculate(ctx);
			Object obj = sub1.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				s = (String)obj;
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xml" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xml" + mm.getMessage("function.invalidParam"));
		}
		
		if (val instanceof String) {
			if (option == null || option.indexOf('s') == -1) {
				return XMLUtil.parseXml((String)val, s);
			} else {
				return XMLUtil.parseXmlString((String)val);
			}
		} else if (val instanceof Sequence) {
			return XMLUtil.toXml((Sequence)val, null, s);
		} else if (val instanceof Record) {
			return XMLUtil.toXml((Record)val, null, s);
		} else if (val == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xml" + mm.getMessage("function.paramTypeError"));
		}
	}
}
