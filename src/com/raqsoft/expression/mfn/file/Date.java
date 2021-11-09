package com.raqsoft.expression.mfn.file;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.FileFunction;

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
