package com.scudata.expression;

import com.scudata.cellset.ICellSet;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.dm.Context;
import com.scudata.expression.fn.Call;
import com.scudata.expression.fn.PCSFunction;

/**
 * 注册的脚步函数
 * @author WangXiaoJun
 *
 */
public class DfxFunction {
	//private String funcName; // 函数名
	private String dfxPathName; // 脚本路径名
	private boolean hasOptParam; // 是否有选项参数
	
	private PgmCellSet.FuncInfo funcInfo;
	
	public DfxFunction(String dfxPathName, String opt) {
		this.dfxPathName = dfxPathName;
		hasOptParam = opt != null && opt.indexOf('o') != -1;
	}
	
	public DfxFunction(PgmCellSet.FuncInfo funcInfo) {
		this.funcInfo = funcInfo;
	}
	
	public Function newFunction(ICellSet cs, Context ctx, String opt, String param) {
		if (funcInfo != null) {
			Function fn = new PCSFunction(funcInfo);
			fn.setOption(opt);
			fn.setParameter(cs, ctx, param);
			return fn;
		} else {
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

	public PgmCellSet.FuncInfo getFuncInfo() {
		return funcInfo;
	}
}
