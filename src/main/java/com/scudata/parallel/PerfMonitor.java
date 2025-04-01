package com.scudata.parallel;

import com.scudata.common.Logger;

public class PerfMonitor {
//分进程锁，根据最大作业/适合作业数调度,也即定义的倍数	
	private static Object LOCK1 = new Object();
	private static volatile int concurrents = 0;

//主进程锁，根据最大作业数调度
	private static Object LOCKPROCESS = new Object();
	private static volatile int processConcurrents = 0;

	static HostManager hm = HostManager.instance();
	
	public static void enterProcess() {
		synchronized (LOCKPROCESS) {
			int maxNum = hm.getMaxTaskNum();
			if (processConcurrents >= maxNum) {
				try {
					LOCKPROCESS.wait();
				} catch (InterruptedException e) {
				}
			}
			processConcurrents++;
		}
	}

	public static void leaveProcess() {
		synchronized (LOCKPROCESS) {
			processConcurrents--;
			LOCKPROCESS.notify();
		}
	}

	/**
	 * 获取分机上正在运行的并发任务数目，远程分区冗余文件需要挑选任务最少的分机
	 * @return
	 */
	public static int getConcurrentTasks() {
		return processConcurrents;
	}

	/**
	 * 进入任务，处理并发数及等待数
	 */
	public static void enterTask(Object mark) {
		synchronized (LOCK1) {
			int maxNum = hm.getMaxTaskNum();
			
			if (concurrents >= maxNum) {
				try {
					LOCK1.wait();
				} catch (InterruptedException e) {
				}
			}
			concurrents++;
			if(mark!=null){
				Logger.debug(mark);
			}
		}
	}

	public static void leaveTask(Object mark,String suffix) {
		synchronized (LOCK1) {
			concurrents--;
			LOCK1.notify();
			if(mark!=null){
				Logger.debug(suffix+" "+mark);
			}
		}
	}

}
