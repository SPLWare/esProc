package com.scudata.array;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Sequence;
import com.scudata.expression.Relation;
import com.scudata.expression.fn.math.And;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 常量数组
 * @author LW
 *
 */
public class ConstArray implements IArray {
	private static final long serialVersionUID = 1L;

	private Object data;
	private int size;

	// 仅用于序列化
	public ConstArray() {
	}
	
	public ConstArray(Object data, int size) {
		this.data = data;
		this.size = size;
	}
	
	/**
	 * 取数组的类型串，用于错误信息提示
	 * @return 类型串
	 */
	public String getDataType() {
		return Variant.getDataType(data);
	}
	
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
	
	/**
	 * 追加元素，如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void add(Object o) {
		if (Variant.isEquals(data, o)) {
			size++;
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}
	
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(IArray array) {
		if (array.size() == 0) {
			return;
		}
		
		if (array instanceof ConstArray && Variant.isEquals(data, array.get(1))) {
			size += array.size();
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param count 元素个数
	 */
	public void addAll(IArray array, int count) {
		if (count == 0) {
			return;
		}
		
		if (array instanceof ConstArray && Variant.isEquals(data, array.get(1))) {
			size += count;
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(Object []array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}
	
	/**
	 * 插入元素，如果类型不兼容则抛出异常
	 * @param index 插入位置，从1开始计数
	 * @param o 元素值
	 */
	public void insert(int index, Object o) {
		if (Variant.isEquals(data, o)) {
			size++;
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}

	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}
	
	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, Object []array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}

	/**
	 * 追加元素（不检查容量，认为有足够空间存放元素），如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void push(Object o) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}
	
	/**
	 * 追加一个空成员（不检查容量，认为有足够空间存放元素）
	 */
	public void pushNull() {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}
	
	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void push(IArray array, int index) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}
	
	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void add(IArray array, int index) {
		if (Variant.isEquals(data, array.get(index))) {
			size++;
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}
	
	/**
	 * 把array中的第index个元素设给到当前数组的指定元素，如果类型不兼容则抛出异常
	 * @param curIndex 当前数组的元素索引，从1开始计数
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void set(int curIndex, IArray array, int index) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}	
	
	/**
	 * 取指定位置元素
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public Object get(int index) {
		return data;
	}

	/**
	 * 取指定位置元素的整数值
	 * @param index 索引，从1开始计数
	 * @return 整数值
	 */
	public int getInt(int index) {
		return ((Number)data).intValue();
	}
	
	/**
	 * 取指定位置元素的长整数值
	 * @param index 索引，从1开始计数
	 * @return 长整数值
	 */
	public long getLong(int index) {
		return ((Number)data).longValue();
	}
	
	/**
	 * 取指定位置元素组成新数组
	 * @param indexArray 位置数组
	 * @return IArray
	 */
	public IArray get(int []indexArray) {
		return new ConstArray(data, indexArray.length);
	}
	
	/**
	 * 取指定位置元素组成新数组
	 * @param indexArray 位置数组
	 * @param start 起始位置，包含
	 * @param end 结束位置，包含
	 * @param doCheck true：位置可能包含0，0的位置用null填充，false：不会包含0
	 * @return IArray
	 */
	public IArray get(int []indexArray, int start, int end, boolean doCheck) {
		int len = end - start + 1;
		Object data = this.data;
		
		if (doCheck && data != null) {
			Object []resultDatas = new Object[len + 1];
			for (int i = 1; start <= end; ++start, ++i) {
				int q = indexArray[start];
				if (q > 0) {
					resultDatas[i] = data;
				}
			}
			
			return new ObjectArray(resultDatas, len);
		} else {
			return new ConstArray(data, len);
		}
	}
	
	/**
	 * 取指定位置元素组成新数组
	 * @param NumberArray 位置数组
	 * @return IArray
	 */
	public IArray get(NumberArray indexArray) {
		return new ConstArray(data, indexArray.size());
	}
	
	/**
	 * 取某一区段组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @return IArray
	 */
	public IArray get(int start, int end) {
		return new ConstArray(data, end - start);
	}
	
	/**
	 * 使列表的容量不小于minCapacity
	 * @param minCapacity 最小容量
	 */
	public void ensureCapacity(int minCapacity) {
	}
	
	/**
	 * 调整容量，使其与元素数相等
	 */
	public void trimToSize() {
	}
	
	/**
	 * 判断指定位置的元素是否是空
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isNull(int index) {
		return data == null;
	}
	
	/**
	 * 判断元素是否是True
	 * @return BoolArray
	 */
	public BoolArray isTrue() {
		int size = this.size;
		boolean []resultDatas = new boolean[size + 1];
		boolean value = Variant.isTrue(data);
		
		for (int i = 1; i <= size; ++i) {
			resultDatas[i] = value;
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	/**
	 * 判断元素是否是假
	 * @return BoolArray
	 */
	public BoolArray isFalse() {
		int size = this.size;
		boolean []resultDatas = new boolean[size + 1];
		boolean value = Variant.isFalse(data);
		
		for (int i = 1; i <= size; ++i) {
			resultDatas[i] = value;
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	/**
	 * 判断指定位置的元素是否是True
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isTrue(int index) {
		return Variant.isTrue(data);
	}
	
	/**
	 * 判断指定位置的元素是否是False
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isFalse(int index) {
		return Variant.isFalse(data);
	}

	/**
	 * 是否是计算过程中临时产生的数组，临时产生的可以被修改，比如 f1+f2+f3，只需产生一个数组存放结果
	 * @return true：是临时产生的数组，false：不是临时产生的数组
	 */
	public boolean isTemporary() {
		return false;
	}

	/**
	 * 设置是否是计算过程中临时产生的数组
	 * @param ifTemporary true：是临时产生的数组，false：不是临时产生的数组
	 */
	public void setTemporary(boolean ifTemporary) {
	}
	
	/**
	 * 删除最后一个元素
	 */
	public void removeLast() {
		size--;
	}
	
	/**
	 * 删除指定位置的元素
	 * @param index 索引，从1开始计数
	 */
	public void remove(int index) {
		size--;
	}
	
	/**
	 * 删除指定区间内的元素
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 */
	public void removeRange(int fromIndex, int toIndex) {
		size -= (toIndex - fromIndex + 1);
	}
	
	/**
	 * 删除指定位置的元素，序号从小到大排序
	 * @param seqs 索引数组
	 */
	public void remove(int []seqs) {
		size -= seqs.length;
	}
	
	/**
	 * 保留指定区间内的数据
	 * @param start 起始位置（包含）
	 * @param end 结束位置（包含）
	 */
	public void reserve(int start, int end) {
		size = end - start + 1;
	}

	public int size() {
		return size;
	}
	
	/**
	 * 返回数组的非空元素数目
	 * @return 非空元素数目
	 */
	public int count() {
		return data != null ? size : 0;
	}
	
	/**
	 * 判断数组是否有取值为true的元素
	 * @return true：有，false：没有
	 */
	public boolean containTrue() {
		return Variant.isTrue(data);
	}
	
	/**
	 * 返回第一个不为空的元素
	 * @return Object
	 */
	public Object ifn() {
		return data;
	}

	/**
	 * 修改数组指定元素的值，如果类型不兼容则抛出异常
	 * @param index 索引，从1开始计数
	 * @param obj 值
	 */
	public void set(int index, Object obj) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("pdm.modifyConstArrayError"));
	}
	
	/**
	 * 删除所有的元素
	 */
	public void clear() {
		data = null;
		size = 0;
	}

	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @return int 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem) {
		if (size == 0) {
			return -1;
		}
		
		int cmp = Variant.compare(data, elem, true);
		if (cmp == 0) {
			return 1;
		} else if (cmp < 0) {
			return -1;
		} else {
			return -size - 1;
		}
	}
	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @param start 起始查找位置（包含）
	 * @param end 结束查找位置（包含）
	 * @return 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem, int start, int end) {
		if (end == 0) {
			return -1;
		}
		
		int cmp = Variant.compare(data, elem, true);
		if (cmp == 0) {
			return start;
		} else if (cmp < 0) {
			return -1;
		} else {
			return -size - 1;
		}
	}
	
	/**
	 * 返回列表中是否包含指定元素
	 * @param elem Object 待查找的元素
	 * @return boolean true：包含，false：不包含
	 */
	public boolean contains(Object elem) {
		if (size == 0) {
			return false;
		}
		
		return Variant.isEquals(data, elem);
	}
	/**
	 * 判断数组的元素是否在当前数组中
	 * @param isSorted 当前数组是否有序
	 * @param array 数组
	 * @param result 用于存放结果，只找取值为true的
	 */
	public void contains(boolean isSorted, IArray array, BoolArray result) {
		int resultSize = result.size();
		if (size > 0) {
			Object data = this.data;
			for (int i = 1; i <= resultSize; ++i) {
				if (result.isTrue(i) && !Variant.isEquals(data, array.get(i))) {
					result.set(i, false);
				}
			}
		} else {
			for (int i = 1; i <= resultSize; ++i) {
				result.set(i, false);
			}
		}
	}
	
	/**
	 * 返回列表中是否包含指定元素，使用等号比较
	 * @param elem
	 * @return boolean true：包含，false：不包含
	 */
	public boolean objectContains(Object elem) {
		return data == elem;
	}
	
	/**
	 * 返回元素在数组中首次出现的位置
	 * @param elem 待查找的元素
	 * @param start 起始查找位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	public int firstIndexOf(Object elem, int start) {
		if (size == 0) {
			return 0;
		}
		
		return Variant.isEquals(data, elem) ? start : 0;
	}
	
	/**
	 * 返回元素在数组中最后出现的位置
	 * @param elem 待查找的元素
	 * @param start 从后面开始查找的位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	public int lastIndexOf(Object elem, int start) {
		if (size == 0) {
			return 0;
		}
		
		return Variant.isEquals(data, elem) ? start : 0;
	}
	
	/**
	 * 返回元素在数组中所有出现的位置
	 * @param elem 待查找的元素
	 * @param start 起始查找位置（包含）
	 * @param isSorted 当前数组是否有序
	 * @param isFromHead true：从头开始遍历，false：从尾向前开始遍历
	 * @return IntArray
	 */
	public IntArray indexOfAll(Object elem, int start, boolean isSorted, boolean isFromHead) {
		if (size > 0 && Variant.isEquals(data, elem)) {
			int end = size;
			if (isFromHead) {
				end = start;
				start = 1;
			}
			
			IntArray result = new IntArray(end - start + 1);
			if (isFromHead) {
				for (; start <= end; ++start) {
					result.pushInt(start);
				}
			} else {
				for (; end >= start; --end) {
					result.pushInt(end);
				}
			}
			
			return result;
		} else {
			return new IntArray(1);
		}
	}
	
	/**
	 * 复制数组
	 * @return
	 */
	public IArray dup() {
		return new ConstArray(data, size);
	}
	
	/**
	 * 写内容到流
	 * @param out 输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(1);
		out.writeInt(size);
		out.writeObject(data);
	}
	
	/**
	 * 从流中读内容
	 * @param in 输入流
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		in.readByte();
		size = in.readInt();
		data = in.readObject();
	}
	
	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeByte(1);
		out.writeInt(size);
		out.writeObject(data, true);
		return out.toByteArray();
	}
	
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		in.readByte();
		size = in.readInt();
		data = in.readObject(true);
	}
	
	/**
	 * 返回一个同类型的数组
	 * @param count
	 * @return
	 */
	public IArray newInstance(int count) {
		if (data instanceof Integer) {
			return new IntArray(count);
		} else if (data instanceof Long) {
			return new LongArray(count);
		} else if (data instanceof Double) {
			return new DoubleArray(count);
		} else if (data instanceof String) {
			return new StringArray(count);
		} else if (data instanceof Date) {
			return new DateArray(count);
		} else if (data instanceof Boolean) {
			return new BoolArray(count);
		} else {
			return new ObjectArray(count);
		}
	}

	/**
	 * 对数组成员求绝对值
	 * @return IArray 绝对值数组
	 */
	public IArray abs() {
		if (data == null) {
			return this;
		} else {
			return new ConstArray(Variant.abs(data), size);
		}
	}
	
	/**
	 * 对数组成员求负
	 * @return IArray 负值数组
	 */
	public IArray negate() {
		if (data == null) {
			return this;
		} else {
			return new ConstArray(Variant.negate(data), size);
		}
	}

	/**
	 * 对数组成员求非
	 * @return IArray 非值数组
	 */
	public IArray not() {
		Boolean b = Boolean.valueOf(Variant.isFalse(data));
		return new ConstArray(b, size);
	}

	/**
	 * 判断数组的成员是否都是数（可以包含null）
	 * @return true：都是数，false：含有非数的值
	 */
	public boolean isNumberArray() {
		return data == null || data instanceof Number;
	}

	/**
	 * 计算两个数组的相对应的成员的和
	 * @param array 右侧数组
	 * @return 和数组
	 */
	public IArray memberAdd(IArray array) {
		return array.memberAdd(data);
	}

	/**
	 * 计算数组的成员与指定常数的和
	 * @param value 常数
	 * @return 和数组
	 */
	public IArray memberAdd(Object value) {
		value = Variant.add(data, value);
		return new ConstArray(value, size);
	}

	/**
	 * 计算两个数组的相对应的成员的差
	 * @param array 右侧数组
	 * @return 差数组
	 */
	public IArray memberSubtract(IArray array) {
		if (array instanceof IntArray) {
			return memberSubtract((IntArray)array);
		} else if (array instanceof LongArray) {
			return memberSubtract((LongArray)array);
		} else if (array instanceof DoubleArray) {
			return memberSubtract((DoubleArray)array);
		} else if (array instanceof ConstArray) {
			Object value = array.get(1);
			value = Variant.subtract(data, value);
			return new ConstArray(value, size);
		} else if (array instanceof ObjectArray) {
			return memberSubtract((ObjectArray)array);
		} else if (array instanceof DateArray) {
			return memberSubtract((DateArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illSubtract"));
		}
	}
	
	private IArray memberSubtract(IntArray array) {
		int size = this.size;
		int []datas = array.getDatas();
		boolean []signs = array.getSigns();
		
		if (data instanceof Long) {
			long v = ((Long)data).longValue();
			long []resultDatas = new long[size + 1];
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v - datas[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = v;
					} else {
						resultDatas[i] = v - datas[i];
					}
				}
			}
			
			IArray result = new LongArray(resultDatas, null, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Double || data instanceof Float) {
			double v = ((Number)data).doubleValue();
			double []resultDatas = new double[size + 1];
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v - datas[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = v;
					} else {
						resultDatas[i] = v - datas[i];
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, null, size);
			result.setTemporary(true);
			return result;
		} else if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigDecimal decimal;
			if (data instanceof BigDecimal) {
				decimal = (BigDecimal)data;
			} else {
				decimal = new BigDecimal((BigInteger)data);
			}
			
			Object []resultDatas = new Object[size + 1];
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = decimal.subtract(new BigDecimal(datas[i]));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = decimal;
					} else {
						resultDatas[i] = decimal.subtract(new BigDecimal(datas[i]));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			int v = ((Number)data).intValue();
			if (array.isTemporary()) {
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						datas[i] = v - datas[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							datas[i] = v;
						} else {
							datas[i] = v - datas[i];
						}
					}
					
					array.setSigns(null);
				}
				
				return array;
			} else {
				int []resultDatas = new int[size + 1];
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = v - datas[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							resultDatas[i] = v;
						} else {
							resultDatas[i] = v - datas[i];
						}
					}
				}
				
