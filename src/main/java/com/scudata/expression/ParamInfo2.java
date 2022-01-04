package com.scudata.expression;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.resources.EngineMessage;

/**
 * 用于解析形如e1:f1, e2:f2...的参数
 * @author RunQian
 *
 */
public class ParamInfo2 {
	private Expression []exps1;
	private Expression []exps2;

	// has1 第一个参数是否必须存在， has2 第二个参数是否必须存在
	private ParamInfo2(Expression []exps1, Expression []exps2) {
		this.exps1 = exps1;
		this.exps2 = exps2;
	}

	// 返回第一列的所有表达式
	public Expression[] getExpressions1() {
		return exps1;
	}

	// 返回第二列的所有表达式
	public Expression[] getExpressions2() {
		return exps2;
	}

	// 返回第一列的所有表达式字符串
	public String[] getExpressionStrs1() {
		Expression []exps = this.exps1;
		int size = exps.length;
		String []strs = new String[size];

		for (int i = 0; i < size; ++i) {
			if (exps[i] != null) {
				strs[i] = exps[i].getIdentifierName();
			}
		}
		return strs;
	}

	// 返回第二列的所有表达式字符串
	public String[] getExpressionStrs2() {
		Expression []exps = this.exps2;
		int size = exps.length;
		String []strs = new String[size];

		for (int i = 0; i < size; ++i) {
			if (exps[i] != null) {
				strs[i] = exps[i].getIdentifierName();
			}
		}
		return strs;
	}

	// 返回第一列的所有表达式的计算值
	public Object[] getValues1(Context ctx) {
		Expression []exps = this.exps1;
		int size = exps.length;
		Object []vals = new Object[size];

		for (int i = 0; i < size; ++i) {
			if (exps[i] != null) {
				vals[i] = exps[i].calculate(ctx);
			}
		}
		return vals;
	}

	// 返回第二列的所有表达式的计算值
	public Object[] getValues2(Context ctx) {
		Expression []exps = this.exps2;
		int size = exps.length;
		Object []vals = new Object[size];

		for (int i = 0; i < size; ++i) {
			if (exps[i] != null) {
				vals[i] = exps[i].calculate(ctx);
			}
		}
		return vals;
	}

	public static ParamInfo2 parse(IParam param, String funcName, boolean has1, boolean has2) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(funcName + mm.getMessage("function.missingParam"));
		}

		Expression []exps1;
		Expression []exps2;

		char type = param.getType();
		if (type == IParam.Comma) { // e1:f1, e2:f2...
			int size = param.getSubSize();
			exps1 = new Expression[size];
			exps2 = new Expression[size];

			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					if (has1 || has2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(funcName + mm.getMessage("function.invalidParam"));
					}
				} else if (sub.isLeaf()) {
					if (has2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(funcName + mm.getMessage("function.invalidParam"));
					}
					exps1[i] = sub.getLeafExpression();
				} else { // :
					if (sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(funcName + mm.getMessage("function.invalidParam"));
					}

					IParam sub1 = sub.getSub(0);
					if (sub1 == null) {
						if (has1) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(funcName + mm.getMessage("function.invalidParam"));
						}
					} else {
						exps1[i] = sub1.getLeafExpression();
					}

					IParam sub2 = sub.getSub(1);
					if (sub2 == null) {
						if (has2) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(funcName + mm.getMessage("function.invalidParam"));
						}
					} else {
						exps2[i] = sub2.getLeafExpression();
					}
				}
			}
		} else if (type == IParam.Colon) { // e1:f1
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(funcName + mm.getMessage("function.invalidParam"));
			}

			exps1 = new Expression[1];
			IParam sub1 = param.getSub(0);
			if (sub1 == null) {
				if (has1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(funcName + mm.getMessage("function.invalidParam"));
				}
			} else {
				exps1[0] = sub1.getLeafExpression();
			}

			exps2 = new Expression[1];
			IParam sub2 = param.getSub(1);
			if (sub2 == null) {
				if (has2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(funcName + mm.getMessage("function.invalidParam"));
				}
			} else {
				exps2[0] = sub2.getLeafExpression();
			}
		} else if (type == IParam.Normal) { // e1
			if (has2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(funcName + mm.getMessage("function.invalidParam"));
			}

			exps1 = new Expression[] { param.getLeafExpression() };
			exps2 = new Expression[1];
		} else { // ;
			MessageManager mm = EngineMessage.get();
			throw new RQException(funcName + mm.getMessage("function.invalidParam"));
		}

		return new ParamInfo2(exps1, exps2);
	}
}
