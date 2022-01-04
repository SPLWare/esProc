package com.scudata.dm.cursor;

import java.io.IOException;
import java.util.Arrays;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileReader;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.resources.EngineMessage;

/**
 * 按给定的记录在集文件中的位置构建游标
 * @author RunQian
 *
 */
public class PFileCursor extends ICursor {
	private FileObject fo; // 集文件对象
	private long []pos; // 记录在集文件中的位置
	private int bufSize; // 读文件用的缓冲区大小
	private String []selFields; // 选出字段
	
	private BFileReader reader; // 集文件读取器
	private DataStruct ds; // 源文件数据结构
	private DataStruct selDs; // 结果集数据结构
	private int []selIndex; // 源结构的每个字段在结果集结构中的序号
	private int index = 0; // 当前要读的记录的序号
	
	// 如果skip所有则不需要排序
	private boolean isSorted = false; // 位置数组是否已排序
	private boolean isEnd = false; // 是否取数结束
	
	/**
	 * 构建读取指定位置记录的游标
	 * @param fo 集文件对象
	 * @param pos 记录位置数组
	 * @param bufSize 读文件用的缓冲区的大小
	 * @param fields 选出字段名数组
	 * @param opt 选项，u：记录位置数组没有按照从小到大排序，默认是排好序的
	 * @param ctx 计算上下文
	 */
	public PFileCursor(FileObject fo, long []pos, int bufSize, String []fields, String opt, Context ctx) {
		this.fo = fo;
		this.pos = pos;
		this.bufSize = bufSize;
		this.selFields = fields;
		this.ctx = ctx;
		
		if (opt != null) {
			if (opt.indexOf('u') != -1) isSorted = true;
		}
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (n < 1) return null;
		
		BFileReader reader = open();
		if (reader == null) return null;
		
		long []pos = this.pos;
		int index = this.index;
		int rest = pos.length - index;
		if (rest < 1) {
			return null;
		}
		
		if (rest <= n) {
			n = rest;
		}
		
		try {
			Table table;
			if (selFields == null) {
				int fcount = ds.getFieldCount();
				table = new Table(ds, n);
				Object []values = new Object[fcount];
				
				for (int i = 0; i < n; ++i, ++index) {
					reader.seek(pos[index]);
					reader.readRecord(values);
					table.newLast(values);
				}
			} else {
				int []selIndex = this.selIndex;
				table = new Table(selDs, n);
				Object []values = new Object[selDs.getFieldCount()];
				
				for (int i = 0; i < n; ++i, ++index) {
					reader.seek(pos[index]);
					reader.readRecord(selIndex, values);
					table.newLast(values);
				}
			}
			
			this.index += n;
			return table;
		} catch (Exception e) {
			close();
			throw new RQException(e.getMessage(), e);
		}
	}

	private BFileReader open() {
		if (reader != null) return reader;
		if (isEnd) return null;

		try {
			if (!isSorted) {
				Arrays.sort(pos);
				isSorted = true;
			}
			
			reader = new BFileReader(fo);
			reader.open(bufSize);
			ds = reader.getFileDataStruct();
			if (ctx != null) {
				ctx.addResource(this);
			}

			if (selFields != null) {
				int fcount = ds.getFieldCount();
				selIndex = new int[fcount];
				for (int i = 0; i < fcount; ++i) {
					selIndex[i] = -1;
				}

				for (int i = 0, count = selFields.length; i < count; ++i) {
					int q = ds.getFieldIndex(selFields[i]);
					if (q < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(selFields[i] + mm.getMessage("ds.fieldNotExist"));
					}

					if (selIndex[q] != -1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(selFields[i] + mm.getMessage("ds.colNameRepeat"));
					}

					selIndex[q] = i;
					selFields[i] = ds.getFieldName(q);
				}

				selDs = new DataStruct(selFields);
				setDataStruct(selDs);
			} else {
				setDataStruct(ds);
			}

			return reader;
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
		if (n < 1) {
			return 0;
		}
		
		int len = pos.length;
		if (len - index > n) {
			if (!isSorted) {
				Arrays.sort(pos);
				isSorted = true;
			}
			
			index += n;
		} else {
			n = len - index;
			index = len;
			close();
		}

		return n;
	}
	
	/**
	 * 关闭游标
	 */
	public void close() {
		super.close();
		
		isEnd = true;
		if (reader != null) {
			if (ctx != null) ctx.removeResource(this);
			try {
				reader.close();
			} catch (IOException e) {
			}
			
			reader = null;
		}
		
		ds = null;
		selDs = null;
	}

	protected void finalize() throws Throwable {
		close();
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		index = 0;
		isEnd = false;
		return true;
	}
}
