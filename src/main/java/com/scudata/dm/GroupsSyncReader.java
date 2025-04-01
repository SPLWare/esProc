package com.scudata.dm;

import java.util.LinkedList;
import java.util.Queue;

import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.RQException;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.thread.Job;
import com.scudata.thread.ThreadPool;
import com.scudata.util.HashUtil;

/**
 * 用于执行groups的任务同步从游标读取数据并计算hash
 * @author LW
 *
 */
public class GroupsSyncReader {
	private CursorReadJob readers[];
	private int tcount;
	private IntArray curTimes;//记录当前块被取了多少次
	private ObjectArray datas;//要取的数据
	private ThreadPool threadPool;
	private boolean close;
	
	//取数线程从游标里取到的数据，先缓存到这个队列
	private Queue<Object[]> readyDatas = new LinkedList<Object[]>();
	
	public GroupsSyncReader(ICursor[] cursors, Expression[] exps, HashUtil hashUtil, Context ctx) {
		datas = new ObjectArray(1024);
		curTimes = new IntArray(1024);
		
		int tcount = cursors.length;
		int fetchSize = ICursor.FETCHCOUNT * 10;
		int maxCacheSize = tcount * 2;
		
		CursorReadJob readers[] = new CursorReadJob[tcount];
		ThreadPool threadPool = ThreadPool.newSpecifiedInstance(tcount / 2);
		for (int i = 0; i < tcount; ++i) {
			Context tmpCtx = ctx.newComputeContext();
			Expression []tmpExps = Operation.dupExpressions(exps, tmpCtx);
			readers[i] = new CursorReadJob(cursors[i], fetchSize, tmpExps, hashUtil, tmpCtx, maxCacheSize, readyDatas);
			threadPool.submit(readers[i]);
		}
		this.readers = readers;
		this.tcount = tcount;
		this.threadPool = threadPool;
	}
	
	//等待线程完成取数。 运行到这里说明分组比取数快
	private void waitReadData() {
		if (close) {
			return;
		}
		
		while (readyDatas.size() == 0) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				throw new RQException(e);
			}
			CursorReadJob readers[] = this.readers;
			boolean allClosed = true;
			for (int i = 0, len = tcount; i < len; i++) {
				if (readers[i].isClosed()) continue;
				allClosed = false;
			}
			if (allClosed) {
				return;
			}
		}
	}
	
	//把缓存队列里的数据送到datas里
	private void loadData() {
		if (close) {
			return;
		}
		
		//如果缓存队列里没有数据
		if (readyDatas.size() == 0) {
			waitReadData();
			if (readyDatas.size() == 0) {
				close = true;
				threadPool.shutdown();
				return;
			}
		}
		
		//把readyDatas的所有数据送到datas里
		synchronized (readyDatas) {
			int size = readyDatas.size();
			if (size > 0) {
				while (size != 0) {
					datas.add(readyDatas.poll());
					curTimes.addInt(0);
					size--;
				}
			}
		}
	}
	
	/**
	 * 读取一块数据
	 * @param index 块号
	 * @return 数据格式:[读取的数据, 分组列数据, hash列数据]
	 */
	public synchronized Object[] getData(int index) {
		if (close && index > datas.size()) {
			return null;
		}
		
		while (index > datas.size() || datas.isNull(index)) {
			loadData();
			if (close) {
				return null;
			}
		}

		Object[] data = (Object[]) datas.get(index);//根据块号取数
		int[] curTimesData = curTimes.getDatas();//对应块号的计数器++
		curTimesData[index]++;
		
		//如果这一块被所有外部线程都取过了
		if (curTimesData[index] == tcount) {
			datas.set(index, null);//清除
			
			//从缓存取一块数据
			synchronized (readyDatas) {
				if (readyDatas.size() > 0) {
					datas.add(readyDatas.poll());
					curTimes.addInt(0);
				}
			}
		}
		return data;
	}
	
	public ICursor getCursor() {
		return readers[0].getCursor();
	}
}

class CursorReadJob extends Job {
	protected ICursor cursor; // 要取数的游标
	protected int fetchCount; // 每次读取的数据量
	protected boolean isClosed;
	
	protected Expression[] exps;// 计算hash的列
	protected int keyCount;
	protected Context ctx;
	protected HashUtil hashUtil;
	protected int []hashCodes; // 用于保存每个分组字段的哈希值
	
	protected Queue<Object[]> readyDatas;
	protected int maxCacheSize;
	/**
	 * 创建从游标取数的任务，使用getTable得到取数结果
	 * @param threadPool 线程池
	 * @param cursor 游标
	 * @param fetchCount 每次读取的数据量
	 */
	public CursorReadJob(ICursor cursor, int fetchCount, Expression[] exps, 
			HashUtil hashUtil, Context ctx, int maxCacheSize, Queue<Object[]> readyDatas) {
		this.cursor = cursor;
		this.fetchCount = fetchCount;
		
		this.hashUtil = hashUtil;
		this.ctx = ctx;
		this.exps = exps;
		keyCount = exps.length;
		hashCodes = new int[keyCount];
		
		this.maxCacheSize = maxCacheSize;
		this.readyDatas = readyDatas;
	}

	/**
	 * 被线程池里的线程调用，从游标读取数据
	 */
	public void run() {
		HashUtil hashUtil = this.hashUtil;
		ICursor cursor = this.cursor;
		Context ctx = this.ctx;
		Expression[] exps = this.exps;
		int keyCount = this.keyCount;
		
		while (true) {
			Sequence table = cursor.fuzzyFetch(fetchCount);
			if (table == null) {
				isClosed = true;
				return;
			}
			
			Object[] data = new Object[3];
			data[0] = table;
			
			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(table);
			stack.push(current);
			
			try {
				int len = table.length();
				int[] hash = new int[len + 1];
				if (keyCount == 1) {
					IArray array = exps[0].calculateAll(ctx);

					for (int i = 1; i <= len; ++i) {
						hash[i] = hashUtil.hashCode(array.hashCode(i));
					}
					data[1] = array;
				} else {
					IArray[] arrays = new IArray[keyCount];
					int[] hashCodes = this.hashCodes;

					for (int k = 0; k < keyCount; ++k) {
						arrays[k] = exps[k].calculateAll(ctx);
					}

					for (int i = 1; i <= len; ++i) {
						for (int k = 0; k < keyCount; ++k) {
							hashCodes[k] = arrays[k].hashCode(i);
						}

						hash[i] = hashUtil.hashCode(hashCodes, keyCount);
					}

					data[1] = arrays;
				}
				data[2] = new IntArray(hash, null, len);
			} finally {
				stack.pop();
			}
			
			while (readyDatas.size() > maxCacheSize) {
				//只有当取数比分组快时会跑进来
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					throw new RQException(e);
				}
			}
			synchronized (readyDatas) {
				readyDatas.add(data);
			}
		}
	}
	
	public ICursor getCursor() {
		return cursor;
	}
	
	public boolean isClosed() {
		return isClosed;
	}
}
