package com.scudata.dm.op;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.Gather;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HashUtil;
import com.scudata.util.Variant;

/**
 * 对推送来的数据执行有序分组汇总，用于管道和游标带汇总字段的group延迟计算函数
 * @author RunQian
 *
 */
public class Groups extends Operation {
	private Expression []exps; // 分组表达式数组
	private String []names; // 分组字段名数组
	private Expression[] newExps; // 汇总表达式数组
	private String []newNames; // 汇总字段名数组
	
	private Node []gathers; // 汇总表达式对应的汇总函数
	private DataStruct newDs; // 结果集数据结构
	private String opt; // 选项
	private boolean iopt = false; // 是否是@i选项
	private boolean sopt = false; // 是否是@s选项，用累积方式计算
	private boolean eopt = false; // 是否是@e选项
	
	// 有序时采用累积法分组使用
	private Record r; // 当前分组汇总到的记录
	
	// group@q数据已按前半部分表达式有序
	private Expression []sortExps; // 后半部分需要排序表达式
	private Expression []groupExps; // 复制的总的分组表达式
	private String []sortNames;
	private Sequence data;
	private Object []values;
	
	// 前半部分有序时采用累积法分组使用
	private HashUtil hashUtil; // 提供哈希运算的哈希类
	private ListBase1 []groups; // 哈希表
	private Table tempResult; // 分组汇总结果
	private int []sortFields; // 用来对分组结果进行排序
	
	private String []alterFields; // @b选项时结果集需要去掉分组字段
	
