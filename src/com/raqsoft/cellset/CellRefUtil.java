package com.raqsoft.cellset;

import com.raqsoft.common.CellLocation;
import com.raqsoft.common.Sentence;
import com.raqsoft.dm.KeyWord;

public final class CellRefUtil {
	// 错误的单元格引用标识
	public static final String ERRORREF = "#REF!";

	/**
	 * 判断字符是否是行符号
	 * @param c 字符
	 * @return true：是，false：不是
	 */
	public static boolean isRowChar(char c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * 判断字符是否是列符号
	 * @param c 字符
	 * @return true：是，false：不是
	 */
	public static boolean isColChar(char c) {
		return c >= 'A' && c <= 'Z';
	}

	/**
	 * 判断字符串指定位置的前一个字符是否是'.'
	 * @param str 表达式字符串
	 * @param pos 位置
	 * @return true：是，false：不是
	 */
	public static boolean isPrevDot(String str, int pos) {
		for (pos = pos - 1; pos >= 0; --pos) {
			char c = str.charAt(pos);
			if (c == '.') return true;
			if (!Character.isWhitespace(c)) return false;
		}

		return false;
	}

	/**
	 * 改变所引用的单元格的行号
	 * @param cellRow 源行号
	 * @param rowBase 在此行进行增加或者删除行
	 * @param rowIncrement 增加或者删除的行数
	 * @param oldRowCount 原来的总行数
	 * @return 变换后的行名，或者null表示引用格被删除
	 */
	public static String changeRow(int cellRow, int rowBase, int rowIncrement, int oldRowCount) {
		if (rowBase != -1) {
			if (rowIncrement < 0) {
				//引用行被删除，冻结表达式
				if (cellRow >= rowBase && cellRow <= (rowBase - rowIncrement - 1)) {
					return null;
				}
			}

			if (cellRow >= rowBase) {
				cellRow += rowIncrement;
			}
		}
		return CellLocation.toRow(cellRow);
	}

	/**
	 * 改变所引用的单元格的列号
	 * @param cellCol 源列号
	 * @param colBase 在此列进行增加或者删除列
	 * @param colIncrement 增加或者删除的列数
	 * @param oldColCount 原来的总列数
	 * @return 变换后的列名，或者null表示引用格被删除
	 */
	public static String changeCol(int cellCol, int colBase, int colIncrement, int oldColCount) {
		if (colBase != -1) {
			if (colIncrement < 0) {
				//引用列被删除，冻结表达式
				if (cellCol >= colBase && cellCol <= (colBase - colIncrement - 1)) {
					return null;
				}
			}

			if (cellCol >= colBase) {
				cellCol += colIncrement;
			}
		}
		return CellLocation.toCol(cellCol);
	}

	/**
	 * 改变所引用的单元格的行号，用于从一个格复制表达式到另一个格，表达式里引用的格要加上源格到目标格的相对位移
	 * @param cellRow 源行号
	 * @param rowIncrement 增加的行数，负的表示删除的行数
	 * @param rowCount 总行数
	 * @return 变换后的行名，或者null表示格出界
	 */
	public static String changeRow(int cellRow, int rowIncrement, int rowCount) {
		cellRow += rowIncrement;
		if (cellRow <= 0 || cellRow > rowCount) return null;

		return CellLocation.toRow(cellRow);
	}

	/**
	 * 改变所引用的单元格的列号，用于从一个格复制表达式到另一个格，表达式里引用的格要加上源格到目标格的相对位移
	 * @param cellCol 源列号
	 * @param colIncrement 增加的列数，负的表示删除的列数
	 * @param colCount 总列数
	 * @return 变换后的列名，或者null表示格出界
	 */
	public static String changeCol(int cellCol, int colIncrement, int colCount) {
		if (cellCol <= 0 || cellCol > colCount) return null;

		cellCol += colIncrement;
		if (cellCol <= 0 || cellCol > colCount) return null;

		return CellLocation.toCol(cellCol);
	}

	/**
	 * 插入、删除行列时对表达式中的单元格引用进行变迁
	 * @param str 表达式
	 * @param rowBase 操作所在的行
	 * @param rowIncrement 插入或者删除的行数
	 * @param colBase 操作所在的列
	 * @param colIncrement 插入或者删除的列数
	 * @param oldRowCount 原来的总行数
	 * @param oldColCount 原来的总列数
	 * @param error 如果引用的单元格被删除则置error[0]为true
	 * @return 变迁后的表达式
	 */
	public static String relativeRegulateString(String str, int rowBase, int rowIncrement,
										  int colBase, int colIncrement,
										  int oldRowCount, int oldColCount, boolean []error) {
		error[0] = false;
		if (str == null || str.length() == 0 || str.startsWith(ERRORREF)) {
			//冻结或错误的单元格不处理
			return str;
		}

		StringBuffer strNew = null;
		int len = str.length();
		for (int idx = 0; idx < len; ) {
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') { // 跳过字符串
				int tmp = Sentence.scanQuotation(str, idx);
				if (tmp < 0) {
					if (strNew != null) strNew.append(str.substring(idx));
					break;
				} else {
					tmp++;
					if (strNew != null) strNew.append(str.substring(idx, tmp));
					idx = tmp;
				}
			} else if (KeyWord.isSymbol(ch) || ch == KeyWord.CELLPREFIX) {
				if (strNew != null) strNew.append(ch);
				idx++;
			} else {
				int last = KeyWord.scanId(str, idx + 1);
				if (last - idx < 2 || (!isColChar(ch) && ch != '$') || isPrevDot(str, idx)) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				int macroIndex = -1; // A$23中$的索引
				int numIndex = -1; // 数字的索引
				
				for (int i = idx + 1; i < last; ++i) {
					char tmp = str.charAt(i);
					if (tmp == '$') {
						macroIndex = i;
						numIndex = i + 1;
						break;
					} else if (isRowChar(tmp)) {
						numIndex = i;
						break;
					} else if (!isColChar(tmp)) {
						break;
					}
				}

				if (numIndex == -1) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				if (ch == '$') {
					if (macroIndex == -1) { // $A2
						CellLocation lct = CellLocation.parse(str.substring(idx + 1, last));
						if (lct == null || lct.getRow() > oldRowCount || lct.getCol() > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strCol = changeCol(lct.getCol(), colBase,
								colIncrement, oldColCount);
							if (strCol == null) {
								error[0] = true;
								return ERRORREF + str;
							}

							String strRow = changeRow(lct.getRow(), rowBase,
								rowIncrement, oldRowCount);
							if (strRow == null) {
								error[0] = true;
								return ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(strCol);
							strNew.append(strRow);
						}
					} else { // $A$2
						int col = CellLocation.parseCol(str.substring(idx + 1, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));
						if (col == -1 || row == -1 || row > oldRowCount || col > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strCol = changeCol(col, colBase, colIncrement, oldColCount);
							if (strCol == null) {
								error[0] = true;
								return ERRORREF + str;
							}

							String strRow = changeRow(row, rowBase, rowIncrement, oldRowCount);
							if (strRow == null) {
								error[0] = true;
								return ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(strCol);
							strNew.append('$');
							strNew.append(strRow);
						}
					}
				} else {
					if (macroIndex == -1) { // A2
						CellLocation lct = CellLocation.parse(str.substring(idx, last));
						if (lct == null || lct.getRow() > oldRowCount || lct.getCol() > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strCol = changeCol(lct.getCol(), colBase,
								colIncrement, oldColCount);
							if (strCol == null) {
								error[0] = true;
								return ERRORREF + str;
							}

							String strRow = changeRow(lct.getRow(), rowBase,
								rowIncrement, oldRowCount);
							if (strRow == null) {
								error[0] = true;
								return ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(strCol);
							strNew.append(strRow);
						}
					} else { // A$2  A$2@cs
						int col = CellLocation.parseCol(str.substring(idx, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));
						if (col == -1 || row == -1 || row > oldRowCount || col > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strCol = changeCol(col, colBase, colIncrement, oldColCount);
							if (strCol == null) {
								error[0] = true;
								return ERRORREF + str;
							}

							String strRow = changeRow(row, rowBase, rowIncrement, oldRowCount);
							if (strRow == null) {
								error[0] = true;
								return ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(strCol);
							strNew.append('$');
							strNew.append(strRow);
						}
					}
				}

				idx = last;
			}
		}

		return strNew == null ? str : strNew.toString();
	}

	/**
	 * 增删行时调整单元格引用
	 * @param r 单元格的行号
	 * @param rows 插入或删除的行号
	 * @param isInsert true：插入，false：删除
	 * @return 调整后的行号，或者-1表示引用的行被删除
	 */
	public static int adjustRowReference(int r, int []rows, boolean isInsert) {
		int count = rows.length;
		for (int i = 0; i < count; ++i) {
			if (rows[i] > r) {
				return isInsert ? r + i : r - i;
			} else if (rows[i] == r) {
				return isInsert ? r + i + 1 : -1;
			}
		}

		return isInsert ? r + count : r - count;
	}

	/**
	 * 把表达式中对lct1的引用改为lct2，用于把一个格剪切到另一个格，改变对源格的引用到目标格
	 * @param str 表达式
	 * @param lct1 原来引用的单元格
	 * @param lct2 目标单元格
	 * @return 变换后的表达式
	 */
	public static String exchangeCellString(String str, CellLocation lct1, CellLocation lct2) {
		//冻结或错误的单元格不处理
		if (str == null || str.length() == 0 || str.startsWith(ERRORREF)) {
			return str;
		}

		StringBuffer strNew = null;
		int len = str.length();
		for (int idx = 0; idx < len; ) {
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') { // 跳过字符串
				int tmp = Sentence.scanQuotation(str, idx);
				if (tmp < 0) {
					if (strNew != null) strNew.append(str.substring(idx));
					break;
				} else {
					tmp++;
					if (strNew != null) strNew.append(str.substring(idx, tmp));
					idx = tmp;
				}
			} else if (KeyWord.isSymbol(ch) || ch == KeyWord.CELLPREFIX) {
				if (strNew != null) strNew.append(ch);
				idx++;
			} else {
				int last = KeyWord.scanId(str, idx + 1);
				if (last - idx < 2 || (!isColChar(ch) && ch != '$') || isPrevDot(str, idx)) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				int macroIndex = -1; // A$23中$的索引
				int numIndex = -1; // 数字的索引

				for (int i = idx + 1; i < last; ++i) {
					char tmp = str.charAt(i);
					if (tmp == '$') {
						macroIndex = i;
						numIndex = i + 1;
						break;
					} else if (isRowChar(tmp)) {
						numIndex = i;
						break;
					} else if (!isColChar(tmp)) {
						break;
					}
				}

				if (numIndex == -1) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				if (ch == '$') {
					if (macroIndex == -1) { // $A2
						CellLocation lct = CellLocation.parse(str.substring(idx + 1, last));

						if (lct1.equals(lct)) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(lct2.toString());
						} else if (lct2.equals(lct)) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(lct1.toString());
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					} else { // $A$2
						int col = CellLocation.parseCol(str.substring(idx + 1, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));

						if (col == lct1.getCol() && row == lct1.getRow()) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(CellLocation.toCol(lct2.getCol()));
							strNew.append('$');
							strNew.append(CellLocation.toRow(lct2.getRow()));
						} else if (col == lct2.getCol() && row == lct2.getRow()) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(CellLocation.toCol(lct1.getCol()));
							strNew.append('$');
							strNew.append(CellLocation.toRow(lct1.getRow()));
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					}
				} else {
					if (macroIndex == -1) { // A2
						CellLocation lct = CellLocation.parse(str.substring(idx, last));
						if (lct1.equals(lct)) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(lct2.toString());
						} else if (lct2.equals(lct)) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(lct1.toString());
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					} else { // A$2
						int col = CellLocation.parseCol(str.substring(idx, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));
						if (col == lct1.getCol() && row == lct1.getRow()) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(CellLocation.toCol(lct2.getCol()));
							strNew.append('$');
							strNew.append(CellLocation.toRow(lct2.getRow()));
						} else if (col == lct2.getCol() && row == lct2.getRow()) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(CellLocation.toCol(lct1.getCol()));
							strNew.append('$');
							strNew.append(CellLocation.toRow(lct1.getRow()));
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					}
				}

				idx = last;
			}
		}

		return strNew == null ? str : strNew.toString();
	}
}
