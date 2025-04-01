package com.scudata.lib.math;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.array.IArray;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.util.Variant;
import com.scudata.lib.math.prec.NorRec;
import com.scudata.resources.EngineMessage;
import com.scudata.common.MessageManager;

/**
 * 归一化
 * @author bd
 * 原型A.normal()	P.normal(cn), A.normal@r(rec), P.normal(cn, rec)
 */
public class Normal extends SequenceFunction{
	public Object calculate(Context ctx) {
		boolean cover = option != null && option.indexOf('c') > -1;
		boolean re = option != null && option.indexOf('r') > -1;
		String cn = "normal";
		if (re) {
			if (param == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("normal@" + option + " " + mm.getMessage("function.invalidParam"));
			}
			Sequence seq = srcSequence;
			Object o1 = null;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				if (param.isLeaf() || param.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("normal@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				IParam sub1 = param.getSub(1);
				IParam sub2 = param.getSub(0);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("normal@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				int col = 0;
				if (o1 == null || !(o1 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("normal@" + option + " " + mm.getMessage("function.paramTypeError"));
				}
				Record r1 = null;
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				if (o2 instanceof Number) {
					col = ((Number) o2).intValue() - 1;
					cn = r1.dataStruct().getFieldName(col);
				}
				else {
					cn = o2.toString();
					col = r1.dataStruct().getFieldIndex(cn);
				}
				seq = Prep.getFieldValues(seq, col);
			}
			else {
				if (!param.isLeaf()) {
					if (param.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("normal@" + option + " " + mm.getMessage("function.invalidParam"));
					}
					IParam sub1 = param.getSub(0);
					IParam sub2 = param.getSub(1);
					if (sub1 == null || sub2 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("normal@" + option + " " + mm.getMessage("function.invalidParam"));
					}
					o1 = sub1.getLeafExpression().calculate(ctx);
					Object o2 = sub2.getLeafExpression().calculate(ctx);
					if (o2 == null || !(o1 instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("normal@" + option + " " + mm.getMessage("function.paramTypeError"));
					}
					cn = o2.toString();
				}
				else {
					o1 = param.getLeafExpression().calculate(ctx);
					if (!(o1 instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("normal@" + option + " " + mm.getMessage("function.paramTypeError"));
					}
				}
				if (!cover) {
					seq = Prep.dup(seq);
				}
			}
			NorRec nr = new NorRec();
			nr.init((Sequence) o1); 
			Normal.normal(seq, nr);
			return seq;
		}
		else {
			Sequence seq = srcSequence;
			Record r1 = null;
			int col = 0;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				if (param == null ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("normal" + mm.getMessage("function.invalidParam"));
				}
				if (!param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("normal" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				if (o1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("normal" + mm.getMessage("function.paramTypeError"));
				}
				else if (o1 instanceof Number) {
					col = ((Number) o1).intValue() - 1;
					cn = r1.dataStruct().getFieldName(col);
				}
				else {
					cn = o1.toString();
					col = r1.dataStruct().getFieldIndex(cn);
				}
				seq = Prep.getFieldValues(srcSequence, col);
			}
			if (r1 == null && !cover ) {
				seq = Prep.dup(seq);
			}
			NorRec nr = Normal.normal(seq);
			Sequence result = nr.toSeq();
			if (cover) {
				if (r1 != null) {
					Prep.coverPSeq(srcSequence, seq, null, r1.dataStruct(), col);
				}
				//return result;
			}
			Sequence bak = new Sequence(2);
			bak.add(seq);
			bak.add(result);
			return bak;
		}
	}
	
	protected static void normal(Sequence x, NorRec nr) {
		int len = x.length();
		double max = nr.getMax();
		double min = nr.getMin();
		double m = max - min;
		for (int i = 1; i <= len; i++ ) {
			double cur = ((Number) x.get(i)).doubleValue();
			double value = m == 0d ? min : (cur - min)/m;
			x.set(i, Double.valueOf(value));
		}
	}
	
	/**
	 * 清理异常值，将结果改变到Vs中
	 * @param Vs	某数值列的数据
	 * @param x	数值列经过纠偏nmnv处理的数据
	 * @param avg	平均值
	 * @param sd	方差
	 * @return
	 */
	protected static NorRec normal(Sequence cvs) {
		NorRec nr = new NorRec();
		IArray mems = cvs.getMems();
		int size = mems.size();
		if (size < 1) {
			return null;
		}
		Number maxVal = null;
		Number minVal = null;
		int i = 1;
		for (; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof Number) {
				maxVal = (Number)obj;
				minVal = (Number)obj;
				break;
			}
		}

		for (++i; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof Number) {
				if (Variant.compare(obj, minVal, true) < 0) {
					minVal = (Number) obj;
				}
				else if (Variant.compare(maxVal, obj, true) < 0) {
					maxVal = (Number) obj;
				}
			}
		}

		double max = maxVal.doubleValue();
		double min = minVal.doubleValue();
		nr.set(min, max);
		double m = max - min;
		int len = cvs.length();
		for (i = 1; i <= len; i++ ) {
			double cur = ((Number) cvs.get(i)).doubleValue();
			double value = m == 0d ? min : (cur - min)/m;
			cvs.set(i, Double.valueOf(value));
		}
		return nr;
	}
}
