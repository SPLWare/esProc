package com.scudata.expression.mfn.vdb;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;

/**
 * 把排列的指定字段写入到表单或删除满足条件的表单
 * h.write(A:xp,x:F,…;w)
 * @author RunQian
 *
 */
public class Write extends VSFunction {
	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null || param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("write" + mm.getMessage("function.missingParam"));
		}
		
		Expression filter = null;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("write" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(1);
			if (sub == null || !sub.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("write" + mm.getMessage("function.invalidParam"));
			}
			
			filter = sub.getLeafExpression();
			param = param.getSub(0);
			if (param == null || param.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("write" + mm.getMessage("function.missingParam"));
			}
		}
		
		Expression []fieldExps = null;
		String []fields = null;
		if (param.getType() == IParam.Comma) {
			int size = param.getSubSize();
			fields = new String[size - 1];
			fieldExps = new Expression[size - 1];
			for (int i = 1; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("write" + mm.getMessage("function.invalidParam"));
				}
				
				IParam sub0 = sub.getSub(0);
				IParam sub1 = sub.getSub(1);
				if (sub0 == null || sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("write" + mm.getMessage("function.invalidParam"));
				}
				
				fieldExps[i - 1] = sub0.getLeafExpression();
				fields[i - 1] = sub1.getLeafExpression().getIdentifierName();
			}
			
			param = param.getSub(0);
			if (param == null || param.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("write" + mm.getMessage("function.missingParam"));
			}
		}
		
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("write" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 ==  null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("write" + mm.getMessage("function.invalidParam"));
		}
		
		Expression pathExp = sub1.getLeafExpression();
		Object obj = sub0.getLeafExpression().calculate(ctx);
		Sequence seq;
		if (obj instanceof Sequence) {
			seq = (Sequence)obj;
		} else if (obj instanceof BaseRecord) {
			seq = new Sequence(1);
			seq.add(obj);
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("write" + mm.getMessage("function.paramTypeError"));
		}
		
		if (filter == null && fieldExps == null) {
			return vs.deleteAll(seq.calc(pathExp, ctx));
		} else {
			return vs.write(seq, pathExp, fieldExps, fields, filter, ctx);
		}
	}
}
