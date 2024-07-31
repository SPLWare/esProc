package com.scudata.dm.op;

import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;

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
		this(ctx, mcs, true);
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
		
		for (int i = 0; i < count; ++i) {
			channels[i] = cursors[i].newChannel(cursors[i].getContext(), doPush);
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
	 * 过滤
	 * @param function 对应的函数
	 * @param fltExp 过滤条件
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable select(Function function, Expression fltExp, String opt, Context ctx) {
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression curFilter = Operation.dupExpression(fltExp, ctx);
			channel.select(function, curFilter, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression curFilter = Operation.dupExpression(fltExp, ctx);
			channel.select(function, curFilter, opt, pipe, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression [][]curExps = Operation.dupExpressions(exps, ctx);
			Expression [][]curDataExps = Operation.dupExpressions(dataExps, ctx);
			channel.filterJoin(function, curExps, codes, curDataExps, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression [][]curExps = Operation.dupExpressions(exps, ctx);
			Expression [][]curDataExps = Operation.dupExpressions(dataExps, ctx);
			channel.diffJoin(function, curExps, codes, curDataExps, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression [][]curExps = Operation.dupExpressions(exps, ctx);
			Expression [][]curDataExps = Operation.dupExpressions(dataExps, ctx);
			Expression [][]curNewExps = Operation.dupExpressions(newExps, ctx);
			channel.join(function, fname, curExps, codes, curDataExps, curNewExps, newNames, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression [][]curExps = Operation.dupExpressions(exps, ctx);
			Expression [][]curDataExps = Operation.dupExpressions(dataExps, ctx);
			Expression [][]curNewExps = Operation.dupExpressions(newExps, ctx);
			channel.joinRemote(function, fname, curExps, codes, curDataExps, curNewExps, newNames, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression []curDimExps = Operation.dupExpressions(dimExps, ctx);
			Expression [][]curNewExps = Operation.dupExpressions(newExps, ctx);
			channel.fjoin(function, curDimExps, aliasNames, curNewExps, newNames, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression []curExps = Operation.dupExpressions(exps, ctx);
			channel.derive(function, curExps, names, opt, level, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression []curExps = Operation.dupExpressions(newExps, ctx);
			channel.newTable(function, curExps, names, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression []curExps = Operation.dupExpressions(exps, ctx);
			channel.group(function, curExps, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression []curExps = Operation.dupExpressions(exps, ctx);
			Expression []curSortExps = Operation.dupExpressions(sortExps, ctx);
			channel.group(function, curExps, curSortExps, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression []curExps = Operation.dupExpressions(exps, ctx);
			Expression []curNewExps = Operation.dupExpressions(newExps, ctx);
			channel.group(function, curExps, names, curNewExps, newNames, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression []curExps = Operation.dupExpressions(exps, ctx);
			Expression []curSortExps = Operation.dupExpressions(sortExps, ctx);
			Expression []curNewExps = Operation.dupExpressions(newExps, ctx);
			channel.group(function, curExps, names, curSortExps, sortNames, curNewExps, newNames, opt, ctx);
		}
		
		return this;
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
		checkResultChannel();
		
		for (Channel channel : channels) {
			ctx = channel.getContext();
			Expression []curexps = Operation.dupExpressions(exps, ctx);
			Expression []curTimeExps = Operation.dupExpressions(timeExps, ctx);
			channel.switchFk(function, fkNames, timeFkNames, codes, curexps, curTimeExps, opt, ctx);
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
		// 每路的管道已经调用过finish
		//for (Channel channel : channels) {
		//	channel.finish(ctx);
		//}
	}
	
	/**
	 * 返回管道的计算结果
	 * @return
	 */
	public Object result() {
		if (result instanceof IGroupsResult) {
			int count = channels.length;
			IGroupsResult groupsResult = (IGroupsResult)channels[0].getResult();
			IGroupsResult []groupsResults = new IGroupsResult[count - 1];
			
			for (int i = 1; i < count; ++i) {
				groupsResults[i - 1] = (IGroupsResult)channels[i].getResult();
			}
			
			Table value = groupsResult.combineGroupsResult(groupsResults, ctx);
			if (resultNew == null) {
				return value;
			} else {
				Sequence table = resultNew.process(value, ctx);
				if (pkCount > 0 && table instanceof Table) {
					String []pks = new String[pkCount];
					for (int i = 1; i <= pkCount; ++i) {
						pks[i - 1] = "#" + i;
					}
					
					((Table)table).setPrimary(pks);
					return table;
				} else {
					return table;
				}
			}
		} else if (result instanceof TotalResult) {
			int count = channels.length;
			IGroupsResult groupsResult = (IGroupsResult)channels[0].getResult();
			IGroupsResult []groupsResults = new IGroupsResult[count - 1];
			
			for (int i = 1; i < count; ++i) {
				groupsResults[i - 1] = (IGroupsResult)channels[i].getResult();
			}
			
			Table table = groupsResult.combineGroupsResult(groupsResults, ctx);
			if (table == null || table.length() == 0) {
				return null;
			} else {
				TotalResult total = (TotalResult)result;
				BaseRecord r = table.getRecord(1);
				int valCount = total.getCalcExps().length;
				if (valCount == 1) {
					return r.getNormalFieldValue(0);
				} else {
					Sequence seq = new Sequence(valCount);
					for (int i = 0; i < valCount; ++i) {
						seq.add(r.getNormalFieldValue(i));
					}
					
					return seq;
				}
			}
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
	 * 保留管道当前数据到集文件
	 * @param file 集文件
	 * @return this
	 */
	public Channel fetch(FileObject file) {
		checkResultChannel();
		result = new FetchResult(file);

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
		result = IGroupsResult.instance(exps, names, calcExps, calcNames, null, opt, ctx);
		
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
		result = new TotalResult(calcExps, ctx, null);
		
		for (Channel channel : channels) {
			Context ctx = channel.getContext();
			calcExps = Operation.dupExpressions(calcExps, ctx);
			channel.groups(null, null, calcExps, null, null);
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
	 * @param opt 选项
	 * @return
	 */
	public Channel id(Expression []exps, int count, String opt) {
		checkResultChannel();
		result = new IDResult(exps, count, opt, ctx);
		
		for (Channel channel : channels) {
			Push push = new Push(this);
			ctx = channel.getContext();
			channel.addOperation(push, ctx);
		}

		return this;
	}
}