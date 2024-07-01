package com.scudata.expression;

import java.util.List;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.cellset.INormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.Env;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 表达式节点基类
 * @author WangXiaoJun
 *
 */
public abstract class Node {
	// 优先级常量定义，数越小优先级越低
	public static final byte PRI_CMA = (byte) 1; //逗号运算符
	public static final byte PRI_EVL = (byte) 2; //赋值
	//public static final byte PRI_ASS = (byte) 3;
	//public static final byte PRI_CON = (byte) 4;
	public static final byte PRI_LINK = (byte) 4;
	public static final byte PRI_OR = (byte) 5;
	public static final byte PRI_AND = (byte) 6;
	public static final byte PRI_BOR = (byte) 7;
	public static final byte PRI_BXOR = (byte) 8;
	public static final byte PRI_BAND = (byte) 9;
	public static final byte PRI_IN = (byte) 10;
	public static final byte PRI_EQ = (byte) 10;
	public static final byte PRI_NEQ = (byte) 10;
	public static final byte PRI_GT = (byte) 11;
	public static final byte PRI_SL = (byte) 11;
	public static final byte PRI_NGT = (byte) 11;
	public static final byte PRI_NSL = (byte) 11;
	//public static final byte PRI_SHIFT = (byte) 12;
	public static final byte PRI_ADD = (byte) 13;
	public static final byte PRI_SUB = (byte) 13;
	public static final byte PRI_MUL = (byte) 14;
	public static final byte PRI_DIV = (byte) 14;
	public static final byte PRI_MOD = (byte) 14;
	//public static final byte PRI_NEW = (byte) 15;
	public static final byte PRI_NOT = (byte) 16;
	//public static final byte PRI_ADR = (byte) 16; //address
	//public static final byte PRI_PRF = (byte) 17;
	public static final byte PRI_NEGT = (byte) 17; //-
	public static final byte PRI_PLUS = (byte) 17; //+
	public static final byte PRI_SUF = (byte) 18; // A1(n), A.fn() r.f
	public static final byte PRI_NUM = (byte) 19; // 标识符、常数等
	public static final byte PRI_BRK = (byte) 20; // 括号

	protected int priority; // 优先级

	/**
	 * 构造节点
	 */
	public Node() {
		priority = PRI_NUM;
	}

	/**
	 * 当前节点在括号内，给节点的优先级加上括号的优先级
	 * @param inBrackets 括号层数
	 */
	public void setInBrackets(int inBrackets) {
		this.priority += inBrackets * PRI_BRK;
	}

	/**
	 * 取得当前节点的优先级
	 * @return 优先级
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * 设置节点的左侧节点
	 * @param node 节点
	 */
	public void setLeft(Node node) {
		if (node != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.logicError"));
		}
	}

	/**
	 * 设置节点的右侧节点
	 * @param node 节点
	 */
	public void setRight(Node node) {
		if (node != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.logicError"));
		}
	}

	/**
	 * 取节点的左侧节点，没有返回空
	 * @return Node
	 */
	public Node getLeft() {
		return null;
	}

	/**
	 * 取节点的右侧节点，没有返回空
	 * @return Node
	 */
	public Node getRight() {
		return null;
	}

