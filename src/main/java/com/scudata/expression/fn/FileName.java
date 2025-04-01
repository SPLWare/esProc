package com.scudata.expression.fn;

import java.io.File;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.LocalFile;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

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
		boolean eopt = false, nopt = false, dopt = false, popt = false, sopt = false;
		if (option != null) {
			if (option.indexOf('t') != -1) {
				return createTempFile(param, ctx);
			}
			
			if (option.indexOf('e') != -1) eopt = true;
			if (option.indexOf('n') != -1) nopt = true;
			if (option.indexOf('d') != -1) dopt = true;
			if (option.indexOf('p') != -1) popt = true;
			if (option.indexOf('s') != -1) sopt = true;
		}
		
		if (sopt) {
			String pathName = null;
			if (cs instanceof PgmCellSet) {
				pathName = ((PgmCellSet)cs).getName();
			}

			if (pathName == null) {
				return null;
			}
			
			LocalFile lf = new LocalFile(pathName, null, ctx);
			File file = lf.file();
			if (eopt) {
				String name = file.getName();
				int dot = name.lastIndexOf('.');
				if (dot == -1) {
					return null;
				} else {
					return name.substring(dot + 1);
				}
			} else if (nopt) {
				String name = file.getName();
				int dot = name.lastIndexOf('.');
				if (dot == -1) {
					return name;
				} else {
					return name.substring(0, dot);
				}
			} else if (dopt) {
				String str = file.getParent();
				return LocalFile.removeMainPath(str, ctx);
			} else if (popt) {
				return file.getAbsolutePath();
			} else {
				return file.getName();
			}
		} else if (popt) {
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
				File file = new File(name);
				if (!file.isAbsolute()) {
					file = new File(Env.getMainPath(), name);
				}
				
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
		if (eopt) {
			String name = file.getName();
			int dot = name.lastIndexOf('.');
			if (dot == -1) {
				return null;
			} else {
				return name.substring(dot + 1);
			}
		} else if (nopt) {
			String name = file.getName();
			int dot = name.lastIndexOf('.');
			if (dot == -1) {
				return name;
			} else {
				return name.substring(0, dot);
			}
		} else if (dopt) {
			String str = file.getParent();
			return LocalFile.removeMainPath(str, ctx);
		} else {
			return file.getName();
		}
	}
}
