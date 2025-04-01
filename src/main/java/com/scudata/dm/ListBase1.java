package com.scudata.dm;

import java.io.Externalizable;
import java.io.IOException;
import java.util.Comparator;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.IRecord;
import com.scudata.thread.MultithreadUtil;
import com.scudata.util.Variant;

/**
 * 索引从1开始计数的列表
 * @author WangXiaoJun
 *
 */
public class ListBase1 implements Externalizable, IRecord {
	private static final long serialVersionUID = 0x02010005;

	protected Object []elementData;
	protected int size;

	/**
	 * 构建一指定初始容量的空list.
	 * @param initialCapacity 初始容量.
	 */
	public ListBase1(int initialCapacity) {
		if (initialCapacity > 0) {
			elementData = new Object[initialCapacity + 1];
		} else {
			elementData = new Object[1];
		}
	}

	/**
	 * 构建一初始容量为10的空列表.
	 */
	public ListBase1() {
		this(10);
	}

	/**
	 * 构建一指定元素的列表
	 * @param v Object[]
	 */
	public ListBase1(Object []v) {
		size = v.length;
		elementData = new Object[size + 1];
		System.arraycopy(v, 0, elementData, 1, size);
	}

	/**
	 * 复制对象
	 * @param src 源对象
	 */
	public ListBase1(ListBase1 src) {
		size = src.size;
		elementData = new Object[size + 1];
		for (int i = 1; i <= size; i++) {
			elementData[i] = src.get(i);
		}
	}

	/**
	 * 调整列表的容量，使其与元素数相等
	 */
	public void trimToSize() {
		int newLen = size + 1;
		if (newLen < elementData.length) {
			Object oldData[] = elementData;
			elementData = new Object[newLen];
			System.arraycopy(oldData, 1, elementData, 1, size);
		}
	}

	/**
	 * 使列表的容量不小于minCapacity
	 * @param minCapacity 最小容量
	 */
	public void ensureCapacity(int minCapacity) {
		int oldSize = elementData.length;
		if (oldSize <= minCapacity) {
			Object oldData[] = elementData;
			int newSize = (oldSize * 3) / 2;
			if (newSize <= minCapacity) {
				newSize = minCapacity + 1;
			}

			elementData = new Object[newSize];
			System.arraycopy(oldData, 1, elementData, 1, size);
		}
	}

	/**
	 * 返回列表的元素数目
	 * @return int
	 */
	public int size() {
		return size;
	}

	/**
	 * 返回列表是否为空.
	 * @return boolean
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * 返回列表中是否包含指定元素
	 * @param elem Object 待查找的元素
	 * @return boolean true：包含，false：不包含
	 */
	public boolean contains(Object elem) {
		for (int i = 1; i <= size; ++i) {
			if (Variant.isEquals(elem, elementData[i])) return true;
		}
		return false;
	}
	
	/**
	 * 返回列表中是否包含指定元素，使用等号比较
	 * @param elem
	 * @return boolean true：包含，false：不包含
	 */
	public boolean objectContains(Object elem) {
		for (int i = 1; i <= size; ++i) {
			if (elem == elementData[i]) return true;
		}
		return false;
	}

	/**
	 * 返回列表中是否包含指定元素
	 * @param elem Object 待查找的元素
	 * @param comparator Comparator<Object> 比较器
	 * @return boolean true：包含，false：不包含
	 */
	public boolean contains(Object elem, Comparator<Object> comparator) {
		for (int i = 1; i <= size; ++i) {
			if (comparator.compare(elem, elementData[i]) == 0) return true;
		}
		return false;
	}

	/**
	 * 对list的元素进行排序
	 * @param comparator Comparator<Object>
	 */
	public void sort(Comparator<Object> comparator) {
		MultithreadUtil.sort(elementData, 1, size + 1, comparator);
	}

