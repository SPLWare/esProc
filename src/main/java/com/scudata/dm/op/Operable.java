package com.scudata.dm.op;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;

/**
 * 可以附加运算的接口
 * @author WangXiaoJun
 *
 */
public abstract class Operable {
	/**
	 * 附加运算
	 * @param op 运算
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public abstract Operable addOperation(Operation op, Context ctx);
	
	/**
	 * 过滤
	 * @param function 对应的函数
	 * @param fltExp 过滤条件
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable select(Function function, Expression fltExp, String opt, Context ctx) {
		Select op = new Select(function, fltExp, opt);
		return addOperation(op, ctx);
	}
	
	/**
	 * 过滤
	 * @param function 对应的函数
	 * @param fltExp 过滤条件
	 * @param opt 选项
	 * @param pipe 用于处理不满足条件的成员
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable select(Function function, Expression fltExp, String opt, IPipe pipe, Context ctx) {
		Select op = new Select(function, fltExp, opt, pipe);
		return addOperation(op, ctx);
	}
	
	/**
	 * 进行连接过滤，保留能关联上的
	 * @param function 对应的函数
	 * @param exps 当前表关联字段表达式数组
	 * @param codes 维表数组
	 * @param dataExps 维表关联字段表达式数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable filterJoin(Function function, Expression[][] exps, Sequence[] codes, Expression[][] dataExps, String opt, Context ctx) {
		FilterJoin op = new FilterJoin(function, exps, codes, dataExps, opt);
		return addOperation(op, ctx);
	}
	
	/**
	 * 进行连接过滤，保留能关联不上的
	 * @param function 对应的函数
	 * @param exps 当前表关联字段表达式数组
	 * @param codes 维表数组
	 * @param dataExps 维表关联字段表达式数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable diffJoin(Function function, Expression[][] exps, Sequence[] codes, Expression[][] dataExps, String opt, Context ctx) {
		DiffJoin op = new DiffJoin(function, exps, codes, dataExps, opt);
		return addOperation(op, ctx);
	}
	
	/**
	 * 做连接
	 * @param function 对应的函数
	 * @param fname
	 * @param exps 当前表关联字段表达式数组
	 * @param codes 维表数组
	 * @param dataExps 维表关联字段表达式数组
	 * @param newExps
	 * @param newNames
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable join(Function function, String fname, Expression[][] exps, Sequence[] codes,
			  Expression[][] dataExps, Expression[][] newExps, String[][] newNames, String opt, Context ctx) {
		Join op = new Join(function, fname, exps, codes, dataExps, newExps, newNames, opt);
		return addOperation(op, ctx);
	}
	
	/**
	 * 与远程表做连接
	 * @param function 对应的函数
	 * @param fname
	 * @param exps 当前表关联字段表达式数组
	 * @param codes 维表数组
	 * @param dataExps 维表关联字段表达式数组
	 * @param newExps
	 * @param newNames
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable joinRemote(Function function, String fname, Expression[][] exps, 
			Object[] codes, Expression[][] dataExps, 
			Expression[][] newExps, String[][] newNames, String opt, Context ctx) {
		JoinRemote op = new JoinRemote(function, fname, exps, codes, dataExps, newExps, newNames, opt);
		return addOperation(op, ctx);
	}
	
	/**
	 * 做外键式连接
	 * @param function
	 * @param dimExps 连接表达式数组
	 * @param aliasNames 维表记录别名
	 * @param newExps 新产生字段表达式数组
	 * @param newNames 新产生字段名数组
	 * @param opt 选项，i：做交连接
	 * @param ctx
	 * @return
	 */
	public Operable fjoin(Function function, Expression[] dimExps, String []aliasNames, 
			Expression[][] newExps, String[][] newNames, String opt, Context ctx) {
		ForeignJoin op = new ForeignJoin(function, dimExps, aliasNames, newExps, newNames, opt);
		return addOperation(op, ctx);
	}
	
