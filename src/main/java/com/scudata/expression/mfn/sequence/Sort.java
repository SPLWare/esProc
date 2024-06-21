package com.scudata.expression.mfn.sequence;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 对序列做排序，返回排序后的序列
 * A.sort(x) A.sort(xi:di,..) A.sort(…;loc)
 * @author RunQian
 *
 */
public class Sort extends SequenceFunction {
	public Object calculate(Context ctx) {
		String loc = null;
		IParam param = this.param;
		if (param != null && param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sort" + mm.getMessage("function.invalidParam"));
			}

			IParam locParam = param.getSub(1);
			if (locParam != null) {
				Object obj = locParam.getLeafExpression().calculate(ctx);
				if (obj != null && !(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sort" + mm.getMessage("function.paramTypeError"));
				}

				loc = (String)obj;
			}

			param = param.getSub(0);
		}

		if (param == null) {
			return srcSequence.sort(loc, option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.sort(exp, loc, option, ctx);
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "sort", true, false);
			Expression []sortExps = pi.getExpressions1();
			Expression []orderExps = pi.getExpressions2();

			int count = sortExps.length;
			int[] orders = new int[count];
			boolean sign = false;
			for (int i = 0; i < count; ++i) {
				if (orderExps[i] != null) {
					Object obj = orderExps[i].calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sort" + mm.getMessage("function.paramTypeError"));
					}
					orders[i] = ((Number)obj).intValue();
					sign = true;
				} else {
					orders[i] = 1;
				}
			}

			if (sign) {
				return srcSequence.sort(sortExps, orders, loc, option, ctx);
			} else {
				int[] findex = isAllRecordFields(sortExps);
				if (findex != null) {
					return srcSequence.sort(sortExps, loc, option, findex, ctx);
				}
				return srcSequence.sort(sortExps, loc, option, ctx);
			}
		}
	}
	
	/**
	 * 检查比较是否都是记录的字段
	 * @return
	 */
	private int[] isAllRecordFields(Expression[] exps) {
		Sequence seq = srcSequence;
		if (seq.length() == 0) {
			return null;
		}
		
		IArray mems = seq.getMems();
		int fcount = exps.length;
		
		Object obj = mems.get(1);
		if (!(obj instanceof BaseRecord)) {
			return null;
		}
		BaseRecord record = (BaseRecord) obj;
		int[] findex = new int[fcount];
		for (int i = 0; i < fcount; i++) {
			findex[i] = record.getFieldIndex(exps[i].getIdentifierName());
			if (findex[i] == -1) {
				return null;
			}
		}
		return findex;
	}
}
