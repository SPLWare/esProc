package com.scudata.ide.spl.control;

import java.awt.Point;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.common.Area;
import com.scudata.common.CellLocation;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.control.TransferableObject;
import com.scudata.ide.spl.AtomicCell;
import com.scudata.ide.spl.GMSpl;

/**
 * 网格鼠标拖拽监听器
 *
 */
public class EditDropListener implements DropTargetListener {
	/**
	 * 之前选择的坐标
	 */
	private CellLocation oldPos = new CellLocation(0, (int) 0);

	/**
	 * 构造函数
	 */
	public EditDropListener() {
	}

	/**
	 * 拖拽进入了区域
	 */
	public void dragEnter(DropTargetDragEvent dtde) {
		ContentPanel cp = (ContentPanel) dtde.getDropTargetContext()
				.getComponent();
		cp.submitEditor();
		if (cp.getEditor() != null) {
			cp.getEditor().setVisible(false);
		}
		cp.getControl().setActiveCell(null);
	}

	/**
	 * 在拖拽区域内
	 */
	public void dragOver(DropTargetDragEvent dtde) {
		Point p = dtde.getLocation();
		ContentPanel cp = (ContentPanel) dtde.getDropTargetContext()
				.getComponent();
		if (cp.getEditor() != null) {
			cp.getEditor().setVisible(false);
		}
		CellLocation pos = ControlUtils.lookupCellPosition(p.x, p.y, cp);
		if (pos == null) {
			return;
		}
		EditControl control = (EditControl) cp.getControl();
		int row = pos.getRow();
		int col = pos.getCol();
		if (row == oldPos.getRow() && col == oldPos.getCol()) {
			return;
		} else {
			oldPos = pos;
		}
		control.setActiveCell(null);
		control.addSelectedArea(new Area(row, col, row, col), true);
		control.m_cornerSelected = false;
		control.m_selectedCols.clear();
		control.m_selectedRows.clear();
		control.repaint();
	}

	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	public void dragExit(DropTargetEvent dte) {
	}

	/**
	 * 鼠标释放，执行拖拽事件
	 */
	public void drop(DropTargetDropEvent dtde) {
		Point p = dtde.getLocation();
		ContentPanel cp = (ContentPanel) dtde.getDropTargetContext()
				.getComponent();
		CellLocation pos = ControlUtils.lookupCellPosition(p.x, p.y, cp);
		if (pos == null) {
			return;
		}
		EditControl control = (EditControl) cp.jsp;
		Object data = null;
		try {
			data = dtde.getTransferable().getTransferData(
					TransferableObject.objectFlavor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (data == null) {
			return;
		}
		NormalCell nc = (NormalCell) control.getCellSet().getCell(pos.getRow(),
				pos.getCol());
		AtomicCell ac = new AtomicCell(control, nc);
		if (StringUtils.isValidString(data) && ((String) data).startsWith("=")) {
			String exp = GM.getOptionTrimChar0String((String) data);
			ac.setProperty(AtomicCell.CELL_EXP);
			ac.setValue(exp);
			// nc.setExpString(exp);
		} else {
			ac.setProperty(AtomicCell.CELL_VALUE);
			ac.setValue(GM.getOptionTrimChar0Value(data));
			// nc.setValue(GM.getOptionTrimChar0Value(data));
		}
		SplEditor editor = ControlUtils.extractSplEditor(control);
		editor.executeCmd(ac);

		control.setActiveCell(pos);
		control.setSelectedArea(new Area(pos.getRow(), pos.getCol(), pos
				.getRow(), pos.getCol()));
		control.m_selectedCols.clear();
		control.m_selectedRows.clear();
		control.m_cornerSelected = false;
		control.fireRegionSelect(true);
		control.repaint();

		// Drop激活保存按钮
		GMSpl.enableSave();
	}
}
