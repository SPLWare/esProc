package com.raqsoft.expression.mfn.file;

import java.io.File;
import java.io.IOException;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileGroup;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dw.GroupTable;
import com.raqsoft.expression.FileFunction;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 整理组表数据或者复制组表数据到新组表
 * f.reset(f’) f.reset(f’;x)
 * @author RunQian
 *
 */
public class Reset extends FileFunction {
	public Object calculate(Context ctx) {
		Object obj = null;
		if (param == null) {
			FileObject fo = (FileObject) this.file;
			File file = fo.getLocalFile().file();
			
			GroupTable gt;
			try {
				gt = GroupTable.open(file, ctx);
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			return gt.reset(null, option, ctx, null);
		} else if (param.isLeaf()) {
			obj = param.getLeafExpression().calculate(ctx);
			File f = null;
			if (obj instanceof FileObject) {
				f = ((FileObject) obj).getLocalFile().file();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("reset" + mm.getMessage("function.paramTypeError"));
			}
			
			FileObject fo = (FileObject) this.file;
			File file = fo.getLocalFile().file();
			
			try {
				GroupTable gt = GroupTable.open(file, ctx);
				return gt.reset(f, option, ctx, null);
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
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
			
			File f = null;
			FileGroup fg = null;
			String distribute = null;
			IParam sub0 = param.getSub(0);
			if (sub0 != null) {
				obj = sub0.getLeafExpression().calculate(ctx);
				if (obj instanceof FileObject) {
					f = ((FileObject) obj).getLocalFile().file();
				} else if (obj instanceof FileGroup) {
					fg= (FileGroup) obj;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("reset" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			IParam expParam = param.getSub(1);
			if (expParam != null) {
				distribute = expParam.getLeafExpression().toString();
			}
			
			FileObject fo = (FileObject) this.file;
			File file = fo.getLocalFile().file();

			try {
				GroupTable gt = GroupTable.open(file, ctx);
				if (f != null) {
					return gt.reset(f, option, ctx, distribute);
				} else {
					if (distribute == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("reset" + mm.getMessage("function.invalidParam"));
					}
					return gt.resetFileGroup(fg, option, ctx, distribute);
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}
}
