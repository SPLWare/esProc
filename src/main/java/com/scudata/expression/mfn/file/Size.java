package com.scudata.expression.mfn.file;

import com.scudata.dm.Context;
import com.scudata.expression.FileFunction;

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
