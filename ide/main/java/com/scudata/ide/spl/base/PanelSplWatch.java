package com.scudata.ide.spl.base;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.ByteMap;
import com.scudata.common.StringUtils;
import com.scudata.dm.Sequence;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.SheetSpl;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 查看表达式
 *
 */
public abstract class PanelSplWatch extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * 表格控件的滚动面板
	 */
	private JScrollPane jSPTable = new JScrollPane();
	/**
	 * 空行的数量
	 */
	private final int BLANK_ROW_COUNT = 1;
	/**
	 * 阻止变化
	 */
	private boolean preventChange = false;

	/** 序号列 */
	private final byte COL_INDEX = 0;
	/** 表达式列 */
	private final byte COL_EXP = 1;
	/** 值列 */
	private final byte COL_VALUE = 2;

	/**
	 * 表格控件。No.,Expression,Value
	 */
	private JTableEx tableWatch = new JTableEx(IdeSplMessage.get().getMessage(
			"paneldfxwatch.tablewatch")) {
		private static final long serialVersionUID = 1L;

		public void setValueAt(Object aValue, int row, int column) {
			if (!isItemDataChanged(row, column, aValue)) {
				if (column == COL_EXP) {
					if (StringUtils.isValidString(aValue) && this.isEnabled()) {
						recalcTable();
					}
				}
				return;
			}
			super.setValueAt(aValue, row, column);
			if (preventChange) {
				return;
			}
			if (column == COL_EXP) {
				if (StringUtils.isValidString(aValue) && this.isEnabled()) {
					recalcTable();
				}
				dataChanged();
				resetRows(true);
			}
		}

		public void rowfocusChanged(int oldRow, int newRow) {
			if (preventChange) {
				return;
			}
			if (newRow != -1) {
				showRow(newRow);
			}
		}

		public void doubleClicked(int xpos, int ypos, int row, int col,
				MouseEvent e) {
			if (!isEnabled()) {
				return;
			}
			if (col == COL_EXP) {
				try {
					preventChange = true;
					if (GM.dialogEditTableText(GV.appFrame, this, row, col)) {
						dataChanged();
					}
					resetRows(true);
					showRow(row);
				} finally {
					preventChange = false;
				}
			} else if (col == COL_VALUE) {
				showRow(row);
			}
		}

		private void showRow(int row) {
			Object val = data.getValueAt(row, COL_VALUE);
			Object name = data.getValueAt(row, COL_EXP);
			String varName = name == null ? "" : name.toString();
			GVSpl.panelValue.tableValue.setValue1(val, varName);
			GVSpl.panelValue.valueBar.refresh();
			this.repaint();
		}

		public boolean isCellEditable(int row, int column) {
			if (column == COL_VALUE) {
				return false;
			}
			return true;
		}
	};

	/**
	 * 构造函数
	 */
	public PanelSplWatch() {
		try {
			preventChange = true;
			setLayout(new BorderLayout());
			jSPTable.getViewport().add(tableWatch);
			this.add(jSPTable, BorderLayout.CENTER);
			tableWatch.setRowHeight(20);
			tableWatch.setIndexCol(COL_INDEX);
			setCellSet(null);
			recalcTable();
			setWatchEnabled(false);
		} finally {
			preventChange = false;
		}
	}

	/**
	 * 重设表格
	 * 
	 * @param requestFocus
	 */
	private void resetRows(final boolean requestFocus) {
		int len = tableWatch.getRowCount();
		int blankCount = 0;
		for (int r = len - 1; r >= 0; r--) {
			if (StringUtils.isValidString(tableWatch.data
					.getValueAt(r, COL_EXP))) {
				break;
			} else {
				blankCount++;
			}
		}
		if (blankCount == BLANK_ROW_COUNT)
			return;
		final int bc = blankCount;
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				try {
					preventChange = true;
					if (bc >= BLANK_ROW_COUNT) {
						for (int i = 0; i < bc - BLANK_ROW_COUNT; i++) {
							tableWatch.removeRow(tableWatch.getRowCount() - 1);
						}
					} else {
						for (int i = 0; i < BLANK_ROW_COUNT - bc; i++) {
							tableWatch.addRow();
						}
					}
					if (requestFocus) {
						tableWatch.requestFocus();
					}
					tableWatch.setColumnSelectionInterval(COL_EXP, COL_EXP);
					if (tableWatch.getSelectedRow() > -1)
						tableWatch.setEditingRow(tableWatch.getSelectedRow());
					tableWatch.setEditingColumn(COL_EXP);
				} finally {
					preventChange = false;
				}
			}
		});
	}

	/**
	 * 设置网格对象
	 * 
	 * @param cs
	 */
	public void setCellSet(PgmCellSet cs) {
		try {
			preventChange = true;
			tableWatch.acceptText();
			tableWatch.removeAllRows();
			tableWatch.clearSelection();
			if (cs != null) {
				ByteMap bm = cs.getCustomPropMap();
				if (bm != null) {
					Sequence exps = (Sequence) bm.get(GC.CELLSET_EXPS);
					if (exps != null) {
						for (int i = 1, len = exps.length(); i <= len; i++) {
							int row = tableWatch.addRow();
							tableWatch.data.setValueAt(exps.get(i), row,
									COL_EXP);
						}
					}
				}
				resetRows(false);
			}
			setWatchEnabled(cs != null);
		} finally {
			preventChange = false;
		}
	}

	/**
	 * 查看表达式值
	 * 
	 * @param ctx
	 */
	public void watch() { // Context ctx
		try {
			preventChange = true;
			// this.ctx = ctx;
			recalcTable();
		} finally {
			preventChange = false;
		}

	}

	/**
	 * 数据变化了
	 */
	private void dataChanged() {
		if (preventChange) {
			return;
		}
		if (GVSpl.appSheet != null && GVSpl.appSheet instanceof SheetSpl) {
			Sequence exps = new Sequence();
			Object exp;
			for (int i = 0; i < tableWatch.getRowCount(); i++) {
				exp = tableWatch.data.getValueAt(i, COL_EXP);
				if (!StringUtils.isValidString(exp)) {
					continue;
				}
				exps.add(exp);
			}
			((SheetSpl) GVSpl.appSheet).setCellSetExps(exps);
		}
	}

	public abstract Object watch(String expStr);

	/**
	 * 重新计算
	 */
	private synchronized void recalcTable() {
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				try {
					setWatchEnabled(false);
					int rowCount = tableWatch.getRowCount();
					for (int i = 0; i < rowCount; i++) {
						tableWatch.data.setValueAt(null, i, COL_VALUE);
					}
					Object exp, val;

					for (int i = 0; i < rowCount; i++) {
						exp = tableWatch.data.getValueAt(i, COL_EXP);
						if (StringUtils.isValidString(exp)) {
							try {
								val = watch((String) exp);
								tableWatch.data.setValueAt(val, i, COL_VALUE);
							} catch (Throwable ex) {
								tableWatch.data.setValueAt(ex.getMessage(), i,
										COL_VALUE);
							}
						}
					}
				} finally {
					setWatchEnabled(true);
				}
			}
		});

	}

	/**
	 * 是否正在计算
	 * 
	 * @return
	 */
	public boolean isCalculating() {
		return !isEnabled();
	}

	/**
	 * 设置查看表达式是否可用
	 * 
	 * @param enable
	 */
	public void setWatchEnabled(boolean enable) {
		this.setEnabled(enable);
		jSPTable.setEnabled(enable);
		tableWatch.setEnabled(enable);
	}
}
