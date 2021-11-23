package com.scudata.dm.op;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;

/**
 * 游标和管道立即计算函数基类，返回游标或管道最终的计算结果
 * @author RunQian
 *
 */
public interface IResult {
	// 多个线程可能同时往不同的管道push，需要使用线程自己的ctx，否则同时计算的时候可能出错
	
	/**
	 * 处理推送过来的数据，累积到最终的结果上
	 * @param seq 数据
	 * @param ctx 计算上下文
	 */
	public void push(Sequence seq, Context ctx);
	
	/**
	 * 数据推送结束，取最终的计算结果
	 * @return
	 */
	public Object result();
	
	/**
	 * 集群管道最后需要把各节点机的返回结果再合并一下
	 * @param results 每个节点机的计算结果
	 * @return 合并后的结果
	 */
	public Object combineResult(Object []results);
}