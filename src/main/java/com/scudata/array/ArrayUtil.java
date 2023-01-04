package com.scudata.array;

import java.util.Date;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Sequence;
import com.scudata.expression.Relation;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CursorUtil;
import com.scudata.util.Variant;

public final class ArrayUtil {
	public static IArray newArray(Object value, int capacity) {
		if (value instanceof Integer) {
			IntArray result = new IntArray(capacity);
			result.pushInt(((Integer)value).intValue());
			return result;
		} else if (value instanceof Long) {
			LongArray result = new LongArray(capacity);
			result.pushLong(((Long)value).longValue());
			return result;
		} else if (value instanceof Double) {
			DoubleArray result = new DoubleArray(capacity);
			result.pushDouble(((Double)value).doubleValue());
			return result;
		} else if (value instanceof Date) {
			DateArray result = new DateArray(capacity);
			result.pushDate((Date)value);
			return result;
		} else if (value instanceof String) {
			StringArray result = new StringArray(capacity);
			result.pushString((String)value);
			return result;
		} else if (value instanceof Boolean) {
			BoolArray result = new BoolArray(capacity);
			result.pushBool(((Boolean)value).booleanValue());
			return result;
		} else {
			ObjectArray result = new ObjectArray(capacity);
			result.push(value);
			return result;
		}
	}
	
	/**
	 * 取数组成员的布尔值组成新数组
	 * @param array
	 * @param value
	 * @return
	 */
	public static BoolArray booleanValue(IArray array, boolean value) {
		if (value) {
			BoolArray result = array.isTrue();
			if (result == array) {
				return (BoolArray)result.dup();
			} else {
				return result;
			}
		} else {
			return array.isFalse();
		}
	}
	
	/**
	 * 计算数组的成员和null的关系
	 * @param signs 数组成员是否为空标志，true为空
	 * @param size 数组成员数
	 * @param relation 比较关系
	 * @return BoolArray 比较值数组
	 */
	public static BoolArray calcRelationNull(boolean []signs, int size, int relation) {
		boolean []resultDatas = new boolean[size + 1];		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			if (signs != null) {
				System.arraycopy(signs, 1, resultDatas, 1, size);
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!signs[i]) {
						resultDatas[i] = true;
					}
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = true;
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (signs != null) {
				System.arraycopy(signs, 1, resultDatas, 1, size);
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!signs[i]) {
						resultDatas[i] = true;
					}
				}
			}
		} else if (relation == Relation.OR) {
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !signs[i];
				}
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	/**
	 * 数组的成员和null做比较
	 * @param datas 数组
	 * @param size 数组成员数
	 * @param relation 比较关系
	 * @return BoolArray 比较值数组
	 */
	public static BoolArray calcRelationNull(Object []datas, int size, int relation) {
		boolean []resultDatas = new boolean[size + 1];		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			for (int i = 1; i <= size; ++i) {
				if (datas[i] == null) {
					resultDatas[i] = true;
				}
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					resultDatas[i] = true;
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = true;
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			for (int i = 1; i <= size; ++i) {
				if (datas[i] == null) {
					resultDatas[i] = true;
				}
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					resultDatas[i] = true;
				}
			}
		} else if (relation == Relation.OR) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = datas[i] != null;
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	/**
	 * 计算数组的成员和null的关系
	 * @param signs 数组成员是否为空标志，true为空
	 * @param size 数组成员数
	 * @param relation 比较关系
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	public static void calcRelationsNull(boolean []signs, int size, int relation, BoolArray result, boolean isAnd) {
		boolean []resultDatas = result.getDatas();
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = false;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!signs[i]) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (signs != null) {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = false;
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (signs != null) {
					for (int i = 1; i <= size; ++i) {
						if (!signs[i]) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (signs != null) {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							resultDatas[i] = false;
						}
					}
				}
			} else {
				throw new RuntimeException();
			}
		} else {
			// 与左侧结果执行||运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				if (signs != null) {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = true;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!signs[i]) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (signs != null) {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = true;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!signs[i]) {
							resultDatas[i] = true;
						}
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}
	
	/**
	 * 数组的成员和null做比较
	 * @param datas 数组
	 * @param size 数组成员数
	 * @param relation 比较关系
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	public static void calcRelationsNull(Object []datas, int size, int relation, BoolArray result, boolean isAnd) {
		boolean []resultDatas = result.getDatas();	
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (datas[i] == null) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = false;
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (datas[i] == null) {
						resultDatas[i] = false;
					}
				}
			} else {
				throw new RuntimeException();
			}
		} else {
			// 与左侧结果执行||运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				for (int i = 1; i <= size; ++i) {
					if (datas[i] == null) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (datas[i] == null) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						resultDatas[i] = true;
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}
	
	/**
	 * 取余或序列成员异或列
	 * @param o1 Object
	 * @param o2 Object
	 * @return Object
	 */
	public static Object mod(Object o1, Object o2) {
		if (o1 instanceof Number) {
			if (o2 instanceof Number) {
				return Variant.mod((Number)o1, (Number)o2);
			} else if (o2 instanceof Sequence) {
				Sequence seq1 = new Sequence(1);
				seq1.add(o1);
				return CursorUtil.xor(seq1, (Sequence)o2);
			} else if (o2 == null) {
				return null;
			}
		} else if (o1 instanceof Sequence) {
			if (o2 instanceof Sequence) {
				return CursorUtil.xor((Sequence)o1, (Sequence)o2);
			} else if (o2 == null) {
				return o1;
			} else {
				Sequence seq2 = new Sequence(1);
				seq2.add(o2);
				return CursorUtil.xor((Sequence)o1, seq2);
			}
		} else if (o2 instanceof Sequence) {
			if (o1 == null) {
				return o2;
			} else {
				Sequence seq1 = new Sequence(1);
				seq1.add(o1);
				return CursorUtil.xor(seq1, (Sequence)o2);
			}
		} else if (o1 == null) {
			if (o2 instanceof Sequence) {
				return o2;
			} else if (o2 instanceof Number) {
				return null;
			} else if (o2 == null) {
				return null;
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(Variant.getDataType(o1) + mm.getMessage("Variant2.with") +
				Variant.getDataType(o2) + mm.getMessage("Variant2.illMod"));
	}
	
	/**
	 * 整出或序列成员差集
	 * @param o1 Object
	 * @param o2 Object
	 * @return Object
	 */
	public static Object intDivide(Object o1, Object o2) {
		if (o1 instanceof Number) {
			if (o2 instanceof Number) {
				return Variant.intDivide((Number)o1, (Number)o2);
			} else if (o2 == null) {
				return null;
			}
		} else if (o1 instanceof Sequence) {
			if (o2 instanceof Sequence) {
				return ((Sequence)o1).diff((Sequence)o2, false);
			} else if (o2 == null) {
				return o1;
			} else {
				Sequence seq2 = new Sequence(1);
				seq2.add(o2);
				return ((Sequence)o1).diff(seq2, false);
			}
		} else if (o1 == null) {
			if (o2 instanceof Number) {
				return null;
			} else if (o2 == null) {
				return null;
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(Variant.getDataType(o1) + mm.getMessage("Variant2.with") +
				Variant.getDataType(o2) + mm.getMessage("Variant2.illDivide"));
	}
}
