package com.scudata.expression.mfn.sequence;

import com.scudata.dm.Context;
import com.scudata.expression.SequenceFunction;

/**
 * 使用序表或排列第一条记录的数据结构产生空序表返回
 * T.create() P.create()
 * @author RunQian
 *
 */
public class Create extends SequenceFunction {
	public Object calculate(Context ctx) {
		return srcSequence.create();
	}
}
