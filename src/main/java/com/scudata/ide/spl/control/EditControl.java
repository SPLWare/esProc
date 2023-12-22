package com.scudata.ide.spl.control;

import java.awt.dnd.DropTarget;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;

import com.scudata.cellset.datamodel.CellSet;
import com.scudata.ide.spl.GCSpl;
import com.scudata.ide.spl.GVSpl;

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
		panel.addMouseWheelListener(mouseWheelListener);
		MouseWheelListener[] listeners = getMouseWheelListeners();
		if (listeners != null) {
			for (MouseWheelListener l : listeners)
				panel.addMouseWheelListener(l);
		}
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
		headerPanel.addMouseWheelListener(mouseWheelListener);
		MouseWheelListener[] listeners = getMouseWheelListeners();
		if (listeners != null) {
			for (MouseWheelListener l : listeners)
				headerPanel.addMouseWheelListener(l);
		}
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
		panel.addMouseWheelListener(mouseWheelListener);
		MouseWheelListener[] listeners = getMouseWheelListeners();
		if (listeners != null) {
			for (MouseWheelListener l : listeners)
				panel.addMouseWheelListener(l);
		}
		return panel;
	}

	/**
	 * 生成内容面板
	 *
	 * @return 内容面板
	 */
	ContentPanel createContentView() {
		ContentPanel contentPanel = newContentPanel(cellSet);
		CellSelectListener listener = new CellSelectListener(this,
				contentPanel, editable);
		contentPanel.addMouseListener(listener);
		contentPanel.addMouseMotionListener(listener);
		contentPanel.addKeyListener(listener);
		contentPanel.addMouseWheelListener(mouseWheelListener);
		MouseWheelListener[] listeners = getMouseWheelListeners();
		if (listeners != null) {
			for (MouseWheelListener l : listeners)
				contentPanel.addMouseWheelListener(l);
		}
		DropTarget target = new DropTarget(contentPanel, new EditDropListener());
		contentPanel.setDropTarget(target);
		contentPanel.setFocusTraversalKeysEnabled(false);
		return contentPanel;
	}

	private MouseWheelListener mouseWheelListener = new MouseWheelListener() {

		public void mouseWheelMoved(MouseWheelEvent e) {
			if (!e.isControlDown()) {
				return;
			}
			int percent = (int) (scale * 100);
			int wr = e.getWheelRotation();
			int newPercent;
			if (wr < 0) { // 滚轮向上，放大
				newPercent = GCSpl.DEFAULT_SCALES[GCSpl.DEFAULT_SCALES.length - 1];
				for (int i = 0; i < GCSpl.DEFAULT_SCALES.length; i++) {
					if (percent < GCSpl.DEFAULT_SCALES[i] - 7) {
						newPercent = GCSpl.DEFAULT_SCALES[i];
						break;
					}
				}
			} else { // 缩小
				newPercent = GCSpl.DEFAULT_SCALES[0];
				for (int i = GCSpl.DEFAULT_SCALES.length - 1; i >= 0; i--) {
					if (percent > GCSpl.DEFAULT_SCALES[i] + 7) {
						newPercent = GCSpl.DEFAULT_SCALES[i];
						break;
					}
				}
			}
			if (newPercent != percent) {
				setScale(new Float(newPercent) / 100f);
				if (GVSpl.splEditor != null)
					GVSpl.splEditor.setDataChanged(true);
			}
			e.consume();
		}

	};

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
