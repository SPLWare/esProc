package com.scudata.ide.spl.control;

import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.JTextComponent;

import com.scudata.cellset.ICellSet;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.Area;
import com.scudata.common.ByteMap;
import com.scudata.common.CellLocation;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.control.ControlBase;
import com.scudata.ide.common.control.ControlUtilsBase;
import com.scudata.ide.spl.SheetSpl;

/**
 * 网格编辑控件
 *
 */
public abstract class SplControl extends ControlBase {

	private static final long serialVersionUID = 1L;

	/**
	 * 网格对象
	 */
	public PgmCellSet cellSet;

	/** 在编辑时被选中的单元格区域 */
	public Vector<Object> m_selectedAreas = new Vector<Object>();

	/** 当前单元格的位置 */
	public CellLocation m_activeCell;

	/** 单步执行时的当前位置 */
	public CellLocation stepPosition;

	/** 断点 */
	protected ArrayList<CellLocation> breakPoints = new ArrayList<CellLocation>();

	/** 计算的坐标 */
	protected CellLocation calcPos;

	/** 编辑状态 */
	int status;

	/** 编辑时被选中的列号集 */
	Vector<Integer> m_selectedCols = new Vector<Integer>();

	/** 编辑时被选中的行号集 */
	Vector<Integer> m_selectedRows = new Vector<Integer>();

	/** 编辑时是否选中了整个网格 */
	boolean m_cornerSelected = false;

	/** 编辑监听器集合 */
	ArrayList<EditorListener> m_editorListener;

	/** 内容面板 */
	public ContentPanel contentView = null;

	/** 编辑时各列首格的X坐标数组 */
	int[] cellX;

	/** 编辑时各行首格的Y坐标数组 */
	int[] cellY;

	/** 编辑时各列首格的宽度数组 */
	int[] cellW;

	/** 编辑时各行首格的高度数组 */
	int[] cellH;

	/** 显示比例 */
	public float scale = 1.0f;

	/**
	 * 当前的鼠标动作是选择格子还是选中编辑格
	 */
	public boolean isSelectingCell = false;

	/**
	 * 行表头和列表头面板
	 */
	JPanel rowHeaderView = null, colHeaderView = null;
	/**
	 * 列表头面板
	 */
	ColHeaderPanel headerPanel = null;

	/**
	 * 页面对象
	 */
	protected SheetSpl sheet;

	/**
	 * 构造函数
	 */
	public SplControl() {
		this(1, 1);
	}

	/**
	 * 构造函数
	 * 
	 * @param rows
	 *            行数
	 * @param cols
	 *            列数
	 */
	public SplControl(int rows, int cols) {
		super();
		m_editorListener = new ArrayList<EditorListener>();
		getHorizontalScrollBar().setUnitIncrement(10);
		getVerticalScrollBar().setUnitIncrement(10);
		cellSet = new PgmCellSet(rows, cols);
	}

	/**
	 * 设置Sheet对象
	 * 
	 * @param sheet
	 */
	public void setSheet(SheetSpl sheet) {
		this.sheet = sheet;
	}

	/**
	 * 取Sheet对象
	 * 
	 * @return
	 */
	public SheetSpl getSheet() {
		return sheet;
	}

