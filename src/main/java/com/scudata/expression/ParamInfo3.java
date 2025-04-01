package com.scudata.expression;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.resources.EngineMessage;

/**
 * 用于解析形如e1:f1:c1, e2:f2:c2...的参数
 * @author RunQian
 *
 */
public class ParamInfo3 {
	private Expression []exps1;
	private Expression []exps2;
	private Expression []exps3;

	// has1 第一个参数是否必须存在， has2 第二个参数是否必须存在
	private ParamInfo3(Expression []exps1, Expression []exps2, Expression []exps3) {
		this.exps1 = exps1;
		this.exps2 = exps2;
		this.exps3 = exps3;
	}

	// 返回第一列的所有表达式
	public Expression[] getExpressions1() {
		return exps1;
	}

	// 返回第二列的所有表达式
	public Expression[] getExpressions2() {
		return exps2;
	}

	// 返回第三列的所有表达式
	public Expression[] getExpressions3() {
		return exps3;
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

	// 返回第三列的所有表达式字符串
	public String[] getExpressionStrs3() {
		Expression []exps = this.exps3;
		int size = exps.length;
		String []strs = new String[size];

		for (int i = 0; i < size; ++i) {
			if (exps[i] != null) {
				strs[i] = exps[i].getIdentifierName();
			}
		}
		return strs;
	}

	public static ParamInfo3 parse(IParam param, String funcName, boolean has1, boolean has2, boolean has3) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(funcName + mm.getMessage("function.missingParam"));
		}

		Expression []exps1;
		Expression []exps2;
		Expression []exps3;

		char type = param.getType();
		if (type == IParam.Comma) { // e1:f1:c1, e2:f2:c2...
			int size = param.getSubSize();
			exps1 = new Expression[size];
			exps2 = new Expression[size];
			exps3 = new Expression[size];

			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					if (has1 || has2 || has3) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(funcName + mm.getMessage("function.invalidParam"));
					}
				} else if (sub.isLeaf()) {
					if (has2 || has3) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(funcName + mm.getMessage("function.invalidParam"));
					}
					exps1[i] = sub.getLeafExpression();
				} else { // :
					int subSize = sub.getSubSize();
					if (subSize > 3) {
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

					if (subSize == 3) {
						IParam sub3 = sub.getSub(2);
						if (sub3 == null) {
							if (has3) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(funcName + mm.getMessage("function.invalidParam"));
							}
						} else {
							exps3[i] = sub3.getLeafExpression();
						}
					} else {
						if (has3) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(funcName + mm.getMessage("function.invalidParam"));
						}
					}
				}
			}
		} else if (type == IParam.Colon) { // e1:f1:c1
			int subSize = param.getSubSize();
			if (subSize > 3) {
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

			exps3 = new Expression[1];
			if (subSize == 3) {
				IParam sub3 = param.getSub(2);
				if (sub3 == null) {
					if (has3) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(funcName + mm.getMessage("function.invalidParam"));
					}
				} else {
					exps3[0] = sub3.getLeafExpression();
				}
			} else {
				if (has3) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(funcName + mm.getMessage("function.invalidParam"));
				}
			}
		} else if (type == IParam.Normal) { // e1
			if (has2 || has3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(funcName + mm.getMessage("function.invalidParam"));
			}

			exps1 = new Expression[] { param.getLeafExpression() };
			exps2 = new Expression[1];
			exps3 = new Expression[1];
		} else { // ;
			MessageManager mm = EngineMessage.get();
			throw new RQException(funcName + mm.getMessage("function.invalidParam"));
		}

		return new ParamInfo3(exps1, exps2, exps3);
	}
}
