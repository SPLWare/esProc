package com.scudata.dm;

import java.io.Externalizable;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.IRecord;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

public abstract class BaseRecord implements IComputeItem, Externalizable, IRecord, Comparable<BaseRecord> {
	
	/*以下接口继承自IComputeItem，用于计算*/
	public Object getCurrent() {
		return this;
	}
	
	public int getCurrentIndex() {
		throw new RuntimeException();
	}
	
	public Sequence getCurrentSequence() {
		return null;
	}
	
	public boolean isInStack(ComputeStack stack) {
		return stack.isInComputeStack(this);
	}
		
	public void popStack() {
	}
	/*以上接口继承自IComputeItem，用于计算*/

	/**
	 * 针对记录计算表达式
	 * @param exp Expression 计算表达式
	 * @param ctx Context 计算上下文环境
	 * @return Object
	 */
	public Object calc(Expression exp, Context ctx) {
		if (exp == null) {
			return null;
		}

		ComputeStack stack = ctx.getComputeStack();
		stack.push(this);
		try {
			return exp.calculate(ctx);
		} finally {
			stack.pop();
		}
	}

	/**
	 * 记录比较用hash值，相同再用r.v()
	 * @param r 记录
	 */
	public int compareTo(BaseRecord r) {
		if (r == this) {
			return 0;
		}
		
		int h1 = hashCode();
		int h2 = r.hashCode();
		
		if (h1 < h2) {
			return -1;
		} else if (h1 > h2) {
			return 1;
		} else {
			Object []vals1 = getFieldValues();
			Object []vals2 = r.getFieldValues();
			int []pkIndex1 = getPKIndex();
			int []pkIndex2 = r.getPKIndex();
			
			if (pkIndex1 != null && pkIndex2 != null && pkIndex1.length == pkIndex2.length) {
				for (int i = 0; i < pkIndex1.length; ++i) {
					int result = Variant.compare(vals1[pkIndex1[i]], vals2[pkIndex2[i]], true);
					if (result != 0) {
						return result;
					}
				}
				
				return 0;
			} else {
				int len1 = vals1.length;
				int len2 = vals2.length;
				int minLen = len1 > len2 ? len2 : len1;
				
				for (int i = 0; i < minLen; ++i) {
					int result = Variant.compare(vals1[i], vals2[i], true);
					if (result != 0) {
						return result;
					}
				}
				
				return len1 == len2 ? 0 : (len1 > len2 ? 1 : -1);
			}
		}
	}

	/**
	 * 比较记录指定字段的值
	 * @param fields 字段索引数组
	 * @param fvalues 字段值数组
	 * @return 1：当前记录大，0：相等，-1：当前记录小
	 */
	public abstract int compare(int []fields, Object []fvalues);
	
	/**
	 * 按指定字段比较大小，记录的数据结构必须相同？
	 * @param r BaseRecord
	 * @param fields int[] 字段索引
	 * @return int
	 */
	public int compare(BaseRecord r, int []fields) {
		/*if (r == this) {
			return 0;
		} else if (r == null) {
			return 1;
		}*/

		for (int f : fields) {
			int result = Variant.compare(getNormalFieldValue(f), r.getNormalFieldValue(f), true);
			if (result != 0) {
				return result;
			}
		}
		
		return 0;
	}
	
	/**
	 * 按指定字段比较大小，记录的数据结构必须相同？
	 * @param r BaseRecord
	 * @param field 字段索引
	 * @return int
	 */
	public int compare(BaseRecord r, int field) {
		return Variant.compare(getNormalFieldValue(field), r.getNormalFieldValue(field), true);
	}

	/**
	 * 按字段值比较两条记录的大小，记录的数据结构必须相同
	 * @param r 记录
	 * @return 1：当前记录大，0：相等，-1：当前记录小
	 */
	public abstract int compare(BaseRecord r);
	
	/**
	 * 把记录的指定字段与指定数组的元素相比较
	 * @param field 字段索引
	 * @param values 字段值数组
	 * @param index 字段值数组的索引
	 * @return 1：当前记录大，0：相等，-1：当前记录小
	 */
	public int compare(int field, IArray values, int index) {
		return Variant.compare(getFieldValue(field), values.get(index), true);
	}
	
