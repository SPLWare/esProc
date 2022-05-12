package com.scudata.ide.spl.control;

import java.awt.Color;
import java.awt.Font;

import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.ColCell;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.cellset.datamodel.RowCell;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.GC;

/**
 * 网格分析器
 *
 */
public class CellSetParser {
	/**
	 * 空格子
	 */
	private static NormalCell blankCell;

	/**
	 * 网格对象
	 */
	private CellSet cellSet;

	/**
	 * 分析器构造器
	 * 
	 * @param cellSet
	 *            网格对象
	 */
	public CellSetParser(CellSet cellSet) {
		this.cellSet = cellSet;
		blankCell = new PgmNormalCell();
	}

	/**
	 * 取网格对象
	 * 
	 * @return
	 */
	public CellSet getCellSet() {
		return cellSet;
	}

	/**
	 * 取单元格对象
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public NormalCell getCell(int row, int col) {
		return getCell(row, col, false);
	}

	/**
	 * 取单元格对象
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @param create
	 *            指定单元格为null时是否创建格子
	 * @return
	 */
	public NormalCell getCell(int row, int col, boolean create) {
		NormalCell cell = (NormalCell) cellSet.getCell(row, col);
		if (cell == null) {
			if (create) {
				return new PgmNormalCell();
			} else {
				return blankCell;
			}
		}
		return cell;
	}

	/**
	 * 取单元格的间隔
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public int getCellIndent(int row, int col) {
		return ConfigOptions.iIndent.intValue();
	}

	/** 注释格或者块 */
	public static final byte TYPE_NOTE = 1;
	/** 计算和执行格或者块 */
	public static final byte TYPE_CALC = 2;
	/** 常数格 */
	public static final byte TYPE_CONST = 3;
	/** 无值常数格 */
	public static final byte TYPE_CONST_NULL = 4;
	/** 表达式 */
	public static final byte TYPE_EXP = 5;
	/** 无值表达式 */
	public static final byte TYPE_EXP_NULL = 6;

	/**
	 * 取单元格类型
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public byte getCellDispType(int row, int col) {
		NormalCell cell = getCell(row, col);
		int type = cell.getType();
		byte dispType = TYPE_EXP_NULL;
		switch (type) {
		case NormalCell.TYPE_NOTE_CELL:
		case NormalCell.TYPE_NOTE_BLOCK:
			return TYPE_NOTE;
		case NormalCell.TYPE_CALCULABLE_CELL:
		case NormalCell.TYPE_CALCULABLE_BLOCK:
		case NormalCell.TYPE_EXECUTABLE_CELL:
		case NormalCell.TYPE_EXECUTABLE_BLOCK:
			dispType = TYPE_CALC;
			break;
		}
		// 从第一列开始向上查找，找不到向右查找一直到col-1列
		int topRow = 1;
		NormalCell temp;
		for (int c = 1; c < col; c++) {
			for (int r = row; r >= topRow; r--) {
				temp = getCell(r, c);
				switch (temp.getType()) {
				case NormalCell.TYPE_NOTE_BLOCK:
					return TYPE_NOTE;
				case NormalCell.TYPE_CALCULABLE_BLOCK:
				case NormalCell.TYPE_EXECUTABLE_BLOCK:
					dispType = TYPE_CALC;
					topRow = r;
					break;
				default:
					if (StringUtils.isValidString(temp.getExpString())) {
						topRow = r;
						break;
					}
				}
			}
		}
		if (cell.getType() == NormalCell.TYPE_CONST_CELL) { // 常量格
			if (isSubCell(row, col)) {
				return TYPE_CONST_NULL;
			}
			if (dispType != TYPE_CALC) { // 计算格执行格、在计算块执行块中,按表达式格显示 并且不是续格
				return TYPE_CONST;
			}
		}
		if (cell.getValue() != null) { // 有值表达式
			return TYPE_EXP;
		} else { // 无值表达式
			return TYPE_EXP_NULL;
		}
	}

	/**
	 * 取网格所有单元格的显示类型
	 * @param cellSet
	 * @return
	 */
	public static byte[][] getCellDispTypes(PgmCellSet cellSet) {
		CellSetParser parser = new CellSetParser(cellSet);
		int rc = cellSet.getRowCount();
		int cc = cellSet.getColCount();
		byte[][] dispTypes = new byte[rc][cc];
		for (int r = 1; r <= rc; r++) {
			dispTypes[r - 1] = new byte[cc];
			for (int c = 1; c <= cc; c++) {
				dispTypes[r - 1][c - 1] = parser.getCellDispType(r, c);
			}
		}
		return dispTypes;
	}

