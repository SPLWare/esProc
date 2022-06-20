package com.scudata.expression.mfn.pseudo;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.IColumnCursorUtil;
import com.scudata.dw.MemoryTable;
import com.scudata.expression.PseudoFunction;
import com.scudata.parallel.ClusterCursor;

/**
 * 把虚表读成内表
 * T.memory(C,…;w)
 * @author RunQian
 *
 */
public class Memory extends PseudoFunction {
	public Object calculate(Context ctx) {
		//列式内表
		if (option != null && option.indexOf('v') != -1 && IColumnCursorUtil.util != null) {
			String opt = option;
			opt = opt.replace("v", "");
			opt += 'm';
			
			ICursor cursor = CreateCursor.createCursor("memory", pseudo, param, opt, ctx);
			return IColumnCursorUtil.util.createMemoryTable(cursor, null, option);
		}
		
		ICursor cursor = CreateCursor.createCursor("memory", pseudo, param, null, ctx);
		if (cursor instanceof ClusterCursor) {
			return ((ClusterCursor)cursor).memory(null, ctx);
		}
		Sequence seq = cursor.fetch();
		
		Table table;
		if (seq instanceof Table) {
			table = (Table)seq;
		} else if (seq == null) {
			return null;
		} else {
			table = seq.derive("o");
		}
		
		MemoryTable result = new MemoryTable(table);		
		return result;
	}
}