	/**
	 * 返回记录的结构
	 * @return DataStruct
	 */
	public abstract DataStruct dataStruct();
	
	/**
	 * 返回字段的数目
	 * @return 字段数
	 */
	public abstract int getFieldCount();
	
	/**
	 * 返回字段的索引，伪字段从非伪字段的数目开始计数，如果字段不存在则返回-1
	 * @param name 字段名
	 * @return 字段索引，从0开始计数
	 */
	public int getFieldIndex(String name) {
		return dataStruct().getFieldIndex(name);
	}

	/**
	 * 返回记录所有字段名
	 * @return 字段名数组
	 */
	public String[] getFieldNames() {
		return dataStruct().getFieldNames();
	}

	/**
	 * 返回某一字段的值
	 * @param index 字段索引，从0开始计数
	 * @return Object
	 */
	public abstract Object getFieldValue(int index);

	/**
	 * 返回某一字段的值
	 * @param name 字段名
	 * @return Object
	 */
	public abstract Object getFieldValue(String name);
	
	/**
	 * 取字段值，字段不存在返回空，此方法为了支持结构不纯的排列
	 * @param index 字段序号，从0开始计数
	 * @return Object
	 */
	public abstract Object getFieldValue2(int index);

	/**
	 * 返回所有字段的值
	 * @return 字段值数组
	 */
	public abstract Object[] getFieldValues();
	
	/**
	 * 取字段值，不做边界检查
	 * @param index 字段序号，从0开始计数
	 * @return Object
	 */
	public abstract Object getNormalFieldValue(int index);
	
	/**
	 * 取字段值，不做边界检查
	 * @param index 字段序号，从0开始计数
	 * @param out 用于存放结果，容量足够不在做容量判断
	 */
	public abstract void getNormalFieldValue(int index, IArray out);
	
	/**
	 * 创建指定字段的数组
	 * @param f 字段索引，从0开始计数
	 * @param len 数组长度
	 * @return IArray
	 */
	public IArray createFieldValueArray(int f, int len) {
		return new ObjectArray(len);
	}
	
	/**
	 * 返回主键在结构中的索引，没有定义主键则返回空
	 * @return 主键索引数组
	 */
	public int[] getPKIndex() {
		return dataStruct().getPKIndex();
	}
	
