package com.raqsoft.expression.fn;

import java.io.File;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.LocalFile;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/**
 * 列出满足通配符路径的文件名。 directory(path)
 * @author runqian
 *
 */
public class Directory extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		// path省略时返回的[当前主目录,当前临时目录]
		if (param == null) {
			Sequence seq = new Sequence(2);
			seq.add(Env.getMainPath());
			seq.add(Env.getTempPath());
			return seq;
		}

		File file;
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("directory" + mm.getMessage("function.paramTypeError"));
			}

			LocalFile lf = new LocalFile((String)obj, null, ctx);
			file = lf.file();
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("directory" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("directory" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("directory" + mm.getMessage("function.paramTypeError"));
			}
			
			Object partition = sub1.getLeafExpression().calculate(ctx);
			if (partition == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("directory" + mm.getMessage("function.invalidParam"));
			}
			
//			xq 修改partition为Integer 2017年2月14日
			Integer p = -1;
			if(partition instanceof Number){
				p = ((Number)partition).intValue();
			}else{
				p = Integer.parseInt(partition.toString());
			}
			
			LocalFile lf = new LocalFile((String)obj, null, p);
			file = lf.file();
		}
		
		boolean isDir = false, isAbsolute = false, isSub = false, ignoreCase = false;
		if (option != null) {
			if (option.indexOf('m') != -1) {
				boolean b = file.mkdirs();
				return Boolean.valueOf(b);
			} else if (option.indexOf('r') != -1) {
				boolean b = file.delete();
				return Boolean.valueOf(b);
			} else {
				isDir = option.indexOf('d') != -1;
				isAbsolute = option.indexOf('p') != -1;
				isSub = option.indexOf('s') != -1;
				ignoreCase = option.indexOf('c') != -1;
			}
		}

		String pattern = null;
		//FilenameFilter filter = null;
		if (!file.isDirectory()) {
			pattern = file.getName();
			file = file.getParentFile();

			if (!ignoreCase) {
				// 根据操作系统名字来确定是不是区分大小写
				String os = System.getProperty("os.name").toLowerCase();
				if (os.indexOf("windows") > -1) {
					ignoreCase = true;
				}
			}
			
			//filter = new FilenameFilter() {
			//	public boolean accept(File dir, String name) {
			//		return StringUtils.matches(name, pattern, ignoreCase);
			//	}
			//};
		}

		Sequence sequence = new Sequence();
		if (isDir) {
			listSubDirs(file, pattern, isAbsolute, isSub, ignoreCase, ctx, sequence);
		} else {
			listSubFiles(file, pattern, isAbsolute, isSub, ignoreCase, ctx, sequence);
		}
		
		sequence.trimToSize();
		return sequence;
	}
	
	private void listSubFiles(File file, String pattern, boolean isAbsolute, 
			boolean isSub, boolean ignoreCase, Context ctx, Sequence result) {
		if (file == null) {
			return;
		}
		
		File []subs = file.listFiles();
		if (subs == null) {
			return;
		}
		
		for (File sub : subs) {
			if (sub.isFile()) {
				if (pattern == null || StringUtils.matches(sub.getName(), pattern, ignoreCase)) {
					if (isAbsolute) {
						String pathName = sub.getAbsolutePath();
						result.add(LocalFile.removeMainPath(pathName, ctx));
					} else {
						result.add(sub.getName());
					}
				}
			} else if (isSub) {
				listSubFiles(sub, pattern, isAbsolute, isSub, ignoreCase, ctx, result);
			}
		}
	}

	private void listSubDirs(File file, String pattern, boolean isAbsolute, 
			boolean isSub, boolean ignoreCase, Context ctx, Sequence result) {
		File []subs = file.listFiles();
		if (subs == null) return;

		for (File sub : subs) {
			if (sub.isDirectory()) {
				if (pattern == null || StringUtils.matches(sub.getName(), pattern, ignoreCase)) {
					if (isAbsolute) {
						String pathName = sub.getAbsolutePath();
						result.add(LocalFile.removeMainPath(pathName, ctx));
					} else {
						result.add(sub.getName());
					}
				}
				
				if (isSub) {
					listSubDirs(sub, pattern, isAbsolute, isSub, ignoreCase, ctx, result);
				}
			}
		}
	}
}
