package com.scudata.dm.op;

import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.GroupsSyncReader;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HashUtil;
import com.scudata.util.Variant;

/**
 * 用于对推送来的数据执行按单字段进行内存分组汇总运算
 * @author RunQian
 *
 */
public class Groups1Result extends IGroupsResult {
	private Expression gexp; // 分组字段
	private String gname; // 分组字段名
	private Expression []calcExps; // 汇总表达式
	private String []calcNames; // 汇总字段名
	private String opt; // 函数选项
	private Context ctx; // 计算上下文
	
	private HashUtil hashUtil; // 提供哈希运算的哈希类
	private ListBase1 []groups; // 保存各个分段的缓冲区，每个分段对应一个位置，通过哈希计算对应该位置
	private Node[] gathers = null; // 统计表达式中的统计函数

	private DataStruct ds; // 结果集数据结构
	private int valCount; // 汇总字段数
	
	private Table result; // 分组汇总结果
	private BaseRecord prevRecord; // 上一条结果集记录
	private SortedGroupsLink link; // 用于h选项 
	
	private boolean oOpt;
	private boolean iOpt;
	private boolean nOpt;
	private boolean hOpt;
	private boolean eOpt;

	/**
	 * 初始化参数
	 * @param gexp		分组表达式
	 * @param gname		分组表达式名
	 * @param calcExps	统计表达式
	 * @param calcNames	统计表达式名
	 * @param opt		函数选项
	 * @param ctx		上下文变量
	 */
	public Groups1Result(Expression gexp, String gname, Expression[] calcExps, 
			String[] calcNames, String opt, Context ctx) {
		this(gexp, gname, calcExps, calcNames, opt, ctx, Env.getDefaultHashCapacity());
	}
	
	/**
	 * 初始化函数
	 * @param gexp		分组表达式
	 * @param gname		分组表达式名
	 * @param calcExps	统计表达式
	 * @param calcNames	统计表达式名
	 * @param opt		函数选项
	 * @param ctx		上下文变量
	 * @param capacity	结果哈希表大小
	 */
	public Groups1Result(Expression gexp, String gname, Expression[] calcExps, 
			String[] calcNames, String opt, Context ctx, int capacity) {
		this.gexp = gexp;
		this.gname = gname;
		this.calcExps = calcExps;
		this.calcNames = calcNames;
		this.opt = opt;
		this.ctx = ctx;

		if (calcExps != null) {			
			gathers = Sequence.prepareGatherMethods(this.calcExps, ctx);
			valCount = gathers.length;
		}

		// 合并分组列名和统计列名，生成结果集的列名
		String[] colNames = new String[1 + valCount];
		colNames[0] = gname;
		
		if (calcNames != null) {
			System.arraycopy(calcNames, 0, colNames, 1, valCount);
		}

		// 生成结果集数据结构
		ds = new DataStruct(colNames);
		ds.setPrimary(new String[]{gname});

		// 设置函数选项。并根据函数选项，判断是否需要哈希表
		if (opt != null) {
			if (opt.indexOf('o') != -1) {
				oOpt = true;
			} else if (opt.indexOf('i') != -1) {
				iOpt = true;
			} else if (opt.indexOf('n') != -1) {
				nOpt = true;
			} else if (opt.indexOf('h') != -1) {
				hOpt = true;
			}
			
			if (valCount == 1 && opt.indexOf('e') != -1) eOpt = true;
		}
		
		if (hOpt) {
			link = new SortedGroupsLink();
		} else if (!oOpt && !iOpt && !nOpt) {
			hashUtil = new HashUtil(capacity);
			groups = new ListBase1[hashUtil.getCapacity()];
		}
		
		result = new Table(ds, 1024);
	}
	
	/**
	 * 取结果集数据结构
	 * @return DataStruct
	 */
	public DataStruct getResultDataStruct() {
		return ds;
	}
	
	/**
	 * 取二次汇总后用于计算最终结果的表达式，avg可能被分成sum、count两列进行计算
	 * @return
	 */
	public Expression[] getResultExpressions() {
		return null;
	}
	
	/**
	 * 取分组表达式
	 * @return 表达式数组
	 */
	public Expression[] getExps() {
		return new Expression[]{gexp};
	}

	/**
	 * 取分组字段名
	 * @return 字段名数组
	 */
	public String[] getNames() {
		return new String[]{gname};
	}

