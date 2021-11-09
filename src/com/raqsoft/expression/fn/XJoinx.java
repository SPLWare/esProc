package com.raqsoft.expression.fn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.KeyWord;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.MemoryCursor;
import com.raqsoft.dm.cursor.XJoinxCursor;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/**
 * xjoinx(csi:Fi,xi;…)
 * 将游标csi的结果集无条件叉乘起来，产生以Fi…为字段的游标，每个Fi引用原游标csi的一个成员。叉乘过程中，
 * 过滤出csi中满足条件xi的成员。csi必须为可回转的单路游标，csi也可以是序表。
 * @author runqian
 *
 */
public class XJoinx extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}
	
	public Object calculate(Context ctx) {
		if (param == null || param.getType() != IParam.Semicolon) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xjoinx" + mm.getMessage("function.invalidParam"));
		}

		int count = param.getSubSize();
		ICursor []cursors = new ICursor[count];
		String []names = new String[count];
		Expression []exps = new Expression[count];

		for (int i = 0; i < count; ++i) {
			IParam sub = param.getSub(i);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xjoinx" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) { // Ai
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj instanceof ICursor) {
					cursors[i] = (ICursor)obj;
				} else if (obj instanceof Sequence) {
					cursors[i] = new MemoryCursor((Sequence)obj);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xjoinx" + mm.getMessage("function.paramTypeError"));
				}
			} else if (sub.getType() == IParam.Colon) { // Ai:Fi
				if (sub.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xjoinx" + mm.getMessage("function.invalidParam"));
				}

				IParam sub0 = sub.getSub(0);
				if (sub0 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xjoinx" + mm.getMessage("function.invalidParam"));
				}

				Object obj = sub0.getLeafExpression().calculate(ctx);
				if (obj instanceof ICursor) {
					cursors[i] = (ICursor)obj;
				} else if (obj instanceof Sequence) {
					cursors[i] = new MemoryCursor((Sequence)obj);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xjoinx" + mm.getMessage("function.paramTypeError"));
				}

				IParam sub1 = sub.getSub(1);
				if (sub1 != null) {
					names[i] = sub1.getLeafExpression().getIdentifierName();
				}
			} else { // Ai:Fi,xi
				if (sub.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xjoinx" + mm.getMessage("function.invalidParam"));
				}

				IParam seqParam = sub.getSub(0);
				if (seqParam == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xjoinx" + mm.getMessage("function.invalidParam"));
				} else if (seqParam.isLeaf()) {
					Object obj = seqParam.getLeafExpression().calculate(ctx);
					if (obj instanceof ICursor) {
						cursors[i] = (ICursor)obj;
					} else if (obj instanceof Sequence) {
						cursors[i] = new MemoryCursor((Sequence)obj);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xjoinx" + mm.getMessage("function.paramTypeError"));
					}
				} else {
					if (seqParam.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xjoinx" + mm.getMessage("function.invalidParam"));
					}

					IParam sub0 = seqParam.getSub(0);
					if (sub0 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xjoinx" + mm.getMessage("function.invalidParam"));
					}

					Object obj = sub0.getLeafExpression().calculate(ctx);
					if (obj instanceof ICursor) {
						cursors[i] = (ICursor)obj;
					} else if (obj instanceof Sequence) {
						cursors[i] = new MemoryCursor((Sequence)obj);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xjoinx" + mm.getMessage("function.paramTypeError"));
					}

					IParam sub1 = seqParam.getSub(1);
					if (sub1 != null) {
						names[i] = sub1.getLeafExpression().getIdentifierName();
					}
				}

				IParam expParam = sub.getSub(1);
				if (expParam == null || !expParam.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xjoinx" + mm.getMessage("function.invalidParam"));
				}

				exps[i] = expParam.getLeafExpression();
			}
		}

		String []opts = KeyWord.parseStringOptions(names);
		return new XJoinxCursor(cursors, exps, names, opts, option, ctx);
	}
}
