package com.scudata.dm.cursor;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;

/**
 * 对每个文件进行单独排序，纵向连接做为一个游标
 * 用于cs.sortx@n(...;n) 按组进行排序
 * @author RunQian
 *
 */
public class SortxCursor extends ICursor {
	private FileObject []files; // 临时集文件数组
	private int fileIndex = -1; // 当前读到的文件的序号
	private Expression[] sortExps; // 排序表达式
	
	private MemoryCursor cursor; // 当前文件排序后创建的内存游标
	
	/**
	 * 对每个文件进行单独排序，纵向连接做为一个游标
	 * @param files 临时集文件数组
	 * @param sortExps 排序表达式数组
	 * @param ds 结果集数据结构
	 * @param ctx 计算上下文
	 */
	public SortxCursor(FileObject []files, Expression[] sortExps, DataStruct ds, Context ctx) {
		this.files = files;
		this.sortExps = sortExps;
		this.ctx = ctx;
		setDataStruct(ds);
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
		this.ctx = ctx;
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (files == null || n < 1) return null;
		
		if (fileIndex == -1) {
			fileIndex++;
			BFileCursor cs = new BFileCursor(files[0], null, "x", ctx);
			Sequence seq = cs.fetch();
			seq.sort(sortExps, null, "o", ctx);
			cursor = new MemoryCursor(seq);
		}
		
		Sequence table = cursor.fetch(n);
		if (table == null || table.length() < n) {
			fileIndex++;
			if (fileIndex < files.length) {
				BFileCursor cs = new BFileCursor(files[fileIndex], null, "x", ctx);
				Sequence seq = cs.fetch();
				seq.sort(sortExps, null, "o", ctx);
				cursor = new MemoryCursor(seq);
				
				if (table == null) {
					return get(n);
				} else {
					Sequence rest;
					if (n == MAXSIZE) {
						rest = get(n);
					} else {
						rest = get(n - table.length());
					}
					
					table = append(table, rest);
				}
			} else {
				files = null;
				cursor = null;
			}
		}

		return table;
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (files == null || n < 1) return 0;
		
		if (fileIndex == -1) {
			fileIndex++;
			BFileCursor cs = new BFileCursor(files[0], null, "x", ctx);
			Sequence seq = cs.fetch();
			seq.sort(sortExps, null, "o", ctx);
			cursor = new MemoryCursor(seq);
		}

		long count = cursor.skip(n);
		if (count < n) {
			fileIndex++;
			if (fileIndex < files.length) {
				BFileCursor cs = new BFileCursor(files[fileIndex], null, "x", ctx);
				Sequence seq = cs.fetch();
				seq.sort(sortExps, null, "o", ctx);
				cursor = new MemoryCursor(seq);
				
				count += skipOver(n - count);
			}
		}

		return count;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		
		if (files != null) {
			for (FileObject file : files) {
				if (file != null) {
					file.delete();
				}
			}
			
			files = null;
			cursor = null;
		}
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		return false;
	}
}
