package com.raqsoft.dm.op;

import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.util.Variant;

/**
 * 对推送来的数据执行有序分组，用于管道和游标的group延迟计算函数
 * @author RunQian
 *
 */
public class Group extends Operation {
	private Expression []exps; // 分组表达式数组
	private String opt; // 选项
	private boolean isign = false; // 是否是@i选项
	private boolean isDistinct = false; // 是否去重
	
	private Sequence data; // 当前组的数据
	private Object []values; // 当前分组的分组字段值
	
	// group@q数据已按前半部分表达式有序
	private Expression []sortExps; // 后半部分排序表达式
	private boolean isSort; // 是否只排序
	
	public Group(Expression []exps, String opt) {
		this(null, exps, opt);
	}

	public Group(Function function, Expression []exps, String opt) {
		super(function);
		this.exps = exps;
		this.opt = opt;
		if (opt != null) {
			if (opt.indexOf('i') != -1) {
				isign = true;
			} else if (opt.indexOf('1') != -1) {
				isDistinct = true;
				values = new Object[exps.length];
			} else {
				values = new Object[exps.length];
			}
		} else {
			values = new Object[exps.length];
		}
	}
	
	public Group(Function function, Expression []exps,  Expression []sortExps, String opt) {
		super(function);
		this.exps = exps;
		this.opt = opt;
		this.sortExps = sortExps;
		
		data = new Sequence();
		values = new Object[exps.length];
		isSort = opt != null && opt.indexOf('s') != -1;
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
		Expression []dupExps = dupExpressions(exps, ctx);
		if (sortExps == null) {
			return new Group(function, dupExps, opt);
		} else {
			Expression []dupSortExps = dupExpressions(sortExps, ctx);
			return new Group(function, dupExps, dupSortExps, opt);
		}
	}
	
	/**
	 * 数据全部推送完成时调用，返回最后一组的数据
	 * @param ctx 计算上下文
	 * @return Sequence
	 */
	public Sequence finish(Context ctx) {
		if (data != null) {
			if (sortExps == null) {
				Sequence seq = new Sequence(1);
				seq.add(data);
				data = null;
				return seq;
			} else {
				if (isSort) {
					Sequence tmp = data.sort(sortExps, null, null, ctx);
					data = null;
					return tmp;
				} else {
					Sequence tmp = data.group(sortExps, null, ctx);
					data = null;
					return tmp;
				}
			}
		} else {
			return null;
		}
	}

	private Sequence group_i(Sequence seq, Context ctx) {
		Expression boolExp = exps[0];
		Sequence result = new Sequence();
		Sequence group = data;
		if (group == null) {
			group = new Sequence();
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = seq.new Current();
		stack.push(current);

		try {
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				current.setCurrent(i);
				if (Variant.isTrue(boolExp.calculate(ctx)) && group.length() > 0) {
					result.add(group);
					group = new Sequence();
				}
				
				group.add(current.getCurrent());
			}
		} finally {
			stack.pop();
		}

		data = group;
		if (result.length() > 0) {
			return result;
		} else {
			return null;
		}
	}
	
	private Sequence group_o(Sequence seq, Context ctx) {
		Expression[] exps = this.exps;
		int vcount = exps.length;
		Object []values = this.values;
		Sequence result = new Sequence();
		Sequence group = data;
		if (group == null) {
			group = new Sequence();
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = seq.new Current();
		stack.push(current);

		try {
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				current.setCurrent(i);
				if (group.length() > 0) {
					boolean sign = true;
					for (int v = 0; v < vcount; ++v) {
						if (sign) {
							Object value = exps[v].calculate(ctx);
							if (!Variant.isEquals(values[v], value)) {
								sign = false;
								result.add(group);
								group = new Sequence();
								values[v] = value;
							}
						} else {
							values[v] = exps[v].calculate(ctx);
						}
					}
				} else {
					for (int v = 0; v < vcount; ++v) {
						values[v] = exps[v].calculate(ctx);
					}
				}

				group.add(current.getCurrent());
			}
		} finally {
			stack.pop();
		}

		data = group;
		if (result.length() > 0) {
			return result;
		} else {
			return null;
		}
	}
	
	private Sequence group_1(Sequence seq, Context ctx) {
		Expression[] exps = this.exps;
		int vcount = exps.length;
		Object []values = this.values;
		Sequence result = new Sequence();
		
		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = seq.new Current();
		stack.push(current);

		try {
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				current.setCurrent(i);
				boolean sign = true;
				for (int v = 0; v < vcount; ++v) {
					if (sign) {
						Object value = exps[v].calculate(ctx);
						if (!Variant.isEquals(values[v], value)) {
							sign = false;
							result.add(current.getCurrent());
							values[v] = value;
						}
					} else {
						values[v] = exps[v].calculate(ctx);
					}
				}
			}
		} finally {
			stack.pop();
		}

		if (result.length() > 0) {
			return result;
		} else {
			return null;
		}
	}
	
	private Sequence group_q(Sequence seq, Context ctx) {
		Expression[] exps = this.exps;
		Expression[] sortExps = this.sortExps;
		boolean isSort = this.isSort;
		int fcount1 = exps.length;
		
		Object []values = this.values;
		Sequence data = this.data;
		Sequence result = new Sequence();
		
		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = seq.new Current();
		stack.push(current);

		try {
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				current.setCurrent(i);
				boolean isSame = true;
				
				// 计算前半段表达式，检查是否与前一条记录相同
				for (int v = 0; v < fcount1; ++v) {
					if (isSame) {
						Object value = exps[v].calculate(ctx);
						if (!Variant.isEquals(values[v], value)) {
							isSame = false;
							values[v] = value;
						}
					} else {
						values[v] = exps[v].calculate(ctx);
					}
				}

				if (isSame || data.length() == 0) {
					data.add(current.getCurrent());
				} else {
					if (isSort) {
						Sequence tmp = data.sort(sortExps, null, null, ctx);
						result.addAll(tmp);
					} else {
						Sequence tmp = data.group(sortExps, null, ctx);
						result.addAll(tmp);
					}
					
					data.clear();
					data.add(current.getCurrent());
				}
			}
		} finally {
			stack.pop();
		}
		
		if (result.length() > 0) {
			return result;
		} else {
			return null;
		}
	}
	
	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		if (isign) {
			return group_i(seq, ctx);
		} else if (isDistinct) {
			return group_1(seq, ctx);
		} else if (sortExps != null) {
			return group_q(seq, ctx);
		} else {
			return group_o(seq, ctx);
		}
	}
	
	public String getOpt() {
		return opt;
	}
}