	/**
	 * 游标按主键做有序连接
	 * @param function
	 * @param srcKeyExps 连接表达式数组
	 * @param srcNewExps
	 * @param srcNewNames
	 * @param cursors 关联游标数组
	 * @param options 关联选项
	 * @param keyExps 连接表达式数组
	 * @param newExps
	 * @param newNames
	 * @param opt
	 * @param ctx
	 * @return
	 */
	public Operable pjoin(Function function, Expression []srcKeyExps, Expression []srcNewExps, String []srcNewNames, 
			ICursor []cursors, String []options, Expression [][]keyExps, 
			Expression [][]newExps, String [][]newNames, String opt, Context ctx) {
		// 设置跳块信息
		if (this instanceof ICursor) {
			ICursor cs = (ICursor)this;
			cs.setSkipBlock(srcKeyExps, cursors, options, keyExps, newExps, opt);
		}
		
		PrimaryJoin op = new PrimaryJoin(function, srcKeyExps, srcNewExps, srcNewNames, 
				cursors, options, keyExps, newExps, newNames, opt, ctx);
		return addOperation(op, ctx);
	}
	
	/**
	 * 与游标做有序归并连接
	 * @param function 对应的函数
	 * @param exps 当前表关联字段表达式数组
	 * @param cursors 维表游标数组
	 * @param codeExps 维表关联字段表达式数组
	 * @param newExps
	 * @param newNames
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable mergeJoinx(Function function, Expression[][] exps, 
			ICursor []cursors, Expression[][] codeExps, 
			Expression[][] newExps, String[][] newNames, String opt, Context ctx) {
		throw new RuntimeException();
	}
	
	/**
	 * 添加计算列
	 * @param function 对应的函数
	 * @param exps 计算表达式数组
	 * @param names 字段名数组
	 * @param opt 选项
	 * @param level
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable derive(Function function, Expression []exps, String []names, String opt, int level, Context ctx) {
		Derive op = new Derive(function, exps, names, opt, level);
		return addOperation(op, ctx);
	}
	
	/**
	 * 产生新序表
	 * @param function 对应的函数
	 * @param newExps 计算表达式数组
	 * @param names 字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable newTable(Function function, Expression []newExps, String []names, String opt, Context ctx) {
		New op = new New(function, newExps, names, opt);
		return addOperation(op, ctx);
	}
	
	/**
	 * 附加有序分组运算
	 * @param function 对应的函数
	 * @param exps 分组表达式数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable group(Function function, Expression []exps, String opt, Context ctx) {
		Group op = new Group(function, exps, opt);
		return addOperation(op, ctx);
	}
	
	/**
	 * 附加有序分组运算
	 * @param function 对应的函数
	 * @param exps 前半部分有序的分组字段表达式
	 * @param sortExps 后半部分无序的分组字段表达式
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable group(Function function, Expression []exps, Expression []sortExps, String opt, Context ctx) {
		Group op = new Group(function, exps, sortExps, opt);
		return addOperation(op, ctx);
	}
	
	/**
	 * 附加有序分组运算
	 * @param function 对应的函数
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param newExps 汇总表达式
	 * @param newNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable group(Function function, Expression[] exps, String []names, 
			Expression[] newExps, String []newNames, String opt, Context ctx) {
		Groups op = new Groups(function, exps, names, newExps, newNames, opt, ctx);
		return addOperation(op, ctx);
	}
	
	/**
	 * 附加有序分组运算
	 * @param function 对应的函数
	 * @param exps 前半部分有序的分组字段表达式
	 * @param names 字段名数组
	 * @param sortExps 后半部分无序的分组字段表达式
	 * @param sortNames 字段名数组
	 * @param newExps 汇总表达式
	 * @param newNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable group(Function function, Expression[] exps, String []names, 
			Expression[] sortExps, String []sortNames, 
			Expression[] newExps, String []newNames, String opt, Context ctx) {
		Groups op = new Groups(function, exps, names, sortExps, sortNames, newExps, newNames, opt, ctx);
		return addOperation(op, ctx);
	}
	
	/**
	 * 连接计算
	 * @param function 所属的函数对象
	 * @param fkNames 外键字段名数组
	 * @param timeFkNames 时间外键名数组
	 * @param codes 维表数组
	 * @param exps 维表主键数组
	 * @param timeExps 维表的时间更新键数组
	 * @param opt 选项
	 */
	public Operable switchFk(Function function, String[] fkNames, String[] timeFkNames, Sequence[] codes, Expression[] exps, Expression[] timeExps, String opt, Context ctx) {
		Switch op = new Switch(function, fkNames, timeFkNames, codes, exps, timeExps, opt);
		return addOperation(op, ctx);
	}
}