package com.scudata.array;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Comparator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.expression.Relation;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * String数组类的另一种实现，用byte[]，偏移，长度 保存所有String的值。
 * 只支持表达式过滤计算，不支持与其它列的计算
 * 只在列式游标内部过滤时用。不会被取出。
 * @author LW
 *
 */
public class ByteBufferArray implements IArray {
	private static final long serialVersionUID = 1L;
	
	private byte[] buffer;//共用的数组
	private int[] pos;//每个元素在buffer的开始位置
	private byte[] len;//每个元素在buffer的长度
	private int size;
		
	public ByteBufferArray(byte[] buffer, int[] pos, byte[] len, int size) {
		this.buffer = buffer;
		this.pos = pos;
		this.len = len;
		this.size = size;
	}

	public ByteBufferArray(byte[] buffer, int initialCapacity) {
		pos = new int[++initialCapacity];
		len = new byte[initialCapacity];
		this.buffer = buffer;
	}

	private static int compareArrays(byte[] b1, int pos1, int len1, byte[] b2) {
		int len2 = b2.length;
		
		if (len1 == len2) {
			for(int i = 0; i < len1; ++i) {
				if (b1[i + pos1] < b2[i]) {
					return -1;
				} else if (b1[i + pos1] > b2[i]) {
					return 1;
				}
			}
			
			return 0;
		} else if (len1 < len2) {
			for(int i = 0; i < len1; ++i) {
				if (b1[i + pos1] < b2[i]) {
					return -1;
				} else if (b1[i + pos1] > b2[i]) {
					return 1;
				}
			}
			
			return -1;
		} else {
			for(int i = 0; i < len2; ++i) {
				if (b1[i + pos1] < b2[i]) {
					return -1;
				} else if (b1[i + pos1] > b2[i]) {
					return 1;
				}
			}
			
			return 1;
		}
	}
	
	public static int compare(byte[] d1, int pos1, int len1, byte[] d2) {
		if (pos1 == -1) {
			return d2 == null ? 0 : -1;
		} else if (d2 == null) {
			return 1;
		} else {
			int cmp = compareArrays(d1, pos1, len1, d2);
			return cmp < 0 ? -1 : (cmp > 0 ? 1 : 0);
		}
	}
	
