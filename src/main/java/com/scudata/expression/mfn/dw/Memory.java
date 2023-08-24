package com.scudata.expression.mfn.dw;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.IPhyTable;
import com.scudata.dw.MemoryTable;
import com.scudata.dw.PhyTable;
import com.scudata.expression.PhyTableFunction;

/**
 * 把组表读成内表
 * T.memory(C,…;w)
 * @author RunQian
 *
 */
public class Memory extends PhyTableFunction {
	public Object calculate(Context ctx) {
		PhyTable tmd = (PhyTable) table;
		
		ICursor cursor = CreateCursor.createCursor(tmd, param, option, ctx);
		if (option != null && option.indexOf('x') != -1) {
			CreateCursor.setOptionX(cursor, option);
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
		Integer partition = tmd.getGroupTable().getPartition();
		
		if (partition != null) {
			String distribute = tmd.getDistribute();
			result.setDistribute(distribute);
			result.setPart(partition);
		}
		
		if (option != null && option.indexOf('p') != -1) {
			result.setSegmentField1();
		} else {
			String []sortedFieldNames = tmd.getAllSortedColNames();
			if (sortedFieldNames != null) {
				result.setSegmentFields(sortedFieldNames);
			}
		}
		
		return result;
	}
	
	public boolean isLeftTypeMatch(Object obj) {
		if (obj instanceof IPhyTable) {
			if (option != null && option.indexOf('v') != -1)
				return false;
			return true;
		}
		
		return false;
	}
}
