package com.scudata.dm.cursor;

import java.util.ArrayList;

import com.scudata.dm.Sequence;
import com.scudata.dm.op.Operation;

/**
 * 用内存序列构建游标
 * A.cursor() A.cursor(k:n)
 * @author RunQian
 *
 */
public class MemoryCursor extends ICursor {
	private Sequence data; // 序列
	private int startSeq; // 起始位置，包含
	private int endSeq; // 结束位置，不包含
	private int next = 1; // 下一条记录的序号
	
	/**
	 * 构建内存游标
	 * @param seq 源序列
	 */
	public MemoryCursor(Sequence seq) {
		if (seq != null) {
			if (seq.length() > 0) {
				data = seq;
				next = 1;
				startSeq = 1;
				endSeq = seq.length();
			} else {
				setDataStruct(seq.dataStruct());
			}
		}
	}

	/**
	 * 按指定区间构建内存游标
	 * @param seq 源序列
	 * @param start 起始位置，包含
	 * @param end 结束位置，不包含
	 */
	public MemoryCursor(Sequence seq, int start, int end) {
		if (seq != null) {
			if (seq.length() > 0 && start < end) {
				int len = seq.length();
				if (start <= len) {
					data = seq;
					next = start;
					startSeq = start;
					endSeq = end - 1;
					if (endSeq > len) {
						endSeq = len;
					}
				}
			} else {
				setDataStruct(seq.dataStruct());
			}
		}
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		int rest = endSeq - next + 1;
		if (rest < 1 || n < 1) {
			return null;
		}
		
		Sequence data = this.data;
		if (rest >= n) {
			int end = next + n;
			Sequence table = new Sequence(n);
			for (int i = next; i < end; ++i) {
				table.add(data.getMem(i));
			}

			this.next = end;
			return table;
		} else {
			if (next == 1 && endSeq == data.length()) {
				next = endSeq + 1;
				//return data;
				
				// 外面可能会修改data，所以需要产生新对象
				return new Sequence(data);
			}
			
			Sequence table = new Sequence(rest);
			for (int i = next, end = endSeq; i <= end; ++i) {
				table.add(data.getMem(i));
			}

			next = endSeq + 1;
			return table;
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		int rest = endSeq - next + 1;
		if (rest < 1 || n < 1) {
			return 0;
		}

		if (rest > n) {
			this.next += n;
			return n;
		} else {
			next = endSeq + 1;
			return rest;
		}
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		next = endSeq + 1;
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		next = startSeq;
		return true;
	}
	
	public Sequence fuzzyFetch(int n) {
		return fetch();
	}

	/**
	 * 返回剩余的记录并关闭游标
	 * @return Sequence
	 */
	public Sequence fetch() {
		int rest = endSeq - next + 1;
		if (rest < 1) {
			Sequence result = cache;
			close();
			return result;
		}
		
		Sequence result;
		if (next == 1 && endSeq == data.length()) {
			result = data;
		} else {
			result = new Sequence(rest);
			for (int i = next, end = endSeq; i <= end; ++i) {
				result.add(data.getMem(i));
			}
		}
				
		ArrayList<Operation> opList = this.opList;
		if (opList != null) {
			result = doOperation(result, opList, ctx);
			Sequence tmp = finish(opList, ctx);
			if (tmp != null) {
				if (result == null) {
					result = tmp;
				} else {
					result = append(result, tmp);
				}
			}
		}
		
		if (cache != null) {
			result = append(cache, result);
		}
		
		close();
		return result;
	}
}
