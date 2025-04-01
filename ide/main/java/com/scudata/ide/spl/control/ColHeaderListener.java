package com.scudata.ide.spl.control;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.swing.SwingUtilities;

import com.scudata.cellset.IColCell;
import com.scudata.common.Area;
import com.scudata.common.CellLocation;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.control.ControlUtilsBase;
import com.scudata.ide.spl.GCSpl;

/**
 * 列表头监听器
 *
 */
public class ColHeaderListener implements MouseMotionListener, MouseListener,
		KeyListener {
	/**
	 * 网格控件
	 */
	private SplControl control;

	/** 被选择的开始列号 */
	private int startSelectedCol;

	/** 列宽改变时的起始X坐标 */
	private int resizeStartX;

	/** 列宽改变时的被拉伸列号 */
	private int resizeStartCol;

	/** 列宽改变前的原始宽度 */
	private float oldCellWidth;

	/** 列宽改变过程中的临时宽度 */
	private int tmpWidth;

	/**
	 * 是否可编辑
	 */
	private boolean editable = true;

	/**
	 * 当前格
	 */
	private transient CellLocation activeCell = null;

	/**
	 * 支持列头多选
	 */
	protected boolean supportMultiSelect = true;

	/**
	 * 监听器构造函数
	 * 
	 * @param control
	 *            网格控件
	 */
	public ColHeaderListener(SplControl control) {
		this(control, true);
	}

	/**
	 * 监听器构造函数
	 *
	 * @param control
	 *            网格控件
	 * @param editable
	 *            是否可以编辑
	 */
	public ColHeaderListener(SplControl control, boolean editable) {
		this.control = control;
		this.editable = editable;
	}

	/**
	 * 鼠标进入事件
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * 按下鼠标左键时的处理
	 *
	 * @param e
	 *            鼠标事件
	 */
	public void mousePressed(MouseEvent e) {
		if (!editable) {
			showPopup(e);
			return;
		}
		control.getColumnHeader().getView().requestFocus();
		int col = (int) ControlUtils.lookupHeaderIndex(e.getX(), control.cellX,
				control.cellW);
		if (col < 0) {
			return;
		}
		boolean colIsSelected = false;
		if (control.m_selectedCols.size() > 0) {
			for (int i = 0; i < control.m_selectedCols.size(); i++) {
				Integer c = (Integer) control.m_selectedCols.get(i);
				if (c.intValue() == col) {
					colIsSelected = true;
					break;
				}
			}
		}
		if (e.getButton() == MouseEvent.BUTTON1 || !colIsSelected) {
			// 在未选中的行首格按右键时先将其选中
			if (control.status != GCSpl.STATUS_CELLRESIZE
					|| e.getButton() != MouseEvent.BUTTON1) {
				resizeStartCol = 0;
				if (!e.isControlDown() || !supportMultiSelect) {
					control.clearSelectedArea();
					control.m_selectedCols.clear();
				}
				control.m_selectedRows.clear();

				// 学习excel把选中区域的首格作为当前格
				int firstRow = control.getContentPanel().drawStartRow;
				if (firstRow < 1 || firstRow > control.cellSet.getRowCount())
					firstRow = 1;
				control.setActiveCell(new CellLocation(firstRow, col), false);

				control.m_cornerSelected = false;
				if (e.isShiftDown() && this.startSelectedCol > 0) {
					control.m_selectedCols.clear();
					int start = col < this.startSelectedCol ? col
							: this.startSelectedCol;
					int end = col < this.startSelectedCol ? this.startSelectedCol
							: col;
					for (int i = start; i <= end; i++) {
						control.addSelectedCol(new Integer(i));
					}
					control.addSelectedArea(
							new Area(1, start, control.cellSet.getRowCount(),
									end), true);
				} else {
					this.startSelectedCol = col;
					control.addSelectedCol(new Integer(col));
					control.addSelectedArea(
							new Area(1, col, control.cellSet.getRowCount(), col),
							false);
				}
				control.repaint();
				control.fireRegionSelect(true);
			} else if (e.getButton() == MouseEvent.BUTTON1) {
				control.getContentPanel().submitEditor();
				resizeStartX = e.getX();
				resizeStartCol = (int) ControlUtils.lookupHeaderIndex(
						resizeStartX, control.cellX, control.cellW);
				oldCellWidth = control.cellW[col] / control.scale;
				tmpWidth = control.cellW[col];
			}
		}
		showPopup(e);
		activeCell = control.getActiveCell();
		control.setActiveCell(null);
	}

	/**
	 * 鼠标左键释放时的处理
	 *
	 * @param e
	 *            鼠标事件
	 */
	public void mouseReleased(MouseEvent e) {
		if (!editable) {
			showPopup(e);
			return;
		}
		int x = e.getX();
		int col = (int) ControlUtils.lookupHeaderIndex(x, control.cellX,
				control.cellW);
		// 拖拽时，刷新控件造成拖不动，只要被拖拽了，就禁止活动格,遗留问题，拖拽隐藏了活动格，造成光标没法走到下一格
		if (activeCell != null && activeCell.getCol() == col) {
			control.setActiveCell(activeCell, false);
		}

		if (control.status == GCSpl.STATUS_CELLRESIZE) {
			if (resizeStartCol > 0) {
				if (col != resizeStartCol) {
					col = resizeStartCol;
				}
				Vector<Integer> willResizeCols = new Vector<Integer>();
				willResizeCols.add(new Integer(col));
				if (control.m_selectedCols.size() > 0) {
					int c1 = ((Integer) control.m_selectedCols.get(0))
							.intValue();
					int c2 = ((Integer) control.m_selectedCols
							.get(control.m_selectedCols.size() - 1)).intValue();
					int selectedStartCol = (int) Math.min(c1, c2);
					int selectedEndCol = (int) Math.max(c1, c2);
					if (col >= selectedStartCol && col <= selectedEndCol) {
						willResizeCols = control.m_selectedCols;
					}
				}
				IColCell cc = control.cellSet.getColCell(col);
				if (cc != null) {
					cc.setWidth(oldCellWidth);
					resizeColWidth(willResizeCols, tmpWidth / control.scale);
				}
			}
		} else {
			control.fireRegionSelect(true);
			control.status = GCSpl.STATUS_NORMAL;
		}

		final MouseEvent me = e;
		Thread t = new Thread() {
			public void run() {
				showPopup(me);
			}
		};
		SwingUtilities.invokeLater(t); // 延迟弹出，避免菜单在控件中被挡住
	}

	protected void resizeColWidth(Vector<Integer> willResizeCols, float newWidth) {
		control.fireColHeaderResized(willResizeCols, newWidth);
	}

	/**
	 * 鼠标点击事件
	 */
	public void mouseClicked(MouseEvent e) {
		// 双击列标题的格线，触发自动调整列宽的功能
		if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
			int x = e.getX();
			int col = (int) ControlUtils.lookupHeaderIndex(x, control.cellX,
					control.cellW);
			if (x >= control.cellX[col] + control.cellW[col] - 2
					&& x <= control.cellX[col] + control.cellW[col]) {
				Vector<Integer> cols = new Vector<Integer>();
				cols.add(new Integer(col));
				SplEditor editor = ControlUtils.extractSplEditor(control);
				if (editor != null) {
					editor.selectedCols = cols;
					editor.adjustColWidth();
				}
			} else {
				mouseDoubleClicked(e, col);
			}
		}
	}

	public void mouseDoubleClicked(MouseEvent e, int col) {
	}

	/**
	 * 鼠标退出事件
	 */
	public void mouseExited(MouseEvent e) {
	}

	/**
	 * 按下鼠标左键并拖动鼠标时的处理
	 *
	 * @param e
	 *            鼠标事件
	 */
	public void mouseDragged(MouseEvent e) {
		if (!editable) {
			return;
		}
		int x = e.getX();
		int col = (int) ControlUtils.lookupHeaderIndex(x, control.cellX,
				control.cellW);
		if (control.status == GCSpl.STATUS_NORMAL) {
			if (col < 0) {
				return;
			}
			int start = this.startSelectedCol < col ? this.startSelectedCol
					: col;
			int end = this.startSelectedCol > col ? this.startSelectedCol : col;
			if (start <= 0) { // wunan
				return;
			}
			control.m_selectedCols.clear(); // 拖拽多列再返回时，列被多选
			for (int i = start; i <= end; i++) {
				control.addSelectedCol(new Integer(i));
			}
			control.addSelectedArea(
					new Area(1, start, control.cellSet.getRowCount(), end),
					true);
			if (ControlUtils.scrollToVisible(control.getColumnHeader(),
					control, 0, col)) {
				Point p1 = control.getColumnHeader().getViewPosition();
				Point p2 = control.getViewport().getViewPosition();
				p2.x = p1.x;
				control.getViewport().setViewPosition(p2);
			}
			control.repaint();
			control.fireRegionSelect(true);
		}
		if (control.status == GCSpl.STATUS_CELLRESIZE) {
			if (resizeStartCol > 0) {
				if (col != resizeStartCol) {
					col = resizeStartCol;
				}
				tmpWidth = tmpWidth + x - resizeStartX;
				resizeStartX = x;
				if (tmpWidth < 1) {
					tmpWidth = 1;
				}
				float newWidth = tmpWidth / control.scale;
				IColCell cc = control.cellSet.getColCell(col);
				if (cc != null)
					cc.setWidth(newWidth);
				control.getColumnHeader().getView().repaint();

				// 拖拽过程不实时刷新内容，会导致文字的wrapBuffer急剧冗余
				control.getViewport().getView().repaint();
				ControlUtilsBase.clearWrapBuffer();
			}
		}
	}

	/**
	 * 鼠标移动时的处理
	 *
	 * @param e
	 *            鼠标事件
	 */
	public void mouseMoved(MouseEvent e) {
		if (!editable) {
			return;
		}
		int x = e.getX();
		int col = ControlUtils.lookupHeaderIndex(x, control.cellX,
				control.cellW);
		if (col < 0) {
			control.status = GCSpl.STATUS_NORMAL;
			control.getColumnHeader()
					.getView()
					.setCursor(
							Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		if (x >= control.cellX[col] + control.cellW[col] - 2
				&& x <= control.cellX[col] + control.cellW[col]) {
			control.status = GCSpl.STATUS_CELLRESIZE;
			control.getColumnHeader()
					.getView()
					.setCursor(
							Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
		} else {
			control.status = GCSpl.STATUS_NORMAL;
			control.getColumnHeader()
					.getView()
					.setCursor(
							Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	/**
	 * 右键弹出菜单
	 * 
	 * @param e
	 */
	void showPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			control.fireRightClicked(e, GC.SELECT_STATE_COL);
		}
	}

	/**
	 * 按键抬起事件
	 */
	public void keyReleased(KeyEvent e) {
	}

	/**
	 * 键盘被按下的处理 若按下的是shift+方向键，相应改变当前选中的列
	 *
	 * @param e
	 *            键盘事件
	 */
	public void keyPressed(KeyEvent e) {
		if (!editable) {
			return;
		}
		int key = e.getKeyCode();
		switch (key) {
		case KeyEvent.VK_RIGHT:
			if (!e.isShiftDown()) {
				control.toRightCell();
				e.consume();
				return;
			}
			int start = ((Integer) control.m_selectedCols.get(0)).intValue();
			int end = ((Integer) control.m_selectedCols
					.get(control.m_selectedCols.size() - 1)).intValue();
			if (this.startSelectedCol == start) {
				end++;
			} else {
				start++;
			}
			if (end > control.cellSet.getColCount()) {
				end = (int) control.cellSet.getColCount();
			}
			if (start < 1) {
				start = 1;
			}
			control.m_selectedRows.clear();
			control.m_cornerSelected = false;
			control.m_selectedCols.clear();
			for (int i = start; i <= end; i++) {
				control.addSelectedCol(new Integer(i));
			}
			control.addSelectedArea(
					new Area(1, (int) start, control.cellSet.getRowCount(),
							(int) end), true);
			if (ControlUtils.scrollToVisible(control.getColumnHeader(),
					control, 0, (int) end)) {
				Point p1 = control.getColumnHeader().getViewPosition();
				Point p2 = control.getViewport().getViewPosition();
				p2.x = p1.x;
				control.getViewport().setViewPosition(p2);
			}
			control.repaint();
			control.fireRegionSelect(true);
			e.consume();
			break;
		case KeyEvent.VK_LEFT:
			if (!e.isShiftDown()) {
				control.toLeftCell();
				e.consume();
				return;
			}
			start = ((Integer) control.m_selectedCols.get(0)).intValue();
			end = ((Integer) control.m_selectedCols.get(control.m_selectedCols
					.size() - 1)).intValue();
			if (this.startSelectedCol == end) {
				start--;
			} else {
				end--;
			}
			if (end > control.cellSet.getColCount()) {
				end = (int) control.cellSet.getColCount();
			}
			if (start < 1) {
				start = 1;
			}
			control.m_selectedRows.clear();
			control.m_cornerSelected = false;
			control.m_selectedCols.clear();
			for (int i = start; i <= end; i++) {
				control.addSelectedCol(new Integer(i));
			}
			control.addSelectedArea(
					new Area(1, (int) start, control.cellSet.getRowCount(),
							(int) end), true);
			if (ControlUtils.scrollToVisible(control.getColumnHeader(),
					control, 0, (int) start)) {
				Point p1 = control.getColumnHeader().getViewPosition();
				Point p2 = control.getViewport().getViewPosition();
				p2.x = p1.x;
				control.getViewport().setViewPosition(p2);
			}
			control.repaint();
			e.consume();
			break;
		default:
			return;
		}
	}

	/**
	 * 按键按下事件
	 */
	public void keyTyped(KeyEvent e) {
	}

}
