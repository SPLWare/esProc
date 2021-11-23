package com.scudata.ide.dfx.control;

import java.awt.dnd.DropTarget;

import javax.swing.JPanel;

import com.scudata.common.Area;
import com.scudata.common.CellLocation;

/**
 * 网格控件
 *
 */
public abstract class EditControl extends DfxControl {

	private static final long serialVersionUID = 1L;

	/**
	 * 网格是否可以编辑
	 */
	private boolean editable = true;

	/**
	 * 构造函数
	 *
	 * @param rows
	 *            int
	 * @param cols
	 *            int
	 */
	public EditControl(int rows, int cols) {
		super(rows, cols);
	}

	/**
	 * 生成角部面板
	 *
	 * @return 角部面板
	 */
	JPanel createCorner() {
		JPanel panel = new CornerPanel(this, editable);
		CornerListener listener = new CornerListener(this, editable);
		panel.addMouseListener(listener);
		return panel;
	}

	/**
	 * 生成列首格面板
	 *
	 * @return 列首格面板
	 */
	JPanel createColHeaderView() {
		headerPanel = new ColHeaderPanel(this, editable);
		ColHeaderListener listener = new ColHeaderListener(this, editable);
		headerPanel.addMouseListener(listener);
		headerPanel.addMouseMotionListener(listener);
		headerPanel.addKeyListener(listener);
		return headerPanel;
	}

	/**
	 * 生成行首格面板
	 *
	 * @return 行首格面板
	 */
	JPanel createRowHeaderView() {
		JPanel panel = new RowHeaderPanel(this, editable);
		RowHeaderListener listener = new RowHeaderListener(this, editable);
		panel.addMouseListener(listener);
		panel.addMouseMotionListener(listener);
		panel.addKeyListener(listener);
		return panel;
	}

	/**
	 * 生成内容面板
	 *
	 * @return 内容面板
	 */
	ContentPanel createContentView() {
		ContentPanel panel = new ContentPanel(dfx, 1, dfx.getRowCount(), 1,
				dfx.getColCount(), true, true, this);
		CellSelectListener listener = new CellSelectListener(this, panel,
				editable);
		panel.addMouseListener(listener);
		panel.addMouseMotionListener(listener);
		panel.addKeyListener(listener);
		DropTarget target = new DropTarget(panel, new EditDropListener());
		panel.setDropTarget(target);
		panel.setFocusTraversalKeysEnabled(false);

		return panel;
	}

	/**
	 * 提交文本编辑
	 */
	public void acceptText() {
		this.contentView.submitEditor();
	}

	/**
	 * 设置搜索匹配到的格子
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @param searchInSelectedCells
	 *            是否在选择区域内搜索的
	 */
	public void setSearchedCell(int row, int col, boolean searchInSelectedCells) {
		setActiveCell(new CellLocation(row, col));
		ControlUtils.scrollToVisible(this.getViewport(), this, row, col);
		if (!searchInSelectedCells) {
			setSelectedArea(new Area(row, col, row, col));
			this.fireRegionSelect(true);
		}
		this.repaint();
	}

}
