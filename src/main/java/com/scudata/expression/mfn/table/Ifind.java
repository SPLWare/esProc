package com.scudata.expression.mfn.table;

import java.util.ArrayList;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dw.MemoryTable;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.TableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 利用索引对内表进行过滤
 * T.ifind(k,…;I)
 * @author LW
 *
 */
public class Ifind extends TableFunction {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifind" + mm.getMessage("function.missingParam"));
		}
		
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ifind" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(0);
			if (sub == null) return null;
			ArrayList<Expression> list = new ArrayList<Expression>();
			sub.getAllLeafExpression(list);
			Sequence seq = new Sequence();
			for (Expression exp : list) {
				seq.add(exp.calculate(ctx));
			}
			
			String iname = param.getSub(1).getLeafExpression().getIdentifierName();
			return ((MemoryTable)srcTable).ifind(seq, iname, null, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifind" + mm.getMessage("function.missingParam"));
		}
	}
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof MemoryTable;
	}
}
