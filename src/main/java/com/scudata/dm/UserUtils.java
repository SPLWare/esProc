package com.scudata.dm;

import java.util.List;

import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.LineInputCursor;

/**
 * 对外工具类
 */
public final class UserUtils {
	/**
	 * 由行输入产生游标
	 * @param lineInput ILineInput 行输入
	 * @param opt String 选项 t：第一行为标题，i：结果集只有1列时返回成序列
	 * @return ICursor 游标
	 */
	public static ICursor newCursor(ILineInput lineInput, String opt) {
		return new LineInputCursor(lineInput, opt);
	}
	
	/**
	 * 由行输入产生序表或序列
	 * @param lineInput ILineInput 行输入
	 * @param opt String 选项 t：第一行为标题，i：结果集只有1列时返回成序列
	 * @return Sequence 序表或序列
	 */
	public static Sequence newTable(ILineInput lineInput, String opt) {
		return newCursor(lineInput, opt).fetch();
	}
	
	/**
	 * 由数组产生序列
	 * @param values Object[] 值数组
	 * @return Sequence
	 */
	public static Sequence newSequence(Object []values) {
		return new Sequence(values);
	}
	
	/**
	 * 由List产生序列
	 * @param list List 值List
	 * @return Sequence
	 */
	public static Sequence newSequence(List<Object> list) {
		return new Sequence(list.toArray());
	}
}