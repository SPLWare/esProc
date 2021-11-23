package com.scudata.expression.mfn.vdb;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;

/**
 * 根据条件检索数据
 * h.retrieve(F:v,…;F’,…;w’)
 * @author RunQian
 *
 */
public class Retrive extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return vs.retrieve(null, null, null, null, option, ctx);
		}
		
		IParam param = this.param;
		String []dirNames = null;
		Object []dirValues = null;
		String []selFields = null;
		Expression filter = null;
		
		if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("retrieve" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(0);
			if (sub != null) {
				ParamInfo2 pi = ParamInfo2.parse(sub, "retrieve", false, false);
				dirNames = pi.getExpressionStrs1();
				dirValues = pi.getValues2(ctx);
			}
			
			sub = param.getSub(1);
			if (sub == null) {
			} else if (sub.isLeaf()) {
				selFields = new String[]{sub.getLeafExpression().getIdentifierName()};
			} else {
				int fsize = sub.getSubSize();
				selFields = new String[fsize];
				for (int i = 0; i < fsize; ++i) {
					IParam p = sub.getSub(i);
					if (p == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("retrieve" + mm.getMessage("function.invalidParam"));
					}
					
					selFields[i] = p.getLeafExpression().getIdentifierName();
				}
			}
			
			if (size > 2) {
				sub = param.getSub(2);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("retrieve" + mm.getMessage("function.invalidParam"));
				}
				
				filter = sub.getLeafExpression();
			}
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "retrieve", false, false);
			dirNames = pi.getExpressionStrs1();
			dirValues = pi.getValues2(ctx);
		}
		
		return vs.retrieve(dirNames, dirValues, selFields, filter, option, ctx);
	}
}
