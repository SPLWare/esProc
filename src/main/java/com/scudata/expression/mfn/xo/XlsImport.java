package com.scudata.expression.mfn.xo;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.XOFunction;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;

/**
 * xo.xlsimport(Fi,..;s,b:e) 从页s中取出序表
 * 
 * @t 首行是标题，有b参数时认为标题在b行
 * @c 返回成游标，xo必须用@r打开，此时e不能是负数
 * @b 去除前后的空白行，@c时不支持
 */
public class XlsImport extends XOFunction {

	/**
	 * 计算
	 */
	public Object calculate(Context ctx) {
		String opt = option;

		com.scudata.expression.mfn.file.XlsImport.checkOptions(opt);

		boolean isCursor = opt != null && opt.indexOf("c") > -1;
		if (isCursor && !file.supportCursor()) {
			MessageManager mm = AppMessage.get();
			throw new RQException("xlsimport"
					+ mm.getMessage("filexls.coptwithr"));
		}

		if (file.supportCursor()) {
			boolean isW = opt != null && opt.indexOf("w") > -1;
			boolean isS = opt != null && opt.indexOf("s") > -1;
			String wOrSText = isW ? "w" : "s";
			if (isW || isS) {
				MessageManager mm = AppMessage.get();
				throw new RQException("xlsimport"
						+ mm.getMessage("filexls.wswithr", wOrSText));
			}
		}

		if (param == null) {
			try {
				return file.xlsimport(opt);
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}

		String[] fields = null;
		Object s = null;
		int start = 0;
		int end = 0;

		IParam fieldParam;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsimport"
						+ mm.getMessage("function.invalidParam"));
			}

			fieldParam = param.getSub(0);
			IParam param1 = param.getSub(1);
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
		if (s != null) {
			if (!(s instanceof String) && !(s instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsimport"
						+ mm.getMessage("function.paramTypeError"));
			}
		}
		com.scudata.expression.mfn.file.XlsImport
				.checkFieldOptions(fields, opt);

		try {
			return file.xlsimport(fields, s, start, end, opt);
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 对节点做优化
	 * 
	 * @param ctx
	 *            计算上下文
	 * @param Node
	 *            优化后的节点
	 */
	public Node optimize(Context ctx) {
		if (param != null) {
			// 对参数做优化
			param.optimize(ctx);
		}

		return this;
	}
}
