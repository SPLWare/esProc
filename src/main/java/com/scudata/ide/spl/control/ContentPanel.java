package com.scudata.ide.spl.control;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

import com.scudata.cellset.INormalCell;
import com.scudata.cellset.IStyle;
import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.Area;
import com.scudata.common.CellLocation;
import com.scudata.common.IntArrayList;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.control.BorderStyle;
import com.scudata.ide.common.control.CellBorder;
import com.scudata.ide.common.control.CellRect;
import com.scudata.ide.common.control.JWindowNames;
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.common.swing.JTextPaneEx;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.SheetSpl;
import com.scudata.ide.spl.ToolBarProperty;

/** ???????????? */
public class ContentPanel extends JPanel implements InputMethodListener,
		InputMethodRequests {
	private static final long serialVersionUID = 1L;

	/**
	 * ??????ɫ
	 */
	private static Color XOR_COLOR = new Color(51, 0, 51);

	/** ???????? */
	CellSet cellSet;

	/** ?????????? */
	private CellSetParser parser;

	/** ???????????Ƶ???ʼ?? */
	private int startRow;

	/** ???????????ƵĽ????? */
	private int endRow;

	/** ???????????Ƶ???ʼ?? */
	private int startCol;

	/** ???????????ƵĽ????? */
	private int endCol;

	/** ?Ƿ????ڱ༭״̬ */
	private boolean isEditing;

	/** ?Ƿ?ֻ????????ʾ???ڴ?С?ڵ????? */
	private boolean onlyDrawCellInWin;

	/** ?Ƿ??ɱ༭ */
	private boolean editable;

	/** ?????????????Ĺ??????? */
	JScrollPane jsp;

	/** ?????ؼ? */
	private SplControl control;

	/** ??Ԫ????X???????? */
	int[][] cellX;

	/** ??Ԫ????Y???????? */
	int[][] cellY;

	/** ??Ԫ???Ŀ??????? */
	int[][] cellW;

	/** ??Ԫ???ĸ߶????? */
	int[][] cellH;

	/** ??ͼ??ʼ???С??У???ͼ???????С??? */
	public int drawStartRow, drawStartCol, drawEndRow, drawEndCol;

	/** ???б༭?? */
	private JTextPaneEx multiEditor;

	/** ??ǰ?༭?ؼ? */
	private JComponent editor;

	/** ?÷??????ı䵱ǰ??Ԫ??ʱ????¼?ĵ?ǰ?????к? */
	public int rememberedRow = 0;

	/** ?÷??????ı䵱ǰ??Ԫ??ʱ????¼?ĵ?ǰ?????к? */
	public int rememberedCol = 0;

	/** ???б༭?????? */
	public static final int MULTI_EDITOR = 2;

	/**
	 * ???б༭???Ĺ???????
	 */
	private JScrollPane spEditor;

	/**
	 * ?Ƿ???ֹ?仯
	 */
	private boolean preventChange = false;

	/**
	 * ?߿???ʽ
	 */
	private BorderStyle borderStyle = new BorderStyle();

	/** ͼ????????Ⱦ???????߷???4f */
	public static BasicStroke bs1 = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER, 1f, new float[] { 4f }, 0f);
	/** ͼ????????Ⱦ???????߷???5f */
	public static BasicStroke bs2 = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER, 1f, new float[] { 5f }, 0f);

	/**
	 * ͼ????????Ⱦ??
	 */
	private BasicStroke bs = null;

	/**
	 * ???ڱ༭?ĵ?Ԫ??????
	 */
	private CellLocation editPos = null;

	/**
	 * ???ڻ?ԭ?༭?ı?
	 */
	private String undoExp = null;

	protected SheetSpl sheet;

	/**
	 * ???캯??
	 * 
	 * @param cellSet           ????????
	 * @param startRow          ??????????ʼ??
	 * @param endRow            ??????????????
	 * @param startCol          ??????????ʼ??
	 * @param endCol            ??????????????
	 * @param isEditing         ?????Ƿ?λ?ڱ༭?ؼ???
	 * @param onlyDrawCellInWin ?Ƿ?ֻ????ʾ???ڴ?С?ڵ?????
	 * @param jsp               ?????????Ĺ???????
	 */
	public ContentPanel(CellSet cellSet, int startRow, int endRow,
			int startCol, int endCol, boolean isEditing,
			boolean onlyDrawCellInWin, JScrollPane jsp) {
		this(cellSet, startRow, endRow, startCol, endCol, isEditing,
				onlyDrawCellInWin, jsp, null);
	}

	/**
	 * ???캯??
	 * 
	 * @param cellSet           ????????
	 * @param startRow          ??????????ʼ??
	 * @param endRow            ??????????????
	 * @param startCol          ??????????ʼ??
	 * @param endCol            ??????????????
	 * @param isEditing         ?????Ƿ?λ?ڱ༭?ؼ???
	 * @param onlyDrawCellInWin ?Ƿ?ֻ????ʾ???ڴ?С?ڵ?????
	 * @param jsp               ?????????Ĺ???????
	 * @param sheet             ҳ??????
	 */
	public ContentPanel(CellSet cellSet, int startRow, int endRow,
			int startCol, int endCol, boolean isEditing,
			boolean onlyDrawCellInWin, JScrollPane jsp, SheetSpl sheet) {
		this.sheet = sheet;
		this.cellSet = cellSet;
		this.parser = newCellSetParser(cellSet);
		this.startRow = startRow;
		this.endRow = endRow;
		this.startCol = startCol;
		this.endCol = endCol;
		this.isEditing = isEditing;
		this.onlyDrawCellInWin = onlyDrawCellInWin;
		this.jsp = jsp;
		if (jsp instanceof SplControl) {
			control = (SplControl) jsp;
		}
		setDoubleBuffered(true);
		initCellLocations();
		setLayout(null);
		multiEditor = new JTextPaneEx() {
			private static final long serialVersionUID = 1L;

			public Point getToolTipLocation(MouseEvent e) {
				if (control.getActiveCell() == null) {
					return new Point(-9999, -9999);
				}
				int r = control.getActiveCell().getRow();
				int c = control.getActiveCell().getCol();
				Point p = getTipPos(1 + cellX[r][c], 1 + cellY[r][c]);
				return new Point(p.x - cellX[r][c], p.y - cellY[r][c]);
			}

			protected void docUpdate() {
				if (preventChange || super.preventChanged)
					return;
				super.docUpdate();
				try {
					GV.toolBarProperty.setTextEditorText(multiEditor.getText());
				} catch (Throwable t) {
				}
				try {
					resetEditorBounds();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

		};
		multiEditor.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				GVSpl.isCellEditing = true;

			}

			public void focusLost(FocusEvent e) {
				if (GVSpl.matchWindow != null) {
					try {
						Component src = e.getOppositeComponent();
						if (src == null) {
							GVSpl.matchWindow.dispose();
							GVSpl.matchWindow = null;
							return;
						}
						while (!(src instanceof JWindowNames)) {
							src = src.getParent();
							if (src == null)
								break;
						}
						if (!(src instanceof JWindowNames)) {
							GVSpl.matchWindow.dispose();
							GVSpl.matchWindow = null;
						}
					} catch (Throwable t) {
					}
				}
			}
		});

		spEditor = new JScrollPane(multiEditor);
		spEditor.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spEditor.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		add(spEditor);
		if (!isEditing) {
			addMouseListener(new ShowEditorListener(this));
		} else {
			enableInputMethods(true);
			addInputMethodListener(this);
			CellEditingListener listener = new CellEditingListener(control,
					this);
			multiEditor.addKeyListener(listener);
			EditorRightClicked erc = new EditorRightClicked(control);
			multiEditor.addMouseListener(erc);
			MouseAdapter ma = new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						MouseListener[] ml = getMouseListeners();
						if (ml != null) {
							for (int i = 0; i < ml.length; i++) {
								ml[i].mouseClicked(e);
							}
						}
					}
				}
			};
			multiEditor.addMouseListener(ma);
		}
		spEditor.setVisible(false);
		multiEditor.setVisible(false);
	}

	/**
	 * ??????????????
	 * @param cellSet ???? 
	 * @return CellSetParser
	 */
	protected CellSetParser newCellSetParser(CellSet cellSet) {
		return new CellSetParser(cellSet);
	}

	/**
	 * ?????Ƿ????Ա༭
	 * 
	 * @param editable
	 */
	public void setEditable(boolean editable) {
		this.editable = editable;
		repaint();
	}

	/**
	 * ȡ?Ƿ????Ա༭
	 * 
	 * @return
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * ????????????
	 * 
	 * @param newCellSet ????????
	 */
	public void setCellSet(CellSet newCellSet) {
		this.cellSet = newCellSet;
		this.parser = new CellSetParser(newCellSet);
		repaint();
	}

	/**
	 * ȡ?????ؼ?
	 * 
	 * @return
	 */
	public SplControl getControl() {
		return control;
	}

	/**
	 * ?????????ĳߴ?
	 * 
	 * @return ?????ĳߴ?
	 */
	public Dimension getPreferredSize() {
		/* Undoʱû?????¹???????????endCol??ʵ???п??ܲ??? */
		if (endCol > cellSet.getColCount()) {
			endCol = cellSet.getColCount();
		}
		if (endRow > cellSet.getRowCount()) {
			endRow = cellSet.getRowCount();
		}
		/* ????End */
		int width = 0;
		for (int col = startCol; col <= cellSet.getColCount(); col++) {
			width += parser.getColWidth(col);
		}
		int height = 0;
		for (int row = startRow; row <= cellSet.getRowCount(); row++) {
			height += parser.getRowHeight(row);
		}
		return new Dimension(width + 2, height + 2);
	}

	/**
	 * ??ȡ?к??????ڶ??ߵ????ظ߶ȣ????ڹ???????λ
	 * 
	 * @param row int
	 * @return int
	 */
	public int getRowOffset(int row) {
		int h = 0;
		for (int r = 1; r < row; r++) {
			h += parser.getRowHeight(r);
		}
		return h;
	}

	/**
	 * ??ȡ?к??????????ߵ????ظ߶ȣ????ڹ???????λ
	 * 
	 * @param col
	 * @return
	 */
	public int getColOffset(int col) {
		int w = 0;
		for (int c = 1; c < col; c++) {
			w += parser.getColWidth(c);
		}
		return w;
	}

	/**
	 * ????????
	 * 
	 * @param g ????
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		float scale = 1.0f;
		int rows = cellSet.getRowCount() + 1;
		int cols = cellSet.getColCount() + 1;
		if (cols != cellX[0].length || rows != cellX.length) {
			initCellLocations(rows, cols);
		}
		clearCoordinate();

		boolean isSub = false;
		if (isEditing) {
			if (!isSub) {
				g.clearRect(0, 0, 999999, 999999);
			}
			startRow = startCol = 1;
			endRow = rows - 1;
			endCol = cols - 1;
		}

		Rectangle displayWin = null;
		if (onlyDrawCellInWin) {
			displayWin = jsp.getViewport().getViewRect();
		}
		if (!isEditing && !isSub) {
			g.setColor(Color.white);
			if (onlyDrawCellInWin) {
				g.fillRect(displayWin.x, displayWin.y, displayWin.width + 5,
						displayWin.height + 5);
			} else {
				Dimension d = getPreferredSize();
				g.fillRect(0, 0, d.width + 5, d.height + 5);
			}
		}

		int x = 1, y = 1;
		drawStartRow = startRow;
		if (displayWin != null && displayWin.y >= 0) {
			int i;
			for (i = startRow; i < endRow; i++) {
				if (!parser.isRowVisible(i)) {
					continue;
				}
				int rh = parser.getRowHeight(i);
				if (y + rh > displayWin.y) {
					break;
				}
				y += rh;
			}
			drawStartRow = i;
		}

		drawStartCol = startCol;
		if (displayWin != null && displayWin.y >= 0) {
			int i;
			for (i = startCol; i < endCol; i++) {
				if (!parser.isColVisible(i)) {
					continue;
				}
				int rw = parser.getColWidth(i);
				if (x + rw > displayWin.x) {
					break;
				}
				x += rw;
			}
			drawStartCol = i;
		}

		drawEndRow = endRow;
		drawEndCol = endCol;
		int tmpX = x;
		for (int row = drawStartRow; row <= endRow; row++) {
			if (displayWin != null && y >= displayWin.y + displayWin.height) {
				drawEndRow = row - 1;
				break;
			}
			if (!parser.isRowVisible(row)) {
				continue;
			}
			x = tmpX;
			for (int col = drawStartCol; col <= endCol; col++) {
				if (displayWin != null && x >= displayWin.x + displayWin.width) {
					drawEndCol = col - 1;
					break;
				}
				if (!parser.isColVisible(col)) {
					continue;
				}

				int colWidth = parser.getColWidth(col);
				int width = colWidth;
				int height = parser.getRowHeight(row);
				if (displayWin != null && x + width <= displayWin.x) {
					x += colWidth;
					continue;
				}
				if (displayWin != null && y + height <= displayWin.y) {
					x += colWidth;
					continue;
				}

				cellX[row][col] = x;
				cellW[row][col] = width;
				cellY[row][col] = y;
				cellH[row][col] = height;

				// fill back color
				Color bkcolor = parser.getBackColor(row, col);
				if (bkcolor != null) {
					g.setColor(bkcolor);
					g.fillRect(x, y, width, height);
				}

				if (!ConfigOptions.bDispOutCell.booleanValue()) {
					drawText(g, row, col, x, y, width, height, scale);
				}

				drawFlag(g, x, y, parser, row, col);
				// draw border
				CellBorder.setEnv(g, borderStyle, row, col,
						parser.getRowCount(), parser.getColCount(), isEditing);
				CellBorder.drawBorder(x, y, width, height);

				// draw selectedCell
				if (isCellSelected(row, col)) {

					/**
					 * ???ĵ?Ԫ?񱳾?ɫ
					 */
					Color selectCellBkcolor;
					if (ConfigOptions.getCellColor() != null) {
						selectCellBkcolor = ConfigOptions.getCellColor();
					} else {
						selectCellBkcolor = Color.black;
						g.setXORMode(XOR_COLOR);
					}
					g.setColor(selectCellBkcolor);

					g.fillRect(cellX[row][col], cellY[row][col],
							cellW[row][col], cellH[row][col]);
					g.setPaintMode();
				}
				// draw refcell
				if (editor != null && editor.isVisible()) {
					if (editor == multiEditor) {
						List<INormalCell> refCells = multiEditor.getRefCells();
						if (refCells != null && !refCells.isEmpty()) {
							Color refCellColor = multiEditor.getRefCellColor(
									row, col);
							if (refCellColor != null) {
								g.setColor(refCellColor);
								g.drawRect(cellX[row][col], cellY[row][col],
										cellW[row][col], cellH[row][col]);
								g.drawRect(cellX[row][col] + 1,
										cellY[row][col] + 1,
										cellW[row][col] - 2,
										cellH[row][col] - 2);
								g.setPaintMode();
							}
						}
					}
				}

				if (control.isBreakPointCell(row, col)) {
					g.setColor(Color.magenta);
					int r = row;
					int c = col;
					g.fillRect(cellX[r][c], cellY[r][c], cellW[r][c],
							cellH[r][c]);
					g.setPaintMode();
				}
				if (control.getStepPosition() != null) {
					CellLocation cp = control.getStepPosition();
					int r = cp.getRow();
					int c = cp.getCol();
					if (r == row && c == col) {
						g.setColor(Color.blue);
						g.fillRect(cellX[r][c], cellY[r][c], cellW[r][c],
								cellH[r][c]);
						g.setPaintMode();

						if (!ConfigOptions.bDispOutCell.booleanValue()) {
							drawText(g, row, col, x, y, width, height, scale);
						}
					}
				}

				x += colWidth;
			}
			y += parser.getRowHeight(row);
		}

		if (this.control != null && ConfigOptions.bDispOutCell.booleanValue()) {
			// ??????????Ԫ????ʾ?????????????֣?ʹ???????????ϲ?
			for (int row = drawStartRow; row <= drawEndRow; row++) {
				for (int col = drawStartCol; col <= drawEndCol; col++) {
					if (cellX[row][col] == 0) {
						continue;
					}
					// draw cell content
					int height = parser.getRowHeight(row);
					int w = parser.getColWidth(col);
					int pw = getPaintableWidth(row, col);
					int px = cellX[row][col];
					int py = cellY[row][col];
					int halign = parser.getHAlign(row, col);
					if (halign == IStyle.HALIGN_RIGHT) {
						px = px + w - pw;
					}
					drawText(g, row, col, px, py, pw, height, scale);
				}
			}
		}

		if (control != null && control.getCopySourceArea() != null) {
			if (bs == bs1) {
				bs = bs2;
			} else {
				bs = bs1;
			}
			Area a = control.getCopySourceArea();
			int copyBeginRow = a.getBeginRow();
			// ???Ƶ?Area?п??ܳ????滭????
			int copyEndRow = a.getEndRow();
			int copyBeginCol = a.getBeginCol();
			int copyEndCol = a.getEndCol();
			x = -1;
			y = -1;
			if (copyBeginRow < drawStartRow) {
				copyBeginRow = drawStartRow;
				y = new Double(Math.abs(getY())).intValue();
			}
			if (copyBeginCol < drawStartCol) {
				copyBeginCol = drawStartCol;
				x = new Double(Math.abs(getX())).intValue();
			}
			if (y == -1) {
				y = cellY[copyBeginRow][copyBeginCol];
			}
			if (x == -1) {
				x = cellX[copyBeginRow][copyBeginCol];
			}
			int w = -1, h = -1;
			if (copyEndRow > drawEndRow) {
				h = new Double(getBounds().getMaxY() - getBounds().getMinY())
						.intValue();
			}
			if (copyEndCol > drawEndCol) {
				w = new Double(getBounds().getMaxX() - getBounds().getMinX())
						.intValue();
			}
			if (w == -1) {
				w = cellX[copyBeginRow][copyEndCol] - x
						+ cellW[copyBeginRow][copyEndCol];
			}
			if (h == -1) {
				h = cellY[copyEndRow][copyBeginCol] - y
						+ cellH[copyEndRow][copyBeginCol];
			}
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(Color.blue);
			BasicStroke stroke = (BasicStroke) g2.getStroke();
			g2.setStroke(bs);
			g2.drawRect(x, y, w, h);
			g2.setStroke(stroke);
		}

		/*
		 * ????Ϊtrue?????ػ?ʱװ?ؿؼ??ı??ᵼ?¹???û?????? false????װ????????????
		 * ???ػ?ʱ??װ?ؿؼ??ı?,?ƺ?ĳ???????????뷨????????ʱ??????
		 */
		initEditor(submitEditor(true));

		drawSelectedRectBorder(g);

		if (!isSub) {
			g.dispose();
		}
	}

	/**
	 * ??Ԫ???Ƿ???ѡ??
	 * 
	 * @param row ?к?
	 * @param col ?к?
	 * @return
	 */
	private boolean isCellSelected(int row, int col) {
		if (control == null) {
			return false;
		}
		Vector<Object> areas = control.getSelectedAreas();
		for (int i = 0; i < areas.size(); i++) {
			Object one = areas.get(i);
			if (one == null) {
				continue;
			}
			Area a;
			if (one instanceof CellRect) {
				a = ((CellRect) one).getArea();
			} else {
				a = (Area) one;
			}
			if (a.contains(row, col)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * ???ñ༭?????ı?
	 * 
	 * @param text
	 */
	public void setEditorText(String text) {
		preventChange = true;
		if (editor instanceof JTextPaneEx) {
			((JTextPaneEx) editor).setPreventChange(true);
			((JTextPaneEx) editor).setText(text);
			((JTextPaneEx) editor).setPreventChange(false);
		}
		preventChange = false;
	}

	/**
	 * ???ñ༭???ı߽?
	 */
	private void resetEditorBounds() {
		if (editor == null || control == null
				|| control.getActiveCell() == null) {
			return;
		}
		Rectangle srcRect = spEditor.getBounds();
		String editingText = ((JTextComponent) editor).getText();
		int r = control.getActiveCell().getRow();
		int c = control.getActiveCell().getCol();
		if (ConfigOptions.bDispOutCell.booleanValue()) {
			int ew = getEditableWidth(editingText, r, c);
			if (srcRect.width < ew) {
				editingText += "a"; // ??ǰ?Ӹ??ַ?????
			}
		}
		CellRect rect = getEditorBounds(editingText, r, c);
		int w = rect.getRowCount();
		int h = rect.getColCount();
		if (h < srcRect.height) {
			h = srcRect.height;
		}
		if (h < srcRect.height) {
			h = srcRect.height;
		}
		if (editPos == null) {
			if (w != srcRect.width || h != srcRect.height) {
				spEditor.setSize(w, h);
			}
		} else { // ͬһ????????????С?˱༭?????ñ?С
			if (w > srcRect.width || h > srcRect.height) {
				if (w < srcRect.width)
					w = srcRect.width;
				if (h < srcRect.height)
					h = srcRect.height;
				spEditor.setSize(w, h);
			}
		}
	}

	/**
	 * ???ù???
	 * 
	 * @param caretPosition ????λ??
	 * @param newText       ???ı?
	 */
	private void setCaret(int caretPosition, String newText) {
		int len = newText.length();
		if (caretPosition > 0
				&& caretPosition <= len
				&& editor instanceof JTextComponent
				&& caretPosition <= ((JTextComponent) editor).getText()
						.length()) {
			((JTextComponent) editor).setCaretPosition(caretPosition);
		}
	}

	/**
	 * ͨ????????????cell??value??exp??????ˢ?µ?Editor?ؼ? ճ??ʱ,ҲҪ?˶???
	 */
	public void reloadEditorText() {
		CellLocation m_activeCell = control.getActiveCell();
		if (m_activeCell == null) {
			return;
		}
		String text = ControlUtils.getCellText(cellSet, m_activeCell.getRow(),
				m_activeCell.getCol(), isEditing);
		if (editor instanceof JTextComponent) {
			int i = ((JTextComponent) editor).getCaretPosition();
			preventChange = true;
			((JTextComponent) editor).setText(text);
			setCaret(i, text);
			preventChange = false;
		}
	}

	/**
	 * ȡҪ???ƵĿ???
	 * 
	 * @param cr ?к?
	 * @param cc ?к?
	 * @return
	 */
	public int getPaintableWidth(int cr, int cc) {
		int w = parser.getColWidth(cc);

		String drawText = ControlUtils.getCellText(cellSet, cr, cc, true);
		int indent = ConfigOptions.iIndent.intValue();
		float cw = parser.getColWidth(cc) - indent;
		float ch = parser.getRowHeight(cr);
		float textH = ControlUtils.getStringHeight(drawText, cw, GC.font);
		if (ch > textH) {
			return w;
		}

		byte halign = parser.getHAlign(cr, cc);
		if (halign == IStyle.HALIGN_LEFT) {
			for (int c = cc + 1; c <= cellSet.getColCount(); c++) {
				NormalCell nc = (NormalCell) cellSet.getCell(cr, c);
				if (StringUtils.isValidString(nc.getExpString())) {
					break;
				}
				w += parser.getColWidth(c);
			}
		} else if (halign == IStyle.HALIGN_RIGHT) {
			for (int c = cc - 1; c >= 1; c--) {
				NormalCell nc = (NormalCell) cellSet.getCell(cr, c);
				if (StringUtils.isValidString(nc.getExpString())) {
					break;
				}
				w += parser.getColWidth(c);
			}
		}

		return w;
	}

	/**
	 * ȡҪ?༭?Ŀ???
	 * 
	 * @param editingText ?༭?ı?
	 * @param row         ?к?
	 * @param col         ?к?
	 * @return
	 */
	public int getEditableWidth(String editingText, int row, int col) {
		int w = parser.getColWidth(col);

		if (ConfigOptions.bDispOutCell.booleanValue()) {
			int textWidth = ControlUtils
					.getStringMaxWidth(editingText, GC.font);

			for (int c = col + 1; c <= cellSet.getColCount(); c++) {
				NormalCell nc = (NormalCell) cellSet.getCell(row, c);
				if (StringUtils.isValidString(nc.getExpString())
						|| w > textWidth + 5) {
					break;
				}
				w += parser.getColWidth(c);
			}
		}
		return w;
	}

	/**
	 * ȡ???Ա༭?ĸ߶?
	 * 
	 * @param text ?ı?
	 * @param row  ?к?
	 * @param col  ?к?
	 * @param maxW ????????
	 * @return
	 */
	private int getEditableHeight(String text, int row, int col, int maxW) {
		int h = parser.getRowHeight(row);
		// ?༭?ؼ?????Ҫռ????,??????5????
		float textH = ControlUtils.getStringHeight(text, maxW - 5, GC.font);
		for (int r = row + 1; r <= cellSet.getRowCount(); r++) {
			if (h > textH + 3) {
				break;
			}
			h += parser.getRowHeight(r);
		}
		return h;
	}

	/**
	 * ????ѡ???????ı߿?
	 * 
	 * @param g
	 */
	private void drawSelectedRectBorder(Graphics g) {
		Area area = control.getSelectedArea(0);
		if (area == null) {
			return;
		}
		int row = area.getBeginRow();
		int col = area.getBeginCol();
		int endRow = area.getEndRow();
		int endCol = area.getEndCol();
		boolean drawTop = true;
		if (row < drawStartRow && endRow >= drawStartRow) { // ???б?????
			row = drawStartRow;
			drawTop = false;
		}
		boolean drawLeft = true;
		if (col < drawStartCol && endCol >= drawStartCol) { // ???б?????
			col = drawStartCol;
			drawLeft = false;
		}
		boolean drawBottom;
		if (endRow > drawEndRow) {
			endRow = drawEndRow;
			drawBottom = false;
		} else {
			drawBottom = true;
		}
		boolean drawRight;
		if (endCol > drawEndCol) {
			endCol = drawEndCol;
			drawRight = false;
		} else {
			drawRight = true;
		}
		int width = 0;
		for (int c = col; c <= endCol; c++) {
			if (c >= drawStartCol) {
				width += parser.getColWidth(c);
			}
		}
		int height = 0;
		for (int r = row; r <= endRow; r++) {
			if (r >= drawStartRow) {
				height += parser.getRowHeight(r);
			}
		}

		if (width == 0 || height == 0) {
			return;
		}
		row = Math.max(drawStartRow, row);
		((Graphics2D) g).setStroke(new BasicStroke(3.0f));
		g.setColor(Color.BLACK);
		g.setXORMode(Color.lightGray);
		final int LINE_WIDTH = 3;
		int x1, y1, x2, y2;
		if (drawTop) {
			x1 = cellX[row][col];
			y1 = cellY[row][col];
			x2 = cellX[row][col] + width;
			y2 = y1;
			g.drawLine(x1, y1, x2, y2);
		}
		if (drawLeft) {
			x1 = cellX[row][col];
			y1 = cellY[row][col];
			x2 = x1;
			y2 = y1 + height;
			if (drawTop) {
				y1 += LINE_WIDTH;
			}
			if (drawBottom) {
				y2 -= LINE_WIDTH;
			}
			g.drawLine(x1, y1, x2, y2);
		}
		if (drawBottom) {
			x1 = cellX[row][col];
			y1 = cellY[row][col] + height;
			x2 = cellX[row][col] + width;
			y2 = y1;
			g.drawLine(x1, y1, x2, y2);
		}
		if (drawRight) {
			x1 = cellX[row][col] + width;
			y1 = cellY[row][col];
			x2 = x1;
			y2 = cellY[row][col] + height;
			if (drawTop) {
				y1 += LINE_WIDTH;
			}
			if (drawBottom) {
				y2 -= LINE_WIDTH;
			}
			g.drawLine(x1, y1, x2, y2);
		}
	}

	/**
	 * ????Ԫ??????
	 * 
	 * @param g      ????
	 * @param x      X????
	 * @param y      Y????
	 * @param parser ??????????
	 * @param row    ?к?
	 * @param col    ?к?
	 */
	public static void drawFlag(Graphics g, int x, int y, CellSetParser parser,
			int row, int col) {
		PgmNormalCell cell = (PgmNormalCell) parser.getCell(row, col);
		int maxFlagSize = Math.min(parser.getRowHeight(row),
				parser.getColWidth(col));
		int flagSize = Math.min(maxFlagSize, GC.FLAG_SIZE_SMALL);
		Color oldColor = g.getColor();
		if (StringUtils.isValidString(cell.getTip())) {
			g.setColor(Color.white);
			g.setXORMode(new Color(51, 153, 0));
			int w = parser.getColWidth(col);
			int[] x1 = new int[] { x + w - flagSize, x + w, x + w };
			int[] y1 = new int[] { y, y, y + flagSize };

			g.fillPolygon(x1, y1, 3);
		}
		if (cell.isResultCell()) {
			g.setColor(Color.white);
			g.setXORMode(Color.BLUE);
			int h = parser.getRowHeight(row);
			int w = parser.getColWidth(col);
			int[] x1 = new int[] { x + w - flagSize, x + w, x + w };
			int[] y1 = new int[] { y + h, y + h, y + h - flagSize };

			g.fillPolygon(x1, y1, 3);
		}
		g.setPaintMode();
		g.setColor(oldColor);
	}

	/**
	 * ??ʼ?????ӵ?????
	 */
	public void initCellLocations() {
		int rows = parser.getRowCount() + 1;
		int cols = parser.getColCount() + 1;
		initCellLocations(rows, cols);
	}

	/**
	 * ??ʼ?????ӵ?????
	 * 
	 * @param rows ????
	 * @param cols ????
	 */
	public void initCellLocations(int rows, int cols) {
		cellX = new int[rows][cols];
		cellW = new int[rows][cols];
		cellY = new int[rows][cols];
		cellH = new int[rows][cols];
	}

	/**
	 * ???Ƶ?Ԫ??????ʾ???ı?
	 * 
	 * @param g     ????
	 * @param row   ?к?
	 * @param col   ?к?
	 * @param x     X????
	 * @param y     Y????
	 * @param w     ????
	 * @param h     ?߶?
	 * @param scale ??ʾ????
	 */
	private void drawText(Graphics g, int row, int col, int x, int y, int w,
			int h, float scale) {
		String text = parser.getDispText(row, col);
		drawText(text, g, row, col, x, y, w, h, scale);
	}

	/**
	 * ???Ƶ?Ԫ??????ʾ???ı?
	 * 
	 * @param text  ?ı?
	 * @param g     ????
	 * @param row   ?к?
	 * @param col   ?к?
	 * @param x     X????
	 * @param y     Y????
	 * @param w     ????
	 * @param h     ?߶?
	 * @param scale ??ʾ????
	 */
	private void drawText(String text, Graphics g, int row, int col, int x,
			int y, int w, int h, float scale) {
		Font font = parser.getFont(row, col);
		byte halign = parser.getHAlign(row, col);
		byte valign = parser.getVAlign(row, col);

		Color c = parser.getForeColor(row, col);
		if (control.isBreakPointCell(row, col)) {
			c = Color.white;
		} else if (!editable) {
			c = Color.darkGray;
		}
		try {
			if (control.getStepPosition() != null
					&& !control.cellSet.isAutoCalc()) {
				CellLocation cp = control.getStepPosition();
				int rr = cp.getRow();
				int cc = cp.getCol();
				if (rr == row && cc == col) {
					c = Color.white;
					/*
					 * ǰ??ɫΪ??ɫʱ????????????Ԫ????ʾ???뱳??ͬɫ?????????ˣ???????XORģʽ??ʾ
					 * ??????????finallyҪ???û?paintģʽ
					 */
					g.setXORMode(XOR_COLOR);
				}
			}

			boolean underLine = parser.isUnderline(row, col);
			ControlUtils.drawText(g, text, x, y, w, h, underLine, halign,
					valign, font, c);
		} finally {
			g.setPaintMode();
		}
	}

	/**
	 * ????Ԫ???????͸߿?????????
	 */
	private void clearCoordinate() {
		for (int i = 0; i < cellX.length; i++) {
			for (int j = 0; j < cellX[i].length; j++) {
				cellX[i][j] = 0;
				cellY[i][j] = 0;
				cellW[i][j] = 0;
				cellH[i][j] = 0;
			}
		}
	}

	/**
	 * ȡ?༭??
	 * 
	 * @return
	 */
	public JComponent getEditor() {
		return editor;
	}

	/**
	 * ?Ƿ????ڱ༭
	 * 
	 * @return
	 */
	public boolean isEditing() {
		return editor != null && editor.isVisible();
	}

	/**
	 * ???༭?????????ı?
	 * 
	 * @param text Ҫ???ӵ??ı?
	 */
	public void addText(String text) {
		if (editor == null) {
			return;
		}
		if (!editor.isVisible()) {
			return;
		}
		String newText = "";
		if (editor instanceof JComboBoxEx) {
			JComboBoxEx combo = (JComboBoxEx) editor;
			Component c = combo.getEditor().getEditorComponent();
			if (c instanceof JTextComponent) {
				GM.addText((JTextComponent) c, text);
				newText = ((JTextComponent) c).getText();
				combo.setSelectedItem(newText);
			} else {
				Object val = combo.getSelectedItem();
				combo.setSelectedItem(val == null ? text : val + text);
				newText = combo.getSelectedItem() == null ? "" : (String) combo
						.getSelectedItem();
			}
		} else if (editor instanceof JTextComponent) {
			GM.addText((JTextComponent) editor, text);
			newText = ((JTextComponent) editor).getText();
		}
		resetEditorBounds();
		control.fireEditorInputing(newText);
	}

	/**
	 * ?༭????????????
	 * 
	 * @param e   ?????¼?
	 * @param row ?к?
	 * @param col ?к?
	 */
	public void editorMousePressed(MouseEvent e, int row, int col) {
		if (editor == null)
			return;
		int x = e.getX();
		int y = e.getY();
		x -= cellX[row][col];
		y -= cellY[row][col];
		final MouseEvent e1 = new MouseEvent(editor, e.getID(), e.getWhen(),
				e.getModifiers(), x, y, 1, e.isPopupTrigger(), e.getButton());
		try {
			if (editor == multiEditor) {
				Caret caret = multiEditor.getCaret();
				if (caret != null && caret instanceof DefaultCaret) {
					final DefaultCaret dc = (DefaultCaret) caret;
					if (dc != null) {
						dc.mousePressed(e1);
						multiEditor.caretChanged(dc.getDot());
					}
				}
			}
		} catch (Throwable t) {
		}
		MouseListener[] ml = editor.getMouseListeners();
		if (ml != null) {
			for (int i = 0; i < ml.length; i++) {
				ml[i].mousePressed(e1);
			}
		}
	}

	/** ˢ???ı??? */
	public static final byte MODE_PAINT = 0;
	/** ??ʾ?ı??? */
	public static final byte MODE_SHOW = 1;
	/** ?????ı??? */
	public static final byte MODE_HIDE = 2;

	/**
	 * ??ʼ???༭??
	 * 
	 * @param caretPosition ????λ??
	 */
	private void initEditor(int caretPosition) {
		initEditor(caretPosition, MODE_PAINT);
	}

	/**
	 * ??ʼ???༭??
	 * 
	 * @param mode MODE_PAINT,MODE_SHOW,MODE_HIDE
	 */
	public void initEditor(byte mode) {
		int ca = 0;
		try {
			CellLocation cl = control.getActiveCell();
			String text = ControlUtils.getCellText(control.cellSet,
					cl.getRow(), cl.getCol(), true);
			ca = text.length();
		} catch (Throwable t) {
		}
		initEditor(ca, mode);
	}

	/**
	 * ??ʼ???༭??
	 * 
	 * @param caretPosition ????λ??
	 * @param mode          MODE_PAINT,MODE_SHOW,MODE_HIDE
	 */
	public void initEditor(int caretPosition, byte mode) {
		if (mode != MODE_PAINT) {
			editPos = null;
		}
		if (!editable || control == null) {
			if (editor != null && editor.isVisible()) {
				editor.setVisible(false);
			}
			return;
		}
		if (control.getActiveCell() == null) {
			return;
		}

		if (!((SheetSpl) GV.appSheet).isCellSetEditable()) {
			if (editor != null && editor.isVisible()) {
				editor.setVisible(false);
			}
			return;
		}
		int row = control.getActiveCell().getRow();
		int col = control.getActiveCell().getCol();
		if (row > cellSet.getRowCount() || col > cellSet.getColCount()) {
			return;
		}
		if (!parser.isRowVisible(row) || !parser.isColVisible(col)) {
			return;
		}

		// ???´?????ֹƽ??ʱ?Ǽ?????ҳ??
		Container container = this.getParent();
		while (container != null) {
			if (container instanceof JInternalFrame) {
				if (!((JInternalFrame) container).isSelected()) {
					submitEditor();
					return;
				}
				break;
			}
			container = container.getParent();
		}

		editor = multiEditor;

		String text = null;
		switch (mode) {
		case MODE_SHOW:
			if (GVSpl.matchWindow != null) {
				GVSpl.matchWindow.dispose();
			}
			editPos = new CellLocation(row, col);
			if (!editor.isVisible()) {
				editor.setVisible(true);
				spEditor.setVisible(true);
			}
			editor.requestFocus();
			text = ControlUtils.getCellText(cellSet, row, col, isEditing);
			CellRect rect = getEditorBounds(text, row, col);
			editor.setBounds(rect.getBeginRow(), rect.getBeginCol(),
					rect.getRowCount(), rect.getColCount());
			spEditor.setBounds(rect.getBeginRow(), rect.getBeginCol(),
					rect.getRowCount(), rect.getColCount());
			preventChange = true;
			undoExp = text;
			((JTextComponent) editor).setText(text);
			((JTextComponent) editor).setEditable(true);
			preventChange = false;
			GV.isCellEditing = true;
			multiEditor.initRefCells(false);
			break;
		case MODE_PAINT:
			break;
		case MODE_HIDE:
			if (GVSpl.matchWindow != null) {
				GVSpl.matchWindow.dispose();
			}
			if (editor.isVisible()) {
				editor.setVisible(false);
				spEditor.setVisible(false);
			}
			if (cellX.length > row && cellX[row].length > col)
				spEditor.setBounds(cellX[row][col], cellY[row][col], 1, 1);
			else {
				Rectangle rec = spEditor.getBounds();
				spEditor.setBounds(rec.x, rec.y, 1, 1);
			}
			break;
		}
		byte valign = parser.getVAlign(row, col);
		if (valign == IStyle.VALIGN_TOP) {
			editor.setAlignmentY(JComponent.TOP_ALIGNMENT);
		} else if (valign == IStyle.VALIGN_MIDDLE) {
			editor.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		} else if (valign == IStyle.VALIGN_BOTTOM) {
			editor.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
		}
		Color bkcolor = parser.getBackColor(row, col);
		editor.setBackground(bkcolor == null ? new JTextPane().getBackground()
				: bkcolor);
		editor.setForeground(parser.getForeColor(row, col));
		editor.setFont(parser.getFont(row, col));
		spEditor.setBounds(spEditor.getBounds());
		spEditor.setBorder(BorderFactory.createLineBorder(Color.darkGray, 1));
	}

	/**
	 * ȡ?༭???ı߽?
	 * 
	 * @param text ?ı?
	 * @param row  ?к?
	 * @param col  ?к?
	 * @return
	 */
	public CellRect getEditorBounds(String text, int row, int col) {
		int x = cellX[row][col], y = cellY[row][col], w = getEditableWidth(
				text, row, col);
		if (onlyDrawCellInWin) {
			final int BORDER_SIZE = 0;
			Rectangle displayWin = jsp.getViewport().getViewRect();
			if (x + w > displayWin.x + displayWin.width) {
				w = displayWin.x + displayWin.width - x - BORDER_SIZE;
			}
			if (x + w > cellX[row][drawEndCol] + cellW[row][drawEndCol]) {
				w = cellX[row][drawEndCol] + cellW[row][drawEndCol] - x
						- BORDER_SIZE;
			}
		}
		int h = getEditableHeight(text, row, col, w);
		if (onlyDrawCellInWin) {
			final int BORDER_SIZE = 0;
			Rectangle displayWin = jsp.getViewport().getViewRect();
			if (y + h > displayWin.y + displayWin.height) {
				h = displayWin.y + displayWin.height - y - BORDER_SIZE;
			}
			if (parser.isRowVisible(drawEndRow))
				if (y + h > cellY[drawEndRow][col] + cellH[drawEndRow][col]) {
					h = cellY[drawEndRow][col] + cellH[drawEndRow][col] - y
							- BORDER_SIZE;
				}
		}
		return new CellRect(x, y, w, h);
	}

	/**
	 * ?ύ??ǰ?༭???е?????,???ع???λ??
	 * 
	 * @return
	 */
	public int submitEditor() {
		return submitEditor(false);
	}

	/**
	 * ?ύ??ǰ?༭???е?????,???ع???λ??
	 * 
	 * @param isPaint ?Ƿ?ˢ??
	 * @return
	 */
	private int submitEditor(boolean isPaint) {
		if (control == null || editor == null || !editor.isVisible()
				|| control.getActiveCell() == null) {
			return -1;
		}
		String text = ((JTextComponent) editor).getText();
		if (isEditing && !isPaint) {
			int p = ((JTextComponent) editor).getCaretPosition();
			int row = control.getActiveCell().getRow();
			int col = control.getActiveCell().getCol();
			if (!text.equals(ControlUtils.getCellText(cellSet, row, col,
					isEditing))) {
				control.fireCellTextInput(control.getActiveCell(), text);
			}
			return p;
		}
		return text == null ? 0 : text.length();
	}

	/**
	 * ??ԭ?༭?????ı?
	 */
	public void undoEditor() {
		String text = ((JTextComponent) editor).getText();
		if (StringUtils.isValidString(text)) {
			String newText = undoExp;
			if (text.equals(undoExp)) {
				newText = null;
			}
			((JTextComponent) editor).setText(newText);
			((ToolBarProperty) GV.toolBarProperty).setTextEditorText(newText,
					true);
			undoExp = text;
		} else {
			if (StringUtils.isValidString(undoExp)) {
				((JTextComponent) editor).setText(undoExp);
				((ToolBarProperty) GV.toolBarProperty).setTextEditorText(
						undoExp, true);
				undoExp = null;
			}
		}
	}

	/**
	 * ???????뷨????????
	 * 
	 * @return ???뷨????????
	 */
	public InputMethodRequests getInputMethodRequests() {
		return this;
	}

	/**
	 * ȡ??????ʾ??????
	 */
	public Point getToolTipLocation(MouseEvent e) {
		return getTipPos(e.getX(), e.getY());
	}

	/**
	 * ȡ??Ԫ????ʾ??????
	 * 
	 * @param x1 X????
	 * @param y1 Y????
	 * @return
	 */
	private Point getTipPos(int x1, int y1) {
		CellLocation cl = ControlUtils.lookupCellPosition(x1, y1, this);
		if (cl == null) {
			return null;
		}
		int row = cl.getRow(), col = cl.getCol();
		String tips = ((PgmNormalCell) cellSet.getCell(row, col)).getTip();
		if (!StringUtils.isValidString(tips)) {
			return null;
		}
		IntArrayList list = new IntArrayList();
		tips = GM.transTips(tips, list);
		int tipWidth = list.getInt(0) + 10;
		Rectangle displayWin = jsp.getViewport().getViewRect();
		int x;
		if (cellX[row][col] + cellW[row][col] + tipWidth < displayWin.x
				+ displayWin.width) {
			x = cellX[row][col] + cellW[row][col] + GC.TIP_GAP;
		} else {
			x = cellX[row][col] - tipWidth - GC.TIP_GAP;
		}
		if (x < 0 || x > displayWin.x + displayWin.width) {
			x = 0;
		}
		int y;
		int tipHeight = list.getInt(1);
		if (cellY[row][col] + GC.TIP_GAP + tipHeight < displayWin.y
				+ displayWin.height) {
			y = cellY[row][col] + GC.TIP_GAP;
		} else {
			y = displayWin.y + displayWin.height - GC.TIP_GAP - tipHeight;
		}
		if (y < 0 || y > displayWin.y + displayWin.height) {
			y = 0;
		}
		return new Point(x, y);
	}

	/**
	 * ?????뷨?ؼ????????µ??ı?ʱ?Ĵ???
	 * 
	 * @param event ???뷨?¼?
	 */
	public void inputMethodTextChanged(InputMethodEvent event) {
		try {
			int count = event.getCommittedCharacterCount();
			AttributedCharacterIterator text = event.getText();
			String composedText = "";
			char c;
			if (text != null) {
				c = text.first();
				while (count-- > 0) {
					composedText += String.valueOf(c);
					c = text.next();
				}
				if (editor == null || !editor.isVisible()) {
					initEditor(MODE_SHOW);
				}
				if (editor != null && editor instanceof JTextComponent) {
					((JTextComponent) editor).setText(composedText);
					editor.requestFocus();
				}
			}
			event.consume();
		} catch (Throwable t) {
		}
	}

	/**
	 * ????λ?ñ仯
	 */
	public void caretPositionChanged(InputMethodEvent event) {
	}

	/**
	 * ȡ?ı???λ??
	 */
	public Rectangle getTextLocation(TextHitInfo offset) {
		return new Rectangle(0, 0);
	}

	/**
	 * ȡ??????λ??
	 */
	public TextHitInfo getLocationOffset(int x, int y) {
		return null;
	}

	/**
	 * ȡ??????????????λ??
	 */
	public int getInsertPositionOffset() {
		return 0;
	}

	/**
	 * ȡ?ύ???ı?
	 */
	public AttributedCharacterIterator getCommittedText(int beginIndex,
			int endIndex, Attribute[] attributes) {
		return null;
	}

	/**
	 * ȡ?ύ?ı??ĳ???
	 */
	public int getCommittedTextLength() {
		return 0;
	}

	/**
	 * ȡ???????ύ???ı?
	 */
	public AttributedCharacterIterator cancelLatestCommittedText(
			Attribute[] attributes) {
		return null;
	}

	/**
	 * ȡѡ?????ı?
	 */
	public AttributedCharacterIterator getSelectedText(Attribute[] attributes) {
		return null;
	}

	/**
	 * ?ر?
	 */
	public void dispose() {
		cellSet = null;
		jsp = null;
		cellX = null;
		cellY = null;
		cellW = null;
		cellH = null;
		multiEditor = null;
		editor = null;
	}

}
