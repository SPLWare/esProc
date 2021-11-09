package com.raqsoft.expression.fn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.Machines;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dw.pseudo.PseudoTable;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

public class CreatePseudo extends Function {

	public Object calculate(Context ctx) {
		Sequence pd = null;
		Machines hs = null;
		Integer n = null;
		
		IParam param = this.param;
		IParam param0 = null;
		IParam param1 = null;
		IParam param2 = null;
		
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pseudo" + mm.getMessage("function.missingParam"));
		}
		
		if (param.isLeaf()) {
			param0 = param;
		} else {
			if (param.getType() != IParam.Comma) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pseudo" + mm.getMessage("function.invalidParam"));
			}
			param0 = param.getSub(0);
			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pseudo" + mm.getMessage("function.invalidParam"));
			}
			if (sub.isLeaf()) {
				param1 = sub;
			} else {
				param1 = sub.getSub(0);
				param2 = sub.getSub(1);
			}
		}
		
		if (param0 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pseudo" + mm.getMessage("function.invalidParam"));
		} else {
			//µÃµ½pd
			Object obj = param0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pseudo" + mm.getMessage("function.invalidParam"));
			}
			pd = (Sequence) obj;
		}
		
		if (param1 != null) {
			Object obj = param1.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				n = ((Number)obj).intValue();
			} else {
				hs = new Machines();
				if (!hs.set(obj)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pseudo" + mm.getMessage("function.invalidParam"));
				}
			}
		}
		
		if (param2 != null) {
			Object obj = param2.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pseudo" + mm.getMessage("function.invalidParam"));
			}
			n = ((Number)obj).intValue();
		}
		
		if (pd.length() != 1 || (!(pd.get(1) instanceof Record))) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pseudo" + mm.getMessage("function.invalidParam"));
		}
		if (n == null) {
			n = 1;
		} else if (n == 0) {
			n = Env.getCursorParallelNum();
		}
		return new PseudoTable((Record) pd.get(1), hs, n, ctx);
	}

}
