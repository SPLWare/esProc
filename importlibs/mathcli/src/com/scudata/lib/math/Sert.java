package com.scudata.lib.math;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.lib.math.prec.NumStatis;
import com.scudata.lib.math.prec.VarInfo;
import com.scudata.resources.EngineMessage;
import com.scudata.common.MessageManager;

/**
 * 清理异常值 
 * @author bd
 * 原型x.sert(): a=x.avg()，se=x.se()，x.range(a-5*se,a+5*se) /
 * 原型A.sert()	P.sert(cn), A.sert@r(rec), P.sert(cn, rec)
 */
public class Sert extends SequenceFunction{
	public Object calculate(Context ctx) {
		boolean cover = option != null && option.indexOf('c') > -1;
		boolean re = option != null && option.indexOf('r') > -1;
		String cn = "sert";
		Record r1 = null;
		int col = 0;
		if (re) {
			if (param == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sert@" + option + " " + mm.getMessage("function.invalidParam"));
			}
			Sequence seq = srcSequence;
			Object o1 = null;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				if (param.isLeaf() || param.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sert@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				IParam sub1 = param.getSub(1);
				IParam sub2 = param.getSub(0);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sert@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				if (o1 == null || !(o1 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sert@" + option + " " + mm.getMessage("function.paramTypeError"));
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
						throw new RQException("sert@" + option + " " + mm.getMessage("function.invalidParam"));
					}
					IParam sub1 = param.getSub(0);
					IParam sub2 = param.getSub(1);
					if (sub1 == null || sub2 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sert@" + option + " " + mm.getMessage("function.invalidParam"));
					}
					o1 = sub1.getLeafExpression().calculate(ctx);
					Object o2 = sub2.getLeafExpression().calculate(ctx);
					if (o2 == null || !(o1 instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sert@" + option + " " + mm.getMessage("function.paramTypeError"));
					}
					cn = o2.toString();
				}
				else {
					o1 = param.getLeafExpression().calculate(ctx);
					if (!(o1 instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sert@" + option + " " + mm.getMessage("function.paramTypeError"));
					}
				}
				if (!cover) {
					seq = Prep.dup(seq);
				}
			}
			NumStatis ns = new NumStatis();
			ns.init((Sequence) o1); 
			Sert.sertSeq(seq, ns.getAvg(),  ns.getSd(seq));
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
					throw new RQException("sert" + mm.getMessage("function.invalidParam"));
				}
				if (!param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sert" + mm.getMessage("function.invalidParam"));
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
					throw new RQException("sert" + mm.getMessage("function.paramTypeError"));
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

			NumStatis ns = new NumStatis(seq);
			Sert.sertSeq(seq, ns.getAvg(),  ns.getSd(seq));
			Sequence result = ns.toSeq();
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
	
	protected static void sertSeq(Sequence x, double avg, double sd) {
		int n = x.length();
		double l = avg - 5*sd;
		double u = avg + 5*sd;
		
		for(int i = 1; i <= n; i++){
			Number tmp = (Number) x.get(i);
			double v = tmp == null ? 0 : tmp.doubleValue();
			if (v > u ) {
				x.set(i, Double.valueOf(u));
			}
			else if (v < l ) {
				x.set(i, Double.valueOf(v));
			}
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
	protected static void sert(Sequence Vs, Sequence x, double avg, double sd, VarInfo vi) {
		int n = x.length();
		double l = avg - 5*sd;
		double u = avg + 5*sd;
		int changeCount = 0;
		
		for(int i = 1; i <= n; i++){
			Number tmp = (Number) x.get(i);
			double v = tmp == null ? 0 : tmp.doubleValue();
			double res = v;
			boolean change = false;
			if (v > u ) {
				res = u;
				change = true;
			}
			else if (res < l ) {
				res = l;
				change = true;
			}
			if (change) {
				Vs.set(i, res);
				changeCount++;
			}
		}
		if (vi != null) {
			vi.setCleanCount(changeCount);
		}
	}
}
