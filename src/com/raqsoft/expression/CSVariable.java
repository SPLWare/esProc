package com.raqsoft.expression;

import java.util.List;

import com.raqsoft.cellset.INormalCell;
import com.raqsoft.dm.Context;
import com.raqsoft.util.Variant;

/**
 * 单元格引用
 * A1
 * @author RunQian
 *
 */
public class CSVariable extends Node {
	private INormalCell cell;

	public CSVariable(INormalCell cell) {
		this.cell = cell;
	}

	public Object calculate(Context ctx) {
		return cell.getValue(true);
	}

	public Object assign(Object value, Context ctx) {
		cell.setValue(value);
		return value;
	}
	
	public Object addAssign(Object value, Context ctx) {
		Object result = Variant.add(cell.getValue(true), value);
		cell.setValue(result);
		return result;
	}
	
	public byte calcExpValueType(Context ctx) {
		return cell.calcExpValueType(ctx);
	}

	public INormalCell calculateCell(Context ctx) {
		return cell;
	}

	public INormalCell getSourceCell() {
		return cell;
	}
	
	protected void getUsedCells(List<INormalCell> resultList) {
		if (!resultList.contains(cell)) {
			resultList.add(cell);
		}
	}
}