	/**
	 * 把点操作符的左侧对象设给右侧成员函数
	 * @param obj 左侧对象
	 */
	public void setDotLeftObject(Object obj) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Expression.logicError"));
	}

	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
	}
	
	/**
	 * 判断当前节点是否是序列函数
	 * 如果点操作符的右侧节点是序列函数，左侧节点计算出数，则需要把数转成数列
	 * @return
	 */
	public boolean isSequenceFunction() {
		return false;
	}
	
	/**
	 * 用于判断点操作符右面的函数是否和左面对象的类型匹配
	 * @param obj 左面对象
	 * @return true：右面的节点跟左面对象的类型匹配，是其成员或成员函数，false：不匹配
	 */
	public boolean isLeftTypeMatch(Object obj) {
		return true;
	}
	
	/**
	 * 取与当前成员函数同名的下一个同名函数，没有则返回空
	 * @return 下一个同名的成员函数
	 */
	public MemberFunction getNextFunction() {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Expression.logicError"));
	}
	
	/**
	 * 计算节点的值
	 * @param ctx 计算上下文
	 * @return Object
	 */
	public abstract Object calculate(Context ctx);

	/**
	 * 计算出引用的单元格，不是取单元格的值，如果表达式不是单元格引用则返回空
	 * @param ctx 计算上下文
	 * @return INormalCell
	 */
	public INormalCell calculateCell(Context ctx) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("engine.needCellExp"));
	}

	/**
	 * 返回是否包含指定参数
	 * @param name 参数名
	 * @return boolean true：包含，false：不包含
	 */
	protected boolean containParam(String name) {
		return false;
	}

	/**
	 * 查找表达式中用到参数
	 * @param ctx 计算上下文
	 * @param resultList 输出值，用到的参数会添加到这里面
	 */
	protected void getUsedParams(Context ctx, ParamList resultList) {
	}
	
	/**
	 * 查找表达式中可能用到的字段，可能取得不准确或者包含多个表的
	 * @param ctx 计算上下文
	 * @param resultList 输出值，用到的字段名会添加到这里面
	 */
	public void getUsedFields(Context ctx, List<String> resultList) {
	}
	
	/**
	 * 查找表达式中用到单元格
	 * @param resultList 输出值，用到的单元格会添加到这里面
	 */
	protected void getUsedCells(List<INormalCell> resultList) {
	}
	
	/**
	 * 判断节点是否会修改序列的成员值，此方法为了优化[1,2,3].contain(...)这种表达式，
	 * 如果序列不会被更改则[1,2,3]可以被产生成常数序列，而不是每次计算都产生一个序列
	 * @return true：会修改，false：不会修改
	 */
	public boolean ifModifySequence() {
		return true;
	}
	
	/**
	 * 对节点做优化
	 * @param ctx 计算上下文
	 * @param optSequence 是否优化常数序列比如[1,2,3].contain(...)，true：优化
	 * @return 优化后的节点
	 */
	public Node optimize(Context ctx, boolean optSequence) {
		return optimize(ctx);
	}

	/**
	 * 对节点做优化，常数表达式先算成常数
	 * @param ctx 计算上下文
	 * @param Node 优化后的节点
	 */
	public Node optimize(Context ctx) {
		return this;
	}
	
	/**
	 * 对节点做深度优化（包括单元格和参数引用），常数表达式先算成常数
	 * @param ctx 计算上下文
	 * @param Node 优化后的节点
	 */
	public Node deepOptimize(Context ctx) {
		return optimize(ctx);
	}

	/**
	 * 对当前节点进行赋值
	 * @param value 右侧值
	 * @param ctx 计算上下文
	 * @return 右侧值
	 */
	public Object assign(Object value, Context ctx) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("assign.needVar"));
	}
	
	/**
	 * 对当前节点做+=运算
	 * @param value 右侧值
	 * @param ctx 计算上下文
	 * @return Object 运算结果
	 */
	public Object addAssign(Object value, Context ctx) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("assign.needVar"));
	}

	/**
	 * 返回节点的返回值类型
	 * @param ctx 计算上下文
	 * @return byte 类型定义在Expression中
	 */
	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_OTHER;
	}
	
	/**
	 * 用于取偏移，形如A[-1]，F[-1]这种取上一个的运算
	 * @param node 右侧Move节点
	 * @param ctx 计算上下文
	 * @return Object
	 */
	public Object move(Move node, Context ctx) {
		Object obj = calculate(ctx);
		if (!(obj instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("dot.seriesLeft"));
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = stack.getSequenceCurrent((Sequence)obj);
		if (current == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("engine.seriesNotInStack"));
		}

		int index = node.calculateIndex(current, ctx);
		return index > 0 ? current.get(index) : null;
	}

	/**
	 * 用于对偏移对象进行赋值，形如A[-1]=x，F[-1]=x这种赋值运算
	 * @param node 右侧Move节点
	 * @param value 右侧值
	 * @param ctx 计算上下文
	 * @return 右侧值
	 */
	public Object moveAssign(Move node, Object value, Context ctx) {
		Object obj = calculate(ctx);
		if (!(obj instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("dot.seriesLeft"));
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = stack.getSequenceCurrent((Sequence)obj);
		if (current == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("engine.seriesNotInStack"));
		}

		int index = node.calculateIndex(current, ctx);
		if (index > 0) current.assign(index, value);
		return value;
	}

	/**
	 * 用于取范围偏移，形如A[-1:1]，F[-1:1]这种取上一个到下一个之间的元素的运算
	 * @param node 右侧Move节点
	 * @param ctx 计算上下文
	 * @return Object 结果集序列
	 */
	public Object moves(Move node, Context ctx) {
		Object obj = calculate(ctx);
		if (!(obj instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("dot.seriesLeft"));
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = stack.getSequenceCurrent((Sequence)obj);
		if (current == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("engine.seriesNotInStack"));
		}

		// 计算结果集范围
		int []range = node.calculateIndexRange(current, ctx);
		if (range == null) {
			return new Sequence(0);
		}

		int startSeq = range[0];
		int endSeq = range[1];
		Sequence result = new Sequence(endSeq - startSeq + 1);
		for (; startSeq <= endSeq; ++startSeq) {
			result.add(current.get(startSeq));
		}

		return result;
	}
	
	/**
	 * 判断节点是否是指定函数
	 * @param name 函数名
	 * @return true：是指定函数，false：不是
	 */
	public boolean isFunction(String name) {
		return false;
	}

	/*--------以下函数为了实现groups里的汇总函数可以是非聚合表达式--------*/
	
	/**
	 * 做分组运算前准备工作
	 * @param ctx 计算上下文
	 */
	public void prepare(Context ctx) {
	}
	
	/**
	 * 计算新组首条记录的汇总值
	 * @param ctx 计算上下文
	 * @return 汇总值
	 */
	public Object gather(Context ctx) {
		return calculate(ctx);
	}
	
	/**
	 * 计算当前记录的值，汇总到之前的汇总结果oldValue上
	 * @param oldValue 之前的汇总结果
	 * @param ctx 计算上下文
	 * @return 汇总值
	 */
	public Object gather(Object oldValue, Context ctx) {
		return oldValue;
	}
	
	/**
	 * 计算所有记录的值，汇总到结果数组上
	 * @param result 结果数组
	 * @param resultSeqs 每条记录对应的结果数组的序号
	 * @param ctx 计算上下文
	 * @return IArray 结果数组
	 */
	public IArray gather(IArray result, int []resultSeqs, Context ctx) {
		IArray array = calculateAll(ctx);
		if (result == null) {
			result = array.newInstance(Env.INITGROUPSIZE);
		}
		
		for (int i = 1, len = array.size(); i <= len; ++i) {
			// 新产生的组取第一条记录的值，已经产生的组则不再计算
			if (result.size() < resultSeqs[i]) {
				result.add(array, i);
			}
		}
		
		return result;
	}

	/**
	 * 多程程分组的二次汇总运算
	 * @param result 一个线程的分组结果
	 * @param result2 另一个线程的分组结果
	 * @param seqs 另一个线程的分组跟第一个线程分组的对应关系
	 * @param ctx 计算上下文
	 * @return
	 */
	public void gather2(IArray result, IArray result2, int []seqs, Context ctx) {
	}
	
	/**
	 * 取二次汇总对应的表达式
	 * 多线程分组时，每个线程算出一个分组结果，最后需要在第一次分组结果上再做二次分组
	 * @param q 汇总字段序号
	 * @return Expression
	 */
	public Expression getRegatherExpression(int q) {
		String str = "#" + q;
		return new Expression(str);
	}
	
	/**
	 * 第一步分组结束时是否需要调用finish1对汇总值进行首次处理，top需要调用
	 * @return true：需要，false：不需要
	 */
	public boolean needFinish1() {
		return false;
	}
	
	/**
	 * 对第一次分组得到的汇总值进行首次处理，处理后的值还要参加二次分组运算
	 * @param val 汇总值
	 * @return 处理后的汇总值
	 */
	public Object finish1(Object val) {
		return val;
	}
	
	/**
	 * 对第一次分组得到的汇总列进行首次处理，处理后的值还要参加二次分组运算
	 * @param array 计算列的值
	 * @return IArray
	 */
	public IArray finish1(IArray array) {
		return array;
	}
	
	/**
	 * 是否需要对最终汇总值进行处理
	 * @return true：需要，false：不需要
	 */
	public boolean needFinish() {
		return false;
	}
	
	/**
	 * 对分组结束得到的汇总值进行最终处理，像平均值需要做sum/count处理
	 * @param val 汇总值
	 * @return 处理后的汇总值
	 */
	public Object finish(Object val) {
		return val;
	}
	
	/**
	 * 对分组结束得到的汇总列进行最终处理
	 * @param array 计算列的值
	 * @return IArray
	 */
	public IArray finish(IArray array) {
		return array;
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		// 派生类继承此方法检查语法是否有效
	}
	
	/**
	 * 判断是否可以计算全部的值，有赋值运算时只能一行行计算
	 * @return
	 */
	public boolean canCalculateAll() {
		return true;
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		Current current = ctx.getComputeStack().getTopCurrent();
		int len = current.length();
		ObjectArray array = new ObjectArray(len);
		array.setTemporary(true);
		
		for (int i = 1; i <= len; ++i) {
			current.setCurrent(i);
			Object value = calculate(ctx);
			array.push(value);
		}
		
		return array;
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		Current current = ctx.getComputeStack().getTopCurrent();
		int len = current.length();
		ObjectArray array = new ObjectArray(len);
		array.setTemporary(true);
		
		for (int i = 1; i <= len; ++i) {
			if (signArray.isTrue(i) == sign) {
				current.setCurrent(i);
				Object value = calculate(ctx);
				array.push(value);
			} else {
				array.push(null);
			}
		}
		
		return array;
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		BoolArray result = leftResult.isTrue();
		int size = result.size();
		Current current = ctx.getComputeStack().getTopCurrent();
		
		for (int i = 1; i <= size; ++i) {
			if (result.isTrue(i)) {
				current.setCurrent(i);
				Object value = calculate(ctx);
				if (Variant.isFalse(value)) {
					result.set(i, false);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		return false;
	}
	
	/**
	 * 计算表达式的取值范围
	 * @param ctx 计算上行文
	 * @return
	 */
	public IArray calculateRange(Context ctx) {
		if (isMonotone()) {
			return calculateAll(ctx);
		} else {
			return null;
		}
	}
	
	/**
	 * 判断给定的值域范围是否满足当前条件表达式
	 * @param ctx 计算上行文
	 * @return 取值参照Relation. -1：值域范围内没有满足条件的值，0：值域范围内有满足条件的值，1：值域范围的值都满足条件
	 */
	public int isValueRangeMatch(Context ctx) {
		return Relation.PARTICALMATCH;
	}
}
