package com.raqsoft.dm.op;

import com.raqsoft.dm.Context;

/**
 * 可以附加运算的接口
 * @author WangXiaoJun
 *
 */
public interface Operable {
	/**
	 * 附加运算
	 * @param op 运算
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	Operable addOperation(Operation op, Context ctx);
}