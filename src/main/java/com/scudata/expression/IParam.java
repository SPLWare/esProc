package com.scudata.expression;

import java.util.ArrayList;
import java.util.List;

import com.scudata.cellset.INormalCell;
import com.scudata.dm.*;

/**
 * 函数参数接口
 * 函数参数采用多叉树结构存储
 * @author RunQian
 *
 */
public interface IParam {
	static final char NONE = 0; // 还没有产生节点，用于参数分析过程
	
	// 参数节点的类型
	public static final char Semicolon = ';'; // 参数分号分隔符
	public static final char Comma = ',';     // 参数逗号分隔符
	public static final char Colon = ':';     // 参数冒号分隔符
	public static final char Normal = 0;      // 叶子节点

	/**
	 * 返回节点的类型
	 * @return char Normal、Semicolon、Comma、Colon
	 */
	char getType();

	/**
	 * 返回是否是叶子节点
	 * @return boolean true：是，false：不是
	 */
	boolean isLeaf();

	/**
	 * 返回子节点数，叶子节点返回0
	 * @return int 子节点数
	 */
	int getSubSize();

	/**
	 * 返回某一子节点
	 * @param index 序号，从0开始计数
	 * @return IParam 子节点
	 */
	IParam getSub(int index);

	/**
	 * 返回当前节点的表达式字符串，当前节点为叶子节点
	 * @return Expression
	 */
	Expression getLeafExpression();

	/**
	 * 取所有叶子节点的表达式
	 * @param List 输出值，用于存放表达式
	 */
	void getAllLeafExpression(ArrayList<Expression> list);

	/**
	 * 返回表达式数组，只支持单层的参数
	 * @param function 函数名，用于抛出异常
	 * @param canNull 参数是否可空
	 * @return 表达式数组
	 */
	Expression[] toArray(String function, boolean canNull);
	
	/**
	 * 返回表达式字符串数组，只支持单层的参数
	 * @param function 函数名，用于抛出异常
	 * @param canNull 参数是否可空
	 * @return 表达式串数组
	 */
	String []toStringArray(String function, boolean canNull);
	
	/**
	 * 返回字段名数组
	 * @param function 函数名，用于抛出异常
	 * @return 字段名数组
	 */
	String []toIdentifierNames(String function);
	
	/**
	 * 返回是否包含指定参数
	 * @param name 参数名
	 * @return boolean true：包含，false：不包含
	 */
	boolean containParam(String name);

	/**
	 * 查找表达式中用到参数
	 * @param ctx 计算上下文
	 * @param resultList 输出值，用到的参数会添加到这里面
	 */
	void getUsedParams(Context ctx, ParamList resultList);
	
	/**
	 * 查找表达式中可能用到的字段，可能取得不准确或者包含多个表的
	 * @param ctx 计算上下文
	 * @param resultList 输出值，用到的字段名会添加到这里面
	 */
	void getUsedFields(Context ctx, List<String> resultList);

	/**
	 * 查找表达式中用到单元格
	 * @param resultList 输出值，用到的单元格会添加到这里面
	 */
	void getUsedCells(List<INormalCell> resultList);

	/**
	 * 优化参数表达式，返回所有参数是否都是常数
	 * @param ctx 计算上下文
	 * @return boolean true：是，false：不是
	 */
	boolean optimize(Context ctx);

	/**
	 * 由start到end子节点创建一个新参数
	 * @param start 起始位置，包含
	 * @param end 结束位置，不包含
	 * @return IParam 新参数
	 */
	IParam create(int start, int end);

	/**
	 * 判断是否可以计算全部的值，有赋值运算时只能一行行计算
	 * @return
	 */
	boolean canCalculateAll();
	
	/**
	 * 重置表达式，用于表达式缓存，多次执行使用不同的上下文，清除跟上下文有关的缓存信息
	 */
	void reset();
}
