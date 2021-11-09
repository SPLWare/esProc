package com.raqsoft.expression;

import com.raqsoft.cellset.ICellSet;
import com.raqsoft.cellset.INormalCell;
import com.raqsoft.dm.Context;
import com.raqsoft.util.Variant;

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
