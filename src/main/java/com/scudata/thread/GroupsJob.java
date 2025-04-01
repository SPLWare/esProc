package com.scudata.thread;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.IGroupsResult;
import com.scudata.expression.Expression;

/**
 * 对游标或排列执行分组运算的任务
 * @author WangXiaoJun
 *
 */
public class GroupsJob extends Job {
	private ICursor cursor; // 数据游标
	private Sequence seq; // 数据排列
	
	private Expression[] exps; // 分组字段表达式数组
	private String[] names; // 分组字段名数组
	private Expression[] calcExps; // 汇总字段表达式数组
	private String[] calcNames; // 汇总字段名数组
	private String opt; // 选项
	private Context ctx; // 计算上下文
	
	private IGroupsResult groupsResult; // 分组对象
	private int groupCount = -1; // @n方式分组时用户设置的结果集数量
	
	public GroupsJob(ICursor cursor, Expression[] exps, String[] names,
			Expression[] calcExps, String[] calcNames, String opt, Context ctx) {
		this.cursor = cursor;
		this.exps = exps;
		this.names = names;
		this.calcExps = calcExps;
		this.calcNames = calcNames;
		this.opt = opt;
		this.ctx = ctx;
	}
	
	public GroupsJob(Sequence seq, Expression[] exps, String[] names,
			Expression[] calcExps, String[] calcNames, String opt, Context ctx) {
		this.seq = seq;
		this.exps = exps;
		this.names = names;
		this.calcExps = calcExps;
		this.calcNames = calcNames;
		this.opt = opt;
		this.ctx = ctx;
	}
	
	/**
	 * 取分组对象
	 * @return IGroupsResult
	 */
	public IGroupsResult getGroupsResult() {
		return groupsResult;
	}
	
	/**
	 * @n选项使用，设置的结果集数量
	 * @param groupCount
	 */
	public void setGroupCount(int groupCount) {
		this.groupCount = groupCount;
	}

	public void run() {
		if (cursor != null) {
			groupsResult = cursor.getGroupsResult(exps, names, calcExps, calcNames, opt, ctx);
		} else {
			groupsResult = seq.getGroupsResult(exps, names, calcExps, calcNames, opt, ctx);
		}
		
		if (groupCount > 1) {
			groupsResult.setGroupCount(groupCount);
		}
		
		if (seq == null) {
			groupsResult.push(cursor);
		} else {
			groupsResult.push(seq, ctx);
		}
	}
}
