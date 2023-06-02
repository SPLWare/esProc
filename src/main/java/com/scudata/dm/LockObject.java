package com.scudata.dm;

/**
 * 锁对象
 * @author WangXiaoJun
 *
 */
class LockObject implements IResource {
	private volatile Thread thread; // 当前锁定的线程
 
	public LockObject(Context ctx) {
	}
	
	public synchronized void close() {
		if (thread != null) { // Thread.currentThread()
			thread = null;
			notify();
		}
	}
	
	/**
	 * 锁定此对象
	 * @param ms 等待毫秒数，小于0表示永不超时
	 * @param ctx 计算上下文
	 * @return true：成功，false：失败
	 */
	public synchronized boolean lock(long ms, Context ctx) {
		Thread cur = Thread.currentThread();
		if (thread == null) {
			thread = cur;
			ctx.addResource(this);
			return true;
		} else if (thread == cur) {
			return false;
		} else {
			try {
				if (ms >= 0) {
					wait(ms); 
				} else {
					// 监视器所有者线程调用unlock后可能又进入了lock后此线程的wait才被唤醒
					do {
						wait();
					} while (thread != null);
				}
				
				thread = cur;
				ctx.addResource(this);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	
	/**
	 * 解锁
	 * @param ctx 计算上下文
	 * @return true：成功，false：失败
	 */
	public synchronized boolean unlock(Context ctx) {
		if (thread == null) {
			return true;
		} else if (thread != Thread.currentThread()) {
			return false;
		} else {
			thread = null;
			ctx.removeResource(this);
			notify();
			return true;
		}
	}
}