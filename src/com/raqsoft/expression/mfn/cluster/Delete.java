package com.raqsoft.expression.mfn.cluster;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.ClusterTableMetaDataFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 把指定数据从集群组表中删除，组表需有主键
 * T.delete(P)
 * @author RunQian
 *
 */
public class Delete extends ClusterTableMetaDataFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("delete" + mm.getMessage("function.missingParam"));
		}
		
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			return table.delete((Sequence)obj, option);
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("delete" + mm.getMessage("function.paramTypeError"));
		} else {
			return null;
		}
	}
}