	/**
	 * 构造分组运算对象
	 * @param exps 前半部分有序的分组字段表达式
	 * @param names 字段名数组
	 * @param newExps 汇总表达式
	 * @param newNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public Groups(Expression[] exps, String []names, 
			Expression[] newExps, String []newNames, String opt, Context ctx) {
		this(null, exps, names, newExps, newNames, opt, ctx);
	}
	
	/**
	 * 构造分组运算对象
	 * @param function
	 * @param exps 前半部分有序的分组字段表达式
	 * @param names 字段名数组
	 * @param newExps 汇总表达式
	 * @param newNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public Groups(Function function, Expression[] exps, String []names, 
			Expression[] newExps, String []newNames, String opt, Context ctx) {
		super(function);
		int count = exps.length;
		int newCount = newExps == null ? 0 : newExps.length;

		if (names == null) names = new String[count];
		for (int i = 0; i < count; ++i) {
			if (names[i] == null || names[i].length() == 0) {
				names[i] = exps[i].getFieldName();
			}
		}

		if (newNames == null) newNames = new String[newCount];
		for (int i = 0; i < newCount; ++i) {
			if (newNames[i] == null || newNames[i].length() == 0) {
				newNames[i] = newExps[i].getFieldName();
			}
		}

		String []totalNames = new String[count + newCount];
		System.arraycopy(names, 0, totalNames, 0, count);
		System.arraycopy(newNames, 0, totalNames, count, newCount);
		newDs = new DataStruct(totalNames);
		newDs.setPrimary(names);
		gathers = Sequence.prepareGatherMethods(newExps, ctx);
		
		if (opt != null) {
			if (opt.indexOf('i') != -1) iopt = true;
			if (opt.indexOf('s') != -1) sopt = true;
			if (opt.indexOf('e') != -1) eopt = true;
			if (newCount > 0 && opt.indexOf('b') != -1) {
				alterFields = newNames;
			}
		}

		this.exps = exps;
		this.names = names;
		this.newExps = newExps;
		this.newNames = newNames;
		this.opt = opt;
		
		if (sopt) {
			// 采用累积法分组
			gathers = Sequence.prepareGatherMethods(newExps, ctx);
		} else {
			for (int i = 0; i < newCount; ++i) {
				if (newExps[i].getHome() instanceof Gather) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group" + mm.getMessage("function.invalidParam"));
				}
			}
			
			data = new Sequence();
			values = new Object[count];
		}
	}
	
	/**
	 * 构造分组运算对象
	 * @param function
	 * @param exps 前半部分有序的分组字段表达式
	 * @param names 字段名数组
	 * @param sortExps 后半部分无序的分组字段表达式
	 * @param sortNames 字段名数组
	 * @param newExps 汇总表达式
	 * @param newNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public Groups(Function function, Expression[] exps, String []names, 
			Expression[] sortExps, String []sortNames, 
			Expression[] newExps, String []newNames, String opt, Context ctx) {
		super(function);
		int count = exps.length;
		int sortCount = sortExps.length;
		int newCount = newExps == null ? 0 : newExps.length;
		int keyCount = count + sortCount;
		groupExps = new Expression[keyCount];
		
		if (names == null) names = new String[count];
		for (int i = 0; i < count; ++i) {
			groupExps[i] = dupExpression(exps[i], ctx);
			if (names[i] == null || names[i].length() == 0) {
				names[i] = exps[i].getFieldName();
			}
		}

		if (sortNames == null) sortNames = new String[sortCount];
		for (int i = 0; i < sortCount; ++i) {
			groupExps[i + count] = dupExpression(sortExps[i], ctx);
			if (sortNames[i] == null || sortNames[i].length() == 0) {
				sortNames[i] = sortExps[i].getFieldName();
			}
		}

		if (newNames == null) newNames = new String[newCount];
		for (int i = 0; i < newCount; ++i) {
			if (newNames[i] == null || newNames[i].length() == 0) {
				newNames[i] = newExps[i].getFieldName();
			}
		}

		String []pks = new String[keyCount];
		System.arraycopy(names, 0, pks, 0, count);
		System.arraycopy(sortNames, 0, pks, count, sortCount);
		
		String []totalNames = new String[keyCount + newCount];
		System.arraycopy(pks, 0, totalNames, 0, keyCount);
		System.arraycopy(newNames, 0, totalNames, keyCount, newCount);
		newDs = new DataStruct(totalNames);
		newDs.setPrimary(pks);

		if (opt != null) {
			if (opt.indexOf('i') != -1) iopt = true;
			if (opt.indexOf('s') != -1) sopt = true;
			if (opt.indexOf('e') != -1) eopt = true;
			if (newCount > 0 && opt.indexOf('b') != -1) {
				alterFields = newNames;
			}
		}
		
		this.exps = exps;
		this.names = names;
		this.sortExps = sortExps;
		this.sortNames = sortNames;
		this.newExps = newExps;
		this.newNames = newNames;
		this.opt = opt;
		
		if (sopt) {
			// 采用累积法分组
			gathers = Sequence.prepareGatherMethods(newExps, ctx);
			hashUtil = new HashUtil(1031);
			groups = new ListBase1[hashUtil.getCapacity()];
			tempResult = new Table(newDs, 1024);
			values = new Object[keyCount];
			
			sortFields = new int[keyCount];
			for (int i = 0; i < keyCount; ++i) {
				sortFields[i] = i;
			}
		} else {
			for (int i = 0; i < newCount; ++i) {
				if (newExps[i].getHome() instanceof Gather) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group" + mm.getMessage("function.invalidParam"));
				}
			}
			
			data = new Sequence();
			values = new Object[count];
		}
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
		Expression []dupNewExps = dupExpressions(newExps, ctx);
		if (sortExps == null) {
			return new Groups(function, dupExps, names, dupNewExps, newNames, opt, ctx);
		} else {
			Expression []dupSortExps = dupExpressions(sortExps, ctx);
			return new Groups(function, dupExps, names, 
					dupSortExps, sortNames, dupNewExps, newNames, opt, ctx);
		}
	}
	
	/**
	 * 数据全部推送完成时调用，返回最后一组的数据
	 * @param ctx 计算上下文
	 * @return Sequence
	 */
	public Sequence finish(Context ctx) {
		if (r != null) {
			// @i或@s选项
			Table result = new Table(r.dataStruct(), 1);
			result.getMems().add(r);
			r = null;
			return finishGroupsResult(result);
		} else if (data != null && data.length() > 0) {
			Table result = new Table(newDs);
			if (sortExps == null) {
				group1(data, values, newExps, ctx, result);
			} else {
				group(data, groupExps, newExps, ctx, result);
			}
			
			if (eopt) {
				result = result.fieldValues(result.getFieldCount() - 1).derive("o");
			} else if (alterFields != null) {
				result.alter(alterFields, null);
			}
			
			data = new Sequence(); //null; 为了游标reset重复使用需要重新创建序列
			return result;
		} else if (tempResult != null) {
			// @qs选项
			Table result = tempResult;
			tempResult = null;
			groups = null;
			
			result.sortFields(sortFields);
			return finishGroupsResult(result);
		} else {
			return null;
		}
	}

	private Sequence finishGroupsResult(Table result) {
		if (gathers != null) {
			result.finishGather(gathers);
		}
		
		if (eopt) {
			return result.fieldValues(result.getFieldCount() - 1).derive("o");
		}

		if (alterFields != null) {
			result.alter(alterFields, null);
		}
		
		return result;
	}
	
