package com.scudata.dw;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Relation;

/**
 * 用于跳块的filter，在pjoin时用到
 * @author LW
 *
 */
public class BlockFilter extends IFilter {
	
	private IArray startValues;
	private IArray endValues;
	private int size;
	
	public BlockFilter(ColumnMetaData column, IArray[] values) {
		super(column, 0);
		this.startValues = values[0];
		this.endValues = values[1];
		size = startValues.size();
		this.exp = new Expression(column.getColName());
	}

	public boolean match(Object value) {
		return true;
	}
	
	public boolean match(Object minValue, Object maxValue) {
		int size = this.size;
		IArray startValues = this.startValues;
		IArray endValues = this.endValues;
		
		for (int i = 1; i <= size; i++) {
			 // (StartA <= EndB) and (EndA >= StartB)
			int cmp = startValues.compareTo(i, maxValue);
			if (cmp > 0) continue;
			
			cmp = endValues.compareTo(i, minValue);
			if (cmp < 0) continue;
			return true;
		}
		return false;
	}
	
	public IArray calculateAll(Context ctx) {
		//return true
		IArray arr = exp.calculateAll(ctx);
		BoolArray resultArray = new BoolArray(true, arr.size());
		resultArray.setTemporary(true);
		return resultArray;
	}
	
	public IArray calculateAnd(Context ctx, IArray leftResult) {
		return leftResult.isTrue();
	}
	
	public int isValueRangeMatch(Context ctx) {
		IArray arr = exp.calculateAll(ctx);
		Object minValue = arr.get(1);
		Object maxValue = arr.get(2);
		
		int size = this.size;
		IArray startValues = this.startValues;
		IArray endValues = this.endValues;
		
		for (int i = 1; i <= size; i++) {
			 // (StartA <= EndB) and (EndA >= StartB)
			int cmp = startValues.compareTo(i, maxValue);
			if (cmp > 0) continue;
			
			cmp = endValues.compareTo(i, minValue);
			if (cmp < 0) continue;
			return Relation.ALLMATCH;
		}
		return Relation.UNMATCH;
	}
	
}
