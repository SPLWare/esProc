package com.raqsoft.common;

import java.io.*;

/**
 * 本类对应单元格的标示
 */
public class CellLocation implements java.io.Serializable, IRecord {
	private static final long serialVersionUID = 0x00010001;

	private static final int MAX_COLCHAR_COUNT = 6;

	private int row = -1;
	private int col = -1;

	// 存盘时使用
	public CellLocation() {
	}

	public CellLocation(String cellId) {
	  CellLocation cl = parse( cellId );
	  if( cl != null ){
	    setRow(cl.getRow());
	    setCol(cl.getCol());
	  }
	}

	public CellLocation(CellLocation lct) {
		row = lct.row;
		col = lct.col;
	}

	public CellLocation(int r, int c) {
		row = r;
		col = c;
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public void setCol(int col) {
		this.col = col;
	}

	public void set(int row, int col) {
		this.row = row;
		this.col = col;
	}

	public int hashCode() {
		return (row << 8) + col;
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof CellLocation) {
			if ( ( (CellLocation) o).row == row && ( (CellLocation) o).col == col) {
				return true;
			}
		}
		return false;
	}

	public static String toRow(int row) {
		char[] ret = new char[12];
		int j = 12;

		if (row < 0) {
			return null;
		}
		else if (row == 0) {
			ret[--j] = '0';
		}
		else {
			while (row > 0) {
				ret[--j] = (char) ('0' + row % 10);
				row /= 10;
			}
		}
		return new String(ret, j, 12 - j);
	}

	public static String toCol(int col) {
		char[] ret = new char[MAX_COLCHAR_COUNT];
		int i = MAX_COLCHAR_COUNT;
		if (col < 0) {
			return null;
		}
		else if (col == 0) {
			ret[--i] = '`';
		}
		else {
			while (col > 0) {
				ret[--i] = (char) ('A' + (col - 1) % 26);
				col = (col - 1) / 26;
			}
		}

		return new String(ret, i, MAX_COLCHAR_COUNT - i);
	}

	/**
	 * 将字符串转成列号
	 * @param id String
	 * @return int
	 */
	public static int parseCol(String id) {
		if (id == null) return -1;
		int len = id.length();
		if (len == 0) return -1;

		int col = 0;
		for (int i = 0; i < len; i++) {
			char c = id.charAt(i);
			if (c < 'A' || c > 'Z') {
				return -1;
			}
			col = col * 26 + (c - 'A') + 1;
		}
		return col;
	}

	/**
	 * 将字符串转成行号
	 * @param id String
	 * @return int
	 */
	public static int parseRow(String id) {
		if (id == null) return -1;
		int len = id.length();
		if (len == 0) return -1;

		int row = 0;
		for (int i = 0; i < len; i++) {
			char c = id.charAt(i);
			if (c < '0' || c > '9') {
				return -1;
			}
			row = row * 10 + (c - '0');
		}
		return row;
	}

	//单元格的标示转换为行号列号
	public static CellLocation parse(String id) {
		if (id == null) {
			return null;
		}
		int length = id.length();
		if (length < 2) {
			return null;
		}

		long rowSize = 0x7fffffffl;
		int colSize = 0x7fff;

		int i = 0;
		long row = 0;
		int col = 0;
		char c = 0;
		boolean flag = false;

		for (i = 0; i < length; i++) {
			c = id.charAt(i);
			if (c < 'A' || c > 'Z') {
				break;
			}
			col = col * 26 + (c - 'A') + 1;
			if (col > colSize) {
				return null;
			}
			flag = true;
		}
		if (c == '`' && i == 0) {
			flag = true;
			i++;
		}
		if (!flag) {
			return null;
		}
		flag = false;
		for (; i < length; i++) {
			c = id.charAt(i);
			if (c < '0' || c > '9') {
				return null;
			}
			row = row * 10 + (c - '0');
			if (row > rowSize) {
				return null;
			}
			flag = true;
		}
		if (!flag) {
			return null;
		}
		return new CellLocation( (int) row, col);
	}

	public String toString() {
		return getCellId(row, col);
	}

	/**
	 * 根据行号列号转换为单元格的标示
	 * @param row int
	 * @param col int
	 * @return String
	 */
	public static String getCellId(int row, int col) {
		char[] ret = new char[16];
		int i = 8, j = 16;
		if (col < 0) {
			return null;
		}
		else if (col == 0) {
			ret[--i] = '`';
		}
		else {
			while (col > 0) {
				ret[--i] = (char) ('A' + (col - 1) % 26);
				col = (col - 1) / 26;
			}
		}
		if (row < 0) {
			return null;
		}
		else if (row == 0) {
			ret[--j] = '0';
		}
		else {
			while (row > 0) {
				ret[--j] = (char) ('0' + row % 10);
				row /= 10;
			}
		}
		System.arraycopy(ret, j, ret, 8, 16 - j);
		return new String(ret, i, 8 - i + 16 - j);
	}

	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeInt(row);
		out.writeInt(col);
		return out.toByteArray();
	}

	public void fillRecord(byte[] buf) throws IOException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		row = in.readInt();
		col = in.readInt();
	}
}
