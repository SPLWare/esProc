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
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextHitInfo;
import java.awt.geom.Rectangle2D;
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
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.common.swing.JTextPaneEx;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.SheetSpl;
import com.scudata.ide.spl.ToolBarProperty;

/** 网格内容面板 */
public class ContentPanel extends JPanel implements InputMethodListener,
		InputMethodRequests {
	private static final long serialVersionUID = 1L;

	/**
	 * 异或颜色
	 */
	public static Color XOR_COLOR = new Color(51, 0, 51);

	/** 网格对象 */
	CellSet cellSet;

	/** 网格分析器 */
	protected CellSetParser parser;

	/** 内容面板绘制的起始行 */
	protected int startRow;

	/** 内容面板绘制的结束行 */
	protected int endRow;

	/** 内容面板绘制的起始列 */
	protected int startCol;

	/** 内容面板绘制的结束列 */
	protected int endCol;

	/**
	 * 是否处于编辑状态，区分单元格值显示表达式还是显示格值用的。 现在应该是没用的属性了，传的总是true，也就是总显示的单元格表达式
	 **/
	protected boolean isEditing;

	/** 是否只画处于显示窗口大小内的内容 */
	protected boolean onlyDrawCellInWin;

	/** 是否可编辑 */
	protected boolean editable;

	/** 容纳内容面板的滚动窗格 */
	JScrollPane jsp;

	/** 网格控件 */
	protected SplControl control;

	/** 单元格的X坐标数组 */
	int[][] cellX;

	/** 单元格的Y坐标数组 */
	int[][] cellY;

	/** 单元格的宽度数组 */
	int[][] cellW;

	/** 单元格的高度数组 */
	int[][] cellH;

	/** 绘图开始的行、列，绘图结束的行、列 */
	public int drawStartRow, drawStartCol, drawEndRow, drawEndCol;

	/** 多行编辑框 */
	protected JTextPaneEx multiEditor;

	/** 当前编辑控件 */
	protected JComponent editor;

	/** 用方向键改变当前单元格时，记录的当前光标行号 */
	public int rememberedRow = 0;

	/** 用方向键改变当前单元格时，记录的当前光标列号 */
	public int rememberedCol = 0;

	/** 多行编辑器类型 */
	public static final int MULTI_EDITOR = 2;

	/**
	 * 多行编辑器的滚动面板
	 */
	protected JScrollPane spEditor;

	/**
	 * 是否阻止变化
	 */
	protected boolean preventChange = false;

	/**
	 * 边框样式
	 */
	protected BorderStyle borderStyle = new BorderStyle();

	/** 图形轮廓渲染器，虚线风格4f */
	public static BasicStroke bs1 = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER, 1f, new float[] { 4f }, 0f);
	/** 图形轮廓渲染器，虚线风格5f */
	public static BasicStroke bs2 = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER, 1f, new float[] { 5f }, 0f);

	/**
	 * 图形轮廓渲染器
	 */
	protected BasicStroke bs = null;

	/**
	 * 正在编辑的单元格坐标
	 */
	protected CellLocation editPos = null;

	/**
	 * 用于还原编辑文本
	 */
	protected String undoExp = null;

	protected SheetSpl sheet;

	/**
	 * 构造函数
	 * 
	 * @param cellSet
	 *            网格对象
	 * @param startRow
	 *            内容面板起始行
	 * @param endRow
	 *            内容面板结束行
	 * @param startCol
	 *            内容面板起始列
	 * @param endCol
	 *            内容面板结束列
	 * @param isEditing
	 *            面板是否位于编辑控件中
	 * @param onlyDrawCellInWin
	 *            是否只画显示窗口大小内的面板
	 * @param jsp
	 *            容纳面板的滚动窗格
	 */
	public ContentPanel(CellSet cellSet, int startRow, int endRow,
			int startCol, int endCol, boolean isEditing,
			boolean onlyDrawCellInWin, JScrollPane jsp) {
		this(cellSet, startRow, endRow, startCol, endCol, isEditing,
				onlyDrawCellInWin, jsp, null);
	}

	/**
	 * 构造函数
	 * 
	 * @param cellSet
	 *            网格对象
	 * @param startRow
	 *            内容面板起始行
	 * @param endRow
	 *            内容面板结束行
	 * @param startCol
	 *            内容面板起始列
	 * @param endCol
	 *            内容面板结束列
	 * @param isEditing
	 *            面板是否位于编辑控件中
	 * @param onlyDrawCellInWin
	 *            是否只画显示窗口大小内的面板
	 * @param jsp
	 *            容纳面板的滚动窗格
	 * @param sheet
	 *            页面对象
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
		newEditor();
		spEditor = new JScrollPane(multiEditor);
		spEditor.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spEditor.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		multiEditor.setVisible(false);
		spEditor.setVisible(false);
		add(spEditor);
		if (!isEditing) {
			addMouseListener(new ShowEditorListener(this));
		} else {
			enableInputMethods(true);
			addInputMethodListener(this);
			addEditorFocusListener(multiEditor);
			addCellEditingListener(control, multiEditor);
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
	}

	/**
	 * 创建编辑控件
	 */
	protected void newEditor() {
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
	}

	/**
	 * 创建网格解析器
	 * 
	 * @param cellSet
	 *            网格
	 * @return CellSetParser
	 */
	protected CellSetParser newCellSetParser(CellSet cellSet) {
		return new CellSetParser(cellSet);
	}

	/**
	 * 设置网格编辑监听器
	 * 
	 * @return
	 */
	protected void addCellEditingListener(SplControl control,
			JTextComponent jtext) {
		CellEditingListener listener = new CellEditingListener(control, this);
		jtext.addKeyListener(listener);
	}

	/**
	 * 增加编辑控件焦点事件
	 * 
	 * @param jtext
	 */
	protected void addEditorFocusListener(JTextComponent jtext) {
		jtext.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				GVSpl.isCellEditing = true;
			}
		});
	}

	/**
	 * 设置是否可以编辑
	 * 
	 * @param editable
	 */
	public void setEditable(boolean editable) {
		this.editable = editable;
		repaint();
	}

	/**
	 * 取是否可以编辑
	 * 
	 * @return
	 */
	public boolean isEditable() {
		return editable;
	}

	protected boolean isCellEditable(int row, int col) {
		return editable;
	}

	/**
	 * 设置网格对象
	 * 
	 * @param newCellSet
	 *            网格对象
	 */
	public void setCellSet(CellSet newCellSet) {
		this.cellSet = newCellSet;
		this.parser = new CellSetParser(newCellSet);
		repaint();
	}

	/**
	 * 取网格控件
	 * 
	 * @return
	 */
	public SplControl getControl() {
		return control;
	}

	/**
	 * 获得面板的尺寸
	 * 
	 * @return 面板的尺寸
	 */
	public Dimension getPreferredSize() {
		float scale = 1.0f;
		if (control != null) {
			scale = control.scale;
		}
		/* Undo时没有重新构建面板造成endCol跟实际有可能不符 */
		if (endCol > cellSet.getColCount()) {
			endCol = cellSet.getColCount();
		}
		if (endRow > cellSet.getRowCount()) {
			endRow = cellSet.getRowCount();
		}
		/* 不符End */
		int width = 0;
		for (int col = startCol; col <= cellSet.getColCount(); col++) {
			if (!parser.isColVisible(col)) {
				continue;
			}
			width += parser.getColWidth(col, scale);
		}
		int height = 0;
		for (int row = startRow; row <= cellSet.getRowCount(); row++) {
			if (!parser.isRowVisible(row)) {
				continue;
			}
			height += parser.getRowHeight(row, scale);
		}
		return new Dimension(width + 2, height + 2);
	}

	/**
	 * 获取行号相对于顶边的像素高度，用于滚动条定位
	 * 
	 * @param row
	 *            int
	 * @return int
	 */
	public int getRowOffset(int row, float scale) {
		int h = 0;
		for (int r = 1; r < row; r++) {
			if (!parser.isRowVisible(r)) {
				continue;
			}
			h += parser.getRowHeight(r, scale);
		}
		return h;
	}

	/**
	 * 获取列号相对于左边的像素高度，用于滚动条定位
	 * 
	 * @param col
	 * @return
	 */
	public int getColOffset(int col, float scale) {
		int w = 0;
		for (int c = 1; c < col; c++) {
			if (!parser.isColVisible(c)) {
				continue;
			}
			w += parser.getColWidth(c, scale);
		}
		return w;
	}

	/**
	 * 绘制面板
	 * 
	 * @param g
	 *            画布
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		ControlUtils.setGraphicsRenderingHints(g);
		float scale = 1.0f;
		if (control != null) {
			scale = control.scale;
		}
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
				int rh = parser.getRowHeight(i, scale);
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
				int rw = parser.getColWidth(i, scale);
				if (x + rw > displayWin.x) {
					break;
				}
				x += rw;
			}
			drawStartCol = i;
		}

		// 绘制下划线使用的
		float underLineSize = 0.75f;
		underLineSize *= control.scale;
		((Graphics2D) g).setStroke(new BasicStroke(underLineSize));

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

				int colWidth = parser.getColWidth(col, scale);
				int width = colWidth;
				int height = parser.getRowHeight(row, scale);
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

				// 画格子之前统一设置一下不能冲出格子显示
				Shape oldClip = g.getClip();
				try {
					double dispWidth = width;
					double dispHeight = height;
					if (displayWin != null) {
						if (x + width > displayWin.x + displayWin.width) {
							dispWidth = Math.min(width, displayWin.x
									+ displayWin.width - x);
						}
						if (y + height > displayWin.y + displayWin.height) {
							dispHeight = Math.min(height, displayWin.y
									+ displayWin.height - y);
						}
					}
					// 格子可能画不全，起始位置也要调整
					double clipx = x;
					double clipy = y;
					if (displayWin != null) {
						clipx = Math.max(x, displayWin.x);
						clipy = Math.max(y, displayWin.y);
					}
					Rectangle2D oldRect = oldClip.getBounds2D();
					if (clipx + dispWidth > oldRect.getX() + oldRect.getWidth()) {
						dispWidth = oldRect.getX() + oldRect.getWidth() - clipx;
					}
					if (clipy + dispHeight > oldRect.getY()
							+ oldRect.getHeight()) {
						dispHeight = oldRect.getY() + oldRect.getHeight()
								- clipy;
					}
					g.setClip(new Rectangle2D.Double(clipx, clipy, dispWidth,
							dispHeight));

					// fill back color
					Color bkcolor = parser.getBackColor(row, col);
					if (bkcolor != null) {
						g.setColor(bkcolor);
						g.fillRect(x, y, width, height);
					}

					if (!ConfigOptions.bDispOutCell.booleanValue()) {
						drawText(g, row, col, x, y, width, height, scale);
					}

					drawFlag(g, x, y, parser, row, col, scale);

					// draw selectedCell
					if (isCellSelected(row, col)) {

						/**
						 * 更改单元格背景色
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

					if (control.isBreakPointCell(row, col)) {
						g.setColor(ConfigOptions.iBreakPointBColor);
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
							g.setColor(new Color(0, 255, 255));
							g.fillRect(cellX[r][c], cellY[r][c], cellW[r][c],
									cellH[r][c]);
							g.setPaintMode();

							if (!ConfigOptions.bDispOutCell.booleanValue()) {
								drawText(g, row, col, x, y, width, height,
										scale);
							}
						}
					}

				} finally {
					g.setClip(oldClip);
				}
				// draw border
				if (ConfigOptions.bGridline) { // 根据选项是否画网格线
					CellBorder.setEnv(g, borderStyle, row, col,
							parser.getRowCount(), parser.getColCount(),
							isEditing);
					CellBorder.drawBorder(x, y, width, height);
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
								Stroke oldStroke = ((Graphics2D) g).getStroke();
								float lineSize = 2.0f;
								lineSize *= control.scale;
								((Graphics2D) g).setStroke(new BasicStroke(
										lineSize));
								g.drawRect(cellX[row][col], cellY[row][col],
										cellW[row][col], cellH[row][col]);
								// g.drawRect(cellX[row][col] + 1,
								// cellY[row][col] + 1,
								// cellW[row][col] - 2,
								// cellH[row][col] - 2);
								((Graphics2D) g).setStroke(oldStroke);
								g.setPaintMode();
							}
						}
					}
				}

				x += colWidth;
			}
			y += parser.getRowHeight(row, scale);
		}

		if (this.control != null && ConfigOptions.bDispOutCell.booleanValue()) {
			// 如果冲出单元格显示，则最后画文字，使得文字在最上层
			for (int row = drawStartRow; row <= drawEndRow; row++) {
				for (int col = drawStartCol; col <= drawEndCol; col++) {
					if (cellX[row][col] == 0) {
						continue;
					}
					// draw cell content
					int height = parser.getRowHeight(row, scale);
					int w = parser.getColWidth(col, scale);
					int pw = getPaintableWidth(row, col, scale);
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
			// 复制的Area有可能超出绘画面板
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
		 * 参数为true：即重画时装载控件文本会导致滚动没法操作 false：不装载则输入文字
		 * 但重画时不装载控件文本,似乎某个情况下输入法输入文字时看不到
		 */
		initEditor(submitEditor(true));

		drawSelectedRectBorder(g);

		if (!isSub) {
			g.dispose();
		}
	}

	/**
	 * 单元格是否被选中
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	protected boolean isCellSelected(int row, int col) {
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
	 * 设置编辑框的文本
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

	public void setPreventChange(boolean preventChange) {
		this.preventChange = preventChange;
	}

	/**
	 * 重置编辑框的边界
	 */
	protected void resetEditorBounds() {
		if (editor == null || control == null
				|| control.getActiveCell() == null) {
			return;
		}
		Rectangle srcRect = spEditor.getBounds();
		String editingText = ((JTextComponent) editor).getText();
		int r = control.getActiveCell().getRow();
		int c = control.getActiveCell().getCol();
		if (ConfigOptions.bDispOutCell.booleanValue()) {
			int ew = getEditableWidth(editingText, r, c, control.scale);
			if (srcRect.width < ew) {
				editingText += "a"; // 提前加个字符宽度
			}
		}
		CellRect rect = getEditorBounds(editingText, r, c, control.scale);
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
		} else { // 同一个格子如果缩小了编辑框不用变小
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
	 * 设置光标
	 * 
	 * @param caretPosition
	 *            光标位置
	 * @param newText
	 *            新文本
	 */
	protected void setCaret(int caretPosition, String newText) {
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
	 * 通过程序设置了cell的value或exp，重新刷新到Editor控件 粘贴时,也要此动作
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
			try {
				preventChange = true;
				((JTextComponent) editor).setText(text);
				setCaret(i, text);
			} finally {
				preventChange = false;
			}
		}
	}

	/**
	 * 取要绘制的宽度
	 * 
	 * @param cr
	 *            行号
	 * @param cc
	 *            列号
	 * @return
	 */
	public int getPaintableWidth(int cr, int cc, float scale) {
		int w = parser.getColWidth(cc, scale);

		String drawText = ControlUtils.getCellText(cellSet, cr, cc, true);
		int indent = ConfigOptions.iIndent.intValue();
		float cw = parser.getColWidth(cc, scale) - indent;
		float ch = parser.getRowHeight(cr, scale);
		float textH = ControlUtils.getStringHeight(drawText, cw,
				GM.getScaleFont(scale));
		if (ch > textH) {
			return w;
		}

		byte halign = parser.getHAlign(cr, cc);
		if (halign == IStyle.HALIGN_LEFT) {
			for (int c = cc + 1; c <= cellSet.getColCount(); c++) {
				if (!parser.isColVisible(c))
					continue;
				NormalCell nc = (NormalCell) cellSet.getCell(cr, c);
				if (StringUtils.isValidString(nc.getExpString())) {
					break;
				}
				w += parser.getColWidth(c, scale);
			}
		} else if (halign == IStyle.HALIGN_RIGHT) {
			for (int c = cc - 1; c >= 1; c--) {
				if (!parser.isColVisible(c))
					continue;
				NormalCell nc = (NormalCell) cellSet.getCell(cr, c);
				if (StringUtils.isValidString(nc.getExpString())) {
					break;
				}
				w += parser.getColWidth(c, scale);
			}
		}

		return w;
	}

	/**
	 * 取要编辑的宽度
	 * 
	 * @param editingText
	 *            编辑文本
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public int getEditableWidth(String editingText, int row, int col,
			float scale) {
		int w = parser.getColWidth(col, scale);

		if (ConfigOptions.bDispOutCell.booleanValue()) {
			int textWidth = ControlUtils.getStringMaxWidth(editingText,
					GM.getScaleFont(scale));

			for (int c = col + 1; c <= cellSet.getColCount(); c++) {
				if (!parser.isColVisible(c))
					continue;
				NormalCell nc = (NormalCell) cellSet.getCell(row, c);
				if (StringUtils.isValidString(nc.getExpString())
						|| w > textWidth + 5) {
					break;
				}
				w += parser.getColWidth(c, scale);
			}
		}
		return w;
	}

	/**
	 * 取可以编辑的高度
	 * 
	 * @param text
	 *            文本
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @param maxW
	 *            最大宽度
	 * @return
	 */
	protected int getEditableHeight(String text, int row, int col, int maxW,
			float scale) {
		int h = parser.getRowHeight(row, scale);
		// 编辑控件本身要占宽度,多留出5个点
		float textH = ControlUtils.getStringHeight(text, maxW - 5,
				GM.getScaleFont(scale));
		for (int r = row + 1; r <= cellSet.getRowCount(); r++) {
			if (!parser.isRowVisible(r)) {
				continue;
			}
			if (h > textH + 3) {
				break;
			}
			h += parser.getRowHeight(r, scale);
		}
		return h;
	}

	/**
	 * 绘制选择区域的边框
	 * 
	 * @param g
	 */
	protected void drawSelectedRectBorder(Graphics g) {
		Area area = control.getSelectedArea(0);
		if (area == null) {
			return;
		}
		int row = area.getBeginRow();
		int col = area.getBeginCol();
		int endRow = area.getEndRow();
		int endCol = area.getEndCol();
		boolean drawTop = true;
		if (row < drawStartRow && endRow >= drawStartRow) { // 首行被隐藏
			row = drawStartRow;
			drawTop = false;
		}
		boolean drawLeft = true;
		if (col < drawStartCol && endCol >= drawStartCol) { // 首列被隐藏
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
			if (!parser.isColVisible(c)) {
				continue;
			}
			if (c >= drawStartCol) {
				width += parser.getColWidth(c, control.scale);
			}
		}
		int height = 0;
		for (int r = row; r <= endRow; r++) {
			if (!parser.isRowVisible(r)) {
				continue;
			}
			if (r >= drawStartRow) {
				height += parser.getRowHeight(r, control.scale);
			}
		}

		if (width == 0 || height == 0) {
			return;
		}
		row = Math.max(drawStartRow, row);
		float lineSize = 3.0f;
		lineSize *= control.scale;
		((Graphics2D) g).setStroke(new BasicStroke(lineSize));
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
	 * 画单元格标记
	 * 
	 * @param g
	 *            画板
	 * @param x
	 *            X坐标
	 * @param y
	 *            Y坐标
	 * @param parser
	 *            网格解析器
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 */
	public static void drawFlag(Graphics g, int x, int y, CellSetParser parser,
			int row, int col, float scale) {
		PgmNormalCell cell = (PgmNormalCell) parser.getCell(row, col);
		int maxFlagSize = Math.min(parser.getRowHeight(row, scale),
				parser.getColWidth(col, scale));
		int flagSize = Math.min(maxFlagSize, GC.FLAG_SIZE_SMALL);
		Color oldColor = g.getColor();
		if (StringUtils.isValidString(cell.getTip())) {
			g.setColor(Color.white);
			g.setXORMode(new Color(51, 153, 0));
			int w = parser.getColWidth(col, scale);
			int[] x1 = new int[] { x + w - flagSize, x + w, x + w };
			int[] y1 = new int[] { y, y, y + flagSize };

			g.fillPolygon(x1, y1, 3);
		}
		if (cell.isResultCell()) {
			g.setColor(Color.white);
			g.setXORMode(Color.BLUE);
			int h = parser.getRowHeight(row, scale);
			int w = parser.getColWidth(col, scale);
			int[] x1 = new int[] { x + w - flagSize, x + w, x + w };
			int[] y1 = new int[] { y + h, y + h, y + h - flagSize };

			g.fillPolygon(x1, y1, 3);
		}
		g.setPaintMode();
		g.setColor(oldColor);
	}

	/**
	 * 初始化格子的坐标
	 */
	public void initCellLocations() {
		int rows = parser.getRowCount() + 1;
		int cols = parser.getColCount() + 1;
		initCellLocations(rows, cols);
	}

	/**
	 * 初始化格子的坐标
	 * 
	 * @param rows
	 *            行数
	 * @param cols
	 *            列数
	 */
	public void initCellLocations(int rows, int cols) {
		cellX = new int[rows][cols];
		cellW = new int[rows][cols];
		cellY = new int[rows][cols];
		cellH = new int[rows][cols];
	}

	/**
	 * 绘制单元格中显示的文本
	 * 
	 * @param g
	 *            画板
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @param x
	 *            X坐标
	 * @param y
	 *            Y坐标
	 * @param w
	 *            宽度
	 * @param h
	 *            高度
	 * @param scale
	 *            显示比例
	 */
	protected void drawText(Graphics g, int row, int col, int x, int y, int w,
			int h, float scale) {
		String text = parser.getDispText(row, col);
		drawText(text, g, row, col, x, y, w, h, scale);
	}

	protected Color getCellColor(int row, int col) {
		return null;
	}

	protected String getCellDispText(int row, int col) {
		return null;
	}

	protected byte getHAlign(int row, int col) {
		return parser.getHAlign(row, col);
	}

	/**
	 * 绘制单元格中显示的文本
	 * 
	 * @param text
	 *            文本
	 * @param g
	 *            画板
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @param x
	 *            X坐标
	 * @param y
	 *            Y坐标
	 * @param w
	 *            宽度
	 * @param h
	 *            高度
	 * @param scale
	 *            显示比例
	 */
	protected void drawText(String text, Graphics g, int row, int col, int x,
			int y, int w, int h, float scale) {
		Font font = parser.getFont(row, col, scale);
		byte halign = getHAlign(row, col);
		byte valign = parser.getVAlign(row, col);

		Color c = parser.getForeColor(row, col);
		if (control.isBreakPointCell(row, col)) {
			c = Color.white;
		} else if (!isCellEditable(row, col)) {
			c = Color.darkGray;
		}
		try {
			// XORMode绘制不清晰，边缘模糊，还是用正常前景色
			// if (control.getStepPosition() != null
			// && !control.cellSet.isAutoCalc()) {
			// CellLocation cp = control.getStepPosition();
			// int rr = cp.getRow();
			// int cc = cp.getCol();
			// if (rr == row && cc == col) {
			// c = Color.white;
			// /*
			// * 前景色为白色时，如果冲出单元格显示，与背景同色看不出来了，所以用XOR模式显示
			// * 但是在最后finally要设置回paint模式
			// */
			// g.setXORMode(XOR_COLOR);
			// }
			// }

			boolean underLine = parser.isUnderline(row, col);
			int indent = ConfigOptions.iIndent.intValue();
			indent = (int) Math.floor(indent * scale);
			Color cellColor = getCellColor(row, col);
			if (cellColor != null)
				c = cellColor;
			String dispText = getCellDispText(row, col);
			if (dispText != null)
				text = dispText;
			ControlUtils.drawText(g, text, x, y, w, h, underLine, halign,
					valign, font, c, indent);
		} finally {
			g.setPaintMode();
		}
	}

	/**
	 * 将单元格坐标和高宽数组清零
	 */
	protected void clearCoordinate() {
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
	 * 取编辑框
	 * 
	 * @return
	 */
	public JComponent getEditor() {
		return editor;
	}

	/**
	 * 是否正在编辑
	 * 
	 * @return
	 */
	public boolean isEditing() {
		return editor != null && editor.isVisible();
	}

	/**
	 * 向编辑框中增加文本
	 * 
	 * @param text
	 *            要增加的文本
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
	 * 编辑器的鼠标点击
	 * 
	 * @param e
	 *            鼠标事件
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 */
	public void editorMousePressed(MouseEvent e, int row, int col) {
		if (editor == null)
			return;
		int x = e.getX();
		int y = e.getY();
		x -= cellX[row][col];
		y -= cellY[row][col];
		try {
			final MouseEvent e1 = new MouseEvent(editor, e.getID(),
					e.getWhen(), e.getModifiers(), x, y, 1, e.isPopupTrigger(),
					e.getButton());
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
			MouseListener[] ml = editor.getMouseListeners();
			if (ml != null) {
				for (int i = 0; i < ml.length; i++) {
					ml[i].mousePressed(e1);
				}
			}
		} catch (Throwable t) {
		}
	}

	/** 刷新文本框 */
	public static final byte MODE_PAINT = 0;
	/** 显示文本框 */
	public static final byte MODE_SHOW = 1;
	/** 隐藏文本框 */
	public static final byte MODE_HIDE = 2;

	/**
	 * 初始化编辑框
	 * 
	 * @param caretPosition
	 *            光标位置
	 */
	protected void initEditor(int caretPosition) {
		initEditor(caretPosition, MODE_PAINT);
	}

	/**
	 * 初始化编辑框
	 * 
	 * @param mode
	 *            MODE_PAINT,MODE_SHOW,MODE_HIDE
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

	protected void hideEditor() {
		if (editor != null && editor.isVisible()) {
			setEditorVisible(false);
		}
	}

	/**
	 * 初始化编辑框
	 * 
	 * @param caretPosition
	 *            光标位置
	 * @param mode
	 *            MODE_PAINT,MODE_SHOW,MODE_HIDE
	 */
	public void initEditor(int caretPosition, byte mode) {
		if (mode != MODE_PAINT) {
			editPos = null;
		}
		if (!editable || control == null || control.getActiveCell() == null) {
			hideEditor();
			return;
		}
		int row = control.getActiveCell().getRow();
		int col = control.getActiveCell().getCol();
		if (!isCellEditable(row, col)) {
			hideEditor();
			return;
		}

		if (GV.appSheet != null
				&& !((SheetSpl) GV.appSheet).isCellSetEditable()) {
			hideEditor();
			return;
		}
		if (row > cellSet.getRowCount() || col > cellSet.getColCount()) {
			return;
		}
		if (!parser.isRowVisible(row) || !parser.isColVisible(col)) {
			return;
		}

		// 如下代码阻止平铺时非激活的页面
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
			stopMatch();
			editPos = new CellLocation(row, col);
			if (!editor.isVisible()) {
				setEditorVisible(true);
			}
			editor.requestFocus();
			text = ControlUtils.getCellText(cellSet, row, col, isEditing);
			CellRect rect = getEditorBounds(text, row, col, control.scale);
			// editor.setBounds(rect.getBeginRow(), rect.getBeginCol(),
			// rect.getRowCount(), rect.getColCount());
			spEditor.setBounds(rect.getBeginRow(), rect.getBeginCol(),
					rect.getRowCount(), rect.getColCount());
			preventChange = true;
			try {
				undoExp = text;
				((JTextComponent) editor).setText(text);
				((JTextComponent) editor).setEditable(true);
			} finally {
				preventChange = false;
			}
			GV.isCellEditing = true;
			multiEditor.initRefCells(false);
			break;
		case MODE_PAINT:
			break;
		case MODE_HIDE:
			stopMatch();
			if (editor.isVisible()) {
				setEditorVisible(false);
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
		editor.setFont(parser.getFont(row, col, control.scale));
		spEditor.setBounds(spEditor.getBounds());
		spEditor.setBorder(BorderFactory.createLineBorder(Color.darkGray, 1));
	}

	protected void stopMatch() {
	}

	public void setEditorVisible(boolean isVisible) {
		if (spEditor != null)
			spEditor.setVisible(isVisible);
		if (editor != null)
			editor.setVisible(isVisible);
	}

	/**
	 * 取编辑框的边界
	 * 
	 * @param text
	 *            文本
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public CellRect getEditorBounds(String text, int row, int col, float scale) {
		int x = cellX[row][col], y = cellY[row][col], w = getEditableWidth(
				text, row, col, scale);
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
		int h = getEditableHeight(text, row, col, w, scale);
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
	 * 提交当前编辑器中的内容,返回光标位置
	 * 
	 * @return
	 */
	public int submitEditor() {
		return submitEditor(false);
	}

	/**
	 * 提交当前编辑器中的内容,返回光标位置
	 * 
	 * @param isPaint
	 *            是否刷新
	 * @return
	 */
	protected int submitEditor(boolean isPaint) {
		if (control == null || editor == null || !editor.isVisible()
				|| control.getActiveCell() == null) {
			return -1;
		}
		String text = ((JTextComponent) editor).getText();
		if (!isPaint && isCellEditing()) {
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
	 * 单元格正在编辑，网格中或者工具栏都算
	 * 
	 * @return
	 */
	protected boolean isCellEditing() {
		try {
			return isEditing && GV.isCellEditing
					|| GV.toolBarProperty.getWindowEditor().isFocusOwner();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 还原编辑框的文本
	 */
	public void undoEditor() {
		String text = ((JTextComponent) editor).getText();
		try {
			preventChange = true;
			if (StringUtils.isValidString(text)) {
				String newText = undoExp;
				if (text.equals(undoExp)) {
					newText = null;
				}
				((JTextComponent) editor).setText(newText);
				((ToolBarProperty) GV.toolBarProperty).setTextEditorText(
						newText, true);
				undoExp = text;
			} else {
				if (StringUtils.isValidString(undoExp)) {
					((JTextComponent) editor).setText(undoExp);
					((ToolBarProperty) GV.toolBarProperty).setTextEditorText(
							undoExp, true);
					undoExp = null;
				}
			}
		} finally {
			preventChange = false;
		}
	}

	/**
	 * 获得输入法申请对象
	 * 
	 * @return 输入法申请对象
	 */
	public InputMethodRequests getInputMethodRequests() {
		return this;
	}

	/**
	 * 取工具提示的坐标
	 */
	public Point getToolTipLocation(MouseEvent e) {
		return getTipPos(e.getX(), e.getY());
	}

	/**
	 * 取单元格提示的坐标
	 * 
	 * @param x1
	 *            X坐标
	 * @param y1
	 *            Y坐标
	 * @return
	 */
	protected Point getTipPos(int x1, int y1) {
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
	 * 从输入法控件中输入新的文本时的处理
	 * 
	 * @param event
	 *            输入法事件
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
				if (editor != null && editor instanceof JTextComponent) {
					if (StringUtils.isValidString(composedText)) {
						if (editor == null || !editor.isVisible()) {
							initEditor(MODE_SHOW);
						}
						((JTextComponent) editor).setText(composedText);
						editor.requestFocus();
					}
				}
			}
		} catch (Throwable t) {
		}
	}

	/**
	 * 光标位置变化
	 */
	public void caretPositionChanged(InputMethodEvent event) {
	}

	/**
	 * 取文本的位置
	 */
	public Rectangle getTextLocation(TextHitInfo offset) {
		return new Rectangle(0, 0);
	}

	/**
	 * 取抵消的位置
	 */
	public TextHitInfo getLocationOffset(int x, int y) {
		return null;
	}

	/**
	 * 取插入坐标抵消的位置
	 */
	public int getInsertPositionOffset() {
		return 0;
	}

	/**
	 * 取提交的文本
	 */
	public AttributedCharacterIterator getCommittedText(int beginIndex,
			int endIndex, Attribute[] attributes) {
		return null;
	}

	/**
	 * 取提交文本的长度
	 */
	public int getCommittedTextLength() {
		return 0;
	}

	/**
	 * 取消最后提交的文本
	 */
	public AttributedCharacterIterator cancelLatestCommittedText(
			Attribute[] attributes) {
		return null;
	}

	/**
	 * 取选择的文本
	 */
	public AttributedCharacterIterator getSelectedText(Attribute[] attributes) {
		return null;
	}

	/**
	 * 关闭
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
