package com.scudata.expression.fn;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamParser;
import com.scudata.expression.Relation;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

public class If extends Function {
	public void setParameter(ICellSet cs, Context ctx, String param) {
		this.strParam = param;
		this.cs = cs;
		this.param = ParamParser.parse(param, cs, ctx, false, false);
	}

	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_UNKNOWN;
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("if" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			return Boolean.valueOf(Variant.isTrue(obj));
		} else if (param.getType() == IParam.Semicolon) {
			// if(x1:y1,…,xk:yk;y)
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
				// if(x1:y1,…,xk:yk)
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
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IParam param = this.param;
		if (param.isLeaf()) {
			IArray array = param.getLeafExpression().calculateAll(ctx);
			if (array instanceof BoolArray) {
				return array;
			} else {
				return array.isTrue();
			}
		} else if (param.getType() == IParam.Semicolon) {
			// if(x1:y1,…,xk:yk;y)
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("if" + mm.getMessage("function.invalidParam"));
			}
			
			Expression []exps;
			Expression []valExps;
			Expression defaultValExp = null;
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub1 != null) {
				defaultValExp = sub1.getLeafExpression();
			}
			
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("if" + mm.getMessage("function.invalidParam"));
			} else if (sub0.getType() == IParam.Comma) {
				int size = sub0.getSubSize();
				exps = new Expression[size];
				valExps = new Expression[size];
				
				for (int i = 0; i < size; ++i) {
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
					
					exps[i] = c.getLeafExpression();
					c = sub.getSub(1);
					if (c == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("if" + mm.getMessage("function.invalidParam"));
					}
					
					valExps[i] = c.getLeafExpression();
				}
			} else {
				// if(x1:y1;y)
				if (sub0.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("if" + mm.getMessage("function.invalidParam"));
				}
				
				IParam c = sub0.getSub(0);
				if (c == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("if" + mm.getMessage("function.invalidParam"));
				}
				
				exps = new Expression[] {c.getLeafExpression()};
				c = sub0.getSub(1);
				if (c == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("if" + mm.getMessage("function.invalidParam"));
				}
				
				valExps = new Expression[] {c.getLeafExpression()};
			}
			
			return calcIf(exps, valExps, defaultValExp, ctx);
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
				
				// if(x,a,b)
				Expression x = sub.getLeafExpression();
				Expression a = null;
				Expression b = null;
				
				sub = param.getSub(1);
				if (sub != null) {
					a = sub.getLeafExpression();
				}
				
				if (size > 2) {
					sub = param.getSub(2);
					if (sub != null) {
						b = sub.getLeafExpression();
					}
				}
				
				return calcIf(x, a, b, ctx);
			} else {
				// if(x1:y1,…,xk:yk)
				Expression []exps = new Expression[size];
				Expression []valExps = new Expression[size];
				
				for (int i = 0; i < size; ++i) {
					sub = param.getSub(i);
					if (sub == null || sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("if" + mm.getMessage("function.invalidParam"));
					}
					
					IParam sub0 = sub.getSub(0);
					IParam sub1 = sub.getSub(10);
					if (sub0 == null || sub1 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("if" + mm.getMessage("function.invalidParam"));
					}
					
					exps[i] = sub0.getLeafExpression();
					valExps[i] = sub1.getLeafExpression();
				}
				
				return calcIf(exps, valExps, null, ctx);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("if" + mm.getMessage("function.invalidParam"));
		}
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		return calculateAll(ctx);
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		BoolArray result = leftResult.isTrue();
		IArray array = calculateAll(ctx);
		
		for (int i = 1, size = result.size(); i <= size; ++i) {
			if (result.isTrue(i) && array.isFalse(i)) {
				result.set(i, false);
			}
		}
		
		return result;
	}
	
	private static IArray calcIf(Expression x, Expression a, Expression b, Context ctx) {
		IArray signArray = x.calculateAll(ctx);
		IArray array1;
		IArray array2;
		
		if (a != null) {
			array1 = a.calculateAll(ctx, signArray, true);
		} else {
			array1 = new ConstArray(null, signArray.size());
		}
		
		if (b != null) {
			array2 = b.calculateAll(ctx, signArray, false);
		} else {
			array2 = new ConstArray(null, signArray.size());
		}
		
		return array1.combine(signArray, array2);
	}
	
	private static IArray calcIf(Expression []exps, Expression []valExps, Expression defaultValExp, Context ctx) {
		IArray signArray = exps[0].calculateAll(ctx);
		IArray resultArray = valExps[0].calculateAll(ctx, signArray, true);
		
		for (int i = 1; i < exps.length; ++i) {
			IArray curSignArray = exps[i].calculateAll(ctx, signArray, false);
			IArray curValArray = valExps[i].calculateAll(ctx, curSignArray, true);
			resultArray = resultArray.combine(signArray, curValArray);
			signArray = signArray.calcRelation(curSignArray, Relation.OR);
		}
		
		IArray defaultValArray;
		if (defaultValExp != null) {
			defaultValArray = defaultValExp.calculateAll(ctx, signArray, false);
		} else {
			defaultValArray = new ConstArray(null, signArray.size());
		}
		
		return resultArray.combine(signArray, defaultValArray);
	}
}
