package com.scudata.expression.mfn.table;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dw.MemoryTable;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.TableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 给序表按主键创建内存索引，返回源序表
 * T.index(n)
 * @author RunQian
 *
 */
public class Index extends TableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			srcTable.createIndexTable(option);
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("index" + mm.getMessage("function.paramTypeError"));
			}

			int capacity = ((Number)obj).intValue();
			if (capacity > 0) {
				srcTable.createIndexTable(capacity, option);
			} else {
				srcTable.deleteIndexTable();
			}
		}else if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if ((size < 2) || (size > 3)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("index" + mm.getMessage("function.paramTypeError"));
			}
			if (!(srcTable instanceof MemoryTable)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("index" + mm.getMessage("function.paramTypeError"));
			}
			
			MemoryTable table = (MemoryTable) srcTable;
			String[] fields = null;
			String[] valueFields = null;
			String I = null;
			Expression w = null;
			
			IParam sub0 = param.getSub(0);
			IParam fieldParam = param.getSub(1);
			IParam valueFieldParam = null;
			if (size == 3) {
				valueFieldParam = param.getSub(2);
			}
			
			IParam hParam = null;
			if (sub0.isLeaf()) {
				I = (String) sub0.getLeafExpression().getIdentifierName();
			} else if (sub0.getType() == IParam.Colon) {
				I = (String) sub0.getSub(0).getLeafExpression().getIdentifierName();
				hParam = sub0.getSub(1);
			} else if (sub0.getType() == IParam.Comma) {
				if (sub0.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("index" + mm.getMessage("function.paramTypeError"));
				}
				w = sub0.getSub(1).getLeafExpression();
				sub0 = sub0.getSub(0);
				if (sub0.getType() == IParam.Colon) {
					I = (String) sub0.getSub(0).getLeafExpression().getIdentifierName();
					hParam = sub0.getSub(1);
				} else {
					I = (String) sub0.getLeafExpression().getIdentifierName();
				}
			}

			if (fieldParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("index" + mm.getMessage("function.invalidParam"));
			} else if (fieldParam.isLeaf()) {
				fields = new String[]{fieldParam.getLeafExpression().getIdentifierName()};
			} else {
				int fcount = fieldParam.getSubSize();
				fields = new String[fcount];
				for (int i = 0; i < fcount; ++i) {
					IParam sub = fieldParam.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("index" + mm.getMessage("function.invalidParam"));
					}
					
					fields[i] = sub.getLeafExpression().getIdentifierName();
				}
			}
			
			if (valueFieldParam != null) {
				if (valueFieldParam.isLeaf()) {
					valueFields = new String[]{valueFieldParam.getLeafExpression().getIdentifierName()};
				} else {
					int fcount = valueFieldParam.getSubSize();
					valueFields = new String[fcount];
					for (int i = 0; i < fcount; ++i) {
						IParam sub = valueFieldParam.getSub(i);
						if (sub == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("index" + mm.getMessage("function.invalidParam"));
						}
						
						valueFields[i] = sub.getLeafExpression().getIdentifierName();
					}
				}
				
				//key-value-Index
				table.createIMemoryTableIndex(I, fields, valueFields, option, w, ctx);
				return srcTable;
			}
			
			if (hParam != null) {
				//hashIndex
				Integer h = (Integer) hParam.getLeafExpression().calculate(ctx);
				table.createIMemoryTableIndex(I, fields, h, option, w, ctx);
				return srcTable;
			}
			
			table.createIMemoryTableIndex(I, fields, null, option, w, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("index" + mm.getMessage("function.invalidParam"));
		}
		
		return srcTable;
	}
}
