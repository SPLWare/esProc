package com.scudata.expression.mfn.string;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.StringFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 将字符串以指定分隔符拆成序列，没有指定分隔符则拆成单字符组成的序列
 * s.split(d)
 * @author RunQian
 *
 */
public class Split extends StringFunction {
	public Object calculate(Context ctx) {
		String sep = "";
		if (param != null) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				sep = (String)obj;
			} else if (obj instanceof Sequence) {
				int []lens = ((Sequence)obj).toIntArray();
				return split(srcStr, lens, option);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("split" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		return Sequence.toSequence(srcStr, sep, option);
	}
	
	public static Sequence split(String srcStr, int []lens, String opt) {
		boolean bData = false, bTrim = false;
		if (opt != null) {
			if (opt.indexOf('p') != -1) bData = true; // 自动识别成常数
			if (opt.indexOf('t') != -1) bTrim = true;
		}
		
		int count = lens.length;
		int index = 0;
		Sequence result = new Sequence(count);
		for (int i = 0; i < count; ++i) {
			if (lens[i] > 0) {
				int end = index + lens[i];
				String sub = srcStr.substring(index, end);
				index = end;
				
				if (bTrim) {
					sub = sub.trim();
				}
				
				if (bData) {
					result.add(Variant.parse(sub));
				} else {
					result.add(sub);
				}
			} else {
				index -= lens[i];
			}
		}
		
		return result;
	}
}