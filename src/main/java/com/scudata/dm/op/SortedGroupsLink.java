package com.scudata.dm.op;

import com.scudata.array.IArray;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Table;
import com.scudata.util.Variant;

/**
 * 有序记录链表,用于A.groups@h(...)
 * @author RunQian
 *
 */
class SortedGroupsLink {
	/**
	 * 链表的节点
	 * @author RunQian
	 *
	 */
	static class Node {
		private BaseRecord r; // 记录
		private Node next; // 下一个节点
		
		public Node() {
		}
		
		public Node(BaseRecord r) {
			this.r = r;
		}
		
		public void setReocrd(BaseRecord r) {
			this.r = r;
		}
		
		public BaseRecord getRecord() {
			return r;
		}
		
		public int cmp(Object []values) {
			return Variant.compareArrays(r.getFieldValues(), values, values.length);
		}
		
		public int cmp(Object value) {
			return Variant.compare(r.getNormalFieldValue(0), value, true);
		}
	}
	
	private Node first; // 首节点
	private Node prevNode; // 上一次找到的那个节点
	private int len = 0; // 长度
	
	
	/**
	 * 取出链表里的记录返回成序表
	 * @param ds
	 * @return
	 */
	public Table toTable(DataStruct ds) {
		Table table = new Table(ds, len);
		IArray mems = table.getMems();
		for (Node node = first; node != null; node = node.next) {
			mems.add(node.r);
		}
		
		return table;
	}
	
	/**
	 * 根据多字段主键值找到链表的节点，找不到则在相应的位置新建一个节点返回，外面会赋值记录
	 * @param values 主键值数组
	 * @return
	 */
	public Node put(Object []values) {
		if (prevNode == null) {
			len++;
			return first = prevNode= new Node();
		}
		
		Node prev = null;
		Node cur = prevNode;
		
		// 从上次插入的值开始比较如果小于上次插入的值则从头开始找
		while (true) {
			int cmp = cur.cmp(values);
			if (cmp < 0) {
				if (cur.next == null) {
					len++;
					Node node = new Node();
					cur.next = node;
					
					prevNode = node;
					return node;
				} else {
					prev = cur;
					cur = cur.next;
				}
			} else if (cmp == 0) {
				prevNode = cur;
				return cur;
			} else {
				if (prev == null) {
					break;
				} else {
					len++;
					Node node = new Node();
					prev.next = node;
					node.next = cur;
					
					prevNode = node;
					return node;
				}
			}
		}
		
		// 新插入的值比上次插入的值小则从头开始查找
		prev = null;
		cur = first;
		while (true) {
			int cmp = cur.cmp(values);
			if (cmp < 0) {
				prev = cur;
				cur = cur.next;
			} else if (cmp ==0) {
				prevNode = cur;
				return cur;
			} else {
				len++;
				Node node = new Node();
				if (prev != null) {
					prev.next = node;
					node.next = cur;
				} else {
					first = node;
					node.next = cur;
				}
				
				prevNode = node;
				return node;
			}
		}
	}
	
	/**
	 * 根据单字段主键值找到链表的节点，找不到则在相应的位置新建一个节点返回，外面会赋值记录
	 * @param value 主键值
	 * @return
	 */
	public Node put(Object value) {
		if (prevNode == null) {
			len++;
			return first = prevNode= new Node();
		}
		
		Node prev = null;
		Node cur = prevNode;
		
		// 从上次插入的值开始比较如果小于上次插入的值则从头开始找
		while (true) {
			int cmp = cur.cmp(value);
			if (cmp < 0) {
				if (cur.next == null) {
					len++;
					Node node = new Node();
					cur.next = node;
					
					prevNode = node;
					return node;
				} else {
					prev = cur;
					cur = cur.next;
				}
			} else if (cmp == 0) {
				prevNode = cur;
				return cur;
			} else {
				if (prev == null) {
					break;
				} else {
					len++;
					Node node = new Node();
					prev.next = node;
					node.next = cur;
					
					prevNode = node;
					return node;
				}
			}
		}
		
		// 新插入的值比上次插入的值小则从头开始查找
		prev = null;
		cur = first;
		while (true) {
			int cmp = cur.cmp(value);
			if (cmp < 0) {
				prev = cur;
				cur = cur.next;
			} else if (cmp ==0) {
				prevNode = cur;
				return cur;
			} else {
				len++;
				Node node = new Node();
				if (prev != null) {
					prev.next = node;
					node.next = cur;
				} else {
					first = node;
					node.next = cur;
				}
				
				prevNode = node;
				return node;
			}
		}
	}
}