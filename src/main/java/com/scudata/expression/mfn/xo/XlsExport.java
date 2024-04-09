package com.scudata.expression.mfn.xo;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.SubCursor;
import com.scudata.excel.ExcelUtils;
import com.scudata.excel.SheetObject;
import com.scudata.excel.SheetXls;
import com.scudata.excel.XlsFileObject;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.XOFunction;
import com.scudata.resources.EngineMessage;

/**
 * xo.xlsexport(A,x:Fi,..;s) 向sheet中写入序列，s不存在则新加，xo以@w打开时A可是游标
 * 
 * @a s已存在时延用格式追加写，缺省将覆盖写
 * @t 有标题，页上有内容时认为最后一个有内容的行是标题
 */
public class XlsExport extends XOFunction {

	/**
	 * 计算
	 */
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsexport"
					+ mm.getMessage("function.missingParam"));
		}

		IParam param0;
		IParam param1 = null;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsexport"
						+ mm.getMessage("function.invalidParam"));
			}

			param0 = param.getSub(0);
			param1 = param.getSub(1);
			if (param0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsexport"
						+ mm.getMessage("function.invalidParam"));
			}
		} else {
			param0 = param;
		}

		Object src;
		Expression[] exps = null;
		String[] names = null;
		Object s = null;

		if (param0.isLeaf()) {
			src = param0.getLeafExpression().calculate(ctx);
		} else { // series,xi:fi...
			IParam sub = param0.getSub(0);
			if (sub == null || !sub.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsexport"
						+ mm.getMessage("function.invalidParam"));
			}

			src = sub.getLeafExpression().calculate(ctx);
			int size = param0.getSubSize();
			exps = new Expression[size - 1];
			names = new String[size - 1];
			for (int i = 1; i < size; ++i) {
				sub = param0.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xlsexport"
							+ mm.getMessage("function.invalidParam"));
				} else if (sub.isLeaf()) {
					exps[i - 1] = sub.getLeafExpression();
				} else {
					if (sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xlsexport"
								+ mm.getMessage("function.invalidParam"));
					}

					IParam p1 = sub.getSub(0);
					if (p1 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xlsexport"
								+ mm.getMessage("function.invalidParam"));
					}

					exps[i - 1] = p1.getLeafExpression();
					IParam p2 = sub.getSub(1);
					if (p2 != null) {
						names[i - 1] = p2.getLeafExpression()
								.getIdentifierName();
					}
				}
			}
		}

		if (param1 != null) {
			s = param1.getLeafExpression().calculate(ctx);
		}

		// 检查sheet名称
		ExcelUtils.checkSheetName(s);

		String opt = option;
		boolean isTitle = false, isAppend = false;
		if (opt != null) {
			if (opt.indexOf('t') != -1)
				isTitle = true;
			if (opt.indexOf('a') != -1)
				isAppend = true;
		}
		int startRow, maxRowCount;
		SheetObject so = null;
		try {
			// startRowAndMaxRow = xo.getStartRowAndMaxRow(s, isTitle,
			// !isAppend);

			if (file.getFileType() == XlsFileObject.TYPE_READ) {
				throw new RQException("xlsexport"
						+ " : xlsopen@r does not support xlsexport");
			}
			so = file.getSheetObject(s, true, !isAppend);
			SheetXls sx = (SheetXls) so;
			startRow = sx.getStartRow(isTitle);
			maxRowCount = sx.getMaxRowCount();
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
		int maxCount = maxRowCount - startRow;
		if (isTitle)
			maxCount--;
		if (maxCount <= 0) {
			return null;
		}

		Sequence seq = null;
		ICursor cursor = null;
		if (src == null) {
			return null;
		} else if (src instanceof Sequence) {
			seq = (Sequence) src;
			if (seq.length() > maxCount) {
				cursor = new MemoryCursor(seq, 1, maxCount + 1);
				seq = null;
			}
		} else if (src instanceof ICursor) {
			cursor = (ICursor) src;
			cursor = new SubCursor(cursor, maxCount);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsexport"
					+ mm.getMessage("function.paramTypeError"));
		}

		try {
			if (seq != null) {
				file.xlsexport(so, seq, exps, names, s, isTitle, isAppend,
						startRow, ctx);
			} else {
				file.xlsexport(so, cursor, exps, names, s, isTitle, isAppend,
						startRow, ctx);
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
		return null;
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