	/**
	 * 是否子格
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	private boolean isSubCell(int row, int col) {
		int colCount = cellSet.getColCount();
		NormalCell nc;
		String expStr;
		for (int r = row; r >= 1; r--) {
			for (int c = colCount; c >= 1; c--) {
				if (r == row && c > col) {
					c = col;
					continue;
				}
				nc = getCell(r, c);
				expStr = nc.getExpString();
				if (!StringUtils.isValidString(expStr)) {
					continue;
				}
				if (!isSubString(expStr)) {
					return false;
				}
				switch (nc.getType()) {
				case NormalCell.TYPE_CALCULABLE_CELL:
				case NormalCell.TYPE_CALCULABLE_BLOCK:
				case NormalCell.TYPE_EXECUTABLE_CELL:
				case NormalCell.TYPE_EXECUTABLE_BLOCK:
					return true;
				case NormalCell.TYPE_CONST_CELL:
					continue;
				default:
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * 表达式是否子格串
	 * 
	 * @param expStr
	 * @return
	 */
	private boolean isSubString(String expStr) {
		char lastChar = expStr.charAt(expStr.length() - 1);
		if (lastChar != ',' && lastChar != ';' && lastChar != '(') {
			return false;
		}
		return true;
	}

	/**
	 * 取单元格背景色
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public Color getBackColor(int row, int col) {
		return getCellTypeColor(row, col, false);
	}

	/**
	 * 取单元格前景色
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public Color getForeColor(int row, int col) {
		return getCellTypeColor(row, col, true);
	}

	/**
	 * 取单元格类型对应的颜色
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @param isGetForeground
	 *            是否取前景色。true前景色，false背景色
	 * @return
	 */
	protected Color getCellTypeColor(int row, int col, boolean isGetForeground) {
		byte type = getCellDispType(row, col);
		if (type == TYPE_NOTE) { // 注释格或者在注释块中
			return isGetForeground ? ConfigOptions.iNoteFColor
					: ConfigOptions.iNoteBColor;
		}
		if (type == TYPE_CONST_NULL) {
			return isGetForeground ? ConfigOptions.iNValueFColor
					: ConfigOptions.iNValueBColor;
		}
		if (type == TYPE_CONST) { // 计算格执行格、在计算块执行块中,按表达式格显示 并且不是续格
			return isGetForeground ? ConfigOptions.iConstFColor
					: ConfigOptions.iConstBColor;
		}
		if (type == TYPE_EXP) { // 有值表达式
			return isGetForeground ? ConfigOptions.iValueFColor
					: ConfigOptions.iValueBColor;
		} else { // 无值表达式
			return isGetForeground ? ConfigOptions.iNValueFColor
					: ConfigOptions.iNValueBColor;
		}
	}

	/**
	 * 取单元格显示文本
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public String getDispText(int row, int col) {
		NormalCell nc = getCell(row, col);
		String text;
		text = nc.getExpString();
		return text;
	}

	/**
	 * 取单元格字体
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public Font getFont(int row, int col) {
		return GC.font;
	}

	/**
	 * 取单元格字体名称
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public String getFontName(int row, int col) {
		return "Dialog";
	}

	/**
	 * 取单元格字体大小
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public int getFontSize(int row, int col) {
		return ConfigOptions.iFontSize.intValue();
	}

	/**
	 * 取单元格显示格式
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public String getFormat(int row, int col) {
		return null;
	}

	/**
	 * 取单元格水平对齐
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public byte getHAlign(int row, int col) {
		return ConfigOptions.iHAlign.byteValue();
	}

	/**
	 * 取垂直对齐
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public byte getVAlign(int row, int col) {
		return ConfigOptions.iVAlign.byteValue();
	}

	/**
	 * 单元格是否加粗
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public boolean isBold(int row, int col) {
		return ConfigOptions.bBold.booleanValue();
	}

	/**
	 * 单元格是否斜体
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public boolean isItalic(int row, int col) {
		return ConfigOptions.bItalic.booleanValue();
	}

	/**
	 * 单元格是否下划线
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public boolean isUnderline(int row, int col) {
		return ConfigOptions.bUnderline.booleanValue();
	}

	/**
	 * 取行数
	 * 
	 * @return
	 */
	public int getRowCount() {
		return cellSet.getRowCount();
	}

	/**
	 * 取行高
	 * 
	 * @param row
	 *            行号
	 * @param scale
	 *            显示比例
	 * @return
	 */
	public int getRowHeight(int row, float scale) {
		RowCell cell = (RowCell) cellSet.getRowCell(row);
		float height = cell.getHeight();
		int h = (int) Math.ceil(height * scale);
		if (scale != 1.0) {
			h += 1;
		}
		return h;
	}

