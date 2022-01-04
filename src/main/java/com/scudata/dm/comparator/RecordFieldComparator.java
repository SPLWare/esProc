package com.scudata.dm.comparator;

import java.util.Comparator;

import com.scudata.dm.Record;

/**
 * 按照记录指定字段进行比较的比较器
 * @author WangXiaoJun
 *
 */
public class RecordFieldComparator implements Comparator<Object> {
	private int []fieldIndex; // 记录字段序号
	
	public RecordFieldComparator(int[] fieldIndex) {
		this.fieldIndex = fieldIndex;
	}

	public int compare(Object o1, Object o2) {
		if (o1 == null) {
			return (o2 == null) ? 0 : -1;
		}
		return ((Record)o1).compare((Record)o2, fieldIndex);
	}
}
