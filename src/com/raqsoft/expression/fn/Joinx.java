package com.raqsoft.expression.fn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.JoinxCursor_u;
import com.raqsoft.dm.cursor.MemoryCursor;
import com.raqsoft.dw.pseudo.IPseudo;
import com.raqsoft.dw.pseudo.PseudoJoinx;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.CursorUtil;

/**
 * 连接游标对应的序表
 * joinx(csi:Fi,xj,..;…)
 * 针对有序游标csi的结果集使用归并法计算，返回新游标，xj参数全省略则使用主键连接. 有xj参数没主键则使用xj的值连接。
 * 支持多路游标，此时必须路数相同。csi也可以是序表。
 * @author runqian
 *
 */
public class Joinx extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	private Object joinx_u(Context ctx) {
		int count = param.getSubSize();
		Object []objs = new Object[count];
		String []names = new String[count];
		Expression [][]exps = new Expression[count][];
		
		for (int i = 0; i < count; ++i) {
			// csi:Fi,xj,..
			IParam sub = param.getSub(i);
			IParam cursorParam;
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
			} else if (sub.getType() == IParam.Comma) {
				cursorParam = sub.getSub(0);
				if (cursorParam == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}
				
				int expCount = sub.getSubSize() - 1;
				if (i != 0 && expCount != exps[0].length) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.paramCountNotMatch"));
				}

				Expression []curExps = new Expression[expCount];
				exps[i] = curExps;
				for (int p = 0; p < expCount; ++p) {
					IParam expParam = sub.getSub(p + 1);
					if (expParam == null || !expParam.isLeaf()) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
					}

					curExps[p] = expParam.getLeafExpression();
				}
			} else {
				cursorParam = sub;
			}
			
			if (cursorParam.isLeaf()) {
				Object obj = cursorParam.getLeafExpression().calculate(ctx);
				if (i == 0) {
					if (!(obj instanceof ICursor)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.paramTypeError"));
					}
				} else {
					if (!(obj instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.paramTypeError"));
					}
				}

				objs[i] = obj;
			} else {
				if (cursorParam.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}

				IParam sub0 = cursorParam.getSub(0);
				if (sub0 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}

				Object obj = sub0.getLeafExpression().calculate(ctx);
				if (i == 0) {
					if (!(obj instanceof ICursor)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.paramTypeError"));
					}
				} else {
					if (!(obj instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.paramTypeError"));
					}
				}

				objs[i] = obj;
				IParam sub1 = cursorParam.getSub(1);
				if (sub1 != null) {
					names[i] = sub1.getLeafExpression().getIdentifierName();
				}
			}
		}
		
		return new JoinxCursor_u(objs, exps, names, option, ctx);
	}
	
	public Object calculate(Context ctx) {
		if (param == null || param.getType() != IParam.Semicolon) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
		}
		
		if (option != null && option.indexOf('u') != -1) {
			return joinx_u(ctx);
		}
		
		boolean isJoin = option == null || option.indexOf('p') == -1;

		int count = param.getSubSize();
		ICursor []cursors = new ICursor[count];
		IPseudo []pseudos = new IPseudo[count];
		String []names = new String[count];
		Expression [][]exps = new Expression[count][];
				
		for (int i = 0; i < count; ++i) {
			// csi:Fi,xj,..
			IParam sub = param.getSub(i);
			IParam cursorParam;
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
			} else if (sub.getType() == IParam.Comma) {
				cursorParam = sub.getSub(0);
				if (cursorParam == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}
				
				int expCount = sub.getSubSize() - 1;
				if (isJoin && i != 0 && expCount != exps[0].length) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.paramCountNotMatch"));
				}

				Expression []curExps = new Expression[expCount];
				exps[i] = curExps;
				for (int p = 0; p < expCount; ++p) {
					IParam expParam = sub.getSub(p + 1);
					if (expParam == null || !expParam.isLeaf()) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
					}

					curExps[p] = expParam.getLeafExpression();
				}
			} else {
				cursorParam = sub;
				if (isJoin) {
					Expression exp = new Expression("~.v()");
					Expression []curExps = new Expression[]{exp};
					exps[i] = curExps;
				}
			}
			
			if (cursorParam.isLeaf()) {
				Object obj = cursorParam.getLeafExpression().calculate(ctx);
				if (obj instanceof ICursor) {
					cursors[i] = (ICursor)obj;
				} else if (obj instanceof Sequence) {
					cursors[i] = new MemoryCursor((Sequence)obj);
				} else if (obj instanceof IPseudo) {
					pseudos[i] = (IPseudo) obj;
				} else if (obj == null) {
					cursors[i] = new MemoryCursor(null);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.paramTypeError"));
				}
			} else {
				if (cursorParam.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}

				IParam sub0 = cursorParam.getSub(0);
				if (sub0 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}

				Object obj = sub0.getLeafExpression().calculate(ctx);
				if (obj instanceof ICursor) {
					cursors[i] = (ICursor)obj;
				} else if (obj instanceof Sequence) {
					cursors[i] = new MemoryCursor((Sequence)obj);
				} else if (obj instanceof IPseudo) {
					pseudos[i] = (IPseudo) obj;
				} else if (obj == null) {
					cursors[i] = new MemoryCursor(null);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.paramTypeError"));
				}

				IParam sub1 = cursorParam.getSub(1);
				if (sub1 != null) {
					names[i] = sub1.getLeafExpression().getIdentifierName();
				}
			}
		}

		if (pseudos[0] != null) {
//			if (pseudos[0] instanceof ClusterPseudo) {
//				ClusterPseudo cps [] = new ClusterPseudo[count];
//				for (int i = 0; i < count; i++) {
//					cps[i] = (ClusterPseudo) pseudos[i];
//				}
//				return ClusterPseudo.joinx(cps, exps, names, option, ctx);
//			} else {
				return new PseudoJoinx(pseudos, exps, names, option);
//			}
		}
		return CursorUtil.joinx(cursors, names, exps, option, ctx);
	}
}
