package com.scudata.lib.math;

import com.scudata.lib.math.prec.Consts;
import com.scudata.lib.math.prec.DiMvpRec;
import com.scudata.lib.math.prec.VarDateInterval;
import com.scudata.lib.math.prec.VarInfo;
import com.scudata.lib.math.prec.VarRec;
import com.scudata.resources.EngineMessage;
import com.scudata.common.MessageManager;

import java.util.ArrayList;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.util.Variant;

/**
 * 数值目标变量的纠偏处理
 * @author bd
 * A.dateinterval(T)/P.dateinterval(cns, T); @bnie 选项指明目标类型，各选项相斥，优先级按照二值/数值/整数/枚举，无选项自动处理
 * A.dateinterval@r(rec),A.dateinterval@r(cns, rec)
 * 
 * 清理数据集D的离散变量V
 */
public class DateInterval extends SequenceFunction {

	public Object calculate(Context ctx) {
		if (srcSequence == null || srcSequence.length() < 1) {
			return srcSequence;
		}
		boolean re = option != null && option.indexOf('r') > -1;
		if (re) {
			if (param == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("dateinterval@" + option + " " + mm.getMessage("function.invalidParam"));
			}
			DiMvpRec dmr = new DiMvpRec();
			String[] cns = null;
			ArrayList<Sequence> seqs = null;
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
					throw new RQException("dateinterval@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				if (param.isLeaf() || param.getSubSize() < 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dateinterval@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				Object o1 = sub1 == null? null : sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2 == null? null : sub2.getLeafExpression().calculate(ctx);
				if ( !(o2 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dateinterval@" + option + " " + mm.getMessage("function.paramTypeError"));
				}
				if (o1 instanceof Sequence) {
					Sequence seq = (Sequence) o1;
					int len = seq.length();
					int[] cols = new int[len];
					cns = new String[len];
					for (int i = 0; i < len; i++) {
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
				else {
					// 允许不定义选择列名，此时使用全部字段
					seqs = Prep.pseqToSeqs(srcSequence);
					cns = r1.getFieldNames();
				}
				dmr.init((Sequence) o2);
			}
			else {
				if (!param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dateinterval@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
				if ( !(o1 instanceof Sequence) ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dateinterval@" + option + " " + mm.getMessage("function.paramTypeError"));
				}
				dmr.init((Sequence) o1);
				o1 = srcSequence.get(1);
				if (o1 instanceof Sequence) {
					seqs = Prep.seqToSeqs(srcSequence);
				}
				else {
					seqs = new ArrayList<Sequence>();
					seqs.add(srcSequence);
				}
			}
			ArrayList<Sequence> ncv = new ArrayList<Sequence>();
			ArrayList<String> ncn = new ArrayList<String>();
			if (cns == null) {
				int len = seqs.size();
				cns = new String[len];
				for (int i = 1; i <= len; i++) {
					cns[i-1] = "Date"+i;
				}
			}
			dateInterval(dmr, seqs, cns, ncv, ncn);
			return Prep.toTab(ncn, ncv);
		}
		else {
			Sequence tvs = null;
			byte tType = -1;
			String[] cns = null;
			ArrayList<Sequence> seqs = null;
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
				if (param == null || param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dateinterval" + mm.getMessage("function.invalidParam"));
				}
				Record r1 = null;
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				if (param.getSubSize() < 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dateinterval" + mm.getMessage("function.invalidParam"));
				}
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(0);
				Object o1 = sub1 == null ? null : sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2 == null ? null : sub2.getLeafExpression().calculate(ctx);
				if (!(o2 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dateinterval" + mm.getMessage("function.paramTypeError"));
				}
				tvs = (Sequence) o2;
				if (o1 instanceof Sequence) {
					Sequence seq = (Sequence) o1;
					int len = seq.length();
					int[] cols = new int[len];
					cns = new String[len];
					for (int i = 0; i < len; i++) {
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
				else {
					// 允许不定义选择列名，此时使用全部字段
					seqs = Prep.pseqToSeqs(srcSequence);
					cns = r1.getFieldNames();
				}
			}
			else {
				if (!(param.isLeaf())) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dateinterval" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
				if ( !(o1 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dateinterval" + mm.getMessage("function.paramTypeError"));
				}
				tvs = (Sequence) o1;
				int len = tvs.length();
				cns = new String[len];
				for (int i = 1; i <= len; i++) {
					cns[i-1] = "Date"+i;
				}
				o1 = srcSequence.get(1);
				if (o1 instanceof Sequence) {
					seqs = Prep.seqToSeqs(srcSequence);
				}
				else {
					// 只有一列
					seqs = new ArrayList<Sequence>();
					seqs.add(srcSequence);
				}
			}
			if (tType < 1) {
				tType = Prep.getType(tvs);
			}
			ArrayList<Sequence> ncv = new ArrayList<Sequence>();
			ArrayList<String> ncn = new ArrayList<String>();
			DiMvpRec dmr = dateInterval(seqs, cns, tvs, tType, ncv, ncn);
			Sequence result = new Sequence(2);
			Table tab = Prep.toTab(ncn, ncv);
			result.add(tab);
			result.add(dmr.toSeq());
			return result;
		}
	}
	
	
	private static double P_maxMI = 0.95;

	/**
	 * 对数值目标变量做纠偏处理
	 * @param tvs	数值目标变量整列值
	 * @param cn	变量名
	 * @return
	 */
	protected static DiMvpRec dateInterval(ArrayList<Sequence> dvs, String[] dns, Sequence tvs, byte tType,
			ArrayList<Sequence> ncv, ArrayList<String> ncn) {
		DiMvpRec dmr = new DiMvpRec(null);
		//流程：
		//	(d) 对于所有的date，计算任意两个date的差值天数，命名为
		//		“distance_字段名1_字段名2”，只保留差值全部为正或全部为负的衍生字段，	
		//		全部为负的衍生字段取绝对值.
		int dcsize = dvs.size();
		Sequence dv1 = dvs.get(1);
		int length = dv1.length();
		//ArrayList<Integer> dateLocs = pr.getDateLocs();
		//Sequence tvs = pr.getTvs();
		//DMCTable srcTab = pr.getSrcTable();
		
		//int dcsize = dateLocs.size();
		//int length = tvs.length();
		if (dcsize < 2) {
			return null;
		}

		dv1 = null;
		Sequence dv2 = null;
		Sequence rel = null;
		for (int i = 0; i < dcsize; i++) {
			for (int j = 0; j < dcsize; j++) {
				if (i == j) {
					continue;
				}
				dv1 = dvs.get(i);
				dv2 = dvs.get(j);
				
				rel = new Sequence(length);
				boolean normal = true;
				for (int r = 1; r <= length; r++) {
					Object o1 = dv1.get(r);
					Object o2 = dv2.get(r);
					if ((o1 instanceof java.util.Date) && (o2 instanceof java.util.Date)) {
						int relday = (int) Variant.interval((java.util.Date) o1,
								(java.util.Date) o2, null);
						if (relday < 0) {
							//放宽一些要求，保留日期差值全部为非负的列
							normal = false;
							dv1 = null;
							dv2 = null;
							rel = null;
							break;
						}
						rel.add(Integer.valueOf(relday));
					}
					else if (o1 == null || o2 == null) {
						rel.add(null);
					}
					else {
						normal = false;
						dv1 = null;
						dv2 = null;
						rel = null;
						break;
					}
				}
				if (normal) {
					//String[] cns = {dns[i], dns[j]};
					// cns是处理重要度使用的，函数计算中缺乏整体的处理记录，不用了
					String newCn = "distance_" + dns[i] + "_" + dns[j];
					VarDateInterval vdi = new VarDateInterval(newCn, Consts.F_NUMBER);
					vdi.init(rel);
					double freq = vdi.getMissingRate();
					// 不参与建模的判定有可能变化
					if (freq > P_maxMI) {
						//缺失率大于95%，该字段不参与建模
						dv1 = null;
						dv2 = null;
						rel = null;
						continue;
					}
					vdi.setDateVar(dns[i], dns[j]);
					/*
					/* 函数中不直接根据列名获取数据
					VarSrcInfo vsi = pr.getVarSrcInfo(vns[i], false);
					vsi.addDateInterval(vdi);
					vsi = pr.getVarSrcInfo(vns[j], false);
					vsi.addDateInterval(vdi);
					*/
					VarRec vr = new VarRec(false, false, vdi);

					// 先处理整字段的MissingIndicator
					// 这里预留，由于前面生成日期差值列时的要求是全正或全负，因此是不会出现缺失的
					//（实际上考虑到计算差值时仅保留整数，所以目前执行的是全非负，全负的会在字段调转时记录）
					//recMI(rel, newCn, pr, freq, vr);

					byte type = Prep.getType(rel);
					vr.setType(type);
					vdi.setType(type);
					if (type == Consts.F_SINGLE_VALUE) {
						// 衍生的日期差字段为单值，不添加了
						vdi.setStatus(VarInfo.VAR_DEL_SINGLE);
						dv1 = null;
						dv2 = null;
						rel = null;
					} else if (type == Consts.F_TWO_VALUE) {
						// 衍生的日期差字段为二值
						//dmr.addIntervalRec(vr, dns[i], dns[j]);
						dmr.addIntervalRec(vr, String.valueOf(i), String.valueOf(j));
						//dealEnum(rel, newCn, pr, freq, vr, Consts.F_TWO_VALUE,
						//		ResultCol.CL_OTHERS, (byte) 0, cns, vdi);
						Prep.dealEnum(rel, newCn, freq, vr, Consts.F_TWO_VALUE, tvs, tType, null, ncv, ncn);
					} else if (type == Consts.F_ENUM) {
						// 衍生的日期差字段为枚举型
						dmr.addIntervalRec(vr, String.valueOf(i), String.valueOf(j));
						//dmr.addIntervalRec(vr, dns[i], dns[j]);
						//dealEnum(rel, newCn, pr, freq, vr, Consts.F_ENUM,
						//		ResultCol.CL_OTHERS, (byte) 0, cns, vdi);
						Prep.dealEnum(rel, newCn, freq, vr, Consts.F_ENUM, tvs, tType, null, ncv, ncn);
					} else if (type == Consts.F_COUNT || type == Consts.F_NUMBER) {
						// 剩下的可能就是为计数型了
						dmr.addIntervalRec(vr, String.valueOf(i), String.valueOf(j));
						//dmr.addIntervalRec(vr, dns[i], dns[j]);
						//vr = dealNumerical(rel, newCn, pr, freq, vr, Consts.F_NUMBER,
						//		ResultCol.CL_OTHERS, (byte) 0, cns, vdi);
						Prep.dealNumerical(rel, newCn, freq, vr, Consts.F_ENUM, null, ncv, ncn);
					}
					//if (rel != null) {
						//ncv.add(rel);
						//ncn.add(newCn);
					//}
				}
			}
		}
		return dmr;
	}
	
	protected static void dateInterval(DiMvpRec dmr, ArrayList<Sequence> dvs, String[] dns,
			ArrayList<Sequence> ncv, ArrayList<String> ncn) {
		//流程：
		//	(d) 对于所有的date，计算任意两个date的差值天数，命名为
		//		“distance_字段名1_字段名2”，只保留差值全部为正或全部为负的衍生字段，	
		//		全部为负的衍生字段取绝对值.
		ArrayList<VarRec> vrs = dmr.getIntervalRecs();
		Sequence dv1 = dvs.get(1);
		int length = dv1.length();
		// 添加大数据预测兼容处理
		int cols = vrs == null ? 0 : vrs.size();
		if (cols > 0) {
			ArrayList<String> interval1 = dmr.getInterval1();
			ArrayList<String> interval2 = dmr.getInterval2();
			// edited by bd, 2022.5.3, 函数中记录的interval1和interva2都是字段序号（1开始）
			for (int c = 0; c < cols; c++) {
				String cn1 = interval1.get(c);
				String cn2 = interval2.get(c);
				int di1 = Integer.valueOf(cn1);
				int di2 = Integer.valueOf(cn2);
				dv1 = dvs.get(di1);
				Sequence dv2 = dvs.get(di2);
				Sequence rel = new Sequence(length);
				for (int r = 1; r <= length; r++) {
					Object o1 = dv1.get(r);
					Object o2 = dv2.get(r);
					if ((o1 instanceof java.util.Date) && (o2 instanceof java.util.Date)) {
						int relday = (int) Variant.interval((java.util.Date) o1,
								(java.util.Date) o2, null);
						rel.add(Integer.valueOf(relday));
					}
					else {
						rel.add(null);
					}
				}
				
				VarRec vr = vrs.get(c);
				if (!vr.onlyHasMI()) {
					//String newCn = "distance_" + cn1 + "_" + cn2;
					String newCn = "distance_" + dns[di1] + "_" + dns[di2];
					//reprep(vr, rel, newCn, vr.getType(), pr);
					Prep.prep(vr, rel, newCn, ncv, ncn);
				}
			}
		}
	}
}
