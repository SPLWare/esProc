package com.raqsoft.expression.mfn.cluster;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.ClusterTableMetaDataFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 把集群组表读成集群内表
 * T.memory(C,…;w)
 * @author RunQian
 *
 */
public class Memory extends ClusterTableMetaDataFunction {
	public Object calculate(Context ctx) {
		String []fields = null;
		Expression filter = null;
		IParam fieldParam;
		
		if (param != null && param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("memory" + mm.getMessage("function.invalidParam"));
			}
			
			fieldParam = param.getSub(0);
			IParam sub = param.getSub(1);
			if (sub != null) {
				filter = sub.getLeafExpression();
			}
		} else {
			fieldParam = param;
		}
		
		if (fieldParam == null) {
		} else if (fieldParam.isLeaf()) {
			fields = new String[]{fieldParam.getLeafExpression().getIdentifierName()};
		} else {
			int size = fieldParam.getSubSize();
			fields = new String[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = fieldParam.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
				}
				
				fields[i] = sub.getLeafExpression().getIdentifierName();
			}
		}
		
		return table.memory(fields, filter, ctx);
	}
}
