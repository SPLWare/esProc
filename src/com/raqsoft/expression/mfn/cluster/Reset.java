package com.raqsoft.expression.mfn.cluster;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileObject;
import com.raqsoft.expression.ClusterFileFunction;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 整理集群组表的数据或者复制集群组表数据到新集群组表
 * f.reset(f’) f.reset(f’;x)
 * @author RunQian
 *
 */
public class Reset extends ClusterFileFunction {
	public Object calculate(Context ctx) {
		Object obj = null;
		if (param == null) {
			return file.resetGroupTable(null, option, null);
		} else if (param.isLeaf()) {
			obj = param.getLeafExpression().calculate(ctx);
			String fname = null;
			if (obj instanceof FileObject) {
				fname = ((FileObject) obj).getFileName();
			} else if (obj instanceof String) {
				fname = (String) obj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("reset" + mm.getMessage("function.paramTypeError"));
			}
			
			return file.resetGroupTable(fname, option, null);
		} else {
			if (param.getType() != IParam.Semicolon) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("reset" + mm.getMessage("function.invalidParam"));
			}
			
			int size = param.getSubSize();
			if (size != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("reset" + mm.getMessage("function.invalidParam"));
			}
			
			String fname = null;
			String distribute = null;
			IParam sub0 = param.getSub(0);
			if (sub0 != null) {
				obj = sub0.getLeafExpression().calculate(ctx);
				if (obj instanceof FileObject) {
					fname = ((FileObject) obj).getFileName();
				} else if (obj instanceof String) {
					fname = (String) obj;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("reset" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			IParam expParam = param.getSub(1);
			if (expParam != null) {
				distribute = expParam.getLeafExpression().toString();
			}

			return file.resetGroupTable(fname, option, distribute);
		}
	}
}