	/**
	 * 取汇总表达式
	 * @return 表达式数组
	 */
	public Expression[] getCalcExps() {
		return calcExps;
	}

	/**
	 * 取汇总字段名
	 * @return 字段名数组
	 */
	public String[] getCalcNames() {
		return calcNames;
	}

	/**
	 * 取选项
	 * @return
	 */
	public String getOption() {
		return opt;
	}
	
	/**
	 * 取是否是有序分组
	 * @return true：是，数据按分组字段有序，false：不是
	 */
	public boolean isSortedGroup() {
		return oOpt;
	}
	
	/**
	 * 取二次汇总表达式，用于多线程分组
	 * @return
	 */
	public Expression[] getRegatherExpressions() {
		if (valCount > 0) {
			Expression []valExps = new Expression[valCount];
			for (int i = 0, q = 2; i < valCount; ++i, ++q) {
				Node gather = calcExps[i].getHome();
				gather.prepare(ctx);
				valExps[i] = gather.getRegatherExpression(q);
			}
			
			return valExps;
		} else {
			return null;
		}
	}
	
	/**
	 * 取二次汇总数据结构
	 * @return DataStruct
	 */
	public DataStruct getRegatherDataStruct() {
		return ds;
	}
	
	/**
	 * 并行运算时，取出每个线程的中间计算结果，还需要进行二次汇总
	 * @return Table
	 */
	public Table getTempResult() {
		if (hashUtil != null) {
			this.hashUtil = null;
			this.groups = null;
		}  else if (nOpt) {
			result.deleteNullFieldRecord(0);
		} else if (hOpt) {
			if (opt == null || opt.indexOf('u') == -1) {
				result = link.toTable(ds);
			}
			
			link = null;
		}
		
		Table table = result;
		prevRecord = null;
		result = null;

		if (table.length() > 0) {
			if (valCount > 0) {
				table.finishGather1(gathers);
			}
			
			return table;
		} else {
			return null;
		}
	}

