package com.scudata.expression.mfn.table;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.IParam;
import com.scudata.expression.TableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 设置序表的主键
 * T.keys(Ki,…)
 * @author RunQian
 *
 */
public class Keys extends TableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			srcTable.setPrimary(null);
		} else if (param.getType() == IParam.Semicolon) {
			IParam sub1 = param.getSub(1);
			if (sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("keys" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub1.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("keys" + mm.getMessage("function.paramTypeError"));
			}

			int capacity = ((Number)obj).intValue();
			if (capacity < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("keys" + mm.getMessage("function.invalidParam"));
			}
			
			param = param.getSub(0);
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("keys" + mm.getMessage("function.invalidParam"));
			}
			
			String []cols;
			if (param.isLeaf()) {
				cols = new String[]{param.getLeafExpression().getIdentifierName()};
			} else {
				int size = param.getSubSize();
				cols = new String[size];
				for (int i = 0; i < size; ++i) {
					IParam sub = param.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("keys" + mm.getMessage("function.invalidParam"));
					}
					
					cols[i] = sub.getLeafExpression().getIdentifierName();
				}
			}
			
			srcTable.setPrimary(cols, option);
			srcTable.createIndexTable(capacity, option);
		} else {
			String []cols;
			if (param.isLeaf()) {
				cols = new String[]{param.getLeafExpression().getIdentifierName()};
			} else {
				int size = param.getSubSize();
				cols = new String[size];
				for (int i = 0; i < size; ++i) {
					IParam sub = param.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("keys" + mm.getMessage("function.invalidParam"));
					}
					
					cols[i] = sub.getLeafExpression().getIdentifierName();
				}
			}
			
			srcTable.setPrimary(cols, option);
			if (option != null && option.indexOf('i') != -1) {
				srcTable.createIndexTable(option);
			}
		}
		
		return srcTable;
	}
}
