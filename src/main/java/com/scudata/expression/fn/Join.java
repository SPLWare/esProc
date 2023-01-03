package com.scudata.expression.fn;

import java.util.ArrayList;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CursorUtil;

/**
 * 多序列叉乘
 * 将多个序列Ai按照关联字段/关联表达式xj和x1相等的关系叉乘，产生以Fi,…为字段的序表，Fi为引用字段，
 * 引用原排列Ai中的记录，xj全省略时用Ai的主键。某个xj 省略时该项条件不必匹配。
 * join(Ai:Fi,xj,..;…) join@x(csi:Fi,xj,..;…)
 * @author runqian
 *
 */
public class Join extends Function {
	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("join" + mm.getMessage("function.missingParam"));
		} else if (param.getType() != IParam.Semicolon) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("join" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		boolean isPJoin = false, isIsect = false, isDiff = false;
		if (option != null) {
			if (option.indexOf('p') != -1) {
				isPJoin = true;
			} else if (option.indexOf('i') != -1) {
				isIsect = true;
			} else if (option.indexOf('d') != -1) {
				isDiff = true;
			}
		}
		
		int count = param.getSubSize();
		Sequence []srcSeries = new Sequence[count];
		String []names = new String[count];
		Expression [][]exps = new Expression[count][];
		boolean isMix = false;

		for (int i = 0; i < count; ++i) {
			// Ai:Fi,xj,..
			IParam sub = param.getSub(i);
			IParam seqParam;
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("join" + mm.getMessage("function.invalidParam"));
			} else if (sub.getType() == IParam.Comma) {
				seqParam = sub.getSub(0);
				if (seqParam == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.invalidParam"));
				}
				
				int expCount = sub.getSubSize() - 1;
				if (!isPJoin && i != 0 && expCount != exps[0].length) {
					if (expCount > exps[0].length) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("join" + mm.getMessage("function.paramCountNotMatch"));
					}
					
					isMix = true;
				}

				Expression []curExps = new Expression[expCount];
				exps[i] = curExps;
				for (int p = 0; p < expCount; ++p) {
					IParam expParam = sub.getSub(p + 1);
					if (expParam == null) {
						if (i == 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("join" + mm.getMessage("function.invalidParam"));
						}
						
						isMix = true;
					} else {
						curExps[p] = expParam.getLeafExpression();
					}
				}
			} else {
				seqParam = sub;
				if (!isPJoin) {
					Expression exp = new Expression("~.v()");
					Expression []curExps = new Expression[]{exp};
					exps[i] = curExps;
					
					if (i != 0 && exps[0].length > 1) {
						isMix = true;
					}
				}
			}
			
			if (seqParam.isLeaf()) { // Ai
				Expression exp = seqParam.getLeafExpression();
				names[i] = exp.getIdentifierName();
				Object obj = exp.calculate(ctx);
				if (obj instanceof Sequence) {
					srcSeries[i] = (Sequence)obj;
				} else if (obj == null) {
					srcSeries[i] = new Sequence(0);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.paramTypeError"));
				}
			} else { // Ai:Fi
				if (seqParam.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.invalidParam"));
				}

				IParam sub0 = seqParam.getSub(0);
				if (sub0 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.invalidParam"));
				}

				Expression exp = sub0.getLeafExpression();
				Object obj = exp.calculate(ctx);
				if (obj instanceof Sequence) {
					srcSeries[i] = (Sequence)obj;
				} else if (obj == null) {
					srcSeries[i] = new Sequence(0);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.paramTypeError"));
				}

				IParam sub1 = seqParam.getSub(1);
				if (sub1 != null) {
					names[i] = sub1.getLeafExpression().getIdentifierName();
				} else {
					names[i] = exp.getIdentifierName();
				}
			}
		}

		if (isPJoin) {
			return Sequence.pjoin(srcSeries, names, option);
		} else if (isIsect || isDiff) {
			return CursorUtil.filterJoin(srcSeries, exps, option, ctx);
		} else {
			if (isMix) {
				int type = 0; // join
				if (option != null) {
					if (option.indexOf('1') != -1) {
						type = 1;
						if (option.indexOf('f') != -1) { // "1f"不能同时设置
							MessageManager mm = EngineMessage.get();
							throw new RQException(option + mm.getMessage("engine.optConflict"));
						}
					} else if (option.indexOf('f') != -1) {
						type = 2;
					}
				}
				
				return CursorUtil.mixJoin(srcSeries, exps, names, type, ctx);
			} else {
				Sequence result = Sequence.join(srcSeries, exps, names, option, ctx);
				return checkOptionX(srcSeries, result, names);
			}
		}
	}

	private Sequence checkOptionX(Sequence []srcSeries, Sequence table, String[] names) {
		if (option == null || option.indexOf('x') == -1 || table == null) {
			return table;
		}
		
		IArray mems = table.getMems();
		int size = mems.size();
		if (size == 0) {
			return table;
		}
		
		int count = names.length;
		int newCount = count;
		boolean hasAllRecord = false;
		boolean isAllRecord[] = new boolean[count];
		int fcount[] = new int[count];
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < count; i++) {
			fcount[i] = srcSeries[i].dataStruct().getFieldCount();
			isAllRecord[i] = true;
			for (int j = 0; j < fcount[i]; j++) {
				boolean b = isAllRecord(mems, i, j, list, names);
				if (!b) {
					isAllRecord[i] = false;
					break;
				}
			}
			if (isAllRecord[i]) {
				newCount += (fcount[i] - 1);
				hasAllRecord = true;
			}
		}
		
		if (!hasAllRecord) {
			return table;
		}
		
		String newNames[] = new String[newCount];
		list.toArray(newNames);
		int findex = 0;
				
		Table out = new Table(newNames);
		for (int i = 1; i <= size; ++i) {
			findex = 0;
			BaseRecord record = out.newLast();
			BaseRecord oldRecord = (BaseRecord) mems.get(i);
			for (int j = 0; j < count; j++) {
				if (isAllRecord[j]) {
					BaseRecord subRecord = (BaseRecord) oldRecord.getFieldValue(j);
					for (int f = 0; f < fcount[j]; f++) {
						record.set(findex + f, subRecord.getFieldValue(f));
					}
					findex += fcount[j];
				} else {
					record.set(findex, oldRecord.getFieldValue(j));
					findex++;
				}
			}
		}

		return out;
	}
	
	private boolean isAllRecord(IArray mems, int i, int j, ArrayList<String> list, String names[]) {
		boolean b = true;
		int size = mems.size();
		int c;
		for (c = 1; c <= size; c++) {
			BaseRecord record = (BaseRecord) mems.get(c);
			if (null == record) {
				continue;
			}
			record = (BaseRecord) record.getFieldValue(i);
			if (null == record) {
				continue;
			}
			Object obj = record.getFieldValue(j);
			if (obj == null) {
				continue;
			} else if (obj instanceof BaseRecord) {
				String name = record.getFieldNames()[j];
				if (list.contains(name)) {
					list.add(names[i] + "_" + name);
				} else {
					list.add(name);
				}
				break;
			} else {
				b = false;
				break;
			}
		}
		
		if (!b) {
			list.add(names[i]);
		}
		
		if (c > size) {
			list.add(names[i] + "_" + j);
		}
		
		return b;
	}
}
