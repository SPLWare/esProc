package com.scudata.expression.mfn.record;

import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.RecordFunction;

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
