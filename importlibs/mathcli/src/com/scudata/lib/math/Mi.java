package com.scudata.lib.math;

import com.scudata.resources.EngineMessage;
import com.scudata.common.MessageManager;

import java.util.ArrayList;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.SequenceFunction;
import com.scudata.lib.math.prec.Consts;

/**
 * 生成缺失值指示列Missing Indicator
 * @author bd
 * 原型D.mi(V,freq,P,i), D.mi(V,P,i,rePrep), Seq.mi(freq), Seq.mi(P, i, rePrep)
 * A.mi()/P.mi(cn); A.mi@r(rec), P.mi@r(cn, rec)
 * 考察数据集D的变量V，并添加指示列Indicator /
 */
public class Mi extends SequenceFunction {

	public Object calculate(Context ctx) {
		boolean re = option != null && option.indexOf('r') > -1;
		String cn = "mi";
		if (re) {
			Sequence seq = srcSequence;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				Record r1 = null;
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				if (param == null || !param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mi@" + option + mm.getMessage("function.invalidParam"));
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
				if (o1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mi@" + option + mm.getMessage("function.paramTypeError"));
				}
				int col = 0;
				if (o1 instanceof Number) {
					col = ((Number) o1).intValue() - 1;
					cn = r1.dataStruct().getFieldName(col);
				}
				else {
					cn = o1.toString();
					col = r1.dataStruct().getFieldIndex(cn);
				}
				seq = Prep.getFieldValues(srcSequence, col);
			}
			Sequence newcvs = Mi.ifNull(seq);
			String micn = "MI_" + cn;
			ArrayList<String> cns = new ArrayList<String>();
			ArrayList<Sequence> cvs = new ArrayList<Sequence>();
			cns.add(micn);
			cvs.add(newcvs);
			return Prep.toTab(cns, cvs);
		}
		else {
			Sequence seq = srcSequence;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				Record r1 = null;
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				if (param == null || !param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mi" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
				if (o1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mi" + mm.getMessage("function.paramTypeError"));
				}
				int col = 0;
				if (o1 instanceof Number) {
					col = ((Number) o1).intValue() - 1;
					cn = r1.dataStruct().getFieldName(col);
				}
				else {
					cn = o1.toString();
					col = r1.dataStruct().getFieldIndex(cn);
				}
				seq = Prep.getFieldValues(srcSequence, col);
			}
			Sequence result = Mi.recMI(seq);
			if (result == null) {
				return null;
			}
			String micn = "MI_" + cn;
			ArrayList<String> cns = new ArrayList<String>();
			ArrayList<Sequence> cvs = new ArrayList<Sequence>();
			cns.add(micn);
			cvs.add(result);
			return Prep.toTab(cns, cvs);
		}
	}
	
	protected static Sequence ifNull(Sequence seq) {
		return ifNull(seq, false);
	}
	
	private static Sequence ifNull(Sequence seq, boolean check) {
		int len = seq == null ? 0 : seq.length();
		if ( len < 1 ) {
			return new Sequence();
		}
		Sequence result = new Sequence(len);
		for (int i = 1; i <= len; i++) {
			Object o = seq.get(i);
			if (o == null) {
				result.add(Consts.CONST_YES);
			}
			else {
				if (check && ("NA".equals(o) || "".equals(o))) {
					result.add(Consts.CONST_YES);
				}
				else {
					result.add(Consts.CONST_NO);
				}
			}
		}
		return result;
	}
	
	/**
	 * 考察数据集中某列数据，并添加指示列Indicator，Ver2
	 * 如果V中存在null，则添加MI_FNAME列，对于空值该列赋值为1，否则为0
	 * @param Vs	变量值
	 * @param freq	空值频度
	 * @return 		生成的MI字段，不生成返回null
	 */
	protected static Sequence mi(Sequence Vs, double freq){
		if ( freq >= Prep.MISSING_MIN && freq <= Prep.MISSING_MAX) {
			if (Vs.contains(null, false)) {
				Sequence Vmi = ifNull(Vs);
				return Vmi;
			}
		}
		return null;
	}
	
	protected static Sequence recMI(Sequence cvs) {
		int size = cvs.length();
		int missing = 0;
		for (int i = 1; i <= size; i++) {
			Object obj = cvs.get(i);
			if (obj == null) {
				missing++;
			}
		}
		double freq = missing * 1d / size;

		return Mi.mi(cvs, freq);
	}
}
