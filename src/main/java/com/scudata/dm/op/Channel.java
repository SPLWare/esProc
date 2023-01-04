package com.scudata.dm.op;

import java.util.ArrayList;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;

/**
 * 管道对象，管道可以附加多种运算，但只能定义一种结果集运算
 * @author WangXiaoJun
 *
 */
public class Channel extends Operable implements IPipe {
	protected Context ctx; // 用多线程游标取数时需要更改上下文并重新解析表达式
	private ArrayList<Operation> opList; // 附加操作列表
	protected IResult result; // 管道最终的结果集函数
	
	// 分组表达式里如果有sum(...)+sum(...)这样的汇总项会变成groups(...).new(...)，用于存放后面的new
	protected New resultNew;
	
	/**
	 * 构建管道
	 * @param ctx 计算上下文
	 */
	public Channel(Context ctx) {
		this.ctx = ctx;
	}
	
	/**
	 * 用游标构建管道，游标的数据将会推给此管道
	 * @param ctx 计算上下文
	 * @param cs 游标
	 */
	public Channel(Context ctx, ICursor cs) {
		this.ctx = ctx;
		Push push = new Push(this);
		cs.addOperation(push, ctx);
	}
	
	/**
	 * 给游标添加push数据到管道的操作
	 * @param cs
	 */
	public void addPushToCursor(ICursor cs) {
		Push push = new Push(this);
		cs.addOperation(push, ctx);
	}
	
	/**
	 * 为管道附加运算
	 * @param op 运算
	 * @param ctx 计算上下文
	 * @return Operable
	 */
	public Operable addOperation(Operation op, Context ctx) {
		checkResultChannel();
		
		this.ctx = ctx;
		if (opList == null) {
			opList = new ArrayList<Operation>();
		}
		
		opList.add(op);
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
		if (opList != null) {
			for (Operation op : opList) {
				if (seq == null || seq.length() == 0) {
					return;
				}
				
				seq = op.process(seq, ctx);
			}
		}
		
		if (result != null && seq != null) {
			result.push(seq, ctx);
		}
	}
	
	/**
	 * 数据推送结束时调用，有些附加的操作会缓存数据，需要调用finish进行最后的处理
	 * @param ctx 计算上下文
	 */
	public void finish(Context ctx) {
		if (opList == null) {
			if (result != null) {
				result.finish(ctx);
			}
		} else {
			Sequence seq = null;
			for (Operation op : opList) {
				if (seq == null) {
					seq = op.finish(ctx);
				} else {
					seq = op.process(seq, ctx);
					Sequence tmp = op.finish(ctx);
					if (tmp != null) {
						if (seq != null) {
							seq = ICursor.append(seq, tmp);
						} else {
							seq = tmp;
						}
					}
				}
			}
			
			if (result != null && seq != null) {
				result.push(seq, ctx);
				result.finish(ctx);
			}
		}
	}
	
	/**
	 * 返回管道的计算结果
	 * @return
	 */
	public Object result() {
		if (result != null) {
			Object val = result.result();
			result = null;
			if (resultNew != null) {
				if (val instanceof Sequence) {
					return resultNew.process((Sequence)val, ctx);
				} else {
					return val;
				}
			} else {
				return val;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * 保留管道当前数据做为结果集
	 * @return this
	 */
	public Channel fetch() {
		checkResultChannel();
		result = new FetchResult();
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
		return this;
	}
	
	/**
	 * 对管道当前数据进行去重运算并作为结果集
	 * @param exps 去重表达式
	 * @param count 保留的结果数
	 * @param opt 选项
	 * @return
	 */
	public Channel id(Expression []exps, int count, String opt) {
		checkResultChannel();
		result = new IDResult(exps, count, opt, ctx);
		return this;
	}
	
	/**
	 * 取管道的结果对象
	 * @return IResult
	 */
	public IResult getResult() {
		return result;
	}
	
	/**
	 * groups运算如果汇总字段不是单纯的聚合表达，最后需要再new一下
	 * @param op new操作
	 */
	public void setResultNew(New op) {
		resultNew = op;
	}
	
	/**
	 * 取计算上下文
	 * @return Context
	 */
	public Context getContext() {
		return ctx;
	}
}