package com.scudata.dm.op;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;

/**
 * 多路管道对象，管道可以附加多种运算，但只能定义一种结果集运算
 * @author WangXiaoJun
 *
 */
public class MultipathChannel extends Channel {
	private Channel []channels;
	
	/**
	 * 由多路游标构建多路管道
	 * @param ctx 计算上下文
	 * @param mcs 多路游标
	 */
	public MultipathChannel(Context ctx, MultipathCursors mcs) {
		super(ctx);
		
		ICursor []cursors = mcs.getCursors();
		int count = cursors.length;
		channels = new Channel[count];
		
		for (int i = 0; i < count; ++i) {
			channels[i] = new Channel(cursors[i].getContext(), cursors[i]);
		}
	}
	
	/**
	 * 由多路游标构建多路管道
	 * @param ctx 计算上下文
	 * @param mcs 多路游标
	 * @param doPush 是否对游标生成push操作
		游标可能fetch@0缓存了一部分数据，如果管道的运算还没定义完就给游标附加push，会导致缓存的数据没有被管道后来附加的运算计算
	 */
	public MultipathChannel(Context ctx, MultipathCursors mcs, boolean doPush) {
		super(ctx);
		
		ICursor []cursors = mcs.getCursors();
		int count = cursors.length;
		channels = new Channel[count];
		
		if (doPush) {
			for (int i = 0; i < count; ++i) {
				channels[i] = new Channel(cursors[i].getContext(), cursors[i]);
			}
		} else {
			for (int i = 0; i < count; ++i) {
				channels[i] = new Channel(cursors[i].getContext(), cursors[i].isColumnCursor());
			}
		}
	}
	
	/**
	 * 给游标添加push数据到管道的操作
	 * @param cs
	 */
	public void addPushToCursor(ICursor cs) {
		MultipathCursors mcs = (MultipathCursors)cs;
		ICursor []cursors = mcs.getCursors();
		int count = cursors.length;
		
		for (int i = 0; i < count; ++i) {
			Push push = new Push(channels[i]);
			cursors[i].addOperation(push, cursors[i].getContext());
		}
	}
	
	/**
	 * 为管道附加运算
	 * @param op 运算
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable addOperation(Operation op, Context ctx) {
		checkResultChannel();
		for (Channel channel : channels) {
			ctx = channel.getContext();
			channel.addOperation(op.duplicate(ctx), ctx);
		}
		
		return this;
	}
	
	/**
	 * 检查是否已经有结果集函数了
	 */
	protected void checkResultChannel() {
		if (result != null) {
			throw new RQException("附加结果集之后不能再继续附加其它运算");
		}
	}
	
	/**
	 * 往管道推送数据，可能会有多个源同时往管道推送数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 */
	public synchronized void push(Sequence seq, Context ctx) {
		if (result != null && seq != null) {
			result.push(seq, ctx);
		}
	}
	
	/**
	 * 数据推送结束时调用，有些附加的操作会缓存数据，需要调用finish进行最后的处理
	 * @param ctx 计算上下文
	 */
	public void finish(Context ctx) {
		for (Channel channel : channels) {
			channel.finish(ctx);
		}
	}
	
	/**
	 * 返回管道的计算结果
	 * @return
	 */
	public Object result() {
		if (result instanceof IGroupsResult) {
			Table value = null;
			for (Channel channel : channels) {
				IGroupsResult groups = (IGroupsResult)channel.getResult();
				if (value == null) {
					value = groups.getTempResult();
				} else {
					value.addAll(groups.getTempResult());
				}
			}
			
			IGroupsResult gr = (IGroupsResult)result;
			result = null;
			if (value == null || gr.isSortedGroup()) {
				return value;
			}
			
			String []names = gr.getNames();
			Expression []calcExps = gr.getCalcExps();
			String []calcNames = gr.getCalcNames();
			String opt = gr.getOption();
			int keyCount = names == null ? 0 : names.length;
			Expression []keyExps = null;
			if (keyCount > 0) {
				keyExps = new Expression[keyCount];
				for (int i = 0, q = 1; i < keyCount; ++i, ++q) {
					keyExps[i] = new Expression(ctx, "#" + q);
				}
			}

			int valCount = calcExps == null ? 0 : calcExps.length;
			Expression []valExps = null;
			if (valCount > 0) {
				valExps = new Expression[valCount];
				for (int i = 0, q = keyCount + 1; i < valCount; ++i, ++q) {
					Node gather = calcExps[i].getHome();
					gather.prepare(ctx);
					valExps[i] = gather.getRegatherExpression(q);
				}
			}

			value = value.groups(keyExps, names, valExps, calcNames, opt, ctx);
			if (resultNew != null) {
				return resultNew.process(value, ctx);
			} else {
				return value;
			}
		} else if (result instanceof TotalResult) {
			TotalResult tr = (TotalResult)result;
			Expression []calcExps = tr.getCalcExps();
			int valCount = calcExps.length;
			int channelCount = channels.length;
			Table value;
			if (valCount == 1) {
				String []fnames = new String[]{"_1"};
				value = new Table(fnames, channelCount);
				for (Channel channel : channels) {
					TotalResult total = (TotalResult)channel.getResult();
					Record r = value.newLast();
					r.setNormalFieldValue(0, total.getTempResult());
				}
			} else {
				String []fnames = new String[valCount];
				for (int i = 1; i < valCount; ++i) {
					fnames[i - 1] = "_" + i;
				}
				
				value = new Table(fnames, channelCount);
				for (Channel channel : channels) {
					TotalResult total = (TotalResult)channel.getResult();
					Sequence seq = (Sequence)total.getTempResult();
					value.newLast(seq.toArray());
				}
			}
			
			Expression []valExps = new Expression[valCount];
			for (int i = 0; i < valCount; ++i) {
				Node gather = calcExps[i].getHome();
				gather.prepare(ctx);
				valExps[i] = gather.getRegatherExpression(i + 1);
			}
			
			TotalResult total = new TotalResult(valExps, ctx);
			total.push(value, ctx);
			return total.result();
		} else if (result != null) {
			Object val = result.result();
			result = null;
			return val;
		} else {
			return null;
		}
	}
	
