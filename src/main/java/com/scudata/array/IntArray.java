package com.scudata.array;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Date;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Sequence;
import com.scudata.expression.Relation;
import com.scudata.expression.fn.math.Bit1;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.MultithreadUtil;
import com.scudata.util.Variant;

/**
 * 整数数组，从1开始计数
 * @author WangXiaoJun
 *
 */
public class IntArray implements NumberArray {
	private static final long serialVersionUID = 1L;
	private static final byte NULL_SIGN = 60; // 用于序列化时表示signs是否为空
	
	private int []datas; // 第0个元素表示是否是临时数组
	private boolean []signs; // 表示相应位置的元素是否是null（true表示null），有null成员时才产生
	private int size; // 元素数量

	public IntArray() {
		datas = new int[DEFAULT_LEN];
	}
	
	public IntArray(int initialCapacity) {
		datas = new int[++initialCapacity];
	}
	
	/**
	 * 由区间创建一个数组
	 * @param startValue 起始值（包含）
	 * @param endValue 结束值（包含）
	 */
	public IntArray(int startValue, int endValue) {
		size = endValue - startValue + 1;
		datas = new int[size + 1];
		
		for (int i = 1; startValue <= endValue; ++startValue, ++i) {
			datas[i] = startValue;
		}
	}
	
	public IntArray(int []datas, boolean []signs, int size) {
		this.datas = datas;
		this.signs = signs;
		this.size = size;
	}
	
	/**
	 * 比较指定数和指定对象的大小，null最小
	 * @param n1 左值
	 * @param o2 右值
	 * @return 1：左值大，0：同样大，-1：右值大
	 */
	private static int compare(int n1, Object o2) {
		if (o2 instanceof Integer) {
			int n2 = ((Integer)o2).intValue();
			return (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
		} else if (o2 instanceof Long) {
			long n2 = ((Long)o2).longValue();
			return (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
		} else if (o2 instanceof BigDecimal) {
			return new BigDecimal(n1).compareTo((BigDecimal)o2);
		} else if (o2 instanceof BigInteger) {
			return BigInteger.valueOf(n1).compareTo((BigInteger)o2);
		} else if (o2 instanceof Number) {
			return Double.compare(n1, ((Number)o2).doubleValue());
		} else if (o2 == null) {
			return 1;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", n1, o2,
					mm.getMessage("DataType.Integer"), Variant.getDataType(o2)));
		}
	}
	
	private static int compare(int n1, int n2) {
		return n1 < n2 ? -1 : (n1 > n2 ? 1 : 0);
	}
	
	public int[] getDatas() {
		return datas;
	}
	
	public boolean[] getSigns() {
		return signs;
	}
	
	public void setSigns(boolean []signs) {
		this.signs = signs;
	}

	/**
	 * 取数组的类型串，用于错误信息提示
	 * @return 类型串
	 */
	public String getDataType() {
		MessageManager mm = EngineMessage.get();
		return mm.getMessage("DataType.Integer");
	}
	
	/**
	 * 复制数组
	 * @return
	 */
	public IArray dup() {
		int len = size + 1;
		int []newDatas = new int[len];
		System.arraycopy(datas, 0, newDatas, 0, len);
		
		boolean []newSigns = null;
		if (signs != null) {
			newSigns = new boolean[len];
			System.arraycopy(signs, 0, newSigns, 0, len);
		}
		
		return new IntArray(newDatas, newSigns, size);
	}
	
	/**
	 * 写内容到流
	 * @param out 输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			out.writeByte(1);
		} else {
			out.writeByte(NULL_SIGN + 1);
		}
		
		out.writeInt(size);
		for (int i = 1; i <= size; ++i) {
			out.writeInt(datas[i]);
		}
		
		if (signs != null) {
			for (int i = 1; i <= size; ++i) {
				out.writeBoolean(signs[i]);
			}
		}
	}
	
	/**
	 * 从流中读内容
	 * @param in 输入流
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte sign = in.readByte();
		size = in.readInt();
		int len = size + 1;
		int []datas = this.datas = new int[len];
		
		for (int i = 1; i < len; ++i) {
			datas[i] = in.readInt();
		}
		
		if (sign > NULL_SIGN) {
			boolean []signs = this.signs = new boolean[len];
			for (int i = 1; i < len; ++i) {
				signs[i] = in.readBoolean();
			}
		}
	}
	
	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			out.writeByte(1);
		} else {
			out.writeByte(NULL_SIGN + 1);
		}
		
		out.writeInt(size);
		for (int i = 1; i <= size; ++i) {
			out.writeInt(datas[i]);
		}
		
		if (signs != null) {
			for (int i = 1; i <= size; ++i) {
				out.writeBoolean(signs[i]);
			}
		}

		return out.toByteArray();
	}
	
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		byte sign = in.readByte();
		size = in.readInt();
		int len = size + 1;
		int []datas = this.datas = new int[len];
		
		for (int i = 1; i < len; ++i) {
			datas[i] = in.readInt();
		}
		
		if (sign > NULL_SIGN) {
			boolean []signs = this.signs = new boolean[len];
			for (int i = 1; i < len; ++i) {
				signs[i] = in.readBoolean();
			}
		}
	}
	
	/**
	 * 返回一个同类型的数组
	 * @param count
	 * @return
	 */
	public IArray newInstance(int count) {
		return new IntArray(count);
	}

