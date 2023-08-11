package com.scudata.util;

import com.scudata.dm.BFileWriter;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.cursor.BFileSortxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;

/**
 * 集文件的工具类
 * @author LW
 *
 */
public class BFileUtil {
	/**
	 * 对及文件进行外存排序
	 * @param file 文件
	 * @param outFile 输出文件
	 * @param fields 排序字段数组
	 * @param ctx 计算上下文
	 * @param opt 选项(暂时无用)
	 * @return 排好序的游标
	 */
	public static void sortx(FileObject file, FileObject outFile, String[] fields, Context ctx, String opt) {
		int fcount = fields.length;
		BFileSortxCursor cursor = new BFileSortxCursor(file, fields);
		DataStruct ds  = cursor.getFileDataStruct();
		
		Expression[] tempExps = new Expression[fcount];
		for (int i = 0; i < fcount; i++) {
			tempExps[i] = new Expression(fields[i]);
		}
		EnvUtil.setMaxUsedMemoryPercent(0.2);
		ICursor cs = CursorUtil.sortx(cursor, tempExps, ctx, 0, opt);
		EnvUtil.setMaxUsedMemoryPercent(0.4);
		
		BFileWriter writer = new BFileWriter(outFile, null);
		writer.exportBinary(cs, ds, fcount, ctx);
		writer.close();
	}
}
