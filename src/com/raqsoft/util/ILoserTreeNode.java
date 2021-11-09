package com.raqsoft.util;

/**
 * 用于有序数据的多路归并运算，每一路是一个节点
 * @author RunQian
 *
 */
public interface ILoserTreeNode extends Comparable<ILoserTreeNode> {
	/**
	 * 弹出当前归并路的当前元素
	 * @return 元素值
	 */
	Object popCurrent();
	
	/**
	 * 返回当前归并路是否还有元素
	 * @return
	 */
	boolean hasNext();
}
