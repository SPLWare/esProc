package com.scudata.expression;

import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.dm.Context;
import com.scudata.util.Variant;

/**
 * @当前单元格引用
 * @author WangXiaoJun
 *
 */
public class CurrentCell extends Node {
	private ICellSet cs;

	public CurrentCell(ICellSet cs) {
		this.cs = cs;
	}

	public Object calculate(Context ctx) {
		INormalCell cell = cs.getCurrent();
		return cell.getValue(true);
	}

	public Object assign(Object value, Context ctx) {
		INormalCell cell = cs.getCurrent();
		if (cell != null) {
			cell.setValue(value);
		}
		return value;
	}
	
	public Object addAssign(Object value, Context ctx) {
		INormalCell cell = cs.getCurrent();
		Object result = Variant.add(cell.getValue(true), value);
		cell.setValue(result);
		return result;
	}
}
