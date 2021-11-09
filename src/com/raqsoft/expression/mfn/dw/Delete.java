package com.raqsoft.expression.mfn.dw;

import java.io.IOException;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.TableMetaDataFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 把指定数据从组表中删除，组表需有主键
 * T.delete(P)
 * @author RunQian
 *
 */
public class Delete extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("delete" + mm.getMessage("function.missingParam"));
		}
		
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			try {
				return table.delete((Sequence)obj, option);
			} catch (IOException e) {
				throw new RQException(e);
			}
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("delete" + mm.getMessage("function.paramTypeError"));
		} else {
			return null;
		}
	}
}
