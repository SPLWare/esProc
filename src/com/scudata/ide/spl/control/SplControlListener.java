package com.scudata.ide.spl.control;

import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.text.JTextComponent;

import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.common.Area;
import com.scudata.common.CellLocation;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.IAtomicCmd;
import com.scudata.ide.common.control.CellRect;
import com.scudata.ide.spl.AtomicCell;
import com.scudata.ide.spl.GCSpl;

/**
 * 网格控件的监听器
 *
 */
public class SplControlListener implements EditorListener {
	/**
	 * 网格控件编辑器
	 */
	SplEditor editor;

	/**
	 * 构造函数
	 * 
	 * @param editor 网格控件编辑器
	 */
	public SplControlListener(SplEditor editor) {
		this.editor = editor;
	}

	/**
	 * 取网格控件编辑器
	 * 
	 * @return
	 */
	public SplEditor getEditor() {
		return editor;
	}

	/**
	 * 右键点击事件
	 */
	public void rightClicked(MouseEvent e, int clickPlace) {
		editor.getSplListener().rightClicked(e.getComponent(), e.getX(), e.getY());
	}

	/**
	 * 选择区域
	 */
	public void regionsSelect(Vector<Object> vectRegion, Vector<Integer> selectedRows, Vector<Integer> selectedColumns,
			boolean selectedAll, boolean keyEvent) {

		editor.selectedRects.clear();
		for (int i = 0; i < vectRegion.size(); i++) {
			Area a = (Area) vectRegion.get(i);
			if (a == null) {
				continue;
			}
			editor.selectedRects.add(new CellRect(a));
		}
		editor.selectedCols = selectedColumns;
		editor.selectedRows = selectedRows;
		if (vectRegion.isEmpty()) {
			editor.selectState = GCSpl.SELECT_STATE_NONE;
		} else if (selectedAll) {
			editor.selectState = GCSpl.SELECT_STATE_DM;
		} else if (selectedColumns.size() > 0) {
			editor.selectState = GCSpl.SELECT_STATE_COL;
		} else if (selectedRows.size() > 0) {
			editor.selectState = GCSpl.SELECT_STATE_ROW;
		} else {
			editor.selectState = GCSpl.SELECT_STATE_CELL;
		}
		editor.getSplListener().selectStateChanged(editor.selectState, keyEvent);
	}

	/**
	 * 列宽变化了
	 */
	public boolean columnWidthChange(Vector<Integer> vectColumn, float nWidth) {
		// 行高列宽改变时会产生大量冗余的wrapString Key，清空一下
		ControlUtils.clearWrapBuffer();
		editor.selectedCols = vectColumn;
		editor.setColumnWidth(nWidth);
		return true;
	}

	/**
	 * 行高变化了
	 */
	public boolean rowHeightChange(Vector<Integer> vectRow, float nHeight) {
		// 行高列宽改变时会产生大量冗余的wrapString Key，清空一下
		ControlUtils.clearWrapBuffer();
		editor.selectedRows = vectRow;
		editor.setRowHeight(nHeight);
		return true;
	}

	/**
	 * 区域移动事件
	 * 
	 * @param area       区域
	 * @param nRowPos    行号
	 * @param nColumnPos 列号
	 * @return
	 */
	public boolean cellRegionMove(Area area, int nRowPos, int nColumnPos) {
		return true;
	}

	/**
	 * 区域粘帖消息
	 * 
	 * @param area       粘贴的表格区域
	 * @param nRowPos    粘贴的行位置
	 * @param nColumnPos 粘贴的列位置
	 * @return ture 消息已被处理，false 消息未被处理
	 */
	public boolean cellRegionPaste(Area area, int nRowPos, int nColumnPos) {
		return true;
	}

	/**
	 * 区域扩展消息（尚没实现）
	 * 
	 * @param area          扩展的表格区域
	 * @param nColumnExpand 列扩展数(正，向右扩展；负，向左扩展；0，不扩展)
	 * @param nRowExpand    行扩展数(正，向下扩展；负，向上扩展；0，不扩展)
	 * @return ture 消息已被处理，false 消息未被处理
	 */
	public boolean cellRegionExpand(Area area, int nColumnExpand, int nRowExpand) {
		return true;
	}

	/**
	 * 区域缩减消息（尚没实现）
	 * 
	 * @param area          缩减的表格区域
	 * @param nRowShrink    行缩减数（正，缩去的行数；0，不缩减）
	 * @param nColumnShrink 列缩减数（正，缩去的列数；0，不缩减）
	 * @return ture 消息已被处理，false 消息未被处理
	 */
	public boolean cellRegionShrink(Area area, int nColumnShrink, int nRowShrink) {
		return true;
	}

	/**
	 * 取列高变化的原子命令
	 * 
	 * @param control 网格控件
	 * @param row     行号
	 * @param col     列号
	 * @param newText 文本
	 * @return
	 */
	public static AtomicCell getCellHeightCmd(SplControl control, int row, int col, String newText) {
		// 冲出单元格显示时不自动调整行高，希望看到的就是一行代码
		if (!ConfigOptions.bAutoSizeRowHeight.booleanValue()) {
			return null;
		}
		CellSet ics = control.cellSet;
		float w = control.getContentPanel().getEditableWidth(newText, row, col);
		float h = ics.getRowCell(row).getHeight();
		float textH = ControlUtils.getStringHeight(newText, w, GC.font) + 10;
		if (h < textH) {
			AtomicCell rac = new AtomicCell(control, ics.getRowCell(row));
			rac.setProperty(AtomicCell.ROW_HEIGHT);
			rac.setValue(new Float(textH));
			return rac;
		}
		return null;
	}

	/**
	 * 表格文本编辑消息
	 * 
	 * @param row     编辑的行号
	 * @param col     编辑的列号
	 * @param strText 编辑后的文本
	 * @return ture 消息已被处理，false 消息未被处理
	 */
	public boolean cellTextInput(int row, int col, String strText) {
		CellSet ics = editor.getComponent().cellSet;
		strText = strText != null ? strText : "";
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		NormalCell nc = (NormalCell) ics.getCell(row, (int) col);
		AtomicCell ac = new AtomicCell(editor.getComponent(), nc);
		ac.setProperty(AtomicCell.CELL_EXP);
		if (StringUtils.isValidString(strText)) {
			ac.setValue(strText);
		} else {
			ac.setValue(null);
		}
		cmds.add(ac);

		ac = getCellHeightCmd(editor.getComponent(), row, (int) col, strText);
		if (ac != null) {
			cmds.add(ac);
		}
		editor.executeCmd(cmds);
		return true;
	}

	public void editorInputing(String text) {
	}

	/**
	 * 鼠标双击事件
	 * 
	 * @param e MouseEvent
	 */
	public void doubleClicked(MouseEvent e) {
		if (editor.getComponent() == null)
			return;
		CellLocation pos = editor.getComponent().getActiveCell();
		if (pos != null) {
			JTextComponent textEditor = editor.getComponent().getEditor();
			if (textEditor == null || !textEditor.isEditable())
				return;
			editor.textEditor();
		}
	}

	/**
	 * 鼠标移动
	 * 
	 * @param row 行
	 * @param col 列
	 */
	public void mouseMove(int row, int col) {
	}

	/**
	 * 滚动条移动事件
	 */
	public void scrollBarMoved() {

	}
}
