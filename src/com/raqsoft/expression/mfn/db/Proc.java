package com.raqsoft.expression.mfn.db;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.DBFunction;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.DatabaseUtil;

/**
 * 调用存储过程计算，返回的结果集由参数中的v带出
 * db.proc(sql,a:t:m:v,…)
 * @author RunQian
 *
 */
public class Proc extends DBFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("proc" + mm.getMessage("function.missingParam"));
		}

		String strSql;
		Object[] sqlParams = null;
		byte[] types = null;
		byte[] modes = null;
		String[] outParams = null;

		char type = param.getType();
		if (type == IParam.Normal) { // 没有参数
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("proc" + mm.getMessage("function.paramTypeError"));
			}

			strSql = (String)obj;
		} else if (type == IParam.Comma) {
			IParam sub0 = param.getSub(0); // sql表达式
			if (sub0 == null || !sub0.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("proc" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("proc" + mm.getMessage("function.paramTypeError"));
			}

			strSql = (String)obj;

			int paramSize = param.getSubSize() - 1; // 参数个数
			sqlParams = new Object[paramSize];
			types = new byte[paramSize];
			modes = new byte[paramSize];
			outParams = new String[paramSize];

			for (int i = 0; i < paramSize; ++i) {
				modes[i] = DatabaseUtil.PROC_MODE_IN;
				IParam sub = param.getSub(i + 1);
				if (sub == null) continue;

				if (sub.isLeaf()) { // 只有参数没有指定类型
					sqlParams[i] = sub.getLeafExpression().calculate(ctx);
				} else {
					int size = sub.getSubSize();
					if (size > 4) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("proc" + mm.getMessage("function.invalidParam"));
					}

					IParam subi0 = sub.getSub(0); // 参数值
					IParam subi1 = sub.getSub(1); // 参数类型
					if (subi0 != null) sqlParams[i] = subi0.getLeafExpression().calculate(ctx);
					if (subi1 != null) {
						Object tmp = subi1.getLeafExpression().calculate(ctx);
						if (!(tmp instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("proc" + mm.getMessage("function.paramTypeError"));
						}

						types[i] = ((Number)tmp).byteValue();
					}

					if (size > 2) {
						IParam subi2 = sub.getSub(2); // 输入输出模式
						if (subi2 != null) {
							Object tmp = subi2.getLeafExpression().calculate(ctx);
							if (!(tmp instanceof String)) {
								MessageManager mm = EngineMessage.get();
								throw new RQException("proc" + mm.getMessage("function.paramTypeError"));
							}

							// 默认为输入参数
							String modeStr = (String)tmp;
							if (modeStr.indexOf('i') != -1) {
								if (modeStr.indexOf('o') != -1) {
									modes[i] = DatabaseUtil.PROC_MODE_INOUT;
								}
							} else {
								if (modeStr.indexOf('o') != -1) {
									modes[i] = DatabaseUtil.PROC_MODE_OUT;
								}
							}
						}
					}

					if (size > 3) {
						IParam subi3 = sub.getSub(3); // 输出参数名
						if (subi3 != null) {
							outParams[i] = subi3.getLeafExpression().getIdentifierName();
						}
					}
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("proc" + mm.getMessage("function.invalidParam"));
		}

		return db.proc(strSql, sqlParams, types, modes, outParams, ctx);
	}
}