	/**
	 * 二分法查找指定元素
	 * @param elem Object
	 * @param comparator Comparator<Object>
	 * @return int 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem, Comparator<Object> comparator) {
		return binarySearch(elem, 1, size, comparator);
	}

	/**
	 * 二分法查找指定元素
	 * @param elem
	 * @return int 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem) {
		Object[] elementData = this.elementData;
		int low = 1, high = size();
		while (low <= high) {
			int mid = (low + high) >> 1;
			int cmp = Variant.compare(elementData[mid], elem, true);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}

		return -low; // key not found
	}

	/**
	 * 二分法查找指定元素
	 * @param elem Object
	 * @param low int 起始位置(包含)
	 * @param high int   结束位置(包含)
	 * @param comparator Comparator<Object>
	 * @return int 元素的索引,如果不存在返回负的插入位置.
	 */
	public int binarySearch(Object elem, int low, int high, Comparator<Object> comparator) {
		Object[] elementData = this.elementData;
		while (low <= high) {
			int mid = (low + high) >> 1;
			int cmp = comparator.compare(elementData[mid], elem);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}

		return -low; // key not found
	}

	/**
	 * 返回元素出现的位置
	 * @param elem Object
	 * @param start int 起始位置(包含)
	 * @param end int   结束位置(包含)
	 * @return int 如果元素不存在返回-1
	 */
	public int indexOf(Object elem, int start, int end) {
		for (int i = start; i <= end; ++i) {
			if (Variant.isEquals(elem, elementData[i])) {
				return i;
			}
		}
		
		return -1;
	}

	/**
	 * 返回元素首次出现的位置
	 * @param elem 待查找的元素
	 * @return  如果元素不存在返回-1
	 */
	public int firstIndexOf(Object elem) {
		for (int i = 1; i <= size; ++i) {
			if (Variant.isEquals(elem, elementData[i])) {
				return i;
			}
		}
		
		return -1;
	}

	/**
	 * 返回元素首次出现的位置
	 * @param elem Object
	 * @param isSorted boolean 列表是否是有序的
	 * @return int 如果元素不存在返回-1
	 */
	public int firstIndexOf(Object elem, boolean isSorted) {
		if (isSorted) {
			int index = binarySearch(elem);
			if (index < 1) {
				return -1;
			}

			// 往前比较，找到第一个与obj相同的元素
			Object []elementData = this.elementData;
			while (index > 1) {
				if (Variant.isEquals(elem, elementData[index - 1])) {
					index--;
				} else {
					break;
				}
			}

			return index;
		} else {
			for (int i = 1; i <= size; ++i) {
				if (Variant.isEquals(elem, elementData[i])) {
					return i;
				}
			}
			
			return -1;
		}
	}

	/**
	 * 返回元素最后出现的位置
	 * @param elem Object
	 * @return 如果元素不存在返回-1
	 */
	public int lastIndexOf(Object elem) {
		for (int i = size; i > 0; --i) {
			if (Variant.isEquals(elem, elementData[i])) {
				return i;
			}
		}
		
		return -1;
	}

	/**
	 * 返回元素最后出现的位置
	 * @param elem Object
	 * @param comparator Comparator<Object>
	 * @param isSorted boolean list是否是有序的
	 * @return int 如果元素不存在返回-1
	 */
	public int lastIndexOf(Object elem, Comparator<Object> comparator, boolean isSorted) {
		if (isSorted) {
			int index = binarySearch(elem, comparator);
			if (index < 1) {
				return -1;
			}

			// 往前比较，找到第一个与obj相同的元素
			Object []elementData = this.elementData;
			while (index < size) {
				if (comparator.compare(elem, elementData[index + 1]) == 0) {
					index++;
				} else {
					break;
				}
			}

			return index;
		} else {
			for (int i = size; i > 0; --i) {
				if (comparator.compare(elem, elementData[i]) == 0) {
					return i;
				}
			}
			
			return -1;
		}
	}

