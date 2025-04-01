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
 * 用指定序列填充序表的字段，或把字段名和字段值组成序列转成序表
 * T.record(A,k)
 * A.record() A是字段值序列组成的序列
 * A.record(n) A是字段名和字段值组成序列
 * @author RunQian
 *
 */
public class RecordValue extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (srcSequence instanceof Table) {
			return record((Table)srcSequence, ctx);
		} else {
			if (param == null) {
				return srcSequence.toTable();
			} else if (param.isLeaf()) {
				Object obj = param.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("record" + mm.getMessage("function.paramTypeError"));
				}
				
				int fcount = ((Number)obj).intValue();
				return record(srcSequence, fcount);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("record" + mm.getMessage("function.invalidParam"));
			}
		}
	}

	private static Table record(Sequence seq, int fcount) {
		int len = seq.length();
		if (len == 0) {
			return null;
		} else if (fcount < 1 || fcount > len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("record" + mm.getMessage("function.invalidParam"));
		}
		
		String []fnames = new String[fcount];
		for (int i = 1; i <= fcount; ++i) {
			Object obj = seq.getMem(i);
			if (obj instanceof String) {
				fnames[i - 1] = (String)obj;
			} else if (obj != null) {
				fnames[i - 1] = obj.toString();
			}
		}
		
		Table table = new Table(fnames, len / fcount);
		Sequence tmp = seq.get(fcount + 1, len + 1);
		table.record(1, tmp, null);
		return table;
	}
	
	private Sequence record(Table srcTable, Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("record" + mm.getMessage("function.missingParam"));
		}

		Expression srcExp;
		int pos = 0;
		
		if (param.isLeaf()) {
			srcExp = param.getLeafExpression();
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("record" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("record" + mm.getMessage("function.invalidParam"));
			}
			
			srcExp = sub1.getLeafExpression();
			Object posObj = sub2.getLeafExpression().calculate(ctx);
			if (!(posObj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("record" + mm.getMessage("function.paramTypeError"));
			}

			pos = ((Number)posObj).intValue();
		}

		Object src = srcExp.calculate(ctx);
		if (src instanceof Sequence) {
			return srcTable.record(pos, (Sequence)src, option);
		} else if (src == null) {
			return srcTable;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("record" + mm.getMessage("function.paramTypeError"));
		}
	}
}
