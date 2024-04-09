package com.scudata.expression.mfn.xo;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.excel.ExcelUtils;
import com.scudata.excel.XlsFileObject;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.XOFunction;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;

/**
 * xo.xlsmove(s,s’;xo’) 
 * 
 * 把xo中名为s的sheet移动到xo’，命名为s’；
 * xo’省略，表示sheet改名，s’也省略表示删除；
 * xo’未省略，s’省略表示用s的原名
 * 
 * @c 复制
 */
public class XlsMove extends XOFunction {

	/**
	 * 计算
	 */
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsmove"
					+ mm.getMessage("function.missingParam"));
		}

		IParam param0;
		IParam param1 = null;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsmove"
						+ mm.getMessage("function.invalidParam"));
			}

			param0 = param.getSub(0);
			param1 = param.getSub(1);
			if (param0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsmove"
						+ mm.getMessage("function.invalidParam"));
			}
		} else {
			param0 = param;
		}

		Object s = null, s1 = null;

		if (param0.isLeaf()) {
			s = param0.getLeafExpression().calculate(ctx);
		} else if (param0.getType() != IParam.Comma || param0.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsmove"
					+ mm.getMessage("function.invalidParam"));
		} else {
			s = param0.getSub(0).getLeafExpression().calculate(ctx);
			s1 = param0.getSub(1).getLeafExpression().calculate(ctx);
		}

		Object xo1 = null;
		if (param1 != null) {
			xo1 = param1.getLeafExpression().calculate(ctx);
		}

		String opt = option;
		boolean isCopy = false;
		if (opt != null) {
			if (opt.indexOf('c') != -1)
				isCopy = true;
		}

		if (xo1 != null) {
			if (!(xo1 instanceof XlsFileObject)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsmove"
						+ mm.getMessage("function.paramTypeError"));
			}
			if (file == xo1) { // 如果xo和xo'相同，认为是同工作簿
				xo1 = null;
			}
		}

		if (!StringUtils.isValidString(s)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsmove"
					+ mm.getMessage("function.invalidParam"));
		}

		// 检查sheet名称
		ExcelUtils.checkSheetName(s);
		ExcelUtils.checkSheetName(s1);

		// 同工作簿没有s'不能用复制选项
		if (xo1 == null && !StringUtils.isValidString(s1) && isCopy) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsmove"
					+ mm.getMessage("function.invalidParam"));
		}

		if (file.getFileType() != XlsFileObject.TYPE_NORMAL
				|| (xo1 != null && ((XlsFileObject) xo1).getFileType() != XlsFileObject.TYPE_NORMAL)) {
			// : xlsopen@r or @w does not support xlsmove
			throw new RQException("xlsmove"
					+ AppMessage.get().getMessage("filexls.rwcell"));
		}

		try {
			file.xlsmove((String) s,
					StringUtils.isValidString(s1) ? (String) s1 : null,
					(XlsFileObject) xo1, isCopy);
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