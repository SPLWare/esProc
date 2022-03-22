package com.scudata.expression.fn;

import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.excel.ExcelUtils;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.mfn.sequence.Export;
import com.scudata.resources.EngineMessage;

/**
 * E(x) x是二层序列时，转换成多行序表，每行一条记录，第一行是标题。
 * x是串，理解为回车分隔行/TAB分隔列的串，先拆开再转换。
 * x是序表/排列时，转换成二层序列。
 * 
 * @b 无标题
 * @p 二层序列是转置的
 * @s x是序表时返回成回车/TAB分隔的串
 * @2 x是二层序列时不处理，x是一层序列时转换成每行一个的二层序列
 *   @1 x是二层序列时不处理，x是一层序列时转换成只有一行的二层序列
 * @1 x是一层序列时不处理，x是二层序列时用.conj()转成一层序列
 */
public class E extends Function {
	/**
	 * 计算
	 */
	public Object calculate(Context ctx) {
		final String FUNC_NAME = "E";
		String opt = option;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(FUNC_NAME
					+ mm.getMessage("function.missingParam"));
		}
		IParam xParam;
		if (param.getType() != IParam.Normal) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(FUNC_NAME
					+ mm.getMessage("function.invalidParam"));
		} else {
			xParam = param;
		}
		if (xParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(FUNC_NAME
					+ mm.getMessage("function.missingParam"));
		}
		if (!xParam.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(FUNC_NAME
					+ mm.getMessage("function.invalidParam"));
		}
		Object x = xParam.getLeafExpression().calculate(ctx);
		if (x == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(FUNC_NAME
					+ mm.getMessage("function.missingParam"));
		}
		boolean isB = opt != null && opt.indexOf("b") != -1;
		boolean isP = opt != null && opt.indexOf("p") != -1;
		boolean isS = opt != null && opt.indexOf("s") != -1;

		boolean isTwo = opt != null && opt.indexOf("2") != -1;
		boolean isOne = opt != null && opt.indexOf("1") != -1;
		boolean isTwoOne = isTwo && isOne;

		if (x instanceof Sequence) {
			Sequence seq = (Sequence) x;
			if (isS && seq instanceof Table) {
				// @s x是序表时返回成回车/TAB分隔的串
				return exportS((Table) seq, !isB);
			}
			if (seq instanceof Table || seq.isPmt()) {
				// x是序表/排列时，转换成二层序列。
				seq = pmt2Sequence(seq, !isB);
				return seq;
			} else if (isSeqSequence(seq)) {
				if (isOne) {
					// x是二层序列时用.conj()转成一层序列
					seq = seq.conj(null);
				} else {
					// x是二层序列时，转换成多行序表，每行一条记录，第一行是标题。
					if (isP) { // 二层序列是转置的
						seq = ExcelUtils.transpose(seq);
					}
					seq = sequence2Pmt(seq, !isB);
				}
				return seq;
			} else if (isTwoOne) {
				// x是一层序列时转换成只有一行的二层序列
				Sequence seq2 = new Sequence();
				seq2.add(seq); // 将序列作为二层序列的一行
				return seq2;
			} else if (isTwo) {
				// x是一层序列时转换成每行一个的二层序列
				Sequence seq2 = new Sequence();
				for (int i = 1, len = seq.length(); i <= len; i++) {
					Sequence rowSeq = new Sequence();
					rowSeq.add(seq.get(i));
					seq2.add(rowSeq);
				}
				return seq2;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(FUNC_NAME
						+ mm.getMessage("function.invalidParam"));
			}
		} else if (x instanceof String) {
			// x是串，理解为回车分隔行/TAB分隔列的串，先拆开再转换。
			if (!StringUtils.isValidString(x)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(FUNC_NAME
						+ mm.getMessage("function.missingParam"));
			}
			Sequence seq = importS((String) x, !isB);
			seq = pmt2Sequence(seq, !isB);
			return seq;
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(FUNC_NAME
				+ mm.getMessage("function.invalidParam"));
	}

	/**
	 * 回车分隔行/TAB分隔列的串，转成序表
	 * 
	 * @param str      回车分隔行/TAB分隔列的串
	 * @param hasTitle 是否有标题行
	 * @return
	 */
	private Sequence importS(String str, boolean hasTitle) {
		StringBuffer buf = new StringBuffer();
		buf.append(Escape.addEscAndQuote(str));
		buf.append(".import");
		if (hasTitle)
			buf.append("@t");
		buf.append("(;" + COL_SEP + ")");
		Expression exp = new Expression(buf.toString());
		Sequence seq = (Sequence) exp.calculate(new Context());
		return seq;
	}

	/**
	 * 序表转成回车/TAB分隔的串
	 * 
	 * @param t 序表
	 * @return 回车/TAB分隔的串
	 */
	private String exportS(Table t, boolean hasTitle) {
		String opt = hasTitle ? "t" : null;
		String exportStr = Export.export(t, null, null, COL_SEP, opt,
				new Context());
		return exportStr;
	}

	/**
	 * 将序表或者排列，转换为二层序列
	 * 
	 * @param pmt      序表或者排列
	 * @param hasTitle 是否有标题行
	 * @return 二层序列
	 */
	private Sequence pmt2Sequence(Sequence pmt, boolean hasTitle) {
		Sequence seq = new Sequence();
		if (hasTitle) {
			DataStruct ds = pmt.dataStruct();
			if (ds != null) {
				seq.add(new Sequence(ds.getFieldNames()));
			}
		}
		Record r;
		for (int i = 1, len = pmt.length(); i <= len; i++) {
			r = (Record) pmt.get(i);
			seq.add(new Sequence(r.getFieldValues()));
		}
		return seq;
	}

	/**
	 * 将二层序列转换为序表
	 * 
	 * @param seq      二层序列
	 * @param hasTitle 是否有标题行
	 * @return 序表
	 */
	private Table sequence2Pmt(Sequence seq, boolean hasTitle) {
		Table t = null;
		Sequence rowSeq;
		int cc = 0;
		for (int i = 1, len = seq.length(); i <= len; i++) {
			rowSeq = (Sequence) seq.get(i);
			if (rowSeq == null || rowSeq.length() == 0) {
				if (t == null)
					continue;
				else
					t.newLast();
			}
			if (t == null) {
				cc = rowSeq.length();
				String[] colNames = new String[cc];
				if (hasTitle) {
					Object val;
					String colName;
					for (int c = 1; c <= cc; c++) {
						val = rowSeq.get(c);
						if (val != null) {
							colName = String.valueOf(val);
						} else {
							colName = null;
						}
						if (!StringUtils.isValidString(colName)) {
							colName = "_" + c;
						}
						colNames[c - 1] = colName;
					}

				} else {
					for (int c = 1; c <= cc; c++) {
						colNames[c - 1] = "_" + c;
					}
				}
				t = new Table(colNames);
				if (hasTitle) {
					continue;
				}
			}
			Object[] rowData = new Object[cc];
			for (int c = 1, count = Math.min(cc, rowSeq.length()); c <= count; c++) {
				rowData[c - 1] = rowSeq.get(c);
			}
			t.newLast(rowData);
		}
		return t;
	}

	/**
	 * 是否二层序列
	 * 
	 * @param seq 序列
	 * @return 是否二层序列
	 */
	private boolean isSeqSequence(Sequence seq) {
		if (seq == null)
			return false;
		Object obj;
		boolean hasSequence = false;
		for (int i = 1, len = seq.length(); i <= len; i++) {
			obj = seq.get(i);
			if (obj != null) {
				if (obj instanceof Sequence) {
					hasSequence = true;
				} else {
					return false;
				}
			}
		}
		return hasSequence;
	}

	private static final String COL_SEP = "\t";
}
