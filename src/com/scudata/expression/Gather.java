package com.scudata.expression;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;

/**
 * 分组运算的汇总函数继承此类
 * @author RunQian
 *
 */
abstract public class Gather extends Function {
	/**
	 * 对节点做优化，常数表达式先算成常数
	 * @param ctx 计算上下文
	 * @param Node 优化后的节点
	 */
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	/**
	 * 做分组运算前准备工作
	 * @param ctx 计算上下文
	 */
	abstract public void prepare(Context ctx);

	/**
	 * 计算新组首条记录的汇总值
	 * @param ctx 计算上下文
	 * @return 汇总值
	 */
	abstract public Object gather(Context ctx);

	/**
	 * 计算当前记录的值，汇总到之前的汇总结果oldValue上
	 * @param oldValue 之前的汇总结果
	 * @param ctx 计算上下文
	 * @return 汇总值
	 */
	abstract public Object gather(Object oldValue, Context ctx);
	
	/**
	 * 取二次汇总对应的表达式
	 * 多线程分组时，每个线程算出一个分组结果，最后需要在第一次分组结果上再做二次分组
	 * @param q 汇总字段序号
	 * @return Expression
	 */
	abstract public Expression getRegatherExpression(int q);
	
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
	 * 针对给定序列算出汇总值
	 * @param seq 序列
	 * @return 汇总值
	 */
	public Object gather(Sequence seq) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getFunctionName() + mm.getMessage("engine.unknownGroupsMethod"));
	}
}
