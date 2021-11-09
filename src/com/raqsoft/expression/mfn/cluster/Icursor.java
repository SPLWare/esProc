package com.raqsoft.expression.mfn.cluster;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.ClusterTableMetaDataFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 利用集群组表的索引对组表进行过滤，返回集群游标
 * T.icursor(C,…;w,I)
 * @author RunQian
 *
 */
public class Icursor extends ClusterTableMetaDataFunction {
	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("icursor" + mm.getMessage("function.missingParam"));
		}
		
		String []fields = null;
		Expression w = null;
		String I = null;
		
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}

			IParam sub = param.getSub(0);
			if (sub != null) {
				if (sub.isLeaf()) {
					fields = new String[]{sub.getLeafExpression().getIdentifierName()};
				} else {
					int fcount = sub.getSubSize();
					fields = new String[fcount];
					for (int f = 0; f < fcount; ++f) {
						IParam p = sub.getSub(f);
						if (p == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
						}
						
						fields[f] = p.getLeafExpression().getIdentifierName();
					}
				}
			}
			
			param = param.getSub(1);
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
		}
		
		IParam sub0;
		if (param.isLeaf()) {
			sub0 = param;
			I = null;
		} else {
			sub0 = param.getSub(0);
			I = (String) param.getSub(1).getLeafExpression().getIdentifierName();
		}
		
		if (sub0 != null) {
			w = sub0.getLeafExpression();
		}
		
		if (w == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
		}
		
		return table.icursor(fields, w, I, option, ctx);
	}
}
