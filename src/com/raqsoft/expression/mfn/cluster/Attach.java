package com.raqsoft.expression.mfn.cluster;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.ClusterTableMetaDataFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取集群组表的附表或者为集群组表增加附表
 * T.attach(T’,C…)
 * @author RunQian
 *
 */
public class Attach extends ClusterTableMetaDataFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("attach" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			String tableName = param.getLeafExpression().getIdentifierName();
			return table.getTableMetaData(tableName);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("attach" + mm.getMessage("function.invalidParam"));
		}
	}
}
