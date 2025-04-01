package com.scudata.expression.mfn.file;

import com.scudata.dm.Context;
import com.scudata.expression.FileFunction;

/**
 * 取文件最后修改日期时间
 * f.date()
 * @author RunQian
 *
 */
public class Date extends FileFunction {
	public Object calculate(Context ctx) {
		return file.lastModified();
	}
}
