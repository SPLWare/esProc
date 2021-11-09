package com.raqsoft.expression.mfn.record;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.RecordFunction;

/**
 * 使用记录的数据结构产生空序表返回
 * r.create()
 * @author RunQian
 *
 */
public class Create extends RecordFunction {
	public Object calculate(Context ctx) {
		Table table = new Table(srcRecord.dataStruct());
		return table;
	}
}
