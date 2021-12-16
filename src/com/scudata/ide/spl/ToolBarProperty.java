package com.scudata.ide.spl;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.Area;
import com.scudata.common.CellLocation;
import com.scudata.common.IByteMap;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.ToolBarPropertyBase;
import com.scudata.ide.common.control.CellRect;
import com.scudata.ide.spl.control.CellEditingListener;
import com.scudata.ide.spl.control.ContentPanel;
import com.scudata.ide.spl.control.SplControl;
import com.scudata.ide.spl.control.SplEditor;

/**
 * 属性工具栏
 *
 */
public class ToolBarProperty extends ToolBarPropertyBase {
	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 */
	public ToolBarProperty() {
		super();
	}

	/**
	 * 设置编辑框的文本
	 */
	public void setTextEditorText(String newText) {
		setTextEditorText(newText, false);
	}

	/**
	 * 设置编辑框的文本
	 * 
	 * @param newText   新文本
	 * @param isRefresh 是否刷新
	 */
	public void setTextEditorText(String newText, boolean isRefresh) {
		if (textEditorFont != GC.font) {
			textEditor.setFont(GC.font);
		}

		if (!isRefresh && !GV.isCellEditing) {
			return;
		}
		try {
			preventAction = true;
			textEditor.setPreventChange(true);
			try {
				textEditor.setText(newText);
			} catch (Exception e) {
				// 第三方文本框控件会打印异常，但是显示不会出现错误。
				// 先把异常信息屏蔽了。
			}
			textEditor.initRefCells(false);
			resetTextWindow();
		} finally {
			preventAction = false;
			textEditor.setPreventChange(false);
		}
	}

	/**
	 * 初始化
	 */
	public void init() {
		if (GVSpl.splEditor == null) {
			return;
		}
		SplControl control = GVSpl.splEditor.getComponent();
		ContentPanel cp = control.getContentPanel();
		CellEditingListener editListener = new CellEditingListener(control, cp);
		KeyListener[] kls = textEditor.getKeyListeners();
		if (kls != null) {
			int len = kls.length;
			for (int i = len - 1; i >= 0; i--) {
				if (kls[i] instanceof CellEditingListener) {
					textEditor.removeKeyListener(kls[i]);
				}
			}
		}
		textEditor.addKeyListener(editListener);
	}

	/**
	 * 刷新Cell属性到工具栏
	 * 
	 * @param selectState byte 暂时没用
	 * @param values      IByteMap
	 */
	public void refresh(byte selectState, IByteMap values) {
		if (GVSpl.cmdSender == this) {
			return;
		}
		preventAction = true;
		initProperties();
		setEnabled(true);
		if (values == null || values.size() == 0) {
			preventAction = false;
			return;
		}

		if (GVSpl.splEditor != null) {
			CellRect rect = GVSpl.splEditor.getSelectedRect();
			if (rect != null) {
				String rectText = GMSpl.getCellID(rect.getBeginRow(), rect.getBeginCol());
				if (rect.getRowCount() > 1 || rect.getColCount() > 1) {
					rectText += "-" + GMSpl.getCellID(rect.getEndRow(), rect.getEndCol());
				}
				cellName.setText(rectText);
			}
		}

		Object o;
		o = values.get(AtomicCell.CELL_EXP);
		if (StringUtils.isValidString(o)) {
			setTextEditorText((String) o, true);
		} else {
			setTextEditorText("", true);
		}

		preventAction = false;
		this.selectState = selectState;
	}

	/**
	 * 当输入cellExp时,通知改变
	 * 
	 * @return 无
	 */
	public void textEdited(KeyEvent e) {
		if (preventAction) {
			return;
		}
		GVSpl.cmdSender = this;
		GV.isCellEditing = false;
		try {
			String text = textEditor.getText();
			resetTextWindow();
			GVSpl.splEditor.setEditingText(text);
		} catch (Exception ex) {
		}
	}

