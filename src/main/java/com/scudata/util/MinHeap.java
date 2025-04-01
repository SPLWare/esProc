package com.scudata.util;

import java.util.Comparator;

import com.scudata.dm.comparator.BaseComparator;

/**
 * 用于取最小的n个值的堆
 * @author WangXiaoJun
 *
 */
public class MinHeap {
	// 大根堆(堆顶元素最大)，有新元素进来时先跟堆顶进行比较，如果比堆顶大则丢弃
	private Object []heap; // 用于保存二叉树各个节点的值，数组0的位置空闲
	private int maxSize; // 最多保留的值的数量
	private int currentSize; // 当前已有的值的数量
	private Comparator<Object> comparator; // 值比较器

	/**
	 * 构建取maxSize个最小值的堆
	 * @param maxSize 数量
	 */
	public MinHeap(int maxSize) {
		this(maxSize, new BaseComparator());
	}
	
	/**
	 * 构建取maxSize个最小值的堆
	 * @param maxSize 数量
	 * @param comparator 比较器
	 */
	public MinHeap(int maxSize, Comparator<Object> comparator) {
		this.heap = new Object[maxSize + 1];
		this.maxSize = maxSize;
		this.currentSize = 0;
		this.comparator = comparator;
	}

	/**
	 * 返回当前的值数量
	 * @return 数量
	 */
	public int size() {
		return currentSize;
	}

	/**
	 * 加入新值
	 * @param o 值
	 * @return true：当前值暂时在最小的maxSize个值范围内，false：当前值太大被丢弃
	 */
	public boolean insert(Object o) {
		Object []heap = this.heap;
		if (currentSize == maxSize) {
			if (comparator.compare(o, heap[1]) >= 0) {
				return false;
			} else {
				deleteRoot();
				return insert(o);
			}
		} else {
			int i = ++currentSize;
			while (i != 1 && comparator.compare(o, heap[i/2]) > 0) {
				heap[i] = heap[i/2]; // 将元素下移
				i /= 2;              // 移向父节点
			}

			heap[i] = o;
			return true;
		}
	}
	
	/**
	 * 把另一个堆的数据加到当前堆
	 * @param other
	 */
	public void insertAll(MinHeap other) {
		Object []heap = other.heap;
		for (int i = 1, currentSize = other.currentSize; i <= currentSize; ++i) {
			insert(heap[i]);
		}
	}

	/**
	 * 删除根节点
	 */
	private void deleteRoot() {
		// 把最后一个元素放在堆顶，然后自顶向下调整
		Object []heap = this.heap;
		int currentSize = this.currentSize;
		Object o = heap[currentSize];

		int i = 1;
		int c = 2; // 子节点
		while(c < currentSize) {
			// 找出较大的子节点
			int rc = c + 1;  // 右子节点
			if (rc < currentSize && comparator.compare(heap[rc], heap[c]) > 0) {
				c = rc;
			}

			if (comparator.compare(o, heap[c]) < 0) {
				heap[i] = heap[c];
				i = c;
				c *= 2;
			} else {
				break;
			}
		}

		heap[i] = o;
		heap[currentSize] = null;
		this.currentSize--;
	}

	/**
	 * 返回所有元素
	 * @return 元素数组
	 */
	public Object[] toArray() {
		Object []objs = new Object[currentSize];
		System.arraycopy(heap, 1, objs, 0, currentSize);
		//Arrays.sort(objs);
		return objs;
	}
	
	/**
	 * 取堆顶元素
	 * @return
	 */
	public Object getTop() {
		return heap[1];
	}
}
