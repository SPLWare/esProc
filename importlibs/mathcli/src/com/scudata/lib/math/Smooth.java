package com.scudata.lib.math;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.special.Erf;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.lib.math.prec.Consts;
import com.scudata.lib.math.prec.NorRec;
import com.scudata.lib.math.prec.NumStatis;
import com.scudata.lib.math.prec.SCRec;
import com.scudata.lib.math.prec.SmMulRec;
import com.scudata.lib.math.prec.SmRec;
import com.scudata.lib.math.prec.VarInfo;
import com.scudata.lib.math.prec.SmMulRec.SmDerive;
import com.scudata.resources.EngineMessage;
import com.scudata.common.MessageManager;

/**
 * 计算偏度
 * @author bd
 * P.smooth(cn, T)/A.smooth(T), @bnie 选项指明目标类型，各选项相斥，优先级按照二值/数值/整数/枚举，无选项自动处理
 * P.smooth@r(cn, Rec)/A.smooth@r(Rec)
 */
public class Smooth extends SequenceFunction {
	public Object calculate(Context ctx) {
		boolean cover = option != null && option.indexOf('c') > -1;
		boolean re = option != null && option.indexOf('r') > -1;
		boolean ori = option != null && option.indexOf('o') > -1;
		String cn = "smooth";
		if (re) {
			if (param == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("smooth@" + option + " " + mm.getMessage("function.invalidParam"));
			}
			Sequence seq = srcSequence;
			SmRec smRec = new SmRec();
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
					throw new RQException("smooth@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				if (param.isLeaf() || param.getSubSize() < 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				Object o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				if (o1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth@" + option + " " + mm.getMessage("function.paramTypeError"));
				}
				if ( !(o2 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth@" + option + " " + mm.getMessage("function.paramTypeError"));
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
				smRec = smRec.init((Sequence) o2);
			}
			else {
				if (!param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth@" + option + " " + mm.getMessage("function.invalidParam"));
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
				if ( !(o1 instanceof Sequence) ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth@" + option + " " + mm.getMessage("function.paramTypeError"));
				}
				smRec = smRec.init((Sequence) o1);
				if (!cover) {
					seq = Prep.dup(seq);
				}
			}
			ArrayList<Sequence> ncv = new ArrayList<Sequence>();
			ArrayList<String> ncn = new ArrayList<String>();
			smooth(seq, cn, smRec, ncv, ncn);
			if (ncn.size() == 0) {
				ncv.add(seq);
				cn = "Smooth_" + cn;
				ncn.add(cn);
			}
			return Prep.toTab(ncn, ncv);
		}
		else {
			Sequence seq = srcSequence;
			Sequence tvs = null;
			byte tType = 0;
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
			Record r1 = null;
			int col = 0;
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				if (param == null ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth" + mm.getMessage("function.invalidParam"));
				}
				if (param.isLeaf() || param.getSubSize() < 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth" + mm.getMessage("function.invalidParam"));
				}
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				Object o1 = sub1 == null ? null : sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2 == null ? null : sub2.getLeafExpression().calculate(ctx);
				if (o1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth" + mm.getMessage("function.paramTypeError"));
				}
				if ( !(o2 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth" + mm.getMessage("function.paramTypeError"));
				}
				if (o1 instanceof Number) {
					col = ((Number) o1).intValue() - 1;
					cn = r1.dataStruct().getFieldName(col);
				}
				else {
					cn = o1.toString();
					col = r1.dataStruct().getFieldIndex(cn);
				}
				seq = Prep.getFieldValues(srcSequence, col);
				tvs = (Sequence) o2;
			}
			else {
				if (param == null || !(param.isLeaf())) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
				if ( !(o1 instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("smooth" + mm.getMessage("function.paramTypeError"));
				}
				tvs = (Sequence) o1;
				if (!cover ) {
					seq = Prep.dup(seq);
				}
			}
			if (tType < 1) {
				tType = Prep.getType(tvs);
			}
			ArrayList<Sequence> ncv = null;
			ArrayList<String> ncn = null;
			if (tType == Consts.F_ENUM) {
				// 多值目标，会生成多列
				ncv = new ArrayList<Sequence>();
				ncn = new ArrayList<String>();
			}
			SmRec smRec = smooth(seq, cn, !ori, tvs, tType, ncv, ncn);
			Sequence result = smRec.toSeq();
			if (ncv!= null && ncv.size() > 0) {
				Table tab = Prep.toTab(ncn, ncv);
				Sequence bak = new Sequence(2);
				bak.add(tab);
				bak.add(result);
				return result;
			}
			if (cover) {
				if (r1 != null) {
					if (smRec != null) {
						cn = "Smooth_" + cn;
					}
					Prep.coverPSeq(srcSequence, seq, cn, r1.dataStruct(), col);
				}
				//return result;
			}
			Sequence bak = new Sequence(2);
			bak.add(seq);
			bak.add(result);
			return bak;
		}
	}
	
	protected static void smooth(Sequence x, NorRec nr) {
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
	
	private static double SmoothFactor = 5d;
	/**
	 * 平滑化，将结果改变到Vs中
	 * @param Vs	某数值列的数据
	 * @param x	数值列经过纠偏nmnv处理的数据
	 * @param avg	平均值
	 * @param sd	方差
	 * @return
	 */
	/**
	 * 平滑化，记录结果
	 * @param cvs	变量值
	 * @param vn	变量名
	 * @param tvs	目标值
	 * @param ifErf	是否使用erf
	 * @param tarType	目标类型
	 * @param ncv	结果的新变量值
	 * @param ncn	结果的新变量名
	 * @return
	 */
	protected static SmRec smooth(Sequence cvs, String vn, boolean ifErf, Sequence tvs, byte tarType,
			ArrayList<Sequence> ncv, ArrayList<String> ncn) {
		SmRec sr = new SmRec();
		// 根据erf参数，决定是否使用逆误差函数来做预处理
		if (ifErf) {
			// 逆误差函数处理平滑化，预处理v2.5
			if (tarType == Consts.F_TWO_VALUE) {
				// 目标变量是二值型，执行smbtcv
				Sequence X = cvs.id(null);
				// 增加对多值目标的处理, 对普通二值，处理时选1
				Sequence erf = calcRBinary(cvs, tvs, 1, X);
				Prep.setSequenceB(cvs, X, erf);
				sr.setX(X);
				sr.setX1(erf);
			} else if (tarType == Consts.F_ENUM) {
				// 多值目标的预处理开始支持
				Sequence X = cvs.id(null);
				int[] vcs = calcCount(tvs);
				int size = vcs.length;
				// 舍弃最低分类
				int mini = 0;
				int min = vcs[0];
				for (int i = 0; i < size; i++) {
					if (vcs[i] < min) {
						min = vcs[i];
						mini = i;
					}
				}
				// 接口添加字段名参数，这是因为在产生衍生变量时也可能需要平滑化，此时不能使用初始字段名，否则会出现问题
				vn = "Smooth_" + vn;
				sr = new SmMulRec();
				SmMulRec smr = (SmMulRec) sr;
				// 报告信息的衍生变量名，不记录纠偏调整信息，暂不记录平滑化衍生
				//vsi.setName(vn);
				//vsi.setIfSmooth(true);
				//vsi.setIfSmoothDerive(true);
				ArrayList<String> smcns = new ArrayList<String>(size - 1);
				ArrayList<VarInfo> smvis = new ArrayList<VarInfo>(size - 1);
				// 生成的
				for (int i = 0; i < size; i++) {
					if (i == mini) {
						// 最低分类，舍去
						smcns.add(null);
						continue;
					}
					// 接口添加字段名参数，这是因为在产生衍生变量时也可能需要平滑化，此时不能使用初始字段名，否则会出现问题
					String cni = vn+"_"+i+"_TCs";
					VarInfo smvi = new VarInfo(cni, Consts.F_TWO_VALUE);
					SmRec sri = new SmRec();
					sri.setTv(i);
					Sequence erf = calcRBinary(cvs, tvs, i, X);
					Sequence cvsi = Prep.createSequenceB(cvs, X, erf);
					smvi.init(cvsi);
					sri.setX(X);
					sri.setX1(erf);
					
					// step12, 纠偏变换
					SCRec scr = CorSkew.corSkew(cvsi, cni);
					cni = scr.getPrefix() + cni;
					// step13, 清理异常值
					NumStatis ns = scr.getNumStatis();
					Sert.sertSeq(cvsi, ns.getAvg(), ns.getSd(cvs));

					// step15，保留下来的数值变量，做归一化处理
					NorRec nr = Normal.normal(cvsi);
					
					smr.addDerive(sri, scr, nr);

					//if (level == ResultCol.CL_DATE ) {
						//时间字段，需要用排序
					//	pr.addCol(cni, cvsi, level, this.ci, index, srcCn);
					//}
					//else {
						// 对平滑化衍生列，暂时先不排那么细致
						//pr.addCol(cni, cvsi, level, this.ci, (byte)(i+1), srcCn);
					//}
					ncv.add(cvsi);
					ncn.add(cni);
					smcns.add(cni);
					smvi.setName(cni);
					smvi.setIfSmoothDerive(true);
					smvis.add(smvi);
				}
				//pr.addSmDerive(smcns);
			} else {
				// 目标变量是数值型的erf处理
				Sequence X = cvs.id(null);
				Sequence erf = calcRNumerical(cvs, tvs, X);
				Prep.setSequenceB(cvs, X, erf);
				sr.setX(X);
				sr.setX1(erf);
			}
		}
		else {
			// 原处理方案
			if (tarType == Consts.F_TWO_VALUE) {
				// 目标变量是二值型，执行smbtcv
				Sequence X = cvs.id(null);
				double r1 = Freq.freq(tvs, new Integer(1));
				double r0 = 1d - r1;
				// 增加对多值目标的处理, 对普通二值，处理时选1
				Sequence[] Ns = calcNBinary(cvs, tvs, 1, X);
				Sequence N1 = Ns[1];
				Sequence N0 = Ns[0];
				Sequence X1 = Prep.calcX1B(N1, r1, N0, r0, SmoothFactor);
				Prep.setSequenceB(cvs, X, X1);
				sr.setX(X);
				sr.setX1(X1);
			} else if (tarType == Consts.F_ENUM) {
				// 多值目标的预处理开始支持
				Sequence X = cvs.id(null);
				int[] vcs = calcCount(tvs);
				int size = vcs.length;
				// 舍弃最低分类
				int mini = 0;
				int min = vcs[0];
				for (int i = 0; i < size; i++) {
					if (vcs[i] < min) {
						min = vcs[i];
						mini = i;
					}
				}
				vn = "Smooth_" + vn;
				sr = new SmMulRec();
				SmMulRec smr = (SmMulRec) sr;
				// 报告信息的衍生变量名，不记录纠偏调整信息，暂不记录平滑化衍生
				//vsi.setName(cn);
				//vsi.setIfSmooth(true);
				//vsi.setIfSmoothDerive(true);
				ArrayList<String> smcns = new ArrayList<String>(size - 1);
				ArrayList<VarInfo> smvis = new ArrayList<VarInfo>(size - 1);
				// 生成的
				for (int i = 0; i < size; i++) {
					if (i == mini) {
						// 最低分类，舍去
						smcns.add(null);
						continue;
					}
					String cni = vn+"_"+i+"_TCs";
					VarInfo smvi = new VarInfo(cni, Consts.F_TWO_VALUE);
					SmRec sri = new SmRec();
					sri.setTv(i);
					double r1 = vcs[i]*1d/tvs.length();
					double r0 = 1d - r1;
					Sequence[] Ns = calcNBinary(cvs, tvs, i, X);
					Sequence N1 = Ns[1];
					Sequence N0 = Ns[0];
					Sequence X1 = Prep.calcX1B(N1, r1, N0, r0, SmoothFactor);
					Sequence cvsi = Prep.createSequenceB(cvs, X, X1);
					smvi.init(cvsi);
					sri.setX(X);
					sri.setX1(X1);
					
					// step12, 纠偏变换
					SCRec scr = CorSkew.corSkew(cvsi, cni);
					cni = scr.getPrefix() + cni;
					// step13, 清理异常值
					NumStatis ns = scr.getNumStatis();
					Sert.sertSeq(cvsi, ns.getAvg(), ns.getSd(cvs));

					// step15，保留下来的数值变量，做归一化处理
					NorRec nr = Normal.normal(cvsi);
					
					smr.addDerive(sri, scr, nr);

					//if (level == ResultCol.CL_DATE ) {
						//时间字段，需要用排序
						//pr.addCol(cni, cvsi, level, this.ci, index, srcCn);
					//}
					//else {
						// 对平滑化衍生列，暂时先不排那么细致
						//pr.addCol(cni, cvsi, level, this.ci, (byte)(i+1), srcCn);
					//}
					ncv.add(cvsi);
					ncn.add(cni);
					smcns.add(cni);
					smvi.setName(cni);
					smvi.setIfSmoothDerive(true);
					smvis.add(smvi);
				}
				//pr.addSmDerive(smcns);
			} else {
				// 目标变量是数值型smntcv
				Sequence X = cvs.id(null);		
				double y = ((Number) tvs.average()).doubleValue();
				Sequence N = Prep.calcN3(cvs, X);
				Sequence Y = Prep.calcY(cvs, tvs, X);
				Sequence X1 = Prep.calcX1N2(X, N, Y, y, SmoothFactor);
				Prep.setSequenceB(cvs, X, X1);
				sr.setX(X);
				sr.setX1(X1);
			}
		}
		return sr;
	}
	
	protected static void smooth(Sequence cvs, String cn, SmRec smRec, ArrayList<Sequence> ncv, ArrayList<String> ncn) {
		if (smRec != null) {
			// 高基数分类变量
			//  平滑化会有两种情况，目标为二值或多值
			if (smRec instanceof SmMulRec) {
				SmMulRec smr = (SmMulRec) smRec;
				ArrayList<SmDerive> sds = smr.getDerives();
				cn = "Smooth_" + cn;
				for (SmDerive sd : sds) {
					SmRec sr = sd.getSmRec();
					int tv = sr.getTv();
					String cni = cn+"_"+tv+"_TCs";
					Sequence X = sr.getX();
					Sequence X1 = sr.getX1();
					//这里目标变量只会是多值型，执行smbtcv
					Sequence cvsi = Prep.createSequenceB(cvs, X, X1);

					// step12, 纠偏变换
					SCRec scRec = sd.getSCRec();
					if (scRec == null) {
						// 如果未执行纠偏，说明是二值变量，直接添加了列值返回就是了
						ncn.add(cni);
						ncv.add(cvsi);
						//pr.addCol2(cni, cvsi);
						continue;
					}
					CorSkew.corSkew(cvsi, cni, scRec);
					cni = scRec.getPrefix() + cni;

					NumStatis ns = scRec.getNumStatis();
					// step13, 清理异常值
					Sert.sertSeq(cvsi, ns.getAvg(), ns.getSd(cvsi));

					// step15，保留下来的数值变量，做归一化处理
					NorRec nr = sd.getNorRec();
					Normal.normal(cvsi, nr);
					ncn.add(cni);
					ncv.add(cvsi);
				}
				// 平滑化衍生的话，至此本列的各个衍生列已经生成及处理完毕
			}
			else {
				// step11, 平滑化
				cn = "Smooth_" + cn;
				Sequence X = smRec.getX();
				Sequence X1 = smRec.getX1();
				
				Prep.setSequenceB(cvs, X, X1);
			}
		}
	}
	
	private static int[] calcCount(Sequence tvs) {
		int[] vcs = {0, 0};
		int size = tvs == null ? 0 : tvs.length();
		for (int i = 0; i < size; i++) {
			Object o = tvs.get(i + 1);
			int n = 0;
			if (o instanceof Number) {
				n = ((Number) o).intValue();
			}
			if (n >= vcs.length) {
				vcs = Arrays.copyOf(vcs, n + 1);
			}
			vcs[n] = vcs[n] +1;
		}
		return vcs;
	}
	
	/**
	 * 优化的平滑化计算，一次循环计算出各个分类的Ni0和Ni1的两个系列
	 * 要求平滑化的枚举序列是数列，就不再去用对象比较判断相等了
	 * @param Vs
	 * @param Ts
	 * @param X
	 * @return
	 */
	private static Sequence[] calcNBinary(Sequence cvs, Sequence tvs, int tv, Sequence X) {
		Sequence[] result = new Sequence[2];
		int xlen = X == null ? 0 : X.length();
		int[] xs = new int[xlen];
		int[] n0 = new int[xlen];
		int[] n1 = new int[xlen];
		for (int i = 0; i < xlen; i++) {
			xs[i] = ((Number) X.get(i+1)).intValue();
			n0[i] = 0;
			n1[i] = 0;
		}
		// 增加对多值目标的处理
		if (tv < 0) {
			// 如果未指明tv，使用-1作为初始值，此时会将其设为1
			tv = 1;
		}
		int dlen = tvs.length();
		for (int d = 1; d<= dlen; d++) {
			int t = ((Number) tvs.get(d)).intValue();
			int v = ((Number) cvs.get(d)).intValue();
			int p = xlen - 1;
			for (int i = 0; i < xlen - 1; i++) {
				if (v == xs[i]) {
					p = i;
					break;
				}
			}
			// 增加对多值目标的处理
			if (t == tv) {
				n1[p] = n1[p] + 1;
			}
			else {
				n0[p] = n0[p] + 1;
			}
		}
		Sequence N0 = new Sequence(xlen);
		Sequence N1 = new Sequence(xlen);
		for (int i = 0; i < xlen; i++) {
			N0.add(Integer.valueOf(n0[i]));
			N1.add(Integer.valueOf(n1[i]));
		}
		result[0] = N0;
		result[1] = N1;
		return result;
	}
	
	private static double sqrt2 = Math.sqrt(2);
	/**
	 * v2.5平滑化计算，逆误差函数之前，计算各个分类的正样本排名折算值数组R
	 * 要求平滑化的枚举序列是数列，就不再去用对象比较判断相等了
	 * @param Vs
	 * @param Ts
	 * @param X
	 * @return
	 */
	private static Sequence calcRBinary(Sequence cvs, Sequence tvs, int tv, Sequence X) {
		int xlen = X == null ? 0 : X.length();
		int[] xs = new int[xlen];
		int[] n = new int[xlen];
		int[] n1 = new int[xlen];
		for (int i = 0; i < xlen; i++) {
			xs[i] = ((Number) X.get(i+1)).intValue();
			n[i] = 0;
			n1[i] = 0;
		}
		// 增加对多值目标的处理
		if (tv < 0) {
			// 如果未指明tv，使用-1作为初始值，此时会将其设为1
			tv = 1;
		}
		int dlen = tvs.length();
		for (int d = 1; d<= dlen; d++) {
			int t = ((Number) tvs.get(d)).intValue();
			int v = ((Number) cvs.get(d)).intValue();
			int p = xlen - 1;
			for (int i = 0; i < xlen - 1; i++) {
				if (v == xs[i]) {
					p = i;
					break;
				}
			}
			// 增加对多值目标的处理
			n[p] = n[p] + 1;
			//if (t == 1) {
			if (t == tv) {
				n1[p] = n1[p] + 1;
			}
		}
		// 正样本占比
		Sequence y = new Sequence(xlen);
		for (int i = 0; i < xlen; i++) {
			y.add(n1[i]*1d/n[i]);
		}
		// 折算排名
		Sequence R = y.ranks(null);
		Sequence erf = new Sequence(xlen);
		for (int i = 0; i < xlen; i++) {
			double d = ((Number) R.get(i+1)).doubleValue();
			double r = (d-0.5)/xlen;
			erf.add(sqrt2 * Erf.erfInv(2*r - 1));
		}
		// 逆误差计算
		return erf;
	}

	/**
	 * v2.5平滑化计算，数值型目标，逆误差函数之前，计算各个分类的正样本排名折算值数组R
	 * 要求平滑化的枚举序列是数列，就不再去用对象比较判断相等了
	 * @param Vs
	 * @param Ts
	 * @param X
	 * @return
	 */
	private static Sequence calcRNumerical(Sequence cvs, Sequence tvs, Sequence X) {
		Sequence y = Prep.calcY(cvs, tvs, X);
		int xlen = X == null ? 0 : X.length();
		// 折算排名
		Sequence R = y.ranks(null);
		Sequence erf = new Sequence(xlen);
		for (int i = 0; i < xlen; i++) {
			double d = ((Number) R.get(i+1)).doubleValue();
			double r = (d-0.5)/xlen;
			erf.add(sqrt2 * Erf.erfInv(2*r - 1));
		}
		// 逆误差计算
		return erf;
	}
}
