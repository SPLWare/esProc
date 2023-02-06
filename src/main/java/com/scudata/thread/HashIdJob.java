package com.scudata.thread;

import java.util.Comparator;

import com.scudata.dm.ListBase1;
import com.scudata.dm.Sequence;
import com.scudata.dm.comparator.BaseComparator;
import com.scudata.expression.fn.gather.ICount.ICountBitSet;
import com.scudata.expression.fn.gather.ICount.ICountPositionSet;
import com.scudata.util.HashUtil;

/**
 * 对序列进行哈希去重
 * @author LW
 *
 */
public class HashIdJob extends Job {
	private Sequence src; // 源序列
	private int start; // 起始位置，包括
	private int end; // 结束位置，不包括
	
	private String opt; // 选项
	
	private Sequence result; // 结果集
	
	public HashIdJob(Sequence src, int start, int end, String opt) {
		this.src = src;
		this.start = start;
		this.end = end;
		this.opt = opt;
	}
	
	public void run() {
		Sequence src = this.src;
		int start = this.start;
		int end = this.end;
		
		int len = end - start;
		this.result = new Sequence(len);
		if (len == 0) {
			return;
		}
		
		HashUtil hashUtil = new HashUtil(len / 2);
		Sequence out = this.result;
		
		if (opt != null && opt.indexOf('n') != -1) {
			ICountPositionSet set = new ICountPositionSet();
			for (int i = start; i < end; ++i) {
				Object item = src.getMem(i);
				if (item instanceof Number && set.add(((Number)item).intValue())) {
					out.add(item);
				}
			}
		} else if (opt != null && opt.indexOf('b') != -1) {
			ICountBitSet set = new ICountBitSet();
			for (int i = start; i < end; ++i) {
				Object item = src.getMem(i);
				if (item instanceof Number && set.add(((Number)item).intValue())) {
					out.add(item);
				}
			}
		} else {
			final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
			ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];

			for (int i = start; i < end; ++i) {
				Object item = src.getMem(i);
				int hash = hashUtil.hashCode(item);
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					groups[hash].add(item);
					out.add(item);
				} else {
					int index = groups[hash].binarySearch(item);
					if (index < 1) {
						groups[hash].add(-index, item);
						out.add(item);
					}
				}
			}
		}
		
		if (opt == null || opt.indexOf('u') == -1) {
			Comparator<Object> comparator = new BaseComparator();
			out.getMems().sort(comparator);
		}
	}

	public void getResult(Sequence table) {
		table.getMems().addAll(result.getMems());
	}
}
