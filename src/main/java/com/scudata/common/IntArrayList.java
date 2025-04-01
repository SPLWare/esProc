package com.scudata.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;

public class IntArrayList extends AbstractList<Integer> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private transient int[] _data = null;
	private int _size = 0;

	public IntArrayList() {
		this(8);
	}

	public IntArrayList(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity " + capacity);
		}
		_data = new int[capacity];
	}

	public int capacity() {
		return _data.length;
	}

	public int size() {
		return _size;
	}

	// 重设成员数
	public void setSize(int size) {
		this._size = size;
	}
	
	public int[] toIntArray() {
		int[] tmp = new int[_size];
		System.arraycopy(_data, 0, tmp, 0, _size);
		return tmp;
	}

	public int getInt(int index) {
		// 有上层负责索引的有效性
		//checkRange(index);
		return _data[index];
	}

	public boolean containsInt(int value) {
		return (-1 != indexOfInt(value));
	}

	public int indexOfInt(int value) {
		for(int i=0;i<_size;i++) {
			if(value == _data[i]) {
				return i;
			}
		}
		return -1;
	}

	public int lastIndexOfInt(int value) {
		for(int i=_size-1;i>=0;i--) {
			if(value == _data[i]) {
				return i;
			}
		}
		return -1;
	}

	public int setInt(int index, int value) {
		checkRange(index);
		int old = _data[index];
		_data[index] = value;
		return old;
	}

	public boolean addInt(int value) {
		ensureCapacity(_size+1);
		_data[_size++] = value;
		return true;
	}

	public boolean addAll(int []vals) {
		if (vals == null) return false;

		int len = vals.length;
		ensureCapacity(_size+len);
		for (int i = 0; i < len; ++i) {
			_data[_size++] = vals[i];
		}

		return true;
	}

	public void addInt(int index, int value) {
		checkRangeIncludingEndpoint(index);
		ensureCapacity(_size+1);
		int numtomove = _size-index;
		System.arraycopy(_data,index,_data,index+1,numtomove);
		_data[index] = value;
		_size++;
	}

	public void clear() {
		modCount++;
		_size = 0;
	}

	public int removeIntAt(int index) {
		checkRange(index);
		modCount++;
		int oldval = _data[index];
		int numtomove = _size - index - 1;
		if(numtomove > 0) {
			System.arraycopy(_data,index+1,_data,index,numtomove);
		}
		_size--;
		return oldval;
	}

	public boolean removeInt(int value) {
		int index = indexOfInt(value);
		if(-1 == index) {
			return false;
		} else {
			removeIntAt(index);
			return true;
		}
	}

	public void ensureCapacity(int mincap) {
		modCount++;
		if(mincap > _data.length) {
			int newcap = (_data.length * 3)/2 + 1;
			int[] olddata = _data;
			_data = new int[newcap < mincap ? mincap : newcap];
			System.arraycopy(olddata,0,_data,0,_size);
		}
	}

	public void trimToSize() {
		modCount++;
		if(_size < _data.length) {
			int[] olddata = _data;
			_data = new int[_size];
			System.arraycopy(olddata,0,_data,0,_size);
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException{
		out.defaultWriteObject();
		out.writeInt(_data.length);
		for(int i=0;i<_size;i++) {
			out.writeInt(_data[i]);
		}
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		_data = new int[in.readInt()];
		for(int i=0;i<_size;i++) {
			_data[i] = in.readInt();
		}
	}

	private final void checkRange(int index) {
		if(index < 0 || index >= _size) {
			throw new IndexOutOfBoundsException("Should be at least 0 and less than " + _size + ", found " + index);
		}
	}

	private final void checkRangeIncludingEndpoint(int index) {
		if(index < 0 || index > _size) {
			throw new IndexOutOfBoundsException("Should be at least 0 and at most " + _size + ", found " + index);
		}
	}


	public Integer get(int index) {
		return new Integer(getInt(index));
	}

	public boolean contains(Object value) {
		return containsInt(((Integer)value).intValue());
	}


	public boolean isEmpty() {
		return (0 == size());
	}

	public int binarySearch(int key) {
		int low = 0;
		int high = _size - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int midVal = _data[mid];

			if (midVal < key)
				low = mid + 1;
			else if (midVal > key)
				high = mid - 1;
			else
				return mid; // key found
		}
		
		return -(low + 1);  // key not found.
	}
	
	/**
	 * 二分法查找元素，元素有序
	 * @param key 待查找的元素值
	 * @param low 起始位置，包括
	 * @param high 结束位置，包括
	 * @return 返回元素位置，或-insert position - 1
	 */
	public int binarySearch(int key, int low, int high) {
		while (low <= high) {
			int mid = (low + high) >>> 1;
			int midVal = _data[mid];

			if (midVal < key)
				low = mid + 1;
			else if (midVal > key)
				high = mid - 1;
			else
				return mid; // key found
		}
		
		return -(low + 1);  // key not found.
	}
	
	public int indexOf(Object value) {
		return indexOfInt(((Integer)value).intValue());
	}

	public int lastIndexOf(Object value) {
		return lastIndexOfInt(((Integer)value).intValue());
	}

	public Integer set(int index, Integer value) {
		return new Integer(setInt(index,((Integer)value).intValue()));
	}

	public boolean add(Integer value) {
		return addInt(((Integer)value).intValue());
	}

	public void add(int index, Integer value) {
		addInt(index,((Integer)value).intValue());
	}

	public Integer remove(int index) {
		return new Integer(removeIntAt(index));
	}

	public boolean remove(Object value) {
		return removeInt(((Integer)value).intValue());
	}

}
