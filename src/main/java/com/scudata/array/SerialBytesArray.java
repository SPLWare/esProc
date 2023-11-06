package com.scudata.array;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Comparator;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.SerialBytes;
import com.scudata.expression.Relation;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.MultithreadUtil;
import com.scudata.util.HashUtil;
import com.scudata.util.Variant;

/**
 * 排号数组
 * @author WangXiaoJun
 *
 */
public class SerialBytesArray implements IArray {
	private static final long NULL = 0;
	
	// 第0个元素表示是否是临时数组
	private long []datas1; 
	private long []datas2;
	private int size;
	
	public SerialBytesArray() {
		this(DEFAULT_LEN);
	}

	public SerialBytesArray(int initialCapacity) {
		++initialCapacity;
		datas1 = new long[initialCapacity];
		datas2 = new long[initialCapacity];
	}
	
	public SerialBytesArray(long []datas1, long []datas2, int size) {
		this.datas1 = datas1;
		this.datas2 = datas2;
		this.size = size;
	}
	
	/**
	 * 写内容到流
	 * @param out 输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		int size = this.size;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
		out.writeInt(size);
		for (int i = 1; i <= size; ++i) {
			out.writeLong(datas1[i]);
		}
		
		for (int i = 1; i <= size; ++i) {
			out.writeLong(datas2[i]);
		}
	}

	/**
	 * 从流中读内容
	 * @param in 输入流
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		size = in.readInt();
		int len = size + 1;
		
		long []datas = this.datas1 = new long[len];
		for (int i = 1; i < len; ++i) {
			datas[i] = in.readLong();
		}
		
		datas = this.datas2 = new long[len];
		for (int i = 1; i < len; ++i) {
			datas[i] = in.readLong();
		}
	}

	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		int size = this.size;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
		out.writeInt(size);
		for (int i = 1; i <= size; ++i) {
			out.writeLong(datas1[i]);
		}
		
		for (int i = 1; i <= size; ++i) {
			out.writeLong(datas2[i]);
		}

		return out.toByteArray();
	}

	public void fillRecord(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(bytes);
		size = in.readInt();
		int len = size + 1;
		
		long []datas = this.datas1 = new long[len];
		for (int i = 1; i < len; ++i) {
			datas[i] = in.readLong();
		}
		
		datas = this.datas2 = new long[len];
		for (int i = 1; i < len; ++i) {
			datas[i] = in.readLong();
		}
	}

	public String getDataType() {
		MessageManager mm = EngineMessage.get();
		return mm.getMessage("DataType.SerialBytes");
	}
	
	/**
	 * 追加排号
	 * @param value1 值1
	 * @param value2 值2
	 */
	public void add(long value1, long value2) {
		int newSize = size + 1;
		ensureCapacity(newSize);
		datas1[newSize] = value1;
		datas2[newSize] = value1;
		size = newSize;
	}

