package com.raqsoft.expression.fn;

import com.raqsoft.cellset.ICellSet;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.ParamParser;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

public class If extends Function {
	public void setParameter(ICellSet cs, Context ctx, String param) {
		this.param = ParamParser.parse(param, cs, ctx, false, false);
	}

	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_UNKNOWN;
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("if" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			return Boolean.valueOf(Variant.isTrue(obj));
		} else if (param.getType() == IParam.Semicolon) {
			// if(x1:y1,бн,xk:yk;y)
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("if" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("if" + mm.getMessage("function.invalidParam"));
			} else if (sub0.getType() == IParam.Comma) {
				for (int i = 0, size = sub0.getSubSize(); i < size; ++i) {
					IParam sub = sub0.getSub(i);
					if (sub == null || sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("if" + mm.getMessage("function.invalidParam"));
					}
					
					IParam c = sub.getSub(0);
					if (c == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("if" + mm.getMessage("function.invalidParam"));
					}
					
					Object obj = c.getLeafExpression().calculate(ctx);
					if (Variant.isTrue(obj)) {
						c = sub.getSub(1);
						if (c != null) {
							return c.getLeafExpression().calculate(ctx);
						} else {
							return null;
						}
					}
				}
				
				IParam sub1 = param.getSub(1);
				if (sub1 != null) {
					return sub1.getLeafExpression().calculate(ctx);
				} else {
					return null;
				}
			} else {
				// if(x1:y1;y)
				if (sub0.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("if" + mm.getMessage("function.invalidParam"));
				}
				
				IParam sub = sub0.getSub(0);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("if" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (Variant.isTrue(obj)) {
					sub = sub0.getSub(1);
				} else {
					sub = param.getSub(1);
				}
				
				if (sub != null) {
					return sub.getLeafExpression().calculate(ctx);
				} else {
					return null;
				}
			}
		} else if (param.getType() == IParam.Comma) {
			int size = param.getSubSize();
			IParam sub = param.getSub(0);
			
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("if" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				if (size > 3) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("if" + mm.getMessage("function.invalidParam"));
				}
				
				// if(a,b,c)
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (Variant.isTrue(obj)) {
					sub = param.getSub(1);
					if (sub != null) {
						return sub.getLeafExpression().calculate(ctx);
					} else {
						return null;
					}
				} else if (size > 2) {
					sub = param.getSub(2);
					if (sub != null) {
						return sub.getLeafExpression().calculate(ctx);
					} else {
						return null;
					}
				} else {
					return null;
				}
			} else {
				// if(x1:y1,бн,xk:yk)
				for (int i = 0; i < size; ++i) {
					sub = param.getSub(i);
					if (sub == null || sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("if" + mm.getMessage("function.invalidParam"));
					}
					
					IParam c = sub.getSub(0);
					if (c == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("if" + mm.getMessage("function.invalidParam"));
					}
					
					Object obj = c.getLeafExpression().calculate(ctx);
					if (Variant.isTrue(obj)) {
						c = sub.getSub(1);
						if (c != null) {
							return c.getLeafExpression().calculate(ctx);
						} else {
							return null;
						}
					}
				}
				
				return null;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("if" + mm.getMessage("function.invalidParam"));
		}
	}
}