	/**
	 * 返回list的元素构成的数组
	 * @return Object[]
	 */
	public Object[] toArray() {
		Object[] result = new Object[size];
		System.arraycopy(elementData, 1, result, 0, size);
		return result;
	}

	/**
	 * 把元素依次赋给a，并返回a
	 * @param a Object[]
	 * @return Object[]
	 */
	public Object[] toArray(Object a[]) {
		System.arraycopy(elementData, 1, a, 0, size);
		return a;
	}
	
	Object[] getDatas() {
		return elementData;
	}

	/**
	 * Returns the element at the specified position in this list.
	 * @param  index index of element to return. 从1开始计数
	 * @return the element at the specified position in this list.
	 */
	public Object get(int index) {
		return elementData[index];
	}

	/**
	 * Replaces the element at the specified position in this list with
	 * the specified element.
	 * @param index index of element to replace.
	 * @param element element to be stored at the specified position.
	 */
	public void set(int index, Object element) {
		elementData[index] = element;
	}

	/**
	 * Appends the specified element to the end of this list.
	 * @param o element to be appended to this list.
	 */
	public void add(Object o) {
		ensureCapacity(size + 1); // Increments modCount!!
		elementData[++size] = o;
	}

	/**
	 * Inserts the specified element at the specified position in this
	 * list. Shifts the element currently at that position (if any) and
	 * any subsequent elements to the right (adds one to their indices).
	 * @param index index at which the specified element is to be inserted.
	 * @param element element to be inserted.
	 */
	public void add(int index, Object element) {
		ensureCapacity(size + 1); // Increments modCount!!

		System.arraycopy(elementData, index, elementData, index + 1, size - index + 1);
		elementData[index] = element;
		size++;
	}

	/**
	 * Removes the element at the specified position in this list.
	 * Shifts any subsequent elements to the left (subtracts one from their indices).
	 * @param index the index of the element to removed.
	 * @return Object old value
	 */
	public Object remove(int index) {
		Object oldValue = elementData[index];
		System.arraycopy(elementData, index + 1, elementData, index, size - index);
		elementData[size--] = null;
		return oldValue;
	}

	// 删除多个元素，序号从小到大排序
	public void remove(int []seqs) {
		int delCount = 0;
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
				System.arraycopy(elementData, cur + 1, elementData, cur - delCount, moveCount);
			}
			
