package com.scudata.lib.math;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.common.MessageManager;

/**
 * 枚举值转换
 * @author bd
 * 原型A.setenum()	P.setenum(cn), A.setenum@r(rec), P.setenum(cn, rec)
 */
public class EnumToInt extends SequenceFunction{
	public Object calculate(Context ctx) {
		boolean cover = option != null && option.indexOf('c') > -1;
		boolean re = option != null && option.indexOf('r') > -1;
		String cn = "setenum";
		Record r1 = null;
		int col = 0;
		if (re) {
			if (param == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("setenum@" + option + " " + mm.getMessage("function.invalidParam"));
			}
			Sequence seq = srcSequence;
			Object o1 = null;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				if (param.isLeaf() || param.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("setenum@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				IParam sub1 = param.getSub(1);
				IParam sub2 = param.getSub(0);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("setenum@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				if (o1 == null || !(o1 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("setenum@" + option + " " + mm.getMessage("function.paramTypeError"));
				}
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
						throw new RQException("setenum@" + option + " " + mm.getMessage("function.invalidParam"));
					}
					IParam sub1 = param.getSub(0);
					IParam sub2 = param.getSub(1);
					if (sub1 == null || sub2 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("setenum@" + option + " " + mm.getMessage("function.invalidParam"));
					}
					o1 = sub1.getLeafExpression().calculate(ctx);
					Object o2 = sub2.getLeafExpression().calculate(ctx);
					if (o2 == null || !(o1 instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("setenum@" + option + " " + mm.getMessage("function.paramTypeError"));
					}
					cn = o2.toString();
				}
				else {
					o1 = param.getLeafExpression().calculate(ctx);
					if (!(o1 instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("setenum@" + option + " " + mm.getMessage("function.paramTypeError"));
					}
				}
				if (!cover) {
					seq = Prep.dup(seq);
				}
			}
			EnumToInt.setEnum(seq, (Sequence) o1);
			if (cover) {
				if (r1 != null) {
					Prep.coverPSeq(srcSequence, seq, null, r1.dataStruct(), col);
				}
				//return result;
			}
			return seq;
		}
		else {
			Sequence seq = srcSequence;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				if (param == null ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("setenum" + mm.getMessage("function.invalidParam"));
				}
				if (!param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("setenum" + mm.getMessage("function.invalidParam"));
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
					throw new RQException("setenum" + mm.getMessage("function.paramTypeError"));
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

			Sequence index = EnumToInt.setEnum(seq);
			if (cover) {
				if (r1 != null) {
					Prep.coverPSeq(srcSequence, seq, null, r1.dataStruct(), col);
				}
				//return result;
			}
			Sequence bak = new Sequence(2);
			bak.add(seq);
			bak.add(index);
			return bak;
		}
	}
	
	/**
	 * 将枚举值转换为整数
	 * @param x
	 * @param avg
	 * @param sd
	 */
	protected static Sequence setEnum(Sequence x) {
		Sequence index = x.id(null);
		setEnum(x, index);
		return index;
	}
	
	/**
	 * 将枚举值转换为整数
	 * @param x
	 * @param avg
	 * @param sd
	 */
	protected static void setEnum(Sequence x, Sequence index) {
		int len = x == null ? 0 : x.length();
		for (int i = 1; i <= len; i++) {
			Object o = x.get(i);
			int loc = index.firstIndexOf(o);
			x.set(i, loc);
		}
	}
}
