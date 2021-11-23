package com.scudata.expression;

import com.scudata.cellset.INormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.resources.EngineMessage;

/**
 * 依次计算代码块中除主格外的单元格，返回最后一个计算格的结果
 * ??
 * @author RunQian
 *
 */
public class SubVal extends Node {
	private PgmCellSet pcs;

	public SubVal(PgmCellSet pcs) {
		this.pcs = pcs;
	}

	public Object calculate(Context ctx) {
		INormalCell cell = pcs.getCurrent();
		if (cell == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("cellset.cellNotExist"));
		}

		return pcs.executeSubCell(cell.getRow(), cell.getCol());
	}
}
