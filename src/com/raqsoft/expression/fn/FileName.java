package com.raqsoft.expression.fn;

import java.io.File;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.LocalFile;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/**
 * filename(fn) 拆分全路径fn中的文件名和扩展名内容。
 * @author runqian
 *
 */
public class FileName extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	private static String createTempFile(IParam param, Context ctx) {
		if (param == null) {
			String pathName = Env.getTempPath();
			if (pathName == null || pathName.length() == 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("filename" + mm.getMessage("function.missingParam"));
			}
			
			FileObject fo = new FileObject(pathName, null, ctx);
			String str = fo.createTempFile();
			return LocalFile.removeMainPath(str, ctx);
		} else {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("filename" + mm.getMessage("function.paramTypeError"));
			}

			String pathName = (String)obj;
			FileObject fo = new FileObject(pathName, null, ctx);
			File file = new File(fo.createTempFile());
			return file.getName();
		}
	}
	
	public Object calculate(Context ctx) {
		if (option != null && option.indexOf('t') != -1) {
			return createTempFile(param, ctx);
		} else if (option != null && option.indexOf('p') != -1) {
			String name = null;
			if (param != null) {
				Object obj = param.getLeafExpression().calculate(ctx);
				if (obj instanceof String) {
					name = (String)obj;
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("filename" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			if (name == null || name.length() == 0) {
				return Env.getMainPath();
			} else {
				File file = new File(Env.getMainPath(), name);
				return file.getAbsolutePath();
			}
		}

		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("filename" + mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		if (!(obj instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("filename" + mm.getMessage("function.paramTypeError"));
		}

		LocalFile lf = new LocalFile((String)obj, null, ctx);
		File file = lf.file();
		if (option == null) {
			return file.getName();
		} else if (option.indexOf('e') != -1) {
			String name = file.getName();
			int dot = name.lastIndexOf('.');
			if (dot == -1) {
				return null;
			} else {
				return name.substring(dot + 1);
			}
		} else if (option.indexOf('n') != -1) {
			String name = file.getName();
			int dot = name.lastIndexOf('.');
			if (dot == -1) {
				return name;
			} else {
				return name.substring(0, dot);
			}
		} else if (option.indexOf('d') != -1) {
			String str = file.getParent();
			return LocalFile.removeMainPath(str, ctx);
		} else {
			return file.getName();
		}
	}
}
