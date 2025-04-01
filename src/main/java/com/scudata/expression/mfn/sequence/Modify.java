package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 修改序列的成员或者序表的字段值
 * A.modify(k,x) T.modify(k,xi:Fi,…) T.modify(k:A,xi:Fi,…)
 * @author RunQian
 *
 */
public class Modify extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (srcSequence instanceof Table) {
			Table table = (Table)srcSequence;
			Object result;
			if (option == null || (option.indexOf('r') == -1 && option.indexOf('f') == -1)) {
				result = modify(table, ctx);
			} else {
				result = modifyRecord(table, ctx);
			}
			
			table.rebuildIndexTable();
			return result;
		} else {
			return modify(srcSequence, ctx);
		}
	}
	
	private Object modify(Table table, Context ctx) {
		if (param.getType() != IParam.Comma) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.invalidParam"));
		}

		int size = param.getSubSize();
		int pos;
		Sequence srcSeries = null;

		IParam posParam = param.getSub(0);
		if (posParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.invalidParam"));
		} else if (posParam.isLeaf()) {
			Object obj = posParam.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.paramTypeError"));
			}
			pos = ((Number)obj).intValue();
		} else { // :
			if (posParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = posParam.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.paramTypeError"));
			}
			pos = ((Number)obj).intValue();

			IParam sub1 = posParam.getSub(1);
			if (sub1 != null) {
				obj = sub1.getLeafExpression().calculate(ctx);
				if (obj instanceof Sequence) {
					srcSeries = (Sequence)obj;
				} else if (obj instanceof Number) {
					srcSeries = new Sequence(1, ((Number)obj).intValue());
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("modify" + mm.getMessage("function.paramTypeError"));
				}
			}
		}

		int count = size - 1;
		Expression []exps = new Expression[count];
		Expression []optExps = new Expression[count];
		String []names = new String[count];
		for (int i = 0; i < count; ++i) {
			IParam sub = param.getSub(i + 1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				exps[i] = sub.getLeafExpression();
			} else {
				int subSize = sub.getSubSize();
				if (subSize > 3) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("modify" + mm.getMessage("function.invalidParam"));
				}

				IParam sub0 = sub.getSub(0);
				if (sub0 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("modify" + mm.getMessage("function.invalidParam"));
				}
				exps[i] = sub0.getLeafExpression();

				IParam sub1 = sub.getSub(1);
				if (sub1 != null) {
					names[i] = sub1.getLeafExpression().getIdentifierName();
				}

				if (subSize == 3) {
					IParam sub2 = sub.getSub(2);
					if (sub2 != null) {
						optExps[i] = sub2.getLeafExpression();
					}
				}
			}
		}

		if (srcSeries == null) {
			if (option == null || option.indexOf('n') == -1) {
				table.modify(pos, exps, names, ctx);
				return table;
			} else {
				return table.modify(pos, exps, names, ctx);
			}
		} else {
			return table.modify(pos, srcSeries, exps, optExps, names, option, ctx);
		}
	}
	
	private Object modifyRecord(Table table, Context ctx) {
		if (param.getType() != IParam.Colon || param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(1);
		if (sub1 == null || !sub1.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.invalidParam"));
		}
		
		Object val = sub1.getLeafExpression().calculate(ctx);
		Sequence seq;
		int pos = 0;
		if (val instanceof Sequence) {
			seq = (Sequence)val;
		} else if (val != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.paramTypeError"));
		} else {
			seq = null;
		}
		
		IParam sub0 = param.getSub(0);
		if (sub0 != null) {
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.paramTypeError"));
			}
			
			pos = ((Number)obj).intValue();
		}
		
		return table.modify(pos, seq, false, option);
	}
	
	private Object modify(Sequence seq, Context ctx) {
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.invalidParam"));
		}
		
		int pos = 0;
		IParam sub = param.getSub(0);
		if (sub != null) {
			Object posVal = sub.getLeafExpression().calculate(ctx);
			if (!(posVal instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.paramTypeError"));
			}

			pos = ((Number)posVal).intValue();
		}
		
		sub = param.getSub(1);
		if (sub == null || !sub.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.invalidParam"));
		}
		
		Object val = sub.getLeafExpression().calculate(ctx);
		return seq.modify(pos, val, option);
	}
}
