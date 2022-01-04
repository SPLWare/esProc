package com.scudata.dm;

import java.util.HashMap;

/**
 * 锁管理器，用于lock(n,s)函数
 * @author WangXiaoJun
 *
 */
public final class LockManager {
	private static HashMap<Object, LockObject> lockMap = new HashMap<Object, LockObject>();
	
	/**
	 * 锁定给定值
	 * @param key 锁值
	 * @param ms 等待毫秒数，小于0表示永不超时
	 * @param ctx 计算上下文
	 * @return 如果成功返回锁值，失败返回0
	 */
	public final static Object lock(Object key, long ms, Context ctx) {
		LockObject lock;
		synchronized(lockMap) {
			lock = lockMap.get(key);
			if (lock == null) {
				lock = new LockObject(ctx);
				lockMap.put(key, lock);
			}
		}
		
		if (lock.lock(ms, ctx)) {
			return key;
		} else {
			return 0;
		}
	}
	
	/**
	 * 解锁给定值
	 * @param key 锁值
	 * @param ctx
	 * @return true：成功，false：失败
	 */
	public final static boolean unLock(Object key, Context ctx) {
		LockObject lock;
		synchronized(lockMap) {
			lock = lockMap.get(key);
			if (lock == null) {
				return false;
			}
		}
		
		return lock.unlock(ctx);
	}
}