				IArray result = new IntArray(resultDatas, null, size);
				result.setTemporary(true);
				return result;
			}
		} else if (data instanceof Date) {
			Date date = (Date)data;
			long time = date.getTime();
			Calendar calendar = Calendar.getInstance();
			Object []resultDatas = new Object[size + 1];
			
			for (int i = 1; i <= size; ++i) {
				if (signs == null || !signs[i]) {
					calendar.setTimeInMillis(time);
					calendar.add(Calendar.DATE, -datas[i]);
					Date resultDate = (Date)date.clone();
					resultDate.setTime(calendar.getTimeInMillis());
					resultDatas[i] = resultDate;
				} else {
					resultDatas[i] = date;
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data == null) {
			return array.negate();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illSubtract"));
		}
	}
	
	private IArray memberSubtract(LongArray array) {
		int size = this.size;
		long []datas = array.getDatas();
		boolean []signs = array.getSigns();
		
		if (data instanceof Double || data instanceof Float) {
			double v = ((Number)data).doubleValue();
			double []resultDatas = new double[size + 1];
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v - datas[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = v;
					} else {
						resultDatas[i] = v - datas[i];
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, null, size);
			result.setTemporary(true);
			return result;
		} else if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigDecimal decimal;
			if (data instanceof BigDecimal) {
				decimal = (BigDecimal)data;
			} else {
				decimal = new BigDecimal((BigInteger)data);
			}
			
			Object []resultDatas = new Object[size + 1];
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = decimal.subtract(new BigDecimal(datas[i]));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = decimal;
					} else {
						resultDatas[i] = decimal.subtract(new BigDecimal(datas[i]));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			long v = ((Number)data).longValue();
			if (array.isTemporary()) {
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						datas[i] = v - datas[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							datas[i] = v;
						} else {
							datas[i] = v - datas[i];
						}
					}
					
					array.setSigns(null);
				}
				
				return array;
			} else {
				long []resultDatas = new long[size + 1];
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = v - datas[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							resultDatas[i] = v;
						} else {
							resultDatas[i] = v - datas[i];
						}
					}
				}
				
				IArray result = new LongArray(resultDatas, null, size);
				result.setTemporary(true);
				return result;
			}
		} else if (data instanceof Date) {
			Date date = (Date)data;
			long time = date.getTime();
			Calendar calendar = Calendar.getInstance();
			Object []resultDatas = new Object[size + 1];
			
			for (int i = 1; i <= size; ++i) {
				if (signs == null || !signs[i]) {
					calendar.setTimeInMillis(time);
					calendar.add(Calendar.DATE, -(int)datas[i]);
					Date resultDate = (Date)date.clone();
					resultDate.setTime(calendar.getTimeInMillis());
					resultDatas[i] = resultDate;
				} else {
					resultDatas[i] = date;
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data == null) {
			return array.negate();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illSubtract"));
		}
	}

	private IArray memberSubtract(DoubleArray array) {
		int size = this.size;
		double []datas = array.getDatas();
		boolean []signs = array.getSigns();
		
		if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigDecimal decimal;
			if (data instanceof BigDecimal) {
				decimal = (BigDecimal)data;
			} else {
				decimal = new BigDecimal((BigInteger)data);
			}
			
			Object []resultDatas = new Object[size + 1];
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = decimal.subtract(new BigDecimal(datas[i]));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = decimal;
					} else {
						resultDatas[i] = decimal.subtract(new BigDecimal(datas[i]));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			double v = ((Number)data).doubleValue();
			if (array.isTemporary()) {
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						datas[i] = v - datas[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							datas[i] = v;
						} else {
							datas[i] = v - datas[i];
						}
					}
					
					array.setSigns(null);
				}
				
				return array;
			} else {
				double []resultDatas = new double[size + 1];
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = v - datas[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							resultDatas[i] = v;
						} else {
							resultDatas[i] = v - datas[i];
						}
					}
				}
				
				IArray result = new DoubleArray(resultDatas, null, size);
				result.setTemporary(true);
				return result;
			}
		} else if (data instanceof Date) {
			Date date = (Date)data;
			long time = date.getTime();
			Calendar calendar = Calendar.getInstance();
			Object []resultDatas = new Object[size + 1];
			
			for (int i = 1; i <= size; ++i) {
				if (signs == null || !signs[i]) {
					calendar.setTimeInMillis(time);
					calendar.add(Calendar.DATE, -(int)datas[i]);
					Date resultDate = (Date)date.clone();
					resultDate.setTime(calendar.getTimeInMillis());
					resultDatas[i] = resultDate;
				} else {
					resultDatas[i] = date;
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data == null) {
			return array.negate();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illSubtract"));
		}
	}
	
	private IArray memberSubtract(DateArray array) {
		if (data instanceof Date) {
			int size = this.size;
			Date date = (Date)data;
			Date []datas = array.getDatas();
			
			long []resultDatas = new long[size + 1];
			boolean []resultSigns = null;
			for (int i = 1; i <= size; ++i) {
				if (datas[i] == null) {
					if (resultSigns == null) {
						resultSigns = new boolean[size + 1];
					}
					
					resultSigns[i] = true;
				} else {
					resultDatas[i] = Variant.dayInterval(datas[i], date);
				}
			}
			
			IArray result = new LongArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illSubtract"));
		}
	}

	private ObjectArray memberSubtract(ObjectArray array) {
		Object data = this.data;
		int size = this.size;
		Object []datas = array.getDatas();
		
		if (array.isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				datas[i] = Variant.subtract(data, datas[i]);
			}
			
			return array;
		} else {
			Object []resultDatas = new Object[size + 1];
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = Variant.subtract(data, datas[i]);
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}

	/**
	 * 计算两个数组的相对应的成员的积
	 * @param array 右侧数组
	 * @return 积数组
	 */
	public IArray memberMultiply(IArray array) {
		return array.memberMultiply(data);
	}

	/**
	 * 计算数组的成员与指定常数的积
	 * @param value 常数
	 * @return 积数组
	 */
	public IArray memberMultiply(Object value) {
		value = Variant.multiply(data, value);
		return new ConstArray(value, size);
	}

	/**
	 * 计算两个数组的相对应的成员的除
	 * @param array 右侧数组
	 * @return 商数组
	 */
	public IArray memberDivide(IArray array) {
		if (array instanceof IntArray) {
			return memberDivide((IntArray)array);
		} else if (array instanceof LongArray) {
			return memberDivide((LongArray)array);
		} else if (array instanceof DoubleArray) {
			return memberDivide((DoubleArray)array);
		} else if (array instanceof ConstArray) {
			Object value = array.get(1);
			value = Variant.divide(data, value);
			return new ConstArray(value, size);
		} else if (array instanceof ObjectArray) {
			return memberDivide((ObjectArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}
	
	private IArray memberDivide(IntArray array) {
		Object data = this.data;
		int size = this.size;
		int []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigDecimal decimal;
			if (data instanceof BigDecimal) {
				decimal = (BigDecimal)data;
			} else {
				decimal = new BigDecimal((BigInteger)data);
			}
			
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = decimal.divide(new BigDecimal(d2[i]), Variant.Divide_Scale, Variant.Divide_Round);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = decimal.divide(new BigDecimal(d2[i]), Variant.Divide_Scale, Variant.Divide_Round);
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			double v = ((Number)data).doubleValue();
			double []resultDatas = new double[size + 1];
			boolean []resultSigns = null;
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v / (double)d2[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = v / (double)d2[i];
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof String) {
			String str = (String)data;
			Object []resultDatas = new Object[size + 1];
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = str + d2[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = str + d2[i];
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;			
		} else if (data == null) {
			return new ConstArray(null, size);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") + 
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}

	private IArray memberDivide(LongArray array) {
		Object data = this.data;
		int size = this.size;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigDecimal decimal;
			if (data instanceof BigDecimal) {
				decimal = (BigDecimal)data;
			} else {
				decimal = new BigDecimal((BigInteger)data);
			}
			
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = decimal.divide(new BigDecimal(d2[i]), Variant.Divide_Scale, Variant.Divide_Round);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = decimal.divide(new BigDecimal(d2[i]), Variant.Divide_Scale, Variant.Divide_Round);
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			double v = ((Number)data).doubleValue();
			double []resultDatas = new double[size + 1];
			boolean []resultSigns = null;
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v / (double)d2[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = v / (double)d2[i];
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof String) {
			String str = (String)data;
			Object []resultDatas = new Object[size + 1];
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = str + d2[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = str + d2[i];
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;			
		} else if (data == null) {
			return new ConstArray(null, size);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") + 
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}

	private IArray memberDivide(DoubleArray array) {
		Object data = this.data;
		int size = this.size;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigDecimal decimal;
			if (data instanceof BigDecimal) {
				decimal = (BigDecimal)data;
			} else {
				decimal = new BigDecimal((BigInteger)data);
			}
			
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = decimal.divide(new BigDecimal(d2[i]), Variant.Divide_Scale, Variant.Divide_Round);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = decimal.divide(new BigDecimal(d2[i]), Variant.Divide_Scale, Variant.Divide_Round);
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			double v = ((Number)data).doubleValue();
			if (array.isTemporary()) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						d2[i] = v / d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!s2[i]) {
							d2[i] = v / d2[i];
						}
					}
				}
				
				return array;
			} else {
				double []resultDatas = new double[size + 1];
				boolean []resultSigns = null;
				
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = v / d2[i];
					}
				} else {
					resultSigns = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultSigns[i] = true;
						} else {
							resultDatas[i] = v / d2[i];
						}
					}
				}
				
				IArray result = new DoubleArray(resultDatas, resultSigns, size);
				result.setTemporary(true);
				return result;
			}
		} else if (data instanceof String) {
			String str = (String)data;
			Object []resultDatas = new Object[size + 1];
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = str + d2[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = str + d2[i];
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;			
		} else if (data == null) {
			return new ConstArray(null, size);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") + 
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}

	private ObjectArray memberDivide(ObjectArray array) {
		Object data = this.data;
		int size = this.size;
		Object []datas = array.getDatas();
		
		if (array.isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				datas[i] = Variant.divide(data, datas[i]);
			}
			
			return array;
		} else {
			Object []resultDatas = new Object[size + 1];
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = Variant.divide(data, datas[i]);
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}

	/**
	 * 计算两个数组的相对应的数成员取余或序列成员异或列
	 * @param array 右侧数组
	 * @return 余数数组或序列异或列数组
	 */
	public IArray memberMod(IArray array) {
		if (array instanceof IntArray) {
			return memberMod((IntArray)array);
		} else if (array instanceof LongArray) {
			return memberMod((LongArray)array);
		} else if (array instanceof DoubleArray) {
			return memberMod((DoubleArray)array);
		} else if (array instanceof ConstArray) {
			Object value = ArrayUtil.mod(data, array.get(1));
			return new ConstArray(value, size);
		} else if (array instanceof ObjectArray) {
			return memberMod((ObjectArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illMod"));
		}
	}
	
	private IArray memberMod(IntArray array) {
		Object data = this.data;
		int size = this.size;
		int []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (data instanceof Long) {
			long v = ((Number)data).longValue();
			long []resultDatas = new long[size + 1];
			boolean []resultSigns = null;
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v % d2[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = v % d2[i];
					}
				}
			}
			
			IArray result = new LongArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Double || data instanceof Float) {
			double v = ((Number)data).doubleValue();
			double []resultDatas = new double[size + 1];
			boolean []resultSigns = null;
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v % d2[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = v % d2[i];
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigInteger v;
			if (data instanceof BigDecimal) {
				v = ((BigDecimal)data).toBigInteger();
			} else {
				v = (BigInteger)data;
			}
			
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(v.mod(BigInteger.valueOf(d2[i])));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = new BigDecimal(v.mod(BigInteger.valueOf(d2[i])));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			int v = ((Number)data).intValue();
			if (array.isTemporary()) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						d2[i] = v % d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!s2[i]) {
							d2[i] = v % d2[i];
						}
					}
				}
				
				return array;
			} else {
				int []resultDatas = new int[size + 1];
				boolean []resultSigns = null;
				
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = v % d2[i];
					}
				} else {
					resultSigns = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultSigns[i] = true;
						} else {
							resultDatas[i] = v % d2[i];
						}
					}
				}
				
				IArray result = new IntArray(resultDatas, resultSigns, size);
				result.setTemporary(true);
				return result;
			}
		} else if (data == null) {
			return new ConstArray(null, size);
		} else if (data instanceof Sequence) {
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = ArrayUtil.mod(data, d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultDatas[i] = data;
					} else {
						resultDatas[i] = ArrayUtil.mod(data, d2[i]);
					}
				}				
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") + 
					array.getDataType() + mm.getMessage("Variant2.illMod"));
		}
	}
	
	private IArray memberMod(LongArray array) {
		Object data = this.data;
		int size = this.size;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (data instanceof Double || data instanceof Float) {
			double v = ((Number)data).doubleValue();
			double []resultDatas = new double[size + 1];
			boolean []resultSigns = null;
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v % d2[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = v % d2[i];
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigInteger v;
			if (data instanceof BigDecimal) {
				v = ((BigDecimal)data).toBigInteger();
			} else {
				v = (BigInteger)data;
			}
			
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(v.mod(BigInteger.valueOf(d2[i])));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = new BigDecimal(v.mod(BigInteger.valueOf(d2[i])));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			long v = ((Number)data).longValue();
			if (array.isTemporary()) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						d2[i] = v % d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!s2[i]) {
							d2[i] = v % d2[i];
						}
					}
				}
				
				return array;
			} else {
				long []resultDatas = new long[size + 1];
				boolean []resultSigns = null;
				
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = v % d2[i];
					}
				} else {
					resultSigns = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultSigns[i] = true;
						} else {
							resultDatas[i] = v % d2[i];
						}
					}
				}
				
				IArray result = new LongArray(resultDatas, resultSigns, size);
				result.setTemporary(true);
				return result;
			}
		} else if (data == null) {
			return new ConstArray(null, size);
		} else if (data instanceof Sequence) {
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = ArrayUtil.mod(data, d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultDatas[i] = data;
					} else {
						resultDatas[i] = ArrayUtil.mod(data, d2[i]);
					}
				}				
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") + 
					array.getDataType() + mm.getMessage("Variant2.illMod"));
		}
	}
	
	private IArray memberMod(DoubleArray array) {
		Object data = this.data;
		int size = this.size;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigInteger v;
			if (data instanceof BigDecimal) {
				v = ((BigDecimal)data).toBigInteger();
			} else {
				v = (BigInteger)data;
			}
			
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(v.mod(BigInteger.valueOf((long)d2[i])));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = new BigDecimal(v.mod(BigInteger.valueOf((long)d2[i])));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			double v = ((Number)data).longValue();
			if (array.isTemporary()) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						d2[i] = v % d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!s2[i]) {
							d2[i] = v % d2[i];
						}
					}
				}
				
				return array;
			} else {
				double []resultDatas = new double[size + 1];
				boolean []resultSigns = null;
				
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = v % d2[i];
					}
				} else {
					resultSigns = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultSigns[i] = true;
						} else {
							resultDatas[i] = v % d2[i];
						}
					}
				}
				
				IArray result = new DoubleArray(resultDatas, resultSigns, size);
				result.setTemporary(true);
				return result;
			}
		} else if (data == null) {
			return new ConstArray(null, size);
		} else if (data instanceof Sequence) {
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = ArrayUtil.mod(data, d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultDatas[i] = data;
					} else {
						resultDatas[i] = ArrayUtil.mod(data, d2[i]);
					}
				}				
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") + 
					array.getDataType() + mm.getMessage("Variant2.illMod"));
		}
	}
	
	private IArray memberMod(ObjectArray array) {
		Object data = this.data;
		int size = this.size;
		Object []datas = array.getDatas();
		
		if (array.isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				datas[i] = ArrayUtil.mod(data, datas[i]);
			}
			
			return array;
		} else {
			Object []resultDatas = new Object[size + 1];
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = ArrayUtil.mod(data, datas[i]);
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	/**
	 * 计算两个数组的数成员整除或序列成员差集
	 * @param array 右侧数组
	 * @return 整除值数组或序列差集数组
	 */
	public IArray memberIntDivide(IArray array) {
		if (array instanceof IntArray) {
			return memberIntDivide((IntArray)array);
		} else if (array instanceof LongArray) {
			return memberIntDivide((LongArray)array);
		} else if (array instanceof DoubleArray) {
			return memberIntDivide((DoubleArray)array);
		} else if (array instanceof ConstArray) {
			Object value = ArrayUtil.intDivide(data, array.get(1));
			return new ConstArray(value, size);
		} else if (array instanceof ObjectArray) {
			return memberIntDivide((ObjectArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}
	
	private IArray memberIntDivide(IntArray array) {
		Object data = this.data;
		int size = this.size;
		int []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (data instanceof Long || data instanceof Double || data instanceof Float) {
			long v = ((Number)data).longValue();
			long []resultDatas = new long[size + 1];
			boolean []resultSigns = null;
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v / d2[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = v / d2[i];
					}
				}
			}
			
			IArray result = new LongArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigInteger v;
			if (data instanceof BigDecimal) {
				v = ((BigDecimal)data).toBigInteger();
			} else {
				v = (BigInteger)data;
			}
			
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(v.divide(BigInteger.valueOf(d2[i])));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = new BigDecimal(v.divide(BigInteger.valueOf(d2[i])));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			int v = ((Number)data).intValue();
			if (array.isTemporary()) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						d2[i] = v / d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!s2[i]) {
							d2[i] = v / d2[i];
						}
					}
				}
				
				return array;
			} else {
				int []resultDatas = new int[size + 1];
				boolean []resultSigns = null;
				
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = v / d2[i];
					}
				} else {
					resultSigns = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultSigns[i] = true;
						} else {
							resultDatas[i] = v / d2[i];
						}
					}
				}
				
				IArray result = new IntArray(resultDatas, resultSigns, size);
				result.setTemporary(true);
				return result;
			}
		} else if (data == null) {
			return new ConstArray(null, size);
		} else if (data instanceof Sequence) {
			Sequence seq1 = (Sequence)data;
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					Sequence seq2 = new Sequence(1);
					seq2.add(d2[i]);
					resultDatas[i] = seq1.diff(seq2, false);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultDatas[i] = seq1;
					} else {
						Sequence seq2 = new Sequence(1);
						seq2.add(d2[i]);
						resultDatas[i] = seq1.diff(seq2, false);
					}
				}				
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") + 
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}
	
	private IArray memberIntDivide(LongArray array) {
		Object data = this.data;
		int size = this.size;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigInteger v;
			if (data instanceof BigDecimal) {
				v = ((BigDecimal)data).toBigInteger();
			} else {
				v = (BigInteger)data;
			}
			
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(v.divide(BigInteger.valueOf(d2[i])));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = new BigDecimal(v.divide(BigInteger.valueOf(d2[i])));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			long v = ((Number)data).longValue();
			if (array.isTemporary()) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						d2[i] = v / d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!s2[i]) {
							d2[i] = v / d2[i];
						}
					}
				}
				
				return array;
			} else {
				long []resultDatas = new long[size + 1];
				boolean []resultSigns = null;
				
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = v / d2[i];
					}
				} else {
					resultSigns = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultSigns[i] = true;
						} else {
							resultDatas[i] = v / d2[i];
						}
					}
				}
				
				IArray result = new LongArray(resultDatas, resultSigns, size);
				result.setTemporary(true);
				return result;
			}
		} else if (data == null) {
			return new ConstArray(null, size);
		} else if (data instanceof Sequence) {
			Sequence seq1 = (Sequence)data;
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					Sequence seq2 = new Sequence(1);
					seq2.add(d2[i]);
					resultDatas[i] = seq1.diff(seq2, false);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultDatas[i] = seq1;
					} else {
						Sequence seq2 = new Sequence(1);
						seq2.add(d2[i]);
						resultDatas[i] = seq1.diff(seq2, false);
					}
				}				
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") + 
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}
	
	private IArray memberIntDivide(DoubleArray array) {
		Object data = this.data;
		int size = this.size;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if ((data instanceof BigDecimal) || (data instanceof BigInteger)) {
			BigInteger v;
			if (data instanceof BigDecimal) {
				v = ((BigDecimal)data).toBigInteger();
			} else {
				v = (BigInteger)data;
			}
			
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(v.divide(BigInteger.valueOf((long)d2[i])));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						resultDatas[i] = new BigDecimal(v.divide(BigInteger.valueOf((long)d2[i])));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (data instanceof Number) {
			long v = ((Number)data).longValue();
			long []resultDatas = new long[size + 1];
			boolean []resultSigns = null;
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v / (long)d2[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = v / (long)d2[i];
					}
				}
			}
			
			IArray result = new LongArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if (data == null) {
			return new ConstArray(null, size);
		} else if (data instanceof Sequence) {
			Sequence seq1 = (Sequence)data;
			Object []resultDatas = new Object[size + 1];
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					Sequence seq2 = new Sequence(1);
					seq2.add(d2[i]);
					resultDatas[i] = seq1.diff(seq2, false);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultDatas[i] = seq1;
					} else {
						Sequence seq2 = new Sequence(1);
						seq2.add(d2[i]);
						resultDatas[i] = seq1.diff(seq2, false);
					}
				}				
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(Variant.getDataType(data) + mm.getMessage("Variant2.with") + 
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}
	
	private IArray memberIntDivide(ObjectArray array) {
		Object data = this.data;
		int size = this.size;
		Object []datas = array.getDatas();
		
		if (array.isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				datas[i] = ArrayUtil.intDivide(data, datas[i]);
			}
			
			return array;
		} else {
			Object []resultDatas = new Object[size + 1];
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = ArrayUtil.intDivide(data, datas[i]);
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}

	/**
	 * 计算两个数组的相对应的成员的关系运算
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @return 关系运算结果数组
	 */
	public BoolArray calcRelation(IArray array, int relation) {
		return array.calcRelation(data, Relation.getInverseRelation(relation));
	}
	
	/**
	 * 计算两个数组的相对应的成员的关系运算
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @return 关系运算结果数组
	 */
	public BoolArray calcRelation(Object value, int relation) {
		boolean result;
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			result = Variant.compare(data, value, true) == 0;
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			result = Variant.compare(data, value, true) > 0;
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			result = Variant.compare(data, value, true) >= 0;
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			result = Variant.compare(data, value, true) < 0;
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			result = Variant.compare(data, value, true) <= 0;
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			result = Variant.compare(data, value, true) != 0;
		} else if (relation == Relation.AND) {
			result = Variant.isTrue(data) && Variant.isTrue(value);
		} else { // Relation.OR
			result = Variant.isTrue(data) || Variant.isTrue(value);
		}
		
		return new BoolArray(result, size);
	}

	/**
	 * 比较两个数组的大小
	 * @param array 右侧数组
	 * @return 1：当前数组大，0：两个数组相等，-1：当前数组小
	 */
	public int compareTo(IArray array) {
		if (array instanceof ConstArray) {
			int cmp = Variant.compare(data, array.get(1), true);
			if (cmp != 0) {
				return cmp;
			} else if (size == array.size()) {
				return 0;
			} else if (size < array.size()) {
				return -1;
			} else {
				return 1;
			}
		} else {
			return -array.compareTo(this);
		}
	}
	
	/**
	 * 计算数组的2个成员的比较值
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	public int memberCompare(int index1, int index2) {
		return 0;
	}
	
	/**
	 * 判断数组的两个成员是否相等
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	public boolean isMemberEquals(int index1, int index2) {
		return true;
	}
	
	/**
	 * 判断两个数组的指定元素是否相同
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要比较的数组
	 * @param index 要比较的数组的元素的索引
	 * @return true：相同，false：不相同
	 */
	public boolean isEquals(int curIndex, IArray array, int index) {
		return Variant.isEquals(data, array.get(index));
	}
	
	/**
	 * 判断数组的指定元素是否与给定值相等
	 * @param curIndex 数组元素索引，从1开始计数
	 * @param value 值
	 * @return true：相等，false：不相等
	 */
	public boolean isEquals(int curIndex, Object value) {
		return Variant.isEquals(data, value);
	}
	
	/**
	 * 判断两个数组的指定元素的大小
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要比较的数组
	 * @param index 要比较的数组的元素的索引
	 * @return 小于：小于0，等于：0，大于：大于0
	 */
	public int compareTo(int curIndex, IArray array, int index) {
		return Variant.compare(data, array.get(index), true);
	}
	
	/**
	 * 比较数组的指定元素与给定值的大小
	 * @param curIndex 当前数组的元素的索引
	 * @param value 要比较的值
	 * @return
	 */
	public int compareTo(int curIndex, Object value) {
		return Variant.compare(data, value, true);
	}
	
	/**
	 * 取指定成员的哈希值
	 * @param index 成员索引，从1开始计数
	 * @return 指定成员的哈希值
	 */
	public int hashCode(int index) {
		if (data != null) {
			return data.hashCode();
		} else {
			return 0;
		}
	}
	
	/**
	 * 求成员和
	 * @return
	 */
	public Object sum() {
		if (data instanceof Number) {
			return Variant.multiply(data, size);
		} else {
			return null;
		}
	}
	
	/**
	 * 求平均值
	 * @return
	 */
	public Object average() {
		if (data instanceof Number) {
			if (data instanceof BigDecimal || data instanceof Double) {
				return data;
			} else if (data instanceof BigInteger) {
				return new BigDecimal((BigInteger)data);
			} else {
				return ((Number)data).doubleValue();
			}
		} else {
			return null;
		}
	}

	/**
	 * 得到最大的成员
	 * @return
	 */
	public Object max() {
		return data;
	}

	/**
	 * 得到最小的成员
	 * @return
	 */
	public Object min() {
		return data;
	}

	/**
	 * 计算两个数组的相对应的成员的关系运算，只计算result为真的行
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	public void calcRelations(IArray array, int relation, BoolArray result, boolean isAnd) {
		array.calcRelations(data, Relation.getInverseRelation(relation), result, isAnd);
	}
	
	/**
	 * 计算两个数组的相对应的成员的关系运算，只计算result为真的行
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	public void calcRelations(Object value, int relation, BoolArray result, boolean isAnd) {
		boolean []resultDatas = result.getDatas();
		boolean b;
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			b = Variant.compare(data, value, true) == 0;
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			b = Variant.compare(data, value, true) > 0;
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			b = Variant.compare(data, value, true) >= 0;
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			b = Variant.compare(data, value, true) < 0;
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			b = Variant.compare(data, value, true) <= 0;
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			b = Variant.compare(data, value, true) != 0;
		} else {
			throw new RuntimeException();
		}
		
		if (isAnd && !b) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = false;
			}
		} else if (!isAnd && b) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = true;
			}
		}
	}
	
	/**
	 * 计算两个数组的相对应的成员的按位与
	 * @param array 右侧数组
	 * @return 按位与结果数组
	 */
	public IArray bitwiseAnd(IArray array) {
		if (array instanceof ConstArray) {
			Object value = And.and(data, array.get(1));
			return new ConstArray(value, size);
		} else {
			return array.bitwiseAnd(this);
		}
	}

	/**
	 * 取出标识数组取值为真的行对应的数据，组成新数组
	 * @param signArray 标识数组
	 * @return IArray
	 */
	public IArray select(IArray signArray) {
		int size = signArray.size();
		int count = 0;
		
		if (signArray instanceof BoolArray) {
			BoolArray array = (BoolArray)signArray;
			boolean []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (d2[i]) {
						count++;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i] && d2[i]) {
						count++;
					}
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (signArray.isTrue(i)) {
					count++;
				}
			}
		}
		
		return new ConstArray(data, count);
	}
	
	/**
	 * 取某一区段标识数组取值为真的行组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @param signArray 标识数组
	 * @return IArray
	 */
	public IArray select(int start, int end, IArray signArray) {
		int count = 0;
		if (signArray instanceof BoolArray) {
			BoolArray array = (BoolArray)signArray;
			boolean []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			if (s2 == null) {
				for (int i = start; i < end; ++i) {
					if (d2[i]) {
						count++;
					}
				}
			} else {
				for (int i = start; i < end; ++i) {
					if (!s2[i] && d2[i]) {
						count++;
					}
				}
			}
		} else {
			for (int i = start; i < end; ++i) {
				if (signArray.isTrue(i)) {
					count++;
				}
			}
		}
		
		return new ConstArray(data, count);
	}
	
	/**
	 * 把array的指定元素加到当前数组的指定元素上
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要相加的数组
	 * @param index 要相加的数组的元素的索引
	 * @return IArray
	 */
	public IArray memberAdd(int curIndex, IArray array, int index) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illAdd"));
	}	

	/**
	 * 把成员转成对象数组返回
	 * @return 对象数组
	 */
	public Object[] toArray() {
		int size = this.size;
		Object data = this.data;
		
		Object []result = new Object[size];
		for (int i = 0; i < size; ++i) {
			result[i] = data;
		}
		
		return result;
	}
	
	/**
	 * 把成员填到指定的数组
	 * @param result 用于存放成员的数组
	 */
	public void toArray(Object []result) {
		int size = this.size;
		Object data = this.data;
		
		for (int i = 0; i < size; ++i) {
			result[i] = data;
		}
	}

	/**
	 * 把数组从指定位置拆成两个数组
	 * @param pos 位置，包含
	 * @return 返回后半部分元素构成的数组
	 */
	public IArray split(int pos) {
		int resultSize = size - pos + 1;
		this.size = pos - 1;
		return new ConstArray(data, resultSize);
	}
	
	/**
	 * 把指定区间元素分离出来组成新数组
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 * @return
	 */
	public IArray split(int from, int to) {
		int resultSize = to - from + 1;
		this.size -= resultSize;
		return new ConstArray(data, resultSize);
	}
	
	/**
	 * 对数组的元素进行排序
	 */
	public void sort() {
	}
	
	/**
	 * 对数组的元素进行排序
	 * @param comparator 比较器
	 */
	public void sort(Comparator<Object> comparator) {
	}
	
	/**
	 * 返回数组中是否含有记录
	 * @return boolean
	 */
	public boolean hasRecord() {
		return data instanceof BaseRecord;
	}
	
	/**
	 * 返回是否是（纯）排列
	 * @param isPure true：检查是否是纯排列
	 * @return boolean true：是，false：不是
	 */
	public boolean isPmt(boolean isPure) {
		return data instanceof BaseRecord;
	}
	
	/**
	 * 返回数组的反转数组
	 * @return IArray
	 */
	public IArray rvs() {
		return new ConstArray(data, size);
	}

	/**
	 * 对数组元素从小到大做排名，取前count名的位置
	 * @param count 如果count小于0则取后|count|名的位置
	 * @param isAll count为正负1时，如果isAll取值为true则取所有排名第一的元素的位置，否则只取一个
	 * @param isLast 是否从后开始找
	 * @param ignoreNull
	 * @return IntArray
	 */
	public IntArray ptop(int count, boolean isAll, boolean isLast, boolean ignoreNull) {
		if (size == 0 || (data == null && ignoreNull)) {
			return new IntArray(0);
		}
		
		if (count == 1 || count == -1) {
			// 取最小值的位置
			if (isAll) {
				return new IntArray(1, size);
			} else if (isLast) {
				IntArray result = new IntArray(1);
				result.pushInt(size);
				return result;
			} else {
				IntArray result = new IntArray(1);
				result.pushInt(1);
				return result;
			}
		} else if (count > 1) {
			// 取最小的count个元素的位置
			return new IntArray(1, count);
		} else if (count < -1) {
			return new IntArray(1, -count);
		} else {
			return new IntArray(1);
		}
	}
	
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * 把当前数组转成对象数组，如果当前数组是对象数组则返回数组本身
	 * @return ObjectArray
	 */
	public ObjectArray toObjectArray() {
		int size = this.size;
		Object data = this.data;
		Object []resultDatas = new Object[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			resultDatas[i] = data;
		}
		
		return new ObjectArray(resultDatas, size);
	}
	
	/**
	 * 把对象数组转成纯类型数组，不能转则抛出异常
	 * @return IArray
	 */
	public IArray toPureArray() {
		int size = this.size;
		Object data = this.data;
		
		if (data instanceof String) {
			String str = (String)data;
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = str;
			}
			
			return new StringArray(resultDatas, size);
		} else if (data instanceof Date) {
			Date date = (Date)data;
			Date []resultDatas = new Date[size + 1];
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = date;
			}
			
			return new DateArray(resultDatas, size);
		} else if (data instanceof Double) {
			double d = (Double)data;
			double []resultDatas = new double[size + 1];
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d;
			}
			
			return new DoubleArray(resultDatas, null, size);
		} else if (data instanceof Long) {
			long d = (Long)data;
			long []resultDatas = new long[size + 1];
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d;
			}
			
			return new LongArray(resultDatas, null, size);
		} else if (data instanceof Integer) {
			int n = (Integer)data;
			int []resultDatas = new int[size + 1];
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = n;
			}
			
			return new IntArray(resultDatas, null, size);
		} else if (data instanceof Boolean) {
			return new BoolArray(((Boolean)data).booleanValue(), size);
		} else if (data == null) {
			Object []resultDatas = new Object[size + 1];
			return new ObjectArray(resultDatas, size);
		} else {
			Object []resultDatas = new Object[size + 1];
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = data;
			}
			
			return new ObjectArray(resultDatas, size);
		}
	}
	
	/**
	 * 保留数组数据用于生成序列或序表
	 * @param refOrigin 引用源列，不复制数据
	 * @return
	 */
	public IArray reserve(boolean refOrigin) {
		return toPureArray();
	}
	
	/**
	 * 根据条件从两个数组选出成员组成新数组，从当前数组选出标志为true的，从other数组选出标志为false的
	 * @param signArray 标志数组
	 * @param other 另一个数组
	 * @return IArray
	 */
	public IArray combine(IArray signArray, IArray other) {
		if (other instanceof ConstArray) {
			return combine(signArray, ((ConstArray)other).getData());
		} else {
			return other.combine(signArray.isFalse(), data);
		}
	}

	/**
	 * 根据条件从当前数组选出标志为true的，标志为false的置成value
	 * @param signArray 标志数组
	 * @param other 值
	 * @return IArray
	 */
	public IArray combine(IArray signArray, Object value) {
		int size = this.size;
		Object data = this.data;
		Object []resultDatas = new Object[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			if (signArray.isTrue(i)) {
				resultDatas[i] = data;
			} else {
				resultDatas[i] = value;
			}
		}
		
		IArray result = new ObjectArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
}
