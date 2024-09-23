package com.scudata.array;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Comparator;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Sequence;
import com.scudata.expression.Relation;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.MultithreadUtil;
import com.scudata.util.Variant;

/**
 * 布尔数组，从1开始计数
 * @author LW
 *
 */
public class BoolArray implements IArray {
	private static final long serialVersionUID = 1L;
	private static final byte NULL_SIGN = 60; // 用于序列化时表示signs是否为空

	private boolean []datas; // 第0个元素表示是否是临时数组
	private boolean []signs; // 表示相应位置的元素是否是null，有null成员时才产生
	private int size;
	
	public BoolArray() {
		datas = new boolean[DEFAULT_LEN];
	}
	
	public BoolArray(int initialCapacity) {
		datas = new boolean[++initialCapacity];
	}
	
	public BoolArray(boolean value, int size) {
		boolean []datas = this.datas = new boolean[size + 1];
		this.size = size;
		
		if (value) {
			for (int i = 1; i <= size; ++i) {
				datas[i] = true;
			}
		}
	}
	
	public BoolArray(boolean []datas, int size) {
		this.datas = datas;
		this.size = size;
	}
	
	public BoolArray(boolean []datas, boolean []signs, int size) {
		this.datas = datas;
		this.signs = signs;
		this.size = size;
	}
	
	public boolean[] getDatas() {
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
		return mm.getMessage("DataType.Boolean");
	}
	
	/**
	 * 复制数组
	 * @return
	 */
	public IArray dup() {
		int len = size + 1;
		boolean []newDatas = new boolean[len];
		System.arraycopy(datas, 0, newDatas, 0, len);
		
		boolean []newSigns = null;
		if (signs != null) {
			newSigns = new boolean[len];
			System.arraycopy(signs, 0, newSigns, 0, len);
		}
		
		return new BoolArray(newDatas, newSigns, size);
	}
	
