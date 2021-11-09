package com.raqsoft.util;

/**
 * 用于取最大的n个值的堆
 * @author WangXiaoJun
 *
 */
public class MaxHeap {
	// 小根堆(堆顶元素最小)，有新元素进来时先跟堆顶进行比较，如果比堆顶小则丢弃
	private Object []heap; // 用于保存二叉树各个节点的值，数组0的位置空闲
	private int maxSize; // 最多保留的值的数量
	private int currentSize; // 当前已有的值的数量

	/**
	 * 构建取maxSize个最大值的堆
	 * @param maxSize 数量
	 */
	public MaxHeap(int maxSize) {
		this.heap = new Object[maxSize + 1];
		this.maxSize = maxSize;
		this.currentSize = 0;
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
	 * @return true：当前值暂时在最大的maxSize个值范围内，false：当前值太小被丢弃
	 */
	public boolean insert(Object o) {
		Object []heap = this.heap;
		if (currentSize == maxSize) {
			if (Variant.compare(o, heap[1], true) <= 0) {
				return false;
			} else {
				deleteRoot();
				return insert(o);
			}
		} else {
			int i = ++currentSize;
			while (i != 1 && Variant.compare(o, heap[i/2], true) < 0) {
				heap[i] = heap[i/2]; // 将元素下移
				i /= 2;              // 移向父节点
			}

			heap[i] = o;
			return true;
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
			// 找出较小的子节点
			int rc = c + 1;  // 右子节点
			if (rc < currentSize && Variant.compare(heap[rc], heap[c], true) < 0) {
				c = rc;
			}

			if (Variant.compare(o, heap[c], true) > 0) {
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
}
