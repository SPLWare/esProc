package com.scudata.expression;

import com.scudata.cellset.ICellSet;
import com.scudata.dm.Context;
import com.scudata.expression.fn.Call;

/**
 * 注册的脚步函数
 * @author WangXiaoJun
 *
 */
class DfxFunction {
	private String dfxPathName; // 脚本路径名
	private boolean hasOptParam; // 是否有选项参数
	
	public DfxFunction(String dfxPathName, String opt) {
		this.dfxPathName = dfxPathName;
		hasOptParam = opt != null && opt.indexOf('o') != -1;
	}
	
	public Function newFunction(ICellSet cs, Context ctx, String opt, String param) {
		Function fun = new Call();
		if (hasOptParam) {
			if (opt == null) {
				opt = "null";
			} else {
				opt = '"' + opt + '"';
			}
			
			if (param == null) {
				param = opt;
			} else {
				param = opt + ',' + param;
			}
		}
		
		if (param == null || param.length() == 0) {
			fun.setParameter(cs, ctx, '"' + dfxPathName + '"');
		} else {
			fun.setParameter(cs, ctx, '"' + dfxPathName + '"' + ',' + param);
		}
		
		return fun;
	}
}
