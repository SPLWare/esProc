package com.scudata.dw;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

public class NotFindFilter extends FindFilter {
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