	private static int compare(byte[] d1, int pos1, int len1, Object d2) {
		if (d2 instanceof byte[]) {
			if (d1 == null) {
				return -1;
			} else {
				int cmp = compareArrays(d1, pos1, len1, (byte[]) d2);
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
	
	public byte[] getBuffer() {
		return buffer;
	}

	/**
	 * 取数组的类型串，用于错误信息提示
	 * @return 类型串
	 */
	public String getDataType() {
		throw new RuntimeException();
	}
	
	/**
	 * 复制数组
	 * @return
	 */
	public IArray dup() {
		throw new RuntimeException();
	}
	
	/**
	 * 写内容到流
	 * @param out 输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		throw new RuntimeException();
	}
	
	/**
	 * 从流中读内容
	 * @param in 输入流
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		throw new RuntimeException();
	}
	
	public byte[] serialize() throws IOException{
		throw new RuntimeException();
	}
	
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		throw new RuntimeException();
	}
	
	/**
	 * 返回一个同类型的数组
	 * @param count
	 * @return
	 */
	public IArray newInstance(int count) {
		throw new RuntimeException();
	}

	/**
	 * 追加元素，如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void add(Object o) {
		throw new RuntimeException();
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(IArray array) {
		throw new RuntimeException();
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param count 元素个数
	 */
	public void addAll(IArray array, int count) {
		throw new RuntimeException();
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param index 要加入的数据的起始位置
	 * @param count 数量
	 */
	public void addAll(IArray array, int index, int count) {
		throw new RuntimeException();
	}
	
	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 */
	public void addAll(Object []array) {
		throw new RuntimeException();
	}

	/**
	 * 插入元素，如果类型不兼容则抛出异常
	 * @param index 插入位置，从1开始计数
	 * @param o 元素值
	 */
	public void insert(int index, Object o) {
		throw new RuntimeException();
	}

	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, IArray array) {
		throw new RuntimeException();
	}
	
	/**
	 * 在指定位置插入一组元素，如果类型不兼容则抛出异常
	 * @param pos 位置，从1开始计数
	 * @param array 元素数组
	 */
	public void insertAll(int pos, Object []array) {
		throw new RuntimeException();
	}

	public void push(byte[] str) {
		throw new RuntimeException();
	}

	public void pushString(byte[] str) {
		throw new RuntimeException();
	}

	/**
	 * 追加元素（不检查容量，认为有足够空间存放元素），如果类型不兼容则抛出异常
	 * @param o 元素值
	 */
	public void push(Object o) {
		throw new RuntimeException();
	}
	
	/**
	 * 追加一个空成员（不检查容量，认为有足够空间存放元素）
	 */
	public void pushNull() {
		throw new RuntimeException();
	}
	
	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void push(IArray array, int index) {
		throw new RuntimeException();
	}
	
	/**
	 * 把array中的第index个元素添加到当前数组中，如果类型不兼容则抛出异常
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void add(IArray array, int index) {
		throw new RuntimeException();
	}
	
	/**
	 * 把array中的第index个元素设给到当前数组的指定元素，如果类型不兼容则抛出异常
	 * @param curIndex 当前数组的元素索引，从1开始计数
	 * @param array 数组
	 * @param index 元素索引，从1开始计数
	 */
	public void set(int curIndex, IArray array, int index) {
		throw new RuntimeException();
	}

	/**
	 * 取指定位置元素
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public Object get(int index) {
		return new String(buffer, pos[index], len[index]);
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
		throw new RuntimeException();
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
		throw new RuntimeException();
	}
	
	/**
	 * 取指定位置元素组成新数组
	 * @param IArray 位置数组
	 * @return IArray
	 */
	public IArray get(IArray indexArray) {
		throw new RuntimeException();
	}
	
	/**
	 * 取某一区段组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @return IArray
	 */
	public IArray get(int start, int end) {
		throw new RuntimeException();
	}

	/**
	 * 使列表的容量不小于minCapacity
	 * @param minCapacity 最小容量
	 */
	public void ensureCapacity(int minCapacity) {
		throw new RuntimeException();
	}
	
	/**
	 * 调整容量，使其与元素数相等
	 */
	public void trimToSize() {
		throw new RuntimeException();
	}

	/**
	 * 判断指定位置的元素是否是空
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isNull(int index) {
		return pos[index] == -1;
	}
	
	/**
	 * 判断元素是否是True
	 * @return BoolArray
	 */
	public BoolArray isTrue() {
		int size = this.size;
		int[] pos = this.pos;
		boolean []resultDatas = new boolean[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			resultDatas[i] = pos[i] >= 0;
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
		int[] pos = this.pos;
		boolean []resultDatas = new boolean[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			resultDatas[i] = pos[i] < 0;
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
		return pos[index] != -1;
	}
	
	/**
	 * 判断指定位置的元素是否是False
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isFalse(int index) {
		// 空则是false
		return pos[index] == -1;
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
		throw new RuntimeException();
	}

	/**
	 * 删除指定位置的元素
	 * @param index 索引，从1开始计数
	 */
	public void remove(int index) {
		throw new RuntimeException();
	}
	
	/**
	 * 删除指定区间内的元素
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 */
	public void removeRange(int fromIndex, int toIndex) {
		throw new RuntimeException();
	}
	
	/**
	 * 删除指定位置的元素，序号从小到大排序
	 * @param seqs 索引数组
	 */
	public void remove(int []seqs) {
		throw new RuntimeException();
	}
	
	/**
	 * 保留指定区间内的数据
	 * @param start 起始位置（包含）
	 * @param end 结束位置（包含）
	 */
	public void reserve(int start, int end) {
		throw new RuntimeException();
	}

	public int size() {
		return size;
	}
	
	/**
	 * 返回数组的非空元素数目
	 * @return 非空元素数目
	 */
	public int count() {
		int[] pos = this.pos;
		int size = this.size;
		int count = size;
		
		for (int i = 1; i <= size; ++i) {
			if (pos[i] == -1) {
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
		
		int[] pos = this.pos;
		for (int i = 1; i <= size; ++i) {
			if (pos[i] != -1) {
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
		throw new RuntimeException();
	}

	/**
	 * 修改数组指定元素的值，如果类型不兼容则抛出异常
	 * @param index 索引，从1开始计数
	 * @param obj 值
	 */
	public void set(int index, Object obj) {
		throw new RuntimeException();
	}
	
	/**
	 * 删除所有的元素
	 */
	public void clear() {
		throw new RuntimeException();
	}

	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @return int 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem) {
		if (elem instanceof String) {
			byte[] v = ((String)elem).getBytes();
			
			byte[] buf = this.buffer;
			int[] pos = this.pos;
			byte[] len = this.len;
			int low = 1, high = size;
			
			while (low <= high) {
				int mid = (low + high) >> 1;
				int cmp = compare(buf, pos[mid], len[mid], v);
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
			if (size > 0 && pos[1] == -1) {
				return 1;
			} else {
				return -1;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare"));
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
		
		if (elem instanceof String) {
			byte[] v = ((String)elem).getBytes();
			byte[] buffer = this.buffer;
			int[] pos = this.pos;
			byte[] len = this.len;
			int low = start, high = end;
			
			while (low <= high) {
				int mid = (low + high) >> 1;
				int cmp = compare(buffer, pos[mid], len[mid], v);
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
			if (end > 0 && pos[start] == -1) {
				return start;
			} else {
				return -1;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare"));
		}
	}
	
	/**
	 * 返回列表中是否包含指定元素
	 * @param elem Object 待查找的元素
	 * @return boolean true：包含，false：不包含
	 */
	public boolean contains(Object elem) {
		if (elem instanceof String) {
			byte[] v = ((String)elem).getBytes();
			byte[] buffer = this.buffer;
			int[] pos = this.pos;
			byte[] len = this.len;
			int size = this.size;
			
			for (int i = 1; i <= size; ++i) {
				if (pos[i] != -1 && compare(buffer, pos[i], len[i], v) == 0) {
					return true;
				}
			}
			
			return false;
		} else if (elem == null) {
			int size = this.size;
			int[] pos = this.pos;
			for (int i = 1; i <= size; ++i) {
				if (pos[i] == -1) {
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
		throw new RuntimeException();
	}
	
	/**
	 * 返回元素在数组中首次出现的位置
	 * @param elem 待查找的元素
	 * @param start 起始查找位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	public int firstIndexOf(Object elem, int start) {
		throw new RuntimeException();
	}
	
	/**
	 * 返回元素在数组中最后出现的位置
	 * @param elem 待查找的元素
	 * @param start 从后面开始查找的位置（包含）
	 * @return 如果元素存在则返回值大于0，否则返回0
	 */
	public int lastIndexOf(Object elem, int start) {
		throw new RuntimeException();
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
		throw new RuntimeException();
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
		throw new RuntimeException();
	}

	/**
	 * 对数组成员求非
	 * @return IArray 非值数组
	 */
	public IArray not() {
		int[] pos = this.pos;
		int size = this.size;
		
		boolean []newDatas = new boolean[size + 1];
		for (int i = 1; i <= size; ++i) {
			newDatas[i] = pos[i] == -1;
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
		throw new RuntimeException();
	}

	/**
	 * 计算数组的成员与指定常数的和
	 * @param value 常数
	 * @return 和数组
	 */
	public IArray memberAdd(Object value) {
		throw new RuntimeException();
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
		throw new RuntimeException();
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
		if (array instanceof ConstArray) {
			return calcRelation(array.get(1), relation);
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
			return calcRelation(((String)value).getBytes(), relation);
		} else if (value == null) {
			return calcRelationNull(pos, size, relation);
		} else {
			boolean b = Variant.isTrue(value);
			int size = this.size;
			int[] pos = this.pos;
			
			if (relation == Relation.AND) {
				BoolArray result;
				if (!b) {
					result = new BoolArray(false, size);
				} else {
					boolean []resultDatas = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = pos[i] != -1;
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
						resultDatas[i] = pos[i] != -1;
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
	
	/**
	 * 计算数组的成员和null的关系
	 * @param signs 数组成员是否为空标志，true为空
	 * @param size 数组成员数
	 * @param relation 比较关系
	 * @return BoolArray 比较值数组
	 */
	private static BoolArray calcRelationNull(int []pos, int size, int relation) {
		boolean[] signs = null;
		for (int i = 1; i <= size; ++i) {
			if (pos[i] == -1) {
				if (signs == null) signs = new boolean[size];
				signs[i] = true;
			}
		}
		
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
	
	private BoolArray calcRelation(byte[] value, int relation) {
		int size = this.size;
		int[] pos = this.pos;
		byte[] len = this.len;
		byte[] d1 = this.buffer;
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// 是否等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1, pos[i], len[i], value) == 0;
			}
		} else if (relation == Relation.GREATER) {
			// 是否大于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1, pos[i], len[i], value) > 0;
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// 是否大于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1, pos[i], len[i], value) >= 0;
			}
		} else if (relation == Relation.LESS) {
			// 是否小于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1, pos[i], len[i], value) < 0;
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// 是否小于等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1, pos[i], len[i], value) <= 0;
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// 是否不等于判断
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1, pos[i], len[i], value) != 0;
			}
		} else if (relation == Relation.AND) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = pos[i] != -1;
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

	/**
	 * 比较两个数组的大小
	 * @param array 右侧数组
	 * @return 1：当前数组大，0：两个数组相等，-1：当前数组小
	 */
	public int compareTo(IArray array) {
		throw new RuntimeException();
	}
	/**
	 * 计算数组的2个成员的比较值
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	public int memberCompare(int index1, int index2) {
		throw new RuntimeException();
	}
	
	/**
	 * 判断数组的两个成员是否相等
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	public boolean isMemberEquals(int index1, int index2) {
		throw new RuntimeException();
	}
	
	/**
	 * 判断两个数组的指定元素是否相同
	 * @param curIndex 当前数组的元素的索引
	 * @param array 要比较的数组
	 * @param index 要比较的数组的元素的索引
	 * @return true：相同，false：不相同
	 */
	public boolean isEquals(int curIndex, IArray array, int index) {
		throw new RuntimeException();
	}
	
	/**
	 * 判断数组的指定元素是否与给定值相等
	 * @param curIndex 数组元素索引，从1开始计数
	 * @param value 值
	 * @return true：相等，false：不相等
	 */
	public boolean isEquals(int curIndex, Object value) {
		if (value instanceof String) {
			return compare(buffer, pos[curIndex], len[curIndex], ((String)value).getBytes()) == 0;
		} else if (value == null) {
			return pos[curIndex] == -1;
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
		return compare(buffer, pos[curIndex], len[curIndex], array.get(index));
	}
	
	/**
	 * 比较数组的指定元素与给定值的大小
	 * @param curIndex 当前数组的元素的索引
	 * @param value 要比较的值
	 * @return
	 */
	public int compareTo(int curIndex, Object value) {
		return compare(buffer, pos[curIndex], len[curIndex], value);
	}
	
	/**
	 * 取指定成员的哈希值
	 * @param index 成员索引，从1开始计数
	 * @return 指定成员的哈希值
	 */
	public int hashCode(int index) {
		if (pos[index] != -1) {
			return get(index).hashCode();
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
		throw new RuntimeException();
	}
	
	/**
	 * 得到最小的成员
	 * @return
	 */
	public Object min() {
		throw new RuntimeException();
	}

	/**
	 * 计算两个数组的相对应的成员的关系运算，只计算result为真的行
	 * @param array 右侧数组
	 * @param relation 运算关系，参照Relation（大于、小于、等于、...）
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	public void calcRelations(IArray array, int relation, BoolArray result, boolean isAnd) {
		if (array instanceof ConstArray) {
			calcRelations(array.get(1), relation, result, isAnd);
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
			calcRelations(((String)value).getBytes(), relation, result, isAnd);
		} else if (value == null) {
			calcRelationsNull(pos, size, relation, result, isAnd);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
					getDataType(), Variant.getDataType(value)));
		}
	}

	/**
	 * 计算数组的成员和null的关系
	 * @param signs 数组成员是否为空标志，true为空
	 * @param size 数组成员数
	 * @param relation 比较关系
	 * @param result 左侧计算结果，当前关系运算结果需要与左侧结果做逻辑&&或者||运算
	 * @param isAnd true：与左侧做 && 运算，false：与左侧做 || 运算
	 */
	private static void calcRelationsNull(int []pos, int size, int relation, BoolArray result, boolean isAnd) {
		boolean[] signs = null;
		for (int i = 1; i <= size; ++i) {
			if (pos[i] == -1) {
				if (signs == null) signs = new boolean[size];
				signs[i] = true;
			}
		}
		
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
	
	private void calcRelations(byte[] value, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		byte[] d1 = this.buffer;
		int[] pos = this.pos;
		byte[] len = this.len;
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// 与左侧结果执行&&运算
			if (relation == Relation.EQUAL) {
				// 是否等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1, pos[i], len[i], value) != 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1, pos[i], len[i], value) <= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1, pos[i], len[i], value) < 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1, pos[i], len[i], value) >= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1, pos[i], len[i], value) > 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1, pos[i], len[i], value) == 0) {
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
					if (!resultDatas[i] && compare(d1, pos[i], len[i], value) == 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// 是否大于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1, pos[i], len[i], value) > 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// 是否大于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1, pos[i], len[i], value) >= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS) {
				// 是否小于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1, pos[i], len[i], value) < 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// 是否小于等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1, pos[i], len[i], value) <= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// 是否不等于判断
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1, pos[i], len[i], value) != 0) {
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
		throw new RuntimeException();
	}
	
	/**
	 * 取某一区段标识数组取值为真的行组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @param signArray 标识数组
	 * @return IArray
	 */
	public IArray select(int start, int end, IArray signArray) {
		throw new RuntimeException();
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
		throw new RuntimeException();
	}
	
	/**
	 * 把成员填到指定的数组
	 * @param result 用于存放成员的数组
	 */
	public void toArray(Object []result) {
		throw new RuntimeException();
	}
	
	/**
	 * 把数组从指定位置拆成两个数组
	 * @param pos 位置，包含
	 * @return 返回后半部分元素构成的数组
	 */
	public IArray split(int pos) {
		throw new RuntimeException();
	}
	
	/**
	 * 把指定区间元素分离出来组成新数组
	 * @param from 起始位置，包含
	 * @param to 结束位置，包含
	 * @return
	 */
	public IArray split(int from, int to) {
		throw new RuntimeException();
	}
	
	/**
	 * 对数组的元素进行排序
	 */
	public void sort() {
		throw new RuntimeException();
	}
	
	/**
	 * 对数组的元素进行排序
	 * @param comparator 比较器
	 */
	public void sort(Comparator<Object> comparator) {
		throw new RuntimeException();
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
		throw new RuntimeException();
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
		throw new RuntimeException();
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
		throw new RuntimeException();
	}

	/**
	 * 根据条件从当前数组选出标志为true的，标志为false的置成value
	 * @param signArray 标志数组
	 * @param other 值
	 * @return IArray
	 */
	public IArray combine(IArray signArray, Object value) {
		throw new RuntimeException();
	}

	public int[] getPos() {
		return pos;
	}
	
	public byte[] getLen() {
		return len;
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
	
	/**
	 * 取指定位置连续相同的元素数量
	 * @param index 位置
	 * @return 连续相同的元素数量
	 */
	public int getNextEqualCount(int index) {
		throw new RuntimeException();
	}
}
