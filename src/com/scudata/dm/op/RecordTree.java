package com.scudata.dm.op;

import java.util.ArrayDeque;
import java.util.Deque;

import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

/**
 * 记录构成的红黑树，按主键进行比较
 * 用于哈希法外存分组时管理分组字段哈希值相同的组
 * @author RunQian
 *
 */
public class RecordTree {
	public static final boolean RED = true; // 红色
	public static final boolean BLACK = false; // 黑色
	
	/**
	 * 红黑树的节点
	 * @author RunQian
	 *
	 */
	public static class Node {
		Record r;
		boolean color;
		
		Node parent;
		Node left;
		Node right;
		
		public Node(Record r, boolean color) {
			this.r = r;
			this.color = color;
		}
		
		public Node(boolean color) {
			this.color = color;
		}
		
		public Node(boolean color, Node parent){
			this.color = color;
			this.parent = parent;
		}
	}
	
	private Node root;
	
	public RecordTree() {
	}
	
	public RecordTree(Record r) {
		root = new Node(r, BLACK);
	}
	
	/**
	 * 根据主键查找记录节点，找不到返回新建的节点，外面会把记录赋给新节点
	 * @param values 主键值数组
	 * @return
	 */
	public Node get(Object []values) {
		if (root == null) {
			return root = new Node(BLACK);
		}

		Node current = root;
		Node parent = null;
		
		while (true) {
			Object []curValues = current.r.getFieldValues();
			int cmp = Variant.compareArrays(curValues, values, values.length);
			if (cmp == 0) {
				return current;
			} else if (cmp > 0) {
				parent = current;
				current = current.left;
				
				if (current == null) {
					Node newNode = new Node(RED, parent);
					parent.left = newNode;
					
					balanceInsertion(newNode);
					//size++;
					return newNode;
				}
			} else {
				parent = current;
				current = current.right;
				
				if (current == null) {
					Node newNode = new Node(RED, parent);
					parent.right = newNode;
					
					balanceInsertion(newNode);
					//size++;
					return newNode;
				}
			}
		}
	}
	
	// 插入节点后对树做平衡
	private void balanceInsertion(Node node) {
		Node parent;
		Node gparent;
		
		while ((parent = node.parent) != null && parent.color == RED) {
			gparent = parent.parent;
			if (gparent.left == parent) {
				Node uncle = gparent.right;
				if (uncle != null && uncle.color == RED) {
					parent.color = BLACK;
					uncle.color = BLACK;
					gparent.color = RED;
					node = gparent;
				} else {
					if (parent.right == node) {
						leftRonate(parent);
						Node temp = node;
						node = parent;
						parent = temp;
					}
					
					parent.color = BLACK;
					gparent.color = RED;
					rightRonate(gparent);
				}
			} else {
				Node uncle = gparent.left;
				if (uncle != null && uncle.color == RED) {
					parent.color = BLACK;
					uncle.color = BLACK;
					gparent.color = RED;
					node = gparent;
				} else {
					if (parent.left == node) {
						rightRonate(parent);
						Node temp = node;
						node = parent;
						parent = temp;
					}
					
					parent.color = BLACK;
					gparent.color = RED;
					leftRonate(gparent);
				}
			}
		}
		
		root.color = BLACK;
	}
	
	//对某个节点进行左旋
	private void leftRonate(Node x) {
		Node y = x.right;
		if (y.left != null) {
			y.left.parent = x;
		}

		x.right = y.left;
		y.left = x;
		y.parent = x.parent;

		if(x.parent != null) {
			if(x.parent.left == x) {
				x.parent.left = y;
			} else {
				x.parent.right = y;
			}
		} else {
			root = y;
		}
		
		x.parent = y;
	}
	
	//对某个节点进行右旋
	private void rightRonate(Node x) {
		Node y = x.left;
		if(y.right != null) {
			y.right.parent = x;
		}
		
		y.parent = x.parent;
		x.left = y.right;
		y.right = x;
		
		if(x.parent != null) {
			if(x.parent.left == x) {
				x.parent.left = y;
			} else {
				x.parent.right = y;
			}
		} else {
			root = y;
		}
		
		x.parent = y;
	}
	
	// 取最小的节点
	private Node minimum(Node node) {
		while (node.left != null) {
			node = node.left;
		}
		
		return node;
	}
	
	// 取下一个节点
	private Node successor(Node node) {
		if (node.right != null) {
			return minimum(node.right);
		}
		
		Node parent = node.parent;
		while (parent != null && node == parent.right) {
			node = parent;
			parent = parent.parent;
		}
		
		return parent;
	}
	
	/**
	 * 深度优先法取所有节点的记录
	 * @param out 输出参数，存放记录
	 */
	public void depthTraverse(Sequence out) {
		Node node = root;
		if (node == null) {
			return;
		}
		
		// 取值最小的节点，即最左的节点
		node = minimum(node);
		
		do {
			out.add(node.r);
			node = successor(node);
		} while (node != null);
	}
	
	private void recursiveTraverse(Node node, Sequence out) {
		if (node.left != null) {
			recursiveTraverse(node.left, out);
		}
		
		out.add(node.r);
		
		if (node.right != null) {
			recursiveTraverse(node.right, out);
		}
	}
	
	private void breadthTraverse(Node node, Sequence out) {
		Deque<Node> deque = new ArrayDeque<Node>();
		if (node != null) {
			deque.offer(node);
		}
		
		while (!deque.isEmpty()) {
			Node tmp = deque.poll();
			out.add(tmp.r);
			if (tmp.left != null) {
				deque.offer(tmp.left);
			}
			
			if (tmp.right != null) {
				deque.offer(tmp.right);
			}
		}
	}

	/**
	 * 用递归方法深度取所有节点的记录
	 * @param out 输出参数，存放记录
	 */
	public void recursiveTraverse(Sequence out) {
		if (root != null) {
			recursiveTraverse(root, out);
		}
	}
	
	/**
	 * 广度优先法取所有节点的记录
	 * @param out 输出参数，存放记录
	 */
	public void breadthTraverse(Sequence out) {
		if (root != null) {
			breadthTraverse(root, out);
		}
	}
}