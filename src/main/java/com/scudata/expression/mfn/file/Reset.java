package com.scudata.expression.mfn.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileGroup;
import com.scudata.dm.FileObject;
import com.scudata.dm.IFile;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.ColComTable;
import com.scudata.dw.ComTable;
import com.scudata.dw.RowComTable;
import com.scudata.expression.FileFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 整理组表数据或者复制组表数据到新组表
 * f.reset(f’) f.reset(f’,x;cs)
 * @author RunQian
 *
 */
public class Reset extends FileFunction {
	public Object calculate(Context ctx) {
		Object obj = null;
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
		
		if (param == null) {
			FileObject fo = (FileObject) this.file;
			
			ComTable gt;
			try {
				gt = open(fo, ctx);
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			boolean result = gt.reset(null, option, ctx, null, null, cs);
			gt.close();
			return result;
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
				ComTable gt = open(fo, ctx);
				boolean result =  gt.reset(f, option, ctx, null, null, cs);
				gt.close();
				return result;
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		} else {
			int size = param.getSubSize();
			if (size != 2 && size != 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("reset" + mm.getMessage("function.invalidParam"));
			}
			
			File f = null;
			FileGroup fg = null;
			String distribute = null;
			Integer blockSize = null;
			
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
			
			FileObject fo = (FileObject) this.file;
			File file = fo.getLocalFile().file();

			try {
				ComTable gt = open(fo, ctx);
				if (f != null) {
					boolean result =  gt.reset(f, option, ctx, distribute, blockSize, cs);
					gt.close();
					return result;
				} else {
					if (distribute == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("reset" + mm.getMessage("function.invalidParam"));
					}
					boolean result =  gt.resetFileGroup(fg, option, ctx, distribute, blockSize, cs);
					gt.close();
					return result;
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}
	
	private static ComTable open(FileObject fo, Context ctx) throws IOException {
		IFile ifile = fo.getFile();
		if (!ifile.exists()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("file.fileNotExist", fo.getFileName()));
		}
		
		File file = fo.getLocalFile().file();
		RandomAccessFile raf = ifile.getRandomAccessFile();

		raf.seek(6);
		
		if (raf.length() == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}
		
		int flag = raf.read(); 
		if (flag == 'r') {
			return new RowComTable(file, raf, ctx);
		} else if (flag == 'c' || flag == 'C'){
			return new ColComTable(file, raf, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}
	}
}
