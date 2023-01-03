package com.scudata.dm.op;

import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.util.Variant;

/**
 * 游标或管道附加的过滤运算处理类
 * op.select(x) op.select(x;f) op.select(x;ch) op是游标或管道，f是文件，ch是管道，不满足条件的写到文件或管道
 * @author RunQian
 *
 */
public class Select extends Operation {
	protected Expression fltExp; // 过滤表达式
	protected String opt; // 选项
	protected IPipe pipe; // 管道，如果不为空，则把不满足条件的数据推送到管道中

	protected boolean isContinuous; // 满足条件的记录是否是连续的一个区间
	protected boolean isFound; // 是否已经找到，@c时找到后再碰到不匹配的结束查找
	protected boolean isOrg; // 是否改变原序列
	
	public Select(Expression fltExp, String opt) {
		this(null, fltExp, opt, null);
	}

	public Select(Function function, Expression fltExp, String opt) {
		this(function, fltExp, opt, null);
	}

	public Select(Function function, Expression fltExp, String opt, IPipe pipe) {
		super(function);
		this.fltExp = fltExp;
		this.opt = opt;
		this.pipe = pipe;
		
		if (opt != null) {
			this.isContinuous = opt.indexOf('c') != -1;
			this.isOrg = opt.indexOf('o') != -1;
		}
	}
	
	/**
	 * 取过滤表达式
	 * @return
	 */
	public Expression getFilterExpression() {
		return fltExp;
	}

	/**
	 * 取操作是否会减少元素数，比如过滤函数会减少记录
	 * 此函数用于游标的精确取数，如果附加的操作不会使记录数减少则只需按传入的数量取数即可
	 * @return true：会，false：不会
	 */
	public boolean isDecrease() {
		return true;
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		Expression dupExp = dupExpression(fltExp, ctx);
		return new Select(function, dupExp, opt, pipe);
	}

	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		Expression exp = this.fltExp;
		int len = seq.length();
		Sequence result = new Sequence();
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		stack.push(current);

		try {
			if (isContinuous) {
				if (isFound) {
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						Object obj = exp.calculate(ctx);
						if (Variant.isTrue(obj)) {
							result.add(current.getCurrent());
						} else {
							break;
						}
					}
				} else {
					int i = 1;
					for (; i <= len; ++i) {
						current.setCurrent(i);
						Object obj = exp.calculate(ctx);
						if (Variant.isTrue(obj)) {
							isFound = true;
							result.add(current.getCurrent());
							break;
						}
					}
	
					for (++i; i <= len; ++i) {
						current.setCurrent(i);
						Object obj = exp.calculate(ctx);
						if (Variant.isTrue(obj)) {
							result.add(current.getCurrent());
						} else {
							break;
						}
					}
				}
			} else if (pipe == null) {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					if (Variant.isTrue(obj)) {
						result.add(current.getCurrent());
					}
				}
			} else {
				Sequence other = new Sequence();
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					if (Variant.isTrue(obj)) {
						result.add(current.getCurrent());
					} else {
						other.add(current.getCurrent());
					}
				}
				
				if (other.length() != 0) {
					pipe.push(other, ctx);
				}
			}
		} finally {
			stack.pop();
		}

		if (isOrg) {
			seq.setMems(result.getMems());
			return seq;
		} else if (result.length() != 0) {
			return result;
		} else {
			return null;
		}
	}
	
	public Sequence finish(Context ctx) {
		if (pipe != null) {
			pipe.finish(ctx);
		}
		
		return null;
	}
}