	/**
	 * 提交编辑
	 */
	public void submitEditor(String newText, byte forward) {
		SplControl control = GVSpl.splEditor.getComponent();
		control.fireCellTextInput(control.getActiveCell(), newText);

		switch (forward) {
		case UPWARD:
			control.scrollToArea(control.toUpCell());
			break;
		case DOWNWARD:
			control.scrollToArea(control.toDownCell());
			break;
		}
	}

	/**
	 * 选择了编辑框
	 */
	public void editorSelected() {
		if (GVSpl.splEditor == null)
			return;
		if (GVSpl.splEditor.getComponent() == null)
			return;
		ContentPanel cp = GVSpl.splEditor.getComponent().getContentPanel();
		if (cp.getEditor() == null || !cp.getEditor().isVisible()) {
			cp.initEditor(ContentPanel.MODE_SHOW);
		}
		GV.isCellEditing = false;
	}

	/**
	 * 增加文本
	 * 
	 * @param text 文本
	 */
	public void addText(String text) {
		if (!this.isEnabled()) {
			return;
		}
		GM.addText(textEditor, text);
		textEdited(null);
	}

	/**
	 * 取上下文
	 */
	public Context getContext() {
		return GMSpl.prepareParentContext();
	}

	/**
	 * 设置工具栏展开
	 */
	protected void setToolBarExpand() {
		((SPL) GV.appFrame).setToolBarExpand();
	}

	/**
	 * 设置当前格
	 */
	protected void setActiveCell(int row, int col) {
		SplEditor editor = GVSpl.splEditor;
		SplControl control = editor.getComponent();
		control.clearSelectedAreas();
		control.setActiveCell(new CellLocation(row, col));
		ContentPanel cp = control.contentView;
		cp.rememberedRow = row;
		cp.rememberedCol = col;
		Area a = new Area(row, col, row, col);
		a = control.setActiveCell(new CellLocation(row, col));
		control.addSelectedArea(a, false);
		control.repaint();
		cp.requestFocus();
		if (control.cellSet.isAutoCalc()) {
			PgmNormalCell nc = (PgmNormalCell) control.cellSet.getCell(row, col);
			if (nc != null)
				GVSpl.panelValue.tableValue.setValue1(nc.getValue(), nc.getCellId());
		}
		editor.selectedRects.clear();
		editor.selectedRects.add(new CellRect(a));
		editor.selectedCols.clear();
		editor.selectedRows.clear();
		editor.selectState = GCSpl.SELECT_STATE_CELL;
		editor.getSplListener().selectStateChanged(editor.selectState, false);
	}

	/**
	 * 取当前格名称
	 */
	protected String getActiveCellId() {
		SplControl control = GVSpl.splEditor.getComponent();
		CellLocation cl = control.getActiveCell();
		if (cl == null)
			return null;
		return GM.getCellID(cl.getRow(), cl.getCol());
	}

	/**
	 * 取最大的格子坐标
	 */
	protected CellLocation getMaxCellLocation() {
		SplControl control = GVSpl.splEditor.getComponent();
		return new CellLocation(control.cellSet.getRowCount(), control.cellSet.getColCount());
	}

	/**
	 * 按下TAB键
	 */
	public void tabPressed() {
		SplControl control = GVSpl.splEditor.getComponent();
		CellLocation cl = control.getActiveCell();
		if (cl == null)
			return;
		int curCol = cl.getCol();
		if (curCol == control.cellSet.getColCount()) {
			GVSpl.splEditor.appendCols(1);
		}
		control.scrollToArea(control.toRightCell());
	}

	/**
	 * 编辑取消了
	 */
	public void editCancel() {
		SplControl control = GVSpl.splEditor.getComponent();
		NormalCell nc = (NormalCell) control.getCellSet().getCell(control.getActiveCell().getRow(),
				control.getActiveCell().getCol());
		String value = nc.getExpString();
		value = value == null ? GCSpl.NULL : value;
		try {
			textEditor.setText(value);
			textEdited(null);
		} catch (Exception ex) {
		}
	}
}
