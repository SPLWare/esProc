package com.scudata.expression.fn;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.dm.Context;
import com.scudata.dm.KeyWord;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * 程序网里定义的函数的调用 fn(arg)fn为程序网中定义的函数的名字
 * 
 * @author runqian
 *
 */
public class PCSFunction extends Function {
	private PgmCellSet.FuncInfo funcInfo;

	public PCSFunction(PgmCellSet.FuncInfo funcInfo) {
		this.funcInfo = funcInfo;
	}

	public Node optimize(Context ctx) {
		if (param != null)
			param.optimize(ctx);
		return this;
	}

	private Object executeMacro(String macroExp, Context ctx) {
		String []argNames = funcInfo.getArgNames();
		if (argNames == null) {
			Expression exp = new Expression(cs, ctx, macroExp);
			return exp.calculate(ctx);
		}
		
		int pcount = argNames.length;
		String []argExps = new String[pcount];
		
		if (pcount == 1) {
			if (param == null || !param.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(funcInfo.getFnName() + mm.getMessage("function.invalidParam"));
			}
			
			argExps[0] = param.getLeafExpression().toString();
		} else {
			if (param.getSubSize() != pcount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(funcInfo.getFnName() + mm.getMessage("function.invalidParam"));
			}
			
			for (int i = 0; i < pcount; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(funcInfo.getFnName() + mm.getMessage("function.invalidParam"));
				}
				
				argExps[i] = sub.getLeafExpression().toString();
			}
		}
		
		macroExp = replaceMacros(macroExp, argNames, argExps);
		Expression exp = new Expression(cs, ctx, macroExp);
		return exp.calculate(ctx);
	}
	
	private static String replaceMacros(String text, String []argNames, String []argExps) {
		int len = text.length();
		StringBuffer newStr = new StringBuffer(len + 32);
		int argCount = argNames.length;
		int idx = 0;
		
		Next:
		while(idx < len) {
			char c = text.charAt(idx);
			// 字符串中的$不做替换
			if (c == '\'' || c == '\"') {
				int match = Sentence.scanQuotation(text, idx);
				if (match < 0) {
					if (newStr != null) newStr.append(c);
					idx += 1;
				} else {
					if (newStr != null) newStr.append(text.substring(idx, match + 1));
					idx = match + 1;
				}
			} else if (KeyWord.isSymbol(c)) {
				if (newStr != null) newStr.append(c);
				idx += 1;
			} else {
				int last = KeyWord.scanId(text, idx + 1);
				String subStr = text.substring(idx, last);
				idx = last;
				
				for (int i = 0; i < argCount; ++i) {
					if (argNames[i].equals(subStr)) {
						newStr.append(argExps[i]);
						continue Next;
					}
				}
				
				newStr.append(subStr);
			}
		}

		return newStr.toString();
	}
	
	public Object calculate(Context ctx) {
		String macroExp = funcInfo.getMacroExpression();
		if (macroExp != null) {
			return executeMacro(macroExp, ctx);
		}
		
		Object[] args = funcInfo.getDefaultValues();
		boolean hasOptParam = funcInfo.hasOptParam();
		
		if (param != null) {
			if (hasOptParam) {
				if (param.isLeaf()) {
					if (args == null || args.length < 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(funcInfo.getFnName() + mm.getMessage("function.invalidParam"));
					}
					
					args[0] = option;
					if (funcInfo.isMacroArg(1)) {
						args[1] = param.getLeafExpression().toString();
					} else {
						args[1] = param.getLeafExpression().calculate(ctx);
					}
				} else {
					int size = param.getSubSize();
					if (args == null || args.length < size + 1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(funcInfo.getFnName() + mm.getMessage("function.invalidParam"));
					}
					
					args[0] = option;
					for (int i = 0; i < size; ++i) {
						IParam sub = param.getSub(i);
						if (sub != null) {
							if (funcInfo.isMacroArg(i + 1)) {
								args[i + 1] = sub.getLeafExpression().toString();
							} else {
								args[i + 1] = sub.getLeafExpression().calculate(ctx);
							}
						}
					}
				}
			} else {
				if (param.isLeaf()) {
					if (args == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(funcInfo.getFnName() + mm.getMessage("function.invalidParam"));
					}
					
					if (funcInfo.isMacroArg(0)) {
						args[0] = param.getLeafExpression().toString();
					} else {
						args[0] = param.getLeafExpression().calculate(ctx);
					}
				} else {
					int size = param.getSubSize();
					if (args == null || args.length < size) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(funcInfo.getFnName() + mm.getMessage("function.invalidParam"));
					}

					for (int i = 0; i < size; ++i) {
						IParam sub = param.getSub(i);
						if (sub != null) {
							if (funcInfo.isMacroArg(i)) {
								args[i] = sub.getLeafExpression().toString();
							} else {
								args[i] = sub.getLeafExpression().calculate(ctx);
							}
						}
					}
				}
			}
		} else if (hasOptParam) {
			if (args == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(funcInfo.getFnName() + mm.getMessage("function.invalidParam"));
			}
			
			args[0] = option;
		}

		return funcInfo.execute(args, option, ctx);
	}

	/**
	 * 取函数调用信息
	 * 
	 * @return
	 */
	public PgmCellSet.FuncInfo getFuncInfo() {
		return funcInfo;
	}

	/**
	 * 
	 * @param ctx
	 * @return
	 */
	public Object[] prepareArgs(Context ctx) {
		Object[] args = null;
		if (param != null) {
			if (param.isLeaf()) {
				Object val = param.getLeafExpression().calculate(ctx);
				args = new Object[] { val };
			} else {
				int size = param.getSubSize();
				args = new Object[size];

				for (int i = 0; i < size; ++i) {
					IParam sub = param.getSub(i);
					if (sub != null) {
						args[i] = sub.getLeafExpression().calculate(ctx);
					}
				}
			}
		}
		return args;
	}
}
