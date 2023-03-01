package com.scudata.expression.mfn.table;

import java.util.ArrayList;
import java.util.List;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
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
			
			Object key;
			int size = list.size();
			if (size == 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ifind" + mm.getMessage("function.invalidParam"));
			} else if (size == 1) {
				key = list.get(0).calculate(ctx);
			} else {
				Object[] keys = new Object[size];
				for (int i = 0; i < size; i++) {
					keys[i] = list.get(i).calculate(ctx);
				}
				key = keys;
			}
			
			String iname = param.getSub(1).getLeafExpression().getIdentifierName();
			return ((MemoryTable)srcTable).ifind(key, iname, option, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifind" + mm.getMessage("function.missingParam"));
		}
	}
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof MemoryTable;
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() == 2) {
				IParam sub = param.getSub(0);
				sub.getUsedFields(ctx, resultList);
			}
		}
	}
}
