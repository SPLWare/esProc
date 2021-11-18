package com.raqsoft.expression.mfn.pseudo;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.PseudoFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 追加游标数据到组表中
 * T.append(cs)
 * @author RunQian
 *
 */
public class Append extends PseudoFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("append" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("append" + mm.getMessage("function.invalidParam"));
		}
		
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof ICursor) {
			pseudo.append((ICursor)obj, option);
		} else if (obj == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("append" + mm.getMessage("function.invalidParam"));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("append" + mm.getMessage("function.paramTypeError"));
		}
		
		return pseudo;
	}

}
