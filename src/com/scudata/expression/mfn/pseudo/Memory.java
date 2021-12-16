package com.scudata.expression.mfn.pseudo;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.MemoryTable;
import com.scudata.expression.PseudoFunction;

/**
 * 把虚表读成内表
 * T.memory(C,…;w)
 * @author RunQian
 *
 */
public class Memory extends PseudoFunction {
	public Object calculate(Context ctx) {
		ICursor cursor = CreateCursor.createCursor("memory", pseudo, param, ctx);
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
