package com.raqsoft.dm.cursor;

import java.io.IOException;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.ILineInput;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.util.Variant;

/**
 * 用按行读数据接口构建游标
 * 此类是为了给用户提供由自定义读数方法创建游标
 * ILineInput.cursor()
 * @author RunQian
 *
 */
public class LineInputCursor extends ICursor {
	private ILineInput importer; // 按行读数据结构
	private DataStruct ds; // 结果集数据结构
	private boolean isTitle = false; // 第一行是否是标题
	private boolean isSingleField = false; // 是否返回单列组成的序列
	
	/**
	 * 构建按行读数据游标
	 * @param lineInput 按行读数据接口
	 * @param opt 选项
	 */
	public LineInputCursor(ILineInput lineInput, String opt) {
		this.importer = lineInput;
		if (opt != null) {
			if (opt.indexOf('t') != -1) isTitle = true;
			if (opt.indexOf('i') != -1) isSingleField = true;
		}
	}

	private Sequence fetchAll(ILineInput importer, int n) throws IOException {
		Object []line = importer.readLine();
		if (line == null) {
			return null;
		}

		int fcount;
		if (ds == null) {
			fcount = line.length;
			String []fieldNames = new String[fcount];
			if (isTitle) {
				for (int f = 0; f < fcount; ++f) {
					fieldNames[f] = Variant.toString(line[f]);
				}

				line = importer.readLine();
				if (line == null) {
					return null;
				}
			}

			ds = new DataStruct(fieldNames);
			setDataStruct(ds);
		} else {
			fcount = ds.getFieldCount();
		}

		if (isSingleField && fcount != 1) isSingleField = false;
		
		int curLen = line.length;
		if (curLen > fcount) curLen = fcount;

		int initSize = n > INITSIZE ? INITSIZE : n;
		if (isSingleField) {
			Sequence seq = new Sequence(initSize);
			seq.add(line[0]);

			for (int i = 1; i < n; ++i) {
				line = importer.readLine();
				if (line == null) {
					break;
				}

				seq.add(line[0]);
			}

			//seq.trimToSize();
			return seq;
		} else {
			Table table = new Table(ds, initSize);
			Record r = table.newLast();
			for (int f = 0; f < curLen; ++f) {
				r.setNormalFieldValue(f, line[f]);
			}

			for (int i = 1; i < n; ++i) {
				line = importer.readLine();
				if (line == null) {
					break;
				}

				r = table.newLast();
				curLen = line.length;
				if (curLen > fcount) curLen = fcount;
				for (int f = 0; f < curLen; ++f) {
					r.setNormalFieldValue(f, line[f]);
				}
			}

			//table.trimToSize();
			return table;
		}
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (n < 1 || importer == null) return null;

		try {
			return fetchAll(importer, n);
		} catch (IOException e) {
			close();
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		ILineInput importer = this.importer;
		if (n < 1 || importer == null) return 0;

		try {
			if (ds == null && isTitle) {
				Object []line = importer.readLine();
				if (line == null) {
					return 0;
				}
				
				int fcount = line.length;
				String []fieldNames = new String[fcount];
				for (int f = 0; f < fcount; ++f) {
					fieldNames[f] = Variant.toString(line[f]);
				}

				ds = new DataStruct(fieldNames);
			}
			
			for (int i = 0; i < n; ++i) {
				if (!importer.skipLine()) {
					return i;
				}
			}
		} catch (IOException e) {
			close();
			throw new RQException(e.getMessage(), e);
		}

		return n;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (importer != null) {
			try {
				importer.close();
			} catch (IOException e) {
			}

			importer = null;
			ds = null;
		}
	}

	protected void finalize() throws Throwable {
		close();
	}
}
