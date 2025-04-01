package com.scudata.expression.mfn.dw;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dw.Cuboid;
import com.scudata.dw.PhyTable;
import com.scudata.dw.PhyTableGroup;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.PhyTableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 利用预汇总立方体计算分组汇总
 * T.cgroups(Fi,…;y:Gi,…;w;f)
 * @author RunQian
 *
 */
public class Cgroups extends PhyTableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cgroups" + mm.getMessage("function.missingParam"));
		}
		
		IParam sub0;
		IParam sub1 = null;
		IParam sub2 = null;
		Expression w = null;
		FileObject[] files = null;
		boolean hasM = false;
		int n = Env.getParallelNum();
		if (option != null && option.indexOf('m') != -1) {
			hasM = true;
		}
		if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if ((size > 4 && !hasM) || (size > 5 && hasM)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cgroups" + mm.getMessage("function.invalidParam"));
			}
			
			sub0 = param.getSub(0);
			
			sub1 = param.getSub(1);
			if (size > 2) {
				sub2 = param.getSub(2);
			}
			w = sub2 == null ? null : sub2.getLeafExpression();
			
			IParam sub3 = param.getSub(3);
			if (sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cgroups" + mm.getMessage("function.invalidParam"));
			} else {
				int len = sub3.getSubSize();
				if (len == 0) {
					FileObject f = (FileObject) sub3.getLeafExpression().calculate(ctx);
					files = new FileObject[] {f};	
				} else {
					files = new FileObject[len];
					for (int i = 0; i < len; i++) {
						files[i] = (FileObject) sub3.getSub(i).getLeafExpression().calculate(ctx);
					}
				}
			}
			
			if (hasM) {
				IParam sub4 = param.getSub(4);
				if (sub4 != null) {
					n = (Integer) sub4.getLeafExpression().calculate(ctx);
				}
			}
		} else {
			sub0 = param;
		}
		
		String []expNames = null;
		String []names = null;
		String []newExpNames = null;
		String []newNames = null;
		
		if (sub0 != null) {
			ParamInfo2 pi0 = ParamInfo2.parse(sub0, "cuboid", true, false);
			names = pi0.getExpressionStrs2();
			expNames = pi0.getExpressionStrs1();
		}
		
		ParamInfo2 pi1 = null;
		if (sub1 != null) {
			pi1 = ParamInfo2.parse(sub1, "cuboid", true, false);
			newExpNames = pi1.getExpressionStrs1();
			newNames = pi1.getExpressionStrs2();
		}

		if (table instanceof PhyTableGroup) {
			return ((PhyTableGroup)table).cgroups(expNames, names, newExpNames, newNames, 
					w, hasM, n, option, files, ctx);
		} else {
			return Cuboid.cgroups(expNames, names, newExpNames, newNames, 
				(PhyTable) table, w, hasM, n, option, files, ctx);
		}
	}
}
