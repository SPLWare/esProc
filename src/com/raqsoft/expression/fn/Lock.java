package com.raqsoft.expression.fn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.LockManager;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/**
 * 加同步锁
 * lock(n,s)
 * 防止多个线程同时访问文件，加锁以后仅限此线程访问和执行，访问执行完成以后解锁，其他进程才可以继续访问执行。
 * @author runqian
 *
 */
public class Lock extends Function {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("lock" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object key = param.getLeafExpression().calculate(ctx);
			if (option == null || option.indexOf('u') == -1) {
				return LockManager.lock(key, -1, ctx);
			} else {
				return LockManager.unLock(key, ctx);
			}
		} else if (param.getSubSize() == 2) {
			if (option != null && option.indexOf('u') != -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("lock" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("lock" + mm.getMessage("function.invalidParam"));
			}
			
			Object key = sub0.getLeafExpression().calculate(ctx);
			if (key == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("lock" + mm.getMessage("function.invalidParam"));
			}
			
			Object ms = sub1.getLeafExpression().calculate(ctx);
			if (ms instanceof Number) {
				return LockManager.lock(key, ((Number)ms).longValue(), ctx);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("lock" + mm.getMessage("function.paramTypeError"));
			}
			
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("lock" + mm.getMessage("function.invalidParam"));
		}
	}
}
