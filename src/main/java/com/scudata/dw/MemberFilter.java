package com.scudata.dw;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Constant;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.util.Variant;

public class MemberFilter extends FindFilter {
	public static final int MAX_CHECK_NUMBER = 500;
	//private Sequence sequence;
	private IArray array;
	
	public MemberFilter(ColumnMetaData column, int priority, Sequence sequence, Node node) {
		super(column, priority);
		this.array = sequence.getMems();
		//this.sequence = sequence;
		
		if (node != null) exp = new Expression(node);
		right = new Expression(column.getColName());
	}
	
	public MemberFilter(String columnName, int priority, Sequence sequence) {
		super(columnName, priority);
		this.array = sequence.getMems();
		//this.sequence = sequence;
	}
	
	public boolean match(Object value) {
		try {
			int n = ((Number)value).intValue();
			if (n < 1 || n > array.size()) {
				findResult = null;
				return false;
			} else {
				findResult = array.get(n);
				return Variant.isTrue(findResult);
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean match(Object minValue, Object maxValue) {
		int min;
		int max;
		try {
			min = (minValue == null) ? 1 : ((Number)minValue).intValue();
			max = ((Number)maxValue).intValue();
		} catch (Exception e) {
			return false;
		}
		
		if (max >= min && max - min <= MAX_CHECK_NUMBER) {
			IArray array = this.array;
			for (int i = min; i <= max; i++) {
				if (Variant.isTrue(array.get(i))) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	public Object getFindResult() {
		return findResult;
	}
	
	public IArray getFindResultArray() {
		int[] pos = this.pos;
		int len = pos.length - 1;
		IArray array = this.array;
		ObjectArray resultValue = new ObjectArray(len);
		
		for (int i = 1; i <= len; i++) {
			if (pos[i] != 0) {
				resultValue.push(array.get(pos[i]));
			} else {
				resultValue.push(null);
			}
		}
		
		return resultValue;
	}
	
	public IArray calculateAll(Context ctx) {
		IArray key = right.calculateAll(ctx);
		int len = key.size();
		IArray array = this.array;
		int codeLen = array.size();
		boolean[] result = new boolean[len + 1];
		int[] pos = new int[len + 1];
		
		if (key.isNumberArray()) {
			for (int i = 1; i <= len; i++) {
				int n = key.getInt(i);
				if (n > 0 && n <= codeLen) {
					if (array.isFalse(n)) {
						continue;
					}
					result[i] = true;
					pos[i] = n;
				}
			}
		}
		
		this.pos = pos;
		BoolArray resultArray = new BoolArray(result, len);
		resultArray.setTemporary(true);
		return resultArray;
	}
	
	public IArray calculateAnd(Context ctx, IArray leftResult) {
		BoolArray left = leftResult.isTrue();
		boolean[] leftArray = left.getDatas();
		IArray key = right.calculateAll(ctx);
		int len = key.size();
		IArray array = this.array;
		int codeLen = array.size();
		boolean[] result = new boolean[len + 1];
		int[] pos = new int[len + 1];
		
		if (key.isNumberArray()) {
			for (int i = 1; i <= len; i++) {
				if (leftArray[i]) {
					int n = key.getInt(i);
					if (n > 0 && n <= codeLen) {
						if (array.isFalse(n)) {
							continue;
						}
						result[i] = true;
						pos[i] = n;
					}
				}
			}
		}
		
		this.pos = pos;
		BoolArray resultArray = new BoolArray(result, len);
		resultArray.setTemporary(true);
		return resultArray;
	}
	
	public void initExp() {
		if (exp == null) {
			String s = "null(" + column.getColName() + ")";
			exp = new Expression(new Context(), s);
			exp.getHome().setLeft(new Constant(new Sequence(array)));
		}
	}
}