package com.scudata.dw;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Sequence;
import com.scudata.expression.Constant;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * switch过滤器
 * @author runqian
 *
 */
public class FindFilter extends IFilter {
	protected IndexTable it;
	protected Object findResult;
	protected Sequence code; 
	protected Expression right;
	protected int[] pos;
	
	public FindFilter(ColumnMetaData column, int priority, Sequence sequence, Node node) {
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
				it = sequence.newIndexTable();
			}
		}
		if (node != null) {
			exp = new Expression(node);
		}
		right = new Expression(column.getColName());
		code = sequence;
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
	
	public IArray getFindResultArray() {
		int[] pos = this.pos;
		int len = pos.length - 1;
		ObjectArray resultValue = new ObjectArray(len);
		for (int i = 1; i <= len; i++) {
			if (pos[i] != 0) {
				resultValue.push(code.getMem(pos[i]));
			} else {
				resultValue.push(null);
			}
		}
		return resultValue;
	}
	
	public boolean match(Object minValue, Object maxValue) {
		if (Variant.isEquals(minValue, maxValue)) {
			return it.find(minValue) != null;
		}
		
		return true;
	}
	
	public IArray calculateAll(Context ctx) {
		IArray key = right.calculateAll(ctx);
		int[] pos = it.findAllPos(key);
		int len = key.size();
		boolean[] result = new boolean[len + 1];
		for (int i = 1; i <= len; i++) {
			result[i] = pos[i] != 0;
		}
		
		this.pos = pos;
		BoolArray resultArray = new BoolArray(result, len);
		resultArray.setTemporary(true);
		return resultArray;
	}
	
	public IArray calculateAnd(Context ctx, IArray leftResult) {
		// isTrue返回的是临时的BollArray，可以直接修改
		BoolArray resultArray = leftResult.isTrue();
		IArray key = right.calculateAll(ctx);
		int[] pos = it.findAllPos(key, resultArray);
		
		int len = key.size();
		boolean[] resultDatas = resultArray.getDatas();
		for (int i = 1; i <= len; i++) {
			resultDatas[i] = pos[i] > 0;
		}
		
		this.pos = pos;
		return resultArray;
	}
	
	public int isValueRangeMatch(Context ctx) {
		return exp.isValueRangeMatch(ctx);
	}
	
	public void initExp() {
		if (exp == null) {
			String s = "null.find(" + column.getColName() + ")";
			exp = new Expression(s);
			
			exp.getHome().setLeft(new Constant(code));
		}
	}
}