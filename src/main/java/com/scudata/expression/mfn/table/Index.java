package com.scudata.expression.mfn.table;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
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
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("index" + mm.getMessage("function.invalidParam"));
		}
		
		return srcTable;
	}
}
