package com.raqsoft.dw;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.IndexTable;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * switch¹ýÂËÆ÷
 * @author runqian
 *
 */
public class FindFilter extends IFilter {
	protected IndexTable it;
	protected Object findResult;

	public FindFilter(ColumnMetaData column, int priority, Sequence sequence) {
		super(column, priority);
		it = sequence.getIndexTable();
		
		if (it == null) {
			Object obj = sequence.ifn();
			if (obj instanceof Record) {
				DataStruct ds = ((Record)obj).dataStruct();
				int []fields = ds.getPKIndex();
				if (fields == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("ds.lessKey"));
				} else if (fields.length != 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.keyValCountNotMatch"));
				}
				
				it = IndexTable.instance(sequence, fields, sequence.length(), null);
			} else {
				it = IndexTable.instance(sequence);
			}
		}
	}
	
	public FindFilter(String columnName, int priority, Sequence sequence) {
		this.columnName = columnName;
		this.priority = priority;
		it = sequence.getIndexTable();
	}
	
	public FindFilter(ColumnMetaData column, int priority) {
		this.column = column;
		this.priority = priority;
	}

	public FindFilter(String columnName, int priority) {
		this.columnName = columnName;
		this.priority = priority;
	}
	
	public boolean match(Object value) {
		return (findResult = it.find(value)) != null;
	}
	
	public Object getFindResult() {
		return findResult;
	}
	
	public boolean match(Object minValue, Object maxValue) {
		if (Variant.isEquals(minValue, maxValue)) {
			return it.find(minValue) != null;
		}
		
		return true;
	}
}