package com.raqsoft.expression.mfn.op;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.op.Rename;
import com.raqsoft.expression.OperableFunction;
import com.raqsoft.expression.ParamInfo2;

/**
 * 对游标或管道附加对字段进行重命名运算
 * op.rename(F:F',…) op是游标或管道
 * @author RunQian
 *
 */
public class AttachRename extends OperableFunction {
	public Object calculate(Context ctx) {
		ParamInfo2 pi = ParamInfo2.parse(param, "rename", true, false);
		String []srcFields = pi.getExpressionStrs1();
		String []newFields = pi.getExpressionStrs2();
		
		Rename rename = new Rename(this, srcFields, newFields);
		return operable.addOperation(rename, ctx);
	}
}
