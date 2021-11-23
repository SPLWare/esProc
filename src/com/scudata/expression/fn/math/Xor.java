package com.scudata.expression.fn.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * 求序列成员或多个参数的按位异或,非数值成员被忽略
 * @author yanjing
 *
 */
public class Xor extends Function {
	// 计算序列成员按位异或
	private static Object xor(Sequence seq) {
		int size = seq.length();
		if (size == 0) {
			return null;
		} else if (size == 1) {
			return seq.getMem(1);
		}
		
		Object obj = seq.getMem(1);
		long longValue = 0;
		BigInteger bi = null;
		
		// 如果有成员类型是BigDecimal或BigInteger则使用BigInteger运算
		if (obj instanceof BigDecimal) {
			bi = ((BigDecimal)obj).toBigInteger();
		} else if (obj instanceof BigInteger) {
			bi = (BigInteger)obj;
		} else if (obj instanceof Number) {
			longValue = ((Number)obj).longValue();
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xor" + mm.getMessage("function.paramTypeError"));
		}
		
		for (int i = 2; i <= size; ++i) {
			obj = seq.getMem(i);
			if (bi != null) {
				if (obj instanceof Number) {
					BigInteger tmp = Variant.toBigInteger((Number)obj);
					bi = bi.xor(tmp);
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xor" + mm.getMessage("function.paramTypeError"));
				}
			} else if (obj instanceof BigDecimal) {
				bi = ((BigDecimal)obj).toBigInteger();
				bi = bi.xor(BigInteger.valueOf(longValue));
			} else if (obj instanceof BigInteger) {
				bi = (BigInteger)obj;
				bi = bi.xor(BigInteger.valueOf(longValue));
			} else if (obj instanceof Number) {
				longValue ^= ((Number)obj).longValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xor" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		if (bi == null) {
			return longValue;
		} else {
			return new BigDecimal(bi);
		}
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xor" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return xor((Sequence)obj);
			} else {
				return obj;
			}
		} else {
			int size = param.getSubSize();
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xor" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub.getLeafExpression().calculate(ctx);
			long longValue = 0;
			BigInteger bi = null;
			
			// 如果有成员类型是BigDecimal或BigInteger则使用BigInteger运算
			if (obj instanceof BigDecimal) {
				bi = ((BigDecimal)obj).toBigInteger();
			} else if (obj instanceof BigInteger) {
				bi = (BigInteger)obj;
			} else if (obj instanceof Number) {
				longValue = ((Number)obj).longValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xor" + mm.getMessage("function.paramTypeError"));
			}
			
			for (int i = 1; i < size; ++i) {
				sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xor" + mm.getMessage("function.invalidParam"));
				}
				
				obj = sub.getLeafExpression().calculate(ctx);
				if (bi != null) {
					if (obj instanceof Number) {
						BigInteger tmp = Variant.toBigInteger((Number)obj);
						bi = bi.xor(tmp);
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xor" + mm.getMessage("function.paramTypeError"));
					}
				} else if (obj instanceof BigDecimal) {
					bi = ((BigDecimal)obj).toBigInteger();
					bi = bi.xor(BigInteger.valueOf(longValue));
				} else if (obj instanceof BigInteger) {
					bi = (BigInteger)obj;
					bi = bi.xor(BigInteger.valueOf(longValue));
				} else if (obj instanceof Number) {
					longValue ^= ((Number)obj).longValue();
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xor" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			if (bi == null) {
				return longValue;
			} else {
				return new BigDecimal(bi);
			}
		}
	}
}
