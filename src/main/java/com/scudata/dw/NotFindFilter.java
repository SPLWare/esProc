package com.scudata.dw;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

public class NotFindFilter extends FindFilter {
	public NotFindFilter(ColumnMetaData column, int priority, Sequence sequence, Node node) {
		super(column, priority);
		it = sequence.getIndexTable();
		
		if (it == null) {
			Object obj = sequence.ifn();
			if (obj instanceof BaseRecord) {
				DataStruct ds = ((BaseRecord)obj).dataStruct();
				int []fields = ds.getPKIndex();
				if (fields == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("ds.lessKey"));
				} else if (fields.length != 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.keyValCountNotMatch"));
				}
				
				it = sequence.newIndexTable(fields, sequence.length(), null);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPmt"));
			}
		}
				if (node != null) exp = new Expression(node);
		if (node != null) {
			exp = new Expression(node);
		}
		right = new Expression(column.getColName());
		code = sequence;
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
	

	public IArray calculateAll(Context ctx) {
		IArray key = right.calculateAll(ctx);
		int[] pos = it.findAllPos(key);
		int len = key.size();
		boolean[] result = new boolean[len + 1];
		for (int i = 1; i <= len; i++) {
			result[i] = pos[i] == 0;
		}
		
		this.pos = pos;
		BoolArray resultArray = new BoolArray(result, len);
		resultArray.setTemporary(true);
		return resultArray;
	}
	
	public IArray calculateAnd(Context ctx, IArray leftResult) {
		BoolArray left = leftResult.isTrue();
		IArray key = right.calculateAll(ctx);
		int[] pos = it.findAllPos(key, left);
		int len = key.size();
		boolean[] resultDatas = left.getDatas();
		for (int i = 1; i <= len; i++) {
			if (resultDatas[i])
				resultDatas[i] = pos[i] == 0;
		}
		
		this.pos = pos;
		BoolArray resultArray = new BoolArray(resultDatas, len);
		resultArray.setTemporary(true);
		return resultArray;
	}
	
	public int isValueRangeMatch(Context ctx) {
		return exp.isValueRangeMatch(ctx);
	}
	
	public void initExp() {
		if (exp == null) {
			throw new RuntimeException();
		}
	}
}