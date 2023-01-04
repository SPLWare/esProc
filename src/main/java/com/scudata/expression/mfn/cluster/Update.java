package com.scudata.expression.mfn.cluster;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.ClusterPhyTableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 更新集群组表数据，新键值则插入，保持键有序，组表需有主键
 * T.update(P)
 * @author RunQian
 *
 */
public class Update extends ClusterPhyTableFunction {
	public Object calculate(Context ctx) {
		String opt = option;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.missingParam"));
		}
		
		boolean hasN = opt != null && opt.indexOf('n') != -1;
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			if (!hasN) {
				table.update((Sequence)obj, opt);
				return table;
			}
			return table.update((Sequence)obj, opt);
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
