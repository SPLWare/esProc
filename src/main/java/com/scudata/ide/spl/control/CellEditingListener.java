package com.scudata.ide.spl.control;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.CellLocation;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Param;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.expression.CSVariable;
import com.scudata.expression.ElementRef;
import com.scudata.expression.Expression;
import com.scudata.expression.FieldId;
import com.scudata.expression.FieldRef;
import com.scudata.expression.FunctionLib;
import com.scudata.expression.Node;
import com.scudata.expression.VarParam;
import com.scudata.expression.operator.DotOperator;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.ToolBarPropertyBase;
import com.scudata.ide.common.control.ControlUtilsBase;
import com.scudata.ide.common.control.JWindowNames;
import com.scudata.ide.spl.GCSpl;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.SPL;
import com.scudata.ide.spl.SheetSpl;
import com.scudata.util.EnvUtil;

/**
 * 编辑时，在当前单元格获得焦点时的键盘监听器类
 */
public class CellEditingListener implements KeyListener {
	/** 编辑控件 */
	private SplControl control;

	/** 内容面板 */
	private ContentPanel cp;

	/**
	 * 是否按下CTRL键
	 */
	private boolean isCtrlDown = false;

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
		final JTextComponent jtext = getSource(e);
		if (jtext == null)
			return;
		if (!Character.isDefined(e.getKeyChar())) {
			return;
		}
		if (e.isControlDown() || e.isAltDown()) {
			return;
		}
		int key = e.getKeyCode();
		if (e.isShiftDown()) {
			if (!e.isActionKey() && key != KeyEvent.VK_PERIOD) {
				matchKeyWord(jtext);
			}
			return;
		}
		if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER) {
			return;
		}
		control.fireEditorInputing(jtext.getText());
		if (key == KeyEvent.VK_PERIOD) {
			startMatch(jtext);
			return;
		}
		matchKeyWord(jtext);
	}

	private boolean isFuncMatchEnabled = false;

	/**
	 * 匹配关键字
	 * 
	 * @param jtext
	 */
	private void matchKeyWord(JTextComponent jtext) {
		if (GVSpl.matchWindow != null) {
			String text = jtext.getText();
			int caret = jtext.getCaretPosition();
			int dot = GVSpl.matchWindow.getDot();
			if (caret < dot || dot > text.length()) {
				stopMatch();
				return;
			}
			boolean isPeriod = GVSpl.matchWindow.isPeriod();
			int start1 = dot, start2 = dot;
			if (!isPeriod) {
				start1--;
			}
			int end = text.length();
			for (int i = start1 + 1; i < text.length(); i++) {
				char c = text.charAt(i);
				if (KeyWord.isSymbol(c)) {
					end = i;
					break;
				}
			}
			String name = text.substring(start2, end);
			name = name.trim();
			if (GVSpl.matchWindow != null) {
				boolean isSearched = GVSpl.matchWindow.searchName(name);
				if (!isSearched) {
					stopMatch();
				}
			}
		}
	}

	/**
	 * 键被按下时的处理函数，如果按下的是上下键、回车键或Ctrl+左右键，相应改变当前单元格
	 * 
	 * @param e 键盘事件
	 */
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		boolean isMatching = GVSpl.matchWindow != null
				&& GVSpl.matchWindow.isVisible();
		switch (key) {
		case KeyEvent.VK_ENTER:
			if (!GV.isCellEditing) {// 来自工具栏命令
				stopMatch();
				return;
			}
			if (e.isAltDown()) {
				stopMatch();
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
				((SheetSpl) GVSpl.appSheet).calcActiveCell();
				break;
			} else {
				if (isMatching) {
					if (GVSpl.matchWindow != null) {
						GVSpl.matchWindow.selectName();
					}
					isMatching = false;
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
							SplEditor editor = ControlUtils
									.extractSplEditor(control);
							control.getContentPanel().revalidate();
							editor.appendRows(1);
						}
					}
					control.scrollToArea(control.toDownCell());
				}

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
					ControlUtils.extractSplEditor(control).appendCols(1);
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
				if (GVSpl.matchWindow != null) {
					GVSpl.matchWindow.selectBefore();
				}
			}
			if (e.isAltDown()) {
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
				if (GVSpl.matchWindow != null) {
					GVSpl.matchWindow.selectNext();
				}
			}
			if (e.isAltDown()) {
				return;
			}
			JTextComponent tad = getSource(e);
			if (tad.getText() == null || tad.getText().equals("")) {
				control.scrollToArea(control.toDownCell());
			} else if (e.isShiftDown()) {
				startMatch(tad);
				return;
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
					if (editor.canCopyPresent()) {
						editor.copyPresent();
						break;
					}
				} else {
					editor.altC();
					break;
				}
			}
			if (e.getKeyCode() == KeyEvent.VK_V && e.isAltDown()
					&& !e.isControlDown()) {
				if (!e.isShiftDown()) {
					if (GM.canPaste())
						editor.altV();
					break;
				}
			}
			return;
		}
		e.consume();
	}

	/**
	 * 开始匹配
	 * 
	 * @param jtext
	 * @return
	 */
	private boolean startMatch(final JTextComponent jtext) {
		if (GVSpl.matchWindow != null) {
			stopMatch();
		}
		GVSpl.matchWindow = null;
		try {
			if (jtext == null)
				return false;
			CellLocation cl = control.getActiveCell();
			PgmNormalCell activeCell = control.cellSet.getPgmNormalCell(
					cl.getRow(), cl.getCol());
			switch (activeCell.getType()) {
			case PgmNormalCell.TYPE_BLANK_CELL:
			case PgmNormalCell.TYPE_CONST_CELL:
			case PgmNormalCell.TYPE_NOTE_CELL:
			case PgmNormalCell.TYPE_NOTE_BLOCK:
				return false;
			}
			String text = jtext.getText();
			if (!StringUtils.isValidString(text)) {
				return false;
			}
			final int p = jtext.getCaretPosition();
			if (p <= 0)
				return false;
			String preStr = text.substring(0, p);
			while (preStr != null
					&& (preStr.startsWith("=") || preStr.startsWith(">"))) {
				preStr = preStr.substring(1);
			}
			if (!StringUtils.isValidString(preStr)) {
				return false;
			}
			boolean isPeriod = false;
			char preChar = text.charAt(p - 1);
			if (preChar == '.') {
				isPeriod = true;
				preStr = preStr.substring(0, preStr.length() - 1);
			}
			Context ctx = control.cellSet.getContext();
			Object val = null;
			if (isPeriod) {
				preStr = getMainObj(preStr);
			} else {
				// 在函数的括号里按shift-down往左找到函数的".func("或者".func@opt("
				int funcEnd = -1;
				boolean isFunc = false;
				for (int i = preStr.length() - 1; i >= 0; i--) {
					char c = preStr.charAt(i);
					if (c == '(') {
						funcEnd = i;
					} else if (c == '.') {
						if (funcEnd < 0) {
							continue;
						}
						String func = preStr.substring(i + 1, funcEnd);
						int optIndex = func.indexOf("@");
						if (optIndex > 0) {
							func = func.substring(0, optIndex);
						}
						if (FunctionLib.isMemberFnName(func)) {
							int scount = 1;
							for (int k = funcEnd + 2; k < text.length(); k++) {
								c = text.charAt(k);
								if (c == ')') {
									scount--;
								} else if (c == '(') {
									scount++;
								}
								if (scount == 0) {
									if (p > k) {
										return false;
									}
								}
							}
							preStr = getMainObj(preStr.substring(0, i));
							isFunc = true;
							break;
						}
					}
				}
				if (!isFunc && funcEnd > -1) {
					int scount = 1;
					for (int i = funcEnd + 1; i < text.length(); i++) {
						char c = text.charAt(i);
						if (c == ')') {
							scount--;
						} else if (c == '(') {
							scount++;
						}
						if (scount == 0) {
							if (p > i) {
								return false;
							}
						}
					}
					preStr = getMainObj(preStr.substring(0, funcEnd));
				}
			}
			if (preStr == null) {
				return false;
			}
			// "A.F."或者"A(1)."这种情况需要计算
			if (preStr.indexOf(".") > 0 || preStr.indexOf("(") > 0) {
				Expression exp = new Expression(control.cellSet, ctx, preStr);
				Object home = exp.getHome();
				if (home instanceof DotOperator) {
					DotOperator homeNode = (DotOperator) home;
					Node right = homeNode.getRight();
					if (right instanceof FieldRef || right instanceof FieldId) {
						if (isValidNode(homeNode)) {
							val = exp.calculate(ctx);
						}
					}
				} else if (home instanceof ElementRef) {
					ElementRef homeNode = (ElementRef) home;
					if (isValidNode(homeNode)) {
						val = exp.calculate(ctx);
					}
				}
			} else {
				// 看主对象是否格名
				cl = CellLocation.parse(preStr);
				if (cl != null) {
					PgmNormalCell cell = control.cellSet.getPgmNormalCell(
							cl.getRow(), cl.getCol());
					val = cell.getValue(false);
				} else {
					// 再看主对象是否变量
					Param param = EnvUtil.getParam(preStr, ctx);
					if (param != null)
						val = param.getValue();
				}
			}
			String[] fieldNames = getFieldNames(val);
			return showMatchWindow(jtext, p, isPeriod, fieldNames);
		} catch (Throwable t) {
		}
		return false;
	}

	/**
	 * 取主对象。A.F,A.F.F,A(1).F
	 * 
	 * @param preStr
	 * @return
	 */
	private String getMainObj(String preStr) {
		int start = 0;
		int brackets = 0;
		int memberEnd = -1;
		for (int i = preStr.length() - 1; i >= 0; i--) {
			char c = preStr.charAt(i);
			if (c == ')') {
				if (i < preStr.length() - 1) {
					char c1 = preStr.charAt(i + 1);
					if (c1 != '.')
						return null;
				}
				brackets++;
			} else if (c == '(') {
				brackets--;
				if (brackets < 0) {
					start = i + 1;
					break;
				} else if (brackets == 0) {
					memberEnd = i;
				}
			} else if (c == '.') {
				if (memberEnd > -1) { // 检查括号前面的是否函数
					String member = preStr.substring(i + 1, memberEnd);
					if (FunctionLib.isMemberFnName(member))
						return null;
				}
				memberEnd = -1;
			} else if (KeyWord.isSymbol(c)) {
				start = i + 1;
				break;
			}
		}
		return preStr.substring(start);
	}

	/**
	 * 返回主对象的字段名
	 * @param val
	 * @return
	 */
	private String[] getFieldNames(Object val) {
		if (val == null)
			return null;
		DataStruct ds = null;
		if (val instanceof Sequence) {
			ds = ((Sequence) val).dataStruct();
		} else if (val instanceof Record) {
			ds = ((Record) val).dataStruct();
		}
		if (ds != null) {
			String[] fieldNames = ds.getFieldNames();
			if (fieldNames == null || fieldNames.length == 0) {
				return null;
			}
			return fieldNames;
		}
		return null;
	}

	/**
	 * 显示匹配窗口
	 * 
	 * @param jtext
	 * @param p
	 * @param isPeriod
	 * @param val
	 * @return
	 * @throws BadLocationException
	 */
	private boolean showMatchWindow(final JTextComponent jtext, final int p,
			boolean isPeriod, String[] fieldNames) throws BadLocationException {
		if (fieldNames == null)
			return false;
		if (fieldNames != null) {
			GVSpl.matchWindow = new JWindowNames(fieldNames, p, isPeriod) {
				private static final long serialVersionUID = 1L;

				public void selectName(String name) {
					int dot = jtext.getCaretPosition();
					int start = GVSpl.matchWindow.getDot();
					jtext.setSelectionStart(start);
					jtext.setSelectionEnd(dot);
					GM.addText(jtext, name);
					dispose();
					jtext.requestFocus();
				}
			};
			int x = GM.getAbsolutePos(jtext, true);
			int y = GM.getAbsolutePos(jtext, false);
			FontMetrics fmText = jtext.getFontMetrics(jtext.getFont());
			if (jtext instanceof JTextArea) {
				JTextArea jta = (JTextArea) jtext;
				for (int r = 0; r < jta.getRows(); r++) {
					int lineEnd = jta.getLineEndOffset(r);
					if (lineEnd >= p) {
						y += (r + 1) * (jta.getHeight() / jta.getRows());
						int lineStart = jta.getLineStartOffset(r);
						x += fmText.stringWidth(jta.getText(lineStart, p
								- lineStart)
								+ ".");
						break;
					}
				}
			} else if (jtext instanceof JTextPane) {
				String preStr = jtext.getText().substring(0, p);
				int dx = jtext.getVisibleRect().x;
				int dy = jtext.getVisibleRect().y;
				CellLocation activeCell = control.getActiveCell();
				int cellW = control.cellW[activeCell.getCol()];
				if (ConfigOptions.bDispOutCell.booleanValue()) {
					cellW = control.contentView.getPaintableWidth(
							activeCell.getRow(), activeCell.getCol());
				}
				int rowHeight = fmText.getHeight();
				ArrayList<String> rows = ControlUtilsBase.wrapString(preStr,
						fmText, cellW);
				y += rows.size() * rowHeight;
				x += fmText.stringWidth((String) rows.get(rows.size() - 1)
						+ ".");
				x -= dx;
				y -= dy;

				x += 2;
				y += 2;
			}

			Dimension d = GV.appFrame.getSize();
			int maxX = d.width;
			int maxY = d.height;
			final int MAX_WIDTH = 300;
			final int MAX_HEIGHT = 220;
			int w = 150;
			FontMetrics fmWindow = GVSpl.matchWindow
					.getFontMetrics(GVSpl.matchWindow.getFont());
			for (String name : fieldNames) {
				if (StringUtils.isValidString(name)) {
					w = Math.max(w, fmWindow.stringWidth(name));
					if (w >= MAX_WIDTH) {
						w = MAX_WIDTH;
						break;
					}
				}
			}
			final int ROW_HEIGHT = 22;
			final int h = Math.min(ROW_HEIGHT * fieldNames.length, MAX_HEIGHT);

			if (x + w > maxX) { // 右边超宽
				if (x > w) { // 左边够宽
					x -= w;
				} else { // 左边也不够宽，还是显示在右边
					x = maxX - w;
				}
			}
			if (y + h > maxY) { // 下边不够高，15是留个边
				y -= (y + h - maxY) + 15;
			}
			GVSpl.matchWindow.setBounds(x, y, w, h);
			GVSpl.matchWindow.setVisible(true);
			jtext.requestFocus();
			return true;
		}
		return false;
	}

	/**
	 * 停止匹配
	 */
	private void stopMatch() {
		if (GVSpl.matchWindow != null) {
			GVSpl.matchWindow.dispose();
			GVSpl.matchWindow = null;
		}
	}

	/**
	 * 是否合法的结点
	 * 
	 * @param node
	 * @return
	 */
	private boolean isValidNode(Node node) {
		if (node == null)
			return false;
		Node left = node.getLeft();
		while (left != null) {
			if (!(left instanceof VarParam) && !(left instanceof CSVariable)
					&& !(left instanceof DotOperator)
					&& !(left instanceof ElementRef)) {
				return false;
			}
			left = left.getLeft();
		}
		return true;
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
	private JTextComponent getSource(KeyEvent e) {
		Object src = e.getSource();
		if (src instanceof JTextComponent) {
			return (JTextComponent) src;
		}
		if (src instanceof ToolBarPropertyBase) {
			return ((ToolBarPropertyBase) src).getWindowEditor();
		}
		return null;
	}
}
