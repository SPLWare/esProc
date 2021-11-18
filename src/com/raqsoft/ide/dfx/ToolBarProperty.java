package com.raqsoft.ide.dfx;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import com.raqsoft.cellset.datamodel.NormalCell;
import com.raqsoft.cellset.datamodel.PgmNormalCell;
import com.raqsoft.common.Area;
import com.raqsoft.common.CellLocation;
import com.raqsoft.common.IByteMap;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.Context;
import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.common.ToolBarPropertyBase;
import com.raqsoft.ide.common.control.CellRect;
import com.raqsoft.ide.dfx.control.CellEditingListener;
import com.raqsoft.ide.dfx.control.ContentPanel;
import com.raqsoft.ide.dfx.control.DfxControl;
import com.raqsoft.ide.dfx.control.DfxEditor;

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
	 * @param newText
	 *            新文本
	 * @param isRefresh
	 *            是否刷新
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
		if (GVDfx.dfxEditor == null) {
			return;
		}
		DfxControl control = GVDfx.dfxEditor.getComponent();
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
	 * @param selectState
	 *            byte 暂时没用
	 * @param values
	 *            IByteMap
	 */
	public void refresh(byte selectState, IByteMap values) {
		if (GVDfx.cmdSender == this) {
			return;
		}
		preventAction = true;
		initProperties();
		setEnabled(true);
		if (values == null || values.size() == 0) {
			preventAction = false;
			return;
		}

		if (GVDfx.dfxEditor != null) {
			CellRect rect = GVDfx.dfxEditor.getSelectedRect();
			if (rect != null) {
				String rectText = GMDfx.getCellID(rect.getBeginRow(),
						rect.getBeginCol());
				if (rect.getRowCount() > 1 || rect.getColCount() > 1) {
					rectText += "-"
							+ GMDfx.getCellID(rect.getEndRow(),
									rect.getEndCol());
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
		GVDfx.cmdSender = this;
		GV.isCellEditing = false;
		try {
			String text = textEditor.getText();
			resetTextWindow();
			GVDfx.dfxEditor.setEditingText(text);
		} catch (Exception ex) {
		}
	}

	/**
	 * 提交编辑
	 */
	public void submitEditor(String newText, byte forward) {
		DfxControl control = GVDfx.dfxEditor.getComponent();
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
		if (GVDfx.dfxEditor == null)
			return;
		if (GVDfx.dfxEditor.getComponent() == null)
			return;
		ContentPanel cp = GVDfx.dfxEditor.getComponent().getContentPanel();
		if (cp.getEditor() == null || !cp.getEditor().isVisible()) {
			cp.initEditor(ContentPanel.MODE_SHOW);
		}
		GV.isCellEditing = false;
	}

	/**
	 * 增加文本
	 * 
	 * @param text
	 *            文本
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
		return GMDfx.prepareParentContext();
	}

	/**
	 * 设置工具栏展开
	 */
	protected void setToolBarExpand() {
		((DFX) GV.appFrame).setToolBarExpand();
	}

	/**
	 * 设置当前格
	 */
	protected void setActiveCell(int row, int col) {
		DfxEditor editor = GVDfx.dfxEditor;
		DfxControl control = editor.getComponent();
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
		if (control.dfx.isAutoCalc()) {
			PgmNormalCell nc = (PgmNormalCell) control.dfx.getCell(row, col);
			if (nc != null)
				GVDfx.panelValue.tableValue.setValue1(nc.getValue(),
						nc.getCellId());
		}
		editor.selectedRects.clear();
		editor.selectedRects.add(new CellRect(a));
		editor.selectedCols.clear();
		editor.selectedRows.clear();
		editor.selectState = GCDfx.SELECT_STATE_CELL;
		editor.getDFXListener().selectStateChanged(editor.selectState, false);
	}

	/**
	 * 取当前格名称
	 */
	protected String getActiveCellId() {
		DfxControl control = GVDfx.dfxEditor.getComponent();
		CellLocation cl = control.getActiveCell();
		if (cl == null)
			return null;
		return GM.getCellID(cl.getRow(), cl.getCol());
	}

	/**
	 * 取最大的格子坐标
	 */
	protected CellLocation getMaxCellLocation() {
		DfxControl control = GVDfx.dfxEditor.getComponent();
		return new CellLocation(control.dfx.getRowCount(),
				control.dfx.getColCount());
	}

	/**
	 * 按下TAB键
	 */
	public void tabPressed() {
		DfxControl control = GVDfx.dfxEditor.getComponent();
		CellLocation cl = control.getActiveCell();
		if (cl == null)
			return;
		int curCol = cl.getCol();
		if (curCol == control.dfx.getColCount()) {
			GVDfx.dfxEditor.appendCols(1);
		}
		control.scrollToArea(control.toRightCell());
	}

	/**
	 * 编辑取消了
	 */
	public void editCancel() {
		DfxControl control = GVDfx.dfxEditor.getComponent();
		NormalCell nc = (NormalCell) control.getCellSet().getCell(
				control.getActiveCell().getRow(),
				control.getActiveCell().getCol());
		String value = nc.getExpString();
		value = value == null ? GCDfx.NULL : value;
		try {
			textEditor.setText(value);
			textEdited(null);
		} catch (Exception ex) {
		}
	}
}
