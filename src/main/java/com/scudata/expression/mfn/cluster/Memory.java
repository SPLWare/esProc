package com.scudata.expression.mfn.cluster;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.ClusterPhyTableFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 把集群组表读成集群内表
 * T.memory(C,…;w)
 * @author RunQian
 *
 */
public class Memory extends ClusterPhyTableFunction {
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
		
		return table.memory(fields, filter, option, ctx);
	}
}
