package com.scudata.lib.math;

import java.util.ArrayList;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.lib.math.prec.Consts;
import com.scudata.lib.math.prec.FNARec;
import com.scudata.lib.math.prec.VarInfo;
import com.scudata.resources.EngineMessage;
import com.scudata.common.MessageManager;

/**
 * 计算偏度
 * @author bd
 * A.impute(), P.impute(cn), A.impute@r(rec), P.impute@r(cn, rec) 
 */
public class Impute extends SequenceFunction {
	
	public Object calculate(Context ctx) {
		boolean cover = option != null && option.indexOf('c') > -1;
		boolean re = option != null && option.indexOf('r') > -1;
		String cn = "impute";
		Sequence seq = srcSequence;
		Record r1 = null;
		int col = 0;
		if (re) {
			FNARec fRec = new FNARec();
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
				fRec.init((Sequence) o2);
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
					fRec.init((Sequence) o1);
				}
				if (!cover) {
					seq = Prep.dup(seq);
				}
			}
			impute(seq, cn, fRec);
			if (cover) {
				if (r1 != null) {
					Prep.coverPSeq(srcSequence, seq, null, r1.dataStruct(), col);
				}
				//return result;
			}
			return seq;
		}
		else {
			byte type = 0;
			if (option != null) {
				if (option.indexOf('B') > -1) {
					type = Consts.F_TWO_VALUE;
				}
				else if (option.indexOf('N') > -1) {
					type = Consts.F_NUMBER;
				}
				else if (option.indexOf('I') > -1) {
					type = Consts.F_COUNT;
				}
				else if (option.indexOf('E') > -1) {
					type = Consts.F_ENUM;
				}
				else if (option.indexOf('D') > -1) {
					type = Consts.F_DATE;
				}
			}
			if (srcSequence instanceof Table || srcSequence.isPmt()) {
				for (int i = 1, size = srcSequence.length(); i < size; i++ ) {
					Record r = (Record) srcSequence.get(i);
					if (r != null) {
						r1 = r;
						break;
					}
				}
				if (param == null || !param.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("impute" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = param.getLeafExpression().calculate(ctx);
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
			if (r1 == null && !cover ) {
				seq = Prep.dup(seq);
			}
			if (type < 1) {
				type = Prep.getType(seq);
			}
			FNARec fRec = recFNA(seq, cn, type);
			Sequence result = fRec == null ? null : fRec.toSeq();
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
	
	private static double P_m = 50d;

	/**
	 * 对数值变量做纠偏处理
	 * @param cvs	数值变量整列值
	 * @param cn	变量名
	 * @param filePath	如果出现需要排序的情况，在数据较多时缓存文件的路径，暂时不支持
	 * @return
	 */
	protected static FNARec recFNA(Sequence cvs, String cn, byte ctype) {
		if (cvs == null) {
			return null;
		}
		VarInfo vi = new VarInfo(cn, ctype);
		vi.init(cvs);
		return recFNA(cvs, cn, ctype, vi);
	}
	
	protected static FNARec recFNA(Sequence cvs, String cn, byte ctype, VarInfo vi) {
		if (cvs == null) {
			return null;
		}
		double pm = Impute.P_m;
		int size = cvs.length();
		double freq = vi.getMissingRate();

		FNARec fnaRec = new FNARec();
		int msize = 0;
		//众数
		Object maxv = null;

		if (ctype == Consts.F_COUNT) {
			//计数型，和下面的数值型补缺相对于枚举的几种类型都很简单，只需要补空，不需要考虑合并低频分类的事情
			maxv = Prep.clnvCount(cvs);
			fnaRec.setMissing(maxv);
		}
		else if (ctype == Consts.F_NUMBER ) {
			//数值型，补缺用的不是众数，是均值
			Prep.clnv(cvs, vi.getAverage());
			fnaRec.setMissing(vi.getAverage());
			vi.setFillMissing(vi.getAverage());
		}
		else if (ctype == Consts.F_ENUM || ctype == Consts.F_TWO_VALUE || ctype == Consts.F_SINGLE_VALUE ) {
			//二值、枚举，甚至可能执行至此的单值			
			if (size*1d/vi.getCategory() <= 50) {
				//分类变量太琐碎了，平均每个分类值不超过50个，变量删除
				vi.setStatus(VarInfo.VAR_DEL_CATEGORY);;
				return null;
			}
			
			//空值组
			ArrayList<Integer> nA = new ArrayList<Integer>();
			//低频分类组
			ArrayList<Integer> A = new ArrayList<Integer>();

			ArrayList<ArrayList<Integer>> groups = Prep.group(cvs);
			//枚举数
			int len = groups.size();
			
			int check = 6;
			if (freq >= Prep.MISSING_MAX || (freq <= Prep.MISSING_MIN && freq > 0)) {
				//空值存在，但是被替换为众数时，空值不占判断数，check要+1
				check ++;
			}
			
			boolean merge = true;
			if (len <= check) {
				//分类数<=6，此时不合并低频分类
				merge = false;
			}
			
			Object setting = Consts.CONST_OTHERNUMS;
			Object missing = Consts.CONST_NULLNUM;
			//被保留的分类值
			Sequence keepValues = new Sequence();
			//被合并消失的低频分类值
			Sequence otherValues = new Sequence();
			for (int i = 0; i < len; i++ ) {
				ArrayList<Integer> thisg = groups.get(i);
				size = thisg.size();
				if (size < 1) {
					continue;
				}
				Integer index = thisg.get(0);
				Object value = cvs.get(index.intValue());
				if (value == null) {
					//空值组，记录需填补的空值序号
					for (int ri = 0; ri < size;ri ++ ) {
						index = thisg.get(ri);
						nA.add(index);
					}
				}
				else if (merge && size < pm) {
					//需被合并的低频分类组
					otherValues.add(value);
					for (int ri = 0; ri < size;ri ++ ) {
						index = thisg.get(ri);
						A.add(index);
					}
				}
				else {
					//被保留的分类组，查看是否众数组
					keepValues.add(value);
					if (msize < size) {
						msize = size;
						maxv = value;
					}
				}
			}
			len = A.size();
			// edited by bd, 2022.5.13, 根据众数判断数据类型，如果非数值则使用字符串填充
			if (!(maxv instanceof Number)) {
				missing = Consts.CONST_NULL;
				setting = Consts.CONST_OTHERS;
			}
			// 规则修改，nA在频度不高于5%或不低于95%时，用众数置换null
			//需要保证众数不为null
			if ((freq >= Prep.MISSING_MAX || freq <= Prep.MISSING_MIN) && maxv != null) {
				for (Integer index : nA) {
					missing = maxv;
					cvs.set(index.intValue(), maxv);
				}
				nA.clear();
			}
			
			if (nA.size() < pm) {
				//空值为低频组，或者不存在，需和低频组合并处理
				if (len + nA.size() < pm && maxv != null) {
					//空值加入后，其它组仍然是低频组，用众数填补，此时需保证众数是存在的
					setting = maxv;
					fnaRec.setOtherValues(null);
				}
				else {
					// 空值和低频组，共同合并为新的“其它"组
					// 其它值会设定，如果不设定的话，这些值都会被设为众数，就没有小组值和null值的事情了, 存到UP里，等sgnv时再调整
					if (nA.size() > 0) {
						otherValues.add(null);
					}
					fnaRec.setOtherValues(otherValues);
				}
				for (Integer index : nA) {
					cvs.set(index.intValue(), setting);
				}
				for (Integer index : A) {
					cvs.set(index.intValue(), setting);
				}
				fnaRec.setSetting(setting);
				fnaRec.setMissing(setting);
				vi.setFillMissing(setting);
				vi.setFillOthers(setting);
				vi.setFillOthers(otherValues);
			}
			else {
				//空值组单独设置，考察低频合并组的情况
				if (len < pm && maxv != null) {
					//低频合并组总数未达标，用众数设置
					setting = maxv;
					fnaRec.setOtherValues(null);
				}
				else {
					//  其它值会设定，如果不设定的话，这些值都会被设为众数，就没有小组值和null值的事情了, 存到UP里，等sgnv时再调整
					fnaRec.setOtherValues(otherValues);
				}
				for (Integer index : nA) {
					cvs.set(index.intValue(), missing);
				}
				for (Integer index : A) {
					cvs.set(index.intValue(), setting);
				}
				fnaRec.setSetting(setting);
				fnaRec.setMissing(missing);
				vi.setFillMissing(missing);
				vi.setFillOthers(setting);
				vi.setFillOthers(otherValues);
			}
			fnaRec.setKeepValues(keepValues);
			// 规则修改，填补空值后，为单值的，返回null
			if (keepValues.length() < 2 && Prep.card(cvs) == 1) {
				vi.setStatus(VarInfo.VAR_DEL_SINGLE);
				//return null;
				// 不在这里去直接返回空值了
			}
		}
		return fnaRec;
	}
	
	protected static void impute(Sequence cvs, String cn, FNARec fr) {
		Object missing = fr.getMissing();
		Object setting = fr.getSetting();
		if (setting == null) {
			//计数型，和下面的数值型补缺相对于枚举的几种类型都很简单，只需要补空，不需要考虑合并低频分类的事情
			if (missing == null) {
				//未执行过补缺，直接返回
				return;
			}
			Prep.clnv(cvs, missing);
		}
		else {
			Sequence keepValues = fr.getKeepValues();
			
			if (setting instanceof Sequence) {
				missing = ((Sequence) setting).get(2);
				setting = ((Sequence) setting).get(1);
			}
			
			if (missing == null && setting == null) {
				//这两个都是null，说明并未执行过补缺或合并，直接返回即可
				return;
			}
			// 为了防止填补出空值的情况，把setting和missing都设为非空
			else if (missing == null) {
				missing = setting;
			}
			else if (setting == null) {
				setting = missing;
			}
			
			int iSize = cvs == null ? 0 : cvs.length();
			for ( int i = 1; i <= iSize; i++ ) {
				Object v = cvs.get(i);
				if (missing != null && v == null) {
					cvs.set(i, missing);
				}
				else if (keepValues == null || keepValues.pos(v, null) == null) {
					cvs.set(i, setting);
				}
			}
		}
	}
}
