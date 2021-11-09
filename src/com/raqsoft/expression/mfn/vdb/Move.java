package com.raqsoft.expression.mfn.vdb;

import com.raqsoft.resources.EngineMessage;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.VSFunction;

/**
 * 删除目录或者把目录移动到另一个目录下
 * h.move(ps,pd:F)
 * @author RunQian
 *
 */
public class Move extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return vs.delete(option);
		} else if (param.isLeaf()) {
			Object path = param.getLeafExpression().calculate(ctx);
			return vs.delete(path, option);
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("move" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("move" + mm.getMessage("function.invalidParam"));
			}
			
			Object srcPath = sub0.getLeafExpression().calculate(ctx);
			Object destPath;
			Object destName = null;
			if (sub1.isLeaf()) {
				destPath = sub1.getLeafExpression().calculate(ctx);
			} else {
				if (sub1.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("move" + mm.getMessage("function.invalidParam"));
				}
				
				IParam path = sub1.getSub(0);
				IParam name = sub1.getSub(1);
				if (path == null || name == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("move" + mm.getMessage("function.invalidParam"));
				}
				
				destPath = path.getLeafExpression().calculate(ctx);
				destName = name.getLeafExpression().calculate(ctx);
			}
			
			return vs.move(srcPath, destPath, destName);
		}
	}
}
