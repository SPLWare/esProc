package com.scudata.expression.mfn.table;

import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.TableFunction;

/**
 * 把序表读成内表
 * T.memory()
 * @author LW
 *
 */
public class Memory extends TableFunction {
	public Object calculate(Context ctx) {
		Table srcTable = this.srcTable;
		return srcTable.memory(option);
	}
}
