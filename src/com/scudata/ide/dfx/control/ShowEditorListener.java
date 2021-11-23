package com.scudata.ide.dfx.control;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import com.scudata.common.CellLocation;

/**
 * 鼠标选中单元格时显示单元格编辑器的监听器
 */
public class ShowEditorListener implements MouseListener {
	/** 网格内容面板 */
	private ContentPanel cp;

	/**
	 * 监听器构造函数
	 * 
	 * @param panel
	 *            网格内容面板
	 */
	public ShowEditorListener(ContentPanel panel) {
		this.cp = panel;
	}

	/**
	 * 鼠标按下事件处理，显示单元格编辑控件
	 * 
	 * @param e
	 */
	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		CellLocation pos = ControlUtils.lookupCellPosition(x, y, cp);
		if (pos == null) {
			return;
		}
		cp.getControl().setActiveCell(pos);
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

}
