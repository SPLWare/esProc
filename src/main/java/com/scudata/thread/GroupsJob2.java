package com.scudata.thread;

import com.scudata.dm.Context;
import com.scudata.dm.GroupsSyncReader;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.IGroupsResult;
import com.scudata.expression.Expression;

/**
 * 对游标或排列执行分组运算的任务
 * @author LW
 *
 */
public class GroupsJob2 extends Job {
	private GroupsSyncReader reader; // 数据游标
	
	private Expression[] exps; // 分组字段表达式数组
	private String[] names; // 分组字段名数组
	private Expression[] calcExps; // 汇总字段表达式数组
	private String[] calcNames; // 汇总字段名数组
	private String opt; // 选项
	private Context ctx; // 计算上下文
	
	private IGroupsResult groupsResult; // 分组对象
	private int capacity; // hash表大小
	
	private int hashStart;// 包含
	private int hashEnd;// 不包含
	
	public GroupsJob2(GroupsSyncReader reader, Expression[] exps, String[] names,
			Expression[] calcExps, String[] calcNames, String opt, Context ctx, int capacity) {
		this.reader = reader;
		this.exps = exps;
		this.names = names;
		this.calcExps = calcExps;
		this.calcNames = calcNames;
		this.opt = opt;
		this.ctx = ctx;
		this.capacity = capacity;
	}
	
	/**
	 * 取分组对象
	 * @return IGroupsResult
	 */
	public IGroupsResult getGroupsResult() {
		return groupsResult;
	}

	public void run() {
		ICursor cursor = reader.getCursor();
		groupsResult = cursor.getGroupsResult(exps, names, calcExps, calcNames, opt, ctx);
		groupsResult.setCapacity(capacity);
		
		groupsResult.push(reader, hashStart, hashEnd);
	}
	
	public void setHashStart(int hashStart) {
		this.hashStart = hashStart;
	}

	public void setHashEnd(int hashEnd) {
		this.hashEnd = hashEnd;
	}
}