			delCount++;
		}

		for (int i = 0; i < delCount; ++i) {
			elementData[size--] = null;
		}
	}

	/**
	 * Removes all of the elements from this list.  The list will
	 * be empty after this call returns.
	 */
	public void clear() {
		Object []elementData = this.elementData;
		int count = size;
		for (int i = 1; i <= count; ++i)
			elementData[i] = null;

		size = 0;
	}

	/**
	 * Appends all of the elements in the specified Collection to the end of
	 * this list, in the order that they are returned by the
	 * specified Collection's Iterator.  The behavior of this operation is
	 * undefined if the specified Collection is modified while the operation
	 * is in progress.  (This implies that the behavior of this call is
	 * undefined if the specified Collection is this list, and this
	 * list is nonempty.)
	 * @param a the elements to be inserted into this list.
	 */
	public void addAll(Object[] a) {
		int numNew = a.length;
		ensureCapacity(size + numNew);  // Increments modCount
		System.arraycopy(a, 0, elementData, size + 1, numNew);
		size += numNew;
	}

	public void addAll(ListBase1 src) {
		int numNew = src.size;
		ensureCapacity(size + numNew);  // Increments modCount
		System.arraycopy(src.elementData, 1, elementData, size + 1, numNew);
		size += numNew;
	}

	public void addAll(ListBase1 src, int count) {
		ensureCapacity(size + count);  // Increments modCount
		System.arraycopy(src.elementData, 1, elementData, size + 1, count);
		size += count;
	}

	public void addSection(ListBase1 src, int srcIndex) {
		int numNew = src.size - srcIndex + 1;
		ensureCapacity(size + numNew);  // Increments modCount
		System.arraycopy(src.elementData, srcIndex, elementData, size + 1, numNew);
		size += numNew;
	}
	
	// srcStart源起始位置（包括），srcEnd源结束位置（不包括）
	public void addSection(ListBase1 src, int srcStart, int srcEnd) {
		int numNew = srcEnd - srcStart;
		ensureCapacity(size + numNew);  // Increments modCount
		System.arraycopy(src.elementData, srcStart, elementData, size + 1, numNew);
		size += numNew;
	}

	/**
	 * Inserts all of the elements in the specified Collection into this
	 * list, starting at the specified position.  Shifts the element
	 * currently at that position (if any) and any subsequent elements to
	 * the right (increases their indices).  The new elements will appear
	 * in the list in the order that they are returned by the
	 * specified Collection's iterator.
	 * @param index index at which to insert first element
	 *		    from the specified collection.
	 * @param a elements to be inserted into this list.
	 */
	public void addAll(int index, Object[] a) {
		int numNew = a.length;
		ensureCapacity(size + numNew); // Increments modCount

		System.arraycopy(elementData, index, elementData, index + numNew, size - index + 1);
		System.arraycopy(a, 0, elementData, index, numNew);
		size += numNew;
	}

	public void addAll(int index, ListBase1 src) {
		int numNew = src.size;
		ensureCapacity(size + numNew);  // Increments modCount

		System.arraycopy(elementData, index, elementData, index + numNew, size - index + 1);
		System.arraycopy(src.elementData, 1, elementData, index, numNew);
		size += numNew;
	}

	public void addAll(int index, ListBase1 src, int count) {
		ensureCapacity(size + count);  // Increments modCount

		System.arraycopy(elementData, index, elementData, index + count, size - index + 1);
		System.arraycopy(src.elementData, 1, elementData, index, count);
		size += count;
	}

	/**
	 * Removes from this List all of the elements whose index is between
	 * fromIndex, inclusive and toIndex, inclusive.  Shifts any succeeding
	 * elements to the left (reduces their index).
	 * This call shortens the list by <tt>(toIndex - fromIndex)</tt> elements.
	 * (If <tt>toIndex==fromIndex</tt>, this operation has no effect.)
	 *
	 * @param fromIndex index of first element to be removed.
	 * @param toIndex index of last element to be removed.
	 */
	public void removeRange(int fromIndex, int toIndex) {
		System.arraycopy(elementData, toIndex + 1, elementData, fromIndex, size - toIndex);

		int newSize = size - (toIndex - fromIndex + 1);
		while (size != newSize) {
			elementData[size--] = null;
		}
	}

	public void reserve(int start, int end) {
		int newSize = end - start + 1;
		System.arraycopy(elementData, start, elementData, 1, newSize);

		while (size != newSize) {
			elementData[size--] = null;
		}
	}

	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeInt(size);
		out.writeInt(elementData.length);

		for (int i = 1; i <= size; ++i) {
			out.writeObject(elementData[i], true);
		}

		return out.toByteArray();
	}

	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);

		size = in.readInt();
		int length = in.readInt();
		Object elementData[] = new Object[length];
		this.elementData = elementData;

		for (int i = 1; i <= size; ++i) {
			elementData[i] = in.readObject(true);
		}
	}

	/**
	 * 写内容到流
	 * @param out 输出流
	 * @throws IOException
	 */
	public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException {
		out.writeByte(1);
		Object elementData[] = this.elementData;
		out.writeInt(size);
		out.writeInt(elementData.length);

		for (int i = 1; i <= size; ++i) {
			out.writeObject(elementData[i]);
		}
	}

	/**
	 * 从流中读内容
	 * @param in 输入流
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException {
		in.readByte(); // 版本
		size = in.readInt();
		int length = in.readInt();
		Object elementData[] = new Object[length];
		this.elementData = elementData;

		for (int i = 1; i <= size; ++i) {
			elementData[i] = in.readObject();
		}
	}
}
