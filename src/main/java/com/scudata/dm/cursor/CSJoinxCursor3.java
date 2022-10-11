package com.scudata.dm.cursor;

import java.util.ArrayList;

import com.scudata.common.RQException;
import com.scudata.dm.*;
import com.scudata.dm.op.Operation;
import com.scudata.dw.ColumnTableMetaData;
import com.scudata.dw.Cursor;
import com.scudata.expression.Expression;
import com.scudata.util.CursorUtil;

/**
 * 游标joinx类，归并joinx
 * 游标与一个可分段集文件或实表T做归并join运算。
 * @author LiWei
 * 
 */
public class CSJoinxCursor3 extends ICursor {
	private ICursor srcCursor;//源游标
	private Expression []keys;//维表字段
	private Expression []exps;//新的表达式
	
	private ICursor mergeCursor;//归并游标
	private DataStruct ds = null;
	private int len1;//原记录字段数
	private int len2;//新表达式字段数
	
	private boolean isEnd;
	private int n;//缓冲区条数
	
	/**
	 * 构造器
	 * @param cursor	源游标
	 * @param fields	事实join表字段
	 * @param fileTable	维表对象
	 * @param keys		维表join字段
	 * @param exps		维表新表达式
	 * @param expNames	维表新表达式名称
	 * @param fname
	 * @param ctx
	 * @param n
	 * @param option
	 */
	public CSJoinxCursor3(ICursor cursor, Expression []fields, Object fileTable, 
			Expression[] keys, Expression[] exps, String[] expNames, String fname, Context ctx, int n, String option) {
		srcCursor = cursor;
		this.keys = keys;
		this.exps = exps;
		this.ctx = ctx;
		this.n = n;
		if (this.n < ICursor.FETCHCOUNT) {
			this.n = ICursor.FETCHCOUNT;
		}
		
		//如果newNames里有null，则用newExps替代
		if (exps != null && expNames != null) {
			for (int i = 0, len = expNames.length; i < len; i++) {
				if (expNames[i] == null && exps[i] != null) {
					expNames[i] = exps[i].getFieldName();
				}
			}
		}

		//归并两个游标
		ICursor cursors[] = {cursor, toCursor(fileTable)};
		String names[] = {null, null};
		Expression joinKeys[][] = {fields, keys};
		mergeCursor = CursorUtil.joinx(cursors, names, joinKeys, option, ctx);
		
		//组织数据结构
		Sequence temp = mergeCursor.peek(1);
		Record r = (Record) temp.getMem(1);
		Record r1 = (Record) r.getNormalFieldValue(0);
		len1 = r1.getFieldCount();
		len2 = exps == null ? 0 : exps.length;
		names = new String[len1 + len2];
		System.arraycopy(r1.getFieldNames(), 0, names, 0, len1);
		System.arraycopy(expNames, 0, names, len1, len2);
		ds = new DataStruct(names);
	}

	/**
	 * 从join字段和新表达式中提取需要的字段
	 * @param dataExps
	 * @param newExps
	 * @param ctx
	 * @return
	 */
	private static String[] makeFields(Expression []dataExps, Expression []newExps ,Context ctx) {
		int len = dataExps.length;
		ArrayList<String> keys = new ArrayList<String>(len);
		for (int j = 0; j < len; j++) {
			keys.add(dataExps[j].toString());
		}
		for (Expression exp : newExps) {
			exp.getUsedFields(ctx, keys);
		}
		String[] arr = new String[keys.size()];
		keys.toArray(arr);
		return arr;
	}
	
