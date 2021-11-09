package com.raqsoft.expression.mfn.file;

import java.io.File;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileGroup;
import com.raqsoft.dm.FileObject;
import com.raqsoft.expression.FileGroupFunction;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 整理复组表数据或者复制组表数据到另一个复组表
 * f.reset(f’) f.reset(f’;x)
 * @author RunQian
 *
 */
public class FileGroupReset extends FileGroupFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return fg.resetGroupTable(option, ctx);
		}

		Object obj = null; // 新文件名、新文件对象或者新文件组
		String distribute = null; // 分布表达式
		if (param.isLeaf()) {
			obj = param.getLeafExpression().calculate(ctx);
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
			
			IParam sub0 = param.getSub(0);
			if (sub0 != null) {
				obj = sub0.getLeafExpression().calculate(ctx);
			}
			
			IParam expParam = param.getSub(1);
			if (expParam != null) {
				distribute = expParam.getLeafExpression().toString();
			}
		}
		
		if (obj == null) {
			return fg.resetGroupTable(option, ctx);
		} else if (obj instanceof FileObject) {
			File newFile = ((FileObject)obj).getLocalFile().file();
			return fg.resetGroupTable(newFile, option, ctx);
		} else if (obj instanceof String) {
			FileObject fileObject = new FileObject((String)obj);
			File newFile = fileObject.getLocalFile().file();
			return fg.resetGroupTable(newFile, option, ctx);
		} else if (obj instanceof FileGroup) {
			return fg.resetGroupTable((FileGroup)obj, option, distribute, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("reset" + mm.getMessage("function.paramTypeError"));
		}
	}
}
