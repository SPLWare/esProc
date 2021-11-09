package com.raqsoft.dw;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.IndexTable;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

class NotFindFilter extends FindFilter {
	public NotFindFilter(ColumnMetaData column, int priority, Sequence sequence) {
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
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPmt"));
			}
		}
	}
	
	public NotFindFilter(String columnName, int priority, Sequence sequence) {
		super(columnName, priority);
		it = sequence.getIndexTable();
	}
	
	public boolean match(Object value) {
		findResult = value;
		return it.find(value) == null;
	}
	
	public boolean match(Object minValue, Object maxValue) {
		if (Variant.isEquals(minValue, maxValue)) {
			return it.find(minValue) == null;
		}
		
		return true;
	}
}