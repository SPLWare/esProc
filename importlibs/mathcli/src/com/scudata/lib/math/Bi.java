package com.scudata.lib.math;

import com.scudata.lib.math.prec.Consts;
import com.scudata.lib.math.prec.BIRec;
import com.scudata.lib.math.prec.VarInfo;
import com.scudata.resources.EngineMessage;
import com.scudata.common.MessageManager;
import com.scudata.util.Variant;

import java.util.ArrayList;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;

/**
 * 将枚举变量拆分为多个二值变量BinaryIndicator
 * @author bd
 * 原型D.bi(V)
 * A.bi()/P.bi(cn); A.bi@r(rec)/P.bi@r(cn, rec);
 * 生成Binary Indicator列，由列数据seq，视其是否和设入的v相等，生成一个二值列
 */
public class Bi extends SequenceFunction {
	public Object calculate(Context ctx) {
		boolean re = option != null && option.indexOf('r') > -1;
		String cn = "bi";
		if (re) {
			Sequence seq = srcSequence;
			BIRec br = new BIRec();
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				Record r1 = null;
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				if (param == null ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("impute@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				if (param.isLeaf() || param.getSubSize() < 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("impute@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("impute@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				Object o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2 == null ? null : sub2.getLeafExpression().calculate(ctx);
				if (o1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("impute@" + option + " " + mm.getMessage("function.paramTypeError"));
				}
				if (!(o2 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("impute@" + option + " " + mm.getMessage("function.paramTypeError"));
				}
				br.init((Sequence) o2);
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
			else {
				if (param != null && param.isLeaf()) {
					Object o1 = param.getLeafExpression().calculate(ctx);
					if (!(o1 instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("impute@" + option + " " + mm.getMessage("function.paramTypeError"));
					}
					br.init((Sequence) o1);
				}
			}
			ArrayList<String> ncn = new ArrayList<String>();
			ArrayList<Sequence> ncv = new ArrayList<Sequence>(); 
			Bi.bi(seq, cn, br, ncv, ncn);
			return Prep.toTab(ncn, ncv);
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
					throw new RQException("bi" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
				if (o1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("bi" + mm.getMessage("function.paramTypeError"));
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
			ArrayList<String> cns = new ArrayList<String>();
			ArrayList<Sequence> seqs = new ArrayList<Sequence>(); 
			BIRec br = recBi(seq, cn, cns, seqs);
			if (br == null) {
				return null;
			}
			Sequence result = new Sequence(2);
			Table tab = Prep.toTab(cns, seqs);
			result.add(tab);
			result.add(br.toSeq());
			return result;
		}
	}
	
	protected static Sequence ifValue(Sequence seq, Object v) {
		int len = seq == null ? 0 : seq.length();
		if ( len < 1 ) {
			return new Sequence();
		}
		Sequence result = new Sequence(len);
		for (int i = 1; i <= len; i++) {
			Object o = seq.get(i);
			if (Variant.isEquals(o, v)) {
				result.add(Consts.CONST_YES);
			}
			else {
				result.add(Consts.CONST_NO);
			}
		}
		return result;
	}
	
	protected static BIRec recBi(Sequence cvs, String cn, ArrayList<String> cns, ArrayList<Sequence> seqs) {
		Sequence X = cvs.id(null);
		int size = X.length();
		if (size > 6 || size < 2) {
			return null;
		}

		//生成BI时少生成一列，目前不生成X中的第1列
		//二值也要做，保证二值变量为0和1
		//同时修改的是生成逻辑，去掉第一个分类值，而非最后一个
		ArrayList<String> biCns = new ArrayList<String>(size - 1);
		ArrayList<VarInfo> bivis = new ArrayList<VarInfo>(size - 1);
		for (int i = 2; i <= size; i++ ) {
			Object v = X.get(i);
			String bcn = "BI_"+cn+"_"+v.toString();
			Sequence bvs = Bi.ifValue(cvs, v);
			cns.add(bcn);
			seqs.add(bvs);
			//pr.addCol(bcn, bvs, ResultCol.CL_BI, (byte) (i - 1), srcCn);
			VarInfo bivi = new VarInfo(cn, Consts.F_TWO_VALUE);
			bivi.init(bvs);
			bivi.setName(bcn);
			bivis.add(bivi);
			biCns.add(bcn);
		}
		BIRec biRec = new BIRec(X);
		return biRec;
	}
	
	protected static void bi(Sequence cvs, String cn, BIRec br, ArrayList<Sequence> seqs, ArrayList<String> cns) {
		Sequence X = br.getX();
		int size = X.length();
		//(特别的，二值字段不做这个处理) 二值也要做，保证二值变量为0和1
		//同时修改的是生成逻辑，去掉第一个分类值，而非最后一个
		if (size >= 2) {
			//生成BI字段
			for (int i = 2; i <= size; i++ ) {
				Object v = X.get(i);
				String newcn = "BI_"+cn+"_"+v.toString();
				Sequence newcvs = Bi.ifValue(cvs, v);
				cns.add(newcn);
				seqs.add(newcvs);
				//pr.addCol2(newcn, newcvs);
			}
		}
	}
}
