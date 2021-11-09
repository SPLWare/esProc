package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.SequenceFunction;

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
