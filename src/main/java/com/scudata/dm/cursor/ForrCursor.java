package com.scudata.dm.cursor;

import com.scudata.cellset.INormalCell;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.CellLocation;
import com.scudata.dm.*;
import com.scudata.expression.Expression;

/**
 * 用于语句forr游标
 * @author RunQian
 *
 */
public class ForrCursor extends ICursor {
	private PgmCellSet pcs;	// 网格对象
	private NormalCell cell; // forr 所在的单元格
	private NormalCell resultCell; // 结果集单元格
	private int codeBlockEndRow; // 代码块结束行（包括）
	
	private ICursor cs;
	private Expression gexp = null;
	private int gcount = -1;
	
	public ForrCursor(PgmCellSet pcs, NormalCell cell, NormalCell resultCell, int codeBlockEndRow, 
			ICursor cs, Expression gexp, int gcount, Context ctx) {
		this.pcs = pcs;
		this.cell = cell;
		this.resultCell = resultCell;
		this.codeBlockEndRow = codeBlockEndRow;
		this.cs = cs;
		this.gexp = gexp;
		this.gcount = gcount;
		this.ctx = ctx;
	}
	
	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		PgmCellSet pcs = this.pcs;
		if (pcs == null || n < 1) {
			return null;
		}
		
		NormalCell cell = this.cell;
		NormalCell resultCell = this.resultCell;
		int row = cell.getRow();
		int col = cell.getCol();
		int codeBlockEndRow = this.codeBlockEndRow;
		CellLocation lct;
		
		INormalCell current = pcs.getCurrent();
		int initSize = n > INITSIZE ? INITSIZE : n;
		Sequence result = new Sequence(initSize);
		
		for (int i = 0; i < n; ++i) {
			Sequence table;
			if (gexp == null) {
				table = cs.fetch(gcount);
			} else {
				table = cs.fetchGroup(gexp, ctx);
			}
			
			if (table == null || table.length() == 0) {
				break;
			}
			
			cell.setValue(table);
			pcs.setNext(row, col + 1, false); // 执行下一单元格
			
			do {
				lct = pcs.runNext();
			} while (lct != null && lct.getRow() <= codeBlockEndRow);
			
			if (resultCell != null) {
				result.add(resultCell.getValue());
			}
		}

		pcs.setCurrent(current);
		return result;
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		PgmCellSet pcs = this.pcs;
		if (pcs == null || n < 1) {
			return 0;
		}
		
		NormalCell cell = this.cell;
		int row = cell.getRow();
		int col = cell.getCol();
		int codeBlockEndRow = this.codeBlockEndRow;
		CellLocation lct;
		
		INormalCell current = pcs.getCurrent();
		long result = 0;
		
		for (int i = 0; i < n; ++i) {
			Sequence table;
			if (gexp == null) {
				table = cs.fetch(gcount);
			} else {
				table = cs.fetchGroup(gexp, ctx);
			}
			
			if (table == null || table.length() == 0) {
				break;
			}
			
			cell.setValue(table);
			pcs.setNext(row, col + 1, false); // 执行下一单元格
			
			do {
				lct = pcs.runNext();
			} while (lct != null && lct.getRow() <= codeBlockEndRow);
			
			result++;
		}

		pcs.setCurrent(current);
		return result;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (pcs != null) {
			pcs = null;
			cell = null;
			resultCell = null;
			cs = null;
			gexp = null;
		}
	}
}
