package com.scudata.expression.mfn.dw;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.MemoryTable;
import com.scudata.dw.TableMetaData;
import com.scudata.expression.TableMetaDataFunction;

/**
 * 把组表读成内表
 * T.memory(C,…;w)
 * @author RunQian
 *
 */
public class Memory extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		TableMetaData tmd = (TableMetaData) table;
		ICursor cursor = CreateCursor.createCursor(tmd, param, option, ctx);
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
}
