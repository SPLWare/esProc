package com.raqsoft.expression.fn.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 求序列成员或多个参数的按位与,非数值成员被忽略
 * @author yanjing
 *
 */
public class And extends Function {
	// 计算序列成员按位与
	private static Object and(Sequence seq) {
		int size = seq.length();
		if (size == 0) {
			return null;
		} else if (size == 1) {
			return seq.getMem(1);
		}
		
		Object obj = seq.getMem(1);
		boolean returnInt = true; // 是否全部都是Integer，如果是则返回Integer，否则返回Long
		long longValue = 0;
		BigInteger bi = null; // 如果含有BigInteger则返回BigInteger
		
		// 如果有成员类型是BigDecimal或BigInteger则使用BigInteger运算
		if (obj instanceof BigDecimal) {
			bi = ((BigDecimal)obj).toBigInteger();
		} else if (obj instanceof BigInteger) {
			bi = (BigInteger)obj;
		} else if (obj instanceof Number) {
			if (!(obj instanceof Integer)) {
				returnInt = false;
			}
			
			longValue = ((Number)obj).longValue();
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("and" + mm.getMessage("function.paramTypeError"));
		}
		
		for (int i = 2; i <= size; ++i) {
			obj = seq.getMem(i);
			if (bi != null) {
				if (obj instanceof Number) {
					BigInteger tmp = Variant.toBigInteger((Number)obj);
					bi = bi.and(tmp);
				} else if (obj == null) {
					return null;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("and" + mm.getMessage("function.paramTypeError"));
				}
			} else if (obj instanceof BigDecimal) {
				bi = ((BigDecimal)obj).toBigInteger();
				bi = bi.and(BigInteger.valueOf(longValue));
			} else if (obj instanceof BigInteger) {
				bi = (BigInteger)obj;
				bi = bi.and(BigInteger.valueOf(longValue));
			} else if (obj instanceof Number) {
				if (!(obj instanceof Integer)) {
					returnInt = false;
				}
				
				longValue &= ((Number)obj).longValue();
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("and" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		if (bi == null) {
			if (returnInt) {
				return (int)longValue;
			} else {
				return longValue;
			}
		} else {
			return new BigDecimal(bi);
		}
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("and" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return and((Sequence)obj);
			} else {
				return obj;
			}
		} else {
			int size = param.getSubSize();
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("and" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub.getLeafExpression().calculate(ctx);
			boolean returnInt = true; // 是否全部都是Integer，如果是则返回Integer，否则返回Long
			long longValue = 0;
			BigInteger bi = null; // 如果含有BigInteger则返回BigInteger
			
			// 如果有成员类型是BigDecimal或BigInteger则使用BigInteger运算
			if (obj instanceof BigDecimal) {
				bi = ((BigDecimal)obj).toBigInteger();
			} else if (obj instanceof BigInteger) {
				bi = (BigInteger)obj;
			} else if (obj instanceof Number) {
				if (!(obj instanceof Integer)) {
					returnInt = false;
				}
				
				longValue = ((Number)obj).longValue();
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("and" + mm.getMessage("function.paramTypeError"));
			}
			
			for (int i = 1; i < size; ++i) {
				sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("and" + mm.getMessage("function.invalidParam"));
				}
				
				obj = sub.getLeafExpression().calculate(ctx);
				if (bi != null) {
					if (obj instanceof Number) {
						BigInteger tmp = Variant.toBigInteger((Number)obj);
						bi = bi.and(tmp);
					} else if (obj == null) {
						return null;
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("and" + mm.getMessage("function.paramTypeError"));
					}
				} else if (obj instanceof BigDecimal) {
					bi = ((BigDecimal)obj).toBigInteger();
					bi = bi.and(BigInteger.valueOf(longValue));
				} else if (obj instanceof BigInteger) {
					bi = (BigInteger)obj;
					bi = bi.and(BigInteger.valueOf(longValue));
				} else if (obj instanceof Number) {
					if (!(obj instanceof Integer)) {
						returnInt = false;
					}
					
					longValue &= ((Number)obj).longValue();
				} else if (obj == null) {
					return null;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("and" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			if (bi == null) {
				if (returnInt) {
					return (int)longValue;
				} else {
					return longValue;
				}
			} else {
				return new BigDecimal(bi);
			}
		}
	}
}
