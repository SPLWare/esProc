package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 计算序列中满足给定条件的元素的和
 * A.sumif(Ai:xi,…)
 * @author RunQian
 *
 */
public class Sumif extends SequenceFunction {
	public Object calculate(Context ctx) {
		return posSelect("sumif", srcSequence, param, option, ctx).sum();
	}

	// Ai:xi,…       A(Ai.pos@a(xi)^…)
	static Sequence posSelect(String funcName, Sequence srcSeries, IParam param, String option, Context ctx) {
		if (option == null || option.indexOf('b') == -1) {
			option = "a";
		} else {
			option = "ab";
		}

		ParamInfo2 pi = ParamInfo2.parse(param, funcName, true, true);
		Expression []exps = pi.getExpressions1();
		int count = exps.length;
		Sequence []src = new Sequence[count];
		for (int i = 0; i < count; ++i) {
			Object obj = exps[i].calculate(ctx);
			if (!(obj instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(funcName + mm.getMessage("function.paramTypeError"));
			}

			src[i] = (Sequence)obj;
		}

		Object []vals = pi.getValues2(ctx);
		Sequence posSeries = (Sequence)src[0].pos(vals[0], option);
		if (posSeries.length() == 0) return posSeries;

		for (int i = 1; i < count; ++i) {
			posSeries = pos(src[i], vals[i], posSeries);
			if (posSeries.length() == 0) return posSeries;
		}

		return srcSeries.get(posSeries);
	}

	private static Sequence pos(Sequence src, Object obj, Sequence posSeries) {
		int posLen = posSeries.length();
		int srcLen = src.length();
		Sequence retSeries = new Sequence(posLen);

		if (obj instanceof Sequence) {
			Sequence sub = (Sequence)obj;
			int subLen = sub.length();
			int lastPos = srcLen - subLen + 1;

			Next:
			for (int i = 1; i <= posLen; ++i) {
				Number posObj = (Number)posSeries.get(i);
				int pos = posObj.intValue();
				if (pos > lastPos) break;

				for (int j = 1, p = pos; j <= subLen; ++j, ++p) {
					if (!Variant.isEquals(src.get(p), sub.get(j))) {
						continue Next;
					}
				}

				retSeries.add(posObj);
			}
		} else {
			for (int i = 1; i <= posLen; ++i) {
				Number posObj = (Number)posSeries.get(i);
				int pos = posObj.intValue();
				if (pos > srcLen) break;

				if (Variant.isEquals(src.get(pos), obj)) retSeries.add(posObj);
			}
		}

		return retSeries;
	}
}
