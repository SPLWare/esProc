package com.scudata.dm.cursor;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.op.IGroupsResult;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;

/**
 * 用于外存分组cs.groupx@n()和cs.groupx@g()的二次分组
 * groupx有@n和@g选项时，同一组的中间数据会存放到同一个临时文件，所以只需对每个临时文件单独做下二次分组就能得到最终的分组结果
 * @author RunQian
 *
 */
public class GroupxnCursor extends ICursor {
	private FileObject []files; // 首次分组产生的临时集文件
	private int fileIndex = -1; // 当前要读的临时文件索引
	private Expression[] exps; // 分组表达式数组
	private String[] names; // 分组字段名数组
	private Expression[] calcExps; // 汇总表达式数组
	private String[] calcNames; // 汇总字段名数组
	
	private MemoryCursor cursor; // 内存游标，用于保存当前临时文件的二次分组结果
	
	/**
	 * 构建外存分组的二次汇总游标
	 * @param files 首次分组产生的临时集文件数组
	 * @param exps 分组表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param ctx 计算上下文
	 */
	public GroupxnCursor(FileObject []files, Expression[] exps, String[] names, 
			Expression[] calcExps, String[] calcNames, Context ctx) {
		this.files = files;
		this.names = names;
		this.calcNames = calcNames;
		this.ctx = ctx;
		
		int keyCount = exps.length;
		int valCount = calcExps == null ? 0 : calcExps.length;
		String[] colNames = new String[keyCount + valCount];
		System.arraycopy(names, 0, colNames, 0, keyCount);
		if (this.calcNames != null) {
			System.arraycopy(this.calcNames, 0, colNames, keyCount, valCount);
		}

		DataStruct ds = new DataStruct(colNames);
		ds.setPrimary(names);
		setDataStruct(ds);
		
		// 取二次聚合需要用的表达式
		Node[] gathers = Sequence.prepareGatherMethods(calcExps, ctx);
		Expression []keyExps = new Expression[keyCount];
		for (int i = 0, q = 1; i < keyCount; ++i, ++q) {
			keyExps[i] = new Expression(ctx, "#" + q);
		}
		
		Expression []valExps = new Expression[valCount];
		for (int i = 0, q = keyCount + 1; i < valCount; ++i, ++q) {
			valExps[i] = gathers[i].getRegatherExpression(q);
		}
		
		this.exps = keyExps;
		this.calcExps = valExps;
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
			DataStruct ds = cs.getDataStruct();
			IGroupsResult groups = IGroupsResult.instance(exps, names, calcExps, calcNames, ds, "", ctx);
			groups.push(cs);
			Sequence seq = groups.getResultTable();
			cursor = new MemoryCursor(seq);
		}
		
		Sequence table = cursor.fetch(n);
		if (table == null || table.length() < n) {
			fileIndex++;
			if (fileIndex < files.length) {
				BFileCursor cs = new BFileCursor(files[fileIndex], null, "x", ctx);
				DataStruct ds = cs.getDataStruct();
				IGroupsResult groups = IGroupsResult.instance(exps, names, calcExps, calcNames, ds, "", ctx);
				groups.push(cs);
				Sequence seq = groups.getResultTable();
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
			DataStruct ds = cs.getDataStruct();
			IGroupsResult groups = IGroupsResult.instance(exps, names, calcExps, calcNames, ds, "", ctx);
			groups.push(cs);
			Sequence seq = groups.getResultTable();
			cursor = new MemoryCursor(seq);
		}

		long count = cursor.skip(n);
		if (count < n) {
			fileIndex++;
			if (fileIndex < files.length) {
				BFileCursor cs = new BFileCursor(files[fileIndex], null, "x", ctx);
				DataStruct ds = cs.getDataStruct();
				IGroupsResult groups = IGroupsResult.instance(exps, names, calcExps, calcNames, ds, "", ctx);
				groups.push(cs);
				Sequence seq = groups.getResultTable();
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
}
