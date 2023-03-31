package com.scudata.expression.mfn.file;

import java.io.File;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileGroup;
import com.scudata.dm.FileObject;
import com.scudata.expression.FileGroupFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 整理复组表数据或者复制组表数据到另一个复组表
 * f.reset(f’) f.reset(f’;x)
 * @author RunQian
 *
 */
public class FileGroupReset extends FileGroupFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return fg.resetGroupTable(option, null, ctx);
		}

		Object obj = null; // 新文件名、新文件对象或者新文件组
		String distribute = null; // 分布表达式
		Integer blockSize = null;
		
		if (param.isLeaf()) {
			obj = param.getLeafExpression().calculate(ctx);
		} else {
			if (param.getType() != IParam.Semicolon) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("reset" + mm.getMessage("function.invalidParam"));
			}

			int size = param.getSubSize();
			if (size != 2 && size != 3) {
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
			
			IParam blockSizeParam = param.getSub(2);
			if (blockSizeParam != null) {
				String b = blockSizeParam.getLeafExpression().calculate(ctx).toString();
				try {
					blockSize = Integer.parseInt(b);
				} catch (NumberFormatException e) {
				}
			}
		}
		
		if (obj == null) {
			return fg.resetGroupTable(option, blockSize, ctx);
		} else if (obj instanceof FileObject) {
			File newFile = ((FileObject)obj).getLocalFile().file();
			return fg.resetGroupTable(newFile, option, blockSize, ctx);
		} else if (obj instanceof String) {
			FileObject fileObject = new FileObject((String)obj);
			File newFile = fileObject.getLocalFile().file();
			return fg.resetGroupTable(newFile, option, blockSize, ctx);
		} else if (obj instanceof FileGroup) {
			return fg.resetGroupTable((FileGroup)obj, option, distribute, blockSize, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("reset" + mm.getMessage("function.paramTypeError"));
		}
	}
}
