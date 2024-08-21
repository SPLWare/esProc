package com.scudata.dw;

import java.util.Arrays;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.ObjectArray;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Constant;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.expression.Relation;
import com.scudata.util.Variant;

/**
 * 用于Ki=wi和(Ki=wi,…,w)过滤器
 * @author LW
 *
 */
public class FindsFilter extends FindFilter {
	protected boolean pfind;
	protected boolean doFilter;
	
	public FindsFilter(ColumnMetaData column, int priority, Sequence sequence, Node node, boolean pfind, boolean doFilter) {
		super(column, priority, sequence, node);
		this.pfind = pfind;
		this.doFilter = doFilter;
	}
	
	public boolean match(Object value) {
		if (pfind) {
			int pos = it.findPos(value);
			findResult = pos;
			if (doFilter)
				return pos > 0;
			else
				return true;
		} else {
			findResult = it.find(value);
			if (doFilter)
				return findResult != null;
			else
				return true;
		}
		
	}
	
	public Object getFindResult() {
		return findResult;
	}
	
	public IArray getFindResultArray() {
		int[] pos = this.pos;
		int len = pos.length - 1;
		if (pfind) {
			return new IntArray(pos, null, len);
		}
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
		if (doFilter && Variant.isEquals(minValue, maxValue)) {
			return it.find(minValue) != null;
		}
		
		return true;
	}
	
	public IArray calculateAll(Context ctx) {
		IArray key = right.calculateAll(ctx);
		int[] pos = it.findAllPos(key);
		int len = key.size();
		boolean[] result = new boolean[len + 1];
		if (doFilter) {
			for (int i = 1; i <= len; i++) {
				result[i] = pos[i] != 0;
			}
		} else {
			Arrays.fill(result, true);
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
		if (doFilter) {
			for (int i = 1; i <= len; i++) {
				resultDatas[i] = pos[i] > 0;
			}
		}
		
		this.pos = pos;
		return resultArray;
	}
	
	public int isValueRangeMatch(Context ctx) {
		if (doFilter)
			return exp.isValueRangeMatch(ctx);
		else
			return Relation.ALLMATCH;
	}
	
	public void initExp() {
		if (exp == null) {
			String s = "null.find(" + column.getColName() + ")";
			exp = new Expression(s);
			
			exp.getHome().setLeft(new Constant(code));
		}
	}
}