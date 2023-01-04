package com.scudata.lib.math;

import com.scudata.lib.math.prec.NumStatis;
import com.scudata.lib.math.prec.SCRec;
import com.scudata.resources.EngineMessage;
import com.scudata.common.MessageManager;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;

/**
 * 数值目标变量的纠偏处理
 * @author bd
 * A.tskew()/P.tskew(tn), A.tskew@r(rec)/P.tskew@r(tn, rec)
 * 清理数据集D的离散变量V
 */
public class TSkew extends SequenceFunction {

	public Object calculate(Context ctx) {
		boolean cover = option != null && option.indexOf('c') > -1;
		boolean re = option != null && option.indexOf('r') > -1;
		String cn = "tskew";
		if (re) {
			if (param == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("tskew@" + option + " " + mm.getMessage("function.invalidParam"));
			}
			Sequence seq = srcSequence;
			Object o1 = null;
			Record r1 = null;
			int col = 0;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				if (param.isLeaf() || param.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("tskew@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("tskew@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				o1 = sub2.getLeafExpression().calculate(ctx);
				Object o2 = sub1.getLeafExpression().calculate(ctx);
				if (o2 == null || !(o1 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("tskew@" + option + " " + mm.getMessage("function.paramTypeError"));
				}
				if (o2 instanceof Number) {
					col = ((Number) o2).intValue() - 1;
					cn = r1.dataStruct().getFieldName(col);
				}
				else {
					cn = o2.toString();
					col = r1.dataStruct().getFieldIndex(cn);
				}
				seq = Prep.getFieldValues(srcSequence, col);
			}
			else {
				if (!param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("tskew@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				o1 = param.getLeafExpression().calculate(ctx);
				if (!(o1 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("tskew@" + option + " " + mm.getMessage("function.paramTypeError"));
				}
			}
			SCRec scRec = new SCRec();
			scRec.init((Sequence) o1); 
			if (scRec.getMode() == SCRec.MODE_ORI) {
				//如果预处理时目标未经过纠偏处理
				return seq;
			}
			
			double tp = scRec.getP();
			double tm = scRec.getNumStatis().getMin();
			
			Sequence result = Ptsi.ptsi(seq, tp, tm);
			if (cover) {
				if (r1 != null) {
					Prep.coverPSeq(srcSequence, result, null, r1.dataStruct(), col);
				}
				else {
					Prep.coverSeq(srcSequence, result);
				}
			}
			return result;
		}
		else {
			Sequence seq = srcSequence;
			Record r1 = null;
			int col = 0;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				if (param == null ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("tskew" + mm.getMessage("function.invalidParam"));
				}
				if (!param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("tskew" + mm.getMessage("function.invalidParam"));
				}
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
				if (o1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("tskew" + mm.getMessage("function.paramTypeError"));
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
			SCRec scRec = tskew(seq, cn);
			Sequence result = scRec.toSeq();
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
	
	// 目标值纠偏
	protected final static double SCT_MAX = 0.2;
	protected final static double SCT_MIN = -0.2;
	protected final static double SCT_MAX0 = 0.00001;
	protected final static double SCT_MIN0 = -0.00001;
	protected final static int SCT_MAXTRY = 10000;

	/**
	 * 对数值目标变量做纠偏处理
	 * @param tvs	数值目标变量整列值
	 * @param cn	变量名
	 * @param filePath	如果出现需要排序的情况，在数据较多时缓存文件的路径
	 * @return
	 */
	public static SCRec tskew(Sequence tvs, String cn) {
		SCRec scRec = new SCRec();
		double skew = Skew.skew(tvs);

		if (skew >= SCT_MIN && skew <= SCT_MAX) {
			// 2.4(b) 不需纠偏，返回
			scRec.setMode(SCRec.MODE_ORI);
			return scRec;
		}

		NumStatis ns = new NumStatis(tvs);
		scRec.setNumStatis(ns);
		Sequence pts1 = Pts.pts(tvs, ns.getMin(), 1);
		if (skew > SCT_MAX) {
			// 计算log(transbase(x)).skew继续判断
			Sequence power = Pts.power(pts1, 0d);
			double skew0 = recSCSkew(power, ns);
			if (skew0 >= SCT_MIN && skew0 <= SCT_MAX) { // step2
				// 2.4(c) log变换
				scRec.setMode(SCRec.MODE_LOG);
				// 这个位置纠偏后的值和判断值不同了，不用transpose，直接用ln新算法
				Pts.ptsSeq(tvs, 0d, 0d, true);
			} else if (skew0 < SCT_MIN) { // step 6
				// 2.4(d) 在(0,1)之间寻找p，使得Vs.pts(p).skew为0
				scRec.setMode(SCRec.MODE_POWER);
				power = Pts.power(pts1, 1d);
				double top = recSCSkew(power, ns);
				if (top <= SCT_MAX0 && top >= SCT_MIN0) {
					scRec.setP(1);
					tvs.setMems(power.getMems());
				} else if (top > SCT_MAX) {
					double high = 1d;
					double low = 0d;
					double p = 0.5d;
					power = Pts.power(pts1, p);
					double fp = recSCSkew(power, ns);
					int ti = 0;
					boolean normal = true;
					double bottom = skew0;
					while ((fp > SCT_MAX0 || fp < SCT_MIN0) && normal) {
						if (fp > top || fp < bottom) {
							MessageManager mm = EngineMessage.get();
							System.out.println(mm.getMessage("prep.targetNmnvError", cn,
									high, low, p, fp));
							normal = false;
						} else if (fp > SCT_MAX0) {
							top = fp;
							high = p;
						} else if (fp < SCT_MIN0) {
							bottom = fp;
							low = p;
						}
						p = (low + high) / 2;
						power = Pts.power(pts1, p);
						fp = recSCSkew(power, ns);
						if (ti++ >= SCT_MAXTRY) {
							MessageManager mm = EngineMessage.get();
							System.out.println(mm.getMessage("prep.targetNmnvExceed",
									cn, ti));
							normal = false;
						}
					}
					if (normal) {
						p = keep4(p);
						tvs.setMems(power.getMems());
						scRec.setP(p);
					} else { // step 8, condition A1
						MessageManager mm = EngineMessage.get();
						System.out.println(mm.getMessage("prep.targetNmnvSolution8", cn));
						// 没找到p，输出警告信息，目标变量不计算rank，直接不处理
						scRec.setMode(SCRec.MODE_ORI);
						return scRec;
					}
				} else { // step 8, condition A2
					MessageManager mm = EngineMessage.get();
					System.out.println(mm.getMessage("prep.targetNmnvReverse", cn, 0,
							skew0, 1, top));
					System.out.println(mm.getMessage("prep.targetNmnvSolution8", cn));
					// 没找到p，输出警告信息，目标变量不计算rank，直接不处理
					scRec.setMode(SCRec.MODE_ORI);
					return scRec;
				}
			} else { // step 3
						// 目标变量不计算rank，直接不处理
				scRec.setMode(SCRec.MODE_ORI);
				return scRec;
			}
		} else { // skew < Min
					// 计算transbase(x)^2.skew继续判断
			Sequence power = Pts.power(pts1, 2d);
			double skew2 = recSCSkew(power, ns);
			if (skew2 >= SCT_MIN && skew2 <= SCT_MAX) { // step4
				// 2.13(e) 平方变换
				scRec.setP(2d);
				tvs.setMems(power.getMems());
				scRec.setMode(SCRec.MODE_SQUARE);
			} else if (skew2 < SCT_MIN) { // step 5
				// 先用sert清理，然后再判断
				// step12(g)，目标变量不计算rank，直接不处理
				scRec.setMode(SCRec.MODE_ORI);
				return scRec;
			} else { // step 7
						// step12(f) 在(0,1)之间寻找p，使得Vs.pts(p).skew为0
				scRec.setMode(SCRec.MODE_POWER);
				power = Pts.power(pts1, 1d);
				double bottom = recSCSkew(power, ns);
				if (bottom >= SCT_MIN0 && bottom <= SCT_MAX0) {
					scRec.setP(1);
					tvs.setMems(power.getMems());
				} else if (bottom < SCT_MIN) {
					double high = 2d;
					double low = 1d;
					double p = 1.5d;
					power = Pts.power(pts1, p);
					double fp = recSCSkew(power, ns);
					int ti = 0;
					boolean normal = true;
					double top = skew2;
					while ((fp > SCT_MAX0 || fp < SCT_MIN0) && normal) {
						if (fp > top || fp < bottom) {
							MessageManager mm = EngineMessage.get();
							System.out.println(mm.getMessage("prep.targetNmnvError", cn,
									high, low, p, fp));
							normal = false;
						} else if (fp > SCT_MAX0) {
							top = fp;
							high = p;
						} else if (fp < SCT_MIN0) {
							bottom = fp;
							low = p;
						}
						p = (low + high) / 2;
						power = Pts.power(pts1, p);
						fp = recSCSkew(power, ns);
						if (ti++ >= SCT_MAXTRY) {
							MessageManager mm = EngineMessage.get();
							System.out.println(mm.getMessage("prep.targetNmnvExceed",
									cn, ti));
							normal = false;
						}
					}
					if (normal) {
						p = keep4(p);
						tvs.setMems(power.getMems());
						scRec.setP(p);
					} else { // step 8, condition B1
						MessageManager mm = EngineMessage.get();
						System.out.println(mm.getMessage("prep.targetNmnvSolution8", cn));
						// 没找到p，输出警告信息，目标变量不计算rank，直接不处理
						scRec.setMode(SCRec.MODE_ORI);
						return scRec;
					}
				} else { // step 8, condition B2
					MessageManager mm = EngineMessage.get();
					System.out.println(mm.getMessage("prep.targetNmnvReverse", cn, 2,
							skew2, 1, bottom));
					System.out.println(mm.getMessage("prep.targetNmnvSolution8", cn));
					// 没找到p，输出警告信息，目标变量不计算rank，直接不处理
					scRec.setMode(SCRec.MODE_ORI);
					return scRec;
				}
			}
		}
		scRec.setPrefix(tvs);
		return scRec;
	}
	
	// copied from ColProcessor
	private static double recSCSkew(Sequence seq, NumStatis ns) {
		Object avg = seq.average();
		if (avg instanceof Number) {
			double avgValue = ((Number) avg).doubleValue();
			int n = seq.length();
			double result2 = 0;
			double result3 = 0;
			for (int i = 1; i <= n; i++) {
				Number tmp = (Number) seq.get(i);
				double v = tmp == null ? 0 : tmp.doubleValue();
				if (tmp != null) {
					result2 += Math.pow(v - avgValue, 2);
					result3 += Math.pow(v - avgValue, 3);
				}
			}
			double div = result2 / n;
			div = Math.pow(div, 1.5);
			double skew = result3 / n / div;
			ns.setAvgSd(avgValue, Math.sqrt(result2 / (n - 1)));
			return skew;
		}
		return 0d;
	}

	// copied from ColProcessor
	private static double keep4(double p) {
		double power = 10000;
		return Math.round(p * power) / power;
	}
}