	/**
	 * 保留管道当前数据做为结果集
	 * @return
	 */
	public Channel fetch() {
		checkResultChannel(); 	
		result = new FetchResult();

		for (Channel channel : channels) {
			Push push = new Push(this);
			ctx = channel.getContext();
			channel.addOperation(push, ctx);
		}
		
		return this;
	}
	
	/**
	 * 对管道当前数据进行分组运算并做为结果集
	 * @param exps 分组表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总表达式数组
	 * @param calcNames 汇总字段名
	 * @param opt 选项
	 * @return
	 */
	public Channel groups(Expression[] exps, String[] names,
			   Expression[] calcExps, String[] calcNames, String opt) {
		checkResultChannel();
		result = IGroupsResult.instance(exps, names, calcExps, calcNames, opt, ctx);
		
		for (Channel channel : channels) {
			Context ctx = channel.getContext();
			exps = Operation.dupExpressions(exps, ctx);
			calcExps = Operation.dupExpressions(calcExps, ctx);
			channel.groups(exps, names, calcExps, calcNames, opt);
		}

		return this;
	}
	
	/**
	 * 对管道当前数据进行汇总运算并做为结果集
	 * @param calcExps 汇总表达式
	 * @return
	 */
	public Channel total(Expression[] calcExps) {
		checkResultChannel();
		result = new TotalResult(calcExps, ctx);
		
		for (Channel channel : channels) {
			Context ctx = channel.getContext();
			calcExps = Operation.dupExpressions(calcExps, ctx);
			channel.total(calcExps);
		}

		return this;
	}
	
	/**
	 * 对管道当前数据进行外存分组运算并做为结果集
	 * @param exps 分组表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总表达式数组
	 * @param calcNames 汇总字段名
	 * @param opt 选项
	 * @param capacity 内存可以存放的分组结果数量
	 * @return
	 */
	public Channel groupx(Expression[] exps, String[] names,
			   Expression[] calcExps, String[] calcNames, String opt, int capacity) {
		checkResultChannel();
		result = new GroupxResult(exps, names, calcExps, calcNames, opt, ctx, capacity);

		for (Channel channel : channels) {
			Push push = new Push(this);
			ctx = channel.getContext();
			channel.addOperation(push, ctx);
		}

		return this;
	}
	
	/**
	 * 对管道当前数据进行外存排序运算并做为结果集
	 * @param exps 排序表达式数组
	 * @param capacity 内存可以存放的记录数量
	 * @param opt 选项
	 * @return
	 */
	public Channel sortx(Expression[] exps, int capacity, String opt) {
		checkResultChannel();
		result = new SortxResult(exps, ctx, capacity, opt);

		for (Channel channel : channels) {
			Push push = new Push(this);
			ctx = channel.getContext();
			channel.addOperation(push, ctx);
		}

		return this;
	}
	
	/**
	 * 对管道当前数据进行连接运算并做为结果集
	 * @param fields
	 * @param fileTable
	 * @param keys
	 * @param exps
	 * @param expNames
	 * @param fname
	 * @param ctx
	 * @param option
	 * @param capacity
	 * @return
	 */
	public Channel joinx(Expression [][]fields, Object []fileTable, Expression[][] keys, 
			Expression[][] exps, String[][] expNames, String fname, Context ctx, String option, int capacity) {
		checkResultChannel();
		result = new CsJoinxResult(fields, fileTable, keys, exps, expNames, fname, ctx, option, capacity);

		for (Channel channel : channels) {
			Push push = new Push(this);
			ctx = channel.getContext();
			channel.addOperation(push, ctx);
		}

		return this;
	}
	
	/**
	 * 对管道当前数据进行迭代运算并作为结果集
	 * @param exp 迭代表达式
	 * @param initVal 初始值
	 * @param c 条件表达式，迭代过程中c为真则提前结束
	 * @param ctx 计算上下文
	 * @return
	 */
	public Channel iterate(Expression exp, Object initVal, Expression c, Context ctx) {
		checkResultChannel();
		result = new IterateResult(exp, initVal, c, ctx);
		
		for (Channel channel : channels) {
			Push push = new Push(this);
			ctx = channel.getContext();
			channel.addOperation(push, ctx);
		}

		return this;
	}
	
	/**
	 * 对管道当前数据进行去重运算并作为结果集
	 * @param exps 去重表达式
	 * @param count
	 * @return
	 */
	public Channel id(Expression []exps, int count) {
		checkResultChannel();
		result = new IDResult(exps, count, ctx);
		
		for (Channel channel : channels) {
			Push push = new Push(this);
			ctx = channel.getContext();
			channel.addOperation(push, ctx);
		}

		return this;
	}
}