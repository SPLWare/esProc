package com.scudata.ide.spl.control;

import java.awt.dnd.DropTarget;

import javax.swing.JPanel;

import com.scudata.cellset.datamodel.CellSet;

/**
 * 网格控件
 *
 */
public class EditControl extends SplControl {

	private static final long serialVersionUID = 1L;

	/**
	 * 网格是否可以编辑
	 */
	private boolean editable = true;

	/**
	 * 构造函数
	 *
	 * @param rows int
	 * @param cols int
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
		ContentPanel panel = newContentPanel(cellSet);
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
	 * 创建SPL网格面板
	 * @param cellSet
	 * @return ContentPanel
	 */
	protected ContentPanel newContentPanel(CellSet cellSet) {
		return new ContentPanel(cellSet, 1, cellSet.getRowCount(), 1,
				cellSet.getColCount(), true, true, this);
	}

	/**
	 * 提交文本编辑
	 */
	public void acceptText() {
		this.contentView.submitEditor();
	}

}
