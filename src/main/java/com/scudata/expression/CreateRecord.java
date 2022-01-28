package com.scudata.expression;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;

/**
 * 创建记录
 * {f1:v1,...} 字段名是常串，值是表达式
 * @author RunQian
 *
 */
public class CreateRecord extends Function {
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}
	
	public Object calculate(Context ctx) {
		ParamInfo2 pi = ParamInfo2.parse(param, "{}", true, true);
		String []names = pi.getExpressionStrs1();
		Object []values = pi.getValues2(ctx);
		DataStruct ds = new DataStruct(names);
		return new Record(ds, values);
	}
}
