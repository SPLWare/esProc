package com.raqsoft.expression.mfn.vdb;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.VSFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 把数据写入到指定路径的表单
 * h.save(x,p,F)
 * @author RunQian
 *
 */
public class Save extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("save" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object value = param.getLeafExpression().calculate(ctx);
			return vs.save(value);
		}
		
		int size = param.getSubSize();
		if (size > 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("save" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("save" + mm.getMessage("function.invalidParam"));
		}
		
		Object path = sub1.getLeafExpression().calculate(ctx);
		Object name = null;
		
		if (size > 2) {
			IParam sub2 = param.getSub(2);
			if (sub2 != null) {
				name = sub2.getLeafExpression().calculate(ctx);
			}
		}
		
		if (sub0 != null) {
			Object value = sub0.getLeafExpression().calculate(ctx);
			return vs.save(value, path, name);
		} else {
			// 没有给出表单内容则创建目录
			return vs.makeDir(path, name);
		}
	}
}