	/**
	 * 设置网格滚动条监听器
	 */
	public void setSplScrollBarListener() {
		getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				// 如果刷新慢加上延时刷新，暂时速度很快不加了
				contentView.repaint();
			}
		});

		getHorizontalScrollBar().addAdjustmentListener(
				new AdjustmentListener() {
					public void adjustmentValueChanged(AdjustmentEvent e) {
						contentView.repaint();
					}
				});

	}

	/**
	 * 设置格子是否选中
	 * 
	 * @param isSelect
	 */
	public void setSelectCell(boolean isSelect) {
		this.isSelectingCell = isSelect;
	}

	/**
	 * 取网格面板
	 * 
	 * @return
	 */
	public ContentPanel getContentPanel() {
		return contentView;
	}

	/**
	 * 取行表头面板
	 * 
	 * @return
	 */
	public JPanel getRowHeaderPanel() {
		return rowHeaderView;
	}

	/**
	 * 取列表头面板
	 * 
	 * @return
	 */
	public JPanel getColHeaderPanel() {
		return colHeaderView;
	}

	/**
	 * 设置断点
	 * 
	 * @param breakPoints
	 */
	public void setBreakPoints(ArrayList<CellLocation> breakPoints) {
		this.breakPoints = breakPoints;
	}

	/**
	 * 取断点
	 * 
	 * @return
	 */
	public ArrayList<CellLocation> getBreakPoints() {
		return breakPoints;
	}

	/**
	 * 是否设置了断点的行
	 * 
	 * @param row
	 *            int
	 * @return boolean
	 */
	public boolean isBreakPointRow(int row) {
		Iterator<CellLocation> it = breakPoints.iterator();
		while (it.hasNext()) {
			CellLocation cp = it.next();
			if (cp.getRow() == row) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 删除行断点
	 * 
	 * @param row
	 *            指定的行号
	 */
	public void removeRowBreakPoints(int row) {
		for (int i = breakPoints.size() - 1; i >= 0; i--) {
			CellLocation cp = (CellLocation) breakPoints.get(i);
			if (cp.getRow() == row) {
				breakPoints.remove(i);
			} else if (cp.getRow() > row) {
				cp.setRow(cp.getRow() - 1);
			}
		}
	}

	/**
	 * 删除列断点
	 * 
	 * @param col
	 *            指定的列号
	 */
	public void removeColBreakPoints(int col) {
		for (int i = breakPoints.size() - 1; i >= 0; i--) {
			CellLocation cp = (CellLocation) breakPoints.get(i);
			if (cp.getCol() == col) {
				breakPoints.remove(i);
			} else if (cp.getCol() > col) {
				cp.setCol(cp.getCol() - 1);
			}
		}
	}

	/**
	 * 设置断点
	 */
	public void setBreakPoint() {
		CellLocation cp = getActiveCell();
		if (cp == null) {
			return;
		}
		if (breakPoints.contains(cp)) {
			breakPoints.remove(cp);
		} else {
			breakPoints.add(cp);
		}
		repaint();
	}

	/**
	 * 是否断点格
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public boolean isBreakPointCell(int row, int col) {
		Iterator<CellLocation> it = breakPoints.iterator();
		while (it.hasNext()) {
			CellLocation cp = it.next();
			if (cp.getRow() == row && cp.getCol() == col) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否选中的格
	 * 
	 * @return
	 */
	public boolean isSelectingCell() {
		return isSelectingCell;
	}

	/**
	 * 取当前格
	 * 
	 * @return
	 */
	public CellLocation getActiveCell() {
		return m_activeCell;
	}

	/**
	 * 设置单步调试执行格坐标
	 * 
	 * @param cp
	 *            单步调试执行格坐标
	 */
	public void setStepPosition(CellLocation cp) {
		this.stepPosition = cp;
	}

	/**
	 * 取单步调试执行格坐标
	 * 
	 * @return
	 */
	public CellLocation getStepPosition() {
		return stepPosition;
	}

	/**
	 * 设置要计算的格坐标
	 * 
	 * @param cp
	 */
	public void setCalcPosition(CellLocation cp) {
		this.calcPos = cp;
	}

	/**
	 * 取要计算的格坐标
	 * 
	 * @return
	 */
	public CellLocation getCalcPosition() {
		return calcPos;
	}

	/**
	 * 重新加载编辑控件的文本
	 */
	public void reloadEditorText() {
		this.contentView.reloadEditorText();
	}

	/**
	 * 取格子的坐标
	 * 
	 * @param row
	 *            格子的行号
	 * @param col
	 *            格子的列号
	 * @return
	 */
	public Point[] getCellPoint(int row, int col) {
		Point p[] = new Point[2];
		int x1 = cellX[col];
		int y1 = cellY[row];
		int x2 = x1 + cellW[col];
		int y2 = y1 + cellH[row];
		p[0] = new Point(x1, y1);
		p[1] = new Point(x2, y2);
		return p;
	}

	/**
	 * 设置当前格
	 * 
	 * @param pos
	 *            格子的坐标
	 * @return
	 */
	public Area setActiveCell(CellLocation pos) {
		return setActiveCell(pos, true);
	}

	/**
	 * 设置当前格
	 * 
	 * @param pos
	 *            格子的坐标
	 * @param clearSelection
	 *            是否清除之前的选择区域
	 * @return
	 */
	public Area setActiveCell(CellLocation pos, boolean clearSelection) {
		return setActiveCell(pos, clearSelection, true);
	}

	/**
	 * 设置当前格
	 * 
	 * @param pos
	 *            当前单元格位置 clearSelection : 通常情况要清空选择，特殊为选中行列时，移动控件到选择框，避免多区域选择
	 *            多区域选择有很多限制，不能插入等 返回设置该位置后的调整过的区域,用于上层记住选中的区域
	 * @param clearSelection
	 *            是否清除之前的选择区域
	 * @param scrollToVisible
	 *            当前格没有显示时，是否滚动到当前格使其显示
	 * @return
	 */
	public Area setActiveCell(CellLocation pos, boolean clearSelection,
			boolean scrollToVisible) {
		pos = ControlUtilsBase.checkPosition(pos, getCellSet());
		Area a;
		if (pos == null) {
			contentView.submitEditor();
			m_activeCell = null;
			contentView.setEditorVisible(false);
			a = null;
		} else {
			int row = pos.getRow();
			int col = pos.getCol();
			a = new Area(row, col, row, col);
			contentView.rememberedRow = row;
			contentView.rememberedCol = col;

			if (clearSelection) {
				m_selectedRows.clear();
				m_selectedCols.clear();
				m_cornerSelected = false;
			}

			contentView.submitEditor();
			m_activeCell = pos;
			if (scrollToVisible)
				ControlUtils.scrollToVisible(getViewport(), this, pos.getRow(),
						pos.getCol());
			repaint();
			contentView.initEditor(ContentPanel.MODE_HIDE);
			contentView.requestFocus();
		}

		return a;
	}

	/**
	 * 将当前单元格上一行同一列位置的单元格变为当前单元格
	 */
	public Area toUpCell() {
		if (m_activeCell == null) {
			return null;
		}
		CellSetParser parser = new CellSetParser(this.cellSet);
		int row = m_activeCell.getRow();
		int col = m_activeCell.getCol();
		row--;
		if (row < 1)
			return null;
		while (!parser.isRowVisible(row)) {
			row--;
			if (row < 1) {
				return null;
			}
		}
		if (row < 1) {
			return null;
		}
		return setActiveCell(new CellLocation(row, col));
	}

	/**
	 * 将当前单元格下一行同一列位置的单元格变为当前单元格
	 */
	public Area toDownCell() {
		if (m_activeCell == null) {
			return null;
		}
		CellSetParser parser = new CellSetParser(this.cellSet);
		int row = m_activeCell.getRow();
		int col = m_activeCell.getCol();
		row++;
		if (row > cellSet.getRowCount())
			return null;
		while (!parser.isRowVisible(row)) {
			row++;
			if (row > contentView.cellSet.getRowCount()) {
				return null;
			}
		}

		if (row > contentView.cellSet.getRowCount()) {
			return null;
		}
		return setActiveCell(new CellLocation(row, col));
	}

	/**
	 * 将当前单元格左边一列同一行位置的单元格变为当前单元格
	 */
	public Area toLeftCell() {
		if (m_activeCell == null) {
			return null;
		}
		int row = m_activeCell.getRow();
		int col = m_activeCell.getCol();
		col--;
		if (col < 1) {
			return null;
		}
		CellSetParser parser = new CellSetParser(cellSet);
		while (!parser.isColVisible(col)) {
			col--;
			if (col < 1) {
				return null;
			}
		}
		if (col < 1) {
			return null;
		}
		return setActiveCell(new CellLocation(row, col));
	}

	/**
	 * 将当前单元格右边一列同一行位置的单元格变为当前单元格
	 */
	public Area toRightCell() {
		if (m_activeCell == null) {
			return null;
		}
		int row = m_activeCell.getRow();
		int col = m_activeCell.getCol();
		CellSetParser parser = new CellSetParser(cellSet);
		col++;
		if (col > contentView.cellSet.getColCount()) {
			return null;
		}
		while (!parser.isColVisible(col)) {
			col++;
			if (col > contentView.cellSet.getColCount()) {
				return null;
			}
		}

		if (col > contentView.cellSet.getColCount()) {
			return null;
		}
		updateCoords();
		return setActiveCell(new CellLocation(row, col));
	}

	/**
	 * 在表格最后一列点击"tab"键在末列后增加一列时，更新表格列首的坐标和宽度
	 */
	protected void updateCoords() {
		int cols = cellSet.getColCount() + 1;
		if (cellX == null || cols != cellX.length) {
			cellX = new int[cols];
			cellW = new int[cols];
		}
		CellSetParser parser = new CellSetParser(cellSet);
		for (int i = 1; i < cols; i++) {
			if (i == 1) {
				cellX[i] = 1;
			} else {
				cellX[i] = cellX[i - 1] + cellW[i - 1];
			}
			if (!parser.isColVisible(i)) {
				cellW[i] = 0;
			} else {
				cellW[i] = (int) cellSet.getColCell(i).getWidth();
			}
		}
	}

	/**
	 * 将当前选择区域扩展到指定区域，按SHIFT+按键时调用
	 * 
	 * @param region
	 *            要扩展到的区域
	 */
	public void selectToArea(Area region) {
		addSelectedArea(region, true);
		m_selectedCols.clear();
		m_selectedRows.clear();
		m_cornerSelected = false;
		fireRegionSelect(true);
		ControlUtils.scrollToVisible(getViewport(), this, region.getBeginRow(),
				region.getEndCol());
		repaint();
		contentView.requestFocus();
	}

	/**
	 * 将当前选择区域扩展到当前区域下一行同一列位置
	 * 
	 * @param tarPos
	 *            目标格子坐标
	 */
	public void selectToDownCell(CellLocation tarPos) {
		Area region = getSelectedArea(-1);
		int startRow = region.getBeginRow();
		int endRow = region.getEndRow();
		int startCol = region.getBeginCol();
		int endCol = region.getEndCol();
		int row = m_activeCell.getRow();
		CellSetParser parser = new CellSetParser(cellSet);
		if (tarPos != null) {
			int tarRow = tarPos.getRow();
			if (startRow < row) {
				if (tarRow > row) {
					startRow = row;
					endRow = tarRow;
				} else {
					startRow = tarRow;
				}
			} else {
				endRow = tarRow;
			}
		} else {
			if (startRow < row) {
				int nextRow = startRow;
				while (true) {
					int tempRow = nextRow;
					boolean hasCellCrossRows = false;
					if (nextRow > contentView.cellSet.getRowCount()) {
						break;
					}
					if (!hasCellCrossRows) {
						nextRow = tempRow + 1;
						if (nextRow > contentView.cellSet.getRowCount()) {
							break;
						}
						if (!parser.isRowVisible(nextRow))
							continue;
						break;
					}
				}
				if (nextRow > row) {
					nextRow = row;
					endRow++;
				}
				startRow = nextRow;
			} else {
				endRow++;
				if (endRow > contentView.cellSet.getRowCount())
					return;
				while (!parser.isRowVisible(endRow)) {
					endRow++;
					if (endRow > contentView.cellSet.getRowCount())
						return;
				}
			}
		}
		if (endRow > contentView.cellSet.getRowCount()
				|| !parser.isRowVisible(endRow)) {
			return;
		}
		region = new Area(startRow, startCol, endRow, endCol);
		addSelectedArea(region, true);
		m_selectedCols.clear();
		m_selectedRows.clear();
		m_cornerSelected = false;
		fireRegionSelect(true);
		ControlUtils.scrollToVisible(getViewport(), this, region.getEndRow(),
				region.getEndCol());
		repaint();
		contentView.requestFocus();
	}

	/**
	 * 将当前选择区域扩展到当前区域上一行同一列位置
	 * 
	 * @param tarPos
	 *            目标格子坐标
	 */
	void selectToUpCell(CellLocation tarPos) {
		Area region = getSelectedArea(-1);
		int startRow = region.getBeginRow();
		int endRow = region.getEndRow();
		int startCol = region.getBeginCol();
		int endCol = region.getEndCol();
		int row = m_activeCell.getRow();
		CellSetParser parser = new CellSetParser(cellSet);
		if (tarPos != null) {
			int tarRow = tarPos.getRow();
			if (endRow > row) {
				if (tarRow < row) {
					startRow = tarRow;
					endRow = row;
				} else {
					endRow = tarRow;
				}
			} else {
				startRow = tarRow;
			}
		} else {
			if (endRow > row) {
				int nextRow = endRow;
				while (true) {
					int tempRow = nextRow;
					boolean hasCellCrossRows = false;
					if (nextRow < 1) {
						break;
					}
					if (!hasCellCrossRows) {
						nextRow = tempRow - 1;
						if (nextRow < 1) {
							break;
						}
						if (!parser.isRowVisible(nextRow))
							continue;
						break;
					}
				}
				if (nextRow < row) {
					nextRow = row;
					startRow--;
				}
				endRow = nextRow;
			} else {
				startRow--;
				if (startRow < 1)
					return;
				while (!parser.isRowVisible(startRow)) {
					startRow--;
					if (startRow < 1)
						return;
				}
			}
		}
		if (startRow < 1 || !parser.isRowVisible(startRow)) {
			return;
		}
		region = new Area(startRow, startCol, endRow, endCol);

		addSelectedArea(region, true);
		m_selectedCols.clear();
		m_selectedRows.clear();
		m_cornerSelected = false;
		fireRegionSelect(true);
		ControlUtils.scrollToVisible(getViewport(), this, region.getBeginRow(),
				region.getEndCol());
		repaint();
		contentView.requestFocus();
	}

	/**
	 * 将当前选择区域扩展到当前区域下一列同一行位置
	 * 
	 * @param tarPos
	 *            目标格子坐标
	 */
	void selectToRightCell(CellLocation tarPos) {
		Area region = getSelectedArea(-1);
		int startRow = region.getBeginRow();
		int endRow = region.getEndRow();
		int startCol = region.getBeginCol();
		int endCol = region.getEndCol();
		int col = m_activeCell.getCol();
		if (tarPos != null) {
			int tarCol = tarPos.getCol();
			if (startCol < col) {
				if (tarCol > col) {
					startCol = col;
					endCol = tarCol;
				} else {
					startCol = tarCol;
				}
			} else {
				endCol = tarCol;
			}
		} else {
			if (startCol < col) {
				int nextCol = startCol;
				while (true) {
					int tempCol = nextCol;
					boolean hasCellCrossCols = false;
					if (nextCol > contentView.cellSet.getColCount()) {
						break;
					}
					if (!hasCellCrossCols) {
						nextCol = tempCol + 1;
						break;
					}
				}
				if (nextCol > col) {
					nextCol = col;
					endCol++;
				}
				startCol = nextCol;
			} else {
				endCol++;
			}
		}
		if (endCol > contentView.cellSet.getColCount()) {
			return;
		}
		region = new Area(startRow, startCol, endRow, endCol);

		addSelectedArea(region, true);
		m_selectedCols.clear();
		m_selectedRows.clear();
		m_cornerSelected = false;
		fireRegionSelect(true);
		ControlUtils.scrollToVisible(getViewport(), this, region.getEndRow(),
				region.getEndCol());
		repaint();
		contentView.requestFocus();
	}

	/**
	 * 将当前选择区域扩展到当前区域上一列同一行位置
	 * 
	 * @param tarPos
	 *            目标格子坐标
	 */
	void selectToLeftCell(CellLocation tarPos) {
		Area region = getSelectedArea(-1);
		int startRow = region.getBeginRow();
		int endRow = region.getEndRow();
		int startCol = region.getBeginCol();
		int endCol = region.getEndCol();
		int col = m_activeCell.getCol();
		if (tarPos != null) {
			int tarCol = tarPos.getCol();
			if (endCol > col) {
				if (tarCol < col) {
					startCol = tarCol;
					endCol = col;
				} else {
					endCol = tarCol;
				}
			} else {
				startCol = tarCol;
			}
		} else {
			if (endCol > col) {
				int nextCol = endCol;
				while (true) {
					int tempCol = nextCol;
					boolean hasCellCrossCols = false;
					if (nextCol < 1) {
						break;
					}
					if (!hasCellCrossCols) {
						nextCol = tempCol - 1;
						break;
					}
				}
				if (nextCol < col) {
					nextCol = col;
					startCol--;
				}
				endCol = nextCol;
			} else {
				startCol--;
			}
		}
		if (startCol < 1) {
			return;
		}
		region = new Area(startRow, startCol, endRow, endCol);

		addSelectedArea(region, true);
		m_selectedCols.clear();
		m_selectedRows.clear();
		m_cornerSelected = false;
		fireRegionSelect(true);
		ControlUtils.scrollToVisible(getViewport(), this, region.getBeginRow(),
				region.getBeginCol());
		repaint();
		contentView.requestFocus();
	}

	/**
	 * 增加选择的列
	 * 
	 * @param c
	 *            列号
	 */
	public void addSelectedCol(Integer c) {
		if (m_selectedCols.contains(c)) {
			return;
		}
		m_selectedCols.add(c);
	}

	/**
	 * 增加选择的行
	 * 
	 * @param r
	 *            行号
	 */
	public void addSelectedRow(Integer r) {
		if (m_selectedRows.contains(r)) {
			return;
		}
		m_selectedRows.add(r);
	}

	/**
	 * 清除选择的区域
	 */
	public void clearSelectedArea() {
		m_selectedAreas.clear();
	}

	/**
	 * 取选择的区域
	 * 
	 * @return
	 */
	public Vector<Object> getSelectedAreas() {
		return m_selectedAreas;
	}

	/**
	 * 设置选择的区域
	 * 
	 * @param newAreas
	 *            选择的区域
	 */
	public void setSelectedAreas(Vector<Object> newAreas) {
		m_selectedAreas = newAreas;
	}

	/**
	 * 取得指定的区域，index<0时表示取最后的Top区域
	 * 
	 * @param index
	 *            int
	 * @return Area
	 */
	public Area getSelectedArea(int index) {
		if (m_selectedAreas.isEmpty()) {
			return null;
		}
		if (index < 0) {
			index = m_selectedAreas.size() - 1;
		}
		return (Area) m_selectedAreas.get(index);
	}

	/**
	 * 设置选择的区域
	 * 
	 * @param a
	 *            选择的区域
	 */
	public void setSelectedArea(Area a) {
		if (a == null) {
			return;
		}
		clearSelectedArea();
		m_selectedAreas.add(a);
	}

	/**
	 * 全选整个网格
	 */
	public void selectAll() {
		m_cornerSelected = true;
		int rows = cellSet.getRowCount();
		int cols = (int) cellSet.getColCount();

		m_selectedCols.clear();
		for (int i = 1; i <= cols; i++) {
			m_selectedCols.add(new Integer(i));
		}
		m_selectedRows.clear();
		for (int i = 1; i <= rows; i++) {
			// conrol的add方法会判断有否重复行,行多时很费时间
			m_selectedRows.add(new Integer(i));
		}
		setSelectedArea(new Area(1, (int) 1, rows, cols));
		repaint();
		fireRegionSelect(true);
	}

	/**
	 * 增加选择的区域
	 * 
	 * @param a
	 *            选择的区域
	 * @param removeLast
	 *            是否删除上一次选择的区域
	 */
	public void addSelectedArea(Area a, boolean removeLast) {
		if (a == null || m_selectedAreas.contains(a)) {
			return;
		}
		if (removeLast && !m_selectedAreas.isEmpty()) {
			m_selectedAreas.remove(m_selectedAreas.size() - 1);
		}
		m_selectedAreas.add(a);
	}

	/**
	 * 生成并绘制控件
	 */
	public void draw() {
		JPanel corner = createCorner();
		if (corner != null) {
			this.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, corner);
		}

		colHeaderView = createColHeaderView();
		if (colHeaderView != null) {
			JViewport colHeader = new JViewport();
			this.setColumnHeader(colHeader);
			this.setColumnHeaderView(colHeaderView);
		}
		rowHeaderView = createRowHeaderView();
		if (rowHeaderView != null) {
			JViewport rowHeader = new JViewport();
			this.setRowHeader(rowHeader);
			this.setRowHeaderView(rowHeaderView);
		}
		contentView = createContentView();
		if (contentView != null) {
			this.getViewport().setView(contentView);
			this.getViewport().setAutoscrolls(true);
		}
	}

	/**
	 * 滚动到指定区域
	 * 
	 * @param newArea
	 *            要显示的区域
	 */
	public void scrollToArea(Area newArea) {
		if (newArea == null) {
			return;
		}
		setSelectedArea(newArea);
		if (ControlUtils.scrollToVisible(getViewport(), this,
				newArea.getBeginRow(), newArea.getBeginCol())) {
			ContentPanel cp = getContentPanel();
			JScrollBar hBar = getHorizontalScrollBar();
			JScrollBar vBar = getVerticalScrollBar();

			hBar.setValue(cp.getColOffset(newArea.getBeginCol(), scale));
			vBar.setValue(cp.getRowOffset(newArea.getBeginRow(), scale));

		}
		fireRegionSelect(true);
	}

	/**
	 * 生成网格的左上角面板
	 * 
	 * @return
	 */
	abstract JPanel createCorner();

	/**
	 * 生成网格的上表头面板
	 * 
	 * @return
	 */
	abstract JPanel createColHeaderView();

	/**
	 * 生成网格的左表头面板
	 * 
	 * @return
	 */
	abstract JPanel createRowHeaderView();

	/** 生成网格的内容面板 */
	abstract ContentPanel createContentView();

	/**
	 * 设置网格
	 * 
	 * @param cellSet
	 *            网格对象
	 */
	public void setCellSet(PgmCellSet cellSet) {
		this.cellSet = cellSet;
		scale = 1.0f;
		ByteMap bm = cellSet.getCustomPropMap();
		if (bm != null) {
			Object scaleObj = bm.get(GC.CELLSET_SCALE);
			if (scaleObj != null) {
				scale = ((Number) scaleObj).floatValue();
			}
		}
		draw();
	}

	/**
	 * 获得网格对象
	 * 
	 * @return 网格对象
	 */
	public PgmCellSet getCellSet() {
		return cellSet;
	}

	/**
	 * 获得网格对象接口
	 */
	public ICellSet getICellSet() {
		return cellSet;
	}

	/**
	 * 添加网格编辑事件监听器
	 * 
	 * @param listener
	 *            监听器实例
	 */
	public void addEditorListener(EditorListener listener) {
		this.m_editorListener.add(listener);
	}

	/**
	 * 触发行高调整消息
	 * 
	 * @param vectHeader
	 *            行编号集合
	 * @param newHeight
	 *            新的行高值
	 */
	void fireRowHeaderResized(Vector<Integer> vectHeader, float newHeight) {
		for (int i = 0; i < this.m_editorListener.size(); i++) {
			EditorListener listener = (EditorListener) this.m_editorListener
					.get(i);
			listener.rowHeightChange(vectHeader, newHeight);
		}
		Point hp = this.getRowHeader().getViewPosition();
		Point p = this.getViewport().getViewPosition();
		this.getRowHeader().setView(
				this.rowHeaderView == null ? this.createRowHeaderView()
						: this.rowHeaderView);
		this.getRowHeader().setViewPosition(hp);
		this.getViewport().setViewPosition(p);
		contentView.requestFocus();
		repaint();
	}

	/**
	 * 触发列宽调整消息
	 * 
	 * @param vectHeader
	 *            列编号集合
	 * @param newWidth
	 *            新的列宽值
	 */
	void fireColHeaderResized(Vector<Integer> vectHeader, float newWidth) {
		for (int i = 0; i < this.m_editorListener.size(); i++) {
			EditorListener listener = (EditorListener) this.m_editorListener
					.get(i);
			listener.columnWidthChange(vectHeader, newWidth);
		}
		Point hp = this.getColumnHeader().getViewPosition();
		Point p = this.getViewport().getViewPosition();
		this.getColumnHeader().setView(
				this.colHeaderView == null ? this.createColHeaderView()
						: this.colHeaderView);
		this.getColumnHeader().setViewPosition(hp);
		this.getViewport().setViewPosition(p);
		contentView.requestFocus();

		repaint();
	}

	/**
	 * 触发区域移动消息，未实现
	 * 
	 * @return
	 */
	void fireRegionMove() {
	}

	/**
	 * 触发区域粘贴消息，未实现
	 * 
	 * @return
	 */
	void fireRegionPaste() {
	}

	/**
	 * 触发区域选择消息
	 * 
	 * @param keyEvent
	 *            boolean 键盘事件不触发属性值刷新
	 */
	void fireRegionSelect(boolean keyEvent) {
		for (int i = 0; i < this.m_editorListener.size(); i++) {
			EditorListener listener = (EditorListener) this.m_editorListener
					.get(i);
			listener.regionsSelect(m_selectedAreas, this.m_selectedRows,
					this.m_selectedCols, this.m_cornerSelected, keyEvent);
		}
	}

	/**
	 * 触发单元格文本值编辑结束消息
	 * 
	 * @param pos
	 *            被编辑的单元格位置
	 * @param text
	 *            新输入的文本
	 */
	public void fireCellTextInput(CellLocation pos, String text) {
		for (int i = 0; i < this.m_editorListener.size(); i++) {
			EditorListener listener = (EditorListener) this.m_editorListener
					.get(i);
			listener.cellTextInput(pos.getRow(), pos.getCol(), text);
		}
	}

	/**
	 * 触发单元格文本正被编辑消息
	 * 
	 * @param text
	 *            正在被编辑的文本
	 */
	void fireEditorInputing(String text) {
		for (int i = 0; i < this.m_editorListener.size(); i++) {
			EditorListener listener = (EditorListener) this.m_editorListener
					.get(i);
			listener.editorInputing(text);
		}
	}

	/**
	 * 鼠标移动事件
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 */
	void fireMouseMove(int row, int col) {
		for (int i = 0; i < this.m_editorListener.size(); i++) {
			EditorListener listener = (EditorListener) this.m_editorListener
					.get(i);
			listener.mouseMove(row, col);
		}
	}

	/**
	 * 触发鼠标右击事件
	 * 
	 * @param e
	 *            鼠标事件
	 * @param clickPlace
	 *            右击位置，GC中定义的常量
	 */
	void fireRightClicked(MouseEvent e, int clickPlace) {
		for (int i = 0; i < this.m_editorListener.size(); i++) {
			EditorListener listener = (EditorListener) this.m_editorListener
					.get(i);
			listener.rightClicked(e, clickPlace);
		}
	}

	/**
	 * 触发鼠标双击事件
	 * 
	 * @param e
	 */
	void fireDoubleClicked(MouseEvent e) {
		for (int i = 0; i < this.m_editorListener.size(); i++) {
			EditorListener listener = (EditorListener) this.m_editorListener
					.get(i);
			listener.doubleClicked(e);
		}
	}

	/**
	 * 插入列
	 * 
	 * @param col
	 *            插入位置
	 * @param count
	 *            插入的列数
	 */
	public List<NormalCell> insertColumn(int col, int count) {
		if (col > cellSet.getColCount() || col < 0) {
			col = cellSet.getColCount();
		}
		List<NormalCell> cells = insertCol(cellSet, col, count);
		resetControlWidth();
		return cells;
	}

	/**
	 * 插入列
	 * 
	 * @param cellSet
	 * @param col
	 * @param count
	 */
	protected List<NormalCell> insertCol(PgmCellSet cellSet, int col, int count) {
		return cellSet.insertCol(col, count);
	}

	/**
	 * 追加列
	 * 
	 * @param count
	 *            追加的列数
	 */
	public void addColumn(int count) {
		addCol(cellSet, count);
		resetControlWidth();
	}

	/**
	 * 追加列
	 * 
	 * @param cellSet
	 * @param count
	 */
	protected void addCol(PgmCellSet cellSet, int count) {
		cellSet.addCol(count);
	}

	/**
	 * 当插入列、改变列宽时，重设控件宽度
	 */
	protected void resetControlWidth() {
		Point hp = this.getColumnHeader().getViewPosition();
		Point p = this.getViewport().getViewPosition();
		this.getColumnHeader().setView(this.createColHeaderView());
		this.getColumnHeader().setViewPosition(hp);
		this.getViewport().setViewPosition(p);
		contentView.requestFocus();
		repaint();
	}

	/**
	 * 当插入行、改变行高时，重设控件高度
	 */
	protected void resetControlHeight() {
		Point hp = this.getRowHeader().getViewPosition();
		Point p = this.getViewport().getViewPosition();
		this.getRowHeader().setView(this.createRowHeaderView());
		this.getRowHeader().setViewPosition(hp);
		this.getViewport().setViewPosition(p);
		contentView.requestFocus();
		repaint();
	}

	/**
	 * 删除列
	 * 
	 * @param col
	 *            删除位置
	 * @param count
	 *            删除的列数
	 */
	public List<NormalCell> removeColumn(int col, int count) {
		List<NormalCell> adjustCells = null;
		if (col <= cellSet.getColCount() && col > 0) {
			adjustCells = removeCol(cellSet, col, count);
			if (getActiveCell() != null) {
				int activeCellStartCol = getActiveCell().getCol();
				if (col <= activeCellStartCol) {
					activeCellStartCol -= count;
					if (activeCellStartCol < 1) {
						activeCellStartCol = 1;
					}
					getActiveCell().setCol(activeCellStartCol);
				}
			}
		}
		return adjustCells;
	}

	/**
	 * 删除列
	 * 
	 * @param cellSet
	 * @param col
	 * @param count
	 * @return
	 */
	protected List<NormalCell> removeCol(PgmCellSet cellSet, int col, int count) {
		return cellSet.removeCol(col, count);
	}

	/**
	 * 插入行
	 * 
	 * @param row
	 *            行位置
	 * @param count
	 *            插入的行数
	 */
	public List<NormalCell> insertRow(int row, int count) {
		if (row > cellSet.getRowCount() || row < 0) {
			row = cellSet.getRowCount();
		}
		List<NormalCell> cells = insertRow(cellSet, row, count);
		resetControlHeight();
		return cells;
	}

	/**
	 * 插入行
	 * 
	 * @param cellSet
	 * @param row
	 * @param count
	 */
	protected List<NormalCell> insertRow(PgmCellSet cellSet, int row, int count) {
		return cellSet.insertRow(row, count);
	}

	/**
	 * 设置显示的比例
	 * 
	 * @param ratio
	 *            百分比的整数
	 */
	public void setDisplayScale(int ratio) {
		Point p = this.getViewport().getViewPosition();
		p.x = (int) (p.x / scale);
		p.y = (int) (p.y / scale);
		this.scale = ratio / 100f;
		this.getColumnHeader().setView(this.createColHeaderView());
		this.getRowHeader().setView(this.createRowHeaderView());
		contentView = this.createContentView();
		this.getViewport().setView(contentView);
		p.x = (int) (p.x * scale);
		p.y = (int) (p.y * scale);
		this.getViewport().setViewPosition(p);
		this.getColumnHeader().setViewPosition(new Point(p.x, 0));
		this.getRowHeader().setViewPosition(new Point(0, p.y));
		repaint();
	}

	/**
	 * 取显示的比例
	 * 
	 * @return
	 */
	public float getDisplayScale() {
		return scale;
	}

	/**
	 * 追加行
	 * 
	 * @param count
	 *            追加的行数
	 */
	public void addRow(int count) {
		addRow(cellSet, count);
		resetControlHeight();
	}

	/**
	 * 追加行
	 * 
	 * @param cellSet
	 * @param count
	 */
	protected void addRow(PgmCellSet cellSet, int count) {
		cellSet.addRow(count);
	}

	/**
	 * 删除行
	 * 
	 * @param row
	 *            行位置
	 * @param count
	 *            删除的行数
	 */
	public List<NormalCell> removeRow(int row, int count) {
		List<NormalCell> adjustCells = null;
		if (row <= cellSet.getRowCount() && row > 0) {
			adjustCells = removeRow(cellSet, row, count);
			if (getActiveCell() != null) {
				int activeCellStartRow = getActiveCell().getRow();
				if (row <= activeCellStartRow) {
					activeCellStartRow -= count;
					if (activeCellStartRow < 1) {
						activeCellStartRow = 1;
					}
					getActiveCell().setRow(activeCellStartRow);
				}
			}
		}
		return adjustCells;
	}

	/**
	 * 删除行
	 * 
	 * @param cellSet
	 * @param row
	 * @param count
	 * @return
	 */
	protected List<NormalCell> removeRow(PgmCellSet cellSet, int row, int count) {
		return cellSet.removeRow(row, count);
	}

	/**
	 * 删除行列后，清除被砍掉的区域
	 */
	public void clearSelectedAreas() {
		clearSelectedArea();
		m_selectedRows.clear();
		m_selectedCols.clear();
		fireRegionSelect(false);
	}

	/**
	 * 获得控件中的输入编辑框
	 */
	public JTextComponent getEditor() {
		if (contentView == null) {
			return null;
		}
		JComponent textEditor = contentView.getEditor();
		if (textEditor == null || !(textEditor instanceof JTextComponent)) {
			return null;
		}
		return (JTextComponent) textEditor;
	}

	/**
	 * 设置搜索匹配到的格子
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @param searchInSelectedCells
	 *            是否在选择区域内搜索的
	 */
	public void setSearchedCell(int row, int col, boolean searchInSelectedCells) {
		setActiveCell(new CellLocation(row, col));
		ControlUtils.scrollToVisible(this.getViewport(), this, row, col);
		if (!searchInSelectedCells) {
			setSelectedArea(new Area(row, col, row, col));
			this.fireRegionSelect(true);
		}
		this.repaint();
	}

	/**
	 * 关闭控件
	 */
	public void dispose() {
		cellSet = null;
		m_selectedAreas = null;
		m_selectedCols = null;
		m_selectedRows = null;
		m_editorListener = null;
		if (contentView != null) {
			contentView.dispose();
		}
		cellX = null;
		cellY = null;
		cellW = null;
		cellH = null;
	}

	public void setScale(float newScale) {
		float oldScale = scale;
		// Boolean isEditable = null;
		// if (contentView != null) {
		// isEditable = contentView.isEditable();
		// }
		this.scale = newScale;

		// draw时会重新创建网格面板，改成刷新
		// draw();
		if (colHeaderView != null) {
			Point pCol = getViewport().getViewPosition();
			pCol.setLocation(pCol.getX() * scale / oldScale, pCol.getY()
					* scale / oldScale);
			this.setColumnHeaderView(colHeaderView);
			this.getColumnHeader().setViewPosition(pCol);
		}
		if (rowHeaderView != null) {
			Point pRow = getViewport().getViewPosition();
			pRow.setLocation(pRow.getX() * scale / oldScale, pRow.getY()
					* scale / oldScale);
			this.setRowHeaderView(rowHeaderView);
			this.getRowHeader().setViewPosition(pRow);
		}
		if (contentView != null) {
			Point pContent = getViewport().getViewPosition();
			pContent.setLocation(pContent.getX() * scale / oldScale,
					pContent.getY() * scale / oldScale);
			this.getViewport().setView(contentView);
			this.getViewport().setViewPosition(pContent);
		}

		// if (isEditable != null)
		// contentView.setEditable(isEditable);
		ByteMap bm = this.cellSet.getCustomPropMap();
		if (bm == null) {
			bm = new ByteMap();
			this.cellSet.setCustomPropMap(bm);
		}
		bm.put(GC.CELLSET_SCALE, scale);
	}

}
