package com.scudata.expression.mfn.vdb;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;

/**
 * 保存附件，通常是图片
 * h.saveblob(o,n;p:F)
 * @author RunQian
 *
 */
public class SaveBlob extends VSFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("saveblob" + mm.getMessage("function.missingParam"));
		}

		Sequence oldValues = null;
		Sequence newValues = null;
		Object path = null;
		String name = null;
		
		IParam param = this.param;
		if (param.getType() == IParam.Semicolon) {
			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("saveblob" + mm.getMessage("function.invalidParam"));
			}
			
			if (sub.isLeaf()) {
				path = sub.getLeafExpression().calculate(ctx);
			} else {
				if (sub.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("saveblob" + mm.getMessage("function.invalidParam"));
				}
				
				IParam sub0 = sub.getSub(0);
				if (sub0 != null) {
					path = sub0.getLeafExpression().calculate(ctx);
				}
				
				IParam sub1 = sub.getSub(1);
				if (sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("saveblob" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub1.getLeafExpression().calculate(ctx);
				if (obj instanceof String) {
					name = (String)obj;
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("saveblob" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			param = param.getSub(0);
		}
		
		if (param == null) {
			return null;
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				oldValues = (Sequence)obj;
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("saveblob" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("saveblob" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			if (sub0 != null) {
				Object obj = sub0.getLeafExpression().calculate(ctx);
				if (obj instanceof Sequence) {
					oldValues = (Sequence)obj;
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("saveblob" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			IParam sub1 = param.getSub(1);
			if (sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("saveblob" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub1.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				newValues = (Sequence)obj;
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("saveblob" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		return vs.saveBlob(oldValues, newValues, path, name);
	}
}
