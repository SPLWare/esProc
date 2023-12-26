package com.scudata.expression.mfn.xo;

import com.scudata.common.CellLocation;
import com.scudata.common.Escape;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.excel.ExcelUtils;
import com.scudata.excel.XlsFileObject;
import com.scudata.expression.IParam;
import com.scudata.expression.XOFunction;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;

/**
 * 函数xo.xlscell(a:b,s;t)。向页s的格a处填入串t，t是回车/tab分隔的串或序列的序列，分别填入相邻行和列中；s省略为第一页，xo不能以@r@w方式打开
 * 无t参数时，读出从a到b的格文本形式串返回，只有a只读a格，a:读到结尾；a:b省略表示改页s的名称为t。
 * 
 * @i 行插入式填入，缺省是覆盖式
 * @w 读出时返回成格值的序列的序列。
 * @p @w加转置，序列的序列先列后行，是串时忽略
 * @n 取出格文字时做trim，@w时把空串读成null
 * @g 读出与设置图片，无:b参数，t是个blob，目前支持jpg/png格式
 * 
 *
 */
public class XlsCell extends XOFunction {
	/**
	 * 计算
	 */
	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlscell"
					+ mm.getMessage("function.missingParam"));
		}

		IParam sheetParam;
		Object content = null; // 可能是串也可能是图片
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) { // 兼容一下之前的
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlscell"
						+ mm.getMessage("function.invalidParam"));
			}
			sheetParam = param.getSub(0);
			IParam contentParam = param.getSub(1);
			if (contentParam != null) {
				content = contentParam.getLeafExpression().calculate(ctx);
			}
		} else {
			sheetParam = param;
		}
		IParam param0;
		IParam param1;
		if (sheetParam.isLeaf() || sheetParam.getType() == IParam.Colon) { // 省略s
			param0 = sheetParam;
			param1 = null;
		} else if (sheetParam.getType() != IParam.Comma
				|| sheetParam.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlscell"
					+ mm.getMessage("function.invalidParam"));
		} else {
			param0 = sheetParam.getSub(0);
			param1 = sheetParam.getSub(1);
		}
		Object s = null;
		if (param1 != null) {
			s = param1.getLeafExpression().calculate(ctx);
		} else {
			s = 1;
		}

		if (param0 == null) {
			// 没有a:b,是sheet改名
			if (!StringUtils.isValidString(content)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlscell"
						+ mm.getMessage("function.invalidParam"));
			}

			// 检查sheet名称
			ExcelUtils.checkSheetName(content);

			try {
				file.rename(s, (String) content);
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
			return null;
		}

		if (param0 == null || (param1 != null && !param1.isLeaf())) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlscell"
					+ mm.getMessage("function.invalidParam"));
		}

		String cell1, cell2;
		if (param0.isLeaf()) {
			Object val = param0.getLeafExpression().calculate(ctx); // .toString();
			if (!(val instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlscell"
						+ mm.getMessage("function.paramTypeError"));
			}

			cell1 = (String) val;
			cell1 = removeQuota(cell1);
			cell2 = cell1; // :b都没有，表示a单格子
		} else {
			IParam sub = param0.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlscell"
						+ mm.getMessage("function.invalidParam"));
			}

			Object val = sub.getLeafExpression().calculate(ctx); // .toString();
			if (!(val instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlscell"
						+ mm.getMessage("function.paramTypeError"));
			}

			cell1 = (String) val;
			cell1 = removeQuota(cell1);
			sub = param0.getSub(1);
			if (sub != null) {
				val = sub.getLeafExpression().calculate(ctx); // .toString();
				if (!(val instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xlscell"
							+ mm.getMessage("function.paramTypeError"));
				}
				cell2 = (String) val;
				cell2 = removeQuota(cell2);
			} else {
				cell2 = null; // a:，表示取到行尾
			}
		}
		String opt = option;
		boolean isRowInsert = opt != null && opt.indexOf('i') != -1;
		boolean isGraph = opt != null && opt.indexOf('g') != -1;
		boolean isW = opt != null && opt.indexOf('w') != -1;
		boolean isP = opt != null && opt.indexOf("p") > -1;
		boolean isN = opt != null && opt.indexOf("n") > -1;
		if (!isW) {
			if (isP) {
				// 选项@{0}只能和选项@w同时使用。
				throw new RQException(AppMessage.get().getMessage(
						"xlsimport.pnnotw", "p"));
			}
		}
		Object matrix;
		if (content == null || "".equals(content.toString().trim())) { // 如果t是空的，就认为把c置为空
			matrix = null;
		} else {
			if (isGraph) {
				if (!(content instanceof byte[])) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xlscell"
							+ mm.getMessage("function.paramTypeError"));
				}
				matrix = content;
			} else if (content instanceof Sequence) { // 序列的序列
				matrix = content;
			} else if (content instanceof BaseRecord) {
				Sequence seq = new Sequence();
				seq.add(content);
				matrix = seq;
			} else if (content instanceof String) {
				matrix = ExcelUtils.getStringMatrix((String) content, true);
			} else {
				Matrix m = new Matrix(1, 1);
				m.set(0, 0, content);
				matrix = m;
				// MessageManager mm = EngineMessage.get();
				// throw new RQException("xlscell"
				// + mm.getMessage("function.paramTypeError"));
			}
		}

		if (matrix != null)
			if (matrix instanceof Sequence) {
				if (isP) {
					matrix = ExcelUtils.transpose((Sequence) matrix);
				}
			}

		CellLocation pos1 = CellLocation.parse(cell1);
		if (pos1 == null) {
			throw new RQException(AppMessage.get().getMessage(
					"excel.invalidcell", cell1));
		}
		CellLocation pos2 = null;
		if (StringUtils.isValidString(cell2)) {
			pos2 = CellLocation.parse(cell2);
			if (pos2 == null) {
				throw new RQException(AppMessage.get().getMessage(
						"excel.invalidcell", cell1));
			}
		}
		if (isGraph) {
			if (pos2 != null) {
				if (pos2.getRow() != pos1.getRow()
						|| pos2.getCol() != pos1.getCol())
					throw new RQException(AppMessage.get().getMessage(
							"excel.graphwithb"));
			}
		}

		if (file.getFileType() != XlsFileObject.TYPE_NORMAL) {
			// : xlsopen@r or @w does not support xlscell
			throw new RQException("xlscell"
					+ AppMessage.get().getMessage("filexls.rwcell"));
		}

		try {
			return file.xlscell(pos1, pos2, s, matrix, isRowInsert, isGraph,
					isW, isP, isN);
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}

	}

	/**
	 * 删除格子的引号
	 * 
	 * @param cell
	 * @return
	 */
	private String removeQuota(String cell) {
		if (cell == null)
			return null;
		cell = Escape.removeEscAndQuote(cell, '\'');
		return Escape.removeEscAndQuote(cell, '\"');
	}
}
