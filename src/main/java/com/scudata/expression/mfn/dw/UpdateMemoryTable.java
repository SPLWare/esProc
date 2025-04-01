package com.scudata.expression.mfn.dw;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.MemoryTableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 更新内表数据，新键值则插入，保持键有序
 * T.update(P)
 * @author RunQian
 *
 */
public class UpdateMemoryTable extends MemoryTableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.missingParam"));
		}
		
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			return table.update((Sequence)obj, option);
		} else if (obj == null) {
			if (option == null || option.indexOf('n') == -1) {
				return table;
			} else {
				return null;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.paramTypeError"));
		}
	}
}
