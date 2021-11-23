package com.scudata.common;

import java.io.*;

/**
 * 矩阵
 */
public class Matrix
	implements Externalizable {

	  private final static long serialVersionUID =-3689348814741910324L;

	private Object[] data;
	private int rowSize, colSize;

	public Matrix(Object []rows) {
		this.data = rows;
		this.rowSize = rows.length;
		this.colSize = ((Object[])rows[0]).length;
	}

	public Matrix(int rowSize, int colSize) {
		if (rowSize <= 0 || colSize <= 0) {
			throw new RuntimeException();
		}
		this.rowSize = rowSize;
		this.colSize = colSize;
		Object[] tmp = new Object[rowSize];
		this.data = tmp;
		for (int i = rowSize-1; i >=0 ; i--) {
			tmp[i] = new Object[colSize];
		}
	}

	public Matrix() {
		this(20, 10);
	}

	public void setRowSize(int size) {
		if (size <= 0) {
			throw new RuntimeException();
		}
		Object[] data = this.data;
		int rows = this.rowSize;
		if (rows > size) {
			Object[] tmp = new Object[size];
			System.arraycopy(data, 0, tmp, 0, size);
			this.data = tmp;
		}
		else if (rows < size) {
			Object[] tmp = new Object[size];
			int cols = colSize;
			System.arraycopy(data, 0, tmp, 0, rows);
			for (int i = rows; i < size; i++) {
				tmp[i] = new Object[cols];
			}
			this.data = tmp;
		}
		this.rowSize = size;
	}

	public void setColSize(int size) {
		if (size <= 0) {
			throw new RuntimeException();
		}
		Object[] data = this.data;
		int cols = colSize;
		if (cols != size) {
			int min = (cols > size) ? size : cols;
			int rows = this.rowSize;
			for (int i = rows-1; i >=0 ; i--) {
				Object[] tmp = new Object[size];
				System.arraycopy(data[i], 0, tmp, 0, min);
				data[i] = tmp;
			}
		}
		this.colSize = size;
	}

	public void setSize(int rowSize, int colSize) {
		this.setRowSize(rowSize);
		this.setColSize(colSize);
	}

	public int getRowSize() {
		return this.rowSize;
	}

	public int getColSize() {
		return this.colSize;
	}

	public void set(int row, int col, Object value) {
		Object[] tmp = (Object[]) data[row];
		tmp[col] = value;
	}

	public Object get(int row, int col) {
		Object[] tmp = (Object[]) data[row];
		return tmp[col];
	}

	public Object[] getRow(int row) {
		return (Object[]) data[row];
	}

	public void setRow(int row, Object []rowObjs) {
		data[row] = rowObjs;
	}

	public void insertRow(int index) {
		Object[] data = this.data;
		int rows = rowSize;
		int cols = colSize;
		Object[] tmp = new Object[rows + 1];
		System.arraycopy(data, 0, tmp, 0, index);
		int moved = rows - index;
		if (moved > 0) {
			System.arraycopy(data, index, tmp, index + 1, moved);
		}
		tmp[index] = new Object[cols];
		this.data = tmp;
		this.rowSize++;
	}

	public void insertCol(int index) {
		Object[] data = this.data;
		int rows = rowSize;
		int cols = colSize;
		int moved = cols - index;
		for (int i = rows-1; i >=0 ; i--) {
			Object[] tmp = new Object[cols + 1];
			System.arraycopy(data[i], 0, tmp, 0, index);
			if (moved > 0) {
				System.arraycopy(data[i], index, tmp, index + 1, moved);
			}
			data[i] = tmp;
		}
		this.colSize++;
	}

	public void addRow() {
		int rows = this.rowSize;
		Object[] tmp = new Object[rows + 1];
		System.arraycopy(this.data, 0, tmp, 0, rows);
		tmp[rows] = new Object[this.colSize];
		this.data = tmp;
		this.rowSize++;
	}

	public void addCol() {
		Object[] data = this.data;
		int cols = this.colSize;
		for (int i = this.rowSize-1; i >=0; i--) {
			Object[] tmp = new Object[cols + 1];
			System.arraycopy((Object[])data[i], 0, tmp, 0, cols);
			data[i] = tmp;
		}
		this.colSize++;

	}

	public void addRows(int count) {
		if (count < 1) {
			return;
		}

		Object[] data = this.data;
		int rows = this.rowSize;
		int cols = this.colSize;
		Object[] tmp = new Object[rows + count];
		System.arraycopy(data, 0, tmp, 0, rows);

		for (int i = rows+ count-1; i >= rows ; i--) {
			tmp[i] = new Object[cols];
		}
		this.data = tmp;
		this.rowSize += count;
	}

	public void addCols(int count) {
		if (count < 1) {
			return;
		}
		Object[] data = this.data;
		int cols = this.colSize;

		for (int i = this.rowSize-1; i >=0 ; i--) {
			Object[] tmp = new Object[cols + count];
			System.arraycopy((Object[])data[i], 0, tmp, 0, cols);
			data[i] = tmp;
		}
		this.colSize += count;
	}


	public void insertRows(int index, int count) {
		if (count < 1) {
			return;
		}

		Object[] data = this.data;
		int rows = this.rowSize;
		int cols = this.colSize;
		Object[] tmp = new Object[rows + count];
		System.arraycopy(data, 0, tmp, 0, index);
		int moved = rows - index;
		if (moved > 0) {
			System.arraycopy(data, index, tmp, index + count, moved);
		}
		for (int i = index+ count-1; i >= index ; i--) {
			tmp[i] = new Object[cols];
		}
		this.data = tmp;
		this.rowSize += count;
	}

	public void insertCols(int index, int count) {
		if (count < 1) {
			return;
		}

		Object[] data = this.data;
		int rows = this.rowSize;
		int cols = this.colSize;
		int moved = cols - index;
		for (int i = rows-1; i >=0 ; i--) {
			Object[] tmp = new Object[cols + count];
			System.arraycopy(data[i], 0, tmp, 0, index);
			if (moved > 0) {
				System.arraycopy(data[i], index, tmp, index + count, moved);
			}
			data[i] = tmp;
		}
		this.colSize += count;
	}

	public void insertCols(int index, int count, int beginRow, int endRow) {
		insertCols(index, count, beginRow, endRow, true);
	}

	public void insertCols(int index, int count, int beginRow, int endRow, boolean alignColHead) {
		if (count < 1) {
			return;
		}

		Object[] data = this.data;
		int rows = this.rowSize;
		int cols = this.colSize;
		int moved = cols - index;
		for (int i = rows-1; i >=0 ; i--) {
			Object[] tmp = new Object[cols + count];
			if (i >= beginRow && i <= endRow) {
				System.arraycopy(data[i], 0, tmp, 0, index);
				if (moved > 0) {
					System.arraycopy(data[i], index, tmp, index + count, moved);
				}
			}
			else if (i == 0) {
				if (alignColHead) {
					System.arraycopy(data[0], 0, tmp, 0, index);
					if (moved > 0) {
						System.arraycopy(data[i], index, tmp, index + count, moved);
					}
				}
				else {
					System.arraycopy(data[0], 0, tmp, 0, cols);
				}
			}
			else {
				System.arraycopy(data[i], 0, tmp, 0, cols);
			}
			data[i] = tmp;
		}
		this.colSize += count;
	}

	public void deleteRow(int index) {
		Object[] data = this.data;
		int rows = this.rowSize;
		Object[] tmp = new Object[rows - 1];
		System.arraycopy(data, 0, tmp, 0, index);
		int moved = rows - index - 1;
		if (moved > 0) {
			System.arraycopy(data, index + 1, tmp, index, moved);
		}
		this.data = tmp;
		this.rowSize--;
	}

	public void deleteCol(int index) {
		Object[] data = this.data;
		int rows = this.rowSize;
		int cols = this.colSize;
		int moved = cols - index - 1;
		for (int i = rows-1; i >=0 ; i--) {
			Object[] tmp = new Object[cols - 1];
			System.arraycopy(data[i], 0, tmp, 0, index);
			if (moved > 0) {
				System.arraycopy(data[i], index + 1, tmp, index, moved);
			}
			data[i] = tmp;
		}
		this.colSize--;
	}

	public void deleteRows(int index, int count) {
		Object[] data = this.data;
		int rows = data.length;
		Object[] tmp = new Object[rows - count];
		System.arraycopy(data, 0, tmp, 0, index);
		int moved = rows - index - count;
		if (moved > 0) {
			System.arraycopy(data, index + count, tmp, index, moved);
		}
		this.data = tmp;
		this.rowSize -= count;
	}

	public void deleteCols(int index, int count) {
		Object[] data = this.data;
		int rows = data.length;
		int cols = ( (Object[]) data[0]).length;
		int moved = cols - index - count;
		for (int i = rows-1; i >=0 ; i--) {
			Object[] tmp = new Object[cols - count];
			System.arraycopy(data[i], 0, tmp, 0, index);
			if (moved > 0) {
				System.arraycopy(data[i], index + count, tmp, index, moved);
			}
			data[i] = tmp;
		}
		this.colSize -= count;
	}

	/*************************以下继承自Externalizable************************/
	/**
	 * 写内容到流
	 *@param out 输出流
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte( (byte)1);
		out.writeInt(rowSize);
		out.writeInt(colSize);
		Object[] dt = data;
		for (int i = 0; i < rowSize; i++) {
			Object[] row = (Object[]) dt[i];
			for (int j = 0; j < colSize; j++) {
				out.writeObject(row[j]);
			}
		}

	}

	/**
	 * 从流中读内容
	 *@param in 输入流
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte version = in.readByte();
		rowSize = in.readInt();
		colSize = in.readInt();
		Object[] row = new Object[rowSize];
		for (int i = 0; i < rowSize; i++) {
			Object[] col = new Object[colSize];
			for (int j = 0; j < colSize; j++) {
				col[j] = in.readObject();
			}
			row[i] = col;
		}
		this.data = row;
	}

	/**
	 * 调整行的位置
	 * @param rowIndex int[] 调整后的行在源矩阵中的行号
	 */
	public void changeRowOrder(int []rowSeqs) {
		Object []tmp = new Object[rowSize];
		Object []old = this.data;
		for (int i = rowSize - 1; i >= 0; --i) {
			tmp[i] = old[rowSeqs[i]];
		}

		this.data = tmp;
	}

	/**
	 * 删除多行，行号从小到大有序
	 * @param rowSeqs int[] 行号数组，从小到大有序
	 */
	public void deleteRows(int []rowSeqs) {
		if (rowSeqs == null || rowSeqs.length == 0) return;

		int delCount = rowSeqs.length;
		int newSize = this.rowSize - delCount;
		Object[] newData = new Object[newSize];
		Object[] data = this.data;

		int copyCount = 0;
		int prev = 0;
		for (int i = 0; i < delCount; ++i) {
			int curCount = rowSeqs[i] - prev;
			System.arraycopy(data, prev, newData, copyCount, curCount);

			prev = rowSeqs[i] + 1;
			copyCount += curCount;
		}

		// 复制剩下的
		System.arraycopy(data, prev, newData, copyCount, this.rowSize - prev);

		this.data = newData;
		this.rowSize = newSize;
	}

	/**
	 * 添加多行，行号从小到大有序
	 * @param rowSeqs int[] 行号数组，从小到大有序
	 */
	public void insertRows(int []rowSeqs) {
		if (rowSeqs == null || rowSeqs.length == 0) return;

		int addCount = rowSeqs.length;
		int newSize = this.rowSize + addCount;
		Object[] newData = new Object[newSize];
		Object[] data = this.data;

		int colSize = this.colSize;
		int copyCount = 0;
		int prev = 0;
		for (int i = 0; i < addCount; ++i) {
			int curCount = rowSeqs[i] - prev;
			System.arraycopy(data, prev, newData, copyCount, curCount);

			prev = rowSeqs[i];
			copyCount += curCount;

			newData[copyCount] = new Object[colSize];
			copyCount++;
		}

		// 复制剩下的
		System.arraycopy(data, prev, newData, copyCount, this.rowSize - prev);

		this.data = newData;
		this.rowSize = newSize;
	}

	/**
	 * 交换两个区域的位置，[startRow1, endRow1]在[startRow2, endRow2]的前面
	 * @param startRow1 int 区域1的起始行
	 * @param endRow1 int 区域1的结束行
	 * @param startRow2 int 区域2的起始行
	 * @param endRow2 int 区域2的结束行
	 */
	public void exchangeArea(int startRow1, int endRow1, int startRow2, int endRow2) {
		int totalLen = endRow2 - startRow1 + 1;
		Object []tmps = new Object[totalLen];
		int len2 = endRow2 - startRow2 + 1;
		System.arraycopy(data, startRow2, tmps, 0, len2);

		int midLen = startRow2 - endRow1 - 1;
		if (midLen > 0) System.arraycopy(data, endRow1 + 1, tmps, len2, midLen);

		System.arraycopy(data, startRow1, tmps, len2 + midLen, endRow1 - startRow1 + 1);

		System.arraycopy(tmps, 0, data, startRow1, totalLen);
	}

	/**
	 * 移动某一区域到指定位置
	 * @param start int 起始行
	 * @param end int 结束行
	 * @param targetRow int 目标位置
	 */
	public void moveRows(int start, int end, int targetRow) {
		int count = end - start + 1;
		Object [][]rows = new Object[count][];
		System.arraycopy(data, start, rows, 0, count);

		if (start > targetRow) { // 上移
			System.arraycopy(data, targetRow, data, targetRow + count, start - targetRow);
			System.arraycopy(rows, 0, data, targetRow, count);
		} else { // 下移
			System.arraycopy(data, end + 1, data, start, targetRow - end - 1);
			System.arraycopy(rows, 0, data, targetRow - count, count);
		}
	}

	/**
	 * 替换指定区段的行
	 * @param start int 起始行
	 * @param end int 结束行
	 * @param rows Object[][] 要添加的行数组
	 */
	public void replace(int start, int end, Object [][]rows) {
		if (rows == null || rows.length == 0) {
			deleteRows(start, end - start + 1);
			return;
		}

		int delCount = end - start + 1;
		int addCount = rows.length;
		int newSize = rowSize + addCount - delCount;

		Object[] tmp = new Object[newSize];
		System.arraycopy(data, 0, tmp, 0, start);
		System.arraycopy(rows, 0, tmp, start, addCount);
		System.arraycopy(data, end + 1, tmp, start + addCount, rowSize - end - 1);

		this.data = tmp;
		this.rowSize = newSize;
	}

	/**
	 * 交换c1、c2两列的位置
	 * @param c1 int
	 * @param c2 int
	 */
	public void exchangeCol(int c1, int c2) {
		Object[] data = this.data;
		for (int i = 0, len = data.length; i < len; ++i) {
			Object []row = (Object[])data[i];
			Object obj = row[c1];
			row[c1] = row[c2];
			row[c2] = obj;
		}
	}
}
