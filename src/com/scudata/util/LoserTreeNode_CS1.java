package com.scudata.util;

import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.util.Variant;

/**
 * 归并字段数量为一个的游标节点
 * @author RunQian
 *
 */
public class LoserTreeNode_CS1 implements ILoserTreeNode {
	private ICursor cs;
	private int field;
	private boolean isNullMin = true; // null是否当最小值
	
	private Sequence data; // 缓存的数据
	private int seq; // -1表示数据已取完
	private Object value; // 当前记录对应的值，用于比较
	
	public LoserTreeNode_CS1(ICursor cs, int field, boolean isNullMin) {
		this.field = field;
		this.isNullMin = isNullMin;
		data = cs.fuzzyFetch(ICursor.FETCHCOUNT_M);
		if (data != null && data.length() > 0) {
			this.cs = cs;
			seq = 1;
			Record r = (Record)data.getMem(1);
			value = r.getNormalFieldValue(field);
		} else {
			this.cs = null;
			seq = -1;
			value = null;
		}
	}
	
	public Object popCurrent() {
		Object obj = data.getMem(seq);
		if (seq < data.length()) {
			Record r = (Record)data.getMem(++seq);
			value = r.getNormalFieldValue(field);
		} else {
			data = cs.fuzzyFetch(ICursor.FETCHCOUNT_M);
			if (data != null && data.length() > 0) {
				seq = 1;
				Record r = (Record)data.getMem(1);
				value = r.getNormalFieldValue(field);
			} else {
				seq = -1;
				cs = null;
				value = null;
			}
		}
		
		return obj;
	}
	
	public boolean hasNext() {
		return seq != -1;
	}
	
	public int compareTo(ILoserTreeNode other) {
		if (isNullMin) {
			return Variant.compare(value, ((LoserTreeNode_CS1)other).value, true);
		} else {
			return Variant.compare_0(value, ((LoserTreeNode_CS1)other).value);
		}
	}
}
