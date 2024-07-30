package com.scudata.expression.fn;

import java.util.Date;

import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.excel.ExcelUtils;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.mfn.sequence.Export;
import com.scudata.resources.EngineMessage;

/**
 * E(x) x是二层序列时，转换成多行序表，每行一条记录，第一行是标题。 x是串，理解为回车分隔行/TAB分隔列的串，先拆开再转换。
 * x是序表/排列时，转换成二层序列。 x是数值则按Excel规则的日期/时间。 x是日期/时间则转成数值。 未定义的情况则返回x本身。
 * 
 * @b 无标题
 * @p 二层序列是转置的，如果转回二层序列时也转换
 * @s x是序表时返回成回车/TAB分隔的串
 * @1 转成单层序列，x是单值时返回[x]；x是二层序列返回x.conj()，有@p时先列后行
 * @2 x是单值时返回[[x]]，有@p时将转置返回
 * 
 */
public class E extends Function {
	/**
	 * 计算
	 */
	public Object calculate(Context ctx) {
		final String FUNC_NAME = "E";
		String opt = option;
		if (param == null) {
			return null;
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
			return null;
		}
		if (!xParam.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(FUNC_NAME
					+ mm.getMessage("function.invalidParam"));
		}
		Object x = xParam.getLeafExpression().calculate(ctx);
		if (x == null) {
			return null;
		}
		boolean isB = opt != null && opt.indexOf("b") != -1;
		boolean isP = opt != null && opt.indexOf("p") != -1;
		boolean isS = opt != null && opt.indexOf("s") != -1;
		boolean is1 = opt != null && opt.indexOf("1") != -1;
		boolean is2 = opt != null && opt.indexOf("2") != -1;

		if (is1) { // 转成单层序列
			Sequence seq;
			if (x instanceof Sequence) {
				seq = (Sequence) x;
				if (isSequence2(seq)) { // x是二层序列返回x.conj()
					if (isP) { // 有@p时先列后行
						seq = ExcelUtils.transpose(seq);
					}
					seq = seq.conj(null);
				} else {
					// 单层序列不做处理
				}
			} else { // x是单值时返回[x]
				seq = new Sequence();
				seq.add(x);
			}
			return seq;
		}

		if (is2) { // 转成二层序列
			Sequence seq;
			if (x instanceof Sequence) {
				seq = (Sequence) x;
				if (isSequence2(seq)) { // 二层序列
					if (isP) { // 有@p时将转置返回
						seq = ExcelUtils.transpose(seq);
					}
				} else { // Excel参数不应该有单层序列，也处理一下吧
					seqToSeq2(seq);
				}
			} else { // x是单值时返回[[x]]
				seq = new Sequence();
				Sequence memSeq = new Sequence();
				memSeq.add(x);
				seq.add(memSeq);
			}
			return seq;
		}

		if (x instanceof Number) { // excel日期时间的数值转成java日期时间
			Date date = ExcelUtils.excelDateNumber2JavaDate((Number) x);
			return date;
		} else if (x instanceof Date) { // java日期时间转成excel日期时间的数值
			Number excelDateNumber = ExcelUtils
					.javaDate2ExcelDateNumber((Date) x);
			return excelDateNumber;
		} else if (x instanceof Sequence) {
			Sequence seq = (Sequence) x;
			if (isS && seq instanceof Table) {
				// @s x是序表时返回成回车/TAB分隔的串
				return exportS((Table) seq, !isB);
			}
			if (seq instanceof Table || seq.isPmt()) {
				// x是序表/排列时，转换成二层序列。
				if (isP) {
					seq = pmtToSequenceP(seq, !isB);
				} else {
					seq = pmtToSequence(seq, !isB);
				}
				return seq;
			} else if (isSequence2(seq)) {
				// x是二层序列时，转换成多行序表，每行一条记录，第一行是标题。
				if (isP) {
					seq = sequenceToTableP(seq, !isB);
				} else {
					seq = sequenceToTable(seq, !isB);
				}
				return seq;
			} else {
				return x;
			}
		} else if (x instanceof String) {
			// x是串，理解为回车分隔行/TAB分隔列的串，先拆开再转换。
			return transposeString(x, isB, isP);
		}
		return x;
	}

