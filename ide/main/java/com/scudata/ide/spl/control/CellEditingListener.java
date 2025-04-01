package com.scudata.ide.spl.control;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.CellLocation;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.ToolBarPropertyBase;
import com.scudata.ide.spl.GCSpl;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.SPL;
import com.scudata.ide.spl.SheetSpl;

/**
 * 编辑时，在当前单元格获得焦点时的键盘监听器类
 */
public class CellEditingListener implements KeyListener {
	/** 编辑控件 */
	protected SplControl control;

	/** 内容面板 */
	protected ContentPanel cp;

	/**
	 * 是否按下CTRL键
	 */
	protected boolean isCtrlDown = false;

	/**
	 * 监听器构造函数
	 * 
	 * @param control 编辑控件
	 * @param panel   内容面板
	 */
	public CellEditingListener(SplControl control, ContentPanel panel) {
		this.control = control;
		this.cp = panel;
	}

	/**
	 * 键被释放时，向编辑器发送新输入的文本
	 * 
	 * @param e 键盘事件
	 */
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
			isCtrlDown = false;
		}
	}

	/**
	 * 键被按下时的处理函数，如果按下的是上下键、回车键或Ctrl+左右键，相应改变当前单元格
	 * 
	 * @param e 键盘事件
	 */
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		boolean isMatching = isMatching();
		switch (key) {
		case KeyEvent.VK_ENTER:
			if (!GV.isCellEditing) {// 来自工具栏命令
				stopMatch();
				return;
			}
			if (e.isAltDown()) {
				stopMatch();
				if (GVSpl.appSheet != null
						&& GVSpl.appSheet instanceof SheetSpl)
					((SheetSpl) GVSpl.appSheet).calcActiveCell(false);
				break;
			} else if (e.isControlDown()) {
				stopMatch();
				JTextComponent ta = getSource(e);
				int c = ta.getCaretPosition();
				try {
					String head = ta.getText(0, c);
					String end = ta.getText(c, ta.getText().length() - c);
					cp.setEditorText(head + "\n" + end);
					ta.requestFocus();
					ta.setCaretPosition(c + 1);
				} catch (BadLocationException ex) {
				} catch (IllegalArgumentException ex1) {
				}
			} else if (e.isShiftDown()) {
				stopMatch();
				// 接受内容、执行（不论是否有实质改动）、显示结果并自动钉住、不移动光标
				if (GVSpl.appSheet != null
						&& GVSpl.appSheet instanceof SheetSpl)
					((SheetSpl) GVSpl.appSheet).calcActiveCell();
				break;
			} else {
				if (isMatching) {
					keyPressed(key);
					break;
				}
				CellSetParser parser = new CellSetParser(control.cellSet);
				PgmNormalCell cell;
				int nextCol = -1;
				CellLocation cl = control.getActiveCell();
				if (cl == null)
					return;
				int curRow = cl.getRow();
				int curCol = cl.getCol();
				for (int c = curCol + 1; c <= control.cellSet.getColCount(); c++) {
					if (parser.isColVisible(c)) {
						cell = control.cellSet.getPgmNormalCell(curRow, c);
						if (!cell.isNoteBlock() && !cell.isNoteCell()) {
							if (StringUtils.isValidString(parser.getDispText(
									curRow, c))) {
								nextCol = c;
								break;
							}
						}
					}
				}
				if (nextCol > 0) {
					control.getContentPanel().submitEditor();
					control.getContentPanel().revalidate();
					control.scrollToArea(control.setActiveCell(
							new CellLocation(curRow, nextCol), true));
				} else {
					if (control.getActiveCell() != null) {
						CellSet ics = control.getCellSet();
						if (curRow == ics.getRowCount()) {
							control.getContentPanel().submitEditor();
							control.getContentPanel().revalidate();
							appendOneRow();
						}
					}
					control.scrollToArea(control.toDownCell());
				}
				if (GVSpl.panelValue != null)
					GVSpl.panelValue.tableValue.setLocked(false);
			}
			break;
		case KeyEvent.VK_ESCAPE:
			if (control.getActiveCell() == null) {
				break;
			}
			if (isMatching) {
				stopMatch();
			}
			JTextComponent ta = getSource(e);
			NormalCell nc = (NormalCell) control.getCellSet().getCell(
					control.getActiveCell().getRow(),
					control.getActiveCell().getCol());
			String value = nc.getExpString();
			value = value == null ? GCSpl.NULL : value;
			ta.setText(value);
			control.getContentPanel().reloadEditorText();
			control.fireEditorInputing(value);
			cp.requestFocus();
			break;
		case KeyEvent.VK_TAB:
			if (e.isShiftDown()) {
				if (isMatching) {
					stopMatch();
				}
				control.scrollToArea(control.toLeftCell());
			} else if (e.isControlDown()) {
				// CTRL-TAB解释成切换当前活动SHEET（参照EXCEL）
				if (isMatching) {
					stopMatch();
				}
				if (GVSpl.appFrame != null && GVSpl.appFrame instanceof SPL)
					((SPL) GVSpl.appFrame).showNextSheet(isCtrlDown);
				isCtrlDown = true;
			} else {
				if (isMatching) {
					break;
				}
				if (control.getActiveCell() != null) {
					control.getContentPanel().submitEditor();
				}
				int curCol = control.getActiveCell().getCol();
				CellSet ics = control.getCellSet();
				if (curCol == ics.getColCount()) {
					control.getContentPanel().submitEditor();
					appendOneCol();
				}
				control.scrollToArea(control.toRightCell());
			}
			break;
		case KeyEvent.VK_RIGHT:
			JTextComponent tar = getSource(e);
			if (tar.getText() == null || tar.getText().equals("")) {
				control.scrollToArea(control.toRightCell());
			} else {
				return;
			}
			break;
		case KeyEvent.VK_LEFT:
			JTextComponent tal = getSource(e);
			if (tal.getText() == null || tal.getText().equals("")) {
				control.scrollToArea(control.toLeftCell());
			} else {
				return;
			}
			break;
		case KeyEvent.VK_UP:
			if (isMatching) {
				keyPressed(key);
				break;
			}
			if (e.isAltDown()) {
				if (e.getSource() == GV.toolBarProperty.getWindowEditor()) {// 来自工具栏命令
					e.consume();
				}
				return;
			}
			JTextComponent tau = getSource(e);
			if (tau.getText() == null || tau.getText().equals("")) {
				control.scrollToArea(control.toUpCell());
			} else {
				return;
			}
			break;
		case KeyEvent.VK_DOWN:
			if (isMatching) {
				keyPressed(key);
				break;
			}
			if (e.isAltDown()) {
				if (e.getSource() == GV.toolBarProperty.getWindowEditor()) {// 来自工具栏命令
					e.consume();
				}
				return;
			}
			JTextComponent tad = getSource(e);
			if (tad.getText() == null || tad.getText().equals("")) {
				control.scrollToArea(control.toDownCell());
				// } else if (e.isShiftDown()) {
				// startMatch(tad);
				// return;
			} else {
				return;
			}
			break;
		case KeyEvent.VK_F2: {
			if (control.getActiveCell() == null) {
				break;
			}
			if (isMatching) {
				stopMatch();
			}
			if (GVSpl.toolBarProperty != null)
				GVSpl.toolBarProperty.getWindowEditor().requestFocus();
			break;
		}
		case KeyEvent.VK_Z: {
			if (e.isControlDown() && control.getActiveCell() != null) {
				if (isMatching) {
					stopMatch();
				}
				control.getContentPanel().undoEditor();
			}
			break;
		}
		default:
			SplEditor editor = ControlUtils.extractSplEditor(control);
			if (e.getKeyCode() == KeyEvent.VK_C && e.isAltDown()
					&& !e.isControlDown()) {
				if (e.isShiftDown()) {
					if (editor != null && editor.canCopyPresent()) {
						editor.copyPresent();
						break;
					}
				} else {
					if (editor != null)
						editor.altC();
					break;
				}
			}
			if (e.getKeyCode() == KeyEvent.VK_V && e.isAltDown()
					&& !e.isControlDown()) {
				if (!e.isShiftDown()) {
					if (GM.canPaste())
						if (editor != null)
							editor.altV();
					break;
				}
			}
			return;
		}
		e.consume();
	}

	/**
	 * 键盘按下事件
	 */
	public void keyTyped(KeyEvent e) {
	}

	/**
	 * 取源组件
	 * 
	 * @param e
	 * @return
	 */
	protected JTextComponent getSource(KeyEvent e) {
		Object src = e.getSource();
		if (src instanceof JTextComponent) {
			return (JTextComponent) src;
		}
		if (src instanceof ToolBarPropertyBase) {
			return ((ToolBarPropertyBase) src).getWindowEditor();
		}
		return null;
	}

	protected boolean isMatching() {
		return false;
	}

	protected void keyPressed(int keyCode) {
	}

	protected void stopMatch() {
	}

	protected void appendOneRow() {
		SplEditor editor = ControlUtils.extractSplEditor(control);
		if (editor != null)
			editor.appendRows(1);
	}

	protected void appendOneCol() {
		SplEditor editor = ControlUtils.extractSplEditor(control);
		if (editor != null)
			editor.appendCols(1);
	}
}
