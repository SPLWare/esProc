package com.raqsoft.dm;

/**
 * 链表条目
 * @author WangXiaoJun
 *
 * @param <E>
 */
public class LinkEntry<E> {
	private E element;
	private LinkEntry<E> next;
	
	/**
	 * 构建链表条目
	 * @param element 元素值
	 */
	public LinkEntry(E element) {
		this.element = element;
	}
	
	/**
	 * 构建链表条目
	 * @param element 元素值
	 * @param next 下一个链表条目
	 */
	public LinkEntry(E element, LinkEntry<E> next) {
		this.element = element;
		this.next = next;
	}
	
	/**
	 * 取元素值
	 * @return
	 */
	public E getElement() {
		return element;
	}
	
	/**
	 * 取下一个条目
	 * @return
	 */
	public LinkEntry<E> getNext() {
		return next;
	}
	
	/**
	 * 设置下一个条目
	 * @param next
	 */
	public void setNext(LinkEntry<E> next) {
		this.next = next;
	}
}