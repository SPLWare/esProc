package com.scudata.expression.mfn.file;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.excel.FileXls;
import com.scudata.excel.FileXlsR;
import com.scudata.excel.XlsFileObject;
import com.scudata.expression.FileFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;

/**
 * 函数f.xlsopen(p) 读出Excel文件f返回成对象， p是密码；返回对象可作为只读序表
 * stname（页名）,nrows（行数）,ncols（列数）
 * 
 * @r 流式读，对于程序导出的xls有可能行列数返回不正确
 * @w 流式写，此时不能返回索引信息，与@r互斥
 * 
 *
 */
public class XlsOpen extends FileFunction {

	/**
	 * 计算
	 */
	public Object calculate(Context ctx) {
		String opt = option;
		boolean isR = opt != null && opt.indexOf("r") > -1;
		boolean isW = opt != null && opt.indexOf("w") > -1;

		if (isR && isW) {
			// @w与@r互斥ss
			MessageManager mm = AppMessage.get();
			throw new RQException("xlsopen" + mm.getMessage("filexls.notrw")); // ：选项w与r不能同时设置
		}

		if (param == null) {
			// 返回Excel文件对象
			return xlsOpen(isR, isW);
		}

		String pwd = null;
		if (param.getType() != IParam.Normal) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsopen"
					+ mm.getMessage("function.invalidParam"));
		} else {
			if (!param.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsopen"
						+ mm.getMessage("function.invalidParam"));
			}

			IParam pwdParam = param;
			if (pwdParam != null) {
				Object tmp = pwdParam.getLeafExpression().calculate(ctx);
				if (tmp != null) {
					pwd = tmp.toString();
				}
				if ("".equals(pwd))
					pwd = null;
			}
		}
		try {
			return xlsOpen(pwd, isR, isW);
		} catch (RQException e) {
			throw e;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 创建xo文件对象
	 * 
	 * @param isR
	 * @param isW
	 * @return
	 */
	private XlsFileObject xlsOpen(boolean isR, boolean isW) {
		return xlsOpen(null, isR, isW);
	}

	/**
	 * 创建xo文件对象
	 * 
	 * @param pwd
	 *            密码
	 * @param isR
	 *            选项@r
	 * @param isW
	 * @return
	 */
	private XlsFileObject xlsOpen(String pwd, boolean isR, boolean isW) {
		if (isR) {
			return new FileXlsR(file, pwd);
		}
		byte type;
		if (isW) {
			type = XlsFileObject.TYPE_WRITE;
		} else {
			type = XlsFileObject.TYPE_NORMAL;
		}
		return new FileXls(file, pwd, type);
	}
}
