package com.scudata.array;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Comparator;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.expression.Relation;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.MultithreadUtil;
import com.scudata.util.Variant;

public class StringArray implements IArray {
	private static final long serialVersionUID = 1L;
	
	private static String TEMP = new String("");
	
	private String []datas;
	private int size;
	
	public StringArray() {
		datas = new String[DEFAULT_LEN];
	}
	
	public StringArray(int initialCapacity) {
		datas = new String[++initialCapacity];
	}
	
	public StringArray(String []datas, int size) {
		this.datas = datas;
		this.size = size;
	}

	public static int compare(String d1, String d2) {
		if (d1 == null) {
			return d2 == null ? 0 : -1;
		} else if (d2 == null) {
			return 1;
		} else {
			int cmp = d1.compareTo(d2);
			return cmp < 0 ? -1 : (cmp > 0 ? 1 : 0);
		}
	}
	
	private static int compare(String d1, Object d2) {
		if (d2 instanceof String) {
			if (d1 == null) {
				return -1;
			} else {
				int cmp = d1.compareTo((String)d2);
				return cmp < 0 ? -1 : (cmp > 0 ? 1 : 0);
			}
		} else if (d2 == null) {
			return d1 == null ? 0 : 1;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", d1, d2,
					mm.getMessage("DataType.String"), Variant.getDataType(d2)));
		}
	}
	
	public String[] getDatas() {
		return datas;
	}

	/**
	 * 取数组的类型串，用于错误信息提示
	 * @return 类型串
	 */
	public String getDataType() {
		MessageManager mm = EngineMessage.get();
		return mm.getMessage("DataType.String");
	}
	
	/**
	 * 复制数组
	 * @return
	 */
	public IArray dup() {
		int len = size + 1;
		String []newDatas = new String[len];
		System.arraycopy(datas, 0, newDatas, 0, len);
		return new StringArray(newDatas, size);
	}
	
	/**
	 * 写内容到流
	 * @param out 输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		int size = this.size;
		String []datas = this.datas;
		
		out.writeByte(1);
		out.writeInt(size);
		for (int i = 1; i <= size; ++i) {
			out.writeObject(datas[i]);
		}
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
		int len = size + 1;
		String []datas = this.datas = new String[len];
		
		for (int i = 1; i < len; ++i) {
			datas[i] = (String)in.readObject();
		}
	}
	
	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		int size = this.size;
		String []datas = this.datas;
		
		out.writeByte(1);
		out.writeInt(size);
		for (int i = 1; i <= size; ++i) {
			out.writeString(datas[i]);
		}

		return out.toByteArray();
	}
	
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		in.readByte();
		size = in.readInt();
		int len = size + 1;
		String []datas = this.datas = new String[len];
		
		for (int i = 1; i < len; ++i) {
			datas[i] = in.readString();
		}
	}
	
	/**
	 * 返回一个同类型的数组
	 * @param count
	 * @return
	 */
	public IArray newInstance(int count) {
		return new StringArray(count);
	}

	/**
	 * 追加元素，如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void add(Object o) {
		if (o instanceof String) {
			ensureCapacity(size + 1);
			datas[++size] = (String)o;
		} else if (o == null) {
			ensureCapacity(size + 1);
			datas[++size] = null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), Variant.getDataType(o)));
		}
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(IArray array) {
		int size2 = array.size();
		if (size2 == 0) {
		} else if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			ensureCapacity(size + size2);
			
			System.arraycopy(stringArray.datas, 1, datas, size + 1, size2);
			size += size2;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof String) {
				ensureCapacity(size + size2);
				String v = (String)obj;
				String []datas = this.datas;
				
				for (int i = 0; i < size2; ++i) {
					datas[++size] = v;
				}
			} else if (obj == null) {
				ensureCapacity(size + size2);
				String []datas = this.datas;
				
				for (int i = 0; i < size2; ++i) {
					datas[++size] = null;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.String"), array.getDataType()));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), array.getDataType()));
		}
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param count 元素个数
	 */
	public void addAll(IArray array, int count) {
		if (count == 0) {
		} else if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			ensureCapacity(size + count);
			
			System.arraycopy(stringArray.datas, 1, datas, size + 1, count);
			size += count;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof String) {
				ensureCapacity(size + count);
				String v = (String)obj;
				String []datas = this.datas;
				
				for (int i = 0; i < count; ++i) {
					datas[++size] = v;
				}
			} else if (obj == null) {
				ensureCapacity(size + count);
				String []datas = this.datas;
				
				for (int i = 0; i < count; ++i) {
					datas[++size] = null;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.String"), array.getDataType()));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), array.getDataType()));
		}
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param index 要加入的数据的起始位置
	 * @param count 数量
	 */
	public void addAll(IArray array, int index, int count) {
		if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			ensureCapacity(size + count);
			
			System.arraycopy(stringArray.datas, index, datas, size + 1, count);
			size += count;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof String) {
				ensureCapacity(size + count);
				String v = (String)obj;
				String []datas = this.datas;
				
				for (int i = 0; i < count; ++i) {
					datas[++size] = v;
				}
			} else if (obj == null) {
				ensureCapacity(size + count);
				String []datas = this.datas;
				
				for (int i = 0; i < count; ++i) {
					datas[++size] = null;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.String"), array.getDataType()));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), array.getDataType()));
		}
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(Object []array) {
		for (Object obj : array) {
			if (obj != null && !(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.String"), Variant.getDataType(obj)));
			}
		}
		
		int size2 = array.length;
		ensureCapacity(size + size2);
		String []datas = this.datas;
		
		for (int i = 0; i < size2; ++i) {
			datas[++size] = (String)array[i];
		}
	}

	/**
	 * 插入元素，如果类型不兼容则抛出异常
	 * @param index 插入位置，从1开始计数
	 * @param o 元素值
	 */
	public void insert(int index, Object o) {
		if (o instanceof String) {
			ensureCapacity(size + 1);
			
			size++;
			System.arraycopy(datas, index, datas, index + 1, size - index);
			datas[index] = (String)o;
		} else if (o == null) {
			ensureCapacity(size + 1);
			
			size++;
			System.arraycopy(datas, index, datas, index + 1, size - index);
			datas[index] = null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), Variant.getDataType(o)));
		}
	}

	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, IArray array) {
		if (array instanceof StringArray) {
			int numNew = array.size();
			StringArray stringArray = (StringArray)array;
			ensureCapacity(size + numNew);
			
			System.arraycopy(datas, pos, datas, pos + numNew, size - pos + 1);
			System.arraycopy(stringArray.datas, 1, datas, pos, numNew);
			
			size += numNew;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), array.getDataType()));
		}
	}
	
	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, Object []array) {
		for (Object obj : array) {
			if (obj != null && !(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.String"), Variant.getDataType(obj)));
			}
		}
		
		int numNew = array.length;
		ensureCapacity(size + numNew);
		
		System.arraycopy(datas, pos, datas, pos + numNew, size - pos + 1);
		System.arraycopy(array, 0, datas, pos, numNew);
		size += numNew;
	}

	public void push(String str) {
		datas[++size] = str;
	}

	public void pushString(String str) {
		datas[++size] = str;
	}

	/**
	 * 追加元素（不检查容量，认为有足够空间存放元素），如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void push(Object o) {
		if (o instanceof String) {
			datas[++size] = (String)o;
		} else if (o == null) {
			datas[++size] = null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), Variant.getDataType(o)));
		}
	}
	
	/**
	 * 追加一个空成员（不检查容量，认为有足够空间存放元素）
	 */
	public void pushNull() {
		datas[++size] = null;
	}
	
	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void push(IArray array, int index) {
		push(array.get(index));
	}
	
	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void add(IArray array, int index) {
		add(array.get(index));
	}
	
	/**
	 * 把array中的第index个元素设给到当前数组的指定元素，如果类型不兼容则抛出异常
	 * @param curIndex 当前数组的元素索引，从1开始计数
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void set(int curIndex, IArray array, int index) {
		set(curIndex, array.get(index));
	}

	/**
	 * 取指定位置元素
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public Object get(int index) {
		return datas[index];
	}
	
	public String getString(int index) {
		return datas[index];
	}

	/**
	 * 取指定位置元素的整数值
	 * @param index 索引，从1开始计数
	 * @return 长整数值
	 */
	public int getInt(int index) {
		throw new RuntimeException();
	}

	/**
	 * 取指定位置元素的长整数值
	 * @param index 索引，从1开始计数
	 * @return 长整数值
	 */
	public long getLong(int index) {
		throw new RuntimeException();
	}
	
	/**
	 * 取指定位置元素组成新数组
	 * @param indexArray 位置数组
	 * @return IArray
	 */
	public IArray get(int []indexArray) {
		String []datas = this.datas;
		int len = indexArray.length;
		StringArray result = new StringArray(len);
		
		for (int i : indexArray) {
			result.pushString(datas[i]);
		}
		
		return result;
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
		String []datas = this.datas;
		int len = end - start + 1;
		String []resultDatas = new String[len + 1];
		
		if (doCheck) {
			for (int i = 1; start <= end; ++start, ++i) {
				int q = indexArray[start];
				if (q > 0) {
					resultDatas[i] = datas[q];
				}
			}
		} else {
			for (int i = 1; start <= end; ++start) {
				resultDatas[i++] = datas[indexArray[start]];
			}
		}
		
		return new StringArray(resultDatas, len);
	}
	
	/**
	 * 取指定位置元素组成新数组
	 * @param IArray 位置数组
	 * @return IArray
	 */
	public IArray get(IArray indexArray) {
		String []datas = this.datas;
		int len = indexArray.size();
		StringArray result = new StringArray(len);
		
		for (int i = 1; i <= len; ++i) {
			result.pushString(datas[indexArray.getInt(i)]);
		}
		
		return result;
	}
	
	/**
	 * 取某一区段组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @return IArray
	 */
	public IArray get(int start, int end) {
		int newSize = end - start;
		String []newDatas = new String[newSize + 1];
		System.arraycopy(datas, start, newDatas, 1, newSize);
		return new StringArray(newDatas, newSize);
	}

	/**
	 * 使列表的容量不小于minCapacity
	 * @param minCapacity 最小容量
	 */
	public void ensureCapacity(int minCapacity) {
		if (datas.length <= minCapacity) {
			int newCapacity = (datas.length * 3) / 2;
			if (newCapacity <= minCapacity) {
				newCapacity = minCapacity + 1;
			}

			String []newDatas = new String[newCapacity];
			System.arraycopy(datas, 0, newDatas, 0, size + 1);
			datas = newDatas;
		}
	}
	
	/**
	 * 调整容量，使其与元素数相等
	 */
	public void trimToSize() {
		int newLen = size + 1;
		if (newLen < datas.length) {
			String []newDatas = new String[newLen];
			System.arraycopy(datas, 0, newDatas, 0, newLen);
			datas = newDatas;
		}
	}

	/**
	 * 判断指定位置的元素是否是空
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isNull(int index) {
		return datas[index] == null;
	}
	
	/**
	 * 判断元素是否是True
	 * @return BoolArray
	 */
	public BoolArray isTrue() {
		int size = this.size;
		String []datas = this.datas;
		boolean []resultDatas = new boolean[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			resultDatas[i] = datas[i] != null;
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
		String []datas = this.datas;
		boolean []resultDatas = new boolean[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			resultDatas[i] = datas[i] == null;
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
		// 非空则是true
		return datas[index] != null;
	}
	
	/**
	 * 判断指定位置的元素是否是False
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isFalse(int index) {
		// 空则是false
		return datas[index] == null;
	}

	/**
	 * 是否是计算过程中临时产生的数组，临时产生的可以被修改，比如 f1+f2+f3，只需产生一个数组存放结果
	 * @return true：是临时产生的数组，false：不是临时产生的数组
	 */
	public boolean isTemporary() {
		return datas[0] != null;
	}

	/**
	 * 设置是否是计算过程中临时产生的数组
	 * @param ifTemporary true：是临时产生的数组，false：不是临时产生的数组
	 */
	public void setTemporary(boolean ifTemporary) {
		datas[0] = ifTemporary ? TEMP : null;
	}
	
	/**
	 * 删除最后一个元素
	 */
	public void removeLast() {
		datas[size--] = null;
	}

	/**
	 * 删除指定位置的元素
	 * @param index 索引，从1开始计数
	 */
	public void remove(int index) {
		System.arraycopy(datas, index + 1, datas, index, size - index);
		datas[size--] = null;
	}
	
	/**
	 * 删除指定区间内的元素
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 */
	public void removeRange(int fromIndex, int toIndex) {
		System.arraycopy(datas, toIndex + 1, datas, fromIndex, size - toIndex);
		
		int newSize = size - (toIndex - fromIndex + 1);
		while (size != newSize) {
			datas[size--] = null;
		}
	}
	
	/**
	 * 删除指定位置的元素，序号从小到大排序
	 * @param seqs 索引数组
	 */
	public void remove(int []seqs) {
		int delCount = 0;
		String []datas = this.datas;
		
		for (int i = 0, len = seqs.length; i < len; ) {
			int cur = seqs[i];
			i++;

			int moveCount;
			if (i < len) {
				moveCount = seqs[i] - cur - 1;
			} else {
				moveCount = size - cur;
			}

			if (moveCount > 0) {
				System.arraycopy(datas, cur + 1, datas, cur - delCount, moveCount);
			}
			
			delCount++;
		}

		for (int i = 0, q = size; i < delCount; ++i) {
			datas[q - i] = null;
		}
		
		size -= delCount;
	}
	
	/**
	 * 保留指定区间内的数据
	 * @param start 起始位置（包含）
	 * @param end 结束位置（包含）
	 */
	public void reserve(int start, int end) {
		int newSize = end - start + 1;
		System.arraycopy(datas, start, datas, 1, newSize);
		
		for (int i = size; i > newSize; --i) {
			datas[i] = null;
		}
		
		size = newSize;
	}

	public int size() {
		return size;
	}
	
	/**
	 * 返回数组的非空元素数目
	 * @return 非空元素数目
	 */
	public int count() {
		String []datas = this.datas;
		int size = this.size;
		int count = size;
		
		for (int i = 1; i <= size; ++i) {
			if (datas[i] == null) {
				count--;
			}
		}
		
		return count;
	}
	
	/**
	 * 判断数组是否有取值为true的元素
	 * @return true：有，false：没有
	 */
	public boolean containTrue() {
		int size = this.size;
		if (size == 0) {
			return false;
		}
		
		String []datas = this.datas;
		for (int i = 1; i <= size; ++i) {
			if (datas[i] != null) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 返回第一个不为空的元素
	 * @return Object
	 */
	public Object ifn() {
		int size = this.size;
		String []datas = this.datas;
		
		for (int i = 1; i <= size; ++i) {
			if (datas[i] != null) {
				return datas[i];
			}
		}
		
		return null;
	}

	/**
	 * 修改数组指定元素的值，如果类型不兼容则抛出异常
	 * @param index 索引，从1开始计数
	 * @param obj 值
	 */
	public void set(int index, Object obj) {
		if (obj instanceof String) {
			datas[index] = (String)obj;
		} else if (obj == null) {
			datas[index] = null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), Variant.getDataType(obj)));
		}
	}
	
	/**
	 * 删除所有的元素
	 */
	public void clear() {
		Object []datas = this.datas;
		int size = this.size;
		this.size = 0;
		
		while (size > 0) {
			datas[size--] = null;
		}
	}

	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @return int 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem) {
		if (elem instanceof String) {
			String v = (String)elem;
			String []datas = this.datas;
			int low = 1, high = size;
			
			while (low <= high) {
				int mid = (low + high) >> 1;
				int cmp = compare(datas[mid], v);
				if (cmp < 0) {
					low = mid + 1;
				} else if (cmp > 0) {
					high = mid - 1;
				} else {
					return mid; // key found
				}
			}

			return -low; // key not found
		} else if (elem == null) {
			if (size > 0 && datas[1] == null) {
				return 1;
			} else {
				return -1;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", datas[1], elem,
					getDataType(), Variant.getDataType(elem)));
		}
	}
	
	// 数组按降序排序，进行降序二分查找
	private int descBinarySearch(String elem) {
		String []datas = this.datas;
		int low = 1, high = size;
		
		while (low <= high) {
			int mid = (low + high) >> 1;
			int cmp = compare(datas[mid], elem);
			if (cmp < 0) {
				high = mid - 1;
			} else if (cmp > 0) {
				low = mid + 1;
			} else {
				return mid; // key found
			}
		}

		return -low; // key not found
	}
	
	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @param start 起始查找位置（包含）
	 * @param end 结束查找位置（包含）
	 * @return 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem, int start, int end) {
		if (elem instanceof String) {
			String v = (String)elem;
			String []datas = this.datas;
			int low = start, high = end;
			
			while (low <= high) {
				int mid = (low + high) >> 1;
				int cmp = compare(datas[mid], v);
				if (cmp < 0) {
					low = mid + 1;
				} else if (cmp > 0) {
					high = mid - 1;
				} else {
					return mid; // key found
				}
			}

			return -low; // key not found
		} else if (elem == null) {
			if (end > 0 && datas[start] == null) {
				return start;
			} else {
				return -1;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", datas[1], elem,
					getDataType(), Variant.getDataType(elem)));
		}
	}
	
	/**
	 * 返回列表中是否包含指定元素
	 * @param elem Object 待查找的元素
	 * @return boolean true：包含，false：不包含
	 */
	public boolean contains(Object elem) {
		if (elem instanceof String) {
			String v = (String)elem;
			String []datas = this.datas;
			int size = this.size;
			
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null && datas[i].equals(v)) {
					return true;
				}
			}
			
			return false;
		} else if (elem == null) {
			int size = this.size;
			String []datas = this.datas;
			for (int i = 1; i <= size; ++i) {
				if (datas[i] == null) {
					return true;
				}
			}
			
			return false;
		} else {
			return false;
		}
	}
	
	/**
	 * 判断数组的元素是否在当前数组中
	 * @param isSorted 当前数组是否有序
	 * @param array 数组
	 * @param result 用于存放结果，只找取值为true的
	 */
	public void contains(boolean isSorted, IArray array, BoolArray result) {
		int resultSize = result.size();
		if (isSorted) {
			for (int i = 1; i <= resultSize; ++i) {
				if (result.isTrue(i) && binarySearch(array.get(i)) < 1) {
					result.set(i, false);
				}
			}
		} else {
			for (int i = 1; i <= resultSize; ++i) {
				if (result.isTrue(i) && !contains(array.get(i))) {
					result.set(i, false);
				}
			}
		}
	}
	
	/**
	 * 返回列表中是否包含指定元素，使用等号比较
	 * @param elem
	 * @return boolean true：包含，false：不包含
	 */
	public boolean objectContains(Object elem) {
		Object []datas = this.datas;
		for (int i = 1, size = this.size; i <= size; ++i) {
			if (datas[i] == elem) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 返回元素在数组中首次出现的位置
	 * @param elem 待查找的元素
	 * @param start 起始查找位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	public int firstIndexOf(Object elem, int start) {
		if (elem instanceof String) {
			String v = (String)elem;
			String []datas = this.datas;
			int size = this.size;
			
			for (int i = start; i <= size; ++i) {
				if (datas[i] != null && datas[i].equals(v)) {
					return i;
				}
			}
			
			return 0;
		} else if (elem == null) {
			int size = this.size;
			String []datas = this.datas;
			for (int i = start; i <= size; ++i) {
				if (datas[i] == null) {
					return i;
				}
			}
			
			return 0;
		} else {
			return 0;
		}
	}
	
	/**
	 * 返回元素在数组中最后出现的位置
	 * @param elem 待查找的元素
	 * @param start 从后面开始查找的位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	public int lastIndexOf(Object elem, int start) {
		if (elem instanceof String) {
			String v = (String)elem;
			String []datas = this.datas;
			
			for (int i = start; i > 0; --i) {
				if (datas[i] != null && datas[i].equals(v)) {
					return i;
				}
			}
			
			return 0;
		} else if (elem == null) {
			String []datas = this.datas;
			for (int i = start; i > 0; --i) {
				if (datas[i] == null) {
					return i;
				}
			}
			
			return 0;
		} else {
			return 0;
		}
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
		int size = this.size;
		String []datas = this.datas;
		
		if (elem == null) {
			IntArray result = new IntArray(7);
			if (isSorted) {
				if (isFromHead) {
					for (int i = start; i <= size; ++i) {
						if (datas[i] == null) {
							result.addInt(i);
						} else {
							break;
						}
					}
				} else {
					for (int i = start; i > 0; --i) {
						if (datas[i] == null) {
							result.addInt(i);
						}
					}
				}
			} else {
				if (isFromHead) {
					for (int i = start; i <= size; ++i) {
						if (datas[i] == null) {
							result.addInt(i);
						}
					}
				} else {
					for (int i = start; i > 0; --i) {
						if (datas[i] == null) {
							result.addInt(i);
						}
					}
				}
			}
			
			return result;
		} else if (!(elem instanceof String)) {
			return new IntArray(1);
		}

		String str = (String)elem;
		if (isSorted) {
			int end = size;
			if (isFromHead) {
				end = start;
				start = 1;
			}
			
			int index = binarySearch(str, start, end);
			if (index < 1) {
				return new IntArray(1);
			}
			
			// 找到第一个
			int first = index;
			while (first > start && compare(datas[first - 1], str) == 0) {
				first--;
			}
			
			// 找到最后一个
			int last = index;
			while (last < end && compare(datas[last + 1], str) == 0) {
				last++;
			}
			
			IntArray result = new IntArray(last - first + 1);
			if (isFromHead) {
				for (; first <= last; ++first) {
					result.pushInt(first);
				}
			} else {
				for (; last >= first; --last) {
					result.pushInt(last);
				}
			}
			
			return result;
		} else {
			IntArray result = new IntArray(7);
			if (isFromHead) {
				for (int i = start; i <= size; ++i) {
					if (compare(datas[i], str) == 0) {
						result.addInt(i);
					}
				}
			} else {
				for (int i = start; i > 0; --i) {
					if (compare(datas[i], str) == 0) {
						result.addInt(i);
					}
				}
			}
			
			return result;
		}
	}
	
	/**
	 * 对数组成员求绝对值
	 * @return IArray 绝对值数组
	 */
	public IArray abs() {
		MessageManager mm = EngineMessage.get();
		throw new RuntimeException(getDataType() + mm.getMessage("Variant2.illAbs"));
	}

	/**
	 * 对数组成员求负
	 * @return IArray 负值数组
	 */
	public IArray negate() {
		int size = this.size;
		String []datas = this.datas;
		
		// 不需要判断成员是否是null
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					datas[i] = Variant.negate(datas[i]);
				}
			}
			
			return this;
		} else {
			String []newDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					newDatas[i] = Variant.negate(datas[i]);
				}
			}
			
			StringArray  result = new StringArray(newDatas, size);
			result.setTemporary(true);
			return result;
		}
	}

	/**
	 * 对数组成员求非
	 * @return IArray 非值数组
	 */
	public IArray not() {
		String []datas = this.datas;
		int size = this.size;
		
		boolean []newDatas = new boolean[size + 1];
		for (int i = 1; i <= size; ++i) {
			newDatas[i] = datas[i] == null;
		}
		
		IArray  result = new BoolArray(newDatas, size);
		result.setTemporary(true);
		return result;
	}

	/**
	 * 判断数组的成员是否都是数（可以包含null）
	 * @return true：都是数，false：含有非数的值
	 */
	public boolean isNumberArray() {
		return false;
	}

	/**
	 * 计算两个数组的相对应的成员的和
	 * @param array 右侧数组
	 * @return 和数组
	 */
	public IArray memberAdd(IArray array) {
		if (array instanceof StringArray) {
			return memberAdd((StringArray)array);
		} else if (array instanceof ConstArray) {
			return memberAdd(array.get(1));
		} else if (array instanceof ObjectArray) {
			return ((ObjectArray)array).memberAdd(this);
		} else if (array instanceof NumberArray) {
			return memberAdd((NumberArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illAdd"));
		}
	}

	/**
	 * 计算数组的成员与指定常数的和
	 * @param value 常数
	 * @return 和数组
	 */
	public IArray memberAdd(Object value) {
		if (value == null) {
			return this;
		}
		
		int size = this.size;
		String []datas = this.datas;
		
		if (value instanceof String) {
			String str = (String)value;
			if (isTemporary()) {
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						datas[i] += str;
					} else {
						datas[i] = str;
					}
				}
				
				return this;
			} else {
				String []newDatas = new String[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						newDatas[i] = datas[i] + str;
					} else {
						newDatas[i] = str;
					}
				}
				
				StringArray result = new StringArray(newDatas, size);
				result.setTemporary(true);
				return result;
			}
		} else if (value instanceof Number) {
			Number n2 = (Number)value;
			Object []resultDatas = new Object[size + 1];
			
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					Number n1 = Variant.parseNumber(datas[i]);
					if (n1 == null) {
						resultDatas[i] = n2;
					} else {
						resultDatas[i] = Variant.addNum(n1, n2);
					}
				} else {
					resultDatas[i] = n2;
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					Variant.getDataType(value) + mm.getMessage("Variant2.illAdd"));
		}
	}

	private StringArray memberAdd(StringArray array) {
		int size = this.size;
		String []d1 = this.datas;
		String []d2 = array.datas;
		String []newDatas;
		StringArray result;
		
		if (isTemporary()) {
			newDatas = d1;
			result = this;
		} else if (array.isTemporary()) {
			newDatas = d2;
			result = array;
		} else {
			newDatas = new String[size + 1];
			result = new StringArray(newDatas, size);
		}
		
		for (int i = 1; i <= size; ++i) {
			if (d1[i] != null) {
				if (d2[i] != null) {
					newDatas[i] = d1[i] + d2[i];
				} else {
					newDatas[i] = d1[i];
				}
			} else {
				newDatas[i] = d2[i];
			}
		}
		
		return result;
	}
	
	public ObjectArray memberAdd(NumberArray array) {
		int size = this.size;
		String []datas = this.datas;
		Object []resultDatas = new Object[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			if (datas[i] != null) {
				Number n1 = Variant.parseNumber(datas[i]);
				if (n1 == null) {
					resultDatas[i] = array.get(i);
				} else {
					resultDatas[i] = Variant.addNum(n1, (Number)array.get(i));
				}
			} else {
				resultDatas[i] = array.get(i);
			}
		}
		
		ObjectArray result = new ObjectArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	/**
	 * 计算两个数组的相对应的成员的差
	 * @param array 右侧数组
	 * @return 差数组
	 */
	public IArray memberSubtract(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illSubtract"));
	}

	/**
	 * 计算两个数组的相对应的成员的积
	 * @param array 右侧数组
	 * @return 积数组
	 */
	public IArray memberMultiply(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illMultiply"));
	}

	/**
	 * 计算数组的成员与指定常数的积
	 * @param value 常数
	 * @return 积数组
	 */
	public IArray memberMultiply(Object value) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				Variant.getDataType(value) + mm.getMessage("Variant2.illMultiply"));
	}

	/**
	 * 把右侧数组的成员变成String拼接到左侧数组的成员上
	 * @param array 右侧数组
	 * @return 商数组
	 */
	public IArray memberDivide(IArray array) {
		if (array instanceof StringArray) {
			return memberDivide((StringArray)array);
		} else if (array instanceof IntArray) {
			return memberDivide((IntArray)array);
		} else if (array instanceof LongArray) {
			return memberDivide((LongArray)array);
		} else if (array instanceof DoubleArray) {
			return memberDivide((DoubleArray)array);
		} else if (array instanceof ConstArray) {
			return memberDivide(array.get(1));
		} else if (array instanceof BoolArray) {
			return memberDivide((BoolArray)array);
		} else {
			int size = this.size;
			String []datas = this.datas;
			
			if (isTemporary()) {
				for (int i = 1; i <= size; ++i) {
					if (!array.isNull(i)) {
						if (datas[i] != null) {
							datas[i] += array.get(i);
						} else {
							datas[i] = array.get(i).toString();
						}
					}
				}
				
				return this;
			} else {
				String []resultDatas = new String[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						if (!array.isNull(i)) {
							resultDatas[i] = datas[i] + array.get(i);
						} else {
							resultDatas[i] = datas[i];
						}
					} else if (!array.isNull(i)) {
						resultDatas[i] = array.get(i).toString();
					}
				}
				
				StringArray result = new StringArray(resultDatas, size);
				result.setTemporary(true);
				return result;
			}
		}
	}
	
	private StringArray memberDivide(Object value) {
		if (value == null) {
			return this;
		}
		
		String str = value.toString();
		int size = this.size;
		String []datas = this.datas;

		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					datas[i] += str;
				} else {
					datas[i] = str;
				}
			}
			
			return this;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					resultDatas[i] = datas[i] + str;
				} else {
					resultDatas[i] = str;
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private StringArray memberDivide(StringArray array) {
		int size = this.size;
		String []d1 = this.datas;
		String []d2 = array.datas;
		
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (d2[i] != null) {
					if (d1[i] != null) {
						d1[i] += d2[i];
					} else {
						d1[i] = d2[i];
					}
				}
			}
			
			return this;
		} else if (array.isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (d2[i] != null) {
						d2[i] = d1[i] + d2[i];
					} else {
						d2[i] = d1[i];
					}
				}
			}
			
			return array;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (d2[i] != null) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d1[i];
					}
				} else {
					resultDatas[i] = d2[i];
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private StringArray memberDivide(IntArray array) {
		int size = this.size;
		String []d1 = this.datas;
		int []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (s2 == null || !s2[i]) {
					if (d1[i] != null) {
						d1[i] += d2[i];
					} else {
						d1[i] = Integer.toString(d2[i]);
					}
				}
			}
			
			return this;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d1[i];
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = Integer.toString(d2[i]);
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private StringArray memberDivide(LongArray array) {
		int size = this.size;
		String []d1 = this.datas;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (s2 == null || !s2[i]) {
					if (d1[i] != null) {
						d1[i] += d2[i];
					} else {
						d1[i] = Long.toString(d2[i]);
					}
				}
			}
			
			return this;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d1[i];
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = Long.toString(d2[i]);
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private StringArray memberDivide(DoubleArray array) {
		int size = this.size;
		String []d1 = this.datas;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (s2 == null || !s2[i]) {
					if (d1[i] != null) {
						d1[i] += d2[i];
					} else {
						d1[i] = Double.toString(d2[i]);
					}
				}
			}
			
			return this;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d1[i];
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = Double.toString(d2[i]);
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private StringArray memberDivide(BoolArray array) {
		int size = this.size;
		String []d1 = this.datas;
		boolean []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (s2 == null || !s2[i]) {
					if (d1[i] != null) {
						d1[i] += d2[i];
					} else {
						d1[i] = Boolean.toString(d2[i]);
					}
				}
			}
			
			return this;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d1[i];
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = Boolean.toString(d2[i]);
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
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
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illMod"));
	}

	/**
	 * 计算两个数组的数成员整除或序列成员差集
	 * @param array 右侧数组
	 * @return 整除值数组或序列差集数组
	 */
	public IArray memberIntDivide(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illDivide"));
	}

	/**
	 * 计算两个数组的相对应的成员的关系运算
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @return 关系运算结果数组
	 */
	public BoolArray calcRelation(IArray array, int relation) {
		if (array instanceof StringArray) {
			return calcRelation((StringArray)array, relation);
		} else if (array instanceof ConstArray) {
			return calcRelation(array.get(1), relation);
		} else if (array instanceof ObjectArray) {
			return calcRelation((ObjectArray)array, relation);
		} else if (array instanceof BoolArray) {
			return ((BoolArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else if (array instanceof IntArray) {
			return ((IntArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else if (array instanceof LongArray) {
			return ((LongArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else if (array instanceof DoubleArray) {
			return ((DoubleArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else if (array instanceof DateArray) {
			return ((DateArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else {
			return array.calcRelation(this, Relation.getInverseRelation(relation));
		}
	}
	
	/**
	 * 计算两个数组的相对应的成员的关系运算
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @return 关系运算结果数组
	 */
	public BoolArray calcRelation(Object value, int relation) {
		if (value instanceof String) {
			return calcRelation((String)value, relation);
		} else if (value == null) {
			return ArrayUtil.calcRelationNull(datas, size, relation);
		} else {
			boolean b = Variant.isTrue(value);
			int size = this.size;
			String []datas = this.datas;
			
			if (relation == Relation.AND) {
				BoolArray result;
				if (!b) {
					result = new BoolArray(false, size);
				} else {
					boolean []resultDatas = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = datas[i] != null;
					}
					
					result = new BoolArray(resultDatas, size);
				}
				
				result.setTemporary(true);
				return result;
			} else if (relation == Relation.OR) {
				BoolArray result;
				if (b) {
					result = new BoolArray(true, size);
				} else {
					boolean []resultDatas = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = datas[i] != null;
					}
					
					result = new BoolArray(resultDatas, size);
				}
				
				result.setTemporary(true);
				return result;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
						getDataType(), Variant.getDataType(value)));
			}
		}
	}
	
	private BoolArray calcRelation(String value, int relation) {
		int size = this.size;
		String []d1 = this.datas;
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) == 0;
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) > 0;
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) >= 0;
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) < 0;
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) <= 0;
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) != 0;
			}
		} else if (relation == Relation.AND) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] != null;
			}
		} else { // Relation.OR
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = true;
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	private BoolArray calcRelation(StringArray array, int relation) {
		int size = this.size;
		String []d1 = this.datas;
		String []d2 = array.datas;
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) == 0;
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) > 0;
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) >= 0;
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) < 0;
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) <= 0;
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) != 0;
			}
		} else if (relation == Relation.AND) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] != null && d2[i] != null;
			}
		} else { // Relation.OR
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] != null || d2[i] != null;
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}

	BoolArray calcRelation(ObjectArray array, int relation) {
		int size = this.size;
		String []d1 = this.datas;
		Object []d2 = array.getDatas();
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) == 0;
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) > 0;
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) >= 0;
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) < 0;
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) <= 0;
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) != 0;
			}
		} else if (relation == Relation.AND) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] != null && Variant.isTrue(d2[i]);
			}
		} else { // Relation.OR
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] != null || Variant.isTrue(d2[i]);
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}

	/**
	 * 比较两个数组的大小
	 * @param array 右侧数组
	 * @return 1：当前数组大，0：两个数组相等，-1：当前数组小
	 */
	public int compareTo(IArray array) {
		int size1 = this.size;
		int size2 = array.size();
		String []d1 = this.datas;
		
		int size = size1;
		int result = 0;
		if (size1 < size2) {
			result = -1;
		} else if (size1 > size2) {
			result = 1;
			size = size2;
		}

		if (array instanceof StringArray) {
			StringArray array2 = (StringArray)array;
			String []d2 = array2.datas;
			
			for (int i = 1; i <= size; ++i) {
				int cmp = compare(d1[i], d2[i]);
				if (cmp != 0) {
					return cmp;
				}
			}
		} else if (array instanceof ConstArray) {
			Object value = array.get(1);
			if (value instanceof String) {
				String d2 = (String)value;
				for (int i = 1; i <= size; ++i) {
					int cmp = compare(d1[i], d2);
					if (cmp != 0) {
						return cmp;
					}
				}
			} else if (value == null) {
				for (int i = 1; i <= size; ++i) {
					if (d1[i] != null) {
						return 1;
					}
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
						getDataType(), array.getDataType()));
			}
		} else if (array instanceof ObjectArray) {
			ObjectArray array2 = (ObjectArray)array;
			Object []d2 = array2.getDatas();
			
			for (int i = 1; i <= size; ++i) {
				int cmp = compare(d1[i], d2[i]);
				if (cmp != 0) {
					return cmp;
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		}
		
		return result;
	}
	
	/**
	 * 计算数组的2个成员的比较值
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	public int memberCompare(int index1, int index2) {
		return compare(datas[index1], datas[index2]);
	}
	
	/**
	 * 判断数组的两个成员是否相等
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	public boolean isMemberEquals(int index1, int index2) {
		if (datas[index1] == null) {
			return datas[index2] == null;
		} else if (datas[index2] == null) {
			return false;
		} else {
			return datas[index1].equals(datas[index2]);
		}
	}
	
	/**
	 * 判断两个数组的指定元素是否相同
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要比较的数组
	 * @param index 要比较的数组的元素的索引
	 * @return true：相同，false：不相同
	 */
	public boolean isEquals(int curIndex, IArray array, int index) {
		Object value = array.get(index);
		if (value instanceof String) {
			return ((String)value).equals(datas[curIndex]);
		} else if (value == null) {
			return datas[curIndex] == null;
		} else {
			return false;
		}
	}
	
	/**
	 * 判断数组的指定元素是否与给定值相等
	 * @param curIndex 数组元素索引，从1开始计数
	 * @param value 值
	 * @return true：相等，false：不相等
	 */
	public boolean isEquals(int curIndex, Object value) {
		if (value instanceof String) {
			return ((String)value).equals(datas[curIndex]);
		} else if (value == null) {
			return datas[curIndex] == null;
		} else {
			return false;
		}
	}
	
	/**
	 * 判断两个数组的指定元素的大小
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要比较的数组
	 * @param index 要比较的数组的元素的索引
	 * @return 小于：小于0，等于：0，大于：大于0
	 */
	public int compareTo(int curIndex, IArray array, int index) {
		return compare(datas[curIndex], array.get(index));
	}
	
	/**
	 * 比较数组的指定元素与给定值的大小
	 * @param curIndex 当前数组的元素的索引
	 * @param value 要比较的值
	 * @return
	 */
	public int compareTo(int curIndex, Object value) {
		return compare(datas[curIndex], value);
	}
	
	/**
	 * 取指定成员的哈希值
	 * @param index 成员索引，从1开始计数
	 * @return 指定成员的哈希值
	 */
	public int hashCode(int index) {
		if (datas[index] != null) {
			return datas[index].hashCode();
		} else {
			return 0;
		}
	}
	
	/**
	 * 求成员和
	 * @return
	 */
	public Object sum() {
		return null;
	}
	
	/**
	 * 求平均值
	 * @return
	 */
	public Object average() {
		return null;
	}
	
	/**
	 * 得到最大的成员
	 * @return
	 */
	public Object max() {
		int size = this.size;
		if (size == 0) {
			return null;
		}

		String []datas = this.datas;
		String max = null;
		
		int i = 1;
		for (; i <= size; ++i) {
			if (datas[i] != null) {
				max = datas[i];
				break;
			}
		}
		
		for (++i; i <= size; ++i) {
			if (datas[i] != null && max.compareTo(datas[i]) < 0) {
				max = datas[i];
			}
		}
		
		return max;
	}
	
	/**
	 * 得到最小的成员
	 * @return
	 */
	public Object min() {
		int size = this.size;
		if (size == 0) {
			return null;
		}

		String []datas = this.datas;
		String min = null;
		
		int i = 1;
		for (; i <= size; ++i) {
			if (datas[i] != null) {
				min = datas[i];
				break;
			}
		}
		
		for (++i; i <= size; ++i) {
			if (datas[i] != null && min.compareTo(datas[i]) > 0) {
				min = datas[i];
			}
		}
		
		return min;
	}

	/**
	 * 计算两个数组的相对应的成员的关系运算，只计算result为真的行
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	public void calcRelations(IArray array, int relation, BoolArray result, boolean isAnd) {
		if (array instanceof StringArray) {
			calcRelations((StringArray)array, relation, result, isAnd);
		} else if (array instanceof ConstArray) {
			calcRelations(array.get(1), relation, result, isAnd);
		} else if (array instanceof ObjectArray) {
			calcRelations((ObjectArray)array, relation, result, isAnd);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		} 
	}

	/**
	 * 计算两个数组的相对应的成员的关系运算，只计算result为真的行
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	public void calcRelations(Object value, int relation, BoolArray result, boolean isAnd) {
		if (value instanceof String) {
			calcRelations((String)value, relation, result, isAnd);
		} else if (value == null) {
			ArrayUtil.calcRelationsNull(datas, size, relation, result, isAnd);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
					getDataType(), Variant.getDataType(value)));
		}
	}

	private void calcRelations(String value, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		String []d1 = this.datas;
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) != 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) <= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) < 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) >= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) > 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) == 0) {
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
					if (!resultDatas[i] && compare(d1[i], value) == 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) > 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) >= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) < 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) <= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) != 0) {
						resultDatas[i] = true;
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	private void calcRelations(StringArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		String []d1 = this.datas;
		String []d2 = array.datas;
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) != 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) <= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) < 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) >= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) > 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) == 0) {
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
					if (!resultDatas[i] && compare(d1[i], d2[i]) == 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) > 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) >= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) < 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) <= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) != 0) {
						resultDatas[i] = true;
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	void calcRelations(ObjectArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		String []d1 = this.datas;
		Object []d2 = array.getDatas();
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) != 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) <= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) < 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) >= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) > 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) == 0) {
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
					if (!resultDatas[i] && compare(d1[i], d2[i]) == 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) > 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) >= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) < 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) <= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) != 0) {
						resultDatas[i] = true;
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	/**
	 * 计算两个数组的相对应的成员的按位与
	 * @param array 右侧数组
	 * @return 按位与结果数组
	 */
	public IArray bitwiseAnd(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("and" + mm.getMessage("function.paramTypeError"));
	}
	
	/**
	 * 计算两个数组的相对应的成员的按位或
	 * @param array 右侧数组
	 * @return 按位或结果数组
	 */
	public IArray bitwiseOr(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("or" + mm.getMessage("function.paramTypeError"));
	}
	
	/**
	 * 计算两个数组的相对应的成员的按位异或
	 * @param array 右侧数组
	 * @return 按位异或结果数组
	 */
	public IArray bitwiseXOr(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("xor" + mm.getMessage("function.paramTypeError"));
	}
	
	/**
	 * 计算数组成员的按位取反
	 * @return 成员按位取反结果数组
	 */
	public IArray bitwiseNot() {
		MessageManager mm = EngineMessage.get();
		throw new RQException("not" + mm.getMessage("function.paramTypeError"));
	}

	/**
	 * 取出标识数组取值为真的行对应的数据，组成新数组
	 * @param signArray 标识数组
	 * @return IArray
	 */
	public IArray select(IArray signArray) {
		int size = signArray.size();
		String []d1 = this.datas;
		String []resultDatas = new String[size + 1];
		int count = 0;
		
		if (signArray instanceof BoolArray) {
			BoolArray array = (BoolArray)signArray;
			boolean []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (d2[i]) {
						resultDatas[++count] = d1[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i] && d2[i]) {
						resultDatas[++count] = d1[i];
					}
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (signArray.isTrue(i)) {
					resultDatas[++count] = d1[i];
				}
			}
		}
		
		return new StringArray(resultDatas, count);
	}
	
	/**
	 * 取某一区段标识数组取值为真的行组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @param signArray 标识数组
	 * @return IArray
	 */
	public IArray select(int start, int end, IArray signArray) {
		String []d1 = this.datas;
		String []resultDatas = new String[end - start + 1];
		int count = 0;
		
		if (signArray instanceof BoolArray) {
			BoolArray array = (BoolArray)signArray;
			boolean []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			if (s2 == null) {
				for (int i = start; i < end; ++i) {
					if (d2[i]) {
						resultDatas[++count] = d1[i];
					}
				}
			} else {
				for (int i = start; i < end; ++i) {
					if (!s2[i] && d2[i]) {
						resultDatas[++count] = d1[i];
					}
				}
			}
		} else {
			for (int i = start; i < end; ++i) {
				if (signArray.isTrue(i)) {
					resultDatas[++count] = d1[i];
				}
			}
		}
		
		return new StringArray(resultDatas, count);
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
		Object []result = new Object[size];
		System.arraycopy(datas, 1, result, 0, size);
		return result;
	}
	
	/**
	 * 把成员填到指定的数组
	 * @param result 用于存放成员的数组
	 */
	public void toArray(Object []result) {
		System.arraycopy(datas, 1, result, 0, size);
	}
	
	/**
	 * 把数组从指定位置拆成两个数组
	 * @param pos 位置，包含
	 * @return 返回后半部分元素构成的数组
	 */
	public IArray split(int pos) {
		String []datas = this.datas;
		int size = this.size;
		int resultSize = size - pos + 1;
		String []resultDatas = new String[resultSize + 1];
		System.arraycopy(datas, pos, resultDatas, 1, resultSize);
		
		for (int i = pos; i <= size; ++i) {
			datas[i] = null;
		}
		
		this.size = pos - 1;
		return new StringArray(resultDatas, resultSize);
	}
	
	/**
	 * 把指定区间元素分离出来组成新数组
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 * @return
	 */
	public IArray split(int from, int to) {
		String []datas = this.datas;
		int oldSize = this.size;
		int resultSize = to - from + 1;
		String []resultDatas = new String[resultSize + 1];
		System.arraycopy(datas, from, resultDatas, 1, resultSize);
		
		System.arraycopy(datas, to + 1, datas, from, oldSize - to);
		this.size -= resultSize;
		
		for (int i = this.size + 1; i <= oldSize; ++i) {
			datas[i] = null;
		}
		
		return new StringArray(resultDatas, resultSize);
	}
	
	/**
	 * 对数组的元素进行排序
	 */
	public void sort() {
		MultithreadUtil.sort(datas, 1, size + 1);
	}
	
	/**
	 * 对数组的元素进行排序
	 * @param comparator 比较器
	 */
	public void sort(Comparator<Object> comparator) {
		MultithreadUtil.sort(datas, 1, size + 1, comparator);
	}
	
	/**
	 * 返回数组中是否含有记录
	 * @return boolean
	 */
	public boolean hasRecord() {
		return false;
	}
	
	/**
	 * 返回是否是（纯）排列
	 * @param isPure true：检查是否是纯排列
	 * @return boolean true：是，false：不是
	 */
	public boolean isPmt(boolean isPure) {
		return false;
	}
	
	/**
	 * 返回数组的反转数组
	 * @return IArray
	 */
	public IArray rvs() {
		int size = this.size;
		String []datas = this.datas;
		String []resultDatas = new String[size + 1];
		
		for (int i = 1, q = size; i <= size; ++i) {
			resultDatas[i] = datas[q--];
		}
		
		return new StringArray(resultDatas, size);
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
		int size = this.size;
		if (size == 0) {
			return new IntArray(0);
		}
		
		String []datas = this.datas;
		if (ignoreNull) {
			if (count == 1) {
				// 取最小值的位置
				String minValue = null;
				if (isAll) {
					IntArray result = new IntArray(8);
					int i = 1;
					for (; i <= size; ++i) {
						if (datas[i] != null) {
							minValue = datas[i];
							result.addInt(i);
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (datas[i] != null) {
							int cmp = datas[i].compareTo(minValue);
							if (cmp < 0) {
								minValue = datas[i];
								result.clear();
								result.addInt(i);
							} else if (cmp == 0) {
								result.addInt(i);
							}
						}
					}
					
					return result;
				} else if (isLast) {
					int i = size;
					int pos = 0;
					for (; i > 0; --i) {
						if (datas[i] != null) {
							minValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (--i; i > 0; --i) {
						if (datas[i] != null && datas[i].compareTo(minValue) < 0) {
							minValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					if (pos != 0) {
						result.pushInt(pos);
					}
					
					return result;
				} else {
					int i = 1;
					int pos = 0;
					for (; i <= size; ++i) {
						if (datas[i] != null) {
							minValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (datas[i] != null && datas[i].compareTo(minValue) < 0) {
							minValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					if (pos != 0) {
						result.pushInt(pos);
					}
					
					return result;
				}
			} else if (count > 1) {
				// 取最小的count个元素的位置
				int next = count + 1;
				StringArray valueArray = new StringArray(next);
				IntArray posArray = new IntArray(next);
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						int index = valueArray.binarySearch(datas[i]);
						if (index < 1) {
							index = -index;
						}
						
						if (index <= count) {
							valueArray.insert(index, datas[i]);
							posArray.insertInt(index, i);
							if (valueArray.size() == next) {
								valueArray.removeLast();
								posArray.removeLast();
							}
						}
					}
				}
				
				return posArray;
			} else if (count == -1) {
				// 取最大值的位置
				String maxValue = null;
				if (isAll) {
					IntArray result = new IntArray(8);
					int i = 1;
					for (; i <= size; ++i) {
						if (datas[i] != null) {
							maxValue = datas[i];
							result.addInt(i);
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (datas[i] != null) {
							int cmp = datas[i].compareTo(maxValue);;
							if (cmp > 0) {
								maxValue = datas[i];
								result.clear();
								result.addInt(i);
							} else if (cmp == 0) {
								result.addInt(i);
							}
						}
					}
					
					return result;
				} else if (isLast) {
					int i = size;
					int pos = 0;
					for (; i > 0; --i) {
						if (datas[i] != null) {
							maxValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (--i; i > 0; --i) {
						if (datas[i] != null && datas[i].compareTo(maxValue) > 0) {
							maxValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					if (pos != 0) {
						result.pushInt(pos);
					}
					
					return result;
				} else {
					int i = 1;
					int pos = 0;
					for (; i <= size; ++i) {
						if (datas[i] != null) {
							maxValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (datas[i] != null && datas[i].compareTo(maxValue) > 0) {
							maxValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					if (pos != 0) {
						result.pushInt(pos);
					}
					
					return result;
				}
			} else if (count < -1) {
				// 取最大的count个元素的位置
				count = -count;
				int next = count + 1;
				StringArray valueArray = new StringArray(next);
				IntArray posArray = new IntArray(next);
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						int index = valueArray.descBinarySearch(datas[i]);
						if (index < 1) {
							index = -index;
						}
						
						if (index <= count) {
							valueArray.insert(index, datas[i]);
							posArray.insertInt(index, i);
							if (valueArray.size() == next) {
								valueArray.remove(next);
								posArray.remove(next);
							}
						}
					}
				}
				
				return posArray;
			} else {
				return new IntArray(1);
			}
		} else {
			if (count == 1) {
				// 取最小值的位置
				if (isAll) {
					IntArray result = new IntArray(8);
					result.addInt(1);
					String minValue = datas[1];
					
					for (int i = 2; i <= size; ++i) {
						int cmp = compare(datas[i], minValue);
						if (cmp < 0) {
							minValue = datas[i];
							result.clear();
							result.addInt(i);
						} else if (cmp == 0) {
							result.addInt(i);
						}
					}
					
					return result;
				} else if (isLast) {
					String minValue = datas[size];
					int pos = size;
					
					for (int i = size - 1; i > 0; --i) {
						if (compare(datas[i], minValue) < 0) {
							minValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					result.pushInt(pos);
					return result;
				} else {
					String minValue = datas[1];
					int pos = 1;
					
					for (int i = 2; i <= size; ++i) {
						if (compare(datas[i], minValue) < 0) {
							minValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					result.pushInt(pos);
					return result;
				}
			} else if (count > 1) {
				// 取最小的count个元素的位置
				int next = count + 1;
				StringArray valueArray = new StringArray(next);
				IntArray posArray = new IntArray(next);
				
				for (int i = 1; i <= size; ++i) {
					int index = valueArray.binarySearch(datas[i]);
					if (index < 1) {
						index = -index;
					}
					
					if (index <= count) {
						valueArray.insert(index, datas[i]);
						posArray.insertInt(index, i);
						if (valueArray.size() == next) {
							valueArray.removeLast();
							posArray.removeLast();
						}
					}
				}
				
				return posArray;
			} else if (count == -1) {
				// 取最大值的位置
				if (isAll) {
					IntArray result = new IntArray(8);
					String maxValue = datas[1];
					result.addInt(1);
					
					for (int i = 2; i <= size; ++i) {
						int cmp = compare(datas[i], maxValue);
						if (cmp > 0) {
							maxValue = datas[i];
							result.clear();
							result.addInt(i);
						} else if (cmp == 0) {
							result.addInt(i);
						}
					}
					
					return result;
				} else if (isLast) {
					String maxValue = datas[size];
					int pos = size;
					
					for (int i = size - 1; i > 0; --i) {
						if (compare(datas[i], maxValue) > 0) {
							maxValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					result.pushInt(pos);
					return result;
				} else {
					String maxValue = datas[1];
					int pos = 1;
					
					for (int i = 2; i <= size; ++i) {
						if (compare(datas[i], maxValue) > 0) {
							maxValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					result.pushInt(pos);
					return result;
				}
			} else if (count < -1) {
				// 取最大的count个元素的位置
				count = -count;
				int next = count + 1;
				StringArray valueArray = new StringArray(next);
				IntArray posArray = new IntArray(next);
				
				for (int i = 1; i <= size; ++i) {
					int index = valueArray.descBinarySearch(datas[i]);
					if (index < 1) {
						index = -index;
					}
					
					if (index <= count) {
						valueArray.insert(index, datas[i]);
						posArray.insertInt(index, i);
						if (valueArray.size() == next) {
							valueArray.remove(next);
							posArray.remove(next);
						}
					}
				}
				
				return posArray;
			} else {
				return new IntArray(1);
			}
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
		Object []resultDatas = new Object[size + 1];
		System.arraycopy(datas, 1, resultDatas, 1, size);
		return new ObjectArray(resultDatas, size);
	}
	
	/**
	 * 把对象数组转成纯类型数组，不能转则抛出异常
	 * @return IArray
	 */
	public IArray toPureArray() {
		return this;
	}
	
	/**
	 * 保留数组数据用于生成序列或序表
	 * @param refOrigin 引用源列，不复制数据
	 * @return
	 */
	public IArray reserve(boolean refOrigin) {
		if (isTemporary()) {
			setTemporary(false);
			return this;
		} else if (refOrigin) {
			return this;
		} else {
			return dup();
		}
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
		}
		
		int size = this.size;
		String []datas = this.datas;
		
		if (other instanceof StringArray) {
			String []otherDatas = ((StringArray)other).getDatas();
			
			if (isTemporary()) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						datas[i] = otherDatas[i];
					}
				}
				
				return this;
			} else {
				String []resultDatas = new String[size + 1];
				System.arraycopy(datas, 1, resultDatas, 1, size);
				
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						resultDatas[i] = otherDatas[i];
					}
				}
				
				IArray result = new StringArray(resultDatas, size);
				result.setTemporary(true);
				return result;
			}
		} else {
			Object []resultDatas = new Object[size + 1];
			System.arraycopy(datas, 1, resultDatas, 1, size);
			
			for (int i = 1; i <= size; ++i) {
				if (signArray.isFalse(i)) {
					resultDatas[i] = other.get(i);
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
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
		String []datas = this.datas;
		
		if (value instanceof String || value == null) {
			String str = (String)value;
			if (isTemporary()) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						datas[i] = str;
					}
				}
				
				return this;
			} else {
				String []resultDatas = new String[size + 1];
				System.arraycopy(datas, 1, resultDatas, 1, size);
				
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						resultDatas[i] = str;
					}
				}
				
				IArray result = new StringArray(resultDatas, size);
				result.setTemporary(true);
				return result;
			}
		} else {
			Object []resultDatas = new Object[size + 1];
			System.arraycopy(datas, 1, resultDatas, 1, size);
			
			for (int i = 1; i <= size; ++i) {
				if (signArray.isFalse(i)) {
					resultDatas[i] = value;
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	/**
	 * 返回指定数组的成员在当前数组中的位置
	 * @param array 待查找的数组
	 * @param opt 选项，b：同序归并法查找，i：返回单递增数列，c：连续出现
	 * @return 位置或者位置序列
	 */
	public Object pos(IArray array, String opt) {
		return ArrayUtil.pos(this, array, opt);
	}

	/**
	 * 返回数组成员的二进制表示时1的个数和
	 * @return
	 */
	public int bit1() {
		MessageManager mm = EngineMessage.get();
		throw new RQException("bit1" + mm.getMessage("function.paramTypeError"));
	}
}
