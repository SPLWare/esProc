package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CursorUtil;

/**
 * 针对A每一行计算w，再针对w计算x作为新字段F，T为w的别名可用在x中，x是~表示w本身，F若在A中则重新赋值
 * A.fjoin(w:T,x:F,…;…)
 * @author RunQian
 *
 */
public class FJoin extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fjoin" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		Expression[] dimExps;
		String []aliasNames;
		Expression [][]newExps;
		String [][]newNames;
		
		if (param.getType() == IParam.Semicolon) {
			int count = param.getSubSize();
			dimExps = new Expression[count];
			aliasNames = new String[count];
			newExps = new Expression[count][];
			newNames = new String[count][];

			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fjoin" + mm.getMessage("function.invalidParam"));
				}

				parseJoinItem(sub, i, dimExps, aliasNames, newExps, newNames, ctx);
			}
		} else {
			dimExps = new Expression[1];
			aliasNames = new String[1];
			newExps = new Expression[1][];
			newNames = new String[1][];

			parseJoinItem(param, 0, dimExps, aliasNames, newExps, newNames, ctx);
		}
		
		ICursor cs;
		if (option == null || option.indexOf('m') == -1) {
			cs = srcSequence.cursor();
		} else {
			int pathCount = Env.getCursorParallelNum();
			cs = CursorUtil.cursor(srcSequence, pathCount, null, ctx);
		}
		
		cs.fjoin(this, dimExps, aliasNames, newExps, newNames, option, ctx);
		return cs.fetch();
	}
	
	public static void parseJoinItem(IParam param, int index, Expression[] dimExps, String []aliasNames,
			   Expression[][] newExps, String[][] newNames, Context ctx) {
		if (param.isLeaf()) {
			dimExps[index] = param.getLeafExpression();
		} else if (param.getType() == IParam.Colon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fjoin" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fjoin" + mm.getMessage("function.invalidParam"));
			}
			
			dimExps[index] = sub0.getLeafExpression();
			aliasNames[index] = sub1.getLeafExpression().getIdentifierName();
		} else {
			IParam dimParam = param.getSub(0);
			if (dimParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fjoin" + mm.getMessage("function.invalidParam"));
			} else if (dimParam.isLeaf()) {
				dimExps[index] = dimParam.getLeafExpression();
			} else {
				if (dimParam.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fjoin" + mm.getMessage("function.invalidParam"));
				}
				
				IParam sub0 = dimParam.getSub(0);
				IParam sub1 = dimParam.getSub(1);
				if (sub0 == null || sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fjoin" + mm.getMessage("function.invalidParam"));
				}
				
				dimExps[index] = sub0.getLeafExpression();
				aliasNames[index] = sub1.getLeafExpression().getIdentifierName();
			}

			int expCount = param.getSubSize() - 1;
			Expression []tmpExps = new Expression[expCount];
			String []tmpNames = new String[expCount];
			newExps[index] = tmpExps;
			newNames[index] = tmpNames;

			for (int i = 0; i < expCount; ++i) {
				IParam p = param.getSub(i + 1);
				if (p == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fjoin" + mm.getMessage("function.invalidParam"));
				}

				if (p.isLeaf()) {
					tmpExps[i] = p.getLeafExpression();
				} else {
					if (p.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("fjoin" + mm.getMessage("function.invalidParam"));
					}

					IParam sub0 = p.getSub(0);
					if (sub0 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("fjoin" + mm.getMessage("function.invalidParam"));
					}

					tmpExps[i] = sub0.getLeafExpression();
					IParam sub1 = p.getSub(1);
					if (sub1 != null) {
						tmpNames[i] = sub1.getLeafExpression().getIdentifierName();
					}
				}
			}
		}
	}
}
