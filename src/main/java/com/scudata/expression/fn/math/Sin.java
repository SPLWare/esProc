package com.scudata.expression.fn.math;

import com.scudata.array.ConstArray;
import com.scudata.array.DoubleArray;
import com.scudata.array.IArray;
import com.scudata.array.NumberArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

public class Sin extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sin" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sin" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Number) {
			return new Double(Math.sin(((Number)obj).doubleValue()));
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sin" + mm.getMessage("function.paramTypeError"));
		}
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray array = param.getLeafExpression().calculateAll(ctx);
		int size = array.size();
		
		if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof Number) {
				double v = Math.sin(((Number)obj).doubleValue());
				return new ConstArray(new Double(v), size);
			} else if (obj == null) {
				return new ConstArray(null, size);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sin" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		DoubleArray result = new DoubleArray(size);
		result.setTemporary(true);
		
		if (array instanceof NumberArray) {
			NumberArray numberArray = (NumberArray)array;
			for (int i = 1; i <= size; ++i) {
				if (numberArray.isNull(i)) {
					result.pushNull();
				} else {
					double v = Math.sin(numberArray.getDouble(i));
					result.push(v);
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				Object obj = array.get(i);
				if (obj instanceof Number) {
					double v = Math.sin(((Number)obj).doubleValue());
					result.push(v);
				} else if (obj == null) {
					result.pushNull();
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sin" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		IArray array = param.getLeafExpression().calculateAll(ctx, signArray, sign);
		int size = array.size();
		
		if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof Number) {
				double v = Math.sin(((Number)obj).doubleValue());
				return new ConstArray(new Double(v), size);
			} else if (obj == null) {
				return new ConstArray(null, size);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sin" + mm.getMessage("function.paramTypeError"));
			}
		}

		boolean[] signDatas;
		if (sign) {
			signDatas = signArray.isTrue().getDatas();
		} else {
			signDatas = signArray.isFalse().getDatas();
		}
		
		DoubleArray result = new DoubleArray(size);
		result.setTemporary(true);
		
		if (array instanceof NumberArray) {
			NumberArray numberArray = (NumberArray)array;
			for (int i = 1; i <= size; ++i) {
				if (signDatas[i]) {
					if (numberArray.isNull(i)) {
						result.pushNull();
					} else {
						double v = Math.sin(numberArray.getDouble(i));
						result.push(v);
					}
				} else {
					result.pushNull();
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (signDatas[i]) {
					Object obj = array.get(i);
					if (obj instanceof Number) {
						double v = Math.sin(((Number)obj).doubleValue());
						result.push(v);
					} else if (obj == null) {
						result.pushNull();
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sin" + mm.getMessage("function.paramTypeError"));
					}
				} else {
					result.pushNull();
				}
			}
		}
		
		return result;
	}
}
