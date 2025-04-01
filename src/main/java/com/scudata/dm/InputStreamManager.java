package com.scudata.dm;

import java.util.LinkedList;

/**
 * 用于对按块读入的输入流做串行管理
 * @author WangXiaoJun
 *
 */
public final class InputStreamManager extends Thread {
	private static InputStreamManager manager;
	
	// 在等待读取数据的输入流
	private LinkedList <BlockInputStream>bisList = new LinkedList<BlockInputStream>();

	private InputStreamManager(ThreadGroup group) {
		super(group, "InputStreamManager");
	}

	/**
	 * 取输入流管理对象
	 * @return InputStreamManager
	 */
	public synchronized static InputStreamManager getInstance() {
		if (manager == null) { // || !manager.isAlive()
			ThreadGroup group = currentThread().getThreadGroup();
			while (true) {
				ThreadGroup g = group.getParent();
				if (g == null) {
					break;
				} else {
					group = g;
				}
			}

			manager = new InputStreamManager(group);
			manager.setDaemon(true);
			manager.start();
		}

		return manager;
	}

	/**
	 * 把输入流加入队列中读取一块数据
	 * @param is
	 */
	public void read(BlockInputStream is) {
		synchronized(bisList) {
			bisList.add(is);
			bisList.notify();
		}
	}

	/**
	 * 读数据函数
	 */
	public void run() {
		while (true) {
			synchronized(bisList) {
				if (bisList.size() == 0) {
					try {
						// 等待输入流调用read
						bisList.wait();
					} catch (InterruptedException e) {
					}
				}
			}

			// 循环输入流队列，直到所有的输入流都读取完数据
			while (true) {
				BlockInputStream bis;
				synchronized(bisList) {
					if (bisList.size() == 0) {
						break;
					}
					
					bis = bisList.removeFirst();
				}

				bis.fillBuffers();
			}
		}
	}
}