	/**
	 * 返回记录的主键或多主键构成的序列，没有主键抛异常
	 * @return Object
	 */
	public Object getPKValue() {
		DataStruct ds = dataStruct();
		int []pkIndex = ds.getPKIndex();
		if (pkIndex == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("ds.lessKey"));
		} else {
			int keyCount = pkIndex.length - ds.getTimeKeyCount();
			if (keyCount == 1) {
				Object obj = getNormalFieldValue(pkIndex[0]);
				if (obj instanceof BaseRecord) {
					return ((BaseRecord)obj).getPKValue();
				} else {
					return obj;
				}
			} else {
				Sequence keySeries = new Sequence(keyCount);
				for (int i = 0; i < keyCount; ++i) {
					Object obj = getNormalFieldValue(pkIndex[i]);
					if (obj instanceof BaseRecord) {
						obj = ((BaseRecord)obj).getPKValue();
					}

					if (obj instanceof Sequence) {
						keySeries.addAll((Sequence)obj);
					} else {
						keySeries.add(obj);
					}
				}
				
				return keySeries;
			}
		}
	}
	
	/**
	 * 返回记录的值r.v()
	 * @return 如果设置了主键则返回主键值，否则返回所有字段构成的序列
	 */
	public Object value() {
		// 如果外键有环会导致死循环？
		// 指引字段改为取主键？
		DataStruct ds = dataStruct();
		int []pkIndex = ds.getPKIndex();
		if (pkIndex == null) {
			int fcount = ds.getFieldCount();
			Sequence seq = new Sequence(fcount);
			for (int f = 0; f < fcount; ++f) {
				Object obj = getNormalFieldValue(f);
				if (obj instanceof BaseRecord) {
					obj = ((BaseRecord)obj).key();
				}

				if (obj instanceof Sequence) {
					seq.addAll((Sequence)obj);
				} else {
					seq.add(obj);
				}
			}
			
			return seq;
		} else {
			int keyCount = pkIndex.length - ds.getTimeKeyCount();
			if (keyCount == 1) {
				Object obj = getNormalFieldValue(pkIndex[0]);
				if (obj instanceof BaseRecord) {
					return ((BaseRecord)obj).key();
				} else {
					return obj;
				}
			} else {
				Sequence keySeries = new Sequence(keyCount);
				for (int i = 0; i < keyCount; ++i) {
					Object obj = getNormalFieldValue(pkIndex[i]);
					if (obj instanceof BaseRecord) {
						//obj = ((BaseRecord)obj).value();
						obj = ((BaseRecord)obj).key();
					}

					if (obj instanceof Sequence) {
						keySeries.addAll((Sequence)obj);
					} else {
						keySeries.add(obj);
					}
				}
				
				return keySeries;
			}
		}
	}
	
	/**
	 * 返回记录是否有时间键
	 * @return true：有时间键
	 */
	public boolean hasTimeKey() {
		return dataStruct().getTimeKeyCount() > 0;
	}

	/**
	 * 按字段值比较两条记录是否相等，记录的数据结构必须相同？
	 * @param r 要比较的记录
	 * @return boolean true：相等
	 */
	public abstract boolean isEquals(BaseRecord r);
	
	/**
	 * 判断两记录的指定字段是否相等
	 * @param r 要比较的记录
	 * @param index 字段索引
	 * @return boolean true：相等
	 */
	public abstract boolean isEquals(BaseRecord r, int []index);
	
	/**
	 * 优化时使用，判断相邻的记录的数据结构是否相同
	 * @param cur
	 * @return
	 */
	public boolean isSameDataStruct(BaseRecord cur) {
		return dataStruct() == cur.dataStruct();
	}

	/**
	 * 返回记录的主键或多主键构成的序列，没有主键时返回空
	 * @return Object
	 */
	public Object key() {
		DataStruct ds = dataStruct();
		int []pkIndex = ds.getPKIndex();
		if (pkIndex == null) {
			return null;
		} else {
			int keyCount = pkIndex.length - ds.getTimeKeyCount();
			if (keyCount == 1) {
				Object obj = getNormalFieldValue(pkIndex[0]);
				if (obj instanceof BaseRecord) {
					return ((BaseRecord)obj).key();
				} else {
					return obj;
				}
			} else {
				Sequence keySeries = new Sequence(keyCount);
				for (int i = 0; i < keyCount; ++i) {
					Object obj = getNormalFieldValue(pkIndex[i]);
					if (obj instanceof BaseRecord) {
						obj = ((BaseRecord)obj).key();
					}

					if (obj instanceof Sequence) {
						keySeries.addAll((Sequence)obj);
					} else {
						keySeries.add(obj);
					}
				}
				
				return keySeries;
			}
		}
	}

	/**
	 * 修改记录的字段值
	 * @param exps 值表达式数组
	 * @param fields 字段名数组
	 * @param ctx 计算上下文
	 */
	public abstract void modify(Expression[] exps, String[] fields, Context ctx);
	
	/**
	 * 把源记录的字段值赋给当前记录
	 * @param sr 源记录
	 * @param isName 是否按字段名进行复制
	 */
	public abstract void paste(BaseRecord sr, boolean isName);
	
	/**
	 * 把序列的元素依次赋给当前记录
	 * @param sequence 值序列
	 */
	public abstract void paste(Sequence sequence);
	
	/**
	 * 把序列的元素依次赋给当前记录
	 * @param sequence 值序列
	 * @param start 序列的起始位置
	 */
	public abstract void paste(Sequence sequence, int start);
	
	/**
	 * 对记录的外键做递归查询
	 * @param field 外键字段名
	 * @param p 指向的最终记录
	 * @param maxLevel 遍历最大的层次
	 * @return 引用记录构成的序列
	 */
	public Sequence prior(String field, BaseRecord p, int maxLevel) {
		int f = getFieldIndex(field);
		if (f == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
		}
		
		return prior(f, p, maxLevel);
	}
	
	/**
	 * 对记录的外键做递归查询
	 * @param f 外键字段序号，从0开始计数
	 * @param p 指向的最终记录
	 * @param maxLevel 遍历最大的层次
	 * @return 引用记录构成的序列
	 */
	public Sequence prior(int f, BaseRecord p, int maxLevel) {
		if (this == p) {
			return new Sequence(0);
		}
		
		Sequence seq = new Sequence();
		BaseRecord r = this;
		if (maxLevel > 0) {
			for (int i = 0; i < maxLevel; ++i) {
				Object obj = r.getNormalFieldValue(f);
				if (obj == p) {
					seq.add(r);
					return seq;
				} else if (obj == null) {
					return null;
				} else if (obj instanceof BaseRecord) {
					seq.add(r);
					r = (BaseRecord)obj;
				} else {
					return null;
				}
			}
			
			return null;
		} else {
			while (true) {
				Object obj = r.getNormalFieldValue(f);
				if (obj == p) {
					seq.add(r);
					return seq;
				} else if (obj == null) {
					return null;
				} else if (obj instanceof BaseRecord) {
					seq.add(r);
					r = (BaseRecord)obj;
				} else {
					return null;
				}
			}
		}
	}
	
	/**
	 * 计算表达式
	 * @param exp 计算表达式
	 * @param ctx 计算上下文
	 */
	public void run(Expression exp, Context ctx) {
		if (exp == null) {
			return;
		}

		ComputeStack stack = ctx.getComputeStack();
		stack.push(this);
		try {
			exp.calculate(ctx);
		} finally {
			stack.pop();
		}
	}

	/**
	 * 针对记录计算表达式并进行赋值
	 * @param assignExps 赋值表达式数组
	 * @param exps 值表达式数组
	 * @param ctx 计算上下文
	 */
	public void run(Expression[] assignExps, Expression[] exps, Context ctx) {
		if (exps == null || exps.length == 0) return;

		int colCount = exps.length;
		if (assignExps == null) {
			assignExps = new Expression[colCount];
		} else if (assignExps.length != colCount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("run" + mm.getMessage("function.invalidParam"));
		}

		ComputeStack stack = ctx.getComputeStack();
		stack.push(this);
		try {
			for (int c = 0; c < colCount; ++c) {
				if (assignExps[c] == null) {
					exps[c].calculate(ctx);
				} else {
					assignExps[c].assign(exps[c].calculate(ctx), ctx);
				}
			}
		} finally {
			stack.pop();
		}
	}

	/**
	 * 设置指定字段的值
	 * @param index 字段索引，从0开始计数
	 * @param val 字段新值
	 */
	public abstract void set(int index, Object val);

	/**
	 * 设置指定字段的值
	 * @param name 字段名
	 * @param val 字段新值
	 */
	public abstract void set(String name, Object val);

	/**
	 * 把指定记录各字段的值依次设给当前记录，记录字段数需相同
	 * @param r 记录
	 */
	public abstract void set(BaseRecord r);
	
	/**
	 * 设置字段值，不做边界检查
	 * @param index 字段序号，从0开始计数
	 * @param val 字段值
	 */
	public abstract void setNormalFieldValue(int index, Object val);
	
	/**
	 * 设置字段值，如果字段不存在不做任何处理
	 * @param index 字段索引，从0开始计数，如果超界则不做任何处理
	 * @param val 字段新值
	 */
	public abstract void set2(int index, Object val);
	
	/**
	 * 把数组的值从指定字段开始依次设给当前记录
	 * @param index 字段索引，从0开始计数
	 * @param objs 字段值数组
	 */
	public abstract void setStart(int index, Object []objs);
	
	/**
	 * 把数组的值从指定字段开始依次设给当前记录
	 * @param index 字段索引，从0开始计数
	 * @param objs 字段值数组
	 * @param len 赋值的字段数
	 */
	public abstract void setStart(int index, Object []objs, int len);
	
	/**
	 * 把记录的值从指定字段开始依次设给当前记录
	 * @param index 字段索引，从0开始计数
	 * @param r 记录
	 */
	public abstract void setStart(int index, BaseRecord r);
	
	/**
	 * 将当前记录的可文本化字段转成字串
	 * @param opt t：用'\t'分隔字段，缺省用逗号，q：串成员接入时加上引号，缺省不会处理，
	 * f：仅转换r的字段名而非字段值
	 * @return String
	 */
	public abstract String toString(String opt);
	
	/**
	 * 把当前记录转成Record型的记录，如果本事是Record型则直接返回
	 * @return Record
	 */
	public abstract Record toRecord();
}