	/**
	 * 把维表对象转换成游标
	 * @param obj
	 * @return
	 */
	private ICursor toCursor(Object obj) {
		if (obj instanceof ColumnTableMetaData) {
			String fields[] = makeFields(keys, exps, ctx);
			return (Cursor) ((ColumnTableMetaData) obj).cursor(null, fields, null, null, null, null, ctx);
		} else if (obj instanceof FileObject) {
			return new BFileCursor((FileObject) obj, null, null, null);
		} else if (obj instanceof ICursor) {
			return (ICursor) obj;
		} else {
			return null;
		}
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	protected void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			exps = Operation.dupExpressions(exps, ctx);
			super.resetContext(ctx);
		}
	}

	protected Sequence get(int n) {
		if (isEnd || n < 1) return null;
		Sequence temp = mergeCursor.fetch(n);
		if (temp == null || temp.length() == 0) {
			close();
			return null;
		}
		
		Context ctx = this.ctx;
		Expression []exps = this.exps;
		int len1 = this.len1;
		int len2 = this.len2;
		int len = temp.length();
		Table result = new Table(ds);
		for (int i = 1; i <= len; i++) {
			Record r = (Record) temp.getMem(i);
			Record r1 = (Record) r.getNormalFieldValue(0);
			Record r2 = (Record) r.getNormalFieldValue(1);
			
			Record record = result.newLast(r1.getFieldValues());
			for (int f = 0; f < len2; f++) {
				record.setNormalFieldValue(f + len1, r2.calc(exps[f], ctx));
			}
		}
		
		if (len < n) {
			close();
		}
		return result;
	}

	protected long skipOver(long n) {
		if (isEnd || n < 1) return 0;
		Sequence seq = get((int) n);
		if (seq != null) {
			return seq.length();
		} else {
			return 0;
		}
	}

	public synchronized void close() {
		super.close();
		srcCursor.close();
		isEnd = true;
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		super.close();
		srcCursor.reset();
		isEnd = false;
		return true;
	}
	
	/**
	 * 归并join（多套数据）
	 * @param cursor	源游标
	 * @param fields	事实join表字段
	 * @param fileTable	维表对象
	 * @param keys		维表join字段
	 * @param exps		维表新表达式
	 * @param expNames	维表新表达式名称
	 * @param fname
	 * @param ctx
	 * @param n
	 * @param option
	 * @return
	 */
	public static ICursor MergeJoinx(ICursor cursor, Expression[][] fields, Object[] fileTable, Expression[][] keys,
			Expression[][] exps, String[][] expNames, String fname, Context ctx, int n, String option) {
		if (fileTable == null) {
			return null;
		}
		
		if (cursor instanceof MultipathCursors) {
			return MultipathMergeJoinx((MultipathCursors)cursor, fields, fileTable, keys,
					exps, expNames, fname, ctx, n, option);
		}

		ICursor temp = null;
		FileObject tempFile = null;
		int fileCount =  fileTable.length;
		try {
			/**
			 * 对多套数据进行join，每次的结果写出到文件，（不处理最后一套join）
			 */
			for (int i = 0; i < fileCount - 1; i++) {
				temp = new CSJoinxCursor3(cursor, fields[i], fileTable[i], keys[i], exps[i], 
						expNames[i], fname, ctx, n, option);
				
				tempFile = FileObject.createTempFileObject();
				cursor = new BFileCursor(tempFile, null, "x", ctx);
				tempFile.setFileSize(0);
				
				Sequence table = temp.fetch(FETCHCOUNT);
				while (table != null && table.length() != 0) {
					tempFile.exportSeries(table, "ab", null);
					table = temp.fetch(FETCHCOUNT);
				}
				temp = null;
			}
		} catch (Exception e) {
			if (temp != null) {
				temp.close();
			}
			if (tempFile != null && tempFile.isExists()) {
				tempFile.delete();
			}
			if (e instanceof RQException) {
				throw (RQException)e;
			} else {
				throw new RQException(e.getMessage(), e);
			}
		}
		
		int i = fileCount - 1;
		return new CSJoinxCursor3(cursor, fields[i], fileTable[i], keys[i], exps[i], 
				expNames[i], fname, ctx, n, option);
	}
	
	public static ICursor MultipathMergeJoinx(MultipathCursors cursor, Expression[][] fields, Object[] fileTable, Expression[][] keys,
			Expression[][] exps, String[][] expNames, String fname, Context ctx, int n, String option) {

		ICursor[] cursors = cursor.getParallelCursors();
		int pathCount = cursor.getPathCount();
		ICursor results[] = new ICursor[pathCount];
		for (int i = 0; i < pathCount; ++i) {
			results[i] = MergeJoinx(cursors[i], fields, fileTable, keys, exps, expNames, fname, ctx, n, option);
		}
		return new MultipathCursors(results, ctx);
		
	}
}
