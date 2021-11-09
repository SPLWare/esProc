package com.raqsoft.expression.mfn.file;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.FileFunction;

/**
 * 取文件大小
 * f.size()
 * @author RunQian
 *
 */
public class Size extends FileFunction {
	public Object calculate(Context ctx) {
		return new Long(file.size());
	}
}
