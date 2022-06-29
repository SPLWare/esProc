package com.scudata.dw;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.IGroupsResult;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;

/**
 * 列式游标接口
 * @author runqian
 *
 */
abstract public class IColumnCursorUtil {
	public static IColumnCursorUtil util;
	
	static {
		try {
			Class<?> cls = Class.forName("com.scudata.dw.columns.ColumnCursorUtil");
			util = (IColumnCursorUtil) cls.newInstance();
		} catch (ClassNotFoundException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		}
	}
	
	public abstract ICursor cursor(ITableMetaData table);

	public abstract ICursor cursor(ITableMetaData table, String []fields, Expression filter, Context ctx);
	
	public abstract ICursor cursor(ITableMetaData table, Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, Context ctx);
	
	public abstract ICursor cursor(ITableMetaData table, Expression []exps, String []fields, Expression filter, String []fkNames, 
			Sequence []codes, String []opts, int pathCount, Context ctx);
	
	public abstract ICursor cursor(ITableMetaData table, Expression []exps, String []fields, Expression filter, String []fkNames, 
			Sequence []codes, String []opts, int segSeq, int segCount, Context ctx);
	
	public abstract ICursor cursor(ITableMetaData table, Expression []exps, String []fields, Expression filter, String []fkNames, 
			Sequence []codes,  String []opts, MultipathCursors mcs, String opt, Context ctx);
	
	public abstract ICursor createCursor(Object table, IParam param, String opt, Context ctx);
	
	public abstract IGroupsResult getGroupsResultInstance(Expression[] exps, String[] names, Expression[] calcExps, 
			String[] calcNames, String opt, Context ctx);
	
	public abstract Table groups(ICursor cursor, Expression[] exps, String[] names, Expression[] calcExps, String[] calcNames, 
			String opt, Context ctx);
	
	public abstract Table groups(ICursor cursor, Expression[] exps, String[] names, Expression[] calcExps, String[] calcNames, 
			String opt, Context ctx, int groupCount);
	
	/**
	 * 游标对关联字段有序，做有序归并连接
	 * @param cursors 游标数组
	 * @param names 结果集字段名数组
	 * @param exps 关联字段表达式数组
	 * @param opt 选项
	 * @param ctx Context 计算上下文
	 * @return ICursor 结果集游标
	 */
	//public abstract ICursor joinx(ICursor []cursors, String []names, Expression [][]exps, String opt, Context ctx);
	
	/**
	 * 转换为行存序表
	 * @param src
	 * @return
	 */
	public abstract Sequence convert(Sequence src);
	
	public abstract Sequence switchColumnTable(Sequence seq, boolean isIsect, boolean isDiff, String fkName, Sequence code,
			String timeName, IndexTable indexTable, DataStruct ds, int keySeq, boolean isLeft, Context ctx);
	
	public abstract Sequence derive(Sequence table, Expression[] exps, DataStruct newDs, boolean containNull, Context ctx);
	
	public abstract Object createMemoryTable(ICursor cursor, String[] keys, String opt);
}