	/**
	 * 追加元素，如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void add(Object o) {
		if (o instanceof Integer) {
			ensureCapacity(size + 1);
			datas[++size] = ((Integer)o).intValue();
		} else if (o == null) {
			ensureCapacity(size + 1);
			if (signs == null) {
				signs = new boolean[datas.length];
			}
			
			signs[++size] = true;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Integer"), Variant.getDataType(o)));
		}
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(IArray array) {
		int size2 = array.size();
		if (size2 == 0) {
		} else if (array instanceof IntArray) {
			IntArray intArray = (IntArray)array;
			ensureCapacity(size + size2);
			
			System.arraycopy(intArray.datas, 1, datas, size + 1, size2);
			if (intArray.signs != null) {
				if (signs == null) {
					signs = new boolean[datas.length];
				}
				
				System.arraycopy(intArray.signs, 1, signs, size + 1, size2);
			}
			
			size += size2;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof Integer) {
				ensureCapacity(size + size2);
				int v = ((Number)obj).intValue();
				int []datas = this.datas;
				
				for (int i = 0; i < size2; ++i) {
					datas[++size] = v;
				}
			} else if (obj == null) {
				ensureCapacity(size + size2);
				boolean []signs = this.signs;
				if (signs == null) {
					this.signs = signs = new boolean[datas.length];
				}
				
				for (int i = 0; i < size2; ++i) {
					signs[++size] = true;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.Integer"), array.getDataType()));
			}
		} else {
			//MessageManager mm = EngineMessage.get();
			//throw new RQException(mm.getMessage("pdm.arrayTypeError", 
			//		mm.getMessage("DataType.Integer"), array.getDataType()));
			ensureCapacity(size + size2);
			int []datas = this.datas;
			
			for (int i = 1; i <= size2; ++i) {
				Object obj = array.get(i);
				if (obj instanceof Integer) {
					datas[++size] = ((Integer)obj).intValue();
				} else if (obj == null) {
					if (signs == null) {
						signs = new boolean[datas.length];
					}
					
					signs[++size] = true;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("pdm.arrayTypeError", 
							mm.getMessage("DataType.Integer"), Variant.getDataType(obj)));
				}
			}
		}
	}

	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param count 元素个数
	 */
	public void addAll(IArray array, int count) {
		if (count == 0) {
		} else if (array instanceof IntArray) {
			IntArray intArray = (IntArray)array;
			ensureCapacity(size + count);
			
			System.arraycopy(intArray.datas, 1, datas, size + 1, count);
			if (intArray.signs != null) {
				if (signs == null) {
					signs = new boolean[datas.length];
				}
				
				System.arraycopy(intArray.signs, 1, signs, size + 1, count);
			}
			
			size += count;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof Integer) {
				ensureCapacity(size + count);
				int v = ((Number)obj).intValue();
				int []datas = this.datas;
				
				for (int i = 0; i < count; ++i) {
					datas[++size] = v;
				}
			} else if (obj == null) {
				ensureCapacity(size + count);
				boolean []signs = this.signs;
				if (signs == null) {
					this.signs = signs = new boolean[datas.length];
				}
				
				for (int i = 0; i < count; ++i) {
					signs[++size] = true;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.Integer"), array.getDataType()));
			}
		} else {
			//MessageManager mm = EngineMessage.get();
			//throw new RQException(mm.getMessage("pdm.arrayTypeError", 
			//		mm.getMessage("DataType.Integer"), array.getDataType()));
			ensureCapacity(size + count);
			int []datas = this.datas;
			
			for (int i = 1; i <= count; ++i) {
				Object obj = array.get(i);
				if (obj instanceof Integer) {
					datas[++size] = ((Integer)obj).intValue();
				} else if (obj == null) {
					if (signs == null) {
						signs = new boolean[datas.length];
					}
					
					signs[++size] = true;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("pdm.arrayTypeError", 
							mm.getMessage("DataType.Integer"), Variant.getDataType(obj)));
				}
			}
		}
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param index 要加入的数据的起始位置
	 * @param count 数量
	 */
	public void addAll(IArray array, int index, int count) {
		if (array instanceof IntArray) {
			IntArray intArray = (IntArray)array;
			ensureCapacity(size + count);
			
			System.arraycopy(intArray.datas, index, datas, size + 1, count);
			if (intArray.signs != null) {
				if (signs == null) {
					signs = new boolean[datas.length];
				}
				
				System.arraycopy(intArray.signs, index, signs, size + 1, count);
			}
			
			size += count;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof Integer) {
				ensureCapacity(size + count);
				int v = ((Number)obj).intValue();
				int []datas = this.datas;
				
				for (int i = 0; i < count; ++i) {
					datas[++size] = v;
				}
			} else if (obj == null) {
				ensureCapacity(size + count);
				boolean []signs = this.signs;
				if (signs == null) {
					this.signs = signs = new boolean[datas.length];
				}
				
				for (int i = 0; i < count; ++i) {
					signs[++size] = true;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.Integer"), array.getDataType()));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Integer"), array.getDataType()));
		}
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(Object []array) {
		for (Object obj : array) {
			if (obj != null && !(obj instanceof Integer)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.Integer"), Variant.getDataType(obj)));
			}
		}
		
		int size2 = array.length;
		ensureCapacity(size + size2);
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		for (int i = 0; i < size2; ++i) {
			if (array[i] != null) {
				datas[++size] = ((Number)array[i]).intValue();
			} else {
				if (signs == null) {
					this.signs = signs = new boolean[datas.length];
				}
				
				signs[++size] = true;
			}
		}
	}
	
	/**
	 * 插入元素
	 * @param index 插入位置，从1开始计数
	 * @param o 元素值
	 * @return 返回源数组
	 */
	public IArray insertInt(int index, int o) {
		ensureCapacity(size + 1);
		
		size++;
		System.arraycopy(datas, index, datas, index + 1, size - index);
		datas[index] = o;
		
		if (signs != null) {
			System.arraycopy(signs, index, signs, index + 1, size - index);
			signs[index] = false;
		}
		
		return this;
	}
	
	/**
	 * 插入元素，如果类型不兼容则抛出异常
	 * @param index 插入位置，从1开始计数
	 * @param o 元素值
	 */
	public void insert(int index, Object o) {
		if (o instanceof Integer) {
			ensureCapacity(size + 1);
			
			size++;
			System.arraycopy(datas, index, datas, index + 1, size - index);
			datas[index] = ((Integer)o).intValue();
			
			if (signs != null) {
				System.arraycopy(signs, index, signs, index + 1, size - index);
				signs[index] = false;
			}
		} else if (o == null) {
			ensureCapacity(size + 1);
			
			size++;
			System.arraycopy(datas, index, datas, index + 1, size - index);
			
			if (signs == null) {
				signs = new boolean[datas.length];
			} else {
				System.arraycopy(signs, index, signs, index + 1, size - index);
			}
			
			datas[index] = 0;
			signs[index] = true;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Integer"), Variant.getDataType(o)));
		}
	}
	
	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, IArray array) {
		if (array instanceof IntArray) {
			int numNew = array.size();
			IntArray intArray = (IntArray)array;
			ensureCapacity(size + numNew);
			
			System.arraycopy(datas, pos, datas, pos + numNew, size - pos + 1);
			if (signs != null) {
				System.arraycopy(signs, pos, signs, pos + numNew, size - pos + 1);
			}
			
			System.arraycopy(intArray.datas, 1, datas, pos, numNew);
			if (intArray.signs == null) {
				if (signs != null) {
					boolean []signs = this.signs;
					for (int i = 0; i < numNew; ++i) {
						signs[pos + i] = false;
					}
				}
			} else {
				if (signs == null) {
					signs = new boolean[datas.length];
				}
				
				System.arraycopy(intArray.signs, 1, signs, pos, numNew);
			}
			
			size += numNew;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Integer"), array.getDataType()));
		}
	}
	
	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, Object []array) {
		boolean containNull = false;
		for (Object obj : array) {
			if (obj == null) {
				containNull = true;
			} else if (!(obj instanceof Integer)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.Integer"), Variant.getDataType(obj)));
			}
		}

		int numNew = array.length;
		ensureCapacity(size + numNew);
		
		int []datas = this.datas;
		boolean []signs = this.signs;
		System.arraycopy(datas, pos, datas, pos + numNew, size - pos + 1);
		if (signs != null) {
			System.arraycopy(signs, pos, signs, pos + numNew, size - pos + 1);
		}
		
		if (containNull) {
			if (signs == null) {
				this.signs = signs = new boolean[datas.length];
			}
			
			for (int i = 0; i < numNew; ++i) {
				if (array[i] == null) {
					signs[pos + i] = true;
				} else {
					datas[pos + i] = (Integer)array[i];
					signs[pos + i] = false;
				}
			}
		} else {
			for (int i = 0; i < numNew; ++i) {
				datas[pos + i] = (Integer)array[i];
				if (signs != null) {
					signs[pos + i] = false;
				}
			}
		}
		
		size += numNew;
	}
	
	/**
	 * 追加元素（不检查容量，认为有足够空间存放元素），如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void push(Object o) {
		if (o instanceof Integer) {
			datas[++size] = ((Integer)o).intValue();
		} else if (o == null) {
			if (signs == null) {
				signs = new boolean[datas.length];
			}
			
			signs[++size] = true;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Integer"), Variant.getDataType(o)));
		}
	}
	
	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void push(IArray array, int index) {
		if (array instanceof IntArray) {
			if (array.isNull(index)) {
				if (signs == null) {
					signs = new boolean[datas.length];
				}
				
				signs[++size] = true;
			} else {
				datas[++size] = array.getInt(index);
			}
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
		if (array.isNull(index)) {
			ensureCapacity(size + 1);
			if (signs == null) {
				signs = new boolean[datas.length];
			}
			
			signs[++size] = true;
		} else if (array instanceof IntArray) {
			ensureCapacity(size + 1);
			datas[++size] = array.getInt(index);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Integer"), array.getDataType()));
		}
	}
	
	/**
	 * 把array中的第index个元素设给到当前数组的指定元素，如果类型不兼容则抛出异常
	 * @param curIndex 当前数组的元素索引，从1开始计数
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void set(int curIndex, IArray array, int index) {
		if (array.isNull(index)) {
			if (signs == null) {
				signs = new boolean[datas.length];
			}
			
			signs[curIndex] = true;
		} else if (array instanceof IntArray) {
			datas[curIndex] = ((IntArray)array).getInt(index);
		} else {
			set(curIndex, array.get(index));
		}
	}

	/**
	 * 追加元素
	 * @param n 值
	 */
	public void addInt(int n) {
		ensureCapacity(size + 1);
		datas[++size] = n;
	}
	
	/**
	 * 追加空元素
	 */
	public void pushNull() {
		if (signs == null) {
			signs = new boolean[datas.length];
		}
		
		signs[++size] = true;
	}
	
	/**
	 * 追加元素（不检查容量，认为有足够空间存放元素）
	 * @param n 值
	 */
	public void push(int n) {
		datas[++size] = n;
	}
	
	/**
	 * 追加元素（不检查容量，认为有足够空间存放元素）
	 * @param n 值
	 */
	public void pushInt(int n) {
		datas[++size] = n;
	}
	
	/**
	 * 取指定位置元素
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public Object get(int index) {
		if (signs == null || !signs[index]) {
			return ObjectCache.getInteger(datas[index]);
		} else {
			return null;
		}
	}

	/**
	 * 取指定位置元素的整数值
	 * @param index 索引，从1开始计数
	 * @return 整数值
	 */
	public int getInt(int index) {
		return datas[index];
	}

	/**
	 * 取指定位置元素的长整数值
	 * @param index 索引，从1开始计数
	 * @return 长整数值
	 */
	public long getLong(int index) {
		return datas[index];
	}

	/**
	 * 取指定位置元素的浮点数值
	 * @param index 索引，从1开始计数
	 * @return 浮点数值
	 */
	public double getDouble(int index) {
		return datas[index];
	}
	
	/**
	 * 取指定位置元素组成新数组
	 * @param indexArray 位置数组
	 * @return IArray
	 */
	public IArray get(int []indexArray) {
		int []datas = this.datas;
		boolean []signs = this.signs;
		int len = indexArray.length;
		IntArray result = new IntArray(len);
		
		if (signs == null) {
			for (int i : indexArray) {
				result.pushInt(datas[i]);
			}
		} else {
			for (int i : indexArray) {
				if (signs[i]) {
					result.pushNull();
				} else {
					result.pushInt(datas[i]);
				}
			}
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
		int []datas = this.datas;
		boolean []signs = this.signs;
		int len = end - start + 1;
		
		if (doCheck) {
			IntArray result = new IntArray(len);
			if (signs == null) {
				for (; start <= end; ++start) {
					int q = indexArray[start];
					if (q > 0) {
						result.pushInt(datas[q]);
					} else {
						result.pushNull();
					}
				}
			} else {
				for (; start <= end; ++start) {
					int q = indexArray[start];
					if (q < 1 || signs[q]) {
						result.pushNull();
					} else {
						result.pushInt(datas[q]);
					}
				}
			}
			
			return result;
		} else {
			if (signs == null) {
				int []resultDatas = new int[len + 1];
				for (int i = 1; start <= end; ++start) {
					resultDatas[i++] = datas[indexArray[start]];
				}
				
				return new IntArray(resultDatas, null, len);
			} else {
				IntArray result = new IntArray(len);
				for (; start <= end; ++start) {
					int q = indexArray[start];
					if (signs[q]) {
						result.pushNull();
					} else {
						result.pushInt(datas[q]);
					}
				}
				
				return result;
			}
		}
	}
	
	/**
	 * 取指定位置元素组成新数组
	 * @param IArray 位置数组
	 * @return IArray
	 */
	public IArray get(IArray indexArray) {
		int []datas = this.datas;
		boolean []signs = this.signs;
		int len = indexArray.size();
		IntArray result = new IntArray(len);
		
		if (signs == null) {
			for (int i = 1; i <= len; ++i) {
				result.pushInt(datas[indexArray.getInt(i)]);
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				int index = indexArray.getInt(i);
				if (signs[index]) {
					result.pushNull();
				} else {
					result.pushInt(datas[index]);
				}
			}
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
		int []newDatas = new int[newSize + 1];
		System.arraycopy(datas, start, newDatas, 1, newSize);
		
		if (signs == null) {
			return new IntArray(newDatas, null, newSize);
		} else {
			boolean []newSigns = new boolean[newSize + 1];
			System.arraycopy(signs, start, newSigns, 1, newSize);
			return new IntArray(newDatas, newSigns, newSize);
		}
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

			int []newDatas = new int[newCapacity];
			System.arraycopy(datas, 0, newDatas, 0, size + 1);
			datas = newDatas;
			
			if (signs != null) {
				boolean []newSigns = new boolean[newCapacity];
				System.arraycopy(signs, 0, newSigns, 0, size + 1);
				signs = newSigns;
			}
		}
	}
	
	/**
	 * 调整容量，使其与元素数相等
	 */
	public void trimToSize() {
		int newLen = size + 1;
		if (newLen < datas.length) {
			int []newDatas = new int[newLen];
			System.arraycopy(datas, 0, newDatas, 0, newLen);
			datas = newDatas;
			
			if (signs != null) {
				boolean []newSigns = new boolean[newLen];
				System.arraycopy(signs, 0, newSigns, 0, newLen);
				signs = newSigns;
			}
		}
	}
	
	/**
	 * 判断指定位置的元素是否是空
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isNull(int index) {
		return signs != null && signs[index];
	}
	
	/**
	 * 判断元素是否是True
	 * @return BoolArray
	 */
	public BoolArray isTrue() {
		boolean []signs = this.signs;
		int size = this.size;
		boolean []resultDatas = new boolean[size + 1];
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = true;
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = !signs[i];
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
		boolean []resultDatas = new boolean[size + 1];
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = false;
			}
		} else {
			System.arraycopy(signs, 1, resultDatas, 1, size);
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
		return signs == null || !signs[index];
	}
	
	/**
	 * 判断指定位置的元素是否是False
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isFalse(int index) {
		// 空则是false
		return signs != null && signs[index];
	}

	/**
	 * 是否是计算过程中临时产生的数组，临时产生的可以被修改，比如 f1+f2+f3，只需产生一个数组存放结果
	 * @return true：是临时产生的数组，false：不是临时产生的数组
	 */
	public boolean isTemporary() {
		return datas[0] == 1;
	}

	/**
	 * 设置是否是计算过程中临时产生的数组
	 * @param ifTemporary true：是临时产生的数组，false：不是临时产生的数组
	 */
	public void setTemporary(boolean ifTemporary) {
		datas[0] = ifTemporary ? 1 : 0;
	}
	
	/**
	 * 删除最后一个元素
	 */
	public void removeLast() {
		if (signs != null) {
			signs[size] = false;
		}
		
		size--;
	}
	
	/**
	 * 删除指定位置的元素
	 * @param index 索引，从1开始计数
	 */
	public void remove(int index) {
		System.arraycopy(datas, index + 1, datas, index, size - index);
		if (signs != null) {
			System.arraycopy(signs, index + 1, signs, index, size - index);
			signs[size] = false;
		}
		
		datas[size] = 0;
		--size;
	}
	
	/**
	 * 删除指定区间内的元素
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 */
	public void removeRange(int fromIndex, int toIndex) {
		System.arraycopy(datas, toIndex + 1, datas, fromIndex, size - toIndex);
		if (signs != null) {
			System.arraycopy(signs, toIndex + 1, signs, fromIndex, size - toIndex);
			for (int i = size - toIndex + fromIndex; i <= size; ++i) {
				signs[i] = false;
			}
		}
		
		for (int i = size - toIndex + fromIndex; i <= size; ++i) {
			datas[i] = 0;
		}
		
		size -= (toIndex - fromIndex + 1);
	}
	
	/**
	 * 删除指定位置的元素，序号从小到大排序
	 * @param seqs 索引数组
	 */
	public void remove(int []seqs) {
		int delCount = 0;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
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
				if (signs != null) {
					System.arraycopy(signs, cur + 1, signs, cur - delCount, moveCount);
				}
			}
			
			delCount++;
		}

		if (signs != null) {
			for (int i = 0, q = size; i < delCount; ++i) {
				signs[q - i] = false;
			}
		}
		
		for (int i = 0, q = size; i < delCount; ++i) {
			datas[q - i] = 0;
		}
		
		size -= delCount;
	}
	
	/**
	 * 保留指定区间内的数据
	 * @param start 起始位置（包含）
	 * @param end 结束位置（包含）
	 */
	public void reserve(int start, int end) {
		int []datas = this.datas;
		int newSize = end - start + 1;
		System.arraycopy(datas, start, datas, 1, newSize);
		
		if (signs != null) {
			System.arraycopy(signs, start, signs, 1, newSize);
			for (int i = size; i > newSize; --i) {
				signs[i] = false;
			}
		}
		
		for (int i = size; i > newSize; --i) {
			datas[i] = 0;
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
		boolean []signs = this.signs;
		int size = this.size;
		if (signs == null) {
			return size;
		}
		
		int count = size;
		for (int i = 1; i <= size; ++i) {
			if (signs[i]) {
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
		
		boolean []signs = this.signs;
		if (signs == null) {
			return true;
		}
		
		for (int i = 1; i <= size; ++i) {
			if (!signs[i]) {
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
		if (size == 0) {
			return null;
		}
		
		boolean []signs = this.signs;
		if (signs == null) {
			return ObjectCache.getInteger(datas[1]);
		}
		
		for (int i = 1; i <= size; ++i) {
			if (!signs[i]) {
				return ObjectCache.getInteger(datas[i]);
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
		if (obj == null) {
			if (signs == null) {
				signs = new boolean[datas.length];
			}
			
			datas[index] = 0;
			signs[index] = true;
		} else if (obj instanceof Integer) {
			datas[index] = (Integer)obj;
			if (signs != null) {
				signs[index] = false;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Integer"), Variant.getDataType(obj)));
		}
	}
	
	/**
	 * 删除所有的元素
	 */
	public void clear() {
		signs = null;
		size = 0;
	}
	
	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @return int 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem) {
		if (elem instanceof Number) {
			Number number = (Number)elem;
			int v = number.intValue();
			int []datas = this.datas;
			boolean []signs = this.signs;
			int low = 1, high = size;
			
			if (signs != null) {
				// 跳过null
				while (low <= high) {
					if (signs[low]) {
						low++;
					} else {
						break;
					}
				}
			}
			
			if (compare(v, number) == 0) {
				while (low <= high) {
					int mid = (low + high) >> 1;
					if (datas[mid] < v) {
						low = mid + 1;
					} else if (datas[mid] > v) {
						high = mid - 1;
					} else {
						return mid; // key found
					}
				}
			} else {
				while (low <= high) {
					int mid = (low + high) >> 1;
					if (compare(datas[mid], elem) < 0) {
						low = mid + 1;
					} else { // 大于0，不会有相等的情况
						high = mid - 1;
					}
				}
			}

			return -low; // key not found
		} else if (elem == null) {
			if (size > 0 && signs != null && signs[1]) {
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
	
	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @param start 起始查找位置（包含）
	 * @param end 结束查找位置（包含）
	 * @return 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem, int start, int end) {
		if (elem instanceof Number) {
			Number number = (Number)elem;
			int v = number.intValue();
			int []datas = this.datas;
			boolean []signs = this.signs;
			int low = start, high = end;
			
			if (signs != null) {
				// 跳过null
				while (low <= high) {
					if (signs[low]) {
						low++;
					} else {
						break;
					}
				}
			}
			
			if (compare(v, number) == 0) {
				while (low <= high) {
					int mid = (low + high) >> 1;
					if (datas[mid] < v) {
						low = mid + 1;
					} else if (datas[mid] > v) {
						high = mid - 1;
					} else {
						return mid; // key found
					}
				}
			} else {
				while (low <= high) {
					int mid = (low + high) >> 1;
					if (compare(datas[mid], elem) < 0) {
						low = mid + 1;
					} else { // 大于0，不会有相等的情况
						high = mid - 1;
					}
				}
			}

			return -low; // key not found
		} else if (elem == null) {
			if (end > 0 && signs != null && signs[start]) {
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
	
	private int binarySearch(int v, int low, int high) {
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs != null) {
			// 跳过null
			while (low <= high) {
				if (signs[low]) {
					low++;
				} else {
					break;
				}
			}
		}
		
		while (low <= high) {
			int mid = (low + high) >> 1;
			if (datas[mid] < v) {
				low = mid + 1;
			} else if (datas[mid] > v) {
				high = mid - 1;
			} else {
				return mid; // key found
			}
		}

		return -low; // key not found
	}
	
	public int binarySearch(int v) {
		int []datas = this.datas;
		boolean []signs = this.signs;
		int low = 1, high = size;
		
		if (signs != null) {
			// 跳过null
			while (low <= high) {
				if (signs[low]) {
					low++;
				} else {
					break;
				}
			}
		}
		
		while (low <= high) {
			int mid = (low + high) >> 1;
			if (datas[mid] < v) {
				low = mid + 1;
			} else if (datas[mid] > v) {
				high = mid - 1;
			} else {
				return mid; // key found
			}
		}

		return -low; // key not found
	}
	
	// 数组按降序排序，进行降序二分查找
	private int descBinarySearch(int elem) {
		int []datas = this.datas;
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
	 * 返回列表中是否包含指定元素
	 * @param elem Object 待查找的元素
	 * @return boolean true：包含，false：不包含
	 */
	public boolean contains(Object elem) {
		if (elem instanceof Number) {
			Number number = (Number)elem;
			int v = number.intValue();
			
			// 确定要查找的值是否是整数
			if (compare(v, number) != 0) {
				return false;
			}
			
			int []datas = this.datas;
			boolean []signs = this.signs;
			int size = this.size;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (datas[i] == v) {
						return true;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!signs[i] && datas[i] == v) {
						return true;
					}
				}
			}
			
			return false;
		} else if (elem == null) {
			boolean []signs = this.signs;
			if (signs == null) {
				return false;
			}
			
			int size = this.size;
			for (int i = 1; i <= size; ++i) {
				if (signs[i]) {
					return true;
				}
			}
			
			return false;
		} else {
			return false;
		}
	}
	
	public boolean contains(int v) {
		int []datas = this.datas;
		boolean []signs = this.signs;
		int size = this.size;
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				if (datas[i] == v) {
					return true;
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i] && datas[i] == v) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * 判断数组的元素是否在当前数组中
	 * @param isSorted 当前数组是否有序
	 * @param array 数组
	 * @param result 用于存放结果，只找取值为true的
	 */
	public void contains(boolean isSorted, IArray array, BoolArray result) {
		int resultSize = result.size();
		if (array instanceof IntArray) {
			if (isSorted) {
				for (int i = 1; i <= resultSize; ++i) {
					if (result.isTrue(i)) {
						if (array.isNull(i)) {
							if (binarySearch(null) < 1) {
								result.set(i, false);
							}
						} else if (binarySearch(array.getInt(i)) < 1) {
							result.set(i, false);
						}
					}
				}
			} else {
				for (int i = 1; i <= resultSize; ++i) {
					if (result.isTrue(i)) {
						if (array.isNull(i)) {
							if (!contains(null)) {
								result.set(i, false);
							}
						} else if (!contains(array.getInt(i))) {
							result.set(i, false);
						}
					}
				}
			}
		} else if (array instanceof LongArray) {
			if (isSorted) {
				for (int i = 1; i <= resultSize; ++i) {
					if (result.isTrue(i)) {
						if (array.isNull(i)) {
							if (binarySearch(null) < 1) {
								result.set(i, false);
							}
						} else {
							int n = array.getInt(i);
							if (n != array.getLong(i) || binarySearch(n) < 1) {
								result.set(i, false);
							}
						}
					}
				}
			} else {
				for (int i = 1; i <= resultSize; ++i) {
					if (result.isTrue(i)) {
						if (array.isNull(i)) {
							if (!contains(null)) {
								result.set(i, false);
							}
						} else {
							int n = array.getInt(i);
							if (n != array.getLong(i) || !contains(n)) {
								result.set(i, false);
							}
						}
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
		if (elem instanceof Number) {
			Number number = (Number)elem;
			int v = number.intValue();
			
			// 确定要查找的值是否是整数
			if (compare(v, number) != 0) {
				return 0;
			}
			
			int []datas = this.datas;
			boolean []signs = this.signs;
			int size = this.size;
			
			if (signs == null) {
				for (int i = start; i <= size; ++i) {
					if (datas[i] == v) {
						return i;
					}
				}
			} else {
				for (int i = start; i <= size; ++i) {
					if (!signs[i] && datas[i] == v) {
						return i;
					}
				}
			}
			
			return 0;
		} else if (elem == null) {
			boolean []signs = this.signs;
			if (signs == null) {
				return 0;
			} else {
				for (int i = start, size = this.size; i <= size; ++i) {
					if (signs[i]) {
						return i;
					}
				}
				
				return 0;
			}
		} else {
			return 0;
		}
	}
	
	private int firstIndexOf(int v, int start) {
		int []datas = this.datas;
		boolean []signs = this.signs;
		int size = this.size;
		
		if (signs == null) {
			for (int i = start; i <= size; ++i) {
				if (datas[i] == v) {
					return i;
				}
			}
		} else {
			for (int i = start; i <= size; ++i) {
				if (!signs[i] && datas[i] == v) {
					return i;
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * 返回元素在数组中最后出现的位置
	 * @param elem 待查找的元素
	 * @param start 从后面开始查找的位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	public int lastIndexOf(Object elem, int start) {
		if (elem instanceof Number) {
			Number number = (Number)elem;
			int v = number.intValue();
			
			// 确定要查找的值是否是整数
			if (compare(v, number) != 0) {
				return 0;
			}
			
			int []datas = this.datas;
			boolean []signs = this.signs;
			
			if (signs == null) {
				for (int i = start; i > 0; --i) {
					if (datas[i] == v) {
						return i;
					}
				}
			} else {
				for (int i = start; i > 0; --i) {
					if (!signs[i] && datas[i] == v) {
						return i;
					}
				}
			}
			
			return 0;
		} else if (elem == null) {
			boolean []signs = this.signs;
			if (signs == null) {
				return 0;
			} else {
				for (int i = start; i > 0; --i) {
					if (signs[i]) {
						return i;
					}
				}
				
				return 0;
			}
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
		boolean []signs = this.signs;
		
		if (elem == null) {
			IntArray result = new IntArray(7);
			if (signs != null) {
				if (isSorted) {
					if (isFromHead) {
						for (int i = start; i <= size; ++i) {
							if (signs[i]) {
								result.addInt(i);
							} else {
								break;
							}
						}
					} else {
						for (int i = start; i > 0; --i) {
							if (signs[i]) {
								result.addInt(i);
							}
						}
					}
				} else {
					if (isFromHead) {
						for (int i = start; i <= size; ++i) {
							if (signs[i]) {
								result.addInt(i);
							}
						}
					} else {
						for (int i = start; i > 0; --i) {
							if (signs[i]) {
								result.addInt(i);
							}
						}
					}
				}
			}
			
			return result;
		} else if (!(elem instanceof Number)) {
			return new IntArray(1);
		}

		Number number = (Number)elem;
		if (isSorted) {
			int end = size;
			if (!isFromHead) {
				end = start;
				start = 1;
			}
			
			int index = binarySearch(number, start, end);
			if (index < 1) {
				return new IntArray(1);
			}
			
			int []datas = this.datas;
			int v = number.intValue();
			
			// 找到第一个
			int first = index;
			while (first > start && (signs == null || !signs[first - 1]) && datas[first - 1] == v) {
				first--;
			}
			
			// 找到最后一个
			int last = index;
			while (last < end && (signs == null || !signs[last + 1]) && datas[last + 1] == v) {
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
			int []datas = this.datas;
			int v = number.intValue();
			
			// 确定要查找的值是否是整数
			if (compare(v, number) != 0) {
				return new IntArray(1);
			}
			
			IntArray result = new IntArray(7);
			if (isFromHead) {
				for (int i = start; i <= size; ++i) {
					if ((signs == null || !signs[i]) && datas[i] == v) {
						result.addInt(i);
					}
				}
			} else {
				for (int i = start; i > 0; --i) {
					if ((signs == null || !signs[i]) && datas[i] == v) {
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
		int size = this.size;
		IArray result;
		int []datas;
		
		if (isTemporary()) {
			result = this;
			datas = this.datas;
		} else {
			datas = new int[size + 1];
			System.arraycopy(this.datas, 1, datas, 1, size);
			
			boolean []signs = null;
			if (this.signs != null) {
				signs = new boolean[size + 1];
				System.arraycopy(this.signs, 1, signs, 1, size);
			}
			
			result = new IntArray(datas, signs, size);
			result.setTemporary(true);
		}
		
		// 不需要判断成员是否是null
		for (int i = 1; i <= size; ++i) {
			if (datas[i] < 0) {
				datas[i] = -datas[i];
			}
		}
		
		return result;
	}
	
	/**
	 * 对数组成员求负
	 * @return IArray 负值数组
	 */
	public IArray negate() {
		int size = this.size;
		int []datas = this.datas;
		
		// 不需要判断成员是否是null
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				datas[i] = -datas[i];
			}
			
			return this;
		} else {
			int []newDatas = new int[size + 1];
			for (int i = 1; i <= size; ++i) {
				newDatas[i] = -datas[i];
			}
			
			boolean []newSigns = null;
			if (signs != null) {
				newSigns = new boolean[size + 1];
				System.arraycopy(signs, 1, newSigns, 1, size);
			}
			
			IArray  result = new IntArray(newDatas, newSigns, size);
			result.setTemporary(true);
			return result;
		}
	}

	/**
	 * 对数组成员求非
	 * @return IArray 非值数组
	 */
	public IArray not() {
		boolean []signs = this.signs;
		int size = this.size;
		
		if (signs == null) {
			// 没有空元素时取非都是false
			return new ConstArray(Boolean.FALSE, size);
		} else {
			boolean []newDatas = new boolean[size + 1];
			System.arraycopy(signs, 1, newDatas, 1, size);
			
			IArray  result = new BoolArray(newDatas, size);
			result.setTemporary(true);
			return result;
		}
	}

	/**
	 * 判断数组的成员是否都是数（可以包含null）
	 * @return true：都是数，false：含有非数的值
	 */
	public boolean isNumberArray() {
		return true;
	}

	/**
	 * 计算两个数组的相对应的成员的和
	 * @param array 右侧数组
	 * @return 和数组
	 */
	public IArray memberAdd(IArray array) {
		if (array instanceof IntArray) {
			return memberAdd((IntArray)array);
		} else if (array instanceof LongArray) {
			return ((LongArray)array).memberAdd(this);
		} else if (array instanceof DoubleArray) {
			return ((DoubleArray)array).memberAdd(this);
		} else if (array instanceof ConstArray) {
			return memberAdd(array.get(1));
		} else if (array instanceof ObjectArray) {
			return ((ObjectArray)array).memberAdd(this);
		} else if (array instanceof DateArray) {
			return ((DateArray)array).memberAdd(this);
		} else if (array instanceof StringArray) {
			return ((StringArray)array).memberAdd(this);
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
		// 数字和字符串相加则尝试把串转成数字，转不成则当null处理
		if (value instanceof String) {
			value = Variant.parseNumber((String)value);
		}
		
		if (value == null) {
			return this;
		}
		
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (value instanceof Double || value instanceof Float) {
			double v = ((Number)value).doubleValue();
			double []resultDatas = new double[size + 1];
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v + datas[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = v;
					} else {
						resultDatas[i] = v + datas[i];
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, null, size);
			result.setTemporary(true);
			return result;
		} else if ((value instanceof BigDecimal) || (value instanceof BigInteger)) {
			Object []resultDatas = new Object[size + 1];
			BigDecimal v;
			if (value instanceof BigDecimal) {
				v = (BigDecimal)value;
			} else {
				v = new BigDecimal((BigInteger)value);
			}
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v.add(new BigDecimal(datas[i]));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = v;
					} else {
						resultDatas[i] = v.add(new BigDecimal(datas[i]));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (value instanceof Number) {
			long v = ((Number)value).longValue();
			long []resultDatas = new long[size + 1];
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v + datas[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = v;
					} else {
						resultDatas[i] = v + datas[i];
					}
				}
			}
			
			IArray result = new LongArray(resultDatas, null, size);
			result.setTemporary(true);
			return result;
		} else if (value instanceof Date) {
			Date v = (Date)value;
			Object []resultDatas = new Object[size + 1];
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Variant.elapse(v, datas[i], null);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = v;
					} else {
						resultDatas[i] =  Variant.elapse(v, datas[i], null);
					}
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
	
	private LongArray memberAdd(IntArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []d2 = array.datas;
		boolean []s2 = array.signs;
		
		long []datas = new long[size + 1];
		boolean []signs = null;
		
		if (s1 == null) {
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					datas[i] = (long)d1[i] + (long)d2[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						datas[i] = d1[i];
					} else {
						datas[i] = (long)d1[i] + (long)d2[i];
					}
				}
			}
		} else if (s2 == null) {
			for (int i = 1; i <= size; ++i) {
				if (s1[i]) {
					datas[i] = d2[i];
				} else {
					datas[i] = (long)d1[i] + (long)d2[i];
				}
			}
		} else {
			signs = new boolean[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (s1[i]) {
					if (s2[i]) {
						signs[i] = true;
					} else {
						datas[i] = d2[i];
					}
				} else if (s2[i]) {
					datas[i] = d1[i];
				} else {
					datas[i] = (long)d1[i] + (long)d2[i];
				}
			}
		}
		
		LongArray result = new LongArray(datas, signs, size);
		result.setTemporary(true);
		return result;
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
			return memberSubtract(array.get(1));
		} else if (array instanceof ObjectArray) {
			return memberSubtract((ObjectArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illSubtract"));
		}
	}
	
	private IArray memberSubtract(Object value) {
		if (value == null) {
			return this;
		}

		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (value instanceof Long) {
			long v = ((Long)value).longValue();
			long []resultDatas = new long[size + 1];
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = datas[i] - v;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = -v;
					} else {
						resultDatas[i] = datas[i] - v;
					}
				}
			}
			
			IArray result = new LongArray(resultDatas, null, size);
			result.setTemporary(true);
			return result;
		} else if (value instanceof Double || value instanceof Float) {
			double v = ((Number)value).doubleValue();
			double []resultDatas = new double[size + 1];
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = datas[i] - v;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = -v;
					} else {
						resultDatas[i] = datas[i] - v;
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, null, size);
			result.setTemporary(true);
			return result;
		} else if ((value instanceof BigDecimal) || (value instanceof BigInteger)) {
			Object []resultDatas = new Object[size + 1];
			BigDecimal v;
			if (value instanceof BigDecimal) {
				v = (BigDecimal)value;
			} else {
				v = new BigDecimal((BigInteger)value);
			}
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(datas[i]).subtract(v);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = v.negate();
					} else {
						resultDatas[i] = new BigDecimal(datas[i]).subtract(v);
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (value instanceof Number) {
			int v = ((Number)value).intValue();
			if (isTemporary()) {
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						datas[i] -= v;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							datas[i] = -v;
						} else {
							datas[i] -= v;
						}
					}
					
					this.signs = null;
				}
				
				return this;
			} else {
				int []resultDatas = new int[size + 1];
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = datas[i] - v;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							resultDatas[i] = -v;
						} else {
							resultDatas[i] = datas[i] - v;
						}
					}
				}
				
				IArray result = new IntArray(resultDatas, null, size);
				result.setTemporary(true);
				return result;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					Variant.getDataType(value) + mm.getMessage("Variant2.illSubtract"));
		}
	}
	
	private IntArray memberSubtract(IntArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []d2 = array.datas;
		boolean []s2 = array.signs;
		
		IntArray result;
		int []resultDatas;
		boolean []resultSigns;
		if (isTemporary()) {
			result = this;
			resultDatas = d1;
			resultSigns = s1;
		} else if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
			resultSigns = s2;
		} else {
			resultDatas = new int[size + 1];
			if (s1 != null && s2 != null) {
				resultSigns = new boolean[size + 1];
			} else {
				resultSigns = null;
			}
			
			result = new IntArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
		}
		
		if (s1 == null) {
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] - d2[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultDatas[i] = d1[i];
					} else {
						resultDatas[i] = d1[i] - d2[i];
					}
				}
			}
			
			result.signs = null;
		} else if (s2 == null) {
			for (int i = 1; i <= size; ++i) {
				if (s1[i]) {
					resultDatas[i] = -d2[i];
				} else {
					resultDatas[i] = d1[i] - d2[i];
				}
			}
			
			result.signs = null;
		} else {
			for (int i = 1; i <= size; ++i) {
				if (s1[i]) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = -d2[i];
					}
				} else if (s2[i]) {
					resultDatas[i] = d1[i];
				} else {
					resultDatas[i] = d1[i] - d2[i];
				}
			}
		}
		
		return result;
	}

	private LongArray memberSubtract(LongArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		LongArray result;
		long []resultDatas;
		boolean []resultSigns;
		if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
			resultSigns = s2;
		} else {
			resultDatas = new long[size + 1];
			if (s1 != null && s2 != null) {
				resultSigns = new boolean[size + 1];
			} else {
				resultSigns = null;
			}
			
			result = new LongArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
		}
		
		if (s1 == null) {
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] - d2[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultDatas[i] = d1[i];
					} else {
						resultDatas[i] = d1[i] - d2[i];
					}
				}
			}
			
			result.setSigns(null);
		} else if (s2 == null) {
			for (int i = 1; i <= size; ++i) {
				if (s1[i]) {
					resultDatas[i] = -d2[i];
				} else {
					resultDatas[i] = d1[i] - d2[i];
				}
			}
			
			result.setSigns(null);
		} else {
			for (int i = 1; i <= size; ++i) {
				if (s1[i]) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = -d2[i];
					}
				} else if (s2[i]) {
					resultDatas[i] = d1[i];
				} else {
					resultDatas[i] = d1[i] - d2[i];
				}
			}
		}
		
		return result;
	}

	private DoubleArray memberSubtract(DoubleArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		DoubleArray result;
		double []resultDatas;
		boolean []resultSigns;
		if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
			resultSigns = s2;
		} else {
			resultDatas = new double[size + 1];
			if (s1 != null && s2 != null) {
				resultSigns = new boolean[size + 1];
			} else {
				resultSigns = null;
			}
			
			result = new DoubleArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
		}
		
		if (s1 == null) {
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] - d2[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultDatas[i] = d1[i];
					} else {
						resultDatas[i] = d1[i] - d2[i];
					}
				}
			}
			
			result.setSigns(null);
		} else if (s2 == null) {
			for (int i = 1; i <= size; ++i) {
				if (s1[i]) {
					resultDatas[i] = -d2[i];
				} else {
					resultDatas[i] = d1[i] - d2[i];
				}
			}
			
			result.setSigns(null);
		} else {
			for (int i = 1; i <= size; ++i) {
				if (s1[i]) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = -d2[i];
					}
				} else if (s2[i]) {
					resultDatas[i] = d1[i];
				} else {
					resultDatas[i] = d1[i] - d2[i];
				}
			}
		}
		
		return result;
	}
	
	private ObjectArray memberSubtract(ObjectArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		Object []d2 = array.getDatas();
		
		ObjectArray result;
		Object []resultDatas;
		if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
		} else {
			resultDatas = new Object[size + 1];
			result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
		}
		
		for (int i = 1; i <= size; ++i) {
			Object v = d2[i];
			if (v == null) {
				if (s1 == null || !s1[i]) {
					resultDatas[i] = d1[i];
				}
			} else if (v instanceof Long) {
				if (s1 == null || !s1[i]) {
					resultDatas[i] = d1[i] - ((Long)v).longValue();
				} else {
					resultDatas[i] = -((Long)v).longValue();
				}
			} else if (v instanceof Double || v instanceof Float) {
				if (s1 == null || !s1[i]) {
					resultDatas[i] = d1[i] - ((Number)v).doubleValue();
				} else {
					resultDatas[i] = -((Number)v).doubleValue();
				}
			} else if (v instanceof BigDecimal) {
				if (s1 == null || !s1[i]) {
					resultDatas[i] = new BigDecimal(d1[i]).subtract((BigDecimal)v);
				} else {
					resultDatas[i] = ((BigDecimal)v).negate();
				}
			} else if (v instanceof BigInteger) {
				if (s1 == null || !s1[i]) {
					resultDatas[i] = new BigDecimal(d1[i]).subtract(new BigDecimal((BigInteger)v));
				} else {
					resultDatas[i] = new BigDecimal((BigInteger)v).negate();
				}
			} else if (v instanceof Number) {
				if (s1 == null || !s1[i]) {
					resultDatas[i] = d1[i] - ((Number)v).intValue();
				} else {
					resultDatas[i] = -((Number)v).intValue();
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
						Variant.getDataType(v) + mm.getMessage("Variant2.illSubtract"));
			}
		}
		
		return result;
	}

	/**
	 * 计算两个数组的相对应的成员的积
	 * @param array 右侧数组
	 * @return 积数组
	 */
	public IArray memberMultiply(IArray array) {
		if (array instanceof IntArray) {
			return memberMultiply((IntArray)array);
		} else if (array instanceof LongArray) {
			return ((LongArray)array).memberMultiply(this);
		} else if (array instanceof DoubleArray) {
			return ((DoubleArray)array).memberMultiply(this);
		} else if (array instanceof ConstArray) {
			return memberMultiply(array.get(1));
		} else if (array instanceof ObjectArray) {
			return ((ObjectArray)array).memberMultiply(this);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illMultiply"));
		}
	}

	/**
	 * 计算数组的成员与指定常数的积
	 * @param value 常数
	 * @return 积数组
	 */
	public IArray memberMultiply(Object value) {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (value instanceof Double || value instanceof Float) {
			double v = ((Number)value).doubleValue();
			double []resultDatas = new double[size + 1];
			boolean []resultSigns = null;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v * datas[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = v * datas[i];
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if ((value instanceof BigDecimal) || (value instanceof BigInteger)) {
			Object []resultDatas = new Object[size + 1];
			BigDecimal v;
			if (value instanceof BigDecimal) {
				v = (BigDecimal)value;
			} else {
				v = new BigDecimal((BigInteger)value);
			}
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v.multiply(new BigDecimal(datas[i]));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!signs[i]) {
						resultDatas[i] = v.multiply(new BigDecimal(datas[i]));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (value instanceof Number) {
			long v = ((Number)value).longValue();
			long []resultDatas = new long[size + 1];
			boolean []resultSigns = null;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = v * datas[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = v * datas[i];
					}
				}
			}
			
			IArray result = new LongArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if (value == null) {
			return new ConstArray(null, size);
		} else if (value instanceof Sequence) {
			Sequence sequence = (Sequence)value;
			Object []resultDatas = new Object[size + 1];
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = sequence.multiply(datas[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultDatas[i] = null;
					} else {
						resultDatas[i] =  sequence.multiply(datas[i]);
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					Variant.getDataType(value) + mm.getMessage("Variant2.illMultiply"));
		}
	}

	private LongArray memberMultiply(IntArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []d2 = array.datas;
		boolean []s2 = array.signs;
		
		long []datas = new long[size + 1];
		boolean []signs = null;
		
		if (s1 == null) {
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					datas[i] = (long)d1[i] * (long)d2[i];
				}
			} else {
				signs = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						signs[i] = true;
					} else {
						datas[i] = (long)d1[i] * (long)d2[i];
					}
				}
			}
		} else if (s2 == null) {
			signs = new boolean[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (s1[i]) {
					signs[i] = true;
				} else {
					datas[i] = (long)d1[i] * (long)d2[i];
				}
			}
		} else {
			signs = new boolean[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (s1[i] || s2[i]) {
					signs[i] = true;
				} else {
					datas[i] = (long)d1[i] * (long)d2[i];
				}
			}
		}
		
		LongArray result = new LongArray(datas, signs, size);
		result.setTemporary(true);
		return result;
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
			return memberDivide(array.get(1));
		} else if (array instanceof ObjectArray) {
			return memberDivide((ObjectArray)array);
		} else if (array instanceof StringArray) {
			return memberDivide((StringArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}
	
	private IArray memberDivide(Object value) {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if ((value instanceof BigDecimal) || (value instanceof BigInteger)) {
			BigDecimal v;
			if (value instanceof BigDecimal) {
				v = (BigDecimal)value;
			} else {
				v = new BigDecimal((BigInteger)value);
			}
			
			Object []resultDatas = new Object[size + 1];
			BigDecimal decimal;
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					decimal = new BigDecimal(datas[i]);
					resultDatas[i] = decimal.divide(v, Variant.Divide_Scale, Variant.Divide_Round);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!signs[i]) {
						decimal = new BigDecimal(datas[i]);
						resultDatas[i] = decimal.divide(v, Variant.Divide_Scale, Variant.Divide_Round);
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (value instanceof Number) {
			double v = ((Number)value).doubleValue();
			double []resultDatas = new double[size + 1];
			boolean []resultSigns = null;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = (double)datas[i] / v;
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = (double)datas[i] / v;
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if (value instanceof String) {
			String str = (String)value;
			Object []resultDatas = new Object[size + 1];
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = datas[i] + str;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!signs[i]) {
						resultDatas[i] = datas[i] + str;
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;			
		} else if (value == null) {
			return new ConstArray(null, size);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") + 
					Variant.getDataType(value) + mm.getMessage("Variant2.illDivide"));
		}
	}
	
	private DoubleArray memberDivide(IntArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		double []resultDatas = new double[size + 1];
		boolean []resultSigns = null;

		if (s1 != null) {
			resultSigns = new boolean[size + 1];
			if (s2 != null) {
				System.arraycopy(s2, 1, resultSigns, 1, size);
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultSigns[i] = true;
					}
				}
			} else {
				System.arraycopy(s1, 1, resultSigns, 1, size);
			}
		} else if (s2 != null) {
			resultSigns = new boolean[size + 1];
			System.arraycopy(s2, 1, resultSigns, 1, size);
		}
		
		if (resultSigns == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = (double)d1[i] / (double)d2[i];
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!resultSigns[i]) {
					resultDatas[i] = (double)d1[i] / (double)d2[i];
				}
			}
		}
		
		DoubleArray result = new DoubleArray(resultDatas, resultSigns, size);
		result.setTemporary(true);
		return result;
	}
	
	private DoubleArray memberDivide(LongArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		double []resultDatas = new double[size + 1];
		boolean []resultSigns = null;

		if (s1 != null) {
			resultSigns = new boolean[size + 1];
			if (s2 != null) {
				System.arraycopy(s2, 1, resultSigns, 1, size);
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultSigns[i] = true;
					}
				}
			} else {
				System.arraycopy(s1, 1, resultSigns, 1, size);
			}
		} else if (s2 != null) {
			resultSigns = new boolean[size + 1];
			System.arraycopy(s2, 1, resultSigns, 1, size);
		}
		
		if (resultSigns == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = (double)d1[i] / (double)d2[i];
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!resultSigns[i]) {
					resultDatas[i] = (double)d1[i] / (double)d2[i];
				}
			}
		}
		
		DoubleArray result = new DoubleArray(resultDatas, resultSigns, size);
		result.setTemporary(true);
		return result;
	}
	
	private DoubleArray memberDivide(DoubleArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		DoubleArray result;
		double []resultDatas;
		boolean []resultSigns = null;
		if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
			
			if (s1 != null) {
				if (s2 == null) {
					resultSigns = new boolean[size + 1];
					System.arraycopy(s1, 1, resultSigns, 1, size);
					array.setSigns(resultSigns);
				} else {
					resultSigns = s2;
					for (int i = 1; i <= size; ++i) {
						if (s1[i]) {
							resultSigns[i] = true;
						}
					}
				}
			} else {
				resultSigns = s2;
			}
		} else {
			resultDatas = new double[size + 1];
			if (s1 != null) {
				resultSigns = new boolean[size + 1];
				if (s2 != null) {
					System.arraycopy(s2, 1, resultSigns, 1, size);
					for (int i = 1; i <= size; ++i) {
						if (s1[i]) {
							resultSigns[i] = true;
						}
					}
				} else {
					System.arraycopy(s1, 1, resultSigns, 1, size);
				}
			} else if (s2 != null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s2, 1, resultSigns, 1, size);
			}
			
			result = new DoubleArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
		}
		
		if (resultSigns == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = (double)d1[i] / d2[i];
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!resultSigns[i]) {
					resultDatas[i] = (double)d1[i] / d2[i];
				}
			}
		}
		
		return result;
	}
	
	private StringArray memberDivide(StringArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		String []d2 = array.getDatas();
		
		if (array.isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (s1 == null || !s1[i]) {
					if (d2[i] != null) {
						d2[i] = d1[i] + d2[i];
					} else {
						d2[i] = Integer.toString(d1[i]);
					}
				}
			}
			
			return array;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d2[i] != null) {
					if (s1 == null || !s1[i]) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d2[i];
					}
				} else if (s1 == null || !s1[i]) {
					resultDatas[i] = Integer.toString(d1[i]);
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private ObjectArray memberDivide(ObjectArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		Object []d2 = array.getDatas();
		
		ObjectArray result;
		Object []resultDatas;
		if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
		} else {
			resultDatas = new Object[size + 1];
			result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
		}
		
		for (int i = 1; i <= size; ++i) {
			Object v = d2[i];
			if (v instanceof BigDecimal) {
				if (s1 == null || !s1[i]) {
					BigDecimal decimal = new BigDecimal(d1[i]);
					resultDatas[i] = decimal.divide((BigDecimal)v, Variant.Divide_Scale, Variant.Divide_Round);
				} else {
					resultDatas[i] = null;
				}
			} else if (v instanceof BigInteger) {
				if (s1 == null || !s1[i]) {
					BigDecimal decimal = new BigDecimal(d1[i]);
					resultDatas[i] = decimal.divide(new BigDecimal((BigInteger)v), Variant.Divide_Scale, Variant.Divide_Round);
				} else {
					resultDatas[i] = null;
				}
			} else if (v instanceof Number) {
				if (s1 == null || !s1[i]) {
					resultDatas[i] = (double)d1[i] / ((Number)v).doubleValue();
				} else {
					resultDatas[i] = null;
				}
			} else if (v instanceof String) {
				if (s1 == null || !s1[i]) {
					resultDatas[i] = d1[i] + (String)v;
				} else {
					resultDatas[i] = v;
				}
			} else if (v != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
						Variant.getDataType(v) + mm.getMessage("Variant2.illDivide"));
			}
		}
		
		return result;
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
			return memberMod(array.get(1));
		} else if (array instanceof ObjectArray) {
			return memberMod((ObjectArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illMod"));
		}
	}

	private IntArray memberMod(IntArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []d2 = array.datas;
		boolean []s2 = array.signs;
		
		IntArray result;
		int []resultDatas;
		boolean []resultSigns = null;
		
		if (isTemporary()) {
			result = this;
			resultDatas = d1;
			
			if (s2 == null) {
				resultSigns = s1;
			} else if (s1 == null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s2, 1, resultSigns, 1, size);
				this.signs = resultSigns;
			} else {
				resultSigns = s1;
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						// 左右操作数有一个为空则值为空
						resultSigns[i] = true;
					}
				}
			}
		} else if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
			
			if (s1 == null) {
				resultSigns = s2;
			} else if (s2 == null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s1, 1, resultSigns, 1, size);
				array.signs = resultSigns;
			} else {
				resultSigns = s2;
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						// 左右操作数有一个为空则值为空
						resultSigns[i] = true;
					}
				}
			}
		} else {
			resultDatas = new int[size + 1];
			if (s1 != null) {
				resultSigns = new boolean[size + 1];
				if (s2 != null) {
					System.arraycopy(s2, 1, resultSigns, 1, size);
					for (int i = 1; i <= size; ++i) {
						if (s1[i]) {
							resultSigns[i] = true;
						}
					}
				} else {
					System.arraycopy(s1, 1, resultSigns, 1, size);
				}
			} else if (s2 != null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s2, 1, resultSigns, 1, size);
			}
			
			result = new IntArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
		}
		
		if (resultSigns == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] % d2[i];
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!resultSigns[i]) {
					resultDatas[i] = d1[i] % d2[i];
				}
			}
		}
		
		return result;
	}
	
	private LongArray memberMod(LongArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		LongArray result;
		long []resultDatas;
		boolean []resultSigns = null;
		
		if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
			
			if (s1 == null) {
				resultSigns = s2;
			} else if (s2 == null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s1, 1, resultSigns, 1, size);
				array.setSigns(resultSigns);
			} else {
				resultSigns = s2;
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						// 左右操作数有一个为空则值为空
						resultSigns[i] = true;
					}
				}
			}
		} else {
			resultDatas = new long[size + 1];
			if (s1 != null) {
				resultSigns = new boolean[size + 1];
				if (s2 != null) {
					System.arraycopy(s2, 1, resultSigns, 1, size);
					for (int i = 1; i <= size; ++i) {
						if (s1[i]) {
							resultSigns[i] = true;
						}
					}
				} else {
					System.arraycopy(s1, 1, resultSigns, 1, size);
				}
			} else if (s2 != null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s2, 1, resultSigns, 1, size);
			}
			
			result = new LongArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
		}
		
		if (resultSigns == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] % d2[i];
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!resultSigns[i]) {
					resultDatas[i] = d1[i] % d2[i];
				}
			}
		}
		
		return result;
	}
	
	private DoubleArray memberMod(DoubleArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		DoubleArray result;
		double []resultDatas;
		boolean []resultSigns = null;
		
		if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
			
			if (s1 == null) {
				resultSigns = s2;
			} else if (s2 == null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s1, 1, resultSigns, 1, size);
				array.setSigns(resultSigns);
			} else {
				resultSigns = s2;
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						// 左右操作数有一个为空则值为空
						resultSigns[i] = true;
					}
				}
			}
		} else {
			resultDatas = new double[size + 1];
			if (s1 != null) {
				resultSigns = new boolean[size + 1];
				if (s2 != null) {
					System.arraycopy(s2, 1, resultSigns, 1, size);
					for (int i = 1; i <= size; ++i) {
						if (s1[i]) {
							resultSigns[i] = true;
						}
					}
				} else {
					System.arraycopy(s1, 1, resultSigns, 1, size);
				}
			} else if (s2 != null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s2, 1, resultSigns, 1, size);
			}
			
			result = new DoubleArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
		}
		
		if (resultSigns == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] % d2[i];
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!resultSigns[i]) {
					resultDatas[i] = d1[i] % d2[i];
				}
			}
		}
		
		return result;
	}
	
	private IArray memberMod(Object value) {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (value instanceof Long) {
			long v = ((Number)value).longValue();
			long []resultDatas = new long[size + 1];
			boolean []resultSigns = null;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = datas[i] % v;
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = datas[i] % v;
					}
				}
			}
			
			IArray result = new LongArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if (value instanceof Double || value instanceof Float) {
			double v = ((Number)value).doubleValue();
			double []resultDatas = new double[size + 1];
			boolean []resultSigns = null;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = datas[i] % v;
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = datas[i] % v;
					}
				}
			}
			
			IArray result = new DoubleArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if ((value instanceof BigDecimal) || (value instanceof BigInteger)) {
			BigInteger v;
			if (value instanceof BigDecimal) {
				v = ((BigDecimal)value).toBigInteger();
			} else {
				v = (BigInteger)value;
			}
			
			Object []resultDatas = new Object[size + 1];
			BigInteger bi;
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					bi = BigInteger.valueOf(datas[i]);
					resultDatas[i] = new BigDecimal(bi.mod(v));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!signs[i]) {
						bi = BigInteger.valueOf(datas[i]);
						resultDatas[i] = new BigDecimal(bi.mod(v));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (value instanceof Number) {
			int v = ((Number)value).intValue();
			if (isTemporary()) {
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						datas[i] %= v;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!signs[i]) {
							datas[i] %= v;
						}
					}
				}
				
				return this;
			} else {
				int []resultDatas = new int[size + 1];
				boolean []resultSigns = null;
				
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = datas[i] % v;
					}
				} else {
					resultSigns = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							resultSigns[i] = true;
						} else {
							resultDatas[i] = datas[i] % v;
						}
					}
				}
				
				IArray result = new IntArray(resultDatas, resultSigns, size);
				result.setTemporary(true);
				return result;
			}
		} else if (value == null) {
			return new ConstArray(null, size);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") + 
					Variant.getDataType(value) + mm.getMessage("Variant2.illMod"));
		}
	}
	
	private IArray memberMod(ObjectArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		Object []d2 = array.getDatas();
		
		ObjectArray result;
		Object []resultDatas;
		if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
		} else {
			resultDatas = new Object[size + 1];
			result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
		}
		
		for (int i = 1; i <= size; ++i) {
			Object v = d2[i];
			if (v == null || (s1 != null && s1[i])) {
				resultDatas[i] = null;
			} else if (v instanceof Long) {
				resultDatas[i] = d1[i] % ((Number)v).longValue();
			} else if (v instanceof Double || v instanceof Float) {
				resultDatas[i] = d1[i] % ((Number)v).doubleValue();
			} else if (v instanceof BigDecimal) {
				BigInteger bi1 = BigInteger.valueOf(d1[i]);
				BigInteger bi2 = ((BigDecimal)v).toBigInteger();
				resultDatas[i] = new BigDecimal(bi1.mod(bi2));
			} else if (v instanceof BigInteger) {
				BigInteger bi1 = BigInteger.valueOf(d1[i]);
				resultDatas[i] = new BigDecimal(bi1.mod((BigInteger)v));
			} else if (v instanceof Number) {
				resultDatas[i] = ObjectCache.getInteger(d1[i] % ((Number)v).intValue());
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
						Variant.getDataType(v) + mm.getMessage("Variant2.illMod"));
			}
		}
		
		return result;
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
			return memberIntDivide(array.get(1));
		} else if (array instanceof ObjectArray) {
			return memberIntDivide((ObjectArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}
	
	private IntArray memberIntDivide(IntArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []d2 = array.datas;
		boolean []s2 = array.signs;
		
		IntArray result;
		int []resultDatas;
		boolean []resultSigns = null;
		
		if (isTemporary()) {
			result = this;
			resultDatas = d1;
			
			if (s2 == null) {
				resultSigns = s1;
			} else if (s1 == null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s2, 1, resultSigns, 1, size);
				this.signs = resultSigns;
			} else {
				resultSigns = s1;
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						// 左右操作数有一个为空则值为空
						resultSigns[i] = true;
					}
				}
			}
		} else if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
			
			if (s1 == null) {
				resultSigns = s2;
			} else if (s2 == null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s1, 1, resultSigns, 1, size);
				array.signs = resultSigns;
			} else {
				resultSigns = s2;
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						// 左右操作数有一个为空则值为空
						resultSigns[i] = true;
					}
				}
			}
		} else {
			resultDatas = new int[size + 1];
			if (s1 != null) {
				resultSigns = new boolean[size + 1];
				if (s2 != null) {
					System.arraycopy(s2, 1, resultSigns, 1, size);
					for (int i = 1; i <= size; ++i) {
						if (s1[i]) {
							resultSigns[i] = true;
						}
					}
				} else {
					System.arraycopy(s1, 1, resultSigns, 1, size);
				}
			} else if (s2 != null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s2, 1, resultSigns, 1, size);
			}
			
			result = new IntArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
		}
		
		if (resultSigns == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] / d2[i];
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!resultSigns[i]) {
					resultDatas[i] = d1[i] / d2[i];
				}
			}
		}
		
		return result;
	}
	
	private LongArray memberIntDivide(LongArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		LongArray result;
		long []resultDatas;
		boolean []resultSigns = null;
		
		if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
			
			if (s1 == null) {
				resultSigns = s2;
			} else if (s2 == null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s1, 1, resultSigns, 1, size);
				array.setSigns(resultSigns);
			} else {
				resultSigns = s2;
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						// 左右操作数有一个为空则值为空
						resultSigns[i] = true;
					}
				}
			}
		} else {
			resultDatas = new long[size + 1];
			if (s1 != null) {
				resultSigns = new boolean[size + 1];
				if (s2 != null) {
					System.arraycopy(s2, 1, resultSigns, 1, size);
					for (int i = 1; i <= size; ++i) {
						if (s1[i]) {
							resultSigns[i] = true;
						}
					}
				} else {
					System.arraycopy(s1, 1, resultSigns, 1, size);
				}
			} else if (s2 != null) {
				resultSigns = new boolean[size + 1];
				System.arraycopy(s2, 1, resultSigns, 1, size);
			}
			
			result = new LongArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
		}
		
		if (resultSigns == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] / d2[i];
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!resultSigns[i]) {
					resultDatas[i] = d1[i] / d2[i];
				}
			}
		}
		
		return result;
	}
	
	private LongArray memberIntDivide(DoubleArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		LongArray result;
		long []resultDatas = new long[size + 1];
		boolean []resultSigns = null;
		
		if (s1 == null) {
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = (long)d1[i] / (long)d2[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (s2[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = (long)d1[i] / (long)d2[i];
					}
				}
			}
		} else if (s2 == null) {
			resultSigns = new boolean[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (s1[i]) {
					resultSigns[i] = true;
				} else {
					resultDatas[i] = (long)d1[i] / (long)d2[i];
				}
			}
		} else {
			resultSigns = new boolean[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (s1[i] || s2[i]) {
					resultSigns[i] = true;
				} else {
					resultDatas[i] = (long)d1[i] / (long)d2[i];
				}
			}
		}
		
		result = new LongArray(resultDatas, resultSigns, size);
		result.setTemporary(true);
		return result;
	}
	
	private IArray memberIntDivide(Object value) {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (value instanceof Long || value instanceof Double || value instanceof Float) {
			long v = ((Number)value).longValue();
			long []resultDatas = new long[size + 1];
			boolean []resultSigns = null;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = (long)datas[i] / v;
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						resultSigns[i] = true;
					} else {
						resultDatas[i] = (long)datas[i] / v;
					}
				}
			}
			
			IArray result = new LongArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		} else if ((value instanceof BigDecimal) || (value instanceof BigInteger)) {
			BigInteger v;
			if (value instanceof BigDecimal) {
				v = ((BigDecimal)value).toBigInteger();
			} else {
				v = (BigInteger)value;
			}
			
			Object []resultDatas = new Object[size + 1];
			BigInteger bi;
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					bi = BigInteger.valueOf(datas[i]);
					resultDatas[i] = new BigDecimal(bi.divide(v));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!signs[i]) {
						bi = BigInteger.valueOf(datas[i]);
						resultDatas[i] = new BigDecimal(bi.divide(v));
					}
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (value instanceof Number) {
			int v = ((Number)value).intValue();
			if (isTemporary()) {
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						datas[i] /= v;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!signs[i]) {
							datas[i] /= v;
						}
					}
				}
				
				return this;
			} else {
				int []resultDatas = new int[size + 1];
				boolean []resultSigns = null;
				
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = datas[i] / v;
					}
				} else {
					resultSigns = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						if (signs[i]) {
							resultSigns[i] = true;
						} else {
							resultDatas[i] = datas[i] / v;
						}
					}
				}
				
				IArray result = new IntArray(resultDatas, resultSigns, size);
				result.setTemporary(true);
				return result;
			}
		} else if (value == null) {
			return new ConstArray(null, size);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") + 
					Variant.getDataType(value) + mm.getMessage("Variant2.illDivide"));
		}
	}
	
	private IArray memberIntDivide(ObjectArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		Object []d2 = array.getDatas();
		
		ObjectArray result;
		Object []resultDatas;
		if (array.isTemporary()) {
			result = array;
			resultDatas = d2;
		} else {
			resultDatas = new Object[size + 1];
			result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
		}
		
		for (int i = 1; i <= size; ++i) {
			Object v = d2[i];
			if (v == null || (s1 != null && s1[i])) {
				resultDatas[i] = null;
			} else if (v instanceof Long || v instanceof Double || v instanceof Float) {
				resultDatas[i] = (long)d1[i] / ((Number)v).longValue();
			} else if (v instanceof BigDecimal) {
				BigInteger bi1 = BigInteger.valueOf(d1[i]);
				BigInteger bi2 = ((BigDecimal)v).toBigInteger();
				resultDatas[i] = new BigDecimal(bi1.divide(bi2));
			} else if (v instanceof BigInteger) {
				BigInteger bi1 = BigInteger.valueOf(d1[i]);
				resultDatas[i] = new BigDecimal(bi1.divide((BigInteger)v));
			} else if (v instanceof Number) {
				resultDatas[i] = ObjectCache.getInteger(d1[i] / ((Number)v).intValue());
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
						Variant.getDataType(v) + mm.getMessage("Variant2.illDivide"));
			}
		}
		
		return result;
	}
	
	/**
	 * 两个数组的相对应的成员进行比较，返回比较结果数组
	 * @param rightArray 右侧数组
	 * @return IntArray 1：左侧大，0：相等，-1：右侧大
	 */
	public IntArray memberCompare(NumberArray rightArray) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		
		int []resultDatas;
		if (isTemporary()) {
			resultDatas = d1;
		} else {
			resultDatas = new int[size + 1];
		}
		
		if (rightArray instanceof IntArray) {
			IntArray array = (IntArray)rightArray;
			int []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			for (int i = 1; i <= size; ++i) {
				if (s1 == null || !s1[i]) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = d1[i] < d2[i] ? -1 : (d1[i] == d2[i] ? 0 : 1);
					} else {
						resultDatas[i] = 1;
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = -1;
				} else {
					resultDatas[i] = 0;
				}
			}
		} else if (rightArray instanceof LongArray) {
			LongArray array = (LongArray)rightArray;
			long []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			for (int i = 1; i <= size; ++i) {
				if (s1 == null || !s1[i]) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = d1[i] < d2[i] ? -1 : (d1[i] == d2[i] ? 0 : 1);
					} else {
						resultDatas[i] = 1;
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = -1;
				} else {
					resultDatas[i] = 0;
				}
			}			
		} else {
			DoubleArray array = (DoubleArray)rightArray;
			double []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			for (int i = 1; i <= size; ++i) {
				if (s1 == null || !s1[i]) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = Double.compare(d1[i], d2[i]);
					} else {
						resultDatas[i] = 1;
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = -1;
				} else {
					resultDatas[i] = 0;
				}
			}
		}
		
		IntArray result = new IntArray(resultDatas, null, size);
		result.setTemporary(true);
		return result;
	}
	
	/**
	 * 计算两个数组的相对应的成员的关系运算
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @return 关系运算结果数组
	 */
	public BoolArray calcRelation(IArray array, int relation) {
		if (array instanceof IntArray) {
			return calcRelation((IntArray)array, relation);
		} else if (array instanceof LongArray) {
			return calcRelation((LongArray)array, relation);
		} else if (array instanceof DoubleArray) {
			return calcRelation((DoubleArray)array, relation);
		} else if (array instanceof ConstArray) {
			return calcRelation(array.get(1), relation);
		} else if (array instanceof BoolArray) {
			return ((BoolArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else if (array instanceof DateArray) {
			return calcRelation((DateArray)array, relation);
		} else if (array instanceof StringArray) {
			return calcRelation((StringArray)array, relation);
		} else if (array instanceof ObjectArray) {
			return calcRelation((ObjectArray)array, relation);
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
		if (value instanceof Double || value instanceof Float) {
			return calcRelation(((Number)value).doubleValue(), relation);
		} else if (value instanceof BigDecimal) {
			return calcRelation((BigDecimal)value, relation);
		} else if (value instanceof BigInteger) {
			BigDecimal decimal = new BigDecimal((BigInteger)value);
			return calcRelation(decimal, relation);
		} else if (value instanceof Number) {
			return calcRelation(((Number)value).longValue(), relation);
		} else if (value == null) {
			return ArrayUtil.calcRelationNull(signs, size, relation);
		} else {
			boolean b = Variant.isTrue(value);
			int size = this.size;
			boolean []s1 = this.signs;
			
			if (relation == Relation.AND) {
				BoolArray result;
				if (!b) {
					result = new BoolArray(false, size);
				} else if (s1 == null) {
					result = new BoolArray(true, size);
				} else {
					boolean []resultDatas = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = !s1[i];
					}
					
					result = new BoolArray(resultDatas, size);
				}
				
				result.setTemporary(true);
				return result;
			} else if (relation == Relation.OR) {
				BoolArray result;
				if (b || s1 == null) {
					result = new BoolArray(true, size);
				} else {
					boolean []resultDatas = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = !s1[i];
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
	
	private BoolArray calcRelation(IntArray array, int relation) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []d2 = array.datas;
		boolean []s2 = array.signs;
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] == d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = d1[i] == d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] == d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = s2[i];
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] == d2[i];
					}
				}
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] > d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = d1[i] > d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] > d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] > d2[i];
					}
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] >= d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = d1[i] >= d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] >= d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = s2[i];
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] >= d2[i];
					}
				}
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] < d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = d1[i] < d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] < d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = !s2[i];
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] < d2[i];
					}
				}
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] <= d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = d1[i] <= d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] <= d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] <= d2[i];
					}
				}
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] != d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = d1[i] != d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] != d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = !s2[i];
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] != d2[i];
					}
				}
			}
		} else if (relation == Relation.AND) {
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = true;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = !s2[i];
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && !s2[i];
				}
			}
		} else { // Relation.OR
			if (s1 == null || s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] || !s2[i];
				}
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}

	protected BoolArray calcRelation(LongArray array, int relation) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] == d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = d1[i] == d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] == d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = s2[i];
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] == d2[i];
					}
				}
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] > d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = d1[i] > d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] > d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] > d2[i];
					}
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] >= d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = d1[i] >= d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] >= d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = s2[i];
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] >= d2[i];
					}
				}
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] < d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = d1[i] < d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] < d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = !s2[i];
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] < d2[i];
					}
				}
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] <= d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = d1[i] <= d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] <= d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] <= d2[i];
					}
				}
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] != d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = d1[i] != d2[i];
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] != d2[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = !s2[i];
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] != d2[i];
					}
				}
			}
		} else if (relation == Relation.AND) {
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = true;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = !s2[i];
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && !s2[i];
				}
			}
		} else { // Relation.OR
			if (s1 == null || s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] || !s2[i];
				}
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}

	protected BoolArray calcRelation(DoubleArray array, int relation) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = Double.compare(d1[i], d2[i]) == 0;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = Double.compare(d1[i], d2[i]) == 0;
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) == 0;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = s2[i];
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) == 0;
					}
				}
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = Double.compare(d1[i], d2[i]) > 0;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = Double.compare(d1[i], d2[i]) > 0;
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) > 0;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) > 0;
					}
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = Double.compare(d1[i], d2[i]) >= 0;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = Double.compare(d1[i], d2[i]) >= 0;
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) >= 0;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = s2[i];
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) >= 0;
					}
				}
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = Double.compare(d1[i], d2[i]) < 0;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = Double.compare(d1[i], d2[i]) < 0;
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) < 0;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = !s2[i];
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) < 0;
					}
				}
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = Double.compare(d1[i], d2[i]) <= 0;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = Double.compare(d1[i], d2[i]) <= 0;
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) <= 0;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) <= 0;
					}
				}
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = Double.compare(d1[i], d2[i]) != 0;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = Double.compare(d1[i], d2[i]) != 0;
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) != 0;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = !s2[i];
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Double.compare(d1[i], d2[i]) != 0;
					}
				}
			}
		} else if (relation == Relation.AND) {
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = true;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = !s2[i];
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && !s2[i];
				}
			}
		} else { // Relation.OR
			if (s1 == null || s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] || !s2[i];
				}
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}

	private BoolArray calcRelation(long value, int relation) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] == value;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] == value;
					}
				}
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] > value;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] > value;
					}
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] >= value;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = d1[i] >= value;
					}
				}
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] < value;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] < value;
					}
				}
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] <= value;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] <= value;
					}
				}
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] != value;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = d1[i] != value;
					}
				}
			}
		} else if (relation == Relation.AND) {
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i];
				}
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
	
	private BoolArray calcRelation(double value, int relation) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Double.compare(d1[i], value) == 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Double.compare(d1[i], value) == 0;
					}
				}
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Double.compare(d1[i], value) > 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Double.compare(d1[i], value) > 0;
					}
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Double.compare(d1[i], value) >= 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Double.compare(d1[i], value) >= 0;
					}
				}
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Double.compare(d1[i], value) < 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Double.compare(d1[i], value) < 0;
					}
				}
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Double.compare(d1[i], value) <= 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Double.compare(d1[i], value) <= 0;
					}
				}
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Double.compare(d1[i], value) != 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Double.compare(d1[i], value) != 0;
					}
				}
			}
		} else if (relation == Relation.AND) {
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i];
				}
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
	
	private BoolArray calcRelation(BigDecimal value, int relation) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) == 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) == 0;
					}
				}
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) > 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) > 0;
					}
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) >= 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) >= 0;
					}
				}
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) < 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) < 0;
					}
				}
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) <= 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) <= 0;
					}
				}
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) != 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = new BigDecimal(d1[i]).compareTo(value) != 0;
					}
				}
			}
		} else if (relation == Relation.AND) {
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i];
				}
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

	protected BoolArray calcRelation(DateArray array, int relation) {
		boolean []s1 = this.signs;
		Date []datas2 = array.getDatas();
		
		if (relation == Relation.AND) {
			boolean []resultDatas = new boolean[size + 1];
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = datas2[i] != null;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && datas2[i] != null;
				}
			}
			
			BoolArray result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (relation == Relation.OR) {
			boolean []resultDatas = new boolean[size + 1];
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] || datas2[i] != null;
				}
			}
			
			BoolArray result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		}
	}
	
	protected BoolArray calcRelation(StringArray array, int relation) {
		boolean []s1 = this.signs;
		String []datas2 = array.getDatas();
		
		if (relation == Relation.AND) {
			boolean []resultDatas = new boolean[size + 1];
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = datas2[i] != null;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && datas2[i] != null;
				}
			}
			
			BoolArray result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (relation == Relation.OR) {
			boolean []resultDatas = new boolean[size + 1];
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] || datas2[i] != null;
				}
			}
			
			BoolArray result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		}
	}

	protected BoolArray calcRelation(ObjectArray array, int relation) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		Object []d2 = array.getDatas();
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = compare(d1[i], d2[i]) == 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = d2[i] == null;
					} else {
						resultDatas[i] = compare(d1[i], d2[i]) == 0;
					}
				}
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = compare(d1[i], d2[i]) > 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = compare(d1[i], d2[i]) > 0;
					}
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = compare(d1[i], d2[i]) >= 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = d2[i] == null;
					} else {
						resultDatas[i] = compare(d1[i], d2[i]) >= 0;
					}
				}
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = compare(d1[i], d2[i]) < 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = d2[i] != null;
					} else {
						resultDatas[i] = compare(d1[i], d2[i]) < 0;
					}
				}
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = compare(d1[i], d2[i]) <= 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = compare(d1[i], d2[i]) <= 0;
					}
				}
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = compare(d1[i], d2[i]) != 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = d2[i] != null;
					} else {
						resultDatas[i] = compare(d1[i], d2[i]) != 0;
					}
				}
			}
		} else if (relation == Relation.AND) {
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Variant.isTrue(d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && Variant.isTrue(d2[i]);
				}
			}
		} else { // Relation.OR
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] || Variant.isTrue(d2[i]);
				}
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
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		
		int size = size1;
		int result = 0;
		if (size1 < size2) {
			result = -1;
		} else if (size1 > size2) {
			result = 1;
			size = size2;
		}
		
		if (array instanceof IntArray) {
			IntArray array2 = (IntArray)array;
			int []d2 = array2.datas;
			boolean []s2 = array2.signs;
			
			for (int i = 1; i <= size; ++i) {
				if (s1 == null || !s1[i]) {
					if (s2 == null || !s2[i]) {
						if (d1[i] > d2[i]) {
							return 1;
						} else if (d1[i] < d2[i]) {
							return -1;
						}
					} else {
						return 1;
					}
				} else if (s2 == null || !s2[i]) {
					return -1;
				}
			}
		} else if (array instanceof LongArray) {
			LongArray array2 = (LongArray)array;
			long []d2 = array2.getDatas();
			boolean []s2 = array2.getSigns();
			
			for (int i = 1; i <= size; ++i) {
				if (s1 == null || !s1[i]) {
					if (s2 == null || !s2[i]) {
						if (d1[i] > d2[i]) {
							return 1;
						} else if (d1[i] < d2[i]) {
							return -1;
						}
					} else {
						return 1;
					}
				} else if (s2 == null || !s2[i]) {
					return -1;
				}
			}
		} else if (array instanceof DoubleArray) {
			DoubleArray array2 = (DoubleArray)array;
			double []d2 = array2.getDatas();
			boolean []s2 = array2.getSigns();
			
			for (int i = 1; i <= size; ++i) {
				if (s1 == null || !s1[i]) {
					if (s2 == null || !s2[i]) {
						int cmp = Double.compare(d1[i], d2[i]);
						if (cmp != 0) {
							return cmp;
						}
					} else {
						return 1;
					}
				} else if (s2 == null || !s2[i]) {
					return -1;
				}
			}
		} else if (array instanceof ConstArray) {
			Object value = array.get(1);
			if (value instanceof Long) {
				long d2 = ((Number)value).longValue();
				for (int i = 1; i <= size; ++i) {
					if (s1 == null || !s1[i]) {
						if (d1[i] > d2) {
							return 1;
						} else if (d1[i] < d2) {
							return -1;
						}
					} else {
						return -1;
					}
				}
			} else if (value instanceof Double || value instanceof Float) {
				double d2 = ((Number)value).doubleValue();
				for (int i = 1; i <= size; ++i) {
					if (s1 == null || !s1[i]) {
						int cmp = Double.compare(d1[i], d2);
						if (cmp != 0) {
							return cmp;
						}
					} else {
						return -1;
					}
				}
			} else if (value instanceof BigDecimal) {
				BigDecimal d2 = (BigDecimal)value;
				for (int i = 1; i <= size; ++i) {
					if (s1 == null || !s1[i]) {
						int cmp = new BigDecimal(d1[i]).compareTo(d2);
						if (cmp != 0) {
							return cmp;
						}
					} else {
						return -1;
					}
				}
			} else if (value instanceof BigInteger) {
				BigInteger d2 = (BigInteger)value;
				for (int i = 1; i <= size; ++i) {
					if (s1 == null || !s1[i]) {
						int cmp = BigInteger.valueOf(d1[i]).compareTo(d2);
						if (cmp != 0) {
							return cmp;
						}
					} else {
						return -1;
					}
				}
			} else if (value instanceof Number) {
				int d2 = ((Number)value).intValue();
				for (int i = 1; i <= size; ++i) {
					if (s1 == null || !s1[i]) {
						if (d1[i] > d2) {
							return 1;
						} else if (d1[i] < d2) {
							return -1;
						}
					} else {
						return -1;
					}
				}
			} else if (value == null) {
				if (s1 == null) {
					return 1;
				}
				
				for (int i = 1; i <= size; ++i) {
					if (!s1[i]) {
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
				if (s1 == null || !s1[i]) {
					int cmp = compare(d1[i], d2[i]);
					if (cmp != 0) {
						return cmp;
					}
				} else if (d2[i] != null) {
					return -1;
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
		if (signs == null) {
			return compare(datas[index1], datas[index2]);
		} else if (signs[index1]) {
			return signs[index2] ? 0 : -1;
		} else if (signs[index2]) {
			return 1;
		} else {
			return compare(datas[index1], datas[index2]);
		}
	}
	
	/**
	 * 判断数组的两个成员是否相等
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	public boolean isMemberEquals(int index1, int index2) {
		if (signs == null) {
			return datas[index1] == datas[index2];
		} else if (signs[index1]) {
			return signs[index2];
		} else if (signs[index2]) {
			return false;
		} else {
			return datas[index1] == datas[index2];
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
		if (isNull(curIndex)) {
			return array.isNull(index);
		} else if (array.isNull(index)) {
			return false;
		} else if (array instanceof IntArray) {
			return datas[curIndex] == array.getInt(index);
		} else if (array instanceof LongArray) {
			return datas[curIndex] == array.getLong(index);
		} else if (array instanceof DoubleArray) {
			return Double.compare(datas[curIndex], ((DoubleArray)array).getDouble(index)) == 0;
		} else {
			return compare(datas[curIndex], array.get(index)) == 0;
		}
	}
	
	/**
	 * 判断两个数组的指定元素是否相同
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要比较的数组
	 * @param index 要比较的数组的元素的索引
	 * @return true：相同，false：不相同
	 */
	public boolean isEquals(int curIndex, IntArray array, int index) {
		if (isNull(curIndex)) {
			return array.isNull(index);
		} else if (array.isNull(index)) {
			return false;
		} else {
			return datas[curIndex] == array.getInt(index);
		}
	}
	
	/**
	 * 判断数组的指定元素是否与给定值相等
	 * @param curIndex 数组元素索引，从1开始计数
	 * @param value 值
	 * @return true：相等，false：不相等
	 */
	public boolean isEquals(int curIndex, Object value) {
		if (signs == null || !signs[curIndex]) {
			if (value instanceof Integer) {
				return ((Integer)value).intValue() == datas[curIndex];
			} else if (value instanceof Long) {
				return ((Long)value).longValue() == datas[curIndex];
			} else if (value instanceof BigDecimal) {
				return new BigDecimal(datas[curIndex]).equals(value);
			} else if (value instanceof BigInteger) {
				return BigInteger.valueOf(datas[curIndex]).equals(value);
			} else if (value instanceof Number) {
				return Double.compare(datas[curIndex], ((Number)value).doubleValue()) == 0;
			} else {
				return false;
			}
		} else {
			return value == null;
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
		if (isNull(curIndex)) {
			return array.isNull(index) ? 0 : -1;
		} else if (array.isNull(index)) {
			return 1;
		} else if (array instanceof IntArray) {
			return compare(datas[curIndex], array.getInt(index));
		} else if (array instanceof LongArray) {
			return LongArray.compare(datas[curIndex], array.getLong(index));
		} else if (array instanceof DoubleArray) {
			return Double.compare(datas[curIndex], ((DoubleArray)array).getDouble(index));
		} else {
			return compare(datas[curIndex], array.get(index));
		}
	}
	
	/**
	 * 比较数组的指定元素与给定值的大小
	 * @param curIndex 当前数组的元素的索引
	 * @param value 要比较的值
	 * @return
	 */
	public int compareTo(int curIndex, Object value) {
		if (isNull(curIndex)) {
			return value == null ? 0 : -1;
		} else if (value == null) {
			return 1;
		} else {
			return compare(datas[curIndex], value);
		}
	}
	
	/**
	 * 取指定成员的哈希值
	 * @param index 成员索引，从1开始计数
	 * @return 指定成员的哈希值
	 */
	public int hashCode(int index) {
		if (signs == null || !signs[index]) {
			return Integer.hashCode(datas[index]);
		} else {
			return 0;
		}
	}
	
	/**
	 * 求成员和
	 * @return
	 */
	public Object sum() {
		int []datas = this.datas;
		boolean []signs = this.signs;
		int size = this.size;
		long sum = 0;
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				sum += datas[i];
			}
			
			return sum;
		} else {
			boolean sign = false;
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					sum += datas[i];
					sign = true;
				}
			}
			
			return sign ? sum : null;
		}
	}

	/**
	 * 求平均值
	 * @return
	 */
	public Object average() {
		int []datas = this.datas;
		boolean []signs = this.signs;
		int size = this.size;
		long sum = 0;
		int count = 0;
		
		if (signs == null) {
			count = size;
			for (int i = 1; i <= size; ++i) {
				sum += datas[i];
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					sum += datas[i];
					count++;
				}
			}
		}
		
		if (count != 0) {
			return (double)sum / count;
		} else {
			return null;
		}
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
		
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			int max = datas[1];
			for (int i = 2; i <= size; ++i) {
				if (max < datas[i]) {
					max = datas[i];
				}
			}
			
			return ObjectCache.getInteger(max);
		} else {
			int max = 0;
			int i = 1;
			for (; i <= size; ++i) {
				if (!signs[i]) {
					max = datas[i];
					break;
				}
			}
			
			if (i > size) {
				return null;
			}
			
			for (++i; i <= size; ++i) {
				if (!signs[i] && max < datas[i]) {
					max = datas[i];
				}
			}
			
			return ObjectCache.getInteger(max);
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
		
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			int min = datas[1];
			for (int i = 2; i <= size; ++i) {
				if (min > datas[i]) {
					min = datas[i];
				}
			}
			
			return ObjectCache.getInteger(min);
		} else {
			int min = 0;
			int i = 1;
			for (; i <= size; ++i) {
				if (!signs[i]) {
					min = datas[i];
					break;
				}
			}
			
			if (i > size) {
				return null;
			}
			
			for (++i; i <= size; ++i) {
				if (!signs[i] && min > datas[i]) {
					min = datas[i];
				}
			}
			
			return ObjectCache.getInteger(min);
		}
	}

	/**
	 * 计算两个数组的相对应的成员的关系运算，只计算result为真的行
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	public void calcRelations(IArray array, int relation, BoolArray result, boolean isAnd) {
		if (array instanceof IntArray) {
			calcRelations((IntArray)array, relation, result, isAnd);
		} else if (array instanceof LongArray) {
			calcRelations((LongArray)array, relation, result, isAnd);
		} else if (array instanceof DoubleArray) {
			calcRelations((DoubleArray)array, relation, result, isAnd);
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
		if (value instanceof Double || value instanceof Float) {
			calcRelations(((Number)value).doubleValue(), relation, result, isAnd);
		} else if (value instanceof BigDecimal) {
			calcRelations((BigDecimal)value, relation, result, isAnd);
		} else if (value instanceof BigInteger) {
			BigDecimal decimal = new BigDecimal((BigInteger)value);
			calcRelations(decimal, relation, result, isAnd);
		} else if (value instanceof Number) {
			calcRelations(((Number)value).longValue(), relation, result, isAnd);
		} else if (value == null) {
			ArrayUtil.calcRelationsNull(signs, size, relation, result, isAnd);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
					getDataType(), Variant.getDataType(value)));
		}
	}
	
	private void calcRelations(IntArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []d2 = array.datas;
		boolean []s2 = array.signs;
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] != d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || d1[i] != d2[i])) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || d1[i] != d2[i])) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] != s2[i] || (!s1[i] && d1[i] != d2[i]))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] <= d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && d1[i] <= d2[i]) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || d1[i] <= d2[i])) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || (!s2[i] && d1[i] <= d2[i]))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] < d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && d1[i] < d2[i]) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || d1[i] < d2[i])) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s2[i] && (s1[i] || d1[i] < d2[i])) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] >= d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || d1[i] >= d2[i])) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && d1[i] >= d2[i]) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s2[i] || (!s1[i] && d1[i] >= d2[i]))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] > d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || d1[i] > d2[i])) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && d1[i] > d2[i]) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && (s2[i] || d1[i] > d2[i])) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] == d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && d1[i] == d2[i]) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && d1[i] == d2[i]) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && s1[i] == s2[i] && (s1[i] || d1[i] == d2[i])) {
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
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] == d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && d1[i] == d2[i]) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && d1[i] == d2[i]) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] ? s2[i] : !s2[i] && d1[i] == d2[i])) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] > d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || d1[i] > d2[i])) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && d1[i] > d2[i]) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && (s2[i] || d1[i] > d2[i])) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] >= d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || d1[i] >= d2[i])) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && d1[i] >= d2[i]) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s2[i] || (!s1[i] && d1[i] >= d2[i]))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] < d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && d1[i] < d2[i]) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || d1[i] < d2[i])) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s2[i] && (s1[i] || (d1[i] < d2[i]))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] <= d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && d1[i] <= d2[i]) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || d1[i] <= d2[i])) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || (!s2[i] && d1[i] <= d2[i]))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] != d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || d1[i] != d2[i])) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || d1[i] != d2[i])) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] ? !s2[i] : (s2[i] || d1[i] != d2[i]))) {
							resultDatas[i] = true;
						}
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	protected void calcRelations(LongArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] != d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || d1[i] != d2[i])) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || d1[i] != d2[i])) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] != s2[i] || (!s1[i] && d1[i] != d2[i]))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] <= d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && d1[i] <= d2[i]) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || d1[i] <= d2[i])) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || (!s2[i] && d1[i] <= d2[i]))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] < d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && d1[i] < d2[i]) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || d1[i] < d2[i])) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s2[i] && (s1[i] || d1[i] < d2[i])) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] >= d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || d1[i] >= d2[i])) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && d1[i] >= d2[i]) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s2[i] || (!s1[i] && d1[i] >= d2[i]))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] > d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || d1[i] > d2[i])) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && d1[i] > d2[i]) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && (s2[i] || d1[i] > d2[i])) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && d1[i] == d2[i]) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && d1[i] == d2[i]) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && d1[i] == d2[i]) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && s1[i] == s2[i] && (s1[i] || d1[i] == d2[i])) {
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
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] == d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && d1[i] == d2[i]) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && d1[i] == d2[i]) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] ? s2[i] : !s2[i] && d1[i] == d2[i])) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] > d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || d1[i] > d2[i])) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && d1[i] > d2[i]) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && (s2[i] || d1[i] > d2[i])) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] >= d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || d1[i] >= d2[i])) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && d1[i] >= d2[i]) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s2[i] || (!s1[i] && d1[i] >= d2[i]))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] < d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && d1[i] < d2[i]) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || d1[i] < d2[i])) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s2[i] && (s1[i] || (d1[i] < d2[i]))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] <= d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && d1[i] <= d2[i]) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || d1[i] <= d2[i])) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || (!s2[i] && d1[i] <= d2[i]))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && d1[i] != d2[i]) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || d1[i] != d2[i])) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || d1[i] != d2[i])) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] ? !s2[i] : (s2[i] || d1[i] != d2[i]))) {
							resultDatas[i] = true;
						}
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	protected void calcRelations(DoubleArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && Double.compare(d1[i], d2[i]) != 0) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || Double.compare(d1[i], d2[i]) != 0)) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || Double.compare(d1[i], d2[i]) != 0)) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] != s2[i] || (!s1[i] && Double.compare(d1[i], d2[i]) != 0))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && Double.compare(d1[i], d2[i]) <= 0) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && Double.compare(d1[i], d2[i]) <= 0) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || Double.compare(d1[i], d2[i]) <= 0)) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || (!s2[i] && Double.compare(d1[i], d2[i]) <= 0))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && Double.compare(d1[i], d2[i]) < 0) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && Double.compare(d1[i], d2[i]) < 0) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || Double.compare(d1[i], d2[i]) < 0)) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s2[i] && (s1[i] || Double.compare(d1[i], d2[i]) < 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && Double.compare(d1[i], d2[i]) >= 0) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || Double.compare(d1[i], d2[i]) >= 0)) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && Double.compare(d1[i], d2[i]) >= 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s2[i] || (!s1[i] && Double.compare(d1[i], d2[i]) >= 0))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && Double.compare(d1[i], d2[i]) > 0) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || Double.compare(d1[i], d2[i]) > 0)) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && Double.compare(d1[i], d2[i]) > 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && (s2[i] || Double.compare(d1[i], d2[i]) > 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && Double.compare(d1[i], d2[i]) == 0) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && Double.compare(d1[i], d2[i]) == 0) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && Double.compare(d1[i], d2[i]) == 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && s1[i] == s2[i] && (s1[i] || Double.compare(d1[i], d2[i]) == 0)) {
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
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && Double.compare(d1[i], d2[i]) == 0) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && Double.compare(d1[i], d2[i]) == 0) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && Double.compare(d1[i], d2[i]) == 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] ? s2[i] : !s2[i] && Double.compare(d1[i], d2[i]) == 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && Double.compare(d1[i], d2[i]) > 0) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || Double.compare(d1[i], d2[i]) > 0)) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && Double.compare(d1[i], d2[i]) > 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && (s2[i] || Double.compare(d1[i], d2[i]) > 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && Double.compare(d1[i], d2[i]) >= 0) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || Double.compare(d1[i], d2[i]) >= 0)) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && Double.compare(d1[i], d2[i]) >= 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s2[i] || (!s1[i] && Double.compare(d1[i], d2[i]) >= 0))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && Double.compare(d1[i], d2[i]) < 0) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && Double.compare(d1[i], d2[i]) < 0) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || Double.compare(d1[i], d2[i]) < 0)) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s2[i] && (s1[i] || (Double.compare(d1[i], d2[i]) < 0))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && Double.compare(d1[i], d2[i]) <= 0) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && Double.compare(d1[i], d2[i]) <= 0) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || Double.compare(d1[i], d2[i]) <= 0)) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || (!s2[i] && Double.compare(d1[i], d2[i]) <= 0))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && Double.compare(d1[i], d2[i]) != 0) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || Double.compare(d1[i], d2[i]) != 0)) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || Double.compare(d1[i], d2[i]) != 0)) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] ? !s2[i] : (s2[i] || Double.compare(d1[i], d2[i]) != 0))) {
							resultDatas[i] = true;
						}
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	private void calcRelations(long value, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && d1[i] != value) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || d1[i] != value)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && d1[i] <= value) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || d1[i] <= value)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && d1[i] < value) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || d1[i] < value)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && d1[i] >= value) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && d1[i] >= value) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && d1[i] > value) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && d1[i] > value) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && d1[i] == value) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && d1[i] == value) {
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
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && d1[i] == value) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && d1[i] == value) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && d1[i] > value) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && d1[i] > value) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && d1[i] >= value) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && d1[i] >= value) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && d1[i] < value) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || d1[i] < value)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && d1[i] <= value) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || d1[i] <= value)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && d1[i] != value) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || d1[i] != value)) {
							resultDatas[i] = true;
						}
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}
	
	private void calcRelations(double value, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && Double.compare(d1[i], value) != 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || Double.compare(d1[i], value) != 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && Double.compare(d1[i], value) <= 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || Double.compare(d1[i], value) <= 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && Double.compare(d1[i], value) < 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || Double.compare(d1[i], value) < 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && Double.compare(d1[i], value) >= 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && Double.compare(d1[i], value) >= 0) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && Double.compare(d1[i], value) > 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && Double.compare(d1[i], value) > 0) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && Double.compare(d1[i], value) == 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && Double.compare(d1[i], value) == 0) {
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
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && Double.compare(d1[i], value) == 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && Double.compare(d1[i], value) == 0) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && Double.compare(d1[i], value) > 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && Double.compare(d1[i], value) > 0) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && Double.compare(d1[i], value) >= 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && Double.compare(d1[i], value) >= 0) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && Double.compare(d1[i], value) < 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || Double.compare(d1[i], value) < 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && Double.compare(d1[i], value) <= 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || Double.compare(d1[i], value) <= 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && Double.compare(d1[i], value) != 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || Double.compare(d1[i], value) != 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}
	
	private void calcRelations(BigDecimal value, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) != 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || new BigDecimal(d1[i]).compareTo(value) != 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) <= 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || new BigDecimal(d1[i]).compareTo(value) <= 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) < 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || new BigDecimal(d1[i]).compareTo(value) < 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) >= 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && new BigDecimal(d1[i]).compareTo(value) >= 0) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) > 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && new BigDecimal(d1[i]).compareTo(value) > 0) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) == 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && new BigDecimal(d1[i]).compareTo(value) == 0) {
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
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) == 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && new BigDecimal(d1[i]).compareTo(value) == 0) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) > 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && new BigDecimal(d1[i]).compareTo(value) > 0) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) >= 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && new BigDecimal(d1[i]).compareTo(value) >= 0) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) < 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || new BigDecimal(d1[i]).compareTo(value) < 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) <= 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || new BigDecimal(d1[i]).compareTo(value) <= 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && new BigDecimal(d1[i]).compareTo(value) != 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || new BigDecimal(d1[i]).compareTo(value) != 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	protected void calcRelations(ObjectArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		Object []d2 = array.getDatas();
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && compare(d1[i], d2[i]) != 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] ? d2[i] != null : compare(d1[i], d2[i]) != 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && compare(d1[i], d2[i]) <= 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || compare(d1[i], d2[i]) <= 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && compare(d1[i], d2[i]) < 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] ? d2[i] != null : compare(d1[i], d2[i]) < 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && compare(d1[i], d2[i]) >= 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] ? d2[i] == null : compare(d1[i], d2[i]) >= 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && compare(d1[i], d2[i]) > 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && compare(d1[i], d2[i]) > 0) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && compare(d1[i], d2[i]) == 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] ? d2[i] == null : compare(d1[i], d2[i]) == 0)) {
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
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && compare(d1[i], d2[i]) == 0) {
							resultDatas[i] = true;
						}
					}

				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] ? d2[i] == null : compare(d1[i], d2[i]) == 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && compare(d1[i], d2[i]) > 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && compare(d1[i], d2[i]) > 0) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && compare(d1[i], d2[i]) >= 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (d2[i] == null || (!s1[i] && compare(d1[i], d2[i]) >= 0))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && compare(d1[i], d2[i]) < 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && d2[i] != null && (s1[i] || (compare(d1[i], d2[i]) < 0))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && compare(d1[i], d2[i]) <= 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || compare(d1[i], d2[i]) <= 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && compare(d1[i], d2[i]) != 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] ? d2[i] != null : compare(d1[i], d2[i]) != 0)) {
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
	 * 计算两个数组的相对应的成员的按位与
	 * @param array 右侧数组
	 * @return 按位与结果数组
	 */
	public IArray bitwiseAnd(IArray array) {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (array instanceof IntArray) {
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (array.isNull(i)) {
						result.pushNull();
					} else {
						result.pushInt(datas[i] & array.getInt(i));
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i] || array.isNull(i)) {
						result.pushNull();
					} else {
						result.pushInt(datas[i] & array.getInt(i));
					}
				}
			}
			
			return result;
		} else if (array instanceof ConstArray) {
			Object value = array.get(1);
			if (value == null) {
				return new ConstArray(null, size);
			} else if (!(value instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("and" + mm.getMessage("function.paramTypeError"));
			}
			
			int n = ((Number)value).intValue();
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					result.pushInt(datas[i] & n);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						result.pushNull();
					} else {
						result.pushInt(datas[i] & n);
					}
				}
			}
			
			return result;
		} else {
			return array.bitwiseAnd(this);
		}
	}
	
	/**
	 * 计算两个数组的相对应的成员的按位或
	 * @param array 右侧数组
	 * @return 按位或结果数组
	 */
	public IArray bitwiseOr(IArray array) {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (array instanceof IntArray) {
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (array.isNull(i)) {
						result.pushNull();
					} else {
						result.pushInt(datas[i] | array.getInt(i));
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i] || array.isNull(i)) {
						result.pushNull();
					} else {
						result.pushInt(datas[i] | array.getInt(i));
					}
				}
			}
			
			return result;
		} else if (array instanceof ConstArray) {
			Object value = array.get(1);
			if (value == null) {
				return new ConstArray(null, size);
			} else if (!(value instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("or" + mm.getMessage("function.paramTypeError"));
			}
			
			int n = ((Number)value).intValue();
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					result.pushInt(datas[i] | n);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						result.pushNull();
					} else {
						result.pushInt(datas[i] | n);
					}
				}
			}
			
			return result;
		} else {
			return array.bitwiseOr(this);
		}
	}

	/**
	 * 计算两个数组的相对应的成员的按位异或
	 * @param array 右侧数组
	 * @return 按位异或结果数组
	 */
	public IArray bitwiseXOr(IArray array) {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (array instanceof IntArray) {
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (array.isNull(i)) {
						result.pushNull();
					} else {
						result.pushInt(datas[i] ^ array.getInt(i));
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i] || array.isNull(i)) {
						result.pushNull();
					} else {
						result.pushInt(datas[i] ^ array.getInt(i));
					}
				}
			}
			
			return result;
		} else if (array instanceof ConstArray) {
			Object value = array.get(1);
			if (value == null) {
				return new ConstArray(null, size);
			} else if (!(value instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xor" + mm.getMessage("function.paramTypeError"));
			}
			
			int n = ((Number)value).intValue();
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					result.pushInt(datas[i] ^ n);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs[i]) {
						result.pushNull();
					} else {
						result.pushInt(datas[i] ^ n);
					}
				}
			}
			
			return result;
		} else {
			return array.bitwiseXOr(this);
		}
	}
	
	/**
	 * 计算数组成员的按位取反
	 * @return 成员按位取反结果数组
	 */
	public IArray bitwiseNot() {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (isTemporary()) {
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					datas[i] = ~datas[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!signs[i]) {
						datas[i] = ~datas[i];
					}
				}
			}
			
			return this;
		} else {
			int []resultDatas = new int[size + 1];
			boolean []resultSigns = null;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = ~datas[i];
				}
			} else {
				resultSigns = new boolean[size + 1];
				System.arraycopy(signs, 1, resultSigns, 1, size);
				
				for (int i = 1; i <= size; ++i) {
					if (!signs[i]) {
						resultDatas[i] = ~datas[i];
					}
				}
			}
			
			IArray result = new IntArray(resultDatas, resultSigns, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	/**
	 * 取出标识数组取值为真的行对应的数据，组成新数组
	 * @param signArray 标识数组
	 * @return IArray
	 */
	public IArray select(IArray signArray) {
		int size = signArray.size();
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []resultDatas = new int[size + 1];
		int resultSize = 0;
		
		if (s1 == null) {
			if (signArray instanceof BoolArray) {
				BoolArray array = (BoolArray)signArray;
				boolean []d2 = array.getDatas();
				boolean []s2 = array.getSigns();
				
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (d2[i]) {
							resultDatas[++resultSize] = d1[i];
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!s2[i] && d2[i]) {
							resultDatas[++resultSize] = d1[i];
						}
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						resultDatas[++resultSize] = d1[i];
					}
				}
			}
			
			return new IntArray(resultDatas, null, resultSize);
		} else {
			boolean []resultSigns = new boolean[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (signArray.isTrue(i)) {
					++resultSize;
					if (s1[i]) {
						resultSigns[resultSize] = true;
					} else {
						resultDatas[resultSize] = d1[i];
					}
				}
			}
			
			return new IntArray(resultDatas, resultSigns, resultSize);
		}
	}
	
	/**
	 * 取某一区段标识数组取值为真的行组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @param signArray 标识数组
	 * @return IArray
	 */
	public IArray select(int start, int end, IArray signArray) {
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []resultDatas = new int[end - start + 1];
		int resultSize = 0;
		
		if (s1 == null) {
			if (signArray instanceof BoolArray) {
				BoolArray array = (BoolArray)signArray;
				boolean []d2 = array.getDatas();
				boolean []s2 = array.getSigns();
				
				if (s2 == null) {
					for (int i = start; i < end; ++i) {
						if (d2[i]) {
							resultDatas[++resultSize] = d1[i];
						}
					}
				} else {
					for (int i = start; i < end; ++i) {
						if (!s2[i] && d2[i]) {
							resultDatas[++resultSize] = d1[i];
						}
					}
				}
			} else {
				for (int i = start; i < end; ++i) {
					if (signArray.isTrue(i)) {
						resultDatas[++resultSize] = d1[i];
					}
				}
			}
			
			return new IntArray(resultDatas, null, resultSize);
		} else {
			boolean []resultSigns = new boolean[end - start + 1];
			for (int i = start; i < end; ++i) {
				if (signArray.isTrue(i)) {
					++resultSize;
					if (s1[i]) {
						resultSigns[resultSize] = true;
					} else {
						resultDatas[resultSize] = d1[i];
					}
				}
			}
			
			return new IntArray(resultDatas, resultSigns, resultSize);
		}
	}

	/**
	 * 把array的指定元素加到当前数组的指定元素上
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要相加的数组
	 * @param index 要相加的数组的元素的索引
	 * @return IArray
	 */
	public IArray memberAdd(int curIndex, IArray array, int index) {
		// 加法都产生LongArray进行运算，不会调用到此方法
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
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		Object []result = new Object[size];
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				result[i - 1] = ObjectCache.getInteger(datas[i]);
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					result[i - 1] = ObjectCache.getInteger(datas[i]);
				}
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
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				result[i - 1] = ObjectCache.getInteger(datas[i]);
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					result[i - 1] = ObjectCache.getInteger(datas[i]);
				}
			}
		}
	}
	
	/**
	 * 把数组从指定位置拆成两个数组
	 * @param pos 位置，包含
	 * @return 返回后半部分元素构成的数组
	 */
	public IArray split(int pos) {
		int size = this.size;
		boolean []signs = this.signs;
		int resultSize = size - pos + 1;
		int []resultDatas = new int[resultSize + 1];
		System.arraycopy(datas, pos, resultDatas, 1, resultSize);
		
		if (signs == null) {
			this.size = pos - 1;
			return new IntArray(resultDatas, null, resultSize);
		} else {
			boolean []resultSigns = new boolean[resultSize + 1];
			System.arraycopy(signs, pos, resultSigns, 1, resultSize);
			for (int i = pos; i <= size; ++i) {
				signs[i] = false;
			}
			
			this.size = pos - 1;
			return new IntArray(resultDatas, resultSigns, resultSize);
		}
	}
	
	/**
	 * 把指定区间元素分离出来组成新数组
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 * @return
	 */
	public IArray split(int from, int to) {
		int oldSize = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		int resultSize = to - from + 1;
		int []resultDatas = new int[resultSize + 1];
		System.arraycopy(datas, from, resultDatas, 1, resultSize);
		
		System.arraycopy(datas, to + 1, datas, from, oldSize - to);
		this.size -= resultSize;
		
		if (signs == null) {
			return new IntArray(resultDatas, null, resultSize);
		} else {
			boolean []resultSigns = new boolean[resultSize + 1];
			System.arraycopy(signs, from, resultSigns, 1, resultSize);
			System.arraycopy(signs, to + 1, signs, from, oldSize - to);
			return new IntArray(resultDatas, resultSigns, resultSize);
		}
	}
	
	/**
	 * 对数组的元素进行排序
	 */
	public void sort() {
		int size = this.size;
		boolean []signs = this.signs;
		
		if (signs == null) {
			MultithreadUtil.sort(datas, 1, size + 1);
			return;
		}

		int nullCount = 0;
		for (int i = 1; i <= size; ++i) {
			if (signs[i]) {
				nullCount++;
			}
		}
		
		if (nullCount == 0) {
			MultithreadUtil.sort(datas, 1, size + 1);
			this.signs = null;
		} else if (nullCount != size) {
			// 把空元素移到数组前面
			int []datas = this.datas;
			for (int i = size, q = size; i > 0; --i) {
				if (signs[i]) {
					signs[i] = false;
				} else {
					datas[q--] = datas[i];
				}
			}
			
			// 对非空元素进行排序
			MultithreadUtil.sort(datas, nullCount + 1, size + 1);
			for (int i = 1; i <= nullCount; ++i) {
				signs[i] = true;
			}
		}
	}

	/**
	 * 对数组的元素进行排序
	 * @param comparator 比较器
	 */
	public void sort(Comparator<Object> comparator) {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		Integer []values = new Integer[size + 1];
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				values[i] = ObjectCache.getInteger(datas[i]);
			}
			
			MultithreadUtil.sort(values, 1, size + 1, comparator);
			
			for (int i = 1; i <= size; ++i) {
				datas[i] = values[i].intValue();
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					values[i] = ObjectCache.getInteger(datas[i]);
				}
			}			
			
			MultithreadUtil.sort(values, 1, size + 1, comparator);
			
			for (int i = 1; i <= size; ++i) {
				if (values[i] != null) {
					datas[i] = values[i].intValue();
					signs[i] = false;
				} else {
					signs[i] = true;
				}
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
		int []datas = this.datas;
		boolean []signs = this.signs;
		int []resultDatas = new int[size + 1];
		
		if (signs == null) {
			for (int i = 1, q = size; i <= size; ++i) {
				resultDatas[i] = datas[q--];
			}
			
			return new IntArray(resultDatas, null, size);
		} else {
			boolean []resultSigns = new boolean[size + 1];
			for (int i = 1, q = size; i <= size; ++i, --q) {
				if (signs[q]) {
					resultSigns[i] = true;
				} else {
					resultDatas[i] = datas[q];
				}
			}
			
			return new IntArray(resultDatas, resultSigns, size);
		}
	}

	/**
	 * 对数组元素从小到大做排序，取前count个的位置
	 * @param count 如果count小于0则取后|count|名的位置
	 * @param isAll count为正负1时，如果isAll取值为true则取所有排名第一的元素的位置，否则只取一个
	 * @param isLast 是否从后开始找
	 * @param ignoreNull 是否忽略空元素
	 * @return IntArray
	 */
	public IntArray ptop(int count, boolean isAll, boolean isLast, boolean ignoreNull) {
		int size = this.size;
		if (size == 0) {
			return new IntArray(0);
		}
		
		int []datas = this.datas;
		boolean []signs = this.signs;
		if (ignoreNull) {
			if (count == 1) {
				// 取最小值的位置
				int minValue = 0;
				if (isAll) {
					IntArray result = new IntArray(8);
					int i = 1;
					for (; i <= size; ++i) {
						if (signs == null || !signs[i]) {
							minValue = datas[i];
							result.addInt(i);
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (signs == null || !signs[i]) {
							int cmp = compare(datas[i], minValue);
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
						if (signs == null || !signs[i]) {
							minValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (--i; i > 0; --i) {
						if ((signs == null || !signs[i]) && compare(datas[i], minValue) < 0) {
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
						if (signs == null || !signs[i]) {
							minValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if ((signs == null || !signs[i]) && compare(datas[i], minValue) < 0) {
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
				IntArray valueArray = new IntArray(next);
				IntArray posArray = new IntArray(next);
				for (int i = 1; i <= size; ++i) {
					if (signs == null || !signs[i]) {
						int index = valueArray.binarySearch(datas[i]);
						if (index < 1) {
							index = -index;
						}
						
						if (index <= count) {
							valueArray.insertInt(index, datas[i]);
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
				int maxValue = 0;
				if (isAll) {
					IntArray result = new IntArray(8);
					int i = 1;
					for (; i <= size; ++i) {
						if (signs == null || !signs[i]) {
							maxValue = datas[i];
							result.addInt(i);
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (signs == null || !signs[i]) {
							int cmp = compare(datas[i], maxValue);
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
						if (signs == null || !signs[i]) {
							maxValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (--i; i > 0; --i) {
						if ((signs == null || !signs[i]) && compare(datas[i], maxValue) > 0) {
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
						if (signs == null || !signs[i]) {
							maxValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if ((signs == null || !signs[i]) && compare(datas[i], maxValue) > 0) {
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
				IntArray valueArray = new IntArray(next);
				IntArray posArray = new IntArray(next);
				for (int i = 1; i <= size; ++i) {
					if (signs == null || !signs[i]) {
						int index = valueArray.descBinarySearch(datas[i]);
						if (index < 1) {
							index = -index;
						}
						
						if (index <= count) {
							valueArray.insertInt(index, datas[i]);
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
					if (signs != null) {
						for (int i = 1; i <= size; ++i) {
							if (signs[i]) {
								result.addInt(i);
							}
						}
						
						if (result.size() > 0) {
							return result;
						}
					}
					
					result.addInt(1);
					int minValue = datas[1];
					
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
					if (signs != null) {
						for (int i = size; i > 0; --i) {
							if (signs[i]) {
								IntArray result = new IntArray(1);
								result.pushInt(i);
								return result;
							}
						}
					}
					
					int minValue = datas[size];
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
					if (signs != null) {
						for (int i = 1; i <= size; ++i) {
							if (signs[i]) {
								IntArray result = new IntArray(1);
								result.pushInt(i);
								return result;
							}
						}
					}
					
					int minValue = datas[1];
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
				IntArray valueArray = new IntArray(next);
				IntArray posArray = new IntArray(next);
				
				for (int i = 1; i <= size; ++i) {
					if (signs == null || !signs[i]) {
						int index = valueArray.binarySearch(datas[i]);
						if (index < 1) {
							index = -index;
						}
						
						if (index <= count) {
							valueArray.insertInt(index, datas[i]);
							posArray.insertInt(index, i);
							if (valueArray.size() == next) {
								valueArray.removeLast();
								posArray.removeLast();
							}
						}
					} else {
						valueArray.insert(1, null);
						posArray.insertInt(1, i);
						if (valueArray.size() == next) {
							valueArray.removeLast();
							posArray.removeLast();
						}
					}
				}
				
				return posArray;
			} else if (count == -1) {
				// 取最大值的位置
				int maxValue = 0;
				if (isAll) {
					IntArray result = new IntArray(8);
					int i = 1;
					for (; i <= size; ++i) {
						if (signs == null || !signs[i]) {
							maxValue = datas[i];
							result.addInt(i);
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (signs == null || !signs[i]) {
							int cmp = compare(datas[i], maxValue);
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
						if (signs == null || !signs[i]) {
							maxValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (--i; i > 0; --i) {
						if ((signs == null || !signs[i]) && compare(datas[i], maxValue) > 0) {
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
						if (signs == null || !signs[i]) {
							maxValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if ((signs == null || !signs[i]) && compare(datas[i], maxValue) > 0) {
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
				IntArray valueArray = new IntArray(next);
				IntArray posArray = new IntArray(next);
				for (int i = 1; i <= size; ++i) {
					if (signs == null || !signs[i]) {
						int index = valueArray.descBinarySearch(datas[i]);
						if (index < 1) {
							index = -index;
						}
						
						if (index <= count) {
							valueArray.insertInt(index, datas[i]);
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
		}
	}
	
	/**
	 * 对数组元素从小到大做排名，取前count名的位置
	 * @param count 如果count小于0则从大到小做排名
	 * @param ignoreNull 是否忽略空元素
	 * @param iopt 是否按去重方式做排名
	 * @return IntArray
	 */
	public IntArray ptopRank(int count, boolean ignoreNull, boolean iopt) {
		int size = this.size;
		if (size == 0 || count == 0) {
			return new IntArray(0);
		}
		
		int []datas = this.datas;
		boolean []signs = this.signs;
		
		if (count > 0) {
			// 取最小的count个元素的位置
			int next = count + 1;
			IntArray valueArray = new IntArray(next);
			IntArray posArray = new IntArray(next);

			if (iopt) {
				int curCount = 0;
				for (int i = 1; i <= size; ++i) {
					if (signs == null || !signs[i]) {
						if (curCount < count) {
							int index = valueArray.binarySearch(datas[i]);
							if (index < 1) {
								curCount++;
								index = -index;
							}
							
							valueArray.insertInt(index, datas[i]);
							posArray.insertInt(index, i);
						} else {
							int curSize = valueArray.size();
							int cmp = compareTo(i, valueArray, curSize);
							if (cmp < 0) {
								int index = valueArray.binarySearch(datas[i]);
								if (index < 1) {
									index = -index;
									
									// 删除最后相同的成员
									int value = valueArray.getInt(curSize);
									valueArray.removeLast();
									posArray.removeLast();
									for (int j = curSize - 1; j >= count; --j) {
										if (valueArray.getInt(j) == value) {
											valueArray.removeLast();
											posArray.removeLast();
										} else {
											break;
										}
									}
								}
								
								valueArray.insertInt(index, datas[i]);
								posArray.insertInt(index, i);
							} else if (cmp == 0) {
								valueArray.addInt(datas[i]);
								posArray.addInt(i);
							}
						}
					} else if (!ignoreNull) {
						if (curCount < count) {
							if (curCount == 0 || !valueArray.isNull(1)) {
								curCount++;
							}
							
							valueArray.insert(1, null);
							posArray.insertInt(1, i);
						} else {
							if (valueArray.isNull(1)) {
								valueArray.addInt(datas[i]);
								posArray.addInt(i);
							} else {
								// 删除最后相同的成员
								int curSize = valueArray.size();
								int value = valueArray.getInt(curSize);
								valueArray.removeLast();
								posArray.removeLast();
								for (int j = curSize - 1; j >= count; --j) {
									if (valueArray.getInt(j) == value) {
										valueArray.removeLast();
										posArray.removeLast();
									} else {
										break;
									}
								}
								
								valueArray.insert(1, null);
								posArray.insertInt(1, i);
							}
						}
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs == null || !signs[i]) {
						int curSize = valueArray.size();
						if (curSize < count) {
							int index = valueArray.binarySearch(datas[i]);
							if (index < 1) {
								index = -index;
							}
							
							valueArray.insertInt(index, datas[i]);
							posArray.insertInt(index, i);
						} else {
							int cmp = compareTo(i, valueArray, curSize);
							if (cmp < 0) {
								int index = valueArray.binarySearch(datas[i]);
								if (index < 1) {
									index = -index;
								}
								
								valueArray.insertInt(index, datas[i]);
								posArray.insertInt(index, i);
								
								if (valueArray.memberCompare(count, curSize + 1) != 0) {
									// 删除最后相同的成员
									int value = valueArray.getInt(curSize + 1);
									valueArray.removeLast();
									posArray.removeLast();
									for (int j = curSize; j > count; --j) {
										if (valueArray.getInt(j) == value) {
											valueArray.removeLast();
											posArray.removeLast();
										} else {
											break;
										}
									}
								}
							} else if (cmp == 0) {
								valueArray.addInt(datas[i]);
								posArray.addInt(i);
							}
						}
					} else if (!ignoreNull) {
						int curSize = valueArray.size();
						if (curSize < count) {
							valueArray.insert(1, null);
							posArray.insertInt(1, i);
						} else {
							if (valueArray.isNull(curSize)) {
								valueArray.addInt(datas[i]);
								posArray.addInt(i);
							} else {
								valueArray.insert(1, null);
								posArray.insertInt(1, i);
								
								if (valueArray.memberCompare(count, curSize + 1) != 0) {
									// 删除最后相同的成员
									int value = valueArray.getInt(curSize + 1);
									valueArray.removeLast();
									posArray.removeLast();
									for (int j = curSize; j > count; --j) {
										if (valueArray.getInt(j) == value) {
											valueArray.removeLast();
											posArray.removeLast();
										} else {
											break;
										}
									}
								}
							}
						}
					}
				}
			}
			
			return posArray;
		} else {
			// 取最大的count个元素的位置
			count = -count;
			int next = count + 1;
			IntArray valueArray = new IntArray(next);
			IntArray posArray = new IntArray(next);

			if (iopt) {
				int curCount = 0;
				for (int i = 1; i <= size; ++i) {
					if (signs == null || !signs[i]) {
						if (curCount < count) {
							int index = valueArray.descBinarySearch(datas[i]);
							if (index < 1) {
								curCount++;
								index = -index;
							}
							
							valueArray.insertInt(index, datas[i]);
							posArray.insertInt(index, i);
						} else {
							int curSize = valueArray.size();
							int cmp = compareTo(i, valueArray, curSize);
							if (cmp > 0) {
								int index = valueArray.descBinarySearch(datas[i]);
								if (index < 1) {
									index = -index;
									
									// 删除最后相同的成员
									int value = valueArray.getInt(curSize);
									valueArray.removeLast();
									posArray.removeLast();
									for (int j = curSize - 1; j >= count; --j) {
										if (valueArray.getInt(j) == value) {
											valueArray.removeLast();
											posArray.removeLast();
										} else {
											break;
										}
									}
								}
								
								valueArray.insertInt(index, datas[i]);
								posArray.insertInt(index, i);
							} else if (cmp == 0) {
								valueArray.addInt(datas[i]);
								posArray.addInt(i);
							}
						}
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signs == null || !signs[i]) {
						int curSize = valueArray.size();
						if (curSize < count) {
							int index = valueArray.descBinarySearch(datas[i]);
							if (index < 1) {
								index = -index;
							}
							
							valueArray.insertInt(index, datas[i]);
							posArray.insertInt(index, i);
						} else {
							int cmp = compareTo(i, valueArray, curSize);
							if (cmp > 0) {
								int index = valueArray.descBinarySearch(datas[i]);
								if (index < 1) {
									index = -index;
								}
								
								valueArray.insertInt(index, datas[i]);
								posArray.insertInt(index, i);
								
								if (valueArray.memberCompare(count, curSize + 1) != 0) {
									// 删除最后相同的成员
									int value = valueArray.getInt(curSize + 1);
									valueArray.removeLast();
									posArray.removeLast();
									for (int j = curSize; j > count; --j) {
										if (valueArray.getInt(j) == value) {
											valueArray.removeLast();
											posArray.removeLast();
										} else {
											break;
										}
									}
								}
							} else if (cmp == 0) {
								valueArray.addInt(datas[i]);
								posArray.addInt(i);
							}
						}
					}
				}
			}
			
			return posArray;
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
		int []datas = this.datas;
		boolean []signs = this.signs;
		Object []resultDatas = new Object[size + 1];
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = ObjectCache.getInteger(datas[i]);
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					resultDatas[i] = ObjectCache.getInteger(datas[i]);
				}
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
	 * 修改数组指定元素的值
	 * @param index 索引，从1开始计数
	 * @param value 值
	 */
	public void setInt(int index, int value) {
		datas[index] = value;
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
		int []datas = this.datas;
		boolean []signs = this.signs;
		IArray result;
		
		if (other instanceof IntArray) {
			IntArray otherArray = (IntArray)other;
			if (isTemporary()) {
				result = this;
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						if (otherArray.isNull(i)) {
							if (signs == null) {
								signs = new boolean[size + 1];
								this.signs = signs;
							}
							
							signs[i] = true;
						} else {
							datas[i] = otherArray.getInt(i);
							if (signs != null) {
								signs[i] = false;
							}
						}
					}
				}
			} else {
				IntArray resultArray = new IntArray(size);
				result = resultArray;
				
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						if (signArray.isTrue(i)) {
							resultArray.pushInt(datas[i]);
						} else if (otherArray.isNull(i)) {
							resultArray.pushNull();
						} else {
							resultArray.pushInt(otherArray.getInt(i));
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signArray.isTrue(i)) {
							if (signs[i]) {
								resultArray.pushNull();
							} else {
								resultArray.pushInt(datas[i]);
							}
						} else if (otherArray.isNull(i)) {
							resultArray.pushNull();
						} else {
							resultArray.pushInt(otherArray.getInt(i));
						}
					}
				}
			}
		} else if (other instanceof LongArray) {
			LongArray otherArray = (LongArray)other;
			LongArray resultArray = new LongArray(size);
			result = resultArray;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						resultArray.pushLong(datas[i]);
					} else if (otherArray.isNull(i)) {
						resultArray.pushNull();
					} else {
						resultArray.pushLong(otherArray.getLong(i));
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						if (signs[i]) {
							resultArray.pushNull();
						} else {
							resultArray.pushLong(datas[i]);
						}
					} else if (otherArray.isNull(i)) {
						resultArray.pushNull();
					} else {
						resultArray.pushLong(otherArray.getLong(i));
					}
				}
			}
		} else if (other instanceof DoubleArray) {
			DoubleArray otherArray = (DoubleArray)other;
			DoubleArray resultArray = new DoubleArray(size);
			result = resultArray;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						resultArray.pushDouble(datas[i]);
					} else if (otherArray.isNull(i)) {
						resultArray.pushNull();
					} else {
						resultArray.pushDouble(otherArray.getDouble(i));
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						if (signs[i]) {
							resultArray.pushNull();
						} else {
							resultArray.pushDouble(datas[i]);
						}
					} else if (otherArray.isNull(i)) {
						resultArray.pushNull();
					} else {
						resultArray.pushDouble(otherArray.getDouble(i));
					}
				}
			}
		} else {
			Object []resultDatas = new Object[size + 1];
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						resultDatas[i] = ObjectCache.getInteger(datas[i]);
					} else {
						resultDatas[i] = other.get(i);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						if (!signs[i]) {
							resultDatas[i] = ObjectCache.getInteger(datas[i]);
						}
					} else {
						resultDatas[i] = other.get(i);
					}
				}
			}
			
			result = new ObjectArray(resultDatas, size);
		}
		
		result.setTemporary(true);
		return result;
	}

	/**
	 * 根据条件从当前数组选出标志为true的，标志为false的置成value
	 * @param signArray 标志数组
	 * @param other 值
	 * @return IArray
	 */
	public IArray combine(IArray signArray, Object value) {
		int size = this.size;
		int []datas = this.datas;
		boolean []signs = this.signs;
		IArray result;
		
		if (value instanceof Integer) {
			int v = ((Number)value).intValue();
			if (isTemporary()) {
				result = this;
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						if (signArray.isFalse(i)) {
							datas[i] = v;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signArray.isFalse(i)) {
							datas[i] = v;
							signs[i] = false;
						}
					}
				}
			} else {
				int []resultDatas = new int[size + 1];
				boolean []resultSigns = null;
				
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						if (signArray.isTrue(i)) {
							resultDatas[i] = datas[i];
						} else {
							resultDatas[i] = v;
						}
					}
				} else {
					resultSigns = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						if (signArray.isTrue(i)) {
							if (signs[i]) {
								resultSigns[i] = true;
							} else {
								resultDatas[i] = datas[i];
							}
						} else {
							resultDatas[i] = v;
						}
					}
				}
				
				result = new IntArray(resultDatas, resultSigns, size);
			}
		} else if (value instanceof Long) {
			long v = ((Number)value).longValue();
			long []resultDatas = new long[size + 1];
			boolean []resultSigns = null;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						resultDatas[i] = datas[i];
					} else {
						resultDatas[i] = v;
					}
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						if (signs[i]) {
							resultSigns[i] = true;
						} else {
							resultDatas[i] = datas[i];
						}
					} else {
						resultDatas[i] = v;
					}
				}
			}
			
			result = new LongArray(resultDatas, resultSigns, size);
		} else if (value instanceof Double) {
			double v = ((Number)value).doubleValue();
			double []resultDatas = new double[size + 1];
			boolean []resultSigns = null;
			
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						resultDatas[i] = datas[i];
					} else {
						resultDatas[i] = v;
					}
				}
			} else {
				resultSigns = new boolean[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						if (signs[i]) {
							resultSigns[i] = true;
						} else {
							resultDatas[i] = datas[i];
						}
					} else {
						resultDatas[i] = v;
					}
				}
			}
			
			result = new DoubleArray(resultDatas, resultSigns, size);
		} else if (value == null) {
			if (isTemporary()) {
				result = this;
				if (signs == null) {
					signs = new boolean[size + 1];
					this.signs = signs;
				}
				
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						signs[i] = true;
					}
				}
			} else {
				boolean []resultSigns = new boolean[size + 1];
				int []resultDatas = new int[size + 1];
				
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i) && (signs == null || !signs[i])) {
						resultDatas[i] = datas[i];
					} else {
						resultSigns[i] = true;
					}
				}
				
				result = new IntArray(resultDatas, resultSigns, size);
			}
		} else {
			Object []resultDatas = new Object[size + 1];
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						resultDatas[i] = ObjectCache.getInteger(datas[i]);
					} else {
						resultDatas[i] = value;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						if (!signs[i]) {
							resultDatas[i] = ObjectCache.getInteger(datas[i]);
						}
					} else {
						resultDatas[i] = value;
					}
				}
			}
			
			result = new ObjectArray(resultDatas, size);
		}
		
		result.setTemporary(true);
		return result;
	}
	
	/**
	 * 返回指定数组的成员在当前数组中的位置
	 * @param array 待查找的数组
	 * @param opt 选项，b：同序归并法查找，i：返回单递增数列，c：连续出现
	 * @return 位置或者位置序列
	 */
	public Object pos(IArray array, String opt) {
		if (array instanceof IntArray) {
			IntArray intArray = (IntArray)array;
			int len = this.size;
			int subLen = intArray.size;
			if (len < subLen) {
				return null;
			}
			
			boolean isSorted = false, isIncre = false, isContinuous = false;
			if (opt != null) {
				if (opt.indexOf('b') != -1) isSorted = true;
				if (opt.indexOf('i') != -1) isIncre = true;
				if (opt.indexOf('c') != -1) isContinuous = true;
			}

			// 元素依次出现在源序列中
			if (isIncre) {
				IntArray result = new IntArray(subLen);

				if (isSorted) { // 源序列有序
					int pos = 1;
					for (int t = 1; t <= subLen; ++t) {
						if (intArray.isNull(t)) {
							pos = binarySearch(null, pos, len);
						} else {
							pos = binarySearch(intArray.getInt(t), pos, len);
						}
						
						if (pos > 0) {
							result.pushInt(pos);
							pos++;
						} else {
							return null;
						}
					}
				} else {
					int pos = 1;
					for (int t = 1; t <= subLen; ++t) {
						if (intArray.isNull(t)) {
							pos = firstIndexOf(null, pos);
						} else {
							pos = firstIndexOf(intArray.getInt(t), pos);
						}
						
						if (pos > 0) {
							result.pushInt(pos);
							pos++;
						} else {
							return null;
						}
					}
				}

				return new Sequence(result);
			} else if (isContinuous) {
				int maxCandidate = len - subLen + 1; // 比较的次数
				if (isSorted) {
					int candidate = 1;

					// 找到第一个相等的元素的序号
					Next:
					while (candidate <= maxCandidate) {
						int result = compareTo(candidate, intArray, 1);

						if (result < 0) {
							candidate++;
						} else if (result == 0) {
							for (int i = 2, j = candidate + 1; i <= subLen; ++i, ++j) {
								if (!isEquals(j, intArray, i)) {
									candidate++;
									continue Next;
								}
							}

							return candidate;
						} else {
							return null;
						}
					}
				} else {
					nextCand:
					for (int candidate = 1; candidate <= maxCandidate; ++candidate) {
						for (int i = 1, j = candidate; i <= subLen; ++i, ++j) {
							if (!isEquals(j, intArray, i)) {
								continue nextCand;
							}
						}

						return candidate;
					}
				}

				return null;
			} else {
				IntArray result = new IntArray(subLen);
				int pos;
				
				if (isSorted) { // 源序列有序
					for (int t = 1; t <= subLen; ++t) {
						if (intArray.isNull(t)) {
							pos = binarySearch(null);
						} else {
							pos = binarySearch(intArray.getInt(t));
						}

						if (pos > 0) {
							result.pushInt(pos);
						} else {
							return null;
						}
					}
				} else {
					for (int t = 1; t <= subLen; ++t) {
						if (intArray.isNull(t)) {
							pos = firstIndexOf(null, 1);
						} else {
							pos = firstIndexOf(intArray.getInt(t), 1);
						}
						
						if (pos > 0) {
							result.pushInt(pos);
						} else {
							return null;
						}
					}
				}

				return new Sequence(result);
			}
		} else {
			return ArrayUtil.pos(this, array, opt);
		}
	}
	
	/**
	 * 返回数组成员的二进制表示时1的个数和
	 * @return
	 */
	public int bit1() {
		int []datas = this.datas;
		boolean []signs = this.signs;
		int size = this.size;
		int sum = 0;
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				sum += Integer.bitCount(datas[i]);
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					sum += Integer.bitCount(datas[i]);
				}
			}
		}
		
		return sum;
	}
	
	/**
	 * 返回数组成员按位异或值的二进制表示时1的个数和
	 * @param array 异或数组
	 * @return 1的个数和
	 */
	public int bit1(IArray array) {
		if (array instanceof IntArray) {
			return bit1((IntArray)array);
		} else if (array instanceof LongArray) {
			return ((LongArray)array).bit1(this);
		} else {
			int size = this.size;
			int count = 0;
			for (int i = 1; i <= size; ++i) {
				count += Bit1.bitCount(get(i), array.get(i));
			}
			
			return count;
		}
	}
	
	private int bit1(IntArray array) {
		int size = this.size;
		int []d1 = this.datas;
		boolean []s1 = this.signs;
		int []d2 = array.datas;
		boolean []s2 = array.signs;
		int count = 0;
		
		if (s1 == null) {
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					count += Integer.bitCount(d1[i] ^ d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i]) {
						count += Integer.bitCount(d1[i] ^ d2[i]);
					}
				}
			}
		} else if (s2 == null) {
			for (int i = 1; i <= size; ++i) {
				if (!s1[i]) {
					count += Integer.bitCount(d1[i] ^ d2[i]);
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!s1[i] && !s2[i]) {
					count += Integer.bitCount(d1[i] ^ d2[i]);
				}
			}
		}

		return count;
	}
	
	public boolean hasSigns() {
		return signs != null;
	}
}
