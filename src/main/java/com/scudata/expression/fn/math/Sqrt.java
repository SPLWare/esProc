package com.scudata.expression.fn.math;

import com.scudata.array.ConstArray;
import com.scudata.array.DoubleArray;
import com.scudata.array.IArray;
import com.scudata.array.NumberArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

public class Sqrt extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sqrt" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Expression param1 = param.getLeafExpression();
			Object result = param1.calculate(ctx);
			if (result instanceof Number) {
				return new Double(Math.sqrt(((Number)result).doubleValue()));
			} else if (result == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqrt" + mm.getMessage("function.invalidParam"));
			}
			
			Object a = sub0.getLeafExpression().calculate(ctx);
			if (a == null) {
				return null;
			} else if (!(a instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
			}
			
			Object b = sub1.getLeafExpression().calculate(ctx);
			if (!(b instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
			}
			
			double p = 1 / ((Number)b).doubleValue();
			return new Double(Math.pow(((Number)a).doubleValue(), p));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sqrt" + mm.getMessage("function.invalidParam"));
		}
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (param.isLeaf()) {
			IArray array = param.getLeafExpression().calculateAll(ctx);
			int size = array.size();
			
			if (array instanceof ConstArray) {
				Object obj = array.get(1);
				if (obj instanceof Number) {
					obj = new Double(Math.sqrt(((Number)obj).doubleValue()));
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(obj, size);
			} else if (array instanceof NumberArray) {
				NumberArray numberArray = (NumberArray)array;
				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (numberArray.isNull(i)) {
						result.pushNull();
					} else {
						double v = numberArray.getDouble(i);
						result.push(Math.sqrt(v));
					}
				}
				
				return result;
			} else {
				ObjectArray result = new ObjectArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					Object obj = array.get(i);
					if (obj instanceof Number) {
						obj = new Double(Math.sqrt(((Number)obj).doubleValue()));
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
					}
					
					result.push(obj);
				}
				
				return result;
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqrt" + mm.getMessage("function.invalidParam"));
			}
			
			IArray array1 = sub0.getLeafExpression().calculateAll(ctx);
			IArray array2 = sub1.getLeafExpression().calculateAll(ctx);
			int size = array1.size();
			
			if (array2 instanceof ConstArray) {
				Object obj2 = array2.get(1);
				if (obj2 == null) {
					return new ConstArray(null, size);
				} else if (!(obj2 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
				}
				
				double d2 = 1 / ((Number)obj2).doubleValue();
				if (array1 instanceof ConstArray) {
					Object obj1 = array1.get(1);
					if (obj1 instanceof Number) {
						double d1 = ((Number)obj1).doubleValue();
						d1 = Math.pow(d1, d2);
						return new ConstArray(new Double(d1), size);
					} else if (obj1 == null) {
						return new ConstArray(null, size);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
					}
				}

				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				
				if (array1 instanceof NumberArray) {
					NumberArray numberArray = (NumberArray)array1;
					for (int i = 1; i <= size; ++i) {
						if (numberArray.isNull(i)) {
							result.pushNull();
						} else {
							double v = Math.pow(numberArray.getDouble(i), d2);
							result.push(v);
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						Object obj1 = array1.get(i);
						if (obj1 instanceof Number) {
							double v = Math.pow(((Number)obj1).doubleValue(), d2);
							result.push(v);
						} else if (obj1 == null) {
							result.pushNull();
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
						}
					}
				}
				
				return result;
			} else {
				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					Object obj1 = array1.get(i);
					Object obj2 = array2.get(i);
					
					if (obj1 == null || obj2 == null) {
						result.pushNull();
					} else if (obj1 instanceof Number && obj2 instanceof Number) {
						double d2 = 1 / ((Number)obj2).doubleValue();
						double v = Math.pow(((Number)obj1).doubleValue(), d2);
						result.push(v);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
					}
				}
				
				return result;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sqrt" + mm.getMessage("function.invalidParam"));
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
		if (param.isLeaf()) {
			IArray array = param.getLeafExpression().calculateAll(ctx, signArray, sign);
			int size = array.size();
			
			if (array instanceof ConstArray) {
				Object obj = array.get(1);
				if (obj instanceof Number) {
					obj = new Double(Math.sqrt(((Number)obj).doubleValue()));
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
				}
				
				return new ConstArray(obj, size);
			}
			
			boolean[] signDatas;
			if (sign) {
				signDatas = signArray.isTrue().getDatas();
			} else {
				signDatas = signArray.isFalse().getDatas();
			}

			if (array instanceof NumberArray) {
				NumberArray numberArray = (NumberArray)array;
				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (!signDatas[i] || numberArray.isNull(i)) {
						result.pushNull();
					} else {
						double v = numberArray.getDouble(i);
						result.push(Math.sqrt(v));
					}
				}
				
				return result;
			} else {
				ObjectArray result = new ObjectArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						Object obj = array.get(i);
						if (obj instanceof Number) {
							obj = new Double(Math.sqrt(((Number)obj).doubleValue()));
						} else if (obj != null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
						}
						
						result.push(obj);
					} else {
						result.pushNull();
					}
				}
				
				return result;
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqrt" + mm.getMessage("function.invalidParam"));
			}
			
			IArray array1 = sub0.getLeafExpression().calculateAll(ctx);
			IArray array2 = sub1.getLeafExpression().calculateAll(ctx);
			int size = array1.size();
			
			boolean[] signDatas;
			if (sign) {
				signDatas = signArray.isTrue().getDatas();
			} else {
				signDatas = signArray.isFalse().getDatas();
			}
			
			if (array2 instanceof ConstArray) {
				Object obj2 = array2.get(1);
				if (obj2 == null) {
					return new ConstArray(null, size);
				} else if (!(obj2 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
				}
				
				double d2 = 1 / ((Number)obj2).doubleValue();
				if (array1 instanceof ConstArray) {
					Object obj1 = array1.get(1);
					if (obj1 instanceof Number) {
						double d1 = ((Number)obj1).doubleValue();
						d1 = Math.pow(d1, d2);
						return new ConstArray(new Double(d1), size);
					} else if (obj1 == null) {
						return new ConstArray(null, size);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
					}
				}

				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				
				if (array1 instanceof NumberArray) {
					NumberArray numberArray = (NumberArray)array1;
					for (int i = 1; i <= size; ++i) {
						if (!signDatas[i] || numberArray.isNull(i)) {
							result.pushNull();
						} else {
							double v = Math.pow(numberArray.getDouble(i), d2);
							result.push(v);
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signDatas[i]) {
							Object obj1 = array1.get(i);
							if (obj1 instanceof Number) {
								double v = Math.pow(((Number)obj1).doubleValue(), d2);
								result.push(v);
							} else if (obj1 == null) {
								result.pushNull();
							} else {
								MessageManager mm = EngineMessage.get();
								throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
							}
						} else {
							result.pushNull();
						}
					}
				}
				
				return result;
			} else {
				DoubleArray result = new DoubleArray(size);
				result.setTemporary(true);
				
				for (int i = 1; i <= size; ++i) {
					if (signDatas[i]) {
						Object obj1 = array1.get(i);
						Object obj2 = array2.get(i);
						
						if (obj1 == null || obj2 == null) {
							result.pushNull();
						} else if (obj1 instanceof Number && obj2 instanceof Number) {
							double d2 = 1 / ((Number)obj2).doubleValue();
							double v = Math.pow(((Number)obj1).doubleValue(), d2);
							result.push(v);
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
						}
					} else {
						result.pushNull();
					}
				}
				
				return result;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sqrt" + mm.getMessage("function.invalidParam"));
		}
	}
}
