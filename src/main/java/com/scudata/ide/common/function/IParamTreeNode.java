package com.scudata.ide.common.function;

import java.util.List;

/**
 * 参数结点接口
 *
 */
public interface IParamTreeNode {
	/** 叶子节点 */
	public static final char NORMAL = 0;
	/** 1 分号，优先级最低 */
	public static final char SEMICOLON = ';';
	/** 2 逗号 */
	public static final char COMMA = ',';
	/** 3 冒号 */
	public static final char COLON = ':';

	/**
	 * 返回节点的类型
	 * 
	 * @return char Normal、Semicolon、Comma、Colon
	 */
	char getType();

	/**
	 * 返回节点内容
	 * 
	 * @return String
	 */
	String getContent();

	/**
	 * 返回是否是叶子节点
	 * 
	 * @return boolean
	 */
	boolean isLeaf();

	/**
	 * 返回子节点数，叶子节点返回0
	 * 
	 * @return int
	 */
	int getSubSize();

	/**
	 * 返回某一子节点，从0开始计数
	 * 
	 * @param index
	 *            int
	 * @return IParam
	 */
	IParamTreeNode getSub(int index);

	/**
	 * 返回所有的节点，包括分隔符节点
	 * 
	 * @param list
	 *            List 元素是IParamTreeNode
	 */
	void getAllParam(List<IParamTreeNode> list);

	/**
	 * 返回所有的叶子节点
	 * 
	 * @param list
	 *            List 元素是IParamTreeNode
	 */
	void getAllLeafParam(List<IParamTreeNode> list);
}