	/**
	 * 写内容到流
	 * @param out 输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		int size = this.size;
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			out.writeByte(1);
		} else {
			out.writeByte(NULL_SIGN + 1);
		}
		
		out.writeInt(size);
		for (int i = 1; i <= size; ++i) {
			out.writeBoolean(datas[i]);
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
		boolean []datas = this.datas = new boolean[len];
		
		for (int i = 1; i < len; ++i) {
			datas[i] = in.readBoolean();
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			out.writeByte(1);
		} else {
			out.writeByte(NULL_SIGN + 1);
		}
		
		out.writeInt(size);
		for (int i = 1; i <= size; ++i) {
			out.writeBoolean(datas[i]);
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
		boolean []datas = this.datas = new boolean[len];
		
		for (int i = 1; i < len; ++i) {
			datas[i] = in.readBoolean();
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
		return new BoolArray(count);
	}
	
	/**
	 * 追加元素，如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void add(Object o) {
		if (o instanceof Boolean) {
			ensureCapacity(size + 1);
			datas[++size] = (Boolean)o;
		} else if (o == null) {
			ensureCapacity(size + 1);
			if (signs == null) {
				signs = new boolean[datas.length];
			}
			
			signs[++size] = true;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Boolean"), Variant.getDataType(o)));
		}
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(IArray array) {
		int size2 = array.size();
		if (size2 == 0) {
		} else if (array instanceof BoolArray) {
			BoolArray boolArray = (BoolArray)array;
			ensureCapacity(size + size2);
			
			System.arraycopy(boolArray.datas, 1, datas, size + 1, size2);
			if (boolArray.signs != null) {
				if (signs == null) {
					signs = new boolean[datas.length];
				}
				
				System.arraycopy(boolArray.signs, 1, signs, size + 1, size2);
			}
			
			size += size2;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof Boolean) {
				ensureCapacity(size + size2);
				boolean v = ((Boolean)obj).booleanValue();
				boolean []datas = this.datas;
				
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
						mm.getMessage("DataType.Boolean"), array.getDataType()));
			}
		} else {
			//MessageManager mm = EngineMessage.get();
			//throw new RQException(mm.getMessage("pdm.arrayTypeError", 
			//		mm.getMessage("DataType.Boolean"), array.getDataType()));
			ensureCapacity(size + size2);
			boolean []datas = this.datas;
			
			for (int i = 1; i <= size2; ++i) {
				Object obj = array.get(i);
				if (obj instanceof Boolean) {
					datas[++size] = ((Boolean)obj).booleanValue();
				} else if (obj == null) {
					if (signs == null) {
						signs = new boolean[datas.length];
					}
					
					signs[++size] = true;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("pdm.arrayTypeError", 
							mm.getMessage("DataType.Boolean"), Variant.getDataType(obj)));
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
		} else if (array instanceof BoolArray) {
			BoolArray boolArray = (BoolArray)array;
			ensureCapacity(size + count);
			
			System.arraycopy(boolArray.datas, 1, datas, size + 1, count);
			if (boolArray.signs != null) {
				if (signs == null) {
					signs = new boolean[datas.length];
				}
				
				System.arraycopy(boolArray.signs, 1, signs, size + 1, count);
			}
			
			size += count;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof Boolean) {
				ensureCapacity(size + count);
				boolean v = ((Boolean)obj).booleanValue();
				boolean []datas = this.datas;
				
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
						mm.getMessage("DataType.Boolean"), array.getDataType()));
			}
		} else {
			//MessageManager mm = EngineMessage.get();
			//throw new RQException(mm.getMessage("pdm.arrayTypeError", 
			//		mm.getMessage("DataType.Boolean"), array.getDataType()));
			ensureCapacity(size + count);
			boolean []datas = this.datas;
			
			for (int i = 1; i <= count; ++i) {
				Object obj = array.get(i);
				if (obj instanceof Boolean) {
					datas[++size] = ((Boolean)obj).booleanValue();
				} else if (obj == null) {
					if (signs == null) {
						signs = new boolean[datas.length];
					}
					
					signs[++size] = true;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("pdm.arrayTypeError", 
							mm.getMessage("DataType.Boolean"), Variant.getDataType(obj)));
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
		if (array instanceof BoolArray) {
			BoolArray boolArray = (BoolArray)array;
			ensureCapacity(size + count);
			
			System.arraycopy(boolArray.datas, index, datas, size + 1, count);
			if (boolArray.signs != null) {
				if (signs == null) {
					signs = new boolean[datas.length];
				}
				
				System.arraycopy(boolArray.signs, index, signs, size + 1, count);
			}
			
			size += count;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof Boolean) {
				ensureCapacity(size + count);
				boolean v = ((Boolean)obj).booleanValue();
				boolean []datas = this.datas;
				
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
						mm.getMessage("DataType.Boolean"), array.getDataType()));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Boolean"), array.getDataType()));
		}
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(Object []array) {
		for (Object obj : array) {
			if (obj != null && !(obj instanceof Boolean)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.Boolean"), Variant.getDataType(obj)));
			}
		}
		
		int size2 = array.length;		
		ensureCapacity(size + size2);
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		
		for (int i = 0; i < size2; ++i) {
			if (array[i] != null) {
				datas[++size] = ((Boolean)array[i]).booleanValue();
			} else {
				if (signs == null) {
					this.signs = signs = new boolean[datas.length];
				}
				
				signs[++size] = true;
			}
		}
	}
	
	/**
	 * 插入元素，如果类型不兼容则抛出异常
	 * @param index 插入位置，从1开始计数
	 * @param o 元素值
	 */
	public void insert(int index, Object o) {
		if (o instanceof Boolean) {
			ensureCapacity(size + 1);
			
			size++;
			System.arraycopy(datas, index, datas, index + 1, size - index);
			
			if (signs != null) {
				System.arraycopy(signs, index, signs, index + 1, size - index);
			}
			
			datas[index] = (Boolean)o;
		} else if (o == null) {
			ensureCapacity(size + 1);
			
			size++;
			System.arraycopy(datas, index, datas, index + 1, size - index);
			
			if (signs == null) {
				signs = new boolean[datas.length];
			} else {
				System.arraycopy(signs, index, signs, index + 1, size - index);
			}
			
			signs[index] = true;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Boolean"), Variant.getDataType(o)));
		}
	}
	
	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, IArray array) {
		if (array instanceof BoolArray) {
			int numNew = array.size();
			BoolArray boolArray = (BoolArray)array;
			ensureCapacity(size + numNew);
			
			System.arraycopy(datas, pos, datas, pos + numNew, size - pos + 1);
			if (signs != null) {
				System.arraycopy(signs, pos, signs, pos + numNew, size - pos + 1);
			}
			
			System.arraycopy(boolArray.datas, 1, datas, pos, numNew);
			if (boolArray.signs == null) {
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
				
				System.arraycopy(boolArray.signs, 1, signs, pos, numNew);
			}
			
			size += numNew;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Boolean"), array.getDataType()));
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
			} else if (!(obj instanceof Boolean)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.Boolean"), Variant.getDataType(obj)));
			}
		}

		int numNew = array.length;
		ensureCapacity(size + numNew);
		
		boolean []datas = this.datas;
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
					datas[pos + i] = (Boolean)array[i];
					signs[pos + i] = false;
				}
			}
		} else {
			for (int i = 0; i < numNew; ++i) {
				datas[pos + i] = (Boolean)array[i];
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
		if (o instanceof Boolean) {
			datas[++size] = (Boolean)o;
		} else if (o == null) {
			if (signs == null) {
				signs = new boolean[datas.length];
			}
			
			signs[++size] = true;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Boolean"), Variant.getDataType(o)));
		}
	}
	
	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void push(IArray array, int index) {
		if (array instanceof BoolArray) {
			if (array.isNull(index)) {
				if (signs == null) {
					signs = new boolean[datas.length];
				}
				
				signs[++size] = true;
			} else {
				datas[++size] = ((BoolArray)array).getBool(index);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Boolean"), array.getDataType()));
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
		} else if (array instanceof BoolArray) {
			ensureCapacity(size + 1);
			datas[++size] = ((BoolArray)array).getBool(index);
		} else {
			add(array.get(index));
			//MessageManager mm = EngineMessage.get();
			//throw new RQException(mm.getMessage("pdm.arrayTypeError", 
			//		mm.getMessage("DataType.Boolean"), array.getDataType()));
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
		} else if (array instanceof BoolArray) {
			datas[curIndex] = ((BoolArray)array).getBool(index);
		} else {
			set(curIndex, array.get(index));
		}
	}

	/**
	 * 追加元素
	 * @param b 值
	 */
	public void addBool(boolean b) {
		ensureCapacity(size + 1);
		datas[++size] = b;
	}
	
	/**
	 * 追加元素（不检查容量，认为有足够空间存放元素）
	 * @param n 值
	 */
	public void push(boolean n) {
		datas[++size] = n;
	}
	
	/**
	 * 追加元素（不检查容量，认为有足够空间存放元素）
	 * @param n 值
	 */
	public void pushBool(boolean n) {
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
	 * 取指定位置元素
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public Object get(int index) {
		if (signs == null || !signs[index]) {
			return Boolean.valueOf(datas[index]);
		} else {
			return null;
		}
	}

	/**
	 * 取指定位置元素
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean getBool(int index) {
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		int len = indexArray.length;
		BoolArray result = new BoolArray(len);
		
		if (signs == null) {
			for (int i : indexArray) {
				result.pushBool(datas[i]);
			}
		} else {
			for (int i : indexArray) {
				if (signs[i]) {
					result.pushNull();
				} else {
					result.pushBool(datas[i]);
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		int len = end - start + 1;
		BoolArray result = new BoolArray(len);
		
		if (doCheck) {
			if (signs == null) {
				for (; start <= end; ++start) {
					int q = indexArray[start];
					if (q > 0) {
						result.pushBool(datas[q]);
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
						result.pushBool(datas[q]);
					}
				}
			}
		} else {
			if (signs == null) {
				for (; start <= end; ++start) {
					result.pushBool(datas[indexArray[start]]);
				}
			} else {
				for (; start <= end; ++start) {
					int q = indexArray[start];
					if (signs[q]) {
						result.pushNull();
					} else {
						result.pushBool(datas[q]);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 取指定位置元素组成新数组
	 * @param IArray 位置数组
	 * @return IArray
	 */
	public IArray get(IArray indexArray) {
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		int len = indexArray.size();
		BoolArray result = new BoolArray(len);
		
		if (signs == null) {
			for (int i = 1; i <= len; ++i) {
				result.pushBool(datas[indexArray.getInt(i)]);
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				int index = indexArray.getInt(i);
				if (signs[index]) {
					result.pushNull();
				} else {
					result.pushBool(datas[index]);
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
		boolean []newDatas = new boolean[newSize + 1];
		System.arraycopy(datas, start, newDatas, 1, newSize);
		
		if (signs == null) {
			return new BoolArray(newDatas, null, newSize);
		} else {
			boolean []newSigns = new boolean[newSize + 1];
			System.arraycopy(signs, start, newSigns, 1, newSize);
			return new BoolArray(newDatas, newSigns, newSize);
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

			boolean []newDatas = new boolean[newCapacity];
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
			boolean []newDatas = new boolean[newLen];
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
		if (signs == null) {
			if (isTemporary()) {
				return this;
			} else {
				boolean []resultDatas = new boolean[size + 1];
				System.arraycopy(datas, 1, resultDatas, 1, size);
				BoolArray result = new BoolArray(resultDatas, size);
				result.setTemporary(true);
				return result;
			}
		} else {
			int size = this.size;
			boolean []resultDatas = new boolean[size + 1];
			System.arraycopy(datas, 1, resultDatas, 1, size);
			boolean []signs = this.signs;
			
			for (int i = 1; i <= size; ++i) {
				if (signs[i]) {
					resultDatas[i] = false;
				}
			}
			
			BoolArray result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	/**
	 * 判断元素是否是假
	 * @return BoolArray
	 */
	public BoolArray isFalse() {
		int size = this.size;
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		boolean []resultDatas = new boolean[size + 1];
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = !datas[i];
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = signs[i] || !datas[i];
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
		return datas[index] && (signs == null || !signs[index]);
	}
	
	/**
	 * 判断指定位置的元素是否是False
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isFalse(int index) {
		return !datas[index] || (signs != null && signs[index]);
	}

	/**
	 * 是否是计算过程中临时产生的数组，临时产生的可以被修改，比如 f1+f2+f3，只需产生一个数组存放结果
	 * @return true：是临时产生的数组，false：不是临时产生的数组
	 */
	public boolean isTemporary() {
		return datas[0];
	}

	/**
	 * 设置是否是计算过程中临时产生的数组
	 * @param ifTemporary true：是临时产生的数组，false：不是临时产生的数组
	 */
	public void setTemporary(boolean ifTemporary) {
		datas[0] = ifTemporary;
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
		
		size -= (toIndex - fromIndex + 1);
	}
	
	/**
	 * 删除指定位置的元素，序号从小到大排序
	 * @param seqs 索引数组
	 */
	public void remove(int []seqs) {
		int delCount = 0;
		boolean []datas = this.datas;
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
		if (signs != null) {
			System.arraycopy(signs, start, signs, 1, newSize);
			for (int i = size; i > newSize; --i) {
				signs[i] = false;
			}
		}
		
		size = newSize;
	}
	
	public int size() {
		return size;
	}
	
	/**
	 * 返回数组取值为真的元素数目
	 * @return 取值为真的元素数目
	 */
	public int count() {
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		int size = this.size;
		int count = size;
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				if (!datas[i]) {
					count--;
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (signs[i] || !datas[i]) {
					count--;
				}
			}
		}
		
		return count;
	}
	
	/**
	 * 判断数组是否有取值为true的元素
	 * @return true：有，false：没有
	 */
	public boolean containTrue() {
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		int size = this.size;
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				if (datas[i]) {
					return true;
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i] && datas[i]) {
					return true;
				}
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
			return Boolean.valueOf(datas[1]);
		}
		
		for (int i = 1; i <= size; ++i) {
			if (!signs[i]) {
				return Boolean.valueOf(datas[i]);
			}
		}
		
		return null;
	}

	public void set(int index, boolean b) {
		datas[index] = b;
		if (signs != null) {
			signs[index] = false;
		}
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
			
			signs[index] = true;
		} else if (obj instanceof Boolean) {
			datas[index] = (Boolean)obj;
			if (signs != null) {
				signs[index] = false;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.Boolean"), Variant.getDataType(obj)));
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
		if (elem instanceof Boolean) {
			int size = this.size;
			boolean []datas = this.datas;
			boolean []signs = this.signs;
			
			if (((Boolean)elem).booleanValue()) {
				if (size == 0) {
					return -1;
				} else if ((signs == null || !signs[size]) && datas[size]) {
					return size;
				} else {
					return -size - 1;
				}
			} else {
				if (size == 0) {
					return -1;
				} else if (signs == null) {
					return datas[1] ? -1 : 1;
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!signs[i]) {
							return datas[i] ? -i : i;
						}
					}
					
					// 数组元素全部为空
					return -size - 1;
				}
			}
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
		if (elem instanceof Boolean) {
			boolean []datas = this.datas;
			boolean []signs = this.signs;
			
			if (((Boolean)elem).booleanValue()) {
				if (size == 0) {
					return -1;
				} else if ((signs == null || !signs[end]) && datas[end]) {
					return end;
				} else {
					return -end - 1;
				}
			} else {
				if (end == 0) {
					return -1;
				} else if (signs == null) {
					return datas[start] ? -1 : start;
				} else {
					for (int i = start; i <= end; ++i) {
						if (!signs[i]) {
							return datas[i] ? -i : i;
						}
					}
					
					// 数组元素全部为空
					return -end - 1;
				}
			}
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
	
	/**
	 * 返回列表中是否包含指定元素
	 * @param elem Object 待查找的元素
	 * @return boolean true：包含，false：不包含
	 */
	public boolean contains(Object elem) {
		if (elem instanceof Boolean) {
			boolean v = ((Boolean)elem).booleanValue();
			boolean []datas = this.datas;
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
		return false;
	}
	
	/**
	 * 返回元素在数组中首次出现的位置
	 * @param elem 待查找的元素
	 * @param start 起始查找位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	public int firstIndexOf(Object elem, int start) {
		if (elem instanceof Boolean) {
			boolean v = ((Boolean)elem).booleanValue();
			boolean []datas = this.datas;
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
	
	/**
	 * 返回元素在数组中最后出现的位置
	 * @param elem 待查找的元素
	 * @param start 从后面开始查找的位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	public int lastIndexOf(Object elem, int start) {
		if (elem instanceof Boolean) {
			boolean v = ((Boolean)elem).booleanValue();
			boolean []datas = this.datas;
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
		} else if (!(elem instanceof Boolean)) {
			return new IntArray(1);
		}

		boolean b = ((Boolean)elem).booleanValue();
		boolean []datas = this.datas;
		IntArray result = new IntArray(7);
		
		if (isFromHead) {
			if (signs == null) {
				for (int i = start; i <= size; ++i) {
					if (datas[i] == b) {
						result.addInt(i);
					}
				}
			} else {
				for (int i = start; i <= size; ++i) {
					if (!signs[i] && datas[i] == b) {
						result.addInt(i);
					}
				}
			}
		} else {
			if (signs == null) {
				for (int i = start; i > 0; --i) {
					if (datas[i] == b) {
						result.addInt(i);
					}
				}
			} else {
				for (int i = start; i > 0; --i) {
					if (!signs[i] && datas[i] == b) {
						result.addInt(i);
					}
				}
			}
		}
		
		return result;
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		int size = this.size;
		
		if (isTemporary()) {
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					datas[i] = !datas[i];
				}
			} else {
				this.signs = null;
				for (int i = 1; i <= size; ++i) {
					datas[i] = signs[i] || !datas[i];
				}
			}
			
			return this;
		} else {
			boolean []newDatas = new boolean[size + 1];
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					newDatas[i] = !datas[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					newDatas[i] = signs[i] || !datas[i];
				}
			}
			
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
		if (array instanceof StringArray) {
			return memberDivide((StringArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illDivide"));
		}
	}
	
	private StringArray memberDivide(StringArray array) {
		int size = this.size;
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		String []d2 = array.getDatas();
		
		if (array.isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (s1 == null || !s1[i]) {
					if (d2[i] != null) {
						d2[i] = d1[i] + d2[i];
					} else {
						d2[i] = Boolean.toString(d1[i]);
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
					resultDatas[i] = Boolean.toString(d1[i]);
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
		if (array instanceof BoolArray) {
			return calcRelation((BoolArray)array, relation);
		} else if (array instanceof ConstArray) {
			return calcRelation(array.get(1), relation);
		} else if (array instanceof IntArray) {
			return calcRelation((IntArray)array, relation);
		} else if (array instanceof LongArray) {
			return calcRelation((LongArray)array, relation);
		} else if (array instanceof DoubleArray) {
			return calcRelation((DoubleArray)array, relation);
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
		if (value instanceof Boolean) {
			return calcRelation(((Boolean)value).booleanValue(), relation);
		} else if (value == null) {
			return calcRelationNull(relation);
		} else {
			int size = this.size;
			boolean []d1 = this.datas;
			boolean []s1 = this.signs;
			boolean b = Variant.isTrue(value);
			
			if (relation == Relation.AND) {
				BoolArray result;
				boolean []resultDatas;
				if (isTemporary()) {
					resultDatas = d1;
					result = this;
					signs = null;
				} else {
					resultDatas = new boolean[size + 1];
					result = new BoolArray(resultDatas, size);
					result.setTemporary(true);
				}
				
				if (!b) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = false;
					}
				} else if (s1 == null) {
					if (resultDatas != d1) {
						System.arraycopy(d1, 1, resultDatas, 1, size);
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = !s1[i] && d1[i];
					}
				}
				
				return result;
			} else if (relation == Relation.OR) {
				BoolArray result;
				boolean []resultDatas;
				if (isTemporary()) {
					resultDatas = d1;
					result = this;
					signs = null;
				} else {
					resultDatas = new boolean[size + 1];
					result = new BoolArray(resultDatas, size);
					result.setTemporary(true);
				}
				
				if (b) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = true;
					}
				} else if (s1 == null) {
					if (resultDatas != d1) {
						System.arraycopy(d1, 1, resultDatas, 1, size);
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = !s1[i] && d1[i];
					}
				}
				
				return result;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
						getDataType(), Variant.getDataType(value)));
			}
		}
	}
	
	private BoolArray calcRelation(BoolArray array, int relation) {
		int size = this.size;
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		boolean []d2 = array.datas;
		boolean []s2 = array.signs;
		
		BoolArray result;
		boolean []resultDatas;
		if (isTemporary()) {
			resultDatas = d1;
			result = this;
			result.signs = null;
		} else if (array.isTemporary()) {
			resultDatas = d2;
			result = array;
			result.signs = null;
		} else {
			resultDatas = new boolean[size + 1];
			result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
		}
		
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
						resultDatas[i] = Variant.compare(d1[i], d2[i]) > 0;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = Variant.compare(d1[i], d2[i]) > 0;
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) > 0;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) > 0;
					}
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) >= 0;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = true;
						} else {
							resultDatas[i] = Variant.compare(d1[i], d2[i]) >= 0;
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) >= 0;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = s2[i];
					} else if (s2[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) >= 0;
					}
				}
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) < 0;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = Variant.compare(d1[i], d2[i]) < 0;
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) < 0;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = !s2[i];
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) < 0;
					}
				}
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) <= 0;
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s2[i]) {
							resultDatas[i] = false;
						} else {
							resultDatas[i] = Variant.compare(d1[i], d2[i]) <= 0;
						}
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) <= 0;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else if (s2[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Variant.compare(d1[i], d2[i]) <= 0;
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
						resultDatas[i] = d1[i] && d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] && !s2[i] && d2[i];
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && d1[i] && d2[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && d1[i] && !s2[i] && d2[i];
				}
			}
		} else { // Relation.OR
			if (s1 == null) {
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] || d2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] || (!s2[i] && d2[i]);
					}
				}
			} else if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = (!s1[i] && d1[i]) || d2[i];
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = (!s1[i] && d1[i]) || (!s2[i] && d2[i]);
				}
			}
		}
		
		return result;
	}
	
	private BoolArray calcRelation(boolean []s2, int relation) {
		int size = this.size;
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		
		if (isTemporary()) {
			if (relation == Relation.AND) {
				if (s1 == null) {
					if (s2 != null) {
						for (int i = 1; i <= size; ++i) {
							if (s2[i]) {
								d1[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (s1[i]) {
							d1[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (s1[i] || s2[i]) {
							d1[i] = false;
						}
					}
				}
			} else { // Relation.OR
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						d1[i] = true;
					}
				} else if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!s2[i]) {
							d1[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!s2[i]) {
							d1[i] = true;
						} else if (s1[i]) {
							d1[i] = false;
						}
					}
				}
			}
			
			signs = null;
			return this;
		} else {
			boolean []resultDatas = new boolean[size + 1];
			if (relation == Relation.AND) {
				if (s1 == null) {
					if (s2 == null) {
						System.arraycopy(d1, 1, resultDatas, 1, size);
					} else {
						for (int i = 1; i <= size; ++i) {
							resultDatas[i] = d1[i] && !s2[i];
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = !s1[i] && d1[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = !s2[i] && !s1[i] && d1[i];
					}
				}
			} else { // Relation.OR
				if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = true;
					}
				} else if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = d1[i] || !s2[i];
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = !s2[i] || (!s1[i] && d1[i]);
					}
				}
			}
			
			BoolArray result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	protected BoolArray calcRelation(IntArray array, int relation) {
		if (relation != Relation.AND && relation != Relation.OR) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		}
		
		return calcRelation(array.getSigns(), relation);
	}
	
	protected BoolArray calcRelation(LongArray array, int relation) {
		if (relation != Relation.AND && relation != Relation.OR) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		}

		return calcRelation(array.getSigns(), relation);
	}
	
	protected BoolArray calcRelation(DoubleArray array, int relation) {
		if (relation != Relation.AND && relation != Relation.OR) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		}

		return calcRelation(array.getSigns(), relation);
	}
	
	BoolArray calcRelation(DateArray array, int relation) {
		int size = this.size;
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		Object []d2 = array.getDatas();
		
		if (relation == Relation.AND) {
			BoolArray result;
			boolean []resultDatas;
			if (isTemporary()) {
				resultDatas = d1;
				result = this;
				signs = null;
			} else {
				resultDatas = new boolean[size + 1];
				result = new BoolArray(resultDatas, size);
				result.setTemporary(true);
			}
			
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] && Variant.isTrue(d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && d1[i] && Variant.isTrue(d2[i]);
				}
			}
			
			return result;
		} else if (relation == Relation.OR) {
			BoolArray result;
			boolean []resultDatas;
			if (isTemporary()) {
				resultDatas = d1;
				result = this;
				signs = null;
			} else {
				resultDatas = new boolean[size + 1];
				result = new BoolArray(resultDatas, size);
				result.setTemporary(true);
			}
			
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] || Variant.isTrue(d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = (!s1[i] && d1[i]) || Variant.isTrue(d2[i]);
				}
			}
			
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		}
	}
	
	BoolArray calcRelation(StringArray array, int relation) {
		int size = this.size;
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		Object []d2 = array.getDatas();
		
		if (relation == Relation.AND) {
			BoolArray result;
			boolean []resultDatas;
			if (isTemporary()) {
				resultDatas = d1;
				result = this;
				signs = null;
			} else {
				resultDatas = new boolean[size + 1];
				result = new BoolArray(resultDatas, size);
				result.setTemporary(true);
			}
			
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] && Variant.isTrue(d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && d1[i] && Variant.isTrue(d2[i]);
				}
			}
			
			return result;
		} else if (relation == Relation.OR) {
			BoolArray result;
			boolean []resultDatas;
			if (isTemporary()) {
				resultDatas = d1;
				result = this;
				signs = null;
			} else {
				resultDatas = new boolean[size + 1];
				result = new BoolArray(resultDatas, size);
				result.setTemporary(true);
			}
			
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] || Variant.isTrue(d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = (!s1[i] && d1[i]) || Variant.isTrue(d2[i]);
				}
			}
			
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		}
	}
	
	/**
	 * 比较指定数和指定对象的大小，null最小
	 * @param b1 左值
	 * @param o2 右值
	 * @return 1：左值大，0：同样大，-1：右值大
	 */
	private static int compare(boolean b1, Object o2) {
		if (o2 instanceof Boolean) {
			return Variant.compare(b1, ((Boolean)o2).booleanValue());
		} else if (o2 == null) {
			return 1;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", b1, o2,
					mm.getMessage("DataType.Boolean"), Variant.getDataType(o2)));
		}
	}
	
	BoolArray calcRelation(ObjectArray array, int relation) {
		int size = this.size;
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		Object []d2 = array.getDatas();
		
		BoolArray result;
		boolean []resultDatas;
		if (isTemporary()) {
			resultDatas = d1;
			result = this;
			signs = null;
		} else {
			resultDatas = new boolean[size + 1];
			result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
		}
		
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
					resultDatas[i] = d1[i] && Variant.isTrue(d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && d1[i] && Variant.isTrue(d2[i]);
				}
			}
		} else { // Relation.OR
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = d1[i] || Variant.isTrue(d2[i]);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = (!s1[i] && d1[i]) || Variant.isTrue(d2[i]);
				}
			}
		}
		
		return result;
	}
	
	private BoolArray calcRelation(boolean value, int relation) {
		int size = this.size;
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		
		BoolArray result;
		boolean []resultDatas;
		if (isTemporary()) {
			resultDatas = d1;
			result = this;
			result.signs = null;
		} else {
			resultDatas = new boolean[size + 1];
			result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
		}
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Variant.compare(d1[i], value) == 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Variant.compare(d1[i], value) == 0;
					}
				}
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Variant.compare(d1[i], value) > 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Variant.compare(d1[i], value) > 0;
					}
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Variant.compare(d1[i], value) >= 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = false;
					} else {
						resultDatas[i] = Variant.compare(d1[i], value) >= 0;
					}
				}
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Variant.compare(d1[i], value) < 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Variant.compare(d1[i], value) < 0;
					}
				}
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Variant.compare(d1[i], value) <= 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Variant.compare(d1[i], value) <= 0;
					}
				}
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			if (s1 == null) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = Variant.compare(d1[i], value) != 0;
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (s1[i]) {
						resultDatas[i] = true;
					} else {
						resultDatas[i] = Variant.compare(d1[i], value) != 0;
					}
				}
			}
		} else if (relation == Relation.AND) {
			if (!value) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = false;
				}
			} else if (s1 == null) {
				if (resultDatas != d1) {
					System.arraycopy(d1, 1, resultDatas, 1, size);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && d1[i];
				}
			}
		} else { // Relation.OR
			if (value) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = true;
				}
			} else if (s1 == null) {
				if (resultDatas != d1) {
					System.arraycopy(d1, 1, resultDatas, 1, size);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !s1[i] && d1[i];
				}
			}
		}
		
		return result;
	}
	
	private BoolArray calcRelationNull(int relation) {
		int size = this.size;
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		
		BoolArray result;
		boolean []resultDatas;
		if (isTemporary()) {
			resultDatas = datas;
			result = this;
			result.signs = null;
		} else {
			resultDatas = new boolean[size + 1];
			result = new BoolArray(resultDatas, size);
			result.setTemporary(true);
		}
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			if (signs != null) {
				System.arraycopy(signs, 1, resultDatas, 1, size);
			} else if (resultDatas == datas) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = false;
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
					resultDatas[i] = !signs[i];
				}
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = true;
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			if (resultDatas == datas) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = false;
				}
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			if (signs != null) {
				System.arraycopy(signs, 1, resultDatas, 1, size);
			} else if (resultDatas == datas) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = false;
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
					resultDatas[i] = !signs[i];
				}
			}
		} else if (relation == Relation.AND) {
			if (resultDatas == datas) {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = false;
				}
			}
		} else { // Relation.OR
			if (signs == null) {
				if (resultDatas != datas) {
					System.arraycopy(datas, 1, resultDatas, 1, size);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					resultDatas[i] = !signs[i] && datas[i];
				}
			}
		}
		
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
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		
		int size = size1;
		int result = 0;
		if (size1 < size2) {
			result = -1;
		} else if (size1 > size2) {
			result = 1;
			size = size2;
		}

		if (array instanceof BoolArray) {
			BoolArray array2 = (BoolArray)array;
			boolean []d2 = array2.datas;
			boolean []s2 = array2.signs;
			
			for (int i = 1; i <= size; ++i) {
				if (s1 == null || !s1[i]) {
					if (s2 == null || !s2[i]) {
						int cmp = Variant.compare(d1[i], d2[i]);
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
			if (value instanceof Boolean) {
				boolean d2 = ((Boolean)value).booleanValue();
				for (int i = 1; i <= size; ++i) {
					if (s1 == null || !s1[i]) {
						int cmp = Variant.compare(d1[i], d2);
						if (cmp != 0) {
							return cmp;
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
			return Variant.compare(datas[index1], datas[index2]);
		} else if (signs[index1]) {
			return signs[index2] ? 0 : -1;
		} else if (signs[index2]) {
			return 1;
		} else {
			return Variant.compare(datas[index1], datas[index2]);
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
		} else if (array instanceof BoolArray) {
			return datas[curIndex] == ((BoolArray)array).getBool(index);
		} else {
			Object obj = array.get(index);
			return obj instanceof Boolean && ((Boolean)obj).booleanValue() == datas[curIndex];
		}
	}
	
	/**
	 * 判断数组的指定元素是否与给定值相等
	 * @param curIndex 数组元素索引，从1开始计数
	 * @param value 值
	 * @return true：相等，false：不相等
	 */
	public boolean isEquals(int curIndex, Object value) {
		if (value instanceof Boolean) {
			if (signs == null || !signs[curIndex]) {
				return datas[curIndex] == ((Boolean)value).booleanValue();
			} else {
				return false;
			}
		} else if (value == null) {
			return signs != null && signs[curIndex];
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
		if (isNull(curIndex)) {
			return array.isNull(index) ? 0 : -1;
		} else if (array.isNull(index)) {
			return 1;
		} else if (array instanceof BoolArray) {
			return Variant.compare(datas[curIndex], ((BoolArray)array).getBool(index));
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
			return Boolean.hashCode(datas[index]);
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
		
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				if (datas[i]) {
					return Boolean.TRUE;
				}
			}
			
			return Boolean.FALSE;
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i] && datas[i]) {
					return Boolean.TRUE;
				}
			}
			
			return Boolean.FALSE;
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
		
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				if (!datas[i]) {
					return Boolean.FALSE;
				}
			}
			
			return Boolean.TRUE;
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i] && !datas[i]) {
					return Boolean.FALSE;
				}
			}
			
			return Boolean.TRUE;
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
		if (array == this) {
		} else if (array instanceof BoolArray) {
			calcRelations((BoolArray)array, relation, result, isAnd);
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
		if (value instanceof Boolean) {
			calcRelations(((Boolean)value).booleanValue(), relation, result, isAnd);
		} else if (value == null) {
			ArrayUtil.calcRelationsNull(signs, size, relation, result, isAnd);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
					getDataType(), Variant.getDataType(value)));
		}
	}
	
	private void calcRelations(BoolArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		boolean []d2 = array.datas;
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
							if (resultDatas[i] && Variant.compare(d1[i], d2[i]) <= 0) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && Variant.compare(d1[i], d2[i]) <= 0) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || Variant.compare(d1[i], d2[i]) <= 0)) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || (!s2[i] && Variant.compare(d1[i], d2[i]) <= 0))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && Variant.compare(d1[i], d2[i]) < 0) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && !s2[i] && Variant.compare(d1[i], d2[i]) < 0) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || Variant.compare(d1[i], d2[i]) < 0)) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s2[i] && (s1[i] || Variant.compare(d1[i], d2[i]) < 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && Variant.compare(d1[i], d2[i]) >= 0) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || Variant.compare(d1[i], d2[i]) >= 0)) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && Variant.compare(d1[i], d2[i]) >= 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s2[i] || (!s1[i] && Variant.compare(d1[i], d2[i]) >= 0))) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && Variant.compare(d1[i], d2[i]) > 0) {
								resultDatas[i] = false;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (resultDatas[i] && (s2[i] || Variant.compare(d1[i], d2[i]) > 0)) {
								resultDatas[i] = false;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && Variant.compare(d1[i], d2[i]) > 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && (s2[i] || Variant.compare(d1[i], d2[i]) > 0)) {
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
							if (!resultDatas[i] && Variant.compare(d1[i], d2[i]) > 0) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || Variant.compare(d1[i], d2[i]) > 0)) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && Variant.compare(d1[i], d2[i]) > 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && (s2[i] || Variant.compare(d1[i], d2[i]) > 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && Variant.compare(d1[i], d2[i]) >= 0) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && (s2[i] || Variant.compare(d1[i], d2[i]) >= 0)) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && Variant.compare(d1[i], d2[i]) >= 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s2[i] || (!s1[i] && Variant.compare(d1[i], d2[i]) >= 0))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && Variant.compare(d1[i], d2[i]) < 0) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && Variant.compare(d1[i], d2[i]) < 0) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || Variant.compare(d1[i], d2[i]) < 0)) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s2[i] && (s1[i] || (Variant.compare(d1[i], d2[i]) < 0))) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					if (s2 == null) {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && Variant.compare(d1[i], d2[i]) <= 0) {
								resultDatas[i] = true;
							}
						}
					} else {
						for (int i = 1; i <= size; ++i) {
							if (!resultDatas[i] && !s2[i] && Variant.compare(d1[i], d2[i]) <= 0) {
								resultDatas[i] = true;
							}
						}
					}
				} else if (s2 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || Variant.compare(d1[i], d2[i]) <= 0)) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || (!s2[i] && Variant.compare(d1[i], d2[i]) <= 0))) {
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

	private void calcRelations(boolean value, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		boolean []d1 = this.datas;
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
						if (resultDatas[i] && Variant.compare(d1[i], value) <= 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || Variant.compare(d1[i], value) <= 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && Variant.compare(d1[i], value) < 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && (s1[i] || Variant.compare(d1[i], value) < 0)) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && Variant.compare(d1[i], value) >= 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && Variant.compare(d1[i], value) >= 0) {
							resultDatas[i] = false;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && Variant.compare(d1[i], value) > 0) {
							resultDatas[i] = false;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (resultDatas[i] && !s1[i] && Variant.compare(d1[i], value) > 0) {
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
						if (!resultDatas[i] && Variant.compare(d1[i], value) > 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && Variant.compare(d1[i], value) > 0) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && Variant.compare(d1[i], value) >= 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && !s1[i] && Variant.compare(d1[i], value) >= 0) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && Variant.compare(d1[i], value) < 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || Variant.compare(d1[i], value) < 0)) {
							resultDatas[i] = true;
						}
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				if (s1 == null) {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && Variant.compare(d1[i], value) <= 0) {
							resultDatas[i] = true;
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (!resultDatas[i] && (s1[i] || Variant.compare(d1[i], value) <= 0)) {
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

	void calcRelations(ObjectArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		boolean []d1 = this.datas;
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
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		boolean []resultDatas = new boolean[size + 1];
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
			
			return new BoolArray(resultDatas, null, resultSize);
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
			
			return new BoolArray(resultDatas, resultSigns, resultSize);
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
		boolean []d1 = this.datas;
		boolean []s1 = this.signs;
		boolean []resultDatas = new boolean[end - start + 1];
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
			
			return new BoolArray(resultDatas, null, resultSize);
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
			
			return new BoolArray(resultDatas, resultSigns, resultSize);
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		
		Object []result = new Object[size];
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				result[i - 1] = Boolean.valueOf(datas[i]);
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					result[i - 1] = Boolean.valueOf(datas[i]);
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				result[i - 1] = Boolean.valueOf(datas[i]);
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					result[i - 1] = Boolean.valueOf(datas[i]);
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
		boolean []resultDatas = new boolean[resultSize + 1];
		System.arraycopy(datas, pos, resultDatas, 1, resultSize);
		
		if (signs == null) {
			this.size = pos - 1;
			return new BoolArray(resultDatas, null, resultSize);
		} else {
			boolean []resultSigns = new boolean[resultSize + 1];
			System.arraycopy(signs, pos, resultSigns, 1, resultSize);
			for (int i = pos; i <= size; ++i) {
				signs[i] = false;
			}
			
			this.size = pos - 1;
			return new BoolArray(resultDatas, resultSigns, resultSize);
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		
		int resultSize = to - from + 1;
		boolean []resultDatas = new boolean[resultSize + 1];
		System.arraycopy(datas, from, resultDatas, 1, resultSize);
		
		System.arraycopy(datas, to + 1, datas, from, oldSize - to);
		this.size -= resultSize;
		
		if (signs == null) {
			return new BoolArray(resultDatas, null, resultSize);
		} else {
			boolean []resultSigns = new boolean[resultSize + 1];
			System.arraycopy(signs, from, resultSigns, 1, resultSize);
			System.arraycopy(signs, to + 1, signs, from, oldSize - to);
			return new BoolArray(resultDatas, resultSigns, resultSize);
		}
	}
	
	/**
	 * 对数组的元素进行排序
	 */
	public void sort() {
		int size = this.size;
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		
		if (signs == null) {
			int trueCount = 0;
			for (int i = 1; i <= size; ++i) {
				if (datas[i]) {
					trueCount++;
				}
			}
			
			int falseCount = size - trueCount;
			for (int i = 1; i <= falseCount; ++i) {
				datas[i] = false;
			}
			
			for (int i = falseCount + 1; i <= size; ++i) {
				datas[i] = true;
			}
		} else {
			int nullCount = 0;
			int trueCount = 0;
			for (int i = 1; i <= size; ++i) {
				if (signs[i]) {
					signs[i] = false;
					nullCount++;
				} else if (datas[i]) {
					trueCount++;
				}
			}
			
			for (int i = 1; i <= nullCount; ++i) {
				signs[i] = true;
			}
			
			int falseEnd = size - trueCount;
			for (int i = nullCount + 1; i <= falseEnd; ++i) {
				datas[i] = false;
			}
			
			for (int i = falseEnd + 1; i <= size; ++i) {
				datas[i] = true;
			}
		}
	}

	/**
	 * 对数组的元素进行排序
	 * @param comparator 比较器
	 */
	public void sort(Comparator<Object> comparator) {
		int size = this.size;
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		Boolean []values = new Boolean[size + 1];
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				values[i] = Boolean.valueOf(datas[i]);
			}
			
			MultithreadUtil.sort(values, 1, size + 1, comparator);
			
			for (int i = 1; i <= size; ++i) {
				datas[i] = values[i].booleanValue();
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					values[i] = Boolean.valueOf(datas[i]);
				}
			}			
			
			MultithreadUtil.sort(values, 1, size + 1, comparator);
			
			for (int i = 1; i <= size; ++i) {
				if (values[i] != null) {
					datas[i] = values[i].booleanValue();
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		boolean []resultDatas = new boolean[size + 1];
		
		if (signs == null) {
			for (int i = 1, q = size; i <= size; ++i) {
				resultDatas[i] = datas[q--];
			}
			
			return new BoolArray(resultDatas, null, size);
		} else {
			boolean []resultSigns = new boolean[size + 1];
			for (int i = 1, q = size; i <= size; ++i, --q) {
				if (signs[q]) {
					resultSigns[i] = true;
				} else {
					resultDatas[i] = datas[q];
				}
			}
			
			return new BoolArray(resultDatas, resultSigns, size);
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
		throw new RuntimeException();
	}
	
	/**
	 * 对数组元素从小到大做排名，取前count名的位置
	 * @param count 如果count小于0则从大到小做排名
	 * @param ignoreNull 是否忽略空元素
	 * @param iopt 是否按去重方式做排名
	 * @return IntArray
	 */
	public IntArray ptopRank(int count, boolean ignoreNull, boolean iopt) {
		throw new RuntimeException();
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		Object []resultDatas = new Object[size + 1];
		
		if (signs == null) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = Boolean.valueOf(datas[i]);
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (!signs[i]) {
					resultDatas[i] = Boolean.valueOf(datas[i]);
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		IArray result;
		
		if (other instanceof BoolArray) {
			BoolArray otherArray = (BoolArray)other;
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
							datas[i] = otherArray.getBool(i);
							if (signs != null) {
								signs[i] = false;
							}
						}
					}
				}
			} else {
				BoolArray resultArray = new BoolArray(size);
				result = resultArray;
				
				if (signs == null) {
					for (int i = 1; i <= size; ++i) {
						if (signArray.isTrue(i)) {
							resultArray.pushBool(datas[i]);
						} else if (otherArray.isNull(i)) {
							resultArray.pushNull();
						} else {
							resultArray.pushBool(otherArray.getBool(i));
						}
					}
				} else {
					for (int i = 1; i <= size; ++i) {
						if (signArray.isTrue(i)) {
							if (signs[i]) {
								resultArray.pushNull();
							} else {
								resultArray.pushBool(datas[i]);
							}
						} else if (otherArray.isNull(i)) {
							resultArray.pushNull();
						} else {
							resultArray.pushBool(otherArray.getBool(i));
						}
					}
				}
			}
		} else {
			Object []resultDatas = new Object[size + 1];
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						resultDatas[i] = Boolean.valueOf(datas[i]);
					} else {
						resultDatas[i] = other.get(i);
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						if (!signs[i]) {
							resultDatas[i] = Boolean.valueOf(datas[i]);
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
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		IArray result;
		
		if (value instanceof Boolean) {
			boolean v = ((Boolean)value).booleanValue();
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
				boolean []resultDatas = new boolean[size + 1];
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
				
				result = new BoolArray(resultDatas, resultSigns, size);
			}
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
				boolean []resultDatas = new boolean[size + 1];
				
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i) && (signs == null || !signs[i])) {
						resultDatas[i] = Boolean.valueOf(datas[i]);
					} else {
						resultSigns[i] = true;
					}
				}
				
				result = new BoolArray(resultDatas, resultSigns, size);
			}
		} else {
			Object []resultDatas = new Object[size + 1];
			if (signs == null) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						resultDatas[i] = Boolean.valueOf(datas[i]);
					} else {
						resultDatas[i] = value;
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isTrue(i)) {
						if (!signs[i]) {
							resultDatas[i] = Boolean.valueOf(datas[i]);
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
		if (array instanceof BoolArray) {
			BoolArray boolArray = (BoolArray)array;
			int len = this.size;
			int subLen = boolArray.size;
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
						pos = binarySearch(boolArray.get(t), pos, len);
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
						pos = firstIndexOf(boolArray.get(t), pos);
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
						int result = compareTo(candidate, boolArray, 1);

						if (result < 0) {
							candidate++;
						} else if (result == 0) {
							for (int i = 2, j = candidate + 1; i <= subLen; ++i, ++j) {
								if (!isEquals(j, boolArray, i)) {
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
							if (!isEquals(j, boolArray, i)) {
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
						pos = binarySearch(boolArray.get(t));
						if (pos > 0) {
							result.pushInt(pos);
						} else {
							return null;
						}
					}
				} else {
					for (int t = 1; t <= subLen; ++t) {
						pos = firstIndexOf(boolArray.get(t), 1);
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
	
	public boolean hasSigns() {
		return signs != null;
	}
	
	/**
	 * 取指定位置连续相同的元素数量
	 * @param index 位置
	 * @return 连续相同的元素数量
	 */
	public int getNextEqualCount(int index) {
		boolean []datas = this.datas;
		boolean []signs = this.signs;
		int size = this.size;
		int count = 1;
		
		if (signs == null) {
			if (datas[index]) {
				for (++index; index <= size; ++index) {
					if (datas[index]) {
						count++;
					} else {
						break;
					}
				}
			} else {
				for (++index; index <= size; ++index) {
					if (!datas[index]) {
						count++;
					} else {
						break;
					}
				}
			}
		} else {
			if (signs[index]) {
				for (++index; index <= size; ++index) {
					if (signs[index]) {
						count++;
					} else {
						break;
					}
				}
			} else if (datas[index]) {
				for (++index; index <= size; ++index) {
					if (!signs[index] && datas[index]) {
						count++;
					} else {
						break;
					}
				}
			} else {
				for (++index; index <= size; ++index) {
					if (!signs[index] && !datas[index]) {
						count++;
					} else {
						break;
					}
				}
			}
		}
		
		return count;
	}
}
