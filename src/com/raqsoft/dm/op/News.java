package com.raqsoft.dm.op;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;

/**
 * 游标或管道的附加的合并产生的序表处理类
 * cs.news(...)
 * @author RunQian
 *
 */
public class News extends Operation  {
	private Expression gexp; // 返回值为序列的表达式
	private Expression[] newExps; // 字段表达式数组
	private String []names; // 字段名数组
	private String opt; // 选项
	private DataStruct newDs; // 结构集数据结构
	
	public News(Expression gexp, Expression []newExps, String []names, String opt) {
		this(null, gexp, newExps, names, opt);
	}
	
	public News(Function function, Expression gexp, Expression []newExps, String []names, String opt) {
		super(function);

		this.gexp = gexp;
		this.newExps = newExps;
		this.names = names;
		this.opt = opt;
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		Expression dupExp = dupExpression(gexp, ctx);
		Expression []dupExps = dupExpressions(newExps, ctx);
		return new News(function, dupExp, dupExps, names, opt);
	}

	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		Table result;
		if (newDs != null) {
			result = seq.newTables(gexp, newExps, newDs, opt, ctx);
		} else {
			result = seq.newTables(gexp, names, newExps, opt, ctx);
			if (result != null) {
				newDs = result.dataStruct();
			}
		}
		
		if (result.length() != 0) {
			return result;
		} else {
			return null;
		}
	}
}
