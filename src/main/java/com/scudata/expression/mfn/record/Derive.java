package com.scudata.expression.mfn.record;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.RecordFunction;
import com.scudata.resources.EngineMessage;

/**
 * 为排列添加字段生成新序表
 * r.derive(xi:Fi,…) r.derive@x(xi:Fi,…;n)
 * @author RunQian
 *
 */
public class Derive extends RecordFunction {
	public Object calculate(Context ctx) {
		IParam param = this.param;
		int level = 0;
		
		if (param != null && param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
			}
			
			Object val = sub.getLeafExpression().calculate(ctx);
			if (!(val instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.paramTypeError"));
			}
			
			level = ((Number)val).intValue();
			if (level < 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
			}
			
			param = param.getSub(0);
		} else if (option != null && option.indexOf('x') != -1) {
			level = 2;
		}
		
		if (param == null && level < 2) {
			DataStruct ds = srcRecord.dataStruct().dup();
			return new Record(ds, srcRecord.getFieldValues());
		}
		
		Expression []exps = null;
		String []names = null;
		if (param != null) {
			ParamInfo2 pi = ParamInfo2.parse(param, "derive", false, false);
			exps = pi.getExpressions1();
			names = pi.getExpressionStrs2();
		}

		Sequence seq = new Sequence(1);
		seq.add(srcRecord);
		if (level < 2) {
			seq = seq.derive(names, exps, option, ctx);
		} else {
			seq = seq.derive(names, exps, option, ctx, level);
		}
		
		return seq.getMem(1);
	}
}