	/**
	 * 取分组汇总结果
	 * @return Table
	 */
	public Table getResultTable() {
		if (hashUtil != null) {	
			if (opt == null || opt.indexOf('u') == -1) {
				int []fields = new int[]{0};
				result.sortFields(fields);
			}
	
			this.hashUtil = null;
			this.groups = null;
		} else if (nOpt) {
			if (opt.indexOf('0') != -1) {
				result.deleteNullFieldRecord(0);
			} else {
				int len = result.length();
				IArray mems = result.getMems();
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)mems.get(i);
					if (r.getNormalFieldValue(0) == null) {
						r.setNormalFieldValue(0, ObjectCache.getInteger(i));;
					}
				}				
			}
		} else if (hOpt) {
			if (opt == null || opt.indexOf('u') == -1) {
				result = link.toTable(ds);
			}
			
			link = null;
		}

		Table table = result;
		prevRecord = null;
		result = null;
		
		if (table.length() > 0) {
			if (valCount > 0) {
				table.finishGather(gathers);
			}
			
			if (!nOpt && opt != null && opt.indexOf('0') != -1) {
				table.deleteNullFieldRecord(0);
			}
			
			if (eOpt) {
				table = table.fieldValues(ds.getFieldCount() - 1).derive("o");
			}
			
			//table.trimToSize();
		} else if (opt == null || opt.indexOf('t') == -1) {
			table = null;
		}
		
		return table;
	}
	
	/**
	 * 数据推送结束时调用
	 * @param ctx 计算上下文
	 */
	public void finish(Context ctx) {
	}
	
	 /**
	  * 数据推送结束，取最终的计算结果
	  * @return
	  */
	public Object result() {
		return getResultTable();
	}
	
	/**
	 * 处理推送过来的数据，累积到最终的结果上
	 * @param seq 数据
	 * @param ctx 计算上下文
	 */
	public void push(Sequence table, Context ctx) {
		if (table == null || table.length() == 0) return;
		
		if (hashUtil != null) {
			addGroups(table, ctx);
		} else if (oOpt) {
			addGroups_o(table, ctx);
		} else if (iOpt) {
			addGroups_i(table, ctx);
		} else if (nOpt) {
			addGroups_n(table, ctx);
		} else if (hOpt) {
			addGroups_h(table, ctx);
		} else {
			addGroups_1(table, ctx);
		}
	}

	/**
	 * 处理推送过来的游标数据，累积到最终的结果上
	 * @param cursor 游标数据
	 */
	public void push(ICursor cursor) {
		Context ctx = this.ctx;
		if (hashUtil != null) {
			while (true) {
				// 从游标中取得一组数据。
				Sequence src = cursor.fuzzyFetch(ICursor.FETCHCOUNT);
				if (src == null || src.length() == 0) break;
				
				// 把数据添加到分组结果类，并执行统计函数。
				addGroups(src, ctx);
			}
		} else if (oOpt) {
			while (true) {
				Sequence src = cursor.fuzzyFetch(ICursor.FETCHCOUNT);
				if (src == null || src.length() == 0) break;
				
				addGroups_o(src, ctx);
			}
		} else if (iOpt) {
			while (true) {
				Sequence src = cursor.fuzzyFetch(ICursor.FETCHCOUNT);
				if (src == null || src.length() == 0) break;
				
				addGroups_i(src, ctx);
			}
		} else if (nOpt) {
			while (true) {
				Sequence src = cursor.fuzzyFetch(ICursor.FETCHCOUNT);
				if (src == null || src.length() == 0) break;
				
				addGroups_n(src, ctx);
			}
		} else if (hOpt) {
			while (true) {
				Sequence src = cursor.fuzzyFetch(ICursor.FETCHCOUNT);
				if (src == null || src.length() == 0) break;
				
				addGroups_h(src, ctx);
			}
		} else {
			while (true) {
				Sequence src = cursor.fuzzyFetch(ICursor.FETCHCOUNT);
				if (src == null || src.length() == 0) break;
				
				addGroups_1(src, ctx);
			}
		}
	}
	
	// 哈希法分组
	private void addGroups(Sequence table, Context ctx) {
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = this.hashUtil;
		ListBase1 []groups = this.groups;
		Expression gexp = this.gexp;
		int valCount = null == gathers ? 0 : gathers.length;

		Node []gathers = this.gathers;
		Table result = this.result;
		Object key;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(table);
		stack.push(current);

		try {
			for (int i = 1, len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				key = gexp.calculate(ctx);

				BaseRecord r;
				int hash = hashUtil.hashCode(key);
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					r = result.newLast();
					r.setNormalFieldValue(0, key);
					groups[hash].add(r);
					for (int v = 0; v < valCount; ++v) {
						Object val = gathers[v].gather(ctx);
						r.setNormalFieldValue(v + 1, val);
					}
				} else {
					int index = HashUtil.bsearch_r(groups[hash], key);
					if (index < 1) {
						r = result.newLast();
						r.setNormalFieldValue(0, key);
						groups[hash].add(-index, r);
						for (int v = 0; v < valCount; ++v) {
							Object val = gathers[v].gather(ctx);
							r.setNormalFieldValue(v + 1, val);
						}
					} else {
						r = (BaseRecord)groups[hash].get(index);
						for (int f = 1; f <= valCount; ++f) {
							Object val = gathers[f - 1].gather(r.getNormalFieldValue(f), ctx);
							r.setNormalFieldValue(f, val);
						}
					}
				}
			}
		} finally {
			stack.pop();
		}
	}

	private void addGroups_1(Sequence table, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(table);
		stack.push(current);
		int i = 1;
		
		BaseRecord r = prevRecord;
		int valCount = this.valCount;
		Node []gathers = this.gathers;

		try {
			if (r == null) {
				r = prevRecord = result.newLast();
				current.setCurrent(1);
				i++;
				
				for (int v = 0; v < valCount; ++v) {
					Object val = gathers[v].gather(ctx);
					r.setNormalFieldValue(v, val);
				}
			}
			
			for (int len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				for (int v = 0; v < valCount; ++v) {
					Object val = gathers[v].gather(r.getNormalFieldValue(v), ctx);
					r.setNormalFieldValue(v, val);
				}
			}
		} finally {
			stack.pop();
		}
	}
	
	private void addGroups_o(Sequence table, Context ctx) {
		Table result = this.result;
		BaseRecord r = prevRecord;
		Expression gexp = this.gexp;
		int valCount = this.valCount;
		Node []gathers = this.gathers;
		
		Object key;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(table);
		stack.push(current);

		try {
			for (int i = 1, len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				key = gexp.calculate(ctx);

				if (r == null || Variant.compare(r.getNormalFieldValue(0), key, true) != 0) {
					r = result.newLast();
					r.setNormalFieldValue(0, key);

					for (int v = 0; v < valCount; ++v) {
						Object val = gathers[v].gather(ctx);
						r.setNormalFieldValue(v + 1, val);
					}
				} else {
					for (int f = 1; f <= valCount; ++f) {
						Object val = gathers[f - 1].gather(r.getNormalFieldValue(f), ctx);
						r.setNormalFieldValue(f, val);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		prevRecord = r;
	}
	
	private void addGroups_i(Sequence table, Context ctx) {
		Table result = this.result;
		BaseRecord r = prevRecord;
		Expression gexp = this.gexp;
		int valCount = this.valCount;
		Node []gathers = this.gathers;

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(table);
		stack.push(current);

		try {
			for (int i = 1, len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				Object val = gexp.calculate(ctx);

				if (Variant.isTrue(val) || r == null) {
					r = result.newLast();
					r.setNormalFieldValue(0, val);
					
					for (int v = 0; v < valCount; ++v) {
						val = gathers[v].gather(ctx);
						r.setNormalFieldValue(v + 1, val);
					}
				} else {
					for (int f = 1; f <= valCount; ++f) {
						val = gathers[f - 1].gather(r.getNormalFieldValue(f), ctx);
						r.setNormalFieldValue(f, val);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		prevRecord = r;
	}

	/**
	 * 设置分组数，@n选项使用
	 * @param groupCount
	 */
	public void setGroupCount(int groupCount) {
		result.insert(groupCount);
	}

	private void addGroups_n(Sequence table, Context ctx) {
		Table result = this.result;
		Expression gexp = this.gexp;
		int valCount = this.valCount;
		Node []gathers = this.gathers;

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(table);
		stack.push(current);

		try {
			for (int i = 1, len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				Object obj = gexp.calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("groups: " + mm.getMessage("engine.needIntExp"));
				}

				int index = ((Number)obj).intValue();
				if (index < 1) {
					// 碰到小于1的放过不要了，不再报错，不分到任何一组里
					continue;
				}

				BaseRecord r = result.getRecord(index);
				if (r.getNormalFieldValue(0) == null) {
					r.setNormalFieldValue(0, obj);
					for (int v = 0; v < valCount; ++v) {
						Object val = gathers[v].gather(ctx);
						r.setNormalFieldValue(v + 1, val);
					}
				} else {
					for (int f = 1; f <= valCount; ++f) {
						Object val = gathers[f - 1].gather(r.getNormalFieldValue(f), ctx);
						r.setNormalFieldValue(f, val);
					}
				}
			}
		} finally {
			stack.pop();
		}
	}
	
	private void addGroups_h(Sequence table, Context ctx) {
		Expression gexp = this.gexp;
		int valCount = this.valCount;
		Node []gathers = this.gathers;
		Table result = this.result;
		SortedGroupsLink link = this.link;
		
		Object key;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(table);
		stack.push(current);

		try {
			for (int i = 1, len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				key = gexp.calculate(ctx);

				SortedGroupsLink.Node node = link.put(key);
				BaseRecord r = node.getRecord();
				if (r == null) {
					r = result.newLast();
					r.setNormalFieldValue(0, key);
					node.setReocrd(r);
					
					for (int v = 0; v < valCount; ++v) {
						Object val = gathers[v].gather(ctx);
						r.setNormalFieldValue(v + 1, val);
					}
				} else {
					for (int f = 1; f <= valCount; ++f) {
						Object val = gathers[f - 1].gather(r.getNormalFieldValue(f), ctx);
						r.setNormalFieldValue(f, val);
					}
				}
			}
		} finally {
			stack.pop();
		}
	}
	
	/**
	 * 多路运算时对把所有路的运算结果合并进行二次分组汇总，得到最终的汇总结果
	 * @param results 所有路的分组结果构成的数组
	 * @return 最终的汇总结果
	 */
	public Object combineResult(Object []results) {
		int count = results.length;
		Sequence result = new Sequence();
		for (int i = 0; i < count; ++i) {
			if (results[i] instanceof Sequence) {
				result.addAll((Sequence)results[i]);
			}
		}
		
		// 各路游标按分组字段拆分的
		if (opt != null && opt.indexOf('o') != -1) {
			return result.derive("o");
		}
		
		int mcount = calcExps == null ? 0 : calcExps.length;
		Expression []exps2 = new Expression[1];
		exps2[0] = new Expression(ctx, "#1");

		Expression []calcExps2 = null;
		if (mcount > 0) {
			calcExps2 = new Expression[mcount];
			for (int i = 0, q = 2; i < mcount; ++i, ++q) {
				Gather gather = (Gather)calcExps[i].getHome();
				gather.prepare(ctx);
				calcExps2[i] = gather.getRegatherExpression(q);
			}
		}

		return result.groups(exps2, new String[]{gname}, calcExps2, calcNames, opt, ctx);
	}
	
	//用于实现@z
	public void push(GroupsSyncReader reader, int hashStart, int hashEnd) {
		Context ctx = this.ctx;
		int index = 1;
		while (true) {
			Object[] objs = reader.getData(index++);
			if (objs == null) {
				break;
			}
			addGroups_z((Sequence)objs[0], objs[1], (IntArray)objs[2], hashStart, hashEnd, ctx);
		}
	}
	
	//用于实现@z
	public void push(Sequence table, Object key, IntArray hashValue, int hashStart, int hashEnd) {
		addGroups_z(table, key, hashStart, hashEnd, ctx);
	}
	
	// 哈希法分组
	private void addGroups_z(Sequence table, Object obj, IntArray hashValue, int hashStart, int hashEnd, Context ctx) {
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		//HashUtil hashUtil = (HashUtil) obj;
		ObjectArray keyArray = (ObjectArray) obj;
		ListBase1 []groups = this.groups;
		//Expression gexp = this.gexp;
		int valCount = null == gathers ? 0 : gathers.length;

		Node []gathers = this.gathers;
		Table result = this.result;
		Object key;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(table);
		stack.push(current);

		try {
			for (int i = 1, len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				//key = gexp.calculate(ctx);
				key = keyArray.get(i);
				int hash = hashValue.getInt(i);

				BaseRecord r;
				if ((hash % hashEnd) != hashStart) continue;
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					r = result.newLast();
					r.setNormalFieldValue(0, key);
					groups[hash].add(r);
					for (int v = 0; v < valCount; ++v) {
						Object val = gathers[v].gather(ctx);
						r.setNormalFieldValue(v + 1, val);
					}
				} else {
					int index = HashUtil.bsearch_r(groups[hash], key);
					if (index < 1) {
						r = result.newLast();
						r.setNormalFieldValue(0, key);
						groups[hash].add(-index, r);
						for (int v = 0; v < valCount; ++v) {
							Object val = gathers[v].gather(ctx);
							r.setNormalFieldValue(v + 1, val);
						}
					} else {
						r = (BaseRecord)groups[hash].get(index);
						for (int f = 1; f <= valCount; ++f) {
							Object val = gathers[f - 1].gather(r.getNormalFieldValue(f), ctx);
							r.setNormalFieldValue(f, val);
						}
					}
				}
			}
		} finally {
			stack.pop();
		}
	}
		
	// 哈希法分组
	private void addGroups_z(Sequence table, Object obj, int hashStart, int hashEnd, Context ctx) {
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = (HashUtil) obj;
		ListBase1 []groups = this.groups;
		Expression gexp = this.gexp;
		int valCount = null == gathers ? 0 : gathers.length;

		Node []gathers = this.gathers;
		Table result = this.result;
		Object key;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(table);
		stack.push(current);

		try {
			for (int i = 1, len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				key = gexp.calculate(ctx);

				BaseRecord r;
				int hash = hashUtil.hashCode(key);
				if ((hash % hashEnd) != hashStart) continue;
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					r = result.newLast();
					r.setNormalFieldValue(0, key);
					groups[hash].add(r);
					for (int v = 0; v < valCount; ++v) {
						Object val = gathers[v].gather(ctx);
						r.setNormalFieldValue(v + 1, val);
					}
				} else {
					int index = HashUtil.bsearch_r(groups[hash], key);
					if (index < 1) {
						r = result.newLast();
						r.setNormalFieldValue(0, key);
						groups[hash].add(-index, r);
						for (int v = 0; v < valCount; ++v) {
							Object val = gathers[v].gather(ctx);
							r.setNormalFieldValue(v + 1, val);
						}
					} else {
						r = (BaseRecord)groups[hash].get(index);
						for (int f = 1; f <= valCount; ++f) {
							Object val = gathers[f - 1].gather(r.getNormalFieldValue(f), ctx);
							r.setNormalFieldValue(f, val);
						}
					}
				}
			}
		} finally {
			stack.pop();
		}
	}
}
