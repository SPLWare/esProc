package com.raqsoft.expression.mfn.dw;

import java.io.IOException;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.TableMetaDataFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 更新组表数据，新键值则插入，保持键有序，组表需有主键
 * T.update(P)
 * @author RunQian
 *
 */
public class Update extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		String opt = option;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.missingParam"));
		}
		boolean hasN = opt != null && opt.indexOf('n') != -1;
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			try {
				if (!hasN) {
					table.update((Sequence)obj, opt);
					return table;
				}
				return table.update((Sequence)obj, opt);
			} catch (IOException e) {
				throw new RQException(e);
			}
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.paramTypeError"));
		} else {
			if (!hasN) {
				return table;
			} else {
				return null;
			}
		}
	}
}