	/**
	 * 追加元素，如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void add(Object o) {
		if (o instanceof SerialBytes) {
			ensureCapacity(size + 1);
			size++;
			
			SerialBytes sb = (SerialBytes)o;
			datas1[size] = sb.getValue1();
			datas2[size] = sb.getValue2();
		} else if (o == null) {
			ensureCapacity(size + 1);
			size++;
			datas1[size] = NULL;
			datas2[size] = NULL;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.SerialBytes"), Variant.getDataType(o)));
		}
	}

	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(Object[] array) {
		for (Object obj : array) {
			if (obj != null && !(obj instanceof SerialBytes)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.SerialBytes"), Variant.getDataType(obj)));
			}
		}
		
		int size2 = array.length;
		int size = this.size;
		ensureCapacity(size + size2);
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
		for (int i = 0; i < size2; ++i) {
			size++;
			if (array[i] != null) {
				SerialBytes sb = (SerialBytes)array[i];
				datas1[size] = sb.getValue1();
				datas2[size] = sb.getValue2();
			} else {
				datas1[size] = NULL;
				datas2[size] = NULL;
			}
		}
		
		this.size = size;
	}

	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(IArray array) {
		int size2 = array.size();
		if (size2 == 0) {
		} else if (array instanceof SerialBytesArray) {
			ensureCapacity(size + size2);
			SerialBytesArray sba = (SerialBytesArray)array;
			System.arraycopy(sba.datas1, 1, datas1, size + 1, size2);
			System.arraycopy(sba.datas2, 1, datas2, size + 1, size2);
			size += size2;
		} else {
			int size = this.size;
			ensureCapacity(size + size2);
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			
			for (int i = 1; i <= size2; ++i) {
				size++;
				Object obj = array.get(i);
				if (obj instanceof SerialBytes) {
					SerialBytes sb = (SerialBytes)array.get(i);
					datas1[size] = sb.getValue1();
					datas2[size] = sb.getValue2();
				} else if (obj == null) {
					datas1[size] = NULL;
					datas2[size] = NULL;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("pdm.arrayTypeError", 
							mm.getMessage("DataType.SerialBytes"), Variant.getDataType(obj)));
				}
			}
			
			this.size = size;
		}
	}

	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param count 元素个数
	 */
	public void addAll(IArray array, int count) {
		if (count == 0) {
		} else if (array instanceof SerialBytesArray) {
			ensureCapacity(size + count);
			SerialBytesArray sba = (SerialBytesArray)array;
			System.arraycopy(sba.datas1, 1, datas1, size + 1, count);
			System.arraycopy(sba.datas2, 1, datas2, size + 1, count);
			size += count;
		} else {
			int size = this.size;
			ensureCapacity(size + count);
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			
			for (int i = 1; i <= count; ++i) {
				size++;
				Object obj = array.get(i);
				if (obj instanceof SerialBytes) {
					SerialBytes sb = (SerialBytes)obj;
					datas1[size] = sb.getValue1();
					datas2[size] = sb.getValue2();
				} else if (obj == null) {
					datas1[size] = NULL;
					datas2[size] = NULL;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("pdm.arrayTypeError", 
							mm.getMessage("DataType.SerialBytes"), Variant.getDataType(obj)));
				}
			}
			
			this.size = size;
		}
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param index 要加入的数据的起始位置
	 * @param count 数量
	 */
	public void addAll(IArray array, int index, int count) {
		if (array instanceof SerialBytesArray) {
			ensureCapacity(size + count);
			SerialBytesArray sba = (SerialBytesArray)array;
			System.arraycopy(sba.datas1, index, datas1, size + 1, count);
			System.arraycopy(sba.datas2, index, datas2, size + 1, count);
			size += count;
		} else {
			int size = this.size;
			ensureCapacity(size + count);
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			
			for (int i = 1; i <= count; ++i, ++index) {
				size++;
				Object obj = array.get(index);
				if (obj instanceof SerialBytes) {
					SerialBytes sb = (SerialBytes)obj;
					datas1[size] = sb.getValue1();
					datas2[size] = sb.getValue2();
				} else if (obj == null) {
					datas1[size] = NULL;
					datas2[size] = NULL;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("pdm.arrayTypeError", 
							mm.getMessage("DataType.SerialBytes"), Variant.getDataType(obj)));
				}
			}
			
			this.size = size;
		}
	}
	
	/**
	 * 插入元素，如果类型不兼容则抛出异常
	 * @param index 插入位置，从1开始计数
	 * @param o 元素值
	 */
	public void insert(int index, Object o) {
		if (o instanceof SerialBytes) {
			ensureCapacity(size + 1);
			size++;
			
			System.arraycopy(datas1, index, datas1, index + 1, size - index);
			System.arraycopy(datas2, index, datas2, index + 1, size - index);
			
			SerialBytes sb = (SerialBytes)o;
			datas1[index] = sb.getValue1();
			datas2[index] = sb.getValue2();
		} else if (o == null) {
			ensureCapacity(size + 1);
			size++;
			
			System.arraycopy(datas1, index, datas1, index + 1, size - index);
			System.arraycopy(datas2, index, datas2, index + 1, size - index);
			datas1[index] = NULL;
			datas2[index] = NULL;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Long"), Variant.getDataType(o)));
		}
	}
	
	public void insert(int index, long value1, long value2) {
		ensureCapacity(size + 1);
		size++;
		
		System.arraycopy(datas1, index, datas1, index + 1, size - index);
		System.arraycopy(datas2, index, datas2, index + 1, size - index);

		datas1[index] = value1;
		datas2[index] = value2;
	}

	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, IArray array) {
		if (array instanceof SerialBytesArray) {
			int numNew = array.size();
			SerialBytesArray sbArray = (SerialBytesArray)array;
			ensureCapacity(size + numNew);
			
			System.arraycopy(datas1, pos, datas1, pos + numNew, size - pos + 1);
			System.arraycopy(datas2, pos, datas2, pos + numNew, size - pos + 1);
			System.arraycopy(sbArray.datas1, 1, datas1, pos, numNew);
			System.arraycopy(sbArray.datas2, 1, datas2, pos, numNew);
			size += numNew;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.SerialBytes"), array.getDataType()));
		}
	}

	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, Object[] array) {
		for (Object obj : array) {
			if (obj != null && !(obj instanceof SerialBytes)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.SerialBytes"), Variant.getDataType(obj)));
			}
		}

		int numNew = array.length;
		ensureCapacity(size + numNew);
		
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		System.arraycopy(datas1, pos, datas1, pos + numNew, size - pos + 1);
		System.arraycopy(datas2, pos, datas2, pos + numNew, size - pos + 1);			
		
		for (int i = 0; i < numNew; ++i, ++pos) {
			if (array[i] != null) {
				SerialBytes sb = (SerialBytes)array[i];
				datas1[pos] = sb.getValue1();
				datas2[pos] = sb.getValue2();
			} else {
				datas1[pos] = NULL;
				datas2[pos] = NULL;
			}
		}
		
		size += numNew;
	}
	
	/**
	 * 追加排号（不检查容量，认为有足够空间存放元素）
	 * @param value1 值1
	 * @param value2 值2
	 */
	public void push(long value1, long value2) {
		size++;
		datas1[size] = value1;
		datas2[size] = value1;
	}

	/**
	 * 追加元素（不检查容量，认为有足够空间存放元素），如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void push(Object o) {
		if (o instanceof SerialBytes) {
			size++;
			SerialBytes sb = (SerialBytes)o;
			datas1[size] = sb.getValue1();
			datas2[size] = sb.getValue2();
		} else if (o == null) {
			size++;
			datas1[size] = NULL;
			datas2[size] = NULL;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.SerialBytes"), Variant.getDataType(o)));
		}
	}

	/**
	 * 追加空元素
	 */
	public void pushNull() {
		size++;
		datas1[size] = NULL;
		datas2[size] = NULL;
	}

	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void push(IArray array, int index) {
		if (array instanceof SerialBytesArray) {
			SerialBytesArray sba = (SerialBytesArray)array;
			size++;
			datas1[size] = sba.datas1[index];
			datas2[size] = sba.datas2[index];
		} else {
			push(array.get(index));
		}
	}

	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void add(IArray array, int index) {
		if (array instanceof SerialBytesArray) {
			SerialBytesArray sba = (SerialBytesArray)array;
			ensureCapacity(size + 1);
			size++;
			datas1[size] = sba.datas1[index];
			datas2[size] = sba.datas2[index];
		} else {
			add(array.get(index));
		}
	}

	/**
	 * 把array中的第index个元素设给到当前数组的指定元素，如果类型不兼容则抛出异常
	 * @param curIndex 当前数组的元素索引，从1开始计数
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void set(int curIndex, IArray array, int index) {
		if (array instanceof SerialBytesArray) {
			SerialBytesArray sba = (SerialBytesArray)array;
			datas1[curIndex] = sba.datas1[index];
			datas2[curIndex] = sba.datas2[index];
		} else {
			set(curIndex, array.get(index));
		}
	}

	/**
	 * 取指定位置元素
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public Object get(int index) {
		if (datas1[index] != NULL || datas2[index] != NULL) {
			return new SerialBytes(datas1[index], datas2[index]);
		} else {
			return null;
		}
	}

	/**
	 * 取指定位置元素组成新数组
	 * @param indexArray 位置数组
	 * @return IArray
	 */
	public IArray get(int[] indexArray) {
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int len = indexArray.length;
		long []resultDatas1 = new long[len + 1];
		long []resultDatas2 = new long[len + 1];
		
		int seq = 1;
		for (int i : indexArray) {
			resultDatas1[seq] = datas1[i];
			resultDatas2[seq] = datas2[i];
			seq++;
		}
		
		return new SerialBytesArray(resultDatas1, resultDatas2, len);
	}

	/**
	 * 取指定位置元素组成新数组
	 * @param indexArray 位置数组
	 * @param start 起始位置，包含
	 * @param end 结束位置，包含
	 * @param doCheck true：位置可能包含0，0的位置用null填充，false：不会包含0
	 * @return IArray
	 */
	public IArray get(int[] indexArray, int start, int end, boolean doCheck) {
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int len = end - start + 1;
		long []resultDatas1 = new long[len + 1];
		long []resultDatas2 = new long[len + 1];
		
		if (doCheck) {
			for (int seq = 1; start <= end; ++start, ++seq) {
				int index = indexArray[start];
				if (index > 0) {
					resultDatas1[seq] = datas1[index];
					resultDatas2[seq] = datas2[index];
				} else {
					resultDatas1[seq] = NULL;
					resultDatas2[seq] = NULL;
				}
			}
		} else {
			for (int seq = 1; start <= end; ++start, ++seq) {
				resultDatas1[seq] = datas1[indexArray[start]];
				resultDatas2[seq] = datas2[indexArray[start]];
			}
		}
		
		return new SerialBytesArray(resultDatas1, resultDatas2, len);
	}

	/**
	 * 取某一区段组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @return IArray
	 */
	public IArray get(int start, int end) {
		int newSize = end - start;
		long []newDatas1 = new long[newSize + 1];
		long []newDatas2 = new long[newSize + 1];
		
		System.arraycopy(datas1, start, newDatas1, 1, newSize);
		System.arraycopy(datas2, start, newDatas2, 1, newSize);
		return new SerialBytesArray(newDatas1, newDatas2, newSize);
	}

	/**
	 * 取指定位置元素组成新数组
	 * @param IArray 位置数组
	 * @return IArray
	 */
	public IArray get(IArray indexArray) {
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int len = indexArray.size();
		long []resultDatas1 = new long[len + 1];
		long []resultDatas2 = new long[len + 1];
		
		for (int i = 1; i <= len; ++i) {
			if (indexArray.isNull(i)) {
				resultDatas1[i] = NULL;
				resultDatas2[i] = NULL;
			} else {
				resultDatas1[i] = datas1[indexArray.getInt(i)];
				resultDatas2[i] = datas2[indexArray.getInt(i)];
			}
		}

		return new SerialBytesArray(resultDatas1, resultDatas2, len);
	}

	public int getInt(int index) {
		throw new RuntimeException();
	}

	public long getLong(int index) {
		throw new RuntimeException();
	}

	/**
	 * 使列表的容量不小于minCapacity
	 * @param minCapacity 最小容量
	 */
	public void ensureCapacity(int minCapacity) {
		if (datas1.length <= minCapacity) {
			int newCapacity = (datas1.length * 3) / 2;
			if (newCapacity <= minCapacity) {
				newCapacity = minCapacity + 1;
			}

			long []newDatas1 = new long[newCapacity];
			long []newDatas2 = new long[newCapacity];
			System.arraycopy(datas1, 0, newDatas1, 0, size + 1);
			System.arraycopy(datas2, 0, newDatas2, 0, size + 1);
			datas1 = newDatas1;
			datas2 = newDatas2;
		}
	}

	/**
	 * 判断指定位置的元素是否是空
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isNull(int index) {
		return datas1[index] == NULL && datas2[index] == NULL;
	}

	/**
	 * 判断元素是否是True
	 * @return BoolArray
	 */
	public BoolArray isTrue() {
		int size = this.size;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		boolean []resultDatas = new boolean[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			if (datas1[i] != NULL || datas2[i] != NULL) {
				resultDatas[i] = true;
			}
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
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		boolean []resultDatas = new boolean[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			if (datas1[i] == NULL && datas2[i] == NULL) {
				resultDatas[i] = true;
			}
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
		return datas1[index] != NULL || datas2[index] != NULL;
	}

	/**
	 * 判断指定位置的元素是否是False
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isFalse(int index) {
		return datas1[index] == NULL && datas2[index] == NULL;
	}

	/**
	 * 是否是计算过程中临时产生的数组，临时产生的可以被修改，比如 f1+f2+f3，只需产生一个数组存放结果
	 * @return true：是临时产生的数组，false：不是临时产生的数组
	 */
	public boolean isTemporary() {
		return datas1[0] == 1;
	}

	/**
	 * 设置是否是计算过程中临时产生的数组
	 * @param ifTemporary true：是临时产生的数组，false：不是临时产生的数组
	 */
	public void setTemporary(boolean ifTemporary) {
		datas1[0] = ifTemporary ? 1 : 0;
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
		System.arraycopy(datas1, index + 1, datas1, index, size - index);
		System.arraycopy(datas2, index + 1, datas2, index, size - index);
		--size;
	}

	/**
	 * 删除指定位置的元素，序号从小到大排序
	 * @param seqs 索引数组
	 */
	public void remove(int[] seqs) {
		int delCount = 0;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
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
				System.arraycopy(datas1, cur + 1, datas1, cur - delCount, moveCount);
				System.arraycopy(datas2, cur + 1, datas2, cur - delCount, moveCount);
			}
			
			delCount++;
		}
		
		size -= delCount;
	}

	/**
	 * 删除指定区间内的元素
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 */
	public void removeRange(int fromIndex, int toIndex) {
		System.arraycopy(datas1, toIndex + 1, datas1, fromIndex, size - toIndex);
		System.arraycopy(datas2, toIndex + 1, datas2, fromIndex, size - toIndex);
		size -= (toIndex - fromIndex + 1);
	}

	public int size() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * 返回数组的非空元素数目
	 * @return 非空元素数目
	 */
	public int count() {
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int size = this.size;
		int count = size;
		
		for (int i = 1; i <= size; ++i) {
			if (datas1[i] == NULL && datas2[i] == NULL) {
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
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int size = this.size;
		for (int i = 1; i <= size; ++i) {
			if (datas1[i] != NULL || datas2[i] != NULL) {
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
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int size = this.size;
		for (int i = 1; i <= size; ++i) {
			if (datas1[i] != NULL || datas2[i] != NULL) {
				new SerialBytes(datas1[i], datas2[i]);
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
		if (obj instanceof SerialBytes) {
			SerialBytes sb = (SerialBytes)obj;
			datas1[index] = sb.getValue1();
			datas2[index] = sb.getValue2();
		} else if (obj == null) {
			datas1[index] = NULL;
			datas2[index] = NULL;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.SerialBytes"), Variant.getDataType(obj)));
		}
	}

	public void clear() {
		size = 0;
	}

	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @return 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem) {
		return binarySearch(elem, 1, size);
	}
	
	/**
	 * 二分法查找指定排号
	 * @param value1 排号的值1
	 * @param value2 排号的值2
	 * @param start 起始查找位置（包含）
	 * @param end 结束查找位置（包含）
	 * @return
	 */
	public int binarySearch(long value1, long value2, int start, int end) {
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int low = start, high = end;
		
		while (low <= high) {
			int mid = (low + high) >> 1;
			int cmp = SerialBytes.compare(datas1[mid], datas2[mid], value1, value2);
			
			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				return mid; // key found
			}
		}

		return -low; // key not found
	}
	
	// 数组按降序排序，进行降序二分查找
	private int descBinarySearch(long value1, long value2) {
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int low = 1, high = size;
		
		while (low <= high) {
			int mid = (low + high) >> 1;
			int cmp = SerialBytes.compare(datas1[mid], datas2[mid], value1, value2);
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
		if (elem instanceof SerialBytes) {
			SerialBytes sb = (SerialBytes)elem;
			long value1 = sb.getValue1();
			long value2 = sb.getValue2();
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			int low = start, high = end;
			
			while (low <= high) {
				int mid = (low + high) >> 1;
				int cmp = SerialBytes.compare(datas1[mid], datas2[mid], value1, value2);
				
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
			if (end > 0 && datas1[start] == NULL && datas1[end] == NULL) {
				return start;
			} else {
				return -1;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), elem,
					getDataType(), Variant.getDataType(elem)));
		}
	}

	/**
	 * 判断数组中是否包含指定排号
	 * @param value1
	 * @param value2
	 * @return
	 */
	public boolean contains(long value1, long value2) {
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int size = this.size;
		
		for (int i = 1; i <= size; ++i) {
			if (datas1[i] == value1 && datas2[i] == value2) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 返回列表中是否包含指定元素
	 * @param elem Object 待查找的元素
	 * @return boolean true：包含，false：不包含
	 */
	public boolean contains(Object elem) {
		if (elem instanceof SerialBytes) {
			SerialBytes sb = (SerialBytes)elem;
			long value1 = sb.getValue1();
			long value2 = sb.getValue2();
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			int size = this.size;
			
			for (int i = 1; i <= size; ++i) {
				if (datas1[i] == value1 && datas2[i] == value2) {
					return true;
				}
			}
			
			return false;
		} else if (elem == null) {
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			int size = this.size;
			
			for (int i = 1; i <= size; ++i) {
				if (datas1[i] == NULL && datas2[i] == NULL) {
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
		if (array instanceof SerialBytesArray) {
			SerialBytesArray sba = (SerialBytesArray)array;
			long []datas1 = sba.datas1;
			long []datas2 = sba.datas2;
			int size = this.size;
			
			if (isSorted) {
				for (int i = 1; i <= resultSize; ++i) {
					if (result.isTrue(i) && binarySearch(datas1[i], datas2[i], 1, size) < 1) {
						result.set(i, false);
					}
				}
			} else {
				for (int i = 1; i <= resultSize; ++i) {
					if (result.isTrue(i) && !contains(datas1[i], datas2[i])) {
						result.set(i, false);
					}
				}
			}
		} else {
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
	}

	/**
	 * 返回列表中是否包含指定元素，使用等号比较
	 * @param elem
	 * @return boolean true：包含，false：不包含
	 */
	public boolean objectContains(Object elem) {
		return false;
	}

	/**
	 * 返回元素在数组中首次出现的位置
	 * @param elem 待查找的元素
	 * @param start 起始查找位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	public int firstIndexOf(Object elem, int start) {
		if (elem instanceof SerialBytes) {
			SerialBytes sb = (SerialBytes)elem;
			long value1 = sb.getValue1();
			long value2 = sb.getValue2();
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			int size = this.size;
			
			for (int i = start; i <= size; ++i) {
				if (datas1[i] == value1 && datas2[i] == value2) {
					return i;
				}
			}
			
			return 0;
		} else if (elem == null) {
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			int size = this.size;
			
			for (int i = start; i <= size; ++i) {
				if (datas1[i] == NULL && datas2[i] == NULL) {
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
		if (elem instanceof SerialBytes) {
			SerialBytes sb = (SerialBytes)elem;
			long value1 = sb.getValue1();
			long value2 = sb.getValue2();
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			
			for (int i = start; i > 0; --i) {
				if (datas1[i] == value1 && datas2[i] == value2) {
					return i;
				}
			}
			
			return 0;
		} else if (elem == null) {
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			
			for (int i = start; i > 0; --i) {
				if (datas1[i] == NULL && datas2[i] == NULL) {
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
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;

		if (elem instanceof SerialBytes) {
			SerialBytes sb = (SerialBytes)elem;
			long value1 = sb.getValue1();
			long value2 = sb.getValue2();
			
			if (isSorted) {
				int end = size;
				if (isFromHead) {
					end = start;
					start = 1;
				}
				
				int index = binarySearch(sb, start, end);
				if (index < 1) {
					return new IntArray(1);
				}
				
				// 找到第一个
				int first = index;
				while (first > start && datas1[first - 1] == value1 && datas2[first - 1] == value2) {
					first--;
				}
				
				// 找到最后一个
				int last = index;
				while (last < end && datas1[last + 1] == value1 && datas2[last + 1] == value2) {
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
						if (datas1[i] == value1 && datas2[i] == value2) {
							result.addInt(i);
						}
					}
				} else {
					for (int i = start; i > 0; --i) {
						if (datas1[i] == value1 && datas2[i] == value2) {
							result.addInt(i);
						}
					}
				}
				
				return result;
			}
		} else if (elem == null) {
			IntArray result = new IntArray(7);
			if (isSorted) {
				if (isFromHead) {
					for (int i = start; i <= size; ++i) {
						if (datas1[i] == NULL && datas2[i] == NULL) {
							result.addInt(i);
						} else {
							break;
						}
					}
				} else {
					for (int i = start; i > 0; --i) {
						if (datas1[i] == NULL && datas2[i] == NULL) {
							result.addInt(i);
						}
					}
				}
			} else {
				if (isFromHead) {
					for (int i = start; i <= size; ++i) {
						if (datas1[i] == NULL && datas2[i] == NULL) {
							result.addInt(i);
						}
					}
				} else {
					for (int i = start; i > 0; --i) {
						if (datas1[i] == NULL && datas2[i] == NULL) {
							result.addInt(i);
						}
					}
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
		int len = size + 1;
		long []newDatas1 = new long[len];
		long []newDatas2 = new long[len];
		System.arraycopy(datas1, 0, newDatas1, 0, len);
		System.arraycopy(datas2, 0, newDatas2, 0, len);
		
		return new SerialBytesArray(newDatas1, newDatas2, size);
	}

	/**
	 * 返回一个同类型的数组
	 * @param count
	 * @return
	 */
	public IArray newInstance(int count) {
		return new SerialBytesArray(count);
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
		MessageManager mm = EngineMessage.get();
		throw new RuntimeException(getDataType() + mm.getMessage("Variant2.illNegate"));
	}

	/**
	 * 对数组成员求非
	 * @return IArray 非值数组
	 */
	public IArray not() {
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int size = this.size;
		
		boolean []newDatas = new boolean[size + 1];
		for (int i = 1; i <= size; ++i) {
			newDatas[i] = datas1[i] == NULL && datas2[i] == NULL;
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
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illAdd"));
	}

	/**
	 * 计算数组的成员与指定常数的和
	 * @param value 常数
	 * @return 和数组
	 */
	public IArray memberAdd(Object value) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				Variant.getDataType(value) + mm.getMessage("Variant2.illAdd"));
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
	 * 计算两个数组的相对应的成员的除
	 * @param array 右侧数组
	 * @return 商数组
	 */
	public IArray memberDivide(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illDivide"));
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
		if (array instanceof SerialBytesArray) {
			return calcRelation((SerialBytesArray)array, relation);
		} else if (array instanceof ConstArray) {
			return calcRelation(array.get(1), relation);
		} else if (array instanceof ObjectArray) {
			return calcRelation((ObjectArray)array, relation);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		}
	}

	/**
	 * 计算两个数组的相对应的成员的关系运算
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @return 关系运算结果数组
	 */
	public BoolArray calcRelation(Object value, int relation) {
		if (value instanceof SerialBytes) {
			int size = this.size;
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			SerialBytes sb = (SerialBytes)value;
			long value1 = sb.getValue1();
			long value2 = sb.getValue2();
			boolean []resultDatas = new boolean[size + 1];
			
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], value1, value2) == 0;
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], value1, value2) > 0;
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], value1, value2) >= 0;
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], value1, value2) < 0;
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], value1, value2) <= 0;
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], value1, value2) != 0;
				}
			} else if (relation == Relation.AND) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = (datas1[i] != NULL || datas2[i] != NULL) && (value1 != NULL || value2 != NULL);
				}
			} else { // Relation.OR
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = datas1[i] != NULL || datas2[i] != NULL || value1 != NULL || value2 != NULL;
				}
			}
			
			BoolArray result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (value == null) {
			boolean []resultDatas = new boolean[size + 1];		
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				for (int i = 1; i <= size; ++i) {
					if (datas1[i] == NULL && datas2[i] == NULL) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (datas1[i] == NULL || datas2[i] == NULL) {
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
					if (datas1[i] == NULL && datas2[i] == NULL) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (datas1[i] != NULL || datas2[i] != NULL) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.OR) {
				for (int i = 1; i <= size; ++i) {
					if (datas1[i] != NULL || datas2[i] != NULL) {
						resultDatas[i] = true;
					}
				}
			}
			
			BoolArray result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
					getDataType(), Variant.getDataType(value)));
		}
	}
	
	private BoolArray calcRelation(SerialBytesArray other, int relation) {
		int size = this.size;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		long []otherDatas1 = other.datas1;
		long []otherDatas2 = other.datas2;
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) == 0;
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) > 0;
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) >= 0;
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) < 0;
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) <= 0;
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) != 0;
			}
		} else if (relation == Relation.AND) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = (datas1[i] != NULL || datas2[i] != NULL) && (otherDatas1[i] != NULL || otherDatas2[i] != NULL);
			}
		} else { // Relation.OR
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = datas1[i] != NULL || datas2[i] != NULL || otherDatas1[i] != NULL || otherDatas2[i] != NULL;
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	protected BoolArray calcRelation(ObjectArray array, int relation) {
		int size = this.size;
		Object []d2 = array.getDatas();
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compareTo(i, d2[i]) == 0;
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compareTo(i, d2[i]) > 0;
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compareTo(i, d2[i]) >= 0;
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compareTo(i, d2[i]) < 0;
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compareTo(i, d2[i]) <= 0;
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compareTo(i, d2[i]) != 0;
			}
		} else if (relation == Relation.AND) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = isTrue(i) && Variant.isTrue(d2[i]);
			}
		} else { // Relation.OR
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = isTrue(i) || Variant.isTrue(d2[i]);
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}

	/**
	 * 计算两个数组的相对应的成员的关系运算，只计算result为真的行
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	public void calcRelations(IArray array, int relation, BoolArray result, boolean isAnd) {
		if (array instanceof SerialBytesArray) {
			calcRelations((SerialBytesArray)array, relation, result, isAnd);
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
		if (value instanceof SerialBytes) {
			int size = this.size;
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			SerialBytes sb = (SerialBytes)value;
			long value1 = sb.getValue1();
			long value2 = sb.getValue2();
			boolean []resultDatas = result.getDatas();
			
			if (isAnd) {
				// 与左侧结果执行&&运算
				if (relation == Relation.EQUAL) {
					// 是否等于判断
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] &&SerialBytes.compare(datas1[i], datas2[i], value1, value2) != 0) {
							resultDatas[i] = false;
						}
					}
				} else if (relation == Relation.GREATER) {
					// 是否大于判断
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) <= 0) {
							resultDatas[i] = false;
						}
					}
				} else if (relation == Relation.GREATER_EQUAL) {
					// 是否大于等于判断
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) < 0) {
							resultDatas[i] = false;
						}
					}
				} else if (relation == Relation.LESS) {
					// 是否小于判断
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) >= 0) {
							resultDatas[i] = false;
						}
					}
				} else if (relation == Relation.LESS_EQUAL) {
					// 是否小于等于判断
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) > 0) {
							resultDatas[i] = false;
						}
					}
				} else if (relation == Relation.NOT_EQUAL) {
					// 是否不等于判断
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) == 0) {
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
						if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) == 0) {
							resultDatas[i] = true;
						}
					}
				} else if (relation == Relation.GREATER) {
					// 是否大于判断
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) > 0) {
							resultDatas[i] = true;
						}
					}
				} else if (relation == Relation.GREATER_EQUAL) {
					// 是否大于等于判断
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) >= 0) {
							resultDatas[i] = true;
						}
					}
				} else if (relation == Relation.LESS) {
					// 是否小于判断
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) < 0) {
							resultDatas[i] = true;
						}
					}
				} else if (relation == Relation.LESS_EQUAL) {
					// 是否小于等于判断
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) <= 0) {
							resultDatas[i] = true;
						}
					}
				} else if (relation == Relation.NOT_EQUAL) {
					// 是否不等于判断
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], value1, value2) != 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					throw new RuntimeException();
				}
			}
		} else if (value == null) {
			long []datas1 = this.datas1;
			long []datas2 = this.datas2;
			boolean []resultDatas = result.getDatas();
			
			if (isAnd) {
				// 与左侧结果执行&&运算
				if (relation == Relation.EQUAL) {
					// 是否等于判断
					for (int i = 1; i <= size; ++i) {
						if (datas1[i] != NULL || datas2[i] != NULL) {
							resultDatas[i] = false;
						}
					}
				} else if (relation == Relation.GREATER) {
					// 是否大于判断
					for (int i = 1; i <= size; ++i) {
						if (datas1[i] == NULL && datas2[i] == NULL) {
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
						if (datas1[i] != NULL || datas2[i] != NULL) {
							resultDatas[i] = false;
						}
					}
				} else if (relation == Relation.NOT_EQUAL) {
					// 是否不等于判断
					for (int i = 1; i <= size; ++i) {
						if (datas1[i] == NULL && datas2[i] == NULL) {
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
						if (datas1[i] == NULL && datas2[i] == NULL) {
							resultDatas[i] = true;
						}
					}
				} else if (relation == Relation.GREATER) {
					// 是否大于判断
					for (int i = 1; i <= size; ++i) {
						if (datas1[i] != NULL || datas2[i] != NULL) {
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
						if (datas1[i] == NULL && datas2[i] == NULL) {
							resultDatas[i] = true;
						}
					}
				} else if (relation == Relation.NOT_EQUAL) {
					// 是否不等于判断
					for (int i = 1; i <= size; ++i) {
						if (datas1[i] != NULL || datas2[i] != NULL) {
							resultDatas[i] = true;
						}
					}
				} else {
					throw new RuntimeException();
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
					getDataType(), Variant.getDataType(value)));
		}
	}
	
	private void calcRelations(SerialBytesArray other, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		long []otherDatas1 = other.datas1;
		long []otherDatas2 = other.datas2;
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) != 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) <= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) < 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) >= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) > 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) == 0) {
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
					if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) == 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) > 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) >= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) < 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) <= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]) != 0) {
						resultDatas[i] = true;
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}
	
	protected void calcRelations(ObjectArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		Object []d2 = array.getDatas();
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compareTo(i, d2[i]) != 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compareTo(i, d2[i]) <= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compareTo(i, d2[i]) < 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compareTo(i, d2[i]) >= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compareTo(i, d2[i]) > 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compareTo(i, d2[i]) == 0) {
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
					if (!resultDatas[i] && compareTo(i, d2[i]) == 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compareTo(i, d2[i]) > 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compareTo(i, d2[i]) >= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compareTo(i, d2[i]) < 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compareTo(i, d2[i]) <= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compareTo(i, d2[i]) != 0) {
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
	 * 计算数组的2个成员的比较值
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	public int memberCompare(int index1, int index2) {
		return SerialBytes.compare(datas1[index1], datas2[index1], datas1[index2], datas2[index2]);
	}

	/**
	 * 判断数组的两个成员是否相等
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	public boolean isMemberEquals(int index1, int index2) {
		return datas1[index1] == datas1[index2] &&  datas2[index1] == datas2[index2];
	}

	/**
	 * 比较两个数组的大小
	 * @param array 右侧数组
	 * @return 1：当前数组大，0：两个数组相等，-1：当前数组小
	 */
	public int compareTo(IArray array) {
		int size1 = this.size;
		int size2 = array.size();
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
		int size = size1;
		int result = 0;
		if (size1 < size2) {
			result = -1;
		} else if (size1 > size2) {
			result = 1;
			size = size2;
		}

		if (array instanceof SerialBytesArray) {
			SerialBytesArray other = (SerialBytesArray)array;
			long []otherDatas1 = other.datas1;
			long []otherDatas2 = other.datas2;
			
			for (int i = 1; i <= size; ++i) {
				int cmp = SerialBytes.compare(datas1[i], datas2[i], otherDatas1[i], otherDatas2[i]);
				if (cmp != 0) {
					return cmp;
				}
			}
		} else if (array instanceof ConstArray) {
			Object value = array.get(1);
			if (value instanceof SerialBytes) {
				SerialBytes sb = (SerialBytes)value;
				long value1 = sb.getValue1();
				long value2 = sb.getValue2();
				
				for (int i = 1; i <= size; ++i) {
					int cmp = SerialBytes.compare(datas1[i], datas2[i], value1, value2);
					if (cmp != 0) {
						return cmp;
					}
				}
			} else if (value == null) {
				for (int i = 1; i <= size; ++i) {
					if (datas1[i] != NULL || datas2[i] != NULL) {
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
				int cmp = compareTo(i, d2[i]);
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
	 * 取指定成员的哈希值
	 * @param index 成员索引，从1开始计数
	 * @return 指定成员的哈希值
	 */
	public int hashCode(int index) {
		return HashUtil.hashCode(datas1[index] + datas2[index]);
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

		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		long max1 = datas1[1];
		long max2 = datas2[1];
		
		for (int i = 2; i <= size; ++i) {
			if (SerialBytes.compare(max1, max2, datas1[i], datas2[i]) < 0) {
				max1 = datas1[i];
				max2 = datas2[i];
			}
		}
		
		if (max1 != NULL || max2 != NULL) {
			return new SerialBytes(max1, max2);
		} else {
			return null;
		}
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

		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		long min1 = 0;
		long min2 = 0;
		
		int i = 1;
		for (; i <= size; ++i) {
			if (datas1[i] != NULL || datas2[i] != NULL) {
				min1 = datas1[i];
				min2 = datas2[i];
				break;
			}
		}
		
		for (++i; i <= size; ++i) {
			if ((datas1[i] != NULL || datas2[i] != NULL) && 
					SerialBytes.compare(min1, min2, datas1[i], datas2[i]) > 0) {
				min1 = datas1[i];
				min2 = datas2[i];
			}
		}
		
		if (min1 != NULL || min2 != NULL) {
			return new SerialBytes(min1, min2);
		} else {
			return null;
		}
	}

	/**
	 * 保留指定区间内的数据
	 * @param start 起始位置（包含）
	 * @param end 结束位置（包含）
	 */
	public void reserve(int start, int end) {
		int newSize = end - start + 1;
		System.arraycopy(datas1, start, datas1, 1, newSize);
		System.arraycopy(datas2, start, datas2, 1, newSize);
		size = newSize;
	}

	/**
	 * 把成员转成对象数组返回
	 * @return 对象数组
	 */
	public Object[] toArray() {
		int size = this.size;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		Object []result = new Object[size];
		
		for (int i = 1; i <= size; ++i) {
			if (datas1[i] != NULL || datas2[i] != NULL) {
				result[i - 1] = new SerialBytes(datas1[i], datas2[i]);
			}
		}
		
		return result;
	}

	/**
	 * 把成员填到指定的数组
	 * @param result 用于存放成员的数组
	 */
	public void toArray(Object []result) {
		int size = this.size;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;

		for (int i = 1; i <= size; ++i) {
			if (datas1[i] != NULL || datas2[i] != NULL) {
				result[i - 1] = new SerialBytes(datas1[i], datas2[i]);
			} else {
				result[i - 1] = null;
			}
		}
	}

	/**
	 * 把数组从指定位置拆成两个数组
	 * @param pos 位置，包含
	 * @return 返回后半部分元素构成的数组
	 */
	public IArray split(int pos) {
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		int size = this.size;
		int resultSize = size - pos + 1;
		
		long []resultDatas1 = new long[resultSize + 1];
		long []resultDatas2 = new long[resultSize + 1];
		System.arraycopy(datas1, pos, resultDatas1, 1, resultSize);
		System.arraycopy(datas2, pos, resultDatas2, 1, resultSize);
		
		
		this.size = pos - 1;
		return new SerialBytesArray(resultDatas1, resultDatas2, resultSize);
	}

	/**
	 * 把指定区间元素分离出来组成新数组
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 * @return
	 */
	public IArray split(int from, int to) {
		int oldSize = this.size;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
		int resultSize = to - from + 1;
		long []resultDatas1 = new long[resultSize + 1];
		long []resultDatas2 = new long[resultSize + 1];
		System.arraycopy(datas1, from, resultDatas1, 1, resultSize);
		System.arraycopy(datas2, from, resultDatas2, 1, resultSize);
		
		System.arraycopy(datas1, to + 1, datas1, from, oldSize - to);
		System.arraycopy(datas2, to + 1, datas2, from, oldSize - to);
		
		this.size -= resultSize;
		return new SerialBytesArray(resultDatas1, resultDatas2, resultSize);
	}

	/**
	 * 调整容量，使其与元素数相等
	 */
	public void trimToSize() {
		int newLen = size + 1;
		if (newLen < datas1.length) {
			long []newDatas1 = new long[newLen];
			long []newDatas2 = new long[newLen];
			System.arraycopy(datas1, 0, newDatas1, 0, newLen);
			System.arraycopy(datas2, 0, newDatas2, 0, newLen);
			datas1 = newDatas1;
			datas2 = newDatas2;
		}
	}

	/**
	 * 取出标识数组取值为真的行对应的数据，组成新数组
	 * @param signArray 标识数组
	 * @return IArray
	 */
	public IArray select(IArray signArray) {
		int size = signArray.size();
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;

		long []newDatas1 = new long[size + 1];
		long []newDatas2 = new long[size + 1];
		int count = 0;
		
		if (signArray instanceof BoolArray) {
			BoolArray array = (BoolArray)signArray;
			boolean []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (d2[i]) {
						++count;
						newDatas1[count] = datas1[i];
						newDatas2[count] = datas2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i] && d2[i]) {
						++count;
						newDatas1[count] = datas1[i];
						newDatas2[count] = datas2[i];
					}
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (signArray.isTrue(i)) {
					++count;
					newDatas1[count] = datas1[i];
					newDatas2[count] = datas2[i];
				}
			}
		}
		
		return new SerialBytesArray(newDatas1, newDatas2, count);
	}
	
	/**
	 * 取某一区段标识数组取值为真的行组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @param signArray 标识数组
	 * @return IArray
	 */
	public IArray select(int start, int end, IArray signArray) {
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		long []newDatas1 = new long[end - start + 1];
		long []newDatas2 = new long[end - start + 1];
		int count = 0;
		
		if (signArray instanceof BoolArray) {
			BoolArray array = (BoolArray)signArray;
			boolean []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			if (s2 == null) {
				for (int i = start; i < end; ++i) {
					if (d2[i]) {
						++count;
						newDatas1[count] = datas1[i];
						newDatas2[count] = datas2[i];
					}
				}
			} else {
				for (int i = start; i < end; ++i) {
					if (!s2[i] && d2[i]) {
						++count;
						newDatas1[count] = datas1[i];
						newDatas2[count] = datas2[i];
					}
				}
			}
		} else {
			for (int i = start; i < end; ++i) {
				if (signArray.isTrue(i)) {
					++count;
					newDatas1[count] = datas1[i];
					newDatas2[count] = datas2[i];
				}
			}
		}
		
		return new SerialBytesArray(newDatas1, newDatas2, count);
	}

	/**
	 * 判断两个数组的指定元素是否相同
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要比较的数组
	 * @param index 要比较的数组的元素的索引
	 * @return true：相同，false：不相同
	 */
	public boolean isEquals(int curIndex, IArray array, int index) {
		if (array instanceof SerialBytesArray) {
			SerialBytesArray sba = (SerialBytesArray)array;
			return datas1[curIndex] == sba.datas1[index] && datas2[curIndex] == sba.datas2[index];
		} else {
			return isEquals(curIndex, array.get(index));
		}
	}

	/**
	 * 判断数组的指定元素是否与给定值相等
	 * @param curIndex 数组元素索引，从1开始计数
	 * @param value 值
	 * @return true：相等，false：不相等
	 */
	public boolean isEquals(int curIndex, Object value) {
		if (value instanceof SerialBytes) {
			SerialBytes sb = (SerialBytes)value;
			return datas1[curIndex] == sb.getValue1() && datas2[curIndex] == sb.getValue2();
		} else if (value == null) {
			return datas1[curIndex] == NULL && datas2[curIndex] == NULL;
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
		if (array instanceof SerialBytesArray) {
			SerialBytesArray sba = (SerialBytesArray)array;
			return SerialBytes.compare(datas1[curIndex], datas2[curIndex], sba.datas1[index], sba.datas2[index]);
		} else {
			return compareTo(curIndex, array.get(index));
		}
	}

	/**
	 * 比较数组的指定元素与给定值的大小
	 * @param curIndex 当前数组的元素的索引
	 * @param value 要比较的值
	 * @return
	 */
	public int compareTo(int curIndex, Object value) {
		if (value instanceof SerialBytes) {
			SerialBytes sb = (SerialBytes)value;
			return SerialBytes.compare(datas1[curIndex], datas2[curIndex], sb.getValue1(), sb.getValue2());
		} else if (value == null) {
			return datas1[curIndex] != NULL || datas2[curIndex] != NULL ? 1 : 0;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(curIndex), value,
					mm.getMessage("DataType.SerialBytes"), Variant.getDataType(value)));
		}
	}

	/**
	 * 对数组的元素进行排序
	 */
	public void sort() {
		SerialBytes []sbs = new SerialBytes[size];
		toArray(sbs);
		MultithreadUtil.sort(sbs);
		
		int i = 0;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
		for (SerialBytes sb : sbs) {
			i++;
			if (sb == null) {
				datas1[i] = NULL;
				datas2[i] = NULL;
			} else {
				datas1[i] = sb.getValue1();
				datas2[i] = sb.getValue2();
			}
		}
	}

	/**
	 * 对数组的元素进行排序
	 * @param comparator 比较器
	 */
	public void sort(Comparator<Object> comparator) {
		SerialBytes []sbs = new SerialBytes[size];
		toArray(sbs);
		MultithreadUtil.sort(sbs, comparator);
		
		int i = 0;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
		for (SerialBytes sb : sbs) {
			i++;
			if (sb == null) {
				datas1[i] = NULL;
				datas2[i] = NULL;
			} else {
				datas1[i] = sb.getValue1();
				datas2[i] = sb.getValue2();
			}
		}
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
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		long []resultDatas1 = new long[size + 1];
		long []resultDatas2 = new long[size + 1];
		
		for (int i = 1, q = size; i <= size; ++i, --q) {
			resultDatas1[i] = datas1[q];
			resultDatas2[i] = datas2[q];
		}
		
		return new SerialBytesArray(resultDatas1, resultDatas2, size);
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
		
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
		if (ignoreNull) {
			if (count == 1) {
				// 取最小值的位置
				long minValue1 = NULL;
				long minValue2 = NULL;
				
				if (isAll) {
					IntArray result = new IntArray(8);
					int i = 1;
					for (; i <= size; ++i) {
						if (datas1[i] != NULL || datas2[i] != NULL) {
							minValue1 = datas1[i];
							minValue2 = datas2[i];
							result.addInt(i);
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (datas1[i] != NULL || datas2[i] != NULL) {
							int cmp = SerialBytes.compare(datas1[i], datas2[i], minValue1, minValue2);
							if (cmp < 0) {
								minValue1 = datas1[i];
								minValue2 = datas2[i];
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
						if (datas1[i] != NULL || datas2[i] != NULL) {
							minValue1 = datas1[i];
							minValue2 = datas2[i];
							pos = i;
							break;
						}
					}
					
					for (--i; i > 0; --i) {
						if ((datas1[i] != NULL || datas2[i] != NULL) && 
								SerialBytes.compare(datas1[i], datas2[i], minValue1, minValue2) < 0) {
							minValue1 = datas1[i];
							minValue2 = datas2[i];
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
						if (datas1[i] != NULL || datas2[i] != NULL) {
							minValue1 = datas1[i];
							minValue2 = datas2[i];
							pos = i;
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if ((datas1[i] != NULL || datas2[i] != NULL) && 
								SerialBytes.compare(datas1[i], datas2[i], minValue1, minValue2) < 0) {
							minValue1 = datas1[i];
							minValue2 = datas2[i];
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
				SerialBytesArray valueArray = new SerialBytesArray(next);
				IntArray posArray = new IntArray(next);
				for (int i = 1; i <= size; ++i) {
					if (datas1[i] != NULL || datas2[i] != NULL) {
						int index = valueArray.binarySearch(datas1[i], datas2[i], 1, valueArray.size);
						if (index < 1) {
							index = -index;
						}
						
						if (index <= count) {
							valueArray.insert(index, datas1[i], datas2[i]);
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
				long maxValue1 = NULL;
				long maxValue2 = NULL;

				if (isAll) {
					IntArray result = new IntArray(8);
					int i = 1;
					for (; i <= size; ++i) {
						if (datas1[i] != NULL || datas2[i] != NULL) {
							maxValue1 = datas1[i];
							maxValue2 = datas2[i];
							result.addInt(i);
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (datas1[i] != NULL || datas2[i] != NULL) {
							int cmp = SerialBytes.compare(datas1[i], datas2[i], maxValue1, maxValue2);
							if (cmp > 0) {
								maxValue1 = datas1[i];
								maxValue2 = datas2[i];
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
						if (datas1[i] != NULL || datas2[i] != NULL) {
							maxValue1 = datas1[i];
							maxValue2 = datas2[i];
							pos = i;
							break;
						}
					}
					
					for (--i; i > 0; --i) {
						if ((datas1[i] != NULL || datas2[i] != NULL) && 
								SerialBytes.compare(datas1[i], datas2[i], maxValue1, maxValue2) > 0) {
							maxValue1 = datas1[i];
							maxValue2 = datas2[i];
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
						if (datas1[i] != NULL || datas2[i] != NULL) {
							maxValue1 = datas1[i];
							maxValue2 = datas2[i];
							pos = i;
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if ((datas1[i] != NULL || datas2[i] != NULL) && 
								SerialBytes.compare(datas1[i], datas2[i], maxValue1, maxValue2) > 0) {
							maxValue1 = datas1[i];
							maxValue2 = datas2[i];
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
				SerialBytesArray valueArray = new SerialBytesArray(next);
				IntArray posArray = new IntArray(next);
				
				for (int i = 1; i <= size; ++i) {
					if (datas1[i] != NULL || datas2[i] != NULL) {
						int index = valueArray.descBinarySearch(datas1[i], datas2[i]);
						if (index < 1) {
							index = -index;
						}
						
						if (index <= count) {
							valueArray.insert(index, datas1[i], datas2[i]);
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
					long minValue1 = datas1[1];
					long minValue2 = datas2[1];
					
					for (int i = 2; i <= size; ++i) {
						int cmp = SerialBytes.compare(datas1[i], datas2[i], minValue1, minValue2);
						if (cmp < 0) {
							minValue1 = datas1[i];
							minValue2 = datas2[i];
							result.clear();
							result.addInt(i);
						} else if (cmp == 0) {
							result.addInt(i);
						}
					}
					
					return result;
				} else if (isLast) {
					long minValue1 = datas1[size];
					long minValue2 = datas2[size];
					int pos = size;
					
					for (int i = size - 1; i > 0; --i) {
						if (SerialBytes.compare(datas1[i], datas2[i], minValue1, minValue2) < 0) {
							minValue1 = datas1[i];
							minValue2 = datas2[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					result.pushInt(pos);
					return result;
				} else {
					long minValue1 = datas1[1];
					long minValue2 = datas2[1];
					int pos = 1;
					
					for (int i = 2; i <= size; ++i) {
						if (SerialBytes.compare(datas1[i], datas2[i], minValue1, minValue2) < 0) {
							minValue1 = datas1[i];
							minValue2 = datas2[i];
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
				SerialBytesArray valueArray = new SerialBytesArray(next);
				IntArray posArray = new IntArray(next);
				
				for (int i = 1; i <= size; ++i) {
					int index = valueArray.binarySearch(datas1[i], datas2[i], 1, valueArray.size);
					if (index < 1) {
						index = -index;
					}
					
					if (index <= count) {
						valueArray.insert(index, datas1[i], datas2[i]);
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
					long maxValue1 = datas1[1];
					long maxValue2 = datas2[1];
					result.addInt(1);
					
					for (int i = 2; i <= size; ++i) {
						int cmp = SerialBytes.compare(datas1[i], datas2[i], maxValue1, maxValue2);
						if (cmp > 0) {
							maxValue1 = datas1[i];
							maxValue2 = datas2[i];
							result.clear();
							result.addInt(i);
						} else if (cmp == 0) {
							result.addInt(i);
						}
					}
					
					return result;
				} else if (isLast) {
					long maxValue1 = datas1[size];
					long maxValue2 = datas2[size];
					int pos = size;
					
					for (int i = size - 1; i > 0; --i) {
						if (SerialBytes.compare(datas1[i], datas2[i], maxValue1, maxValue2) > 0) {
							maxValue1 = datas1[i];
							maxValue2 = datas2[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					result.pushInt(pos);
					return result;
				} else {
					long maxValue1 = datas1[1];
					long maxValue2 = datas2[1];
					int pos = 1;
					
					for (int i = 2; i <= size; ++i) {
						if (SerialBytes.compare(datas1[i], datas2[i], maxValue1, maxValue2) > 0) {
							maxValue1 = datas1[i];
							maxValue2 = datas2[i];
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
				SerialBytesArray valueArray = new SerialBytesArray(next);
				IntArray posArray = new IntArray(next);
				
				for (int i = 1; i <= size; ++i) {
					int index = valueArray.descBinarySearch(datas1[i], datas2[i]);
					if (index < 1) {
						index = -index;
					}
					
					if (index <= count) {
						valueArray.insert(index, datas1[i], datas2[i]);
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

	/**
	 * 把当前数组转成对象数组，如果当前数组是对象数组则返回数组本身
	 * @return ObjectArray
	 */
	public ObjectArray toObjectArray() {
		int size = this.size;
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		Object []resultDatas = new Object[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			if (datas1[i] != NULL || datas2[i] != NULL) {
				resultDatas[i] = new SerialBytes(datas1[i], datas2[i]);
			}
		}
		
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
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
		if (other instanceof SerialBytesArray) {
			SerialBytesArray sba = (SerialBytesArray)other;
			long []otherDatas1 = sba.datas1;
			long []otherDatas2 = sba.datas2;
			
			if (isTemporary()) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						datas1[i] = otherDatas1[i];
						datas2[i] = otherDatas2[i];
					}
				}
				
				return this;
			} else {
				long []resultDatas1 = new long[size + 1];
				long []resultDatas2 = new long[size + 1];
				System.arraycopy(datas1, 1, resultDatas1, 1, size);
				System.arraycopy(datas2, 1, resultDatas2, 1, size);
				
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						resultDatas1[i] = otherDatas1[i];
						resultDatas2[i] = otherDatas2[i];
					}
				}
				
				IArray result = new SerialBytesArray(resultDatas1, resultDatas2, size);
				result.setTemporary(true);
				return result;
			}
		} else {
			Object []resultDatas = new Object[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (signArray.isFalse(i)) {
					resultDatas[i] = other.get(i);
				} else {
					resultDatas[i] = get(i);
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
		long []datas1 = this.datas1;
		long []datas2 = this.datas2;
		
		if (value instanceof SerialBytes || value == null) {
			long value1 = NULL;
			long value2 = NULL;
			
			if (value != null) {
				SerialBytes sb = (SerialBytes)value;
				value1 = sb.getValue1();
				value2 = sb.getValue2();
			}
			
			if (isTemporary()) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						datas1[i] = value1;
						datas2[i] = value2;
					}
				}
				
				return this;
			} else {
				long []resultDatas1 = new long[size + 1];
				long []resultDatas2 = new long[size + 1];
				System.arraycopy(datas1, 1, resultDatas1, 1, size);
				System.arraycopy(datas2, 1, resultDatas2, 1, size);
				
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						resultDatas1[i] = value1;
						resultDatas2[i] = value2;
					}
				}
				
				IArray result = new SerialBytesArray(resultDatas1, resultDatas2, size);
				result.setTemporary(true);
				return result;
			}
		} else {
			Object []resultDatas = new Object[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (signArray.isFalse(i)) {
					resultDatas[i] = value;
				} else {
					resultDatas[i] = get(i);
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	public long[] getDatas1() {
		return datas1;
	}

	public long[] getDatas2() {
		return datas2;
	}
	
	public long getData1(int index) {
		return datas1[index];
	}

	public long getData2(int index) {
		return datas2[index];
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

	/**
	 * 返回数组成员按位异或值的二进制表示时1的个数和
	 * @param array 异或数组
	 * @return 1的个数和
	 */
	public int bit1(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("bit1" + mm.getMessage("function.paramTypeError"));
	}
}
