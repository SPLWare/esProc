package com.scudata.expression.mfn.string;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.ParamInfo3;
import com.scudata.expression.StringFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HTMLUtil;

/**
 * 取出HTML串中指定tag下的指定序号的文本，返回成序列
 * s.htmlparse(tag:i:j,…)
 * @author RunQian
 *
 */
public class HTMLParse extends StringFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return HTMLUtil.htmlparse(srcStr);
		}
		
		ParamInfo3 pi = ParamInfo3.parse(param, "htmlparse", true, true, false);
		Expression []exps1 = pi.getExpressions1();
		Expression []exps2 = pi.getExpressions2();
		Expression []exps3 = pi.getExpressions3();
		int len = exps1.length;
		
		String []tags = new String[len];
		int []seqs = new int[len];
		int []subSeqs = new int[len];
		
		for (int i = 0; i < len; ++i) {
			Object obj = exps1[i].calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("htmlparse" + mm.getMessage("function.paramTypeError"));
			}
			
			tags[i] = (String)obj;
			obj = exps2[i].calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("htmlparse" + mm.getMessage("function.paramTypeError"));
			}
			
			seqs[i] = ((Number)obj).intValue();
			if (exps3[i] != null) {
				obj = exps3[i].calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("htmlparse" + mm.getMessage("function.paramTypeError"));
				}
				
				subSeqs[i] = ((Number)obj).intValue();
			}
		}
		
		return HTMLUtil.htmlparse(srcStr, tags, seqs, subSeqs);
	}
}

