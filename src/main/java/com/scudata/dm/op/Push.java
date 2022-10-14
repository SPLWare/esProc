package com.scudata.dm.op;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;

/**
 * 游标或管道的附加的推送当前数据到指定管道计算处理类
 * cs.push(ch)
 * @author RunQian
 *
 */
public class Push extends Operation {
	private Channel channel;
	private boolean doFinish = true; // 用于防止多路管道finish的循环调用

	public Push(Channel channel) {
		this.channel = channel;
	}
	
	public Push(Channel channel, boolean doFinish) {
		this.channel = channel;
		this.doFinish = doFinish;
	}
	
	public Push(Function function, Channel channel) {
		super(function);
		this.channel = channel;
	}
	
	/**
	 * 数据全部推送完成时调用，group运算需要知道数据结束来确认最后一组的数据
	 * @param ctx 计算上下文
	 * @return 附加的操作缓存的数据
	 */
	public Sequence finish(Context ctx) {
		if (doFinish) {
			channel.finish(ctx);
		}
		
		return null;
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		return new Push(function, channel);
	}
	
	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		channel.push(seq, ctx);
		return seq;
	}
}
