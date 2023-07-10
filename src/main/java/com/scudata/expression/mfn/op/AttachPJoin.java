package com.scudata.expression.mfn.op;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.OperableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 有序游标上做主键式关联
 * cs.pjoin(K:..,x:F,…;Ai:z,K:…,x:F,…; …)
 * @author RunQian
 *
 */
public class AttachPJoin extends OperableFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pjoin" + mm.getMessage("function.missingParam"));
		} else if (param.getType() != IParam.Semicolon) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam sub = param.getSub(0);
		IParam keyParam;
		Expression []srcNewExps = null;
		String []srcNewNames = null;
		
		if (sub.getType() == IParam.Comma) {
			// K:..,x:F,…
			keyParam = sub.getSub(0);
			if (keyParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
			}
			
			int newSize = sub.getSubSize() - 1;
			srcNewExps = new Expression[newSize];
			srcNewNames = new String[newSize];
			
			for (int i = 0; i < newSize; ++i) {
				IParam newParam = sub.getSub(i + 1);
				if (newParam == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
				} else if (newParam.isLeaf()) {
					srcNewExps[i] = newParam.getLeafExpression();
					srcNewNames[i] = srcNewExps[i].getFieldName();
				} else if (newParam.getSubSize() == 2) {
					IParam sub0 = newParam.getSub(0);
					IParam sub1 = newParam.getSub(1);
					if (sub0 == null || sub1 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
					}
					
					srcNewExps[i] = sub0.getLeafExpression();
					srcNewNames[i] = sub1.getLeafExpression().getIdentifierName();
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
				}
			}
		} else {
			keyParam = sub;
		}
		
		int keyCount;
		Expression []srcKeyExps;
		
		if (keyParam.isLeaf()) {
			keyCount = 1;
			srcKeyExps = new Expression[] {keyParam.getLeafExpression()};
		} else {
			keyCount = keyParam.getSubSize();
			srcKeyExps = new Expression[keyCount];
			
			for (int i = 0; i < keyCount; ++i) {
				sub = keyParam.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
				}
				
				srcKeyExps[i] = sub.getLeafExpression();
			}
		}
		
		int tableCount = param.getSubSize() - 1;
		ICursor []cursors = new ICursor[tableCount]; // 关联游标数组
		String []options = new String[tableCount];
		Expression [][]keyExps = new Expression[tableCount][];
		Expression [][]newExps = new Expression[tableCount][];
		String [][]newNames = new String[tableCount][];
		
		for (int t = 0; t < tableCount; ++t) {
			IParam tableParam = param.getSub(t + 1);
			if (tableParam == null || tableParam.getType() != IParam.Comma) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
			}
			
			// Ai:z,K:…,x:F,…
			Object table;
			sub = tableParam.getSub(0);
			
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				table = sub.getLeafExpression().calculate(ctx);
			} else if (sub.getSubSize() == 2) {
				IParam sub0 = sub.getSub(0);
				IParam sub1 = sub.getSub(1);
				if (sub0 == null || sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
				}
				
				table = sub0.getLeafExpression().calculate(ctx);
				options[t] = sub1.getLeafExpression().toString();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
			}
			
			if (table instanceof ICursor) {
				cursors[t] = (ICursor)table;
			} else if (table instanceof Sequence) {
				cursors[t] = ((Sequence)table).cursor();
			} else if (table == null) {
				cursors[t] = new MemoryCursor(null);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pjoin" + mm.getMessage("function.paramTypeError"));
			}
			
			keyParam = tableParam.getSub(1);
			if (keyParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
			} else if (keyParam.isLeaf()) {
				if (keyCount != 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pjoin" + mm.getMessage("function.paramCountNotMatch"));
				}
				
				keyExps[t] = new Expression[] {keyParam.getLeafExpression()};
			} else {
				if (keyCount != keyParam.getSubSize()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pjoin" + mm.getMessage("function.paramCountNotMatch"));
				}
				
				keyExps[t] = new Expression[keyCount];
				for (int i = 0; i < keyCount; ++i) {
					sub = keyParam.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
					}
					
					keyExps[t][i] = sub.getLeafExpression();
				}
			}
			
			int newSize = tableParam.getSubSize() - 2;
			if (newSize > 0) {
				newExps[t] = new Expression[newSize];
				newNames[t] = new String[newSize];
				
				for (int i = 0; i < newSize; ++i) {
					IParam newParam = tableParam.getSub(i + 2);
					if (newParam == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
					} else if (newParam.isLeaf()) {
						newExps[t][i] = newParam.getLeafExpression();
						newNames[t][i] = newExps[t][i].getFieldName();
					} else if (newParam.getSubSize() == 2) {
						IParam sub0 = newParam.getSub(0);
						IParam sub1 = newParam.getSub(1);
						if (sub0 == null || sub1 == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
						}
						
						newExps[t][i] = sub0.getLeafExpression();
						newNames[t][i] = sub1.getLeafExpression().getIdentifierName();
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
					}
				}
			}
		}
		
		return operable.pjoin(this, srcKeyExps, srcNewExps, srcNewNames, 
				cursors, options, keyExps, newExps, newNames, option, ctx);
	}
}