	/**
	 * 获得总列数
	 * 
	 * @return 总列数
	 */
	public int getColCount() {
		return cellSet.getColCount();
	}

	/**
	 * 取列宽
	 * 
	 * @param col
	 *            列号
	 * @param scale
	 *            显示比例
	 * @return
	 */
	public int getColWidth(int col, float scale) {
		ColCell cell = (ColCell) cellSet.getColCell(col);
		float width = cell.getWidth();
		int w = (int) Math.ceil(width * scale);
		if (scale != 1.0) {
			w += 1;
		}
		return w;
	}

	/**
	 * 取列宽，不可视列返回0
	 * 
	 * @param col
	 *            列号
	 * @return
	 */
	public int getColWidth(int col) {
		if (!isColVisible(col)) {
			return 0;
		}
		ColCell cell = (ColCell) cellSet.getColCell(col);
		float width = cell.getWidth();
		int scale = 1;
		int w = (int) Math.ceil(width * scale);
		if (scale != 1.0) {
			w += 1;
		}
		return w;
	}

	/**
	 * 行是否可视
	 * 
	 * @param row
	 *            行号
	 * @return
	 */
	public boolean isRowVisible(int row) {
		RowCell rc = (RowCell) cellSet.getRowCell(row);
		return rc.getVisible() != RowCell.VISIBLE_ALWAYSNOT;
	}

	/**
	 * 列是否可视
	 * 
	 * @param col
	 *            列号
	 * @return
	 */
	public boolean isColVisible(int col) {
		ColCell cc = (ColCell) cellSet.getColCell(col);
		return cc.getVisible() != ColCell.VISIBLE_ALWAYSNOT;
	}

	/**
	 * 取行高，不可视行返回0
	 * 
	 * @param row
	 * @return
	 */
	public int getRowHeight(int row) {
		if (isRowVisible(row)) {
			return (int) cellSet.getRowCell(row).getHeight();
		} else {
			return 0;
		}
	}

	/**
	 * 取多列宽度
	 * 
	 * @param control
	 *            网格控件
	 * @param startCol
	 *            开始列
	 * @param count
	 *            列数
	 * @param includeHideCol
	 *            是否包含隐藏列宽
	 * @return
	 */
	public int getColsWidth(SplControl control, int startCol, int count,
			boolean includeHideCol) {
		int width = 0;
		for (int i = 0, col = startCol; i < count; i++, col++) {
			if (includeHideCol) {
				width += getColWidth(col, control.scale);
			} else {
				width += getColWidth(col);
			}
		}
		return width;
	}

	/**
	 * 取多行高度
	 * 
	 * @param control
	 *            网格控件
	 * @param startRow
	 *            开始行
	 * @param count
	 *            行数
	 * @param includeHideRow
	 *            是否包含隐藏行高
	 * @return
	 */
	public int getRowsHeight(SplControl control, int startRow, int count,
			boolean includeHideRow) {
		int height = 0;
		for (int i = 0, row = startRow; i < count; i++, row++) {
			if (includeHideRow) {
				height += getRowHeight(row, control.scale);
			} else {
				height += getRowHeight(row);
			}
		}
		return height;
	}

	/**
	 * 取子行的结束行
	 * 
	 * @param row
	 * @return
	 */
	public int getSubEnd(int row) {
		PgmNormalCell cell;
		int subEnd = -1;
		for (int c = 1, cc = cellSet.getColCount(); c <= cc; c++) {
			cell = (PgmNormalCell) cellSet.getCell(row, c);
			switch (cell.getType()) {
			case PgmNormalCell.TYPE_CALCULABLE_BLOCK:
			case PgmNormalCell.TYPE_EXECUTABLE_BLOCK:
			case PgmNormalCell.TYPE_NOTE_BLOCK:
			case PgmNormalCell.TYPE_COMMAND_CELL:
				subEnd = ((PgmCellSet) cellSet).getCodeBlockEndRow(row, c);
				if (subEnd > row) {
					return subEnd;
				}
				break;
			}
		}
		return subEnd;
	}

	/**
	 * 子行是否可视
	 * 
	 * @param row
	 *            行号
	 * @param subEnd
	 *            子行末行
	 * @return
	 */
	public boolean isSubExpand(int row, int subEnd) {
		if (subEnd <= row)
			return false;
		RowCell rc = (RowCell) cellSet.getRowCell(row + 1);
		return rc.getVisible() != RowCell.VISIBLE_ALWAYSNOT;
	}
}
