package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 为序列或序表插入元素
 * A.insert(k,x) T.insert(k) T.insert(k,xi:Fi,…) T.insert(k:A,xi:Fi,…) T.insert(k:A)
 * @author RunQian
 *
 */
public class Insert extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.missingParam"));
		}
		
		if (srcSequence instanceof Table) {
			Object result;
			if (option == null || (option.indexOf('r') == -1 && option.indexOf('f') == -1)) {
				result = insert((Table)srcSequence, ctx);
			} else {
				result = insertRecord((Table)srcSequence, ctx);
			}
			
			srcSequence.rebuildIndexTable();
			return result;
		} else {
			return insert(srcSequence, ctx);
		}
	}

	private Object insert(Sequence seq, Context ctx) {
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub = param.getSub(1);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.invalidParam"));
		}
		
		Object val = sub.getLeafExpression().calculate(ctx);		
		
		sub = param.getSub(0);
		if (sub != null) {
			Object posVal = sub.getLeafExpression().calculate(ctx);
			if (!(posVal instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("insert" + mm.getMessage("function.paramTypeError"));
			}

			seq.insert(((Number)posVal).intValue(), val);
		} else {
			// k未写时，假定A有序，插入x，已存在则不插入
			seq.sortedInsert(val);
		}
		
		if (option == null || option.indexOf('n') == -1) {
			return seq;
		} else {
			return val;
		}
	}
	
	private Object insert(Table table, Context ctx) {
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("insert" + mm.getMessage("function.paramTypeError"));
			}

			if (option == null || option.indexOf('n') == -1) {
				table.insert(((Number)obj).intValue());
				return table;
			} else {
				return table.insert(((Number)obj).intValue());
			}
		} else if (param.getType() == IParam.Colon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("insert" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("insert" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("insert" + mm.getMessage("function.paramTypeError"));
			}
			
			int pos = ((Number)obj).intValue();
			IParam sub1 = param.getSub(1);
			int count = 1;
			if (sub1 != null) {
				obj = sub1.getLeafExpression().calculate(ctx);
				if (obj instanceof Sequence) {
					count = ((Sequence)obj).length();
				} else if (obj instanceof Number) {
					count = ((Number)obj).intValue();
				} else if (obj == null) {
					count = 0;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("insert" + mm.getMessage("function.paramTypeError"));
				}
			}

			return table.insert(pos, count, option);
		} else if (param.getType() == IParam.Comma) {
			int pos = 0;
			Sequence srcSeries = null;
			boolean hasPos = true;

			IParam posParam = param.getSub(0);
			if (posParam == null) {
				hasPos = false;
			} else if (posParam.isLeaf()) {
				Object obj = posParam.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("insert" + mm.getMessage("function.paramTypeError"));
				}
				
				pos = ((Number)obj).intValue();
			} else { // :
				if (posParam.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("insert" + mm.getMessage("function.invalidParam"));
				}

				IParam sub0 = posParam.getSub(0);
				if (sub0 == null) {
					hasPos = false;
				} else {
					Object obj = sub0.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("insert" + mm.getMessage("function.paramTypeError"));
					}
					pos = ((Number)obj).intValue();
				}

				IParam sub1 = posParam.getSub(1);
				if (sub1 != null) {
					Object obj = sub1.getLeafExpression().calculate(ctx);
					if (obj instanceof Sequence) {
						srcSeries = (Sequence)obj;
					} else if (obj instanceof Number) {
						srcSeries = new Sequence(1, ((Number)obj).intValue());
					} else if (obj == null) {
						return table.insert(pos, 0, option);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("insert" + mm.getMessage("function.paramTypeError"));
					}
				}
			}

			int count = param.getSubSize() - 1;
			Expression []exps = new Expression[count];
			Expression []optExps = new Expression[count];
			String []names = new String[count];
			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i + 1);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("insert" + mm.getMessage("function.invalidParam"));
				} else if (sub.isLeaf()) {
					exps[i] = sub.getLeafExpression();
				} else {
					int size = sub.getSubSize();
					if (size > 3) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("insert" + mm.getMessage("function.invalidParam"));
					}

					IParam sub0 = sub.getSub(0);
					if (sub0 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("insert" + mm.getMessage("function.invalidParam"));
					}
					exps[i] = sub0.getLeafExpression();

					IParam sub1 = sub.getSub(1);
					if (sub1 != null) {
						names[i] = sub1.getLeafExpression().getIdentifierName();
					}

					if (size == 3) {
						IParam sub2 = sub.getSub(2);
						if (sub2 != null) {
							optExps[i] = sub2.getLeafExpression();
						}
					}
				}
			}

			if (srcSeries == null) {
				if (hasPos) {
					if (option == null || option.indexOf('n') == -1) {
						table.insert(pos, exps, names, ctx);
						return table;
					} else {
						return table.insert(pos, exps, names, ctx);
					}
				} else {
					if (option == null || option.indexOf('n') == -1) {
						table.sortedInsert(exps, names, ctx);
						return table;
					} else {
						return table.sortedInsert(exps, names, ctx);
					}
				}
			} else {
				if (hasPos) {
					return table.insert(pos, srcSeries, exps, optExps, names, option, ctx);
				} else {
					return table.sortedInsert(srcSeries, exps, names, option, ctx);
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.invalidParam"));
		}
	}
	
	private Object insertRecord(Table table, Context ctx) {
		if (param.getType() != IParam.Colon || param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(1);
		if (sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.invalidParam"));
		}
		
		Object val = sub1.getLeafExpression().calculate(ctx);
		Sequence seq;
		int pos = 0;
		if (val instanceof Sequence) {
			seq = (Sequence)val;
		} else if (val instanceof Record) {
			seq = new Sequence(1);
			seq.add(val);
		} else if (val != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.paramTypeError"));
		} else {
			seq = null;
		}
		
		IParam sub0 = param.getSub(0);
		if (sub0 != null) {
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("insert" + mm.getMessage("function.paramTypeError"));
			}
			
			pos = ((Number)obj).intValue();
			return table.modify(pos, seq, true, option);
		} else {
			return table.sortedInsert(seq, option);
		}
	}
}
