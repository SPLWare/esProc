package com.raqsoft.dm;

import java.io.IOException;
import java.io.OutputStream;

import com.raqsoft.util.Variant;

/**
 * 用于导出排列到文本文件
 * @author WangXiaoJun
 *
 */
public class LineExporter implements ILineOutput {
	private OutputStream os; // 输出流
	private final String charset; // 字符集
	private final byte []colSeparator; // 列分隔符
	private final byte []lineSeparator; // 行分隔符
	private boolean isAppend; // 是否追加写
	
	private char escapeChar = '\\';
	private boolean isQuote = false; // 字符串是否加引号

	/**
	 * 构造按行输出对象
	 * @param os 输出流
	 * @param charset 字符集
	 * @param colSeparator 列分隔符
	 * @param lineSeparator 行分隔符
	 * @param isAppend 是否追加写
	 */
	public LineExporter(OutputStream os, String charset, byte []colSeparator, byte []lineSeparator, boolean isAppend) {
		this.os = os;
		this.charset = charset;
		this.colSeparator = colSeparator;
		this.lineSeparator = lineSeparator;
		this.isAppend = isAppend;
	}
	
	/**
	 * 设置字符串是否加引号
	 * @param b
	 */
	public void setQuote(boolean b) {
		this.isQuote = b;
	}
	
	/**
	 * 设置转义符
	 * @param c 转义符
	 */
	public void setEscapeChar(char c) {
		escapeChar = c;
	}
	
	/**
	 * 取转义符
	 * @return char
	 */
	public char getEscapeChar() {
		return escapeChar;
	}

	/**
	 * 关闭输出
	 * @throws IOException
	 */
	public void close() throws IOException {
		os.close();
	}

	/**
	 * 写出一行数据
	 * @param items 列值组成的数组
	 * @throws IOException
	 */
	public void writeLine(Object []items) throws IOException {
		if (isAppend) {
			os.write(lineSeparator);
		} else {
			isAppend = true;
		}
		
		int last = items.length - 1;
		if (isQuote) {
			for (int i = 0; i < last; ++i) {
				String str = Variant.toExportString(items[i], escapeChar);
				if (str != null) {
					os.write(str.getBytes(charset));
				}

				os.write(colSeparator);
			}

			String str = Variant.toExportString(items[last], escapeChar);
			if (str != null) {
				os.write(str.getBytes(charset));
			}
		} else {
			for (int i = 0; i < last; ++i) {
				String str = Variant.toExportString(items[i]);
				if (str != null) {
					os.write(str.getBytes(charset));
				}

				os.write(colSeparator);
			}

			String str = Variant.toExportString(items[last]);
			if (str != null) {
				os.write(str.getBytes(charset));
			}
		}
	}
}
