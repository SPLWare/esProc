package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 对序列或排列进行对齐
 * P.align(A:x,y) P.align(n,y)
 * @author RunQian
 *
 */
public class Align extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("align" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam tgtParam;
		Expression fltExp = null;
		if (param.getType() == IParam.Comma) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("align" + mm.getMessage("function.invalidParam"));
			}
			
			tgtParam = param.getSub(0);
			if (tgtParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("align" + mm.getMessage("function.invalidParam"));
			}

			IParam fltParam = param.getSub(1);
			if (fltParam != null) {
				if (!fltParam.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("align" + mm.getMessage("function.invalidParam"));
				}
				fltExp = fltParam.getLeafExpression();
			}
		} else if (param.getType() == IParam.Semicolon) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("align" + mm.getMessage("function.paramTypeError"));
		} else {
			tgtParam = param;
		}

		Sequence tgtSeries;
		if (tgtParam.isLeaf()) {
			Object p0 = tgtParam.getLeafExpression().calculate(ctx);
			if (p0 instanceof Number) {
				return srcSequence.align(fltExp, ((Number)p0).intValue(), option, ctx);
			} else if (p0 instanceof Sequence) {
				tgtSeries = (Sequence)p0;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("align" + mm.getMessage("function.invalidParam"));
			}
		} else {
			if (tgtParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("align" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = tgtParam.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("align" + mm.getMessage("function.invalidParam"));
			}

			Object p0 = sub0.getLeafExpression().calculate(ctx);
			if (p0 instanceof Sequence) {
				tgtSeries = (Sequence)p0;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("align" + mm.getMessage("function.paramTypeError"));
			}

			IParam sub1 = tgtParam.getSub(1);
			if (sub1 != null) {
				tgtSeries = tgtSeries.calc(sub1.getLeafExpression(), "o", ctx);
			}
		}

		return srcSequence.align(fltExp, tgtSeries, option, ctx);
	}
}
