package com.scudata.expression.mfn.file;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.UserUtils;
import com.scudata.excel.ExcelTool;
import com.scudata.excel.ExcelUtils;
import com.scudata.excel.XlsxSImporter;
import com.scudata.expression.FileFunction;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;

/**
 * f.xlsimport(Fi,..;s,b:e;p) 读入Excel， s为页名或序号，b,e为行数，e<0倒数 p是密码
 * 
 * @t 首行是标题，有b参数时认为标题在b行
 * @x 使用xlsx格式，缺省使用文件扩展名判断，判断不出用xls
 * @c 返回成游标，只支持xlsx格式；此时e不能小于0
 * @b 去除前后的空白行，@c时不支持
 * @w 读成序列的序列，成员是格值； 与@t@c@b互斥
 * @p @w加转置，序列的序列是先列后行的，是串时忽略
 * @n 读入时做trim，只剩空串时读成null
 * @s 返回成回车/tab分隔的串
 */
public class XlsImport extends FileFunction {

	/**
	 * 计算
	 */
	public Object calculate(Context ctx) {
		String opt = option;
		boolean isCursor = opt != null && opt.indexOf("c") > -1;
		boolean hasTitle = opt != null && opt.indexOf("t") > -1;
		boolean removeBlank = opt != null && opt.indexOf("b") > -1;
		if (isCursor && removeBlank) {
			throw new RQException(AppMessage.get().getMessage("xlsimport.nocb"));
		}

		boolean isW = opt != null && opt.indexOf("w") > -1;
		boolean isS = opt != null && opt.indexOf("s") > -1;
		boolean isP = opt != null && opt.indexOf("p") > -1;
		String wOrSText = isW ? "w" : "s";
		if (isW || isS) {
			if (hasTitle || removeBlank || isCursor) {
				throw new RQException(AppMessage.get().getMessage(
						"xlsimport.nowtbc", wOrSText));
			}
		}

		if (!isW) {
			if (isP) {
				// 选项@{0}只能和选项@w同时使用。
				throw new RQException(AppMessage.get().getMessage(
						"xlsimport.pnnotw", "p"));
			}
		}

		if (param == null) {
			InputStream in = null;
			BufferedInputStream bis = null;
			try {
				boolean isXlsx = ExcelUtils.isXlsxFile(file);
				if (isCursor && !isXlsx) {
					// @c only supports the xlsx format.
					MessageManager mm = AppMessage.get();
					throw new RQException("xlsimport"
							+ mm.getMessage("xlsfile.needxlsx"));
				}

				if (isCursor) {
					XlsxSImporter importer = new XlsxSImporter(file, null, 0,
							0, new Integer(1), opt);
					String cursorOpt = "";
					if (hasTitle)
						cursorOpt += "t";
					return UserUtils.newCursor(importer, cursorOpt);
				} else {
					in = file.getInputStream();
					bis = new BufferedInputStream(in, Env.FILE_BUFSIZE);
					ExcelTool importer = new ExcelTool(in, isXlsx, null);
					return importer.fileXlsImport(opt);
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
				try {
					if (bis != null)
						bis.close();
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
			}
		}

		String[] fields = null;
		Object s = null;
		int start = 0;
		int end = 0;

		IParam fieldParam;
		String pwd = null;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2 && param.getSubSize() != 3) { // 兼容一下之前的
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsimport"
						+ mm.getMessage("function.invalidParam"));
			}

			fieldParam = param.getSub(0);
			IParam param1 = param.getSub(1);
			if (param.getSubSize() == 3) {
				IParam pwdParam = param.getSub(2);
				if (pwdParam != null) {
					Object tmp = pwdParam.getLeafExpression().calculate(ctx);
					if (tmp != null) {
						pwd = tmp.toString();
					}
					if ("".equals(pwd))
						pwd = null;
				}
			}
			if (param1 == null) {
			} else if (param1.isLeaf()) {
				s = param1.getLeafExpression().calculate(ctx);
			} else {
				if (param1.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xlsimport"
							+ mm.getMessage("function.invalidParam"));
				}

				IParam sParam = param1.getSub(0);
				if (sParam != null) {
					s = sParam.getLeafExpression().calculate(ctx);
				}

				IParam posParam = param1.getSub(1);
				if (posParam == null) {
				} else if (posParam.isLeaf()) { // start
					Object obj = posParam.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xlsimport"
								+ mm.getMessage("function.paramTypeError"));
					}

					start = ((Number) obj).intValue();
				} else { // start:end
					if (posParam.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xlsimport"
								+ mm.getMessage("function.invalidParam"));
					}

					IParam sub0 = posParam.getSub(0);
					IParam sub1 = posParam.getSub(1);
					if (sub0 != null) {
						Object obj = sub0.getLeafExpression().calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("xlsimport"
									+ mm.getMessage("function.paramTypeError"));
						}

						start = ((Number) obj).intValue();
					}

					if (sub1 != null) {
						Object obj = sub1.getLeafExpression().calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("xlsimport"
									+ mm.getMessage("function.paramTypeError"));
						}

						end = ((Number) obj).intValue();
					}
				}
			}
		} else {
			fieldParam = param;
		}

		if (fieldParam != null) {
			if (fieldParam.isLeaf()) {
				fields = new String[] { fieldParam.getLeafExpression()
						.getIdentifierName() };
			} else {
				int count = fieldParam.getSubSize();
				fields = new String[count];
				for (int i = 0; i < count; ++i) {
					IParam sub = fieldParam.getSub(i);
					if (sub == null || !sub.isLeaf()) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xlsimport"
								+ mm.getMessage("function.invalidParam"));
					}

					fields[i] = sub.getLeafExpression().getIdentifierName();
				}
			}
		}

		boolean isXlsx = false;
		try {
			isXlsx = ExcelUtils.isXlsxFile(file);
		} catch (Throwable e1) {
			if (StringUtils.isValidString(file.getFileName())) {
				isXlsx = file.getFileName().toLowerCase().endsWith(".xlsx");
			}
		}
		if (isCursor && !isXlsx) {
			// @c only supports the xlsx format.
			MessageManager mm = AppMessage.get();
			throw new RQException("xlsimport"
					+ mm.getMessage("xlsfile.needxlsx"));
		}

		if (isW || isS) {
			if (fields != null) {
				throw new RQException(AppMessage.get().getMessage(
						"xlsimport.nowfields", wOrSText));
			}
		}

		InputStream in = null;
		BufferedInputStream bis = null;
		try {
			if (isCursor) {
				XlsxSImporter importer = new XlsxSImporter(file, fields, start,
						end, s, opt, pwd);
				String cursorOpt = "";
				if (hasTitle)
					cursorOpt += "t";
				return UserUtils.newCursor(importer, cursorOpt);
			} else {
				in = file.getInputStream();
				bis = new BufferedInputStream(in, Env.FILE_BUFSIZE);
				ExcelTool importer = new ExcelTool(in, isXlsx, pwd);
				return importer.fileXlsImport(fields, start, end, s, opt);
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			try {
				if (bis != null)
					bis.close();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 对节点做优化
	 * @param ctx 计算上下文
	 * @param Node 优化后的节点
	 */
	public Node optimize(Context ctx) {
		if (param != null) {
			// 对参数做优化
			param.optimize(ctx);
		}

		return this;
	}
}
