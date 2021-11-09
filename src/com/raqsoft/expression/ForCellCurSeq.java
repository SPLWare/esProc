package com.raqsoft.expression;

import com.raqsoft.cellset.datamodel.PgmCellSet;
import com.raqsoft.dm.Context;

/**
 * 取for单元格的当前循环序号
 * #cell
 * @author RunQian
 *
 */
public class ForCellCurSeq extends Node {
	private PgmCellSet pcs;
	private int row, col;

	public ForCellCurSeq(PgmCellSet pcs, int row, int col) {
		this.pcs = pcs;
		this.row = row;
		this.col = col;
	}

	public Object calculate(Context ctx) {
		return new Integer(pcs.getForCellRepeatSeq(row, col));
	}
}
