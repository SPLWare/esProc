package com.scudata.expression.mfn.file;

import com.scudata.dm.Context;
import com.scudata.expression.FileGroupFunction;

/**
 * 打开复组表
 * fg.open()
 * @author RunQian
 *
 */
public class FileGroupOpen extends FileGroupFunction {
	public Object calculate(Context ctx) {
		return fg.open(option, ctx);
	}
}
