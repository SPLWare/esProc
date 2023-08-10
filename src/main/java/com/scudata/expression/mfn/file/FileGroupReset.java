package com.scudata.expression.mfn.file;

import java.io.File;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileGroup;
import com.scudata.dm.FileObject;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.FileGroupFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 整理复组表数据或者复制组表数据到另一个复组表
 * f.reset(f’) f.reset(f’,x;cs)
 * @author RunQian
 *
 */
public class FileGroupReset extends FileGroupFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return fg.resetGroupTable(option, null, null, ctx);
		}

		Object obj = null; // 新文件名、新文件对象或者新文件组
		String distribute = null; // 分布表达式
		Integer blockSize = null;
		
		ICursor cs = null;
		IParam param = this.param;
		if (param != null && param.getType() == IParam.Semicolon && param.getSubSize() == 2) {
			IParam csParam = param.getSub(1);
			if (csParam != null) {
				obj = csParam.getLeafExpression().calculate(ctx);
				if (obj instanceof ICursor) {
					cs = (ICursor) obj;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("reset" + mm.getMessage("function.paramTypeError"));
				}
			}
			param = param.getSub(0);
		}
		
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
			
			if (size == 3) {
				IParam blockSizeParam = param.getSub(2);
				if (blockSizeParam != null) {
					String b = blockSizeParam.getLeafExpression().calculate(ctx).toString();
					try {
						blockSize = Integer.parseInt(b);
					} catch (NumberFormatException e) {
					}
				}
			}
		}
		
		if (obj == null) {
			return fg.resetGroupTable(option, blockSize, cs, ctx);
		} else if (obj instanceof FileObject) {
			File newFile = ((FileObject)obj).getLocalFile().file();
			return fg.resetGroupTable(newFile, option, blockSize, cs, ctx);
		} else if (obj instanceof String) {
			FileObject fileObject = new FileObject((String)obj);
			File newFile = fileObject.getLocalFile().file();
			return fg.resetGroupTable(newFile, option, blockSize, cs, ctx);
		} else if (obj instanceof FileGroup) {
			return fg.resetGroupTable((FileGroup)obj, option, distribute, blockSize, cs, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("reset" + mm.getMessage("function.paramTypeError"));
		}
	}
}
