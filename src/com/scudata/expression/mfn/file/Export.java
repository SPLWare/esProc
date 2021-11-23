package com.scudata.expression.mfn.file;

import java.io.File;
import java.io.IOException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileWriter;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.IFile;
import com.scudata.dm.LocalFile;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.expression.Expression;
import com.scudata.expression.FileFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 导出排列到文本文件或集文件
 * f.export(A,xi:Fi,…;s)
 * @author RunQian
 *
 */
public class Export extends FileFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("export" + mm.getMessage("function.missingParam"));
		}

		IParam param0;
		Expression sexp = null;
		if (param.getType() == IParam.Semicolon) { // ;
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("export" + mm.getMessage("function.invalidParam"));
			}

			param0 = param.getSub(0);
			if (param0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("export" + mm.getMessage("function.invalidParam"));
			}
			
			IParam param1 = param.getSub(1);
			if (param1 != null) {
				sexp = param1.getLeafExpression();
			}
		} else {
			param0 = param;
		}

		Object src;
		Expression []exps = null;
		String []names = null;
		if (param0.isLeaf()) {
			src = param0.getLeafExpression().calculate(ctx);
		} else { // series,xi:fi...
			IParam sub = param0.getSub(0);
			if (sub == null || !sub.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("export" + mm.getMessage("function.invalidParam"));
			}

			src = sub.getLeafExpression().calculate(ctx);
			int size = param0.getSubSize();
			exps = new Expression[size - 1];
			names = new String[size - 1];
			for (int i = 1; i < size; ++i) {
				sub = param0.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("export" + mm.getMessage("function.invalidParam"));
				} else if (sub.isLeaf()) {
					exps[i - 1] = sub.getLeafExpression();
				} else {
					if (sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("export" + mm.getMessage("function.invalidParam"));
					}

					IParam p1 = sub.getSub(0);
					if (p1 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("export" + mm.getMessage("function.invalidParam"));
					}

					exps[i - 1] = p1.getLeafExpression();
					IParam p2 = sub.getSub(1);
					if (p2 != null) {
						names[i - 1] = p2.getLeafExpression().getIdentifierName();
					}
				}
			}
		}

		export(file, src, exps, names, sexp, option, ctx);
		return null;
	}
	
	
	private static void export(FileObject fo, Object src, Expression []exps, String []names, 
			Expression sexp, String opt, Context ctx) {
		// 如果不是追加写并且源文件已存在，则先写入临时文件，成功后把源文件删除再把临时文件重命名成源文件
		IFile ifile = fo.getFile();
		if ((opt == null || opt.indexOf('a') == -1) && 
			ifile instanceof LocalFile && ifile.size() > 0) {
			File oldFile = ((LocalFile)ifile).file();
			File parentFile = oldFile.getParentFile();
			
			File tmpFile;
			try {
				tmpFile = File.createTempFile("tmpdata", "", parentFile);
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			String cs = fo.getCharset();
			LocalFile lf = new LocalFile(tmpFile.getName(), null, ctx);
			lf.setParent(parentFile.getAbsolutePath());
			fo = new FileObject(lf, tmpFile.getAbsolutePath(), cs, null);
			
			try {
				if (sexp != null && BFileWriter.isBtxOption(opt)) {
					ICursor cursor;
					if (src instanceof Sequence || src == null) {
						cursor = new MemoryCursor((Sequence)src);
					} else if (src instanceof ICursor) {
						cursor = (ICursor)src;
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("export" + mm.getMessage("function.paramTypeError"));
					}
					
					fo.export_g(cursor, exps, names, sexp, opt, ctx);
				} else {
					Object s = sexp == null ? null : sexp.calculate(ctx);
					if (src instanceof Sequence || src == null) {
						fo.exportSeries((Sequence)src, exps, names, opt, s, ctx);
					} else if (src instanceof ICursor) {
						fo.exportCursor((ICursor)src, exps, names, opt, s, ctx);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("export" + mm.getMessage("function.paramTypeError"));
					}
				}
				
				if (oldFile.delete()) {
					tmpFile.renameTo(oldFile);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("export" + mm.getMessage("file.deleteFailed"));
				}
			} catch (RuntimeException e) {
				tmpFile.delete();
				throw e;
			}
		} else {
			if (sexp != null && BFileWriter.isBtxOption(opt)) {
				ICursor cursor;
				if (src instanceof Sequence || src == null) {
					cursor = new MemoryCursor((Sequence)src);
				} else if (src instanceof ICursor) {
					cursor = (ICursor)src;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("export" + mm.getMessage("function.paramTypeError"));
				}
				
				fo.export_g(cursor, exps, names, sexp, opt, ctx);
			} else {
				Object s = sexp == null ? null : sexp.calculate(ctx);
				if (src instanceof Sequence || src == null) {
					fo.exportSeries((Sequence)src, exps, names, opt, s, ctx);
				} else if (src instanceof ICursor) {
					fo.exportCursor((ICursor)src, exps, names, opt, s, ctx);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("export" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
	}
}
