package com.raqsoft.dm.op;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;

/**
 * 管道接口
 * @author RunQian
 *
 */
public interface IPipe {
	/**
	 * 往管道推送数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 */
	void push(Sequence seq, Context ctx);
	
	/**
	 * 数据推送结束
	 * @param ctx
	 */
	void finish(Context ctx);
}