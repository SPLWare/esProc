package com.raqsoft.expression.mfn.vdb;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.VSFunction;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.vdb.IVS;

/**
 * 复制路径到另一个路径下
 * h.copy(ps,pd:F,h')
 * @author RunQian
 *
 */
public class Copy extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("copy" + mm.getMessage("function.missingParam"));
		}

		Object srcPath = null;
		Object destPath = null;
		Object destName = null;
		IVS dest = null;
		
		if (param.isLeaf()) {
			srcPath = param.getLeafExpression().calculate(ctx);
		} else {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("copy" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 != null) {
				srcPath = sub0.getLeafExpression().calculate(ctx);
			}
			
			if (sub1 != null) {
				if (sub1.isLeaf()) {
					destPath = sub1.getLeafExpression().calculate(ctx);
				} else {
					if (sub1.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("copy" + mm.getMessage("function.invalidParam"));
					}
					
					IParam path = sub1.getSub(0);
					IParam name = sub1.getSub(1);
					if (path == null || name == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("copy" + mm.getMessage("function.invalidParam"));
					}
					
					destPath = path.getLeafExpression().calculate(ctx);
					destName = name.getLeafExpression().calculate(ctx);
				}
			}
			
			if (size > 2) {
				IParam sub2 = param.getSub(2);
				if (sub2 != null) {
					Object vs = sub2.getLeafExpression().calculate(ctx);
					if (vs instanceof IVS) {
						dest = (IVS)vs;
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("copy" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
		}
		
		if (dest == null) {
			dest = vs;
		}
		
		return dest.copy(destPath, destName, vs, srcPath);
	}
}
