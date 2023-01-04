package com.scudata.util;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;

/**
 * 归并字段数量为多个的游标节点
 * @author RunQian
 *
 */
public class LoserTreeNode_CS implements ILoserTreeNode {
	private ICursor cs;
	private int []fields;
	private boolean isNullMin = true; // null是否当最小值
	
	private Sequence data; // 缓存的数据
	private int seq; // -1表示数据已取完
	private Object []values; // 当前记录对应的值，用于比较
	
	public LoserTreeNode_CS(ICursor cs, int []fields, boolean isNullMin) {
		this.fields = fields;
		this.isNullMin = isNullMin;
		
		data = cs.fuzzyFetch(ICursor.FETCHCOUNT_M);
		int fcount = fields.length;
		values = new Object[fcount];
		
		if (data != null && data.length() > 0) {
			this.cs = cs;
			seq = 1;
			BaseRecord r = (BaseRecord)data.getMem(1);
			for (int f = 0; f < fcount; ++f) {
				values[f] = r.getNormalFieldValue(fields[f]);
			}
		} else {
			this.cs = null;
			seq = -1;
			values = null;
		}
	}
	
	public Object popCurrent() {
		Object obj = data.getMem(seq);
		if (seq < data.length()) {
			BaseRecord r = (BaseRecord)data.getMem(++seq);
			int []fields = this.fields;
			for (int f = 0, fcount = fields.length; f < fcount; ++f) {
				values[f] = r.getNormalFieldValue(fields[f]);
			}
		} else {
			data = cs.fuzzyFetch(ICursor.FETCHCOUNT_M);
			if (data != null && data.length() > 0) {
				seq = 1;
				BaseRecord r = (BaseRecord)data.getMem(1);
				int []fields = this.fields;
				for (int f = 0, fcount = fields.length; f < fcount; ++f) {
					values[f] = r.getNormalFieldValue(fields[f]);
				}
			} else {
				seq = -1;
				cs = null;
			}
		}
		
		return obj;
	}
	
	public boolean hasNext() {
		return seq != -1;
	}
	
	public int compareTo(ILoserTreeNode other) {
		if (isNullMin) {
			return Variant.compareArrays(values, ((LoserTreeNode_CS)other).values);
		} else {
			return Variant.compareArrays_0(values, ((LoserTreeNode_CS)other).values);
		}
	}
}
