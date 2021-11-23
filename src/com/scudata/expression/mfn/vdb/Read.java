package com.scudata.expression.mfn.vdb;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;

/**
 * 从数据库中读取其它字段追加到指定排列上
 * h.read(A:xp,Fi,…;w)
 * @author RunQian
 *
 */
public class Read extends VSFunction {
	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null || param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("read" + mm.getMessage("function.missingParam"));
		}
		
		Expression filter = null;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("read" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(1);
			if (sub == null || !sub.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("read" + mm.getMessage("function.invalidParam"));
			}
			
			filter = sub.getLeafExpression();
			param = param.getSub(0);
			if (param == null || param.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("read" + mm.getMessage("function.missingParam"));
			}
		}
		
		String []fields = null;
		if (param.getType() == IParam.Comma) {
			int size = param.getSubSize();
			fields = new String[size - 1];
			for (int i = 1; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("read" + mm.getMessage("function.invalidParam"));
				}
				
				fields[i - 1] = sub.getLeafExpression().getIdentifierName();
			}
			
			param = param.getSub(0);
			if (param == null || param.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("read" + mm.getMessage("function.missingParam"));
			}
		}
		
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("read" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 ==  null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("read" + mm.getMessage("function.invalidParam"));
		}
		
		Expression pathExp = sub1.getLeafExpression();
		Object obj = sub0.getLeafExpression().calculate(ctx);
		Sequence seq;
		if (obj instanceof Sequence) {
			seq = (Sequence)obj;
		} else if (obj instanceof Record) {
			seq = new Sequence(1);
			seq.add(obj);
		} else if (obj instanceof ICursor) {
			throw new RuntimeException("unimplemented");
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("read" + mm.getMessage("function.paramTypeError"));
		}
		
		return vs.read(seq, pathExp, fields, filter, ctx);
	}
}