	private Sequence groups_i(Sequence seq, Context ctx) {
		DataStruct newDs = this.newDs;
		Expression boolExp = exps[0];
		Node []gathers = this.gathers;
		int valCount = gathers == null ? 0 : gathers.length;
		
		Table result = new Table(newDs);
		IArray mems = result.getMems();
		Record r = this.r;
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		stack.push(current);

		try {
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				current.setCurrent(i);
				if (r != null) {
					if (Variant.isTrue(boolExp.calculate(ctx))) {
						mems.add(r);
						r = new Record(newDs);
						r.setNormalFieldValue(0, Boolean.TRUE);
						for (int v = 0, f = 1; v < valCount; ++v, ++f) {
							r.setNormalFieldValue(f, gathers[v].gather(ctx));
						}
					} else {
						for (int v = 0, f = 1; v < valCount; ++v, ++f) {
							r.setNormalFieldValue(f, gathers[v].gather(r.getNormalFieldValue(f), ctx));
						}
					}
				} else {
					r = new Record(newDs);
					r.setNormalFieldValue(0, boolExp.calculate(ctx));
					for (int v = 0, f = 1; v < valCount; ++v, ++f) {
						r.setNormalFieldValue(f, gathers[v].gather(ctx));
					}
				}
			}
		} finally {
			stack.pop();
		}

