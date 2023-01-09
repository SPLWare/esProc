package com.scudata.lib.math;

import java.util.ArrayList;

import com.scudata.lib.math.prec.Consts;
import com.scudata.lib.math.prec.VarRec;
import com.scudata.lib.math.prec.VarSrcInfo;
import com.scudata.resources.EngineMessage;
import com.scudata.common.MessageManager;
import com.scudata.common.*;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;

/**
 * D.mvp()	考察数据集D中的“MI_xxx”的列，每10列用二进制计数整合为1列，不足则尽量均分 /
 * A.mvp(T)/P.mvp(cns, T); @bnie 选项指明目标类型，各选项相斥，优先级按照二值/数值/整数/枚举，无选项自动处理
 * A.mvp@r(rec)/P.mvp@r(cns, rec)
 * @author bd
 */
public class Mvp extends SequenceFunction {

	public Object calculate(Context ctx) {
		boolean re = option != null && option.indexOf('r') > -1;
		boolean ifall = option != null && option.indexOf('a') > -1;
		if (srcSequence == null || srcSequence.length() < 1) {
			return srcSequence;
		}
		if (re) {
			if (param == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mvp@" + option + " " + mm.getMessage("function.invalidParam"));
			}
			ArrayList<Sequence> seqs = null;
			Object o1 = null;
			ArrayList<VarRec> vrs = null;
			String[] cns = null;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				if (param.isLeaf() || param.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mvp@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				Record r1 = null;
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				boolean all = false;
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				o1 = sub1 == null ? null : sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2 == null ? null : sub2.getLeafExpression().calculate(ctx);
				if (o2 instanceof Sequence) {
					Sequence seq = (Sequence) o2;
					int size = seq.length();
					vrs = new ArrayList<VarRec>(size);
					for (int i = 1; i <= size; i++) {
						VarRec vr = new VarRec();
						vr.init((Sequence) seq.get(i));
						vrs.add(vr);
					}
				}
				int[] cols = null;
				int clen = 0;
				if (o1 == null) {
					all = true;
				}
				if (!(o1 instanceof Sequence)) {
					// 单列
					Sequence seq = new Sequence(1);
					seq.add(o1);
					o1 = seq;
				}
				if (!(o1 instanceof Sequence)) {
					// 单列
					Sequence seq = new Sequence(1);
					seq.add(o1);
					o1 = seq;
				}
				if (all) {
					// 默认列
					cns = r1.getFieldNames();
					clen = cns.length;
					if (!ifall) {
						// 筛选出所需列
						ArrayList<Integer> loc = Prep.filter(cns, "MI_", true);
						String[] cnso = cns;
						clen = loc.size();
						cns = new String[clen];
						cols = new int[clen];
						for (int c = 0; c < clen; c++) {
							int ci = loc.get(c);
							cns[c] = cnso[ci];
							cols[c] = ci;
						}
					}
					else {
						cols = new int[clen];
						for (int c = 0; c < clen; c++) {
							cols[c] = c;
						}
					}
				}
				else {
					Sequence seq = (Sequence) o1;
					clen = seq.length();
					cols = new int[clen];
					cns = new String[clen];
					for (int i = 0; i < clen; i++) {
						Object o = seq.get(i+1);
						if (o instanceof Number) {
							cols[i] = ((Number) o).intValue() - 1;
							cns[i] = r1.dataStruct().getFieldName(cols[i]);
						}
						else {
							cns[i] = o.toString();
							cols[i] = r1.dataStruct().getFieldIndex(cns[i]);
						}
					}
				}
				seqs = Prep.getFields(srcSequence, cols);
				int len = seqs.size();
				if (len < 2) {
					// 单列，不用计算了
					ArrayList<String> vns = new ArrayList<String>();
					vns.add("MVP1");
					Table tab = Prep.toTab(vns, seqs);
					return tab;
				}
			}
			else {
				if (param != null && param.isLeaf()) {
					o1 = param.getLeafExpression().calculate(ctx);
					if (o1 instanceof Sequence) {
						Sequence seq = (Sequence) o1;
						int size = seq.length();
						vrs = new ArrayList<VarRec>(size);
						for (int i = 1; i <= size; i++) {
							VarRec vr = new VarRec();
							vr.init((Sequence) seq.get(i));
							vrs.add(vr);
						}
					}
				}
				o1 = srcSequence.get(1);
				if (o1 instanceof Sequence) {
					seqs = Prep.seqToSeqs(srcSequence);
				}
				else {
					// 只有一列，直接返回
					ArrayList<String> vns = new ArrayList<String>();
					vns.add("MVP1");
					seqs = new ArrayList<Sequence>();
					seqs.add(srcSequence);
					Table tab = Prep.toTab(vns, seqs);
					return tab;
				}
			}
			ArrayList<Sequence> ncv = new ArrayList<Sequence>();
			ArrayList<String> ncn = new ArrayList<String>();
			Mvp.mvp(seqs, cns, vrs, ncv, ncn);
			return Prep.toTab(ncn, ncv);
		}
		else {
			ArrayList<Sequence> seqs = null;
			Sequence tvs = null;
			byte tType = 0;
			String[] cns = null;
			if (option != null) {
				if (option.indexOf('b') > -1) {
					tType = Consts.F_TWO_VALUE;
				}
				else if (option.indexOf('n') > -1) {
					tType = Consts.F_NUMBER;
				}
				else if (option.indexOf('i') > -1) {
					tType = Consts.F_COUNT;
				}
				else if (option.indexOf('e') > -1) {
					tType = Consts.F_ENUM;
				}
			}
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				// srcSequence 要求是排列或序表
				boolean all = false;
				if (param == null ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mvp" + mm.getMessage("function.invalidParam"));
				}
				if (param.isLeaf() || param.getSubSize() < 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mvp" + mm.getMessage("function.invalidParam"));
				}
				Record r1 = null;
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mvp" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = sub1 == null ? null : sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				int[] cols = null;
				int clen = 0;
				if (o1 == null) {
					all = true;
				}
				if ( !(o2 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mvp" + mm.getMessage("function.paramTypeError"));
				}
				if (!(o1 instanceof Sequence)) {
					// 单列
					Sequence seq = new Sequence(1);
					seq.add(o1);
					o1 = seq;
				}
				if (all) {
					// 默认列
					seqs = Prep.pseqToSeqs(srcSequence);
					cns = r1.getFieldNames();
					clen = cns.length;
					if (!ifall) {
						// 筛选出所需列
						ArrayList<Integer> loc = Prep.filter(cns, "MI_", true);
						String[] cnso = cns;
						clen = loc.size();
						cns = new String[clen];
						cols = new int[clen];
						for (int c = 0; c < clen; c++) {
							int ci = loc.get(c);
							cns[c] = cnso[ci];
							cols[c] = ci;
						}
					}
					else {
						cols = new int[clen];
						for (int c = 0; c < clen; c++) {
							cols[c] = c;
						}
					}
				}
				else {
					Sequence seq = (Sequence) o1;
					clen = seq.length();
					cols = new int[clen];
					cns = new String[clen];
					for (int i = 0; i < clen; i++) {
						Object o = seq.get(i+1);
						if (o instanceof Number) {
							cols[i] = ((Number) o).intValue() - 1;
							cns[i] = r1.dataStruct().getFieldName(cols[i]);
						}
						else {
							cns[i] = o.toString();
							cols[i] = r1.dataStruct().getFieldIndex(cns[i]);
						}
					}
					seqs = Prep.getFields(srcSequence, cols);
				}
				/*
				 * 正常走流程就是了
				int len = seqs.size();
				if (len < 2) {
					// 单列，不用计算了
					ArrayList<String> vns = new ArrayList<String>();
					vns.add("MVP1");
					Table tab = Prep.toTab(vns, seqs);
					Sequence result = new Sequence(2);
					result.add(tab);
					result.add(null);
					return result;
				}
				*/
				tvs = (Sequence) o2;
			}
			else {
				if (param == null || !(param.isLeaf())) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mvp" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
				if ( !(o1 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mvp" + mm.getMessage("function.paramTypeError"));
				}
				tvs = (Sequence) o1;
				o1 = srcSequence.get(1);
				if (o1 instanceof Sequence) {
					seqs = Prep.seqToSeqs(srcSequence);
				}
				else {
					// 只有一列，直接返回
					seqs = new ArrayList<Sequence>();
					seqs.add(srcSequence);
					/*
					 * 正常走流程就是了
					ArrayList<String> vns = new ArrayList<String>();
					vns.add("MVP1");
					Table tab = Prep.toTab(vns, seqs);
					Sequence result = new Sequence(2);
					result.add(tab);
					result.add(null);
					return result;
					*/
				}
			}
			if (tType < 1) {
				tType = Prep.getType(tvs);
			}
			ArrayList<Sequence> ncv = new ArrayList<Sequence>();
			ArrayList<String> ncn = new ArrayList<String>();
			ArrayList<VarRec> rec = recMvp(seqs, cns, tvs, tType, ncn, ncv);
			int size = rec == null ? 0 : rec.size();
			Sequence seq = new Sequence();
			for (int i = 0; i < size; i++) {
				VarRec vr = rec.get(i);
				seq.add(vr.toSeq());
			}
			Table tab = Prep.toTab(ncn, ncv);
			Sequence result = new Sequence(2);
			result.add(tab);
			result.add(seq);
			return result;
		}
	}
	protected static ArrayList<VarRec> recMvp(ArrayList<Sequence> seqs, String[] names, Sequence tvs,
			byte tType, ArrayList<String> ncn, ArrayList<Sequence> ncv) {
		int size = seqs.size();
		if (size < 1) {
			//没有MI，返回
			return null;
		}
		
		int len = seqs.get(0).length();
		int k = (size-1)/10 + 1;
		int misize = (size-1) / k + 1;
		int curf = 0;
		ArrayList<VarRec> vrs = new ArrayList<VarRec>(k);
		for (int i = 0; i < k; i++ ) {
			String mvpN = "MVP"+(i+1);
			//ncn.add("MVP"+(i+1));
			if (i==k-1) {
				misize = size - curf;
			}
			else {
				misize =(size - curf - 1) / (k - i) + 1;
			}
			Sequence curMvp = new Sequence(len);
			for (int j = 0; j < len; ++j) {
				curMvp.add(new Integer(0));
			}
			int mul = 1;
			for (int j = 0; j < misize; j++) {
				Sequence cv = seqs.get(curf + j);
				if (cv == null) {
					MessageManager mm = EngineMessage.get();
					String cn = names == null ? Integer.toString(curf + j) : names[curf + j];
					Logger.warn(mm.getMessage("prep.mvpWrongCol", cn));
					continue;
				}
				addMIMul(curMvp, cv, mul);
				mul = mul * 2;
			}
			VarSrcInfo mvpVsi = new VarSrcInfo(mvpN, Consts.F_ENUM);
			// MVP的统计信息暂不收集，vsi不做初始化了
			//累加完毕，得到的mvp字段作为枚举类型继续处理
			VarRec vr = new VarRec(false, false, mvpVsi);
			//mvp字段不会存在空值，freq为0
			Prep.dealEnum(curMvp, mvpN, 0d, vr, Consts.F_ENUM, tvs, tType, mvpVsi, ncv, ncn);
			//dealEnum(curMvp, mvpN, pr, 0d, vr, Consts.F_ENUM,
			//		ResultCol.CL_OTHERS, (byte) 0, srcCns, mvpVsi);
			//dmr.addMVPRec(vr, cns);
			vrs.add(vr);
			
			curf += misize;
		}
		return vrs;
	}
	
	/**
	 * 将cvs中的数值成员，乘multiple倍后，加到res成员中，改变res，MVP专用，因此最大值为1023，全用int就好
	 * @param res
	 * @param cvs
	 * @param multiple
	 */
	private static void addMIMul(Sequence res, Sequence cvs, int multiple) {
		int len = res.length();
		for (int i = 1; i <= len; i++) {
			int a = ((Number) res.get(i)).intValue();
			int b = ((Number) cvs.get(i)).intValue();
			res.set(i, Integer.valueOf(a + b * multiple));
		}
	}
	
	protected static void mvp(ArrayList<Sequence> seqs, String[] names, ArrayList<VarRec> vrs, 
			ArrayList<Sequence> ncv, ArrayList<String> ncn) {
		int len = seqs.get(0).length();
		int cols = vrs == null ? 0 : vrs.size();
		int size = seqs.size();
		if (cols > 0) {
			int misize = (size-1) / cols + 1;
			int curf = 0;
			for (int i = 0; i < cols; i++ ) {
				String mvpN = "MVP"+(i+1);
				//ncn.add("MVP"+i);
				if (i==cols-1) {
					misize = size - curf;
				}
				else {
					misize =(size - curf - 1) / (cols - i) + 1;
				}
				Sequence curMvp = new Sequence(len);
				for (int j = 0; j < len; ++j) {
					curMvp.add(new Integer(0));
				}
				int mul = 1;
				for (int j = 0; j < misize; j++) {
					Sequence cv = seqs.get(curf + j);
					if (cv == null) {
						MessageManager mm = EngineMessage.get();
						String cn = names == null ? Integer.toString(curf + j) : names[curf + j];
						Logger.warn(mm.getMessage("prep.mvpWrongCol", cn));
						continue;
					}
					addMIMul(curMvp, cv, mul);
					mul = mul * 2;
				}
				
				VarRec vr = vrs.get(i);
				Prep.prep(curMvp, mvpN, vr, ncv, ncn);
				
				curf += misize;
			}
		}
	}
	
	public static void main(String[] args) {
		
	}
}
