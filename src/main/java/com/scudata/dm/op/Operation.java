package com.scudata.dm.op;

import com.scudata.cellset.INormalCell;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;

/**
 * 游标和管道延迟计算函数基类
 * @author RunQian
 *
 */
public abstract class Operation {
	protected Function function; // 当前操作对应的表达式里的函数
	protected INormalCell currentCell; // 运算所在的单元格
	
	/**
	 * 构造函数
	 */
	public Operation() {
	}
	
	/**
	 * 构造函数
	 * @param function 当前操作对应的表达式里的函数
	 */
	public Operation(Function function) {
		this.function = function;
	}
	
	/**
	 * 取当前操作对应的表达式里的函数
	 * @return Function
	 */
	public Function getFunction() {
		return function;
	}
	
	/**
	 * 设置当前操作对应的表达式里的函数
	 * @param function
	 */
	public void setFunction(Function function) {
		this.function = function;
	}
	
	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public abstract Sequence process(Sequence seq, Context ctx);
	
	/**
	 * 取操作是否会减少元素数，比如过滤函数会减少记录
	 * 此函数用于游标的精确取数，如果附加的操作不会使记录数减少则只需按传入的数量取数即可
	 * @return true：会，false：不会
	 */
	public boolean isDecrease() {
		return false;
	}
	
	/**
	 * 数据全部推送完成时调用，group运算需要知道数据结束来确认最后一组的数据
	 * @param ctx 计算上下文
	 * @return 附加的操作缓存的数据
	 */
	public Sequence finish(Context ctx) {
		return null;
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public abstract Operation duplicate(Context ctx);
	
	/**
	 * 复制表达式
	 * @param exp 表达式
	 * @param ctx 计算上下文
	 * @return 复制后的表达式
	 */
	public static Expression dupExpression(Expression exp, Context ctx) {
		if (exp != null) {
			return exp.newExpression(ctx);
		} else {
			return null;
		}
	}
	
	/**
	 * 复制表达式数组
	 * @param exps 表达式数组
	 * @param ctx 计算上下文
	 * @return 复制后的表达式数组
	 */
	public static Expression[] dupExpressions(Expression []exps, Context ctx) {
		if (exps == null) {
			return null;
		}
		
		int len = exps.length;
		Expression []dupExps = new Expression[len];
		for (int i = 0; i < len; ++i) {
			if (exps[i] != null) dupExps[i] = exps[i].newExpression(ctx);
		}
		
		return dupExps;
	}
	
	/**
	 * 复制表达式数组的数组
	 * @param exps 表达式数组的数组
	 * @param ctx 计算上下文
	 * @return 复制后的表达式数组的数组
	 */
	public static Expression[][] dupExpressions(Expression [][]exps, Context ctx) {
		if (exps == null) {
			return null;
		}
		
		int len = exps.length;
		Expression [][]dupExps = new Expression[len][];
		for (int i = 0; i < len; ++i) {
			dupExps[i] = dupExpressions(exps[i], ctx);
		}
		
		return dupExps;
	}

	public INormalCell getCurrentCell() {
		return currentCell;
	}

	public void setCurrentCell(INormalCell currentCell) {
		this.currentCell = currentCell;
	}
}