		this.r = r;
		if (result.length() > 0) {
			return finishGroupsResult(result);
		} else {
			return null;
		}
	}
	
	// 对一组数据计算汇总
	private static void group1(Sequence data, Object []groupValues, Expression []newExps, Context ctx, Table result) {
		if (newExps == null) {
			result.newLast(groupValues);
			return;
		}
		
		Sequence seq = new Sequence(1);
		seq.add(data);
		int keyCount = groupValues.length;
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		current.setCurrent(1);
		stack.push(current);

		// 计算聚合字段值
		try {
			BaseRecord r = result.newLast(groupValues);
			for (Expression newExp : newExps) {
				r.setNormalFieldValue(keyCount++, newExp.calculate(ctx));
			}
		} finally {
			stack.pop();
		}
	}
	
	private Sequence groups_o(Sequence seq, Context ctx) {
		Expression[] exps = this.exps;
		int fcount1 = exps.length;
		Sequence data = this.data;
		Object []values = this.values;
		Object []prevValues = new Object[fcount1];
		Table result = new Table(newDs);

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
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
							System.arraycopy(values, 0, prevValues, 0, fcount1);
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
					group1(data, prevValues, newExps, ctx, result);
					data = new Sequence(); // newExps可能为~，需要重新创建序列
					data.add(current.getCurrent());
				}
			}
		} finally {
			stack.pop();
		}

		this.data = data;
		if (result.length() > 0) {
			if (eopt) {
				result = result.fieldValues(result.getFieldCount() - 1).derive("o");
			} else if (alterFields != null) {
				result.alter(alterFields, null);
			}

			return result;
		} else {
			return null;
		}
	}
	
	// 分组字段有序时用累积方式计算汇总值
	private Sequence groups_os(Sequence seq, Context ctx) {
		DataStruct newDs = this.newDs;
		Expression[] exps = this.exps;
		Node []gathers = this.gathers;
		int keyCount = exps.length;
		int valCount = gathers == null ? 0 : gathers.length;
		
		Table result = new Table(newDs);
		IArray mems = result.getMems();
		Record r = this.r;
		Object []keys = new Object[keyCount];

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		stack.push(current);

		try {
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				current.setCurrent(i);
				for (int k = 0; k < keyCount; ++k) {
					keys[k] = exps[k].calculate(ctx);
				}
				
				if (r != null) {
					if (Variant.compareArrays(r.getFieldValues(), keys, keyCount) == 0) {
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							r.setNormalFieldValue(f, gathers[v].gather(r.getNormalFieldValue(f), ctx));
						}
					} else {
						mems.add(r);
						r = new Record(newDs, keys);
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							r.setNormalFieldValue(f, gathers[v].gather(ctx));
						}
					}
				} else {
					r = new Record(newDs, keys);
					for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
						r.setNormalFieldValue(f, gathers[v].gather(ctx));
					}
				}
			}
		} finally {
			stack.pop();
		}

		this.r = r;
		if (result.length() > 0) {
			return finishGroupsResult(result);
		} else {
			return null;
		}
	}
	
	// 先分组再对每组数据计算汇总
	private static void group(Sequence data, Expression []groupExps, Expression []newExps, Context ctx, Table result) {
		Sequence groups = data.group(groupExps, null, ctx);
		int len = groups.length();
		Sequence keyGroups = new Sequence(len);
		for (int i = 1; i <= len; ++i) {
			Sequence seq = (Sequence)groups.getMem(i);
			keyGroups.add(seq.getMem(1));
		}

		int oldCount = result.length();
		int keyCount = groupExps.length;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(keyGroups);
		stack.push(current);

		// 计算分组字段值
		try {
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = result.newLast();
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					r.setNormalFieldValue(c, groupExps[c].calculate(ctx));
				}
			}
		} finally {
			stack.pop();
		}
		
		if (newExps == null) {
			return;
		}
		
		int valCount = newExps.length;
		current = new Current(groups);
		stack.push(current);

		// 计算聚合字段值
		try {
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)result.getMem(++oldCount);
				current.setCurrent(i);
				for (int c = 0; c < valCount; ++c) {
					r.setNormalFieldValue(c + keyCount, newExps[c].calculate(ctx));
				}
			}
		} finally {
			stack.pop();
		}
	}
	
	// 前半有序分组时采用先算好分组再算汇总值
	private Sequence groups_q(Sequence seq, Context ctx) {
		Expression[] exps = this.exps;
		int fcount1 = exps.length;
		Sequence data = this.data;
		Object []values = this.values;
		Table result = new Table(newDs);

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
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
					group(data, groupExps, newExps, ctx, result);
					data.clear();
					data.add(current.getCurrent());
				}
			}
		} finally {
			stack.pop();
		}

		if (result.length() > 0) {
			if (eopt) {
				result = result.fieldValues(result.getFieldCount() - 1).derive("o");
			} else if (alterFields != null) {
				result.alter(alterFields, null);
			}

			return result;
		} else {
			return null;
		}
	}
	
	// 前半有序分组时采用累积法计算
	private Sequence groups_qs(Sequence seq, Context ctx) {
		int fcount1 = exps.length;
		Expression[] groupExps = this.groupExps;
		Node []gathers = this.gathers;
		int keyCount = groupExps.length;
		int valCount = gathers == null ? 0 : gathers.length;
		Object []values = this.values;
		HashUtil hashUtil = this.hashUtil;
		ListBase1 []groups = this.groups;
		Table tempResult = this.tempResult;
		Table result = new Table(newDs);
		
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize(); // 哈希表的大小
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		stack.push(current);

		try {
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				current.setCurrent(i);
				boolean isSame = true;
				
				// 计算前半段表达式，检查是否与前一条记录相同
				for (int v = 0; v < fcount1; ++v) {
					if (isSame) {
						Object value = groupExps[v].calculate(ctx);
						if (!Variant.isEquals(values[v], value)) {
							isSame = false;
							values[v] = value;
						}
					} else {
						values[v] = groupExps[v].calculate(ctx);
					}
				}

				for (int v = fcount1; v < keyCount; ++v) {
					values[v] = groupExps[v].calculate(ctx);
				}
				
				if (!isSame) {
					// 当前大分组结束，把临时分组值保存到结果中
					tempResult.sortFields(sortFields);
					tempResult.finishGather(gathers);
					result.addAll(tempResult);
					tempResult.clear();
					
					for (int h = 0, capacity = groups.length; h < capacity; ++h) {
						groups[h] = null;
					}
				}
				
				// 把当前记录累积到分组结果上
				int hash = hashUtil.hashCode(values);
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					BaseRecord r = tempResult.newLast(values);
					groups[hash].add(r);
					
					for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
						Object val = gathers[v].gather(ctx);
						r.setNormalFieldValue(f, val);
					}
				} else {
					int index = HashUtil.bsearch_r(groups[hash], values);
					if (index < 1) {
						BaseRecord r = tempResult.newLast(values);
						groups[hash].add(-index, r);
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							Object val = gathers[v].gather(ctx);
							r.setNormalFieldValue(f, val);
						}
					} else {
						BaseRecord r = (BaseRecord)groups[hash].get(index);
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							Object val = gathers[v].gather(r.getNormalFieldValue(f), ctx);
							r.setNormalFieldValue(f, val);
						}
					}
				}
			}
		} finally {
			stack.pop();
		}

		if (result.length() > 0) {
			if (eopt) {
				result = result.fieldValues(result.getFieldCount() - 1).derive("o");
			} else if (alterFields != null) {
				result.alter(alterFields, null);
			}

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
		if (iopt) {
			return groups_i(seq, ctx);
		} else if (sortExps != null) {
			if (sopt) {
				return groups_qs(seq, ctx);
			} else {
				return groups_q(seq, ctx);
			}
		} else {
			if (sopt) {
				return groups_os(seq, ctx);
			} else {
				return groups_o(seq, ctx);
			}
		}
	}
}
