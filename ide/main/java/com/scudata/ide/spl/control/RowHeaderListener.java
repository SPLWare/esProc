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

import com.scudata.cellset.IRowCell;
import com.scudata.common.Area;
import com.scudata.common.CellLocation;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.control.ControlUtilsBase;
import com.scudata.ide.spl.GCSpl;

/**
 * 行表头监听器
 *
 */
public class RowHeaderListener implements MouseMotionListener, MouseListener,
		KeyListener {
	/** 网格编辑控件 */
	private SplControl control;

	/** 选择时的起始行 */
	private int startSelectedRow;

	/** 改变行高时的鼠标起始点Y坐标 */
	private int resizeStartY;

	/** 改变行高时的被操作行 */
	private int resizeStartRow;

	/** 改变行高前的原始行高 */
	private float oldCellHeight;

	/** 改变行高过程中的临时行高 */
	private int tmpHeight;

	/** 是否可以编辑 */
	private boolean editable = true;
	/** 当前格坐标 */
	private transient CellLocation activeCell = null;

	/**
	 * 支持行头多选
	 */
	protected boolean supportMultiSelect = true;

	/**
	 * 监听器构造函数
	 * 
	 * @param control 网格编辑控件
	 */
	public RowHeaderListener(SplControl control) {
		this(control, true);
	}

	/**
	 * 监听器构造函数
	 * 
	 * @param control  网格编辑控件
	 * @param editable 是否可以编辑
	 */
	public RowHeaderListener(SplControl control, boolean editable) {
		this.control = control;
		this.editable = editable;
	}

	/**
	 * 鼠标进入事件
	 * 
	 * @param e
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * 鼠标按下时的处理
	 * 
	 * @param e 鼠标事件
	 */
	public void mousePressed(MouseEvent e) {
		if (!editable) {
			showPopup(e);
			return;
		}
		control.getRowHeader().getView().requestFocus();
		final int row = ControlUtils.lookupHeaderIndex(e.getY(), control.cellY,
				control.cellH);
		if (row < 0) {
			return;
		}

		boolean rowIsSelected = false;
		if (!control.m_selectedRows.isEmpty()) {
			for (int i = 0; i < control.m_selectedRows.size(); i++) {
				Integer r = (Integer) control.m_selectedRows.get(i);
				if (r.intValue() == row) {
					rowIsSelected = true;
					break;
				}
			}
		}
		if (e.getButton() == MouseEvent.BUTTON1 || !rowIsSelected) {
			if (control.status != GCSpl.STATUS_CELLRESIZE
					|| e.getButton() != MouseEvent.BUTTON1) {
				resizeStartRow = 0;
				if (!e.isControlDown() || !supportMultiSelect) {
					control.clearSelectedArea();
					control.m_selectedRows.clear();
				}
				control.m_selectedCols.clear();

				// 学习excel把选中区域的首格作为当前格
				int firstCol = control.getContentPanel().drawStartCol;
				if (firstCol < 1 || firstCol > control.cellSet.getColCount())
					firstCol = 1;
				control.setActiveCell(new CellLocation(row, firstCol), false);

				control.m_cornerSelected = false;
				if (e.isShiftDown() && this.startSelectedRow > 0) {
					control.m_selectedRows = new Vector<Integer>();
					int start = row < this.startSelectedRow ? row
							: this.startSelectedRow;
					int end = row < this.startSelectedRow ? this.startSelectedRow
							: row;
					for (int i = start; i <= end; i++) {
						control.addSelectedRow(new Integer(i));
					}
					control.addSelectedArea(new Area(start, (int) 1, end,
							(int) control.cellSet.getColCount()), false);
				} else {
					this.startSelectedRow = row;
					control.addSelectedRow(new Integer(row));
					control.addSelectedArea(new Area(row, (int) 1, row,
							(int) control.cellSet.getColCount()), false);
				}
				control.repaint();
				control.fireRegionSelect(true);
			} else if (e.getButton() == MouseEvent.BUTTON1) {
				control.getContentPanel().submitEditor();
				resizeStartY = e.getY();
				resizeStartRow = ControlUtils.lookupHeaderIndex(resizeStartY,
						control.cellY, control.cellH);
				oldCellHeight = control.cellH[row] / control.scale;
				tmpHeight = control.cellH[row];
			}
		}
		showPopup(e);
		activeCell = control.getActiveCell();
		control.setActiveCell(null);
	}

	/**
	 * 鼠标释放时的处理
	 * 
	 * @param e 鼠标事件
	 */
	public void mouseReleased(MouseEvent e) {
		if (!editable) {
			showPopup(e);
			return;
		}
		int y = e.getY();
		int row = ControlUtils.lookupHeaderIndex(y, control.cellY,
				control.cellH);
		// 拖拽时，刷新控件造成拖不动，只要被拖拽了，就禁止活动格
		if (activeCell != null && control.m_selectedRows != null
				&& control.m_selectedRows.contains(activeCell.getRow())) {
			control.setActiveCell(activeCell, false);
		}
		if (control.status == GCSpl.STATUS_CELLRESIZE) {
			if (resizeStartRow > 0) {
				if (row != resizeStartRow) {
					row = resizeStartRow;
				}
				Vector<Integer> willResizeRows = new Vector<Integer>();
				willResizeRows.add(new Integer(row));
				if (!control.m_selectedRows.isEmpty()) {
					int r1 = ((Integer) control.m_selectedRows.get(0))
							.intValue();
					int r2 = ((Integer) control.m_selectedRows
							.get(control.m_selectedRows.size() - 1)).intValue();
					int selectedStartRow = Math.min(r1, r2);
					int selectedEndRow = Math.max(r1, r2);
					if (row >= selectedStartRow && row <= selectedEndRow) {
						willResizeRows = control.m_selectedRows;
					}
				}
				IRowCell rc = control.cellSet.getRowCell(row);
				if (rc != null) {
					rc.setHeight(oldCellHeight);
					control.fireRowHeaderResized(willResizeRows, tmpHeight
							/ control.scale);
				}
			}
		} else {
			if (e.getX() > RowHeaderPanel.getHeaderW(control)) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					SplEditor editor = ControlUtils.extractSplEditor(control);
					if (editor != null && editor.expandRow(row)) {
						control.contentView.initCellLocations();
						((RowHeaderPanel) control.getRowHeaderPanel())
								.initRowLocations();
						Vector<Object> newAreas = new Vector<Object>();
						newAreas.add(new Area(row, (short) 1, row,
								(short) control.cellSet.getColCount()));
						editor.setSelectedAreas(newAreas);
						editor.resetEditor();
						control.setActiveCell(new CellLocation(row, 1));
						control.getContentPanel().revalidate();
						control.getRowHeaderPanel().revalidate();
						control.getColHeaderPanel().revalidate();
						control.repaint();
						return;
					}
				}
			}

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

	/**
	 * 鼠标点击事件
	 */
	public void mouseClicked(MouseEvent e) {
		// 双击行标题的格线，触发自动调整行高的功能
		if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
			int y = e.getY();
			int row = ControlUtils.lookupHeaderIndex(y, control.cellY,
					control.cellH);
			if (row < 1) {
				return;
			}
			if (y >= control.cellY[row] + control.cellH[row] - 2
					&& y <= control.cellY[row] + control.cellH[row]) {
				SplEditor editor = ControlUtils.extractSplEditor(control);
				if (editor != null) {
					Vector<Integer> rows = new Vector<Integer>();
					rows.add(new Integer(row));
					editor.selectedRows = rows;
					editor.adjustRowHeight();
				}
			}
		}
	}

	/**
	 * 鼠标退出事件
	 */
	public void mouseExited(MouseEvent e) {
	}

	/**
	 * 按住左键拖动鼠标时的处理
	 * 
	 * @param e 鼠标事件
	 */
	public void mouseDragged(MouseEvent e) {
		if (!editable) {
			return;
		}
		int y = e.getY();
		int row = ControlUtils.lookupHeaderIndex(y, control.cellY,
				control.cellH);
		if (control.status == GCSpl.STATUS_NORMAL) {
			if (row < 0) {
				return;
			}
			int start = Math.min(startSelectedRow, row);
			int end = Math.max(startSelectedRow, row);
			if (start <= 0) {
				return;
			}
			// 拖拽多行再返回时，行被多选
			control.m_selectedRows.clear();
			for (int i = start; i <= end; i++) {
				control.addSelectedRow(new Integer(i));
			}
			control.addSelectedArea(
					new Area(start, 1, end, control.cellSet.getColCount()),
					true);
			if (ControlUtils.scrollToVisible(control.getRowHeader(), control,
					row, (int) 0)) {
				Point p1 = control.getRowHeader().getViewPosition();
				Point p2 = control.getViewport().getViewPosition();
				p2.y = p1.y;
				control.getViewport().setViewPosition(p2);
			}
			control.repaint();
			control.fireRegionSelect(true);
		}
		if (control.status == GCSpl.STATUS_CELLRESIZE) {
			if (resizeStartRow > 0) {
				if (row != resizeStartRow) {
					row = resizeStartRow;
				}
				tmpHeight = tmpHeight + y - resizeStartY;
				resizeStartY = y;
				if (tmpHeight < 1) {
					tmpHeight = 1;
				}
				float newHeight = tmpHeight / control.scale;
				IRowCell rc = control.cellSet.getRowCell(row);
				if (rc != null)
					rc.setHeight(newHeight);
				control.getRowHeader().getView().repaint();

				// 拖拽过程不实时刷新内容，会导致文字的wrapBuffer急剧冗余
				control.getViewport().getView().repaint();
				ControlUtilsBase.clearWrapBuffer();
			}
		}
	}

	/**
	 * 鼠标移动时的处理
	 * 
	 * @param e 鼠标事件
	 */
	public void mouseMoved(MouseEvent e) {
		if (!editable) {
			return;
		}
		int y = e.getY();
		int row = ControlUtils.lookupHeaderIndex(y, control.cellY,
				control.cellH);
		if (row < 0) {
			control.status = GCSpl.STATUS_NORMAL;
			control.getRowHeader()
					.getView()
					.setCursor(
							Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		if (y >= control.cellY[row] + control.cellH[row] - 2
				&& y <= control.cellY[row] + control.cellH[row]) {
			control.status = GCSpl.STATUS_CELLRESIZE;
			control.getRowHeader()
					.getView()
					.setCursor(
							Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
		} else {
			control.status = GCSpl.STATUS_NORMAL;
			control.getRowHeader()
					.getView()
					.setCursor(
							Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	/**
	 * 显示右键弹出菜单
	 * 
	 * @param e
	 */
	void showPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			control.fireRightClicked(e, GC.SELECT_STATE_ROW);
		}
	}

	/**
	 * 键盘按键释放事件
	 */
	public void keyReleased(KeyEvent e) {
	}

	/**
	 * 键盘被按下的处理 若按下的是shift+方向键，相应改变当前选中的行
	 * 
	 * @param e 键盘事件
	 */
	public void keyPressed(KeyEvent e) {
		if (!editable) {
			return;
		}
		int key = e.getKeyCode();
		switch (key) {
		case KeyEvent.VK_DOWN:
			if (!e.isShiftDown()) {
				control.toDownCell();
				e.consume();
				return;
			}
			int start = ((Integer) control.m_selectedRows.get(0)).intValue();
			int end = ((Integer) control.m_selectedRows
					.get(control.m_selectedRows.size() - 1)).intValue();
			if (this.startSelectedRow == start) {
				end++;
			} else {
				start++;
			}
			if (end > control.cellSet.getRowCount()) {
				end = control.cellSet.getRowCount();
			}
			if (start < 1) {
				start = 1;
			}
			control.m_selectedCols.clear();
			control.m_cornerSelected = false;
			control.m_selectedRows.clear();
			for (int i = start; i <= end; i++) {
				control.addSelectedRow(new Integer(i));
			}
			control.addSelectedArea(new Area(start, (int) 1, end,
					(int) control.cellSet.getColCount()), true);
			if (ControlUtils.scrollToVisible(control.getRowHeader(), control,
					end, (int) 0)) {
				Point p1 = control.getRowHeader().getViewPosition();
				Point p2 = control.getViewport().getViewPosition();
				p2.y = p1.y;
				control.getViewport().setViewPosition(p2);
			}
			control.repaint();
			control.fireRegionSelect(true);
			e.consume();
			break;
		case KeyEvent.VK_UP:
			if (!e.isShiftDown()) {
				control.toUpCell();
				e.consume();
				return;
			}
			start = ((Integer) control.m_selectedRows.get(0)).intValue();
			end = ((Integer) control.m_selectedRows.get(control.m_selectedRows
					.size() - 1)).intValue();
			if (this.startSelectedRow == end) {
				start--;
			} else {
				end--;
			}
			if (end > control.cellSet.getRowCount()) {
				end = control.cellSet.getRowCount();
			}
			if (start < 1) {
				start = 1;
			}
			control.m_selectedCols.clear();
			control.m_cornerSelected = false;
			control.m_selectedRows.clear();
			for (int i = start; i <= end; i++) {
				control.addSelectedRow(new Integer(i));
			}
			control.addSelectedArea(new Area(start, (int) 1, end,
					(int) control.cellSet.getColCount()), true);
			if (ControlUtils.scrollToVisible(control.getRowHeader(), control,
					start, (int) 0)) {
				Point p1 = control.getRowHeader().getViewPosition();
				Point p2 = control.getViewport().getViewPosition();
				p2.y = p1.y;
				control.getViewport().setViewPosition(p2);
			}
			control.repaint();
			control.fireRegionSelect(true);
			e.consume();
			break;
		default:
			return;
		}
	}

	public void keyTyped(KeyEvent e) {
	}

}