	/**
	 * x是串，理解为回车分隔行/TAB分隔列的串，先拆开再转换。
	 * 
	 * @return
	 */
	public static Object transposeString(Object x, boolean isB, boolean isP) {
		if (!StringUtils.isValidString(x)) {
			return x;
		}
		Sequence seq = importS((String) x, !isB);
		if (seq == null) {
			return x;
		}
		if (seq instanceof Table || seq.isPmt()) { // 序表或排列转成二层序列
			if (isP) {
				seq = pmtToSequenceP(seq, !isB);
			} else {
				seq = pmtToSequence(seq, !isB);
			}
		} else {
			if (isP) { // 行列转置返回
				seq = ExcelUtils.transpose(seq);
			}
		}
		return seq;
	}

	/**
	 * 回车分隔行/TAB分隔列的串，转成序表
	 * 
	 * @param str
	 *            回车分隔行/TAB分隔列的串
	 * @param hasTitle
	 *            是否有标题行
	 * @return
	 */
	private static Sequence importS(String str, boolean hasTitle) {
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
	 * @param t
	 *            序表
	 * @return 回车/TAB分隔的串
	 */
	private String exportS(Table t, boolean hasTitle) {
		String opt = hasTitle ? "t" : null;
		String exportStr = Export.export(t, null, null, COL_SEP, opt,
				new Context());
		return exportStr;
	}

	/**
	 * 将序表或者排列，转换为二层序列。调用前需要保证pmt是序表或排列
	 * 
	 * @param pmt
	 *            序表或者排列
	 * @param hasTitle
	 *            是否有标题行
	 * @return 二层序列
	 */
	private static Sequence pmtToSequence(Sequence pmt, boolean hasTitle) {
		if (pmt == null)
			return pmt;
		int len = pmt.length();
		Sequence seq;
		if (hasTitle) { // 标题行作为第一行记录
			seq = new Sequence(len + 1);
			DataStruct ds = pmt.dataStruct();
			if (ds != null) {
				seq.add(new Sequence(ds.getFieldNames()));
			}
		} else {
			seq = new Sequence(len);
		}
		BaseRecord r;
		for (int i = 1; i <= len; i++) {
			r = (BaseRecord) pmt.get(i);
			seq.add(new Sequence(r.getFieldValues()));
		}
		return seq;
	}

	/**
	 * 将序表或者排列，转换为二层序列，行列转置返回。调用前需要保证pmt是序表或排列
	 * 
	 * @param pmt
	 *            序表或者排列
	 * @param hasTitle
	 *            是否有标题行
	 * @return 二层序列
	 */
	private static Sequence pmtToSequenceP(Sequence pmt, boolean hasTitle) {
		if (pmt == null)
			return pmt;
		BaseRecord r;
		Object firstMem = pmt.ifn();
		String[] fieldNames = null;
		int colCount = 0;
		if (hasTitle) {
			DataStruct ds = pmt.dataStruct();
			if (ds == null) // 有标题行的数据结构不能为null
				return pmt;
			fieldNames = ds.getFieldNames();
			colCount = fieldNames.length;
		} else {
			if (firstMem == null) {
				// 成员全是空，返回一个二层空序列
				Sequence seq = new Sequence();
				seq.add(new Sequence());
				return seq;
			}
			r = (BaseRecord) firstMem;
			colCount = r.getFieldCount();
		}
		int len = pmt.length();
		Sequence seq = new Sequence(len);
		Sequence colSeq;
		for (int c = 0; c < colCount; c++) {
			colSeq = new Sequence(len);
			if (fieldNames != null) { // 有标题时先加标题
				colSeq.add(fieldNames[c]);
			}
			for (int i = 1; i <= len; i++) {
				r = (BaseRecord) pmt.get(i);
				if (r == null) {
					colSeq.add(null);
				} else {
					if (r.getFieldCount() > c)
						colSeq.add(r.getFieldValue(c));
					else
						colSeq.add(null);
				}
			}
			seq.add(colSeq);
		}
		return seq;
	}

	/**
	 * 将二层序列转换为序表
	 * 
	 * @param seq
	 *            二层序列
	 * @param hasTitle
	 *            是否有标题行
	 * @return 序表
	 */
	private Table sequenceToTable(Sequence seq, boolean hasTitle) {
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
				String[] colNames;
				if (hasTitle) {
					colNames = getColNames(rowSeq);
				} else {
					colNames = getDefaultColNames(cc);
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
	 * 将行列转置的二层序列转换为序表
	 * 
	 * @param seq
	 *            二层序列
	 * @param hasTitle
	 *            是否有标题行
	 * @return 序表
	 */
	private Table sequenceToTableP(Sequence seq, boolean hasTitle) {
		Table t = null;
		int len = seq.length();
		int colCount = ExcelUtils.getSequenceColCount(seq);
		Sequence colSeq;
		Sequence rowSeq;
		boolean isEmptyCol;
		for (int c = 0; c < colCount; c++) {
			colSeq = new Sequence(len);
			isEmptyCol = true;
			for (int i = 1; i <= len; i++) {
				rowSeq = (Sequence) seq.get(i);
				if (rowSeq == null) {
					colSeq.add(null);
				} else {
					if (rowSeq.length() > c) {
						colSeq.add(rowSeq.get(c + 1));
						isEmptyCol = false;
					} else {
						colSeq.add(null);
					}
				}
			}
			if (isEmptyCol) {
				if (t == null)
					continue;
				else
					t.newLast();
			}
			if (t == null) {
				String[] colNames;
				if (hasTitle) {
					colNames = getColNames(colSeq);
				} else {
					colNames = getDefaultColNames(len);
				}
				t = new Table(colNames);
				if (hasTitle) {
					continue;
				}
			}
			t.newLast(colSeq.toArray());
		}
		return t;
	}

	/**
	 * 根据数据生成字段名
	 * 
	 * @param seq
	 * @return
	 */
	private String[] getColNames(Sequence seq) {
		int len = seq.length();
		String[] colNames = new String[len];
		Object val;
		String colName;
		for (int i = 1; i <= len; i++) {
			val = seq.get(i);
			if (val != null) {
				colName = String.valueOf(val);
			} else {
				colName = null;
			}
			if (!StringUtils.isValidString(colName)) {
				colName = "_" + i;
			}
			colNames[i - 1] = colName;
		}
		return colNames;
	}

	/**
	 * 返回缺省字段名
	 * 
	 * @param cc
	 * @return
	 */
	private String[] getDefaultColNames(int cc) {
		String[] colNames = new String[cc];
		for (int c = 1; c <= cc; c++) {
			colNames[c - 1] = "_" + c;
		}
		return colNames;
	}

	/**
	 * 是否二层序列
	 * 
	 * @param seq
	 *            序列
	 * @return 是否二层序列
	 */
	private boolean isSequence2(Sequence seq) {
		if (seq == null)
			return false;
		Object obj;
		boolean memIsSequence = false;
		for (int i = 1, len = seq.length(); i <= len; i++) {
			obj = seq.get(i);
			if (obj != null) {
				if (obj instanceof Sequence) {
					memIsSequence = true;
				} else {
					return false; // 非空成员不是序列返回false
				}
			}
		}
		return memIsSequence;
	}

	/**
	 * 单层序列转为二层序列
	 */
	private void seqToSeq2(Sequence seq) {
		Object obj;
		Sequence memSeq;
		for (int i = 1, len = seq.length(); i <= len; i++) {
			obj = seq.get(i);
			if (obj != null && obj instanceof Sequence) {
				continue;
			} else {
				memSeq = new Sequence();
				memSeq.add(obj);
				seq.set(i, memSeq);
			}
		}
	}

	private static final String COL_SEP = "\t";
}
