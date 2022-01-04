package com.scudata.parallel;

import java.util.HashMap;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.Record;
import com.scudata.dm.ResourceManager;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.GroupmCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.IMultipath;
import com.scudata.dm.cursor.MergesCursor;
import com.scudata.dm.cursor.PJoinCursor;
import com.scudata.dm.op.Operable;
import com.scudata.dm.op.Operation;
import com.scudata.dm.op.TotalResult;
import com.scudata.dw.Cursor;
import com.scudata.dw.MemoryTable;
import com.scudata.dw.TableMetaData;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.FunctionLib;
import com.scudata.expression.Gather;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.ThreadPool;
import com.scudata.util.CursorUtil;

/**
 * 集群游标
 * @author RunQian
 *
 */
public class ClusterCursor extends ICursor implements IClusterObject, IMultipath {
	// 游标的来源，即由什么创建，可能是ClusterTableMetaData、ClusterMemoryTable或其它中间结果
	private IClusterObject source; 
	private Cluster cluster; // 节点机信息
	private int []cursorProxyIds; // 对应的节点机游标代理标识
	private boolean isDistributed; // 是否分布游标，分布文件或者分段则为分布游标
	//private boolean isDistributedFile; // 此集群游标是分布游标还是复写游标
	private int current = 0; // 当前正在取数的游标

	private Expression distribute; // 分布表达式，可以为空
	private String []sortedColNames; // 主键或排序字段
	
	/**
	 * 创建集群游标
	 * @param source 来源，可能是ClusterTableMetaData、ClusterMemoryTable或其它中间结果
	 * @param cursorProxyIds 对应的节点机游标proxy id
	 * @param isDistributed 是否分布游标
	 */
	public ClusterCursor(IClusterObject source, int []cursorProxyIds, boolean isDistributed) {
		this.source = source;
		this.cluster = source.getCluster();
		this.cursorProxyIds = cursorProxyIds;
		this.isDistributed = isDistributed;
		//this.isDistributedFile = isDistributedFile;
	}
	
	/**
	 * 创建集群游标
	 * @param cluster 节点机信息
	 * @param cursorProxyIds 对应的节点机游标proxy id
	 * @param isDistributed 是否分布游标
	 */
	public ClusterCursor(Cluster cluster, int []cursorProxyIds, boolean isDistributed) {
		this.cluster = cluster;
		this.cursorProxyIds = cursorProxyIds;
		this.isDistributed = isDistributed;
	}
	
	/**
	 * 返回是否是分布游标
	 * @return true：是，false：不是
	 */
	public boolean isDistributed() {
		return isDistributed;
	}
	
	public IClusterObject getSource() {
		return source;
	}
	
	public Cluster getCluster() {
		return cluster;
	}

	public int[] getCursorProxyIds() {
		return cursorProxyIds;
	}
	
	public int getCursorProxyId(int unit) {
		return cursorProxyIds[unit];
	}
		
	// 是否是分布游标
	//public boolean isDistributedFile() {
	//	return isDistributedFile;
	//}
	
	public Expression getDistribute() {
		return distribute;
	}

	/**
	 * 设置组表分布表达式
	 * @param distribute 分布表达式
	 */
	public void setDistribute(Expression distribute) {
		this.distribute = distribute;
	}
	
	/**
	 * 取排序字段名
	 * @return 字段名数组
	 */
	public String[] getSortedColNames() {
		return sortedColNames;
	}

	/**
	 * 设置组表排序字段名
	 * @param sortedColNames 字段名数组
	 */
	public void setSortedColNames(String[] sortedColNames) {
		this.sortedColNames = sortedColNames;
	}

	private Sequence fetch(int current, int n) {
		Cluster cluster = getCluster();
		UnitClient client = new UnitClient(cluster.getHost(current), cluster.getPort(current));
		
		try {
			UnitCommand command = new UnitCommand(UnitCommand.CURSOR_FETCH);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[current]));
			command.setAttribute("count", new Integer(n));
			
			Response response = client.send(command);
			return (Sequence)response.checkResult();
		} finally {
			client.close();
		}
	}
	
	/**
	 * 节点机上执行游标取数
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeFetch(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer cursorProxyId = (Integer)attributes.get("cursorProxyId");
		Integer count = (Integer)attributes.get("count");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyId.intValue());
			if (cursor.getCursor() == null) 
				return new Response();
			Sequence result = cursor.getCursor().fetch(count.intValue());
			return new Response(result);
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}

	private long skip(int cursorProxyId, long n) {
		Cluster cluster = getCluster();
		UnitClient client = new UnitClient(cluster.getHost(current), cluster.getPort(current));
		
		try {
			UnitCommand command = new UnitCommand(UnitCommand.CURSOR_SKIP);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[current]));
			command.setAttribute("count", new Long(n));
			
			Response response = client.send(command);
			Long result = (Long)response.checkResult();
			return result.longValue();
		} finally {
			client.close();
		}
	}
	
	/**
	 * 节点机上执行跳过游标数据
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeSkip(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer cursorProxyId = (Integer)attributes.get("cursorProxyId");
		Long count = (Long)attributes.get("count");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyId.intValue());
			long result = cursor.getCursor().skip(count.intValue());
			return new Response(new Long(result));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}

	/**
	 * 取数
	 * @param n 数量
	 */
	protected Sequence get(int n) {
		if (current == -1) {
			return null;
		}
		
		Sequence result = null;
		if (n == MAXSIZE) { // 取出所有
			Cluster cluster = getCluster();
			int count = cluster.getUnitCount();
			int len = count - current;
			UnitJob []jobs = new UnitJob[len];
			ThreadPool pool = TaskManager.getPool();
			
			for (int i = current, j = 0; i < count; ++i, ++j) {
				UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
				UnitCommand command = new UnitCommand(UnitCommand.CURSOR_FETCH);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[i]));
				command.setAttribute("count", new Integer(n));
				jobs[j] = new UnitJob(client, command);
				pool.submit(jobs[j]);
			}
			
			for (int i = 0; i < len; ++i) {
				jobs[i].join();
				Sequence cur = (Sequence)jobs[i].getResult();
				if (result == null) {
					result = cur;
				} else {
					result = append(result, cur);
				}
			}
		} else {
			int size = 0;
			while (result == null || size < n) {
				Sequence cur = fetch(current, n - size);
				if (cur == null || cur.length() == 0) {
					if (++current == cursorProxyIds.length) {
						current = -1;
						break;
					}
				} else {
					if (result == null) {
						result = cur;
					} else {
						result = append(result, cur);
					}
					
					size += cur.length();
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 跳过指定行
	 * @param n 行数
	 */
	protected long skipOver(long n) {
		if (current == -1) {
			return 0;
		}
		
		long size = 0;
		while (size < n) {
			long cur = skip(current, n - size);
			if (cur == 0) {
				if (++current == cursorProxyIds.length) {
					current = -1;
					break;
				}
			} else {
				size += cur;
			}
		}
		
		return size;
	}
	
	public void close() {
		super.close();
		
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.CURSOR_CLOSE);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[i]));				
 				Response response = client.send(command);
				response.checkResult();
			} finally {
				client.close();
			}
		}
	}
	
	/**
	 * 节点机上执行关闭游标
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeClose(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer cursorProxyId = (Integer)attributes.get("cursorProxyId");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyId.intValue());
			cursor.close();
			return new Response();
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public Operable addOperation(Operation op, Context ctx) {
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		Function function = op.getFunction();
		String functionName = function.getFunctionName();
		String option = function.getOption();
		String param = function.getParamString();
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.CURSOR_ADD_OPERATION);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[i]));
				//command.setAttribute("unit", new Integer(i));
				command.setAttribute("functionName", functionName);
				command.setAttribute("option", option);
				command.setAttribute("param", param);
				ClusterUtil.setParams(command, function, ctx);
				
 				Response response = client.send(command);
				response.checkResult();
			} finally {
				client.close();
			}
		}
		
		return this;
	}
	
	/**
	 * 节点机上执行给游标附加运算
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeAddOperation(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer cursorProxyId = (Integer)attributes.get("cursorProxyId");
		//Integer unit = (Integer)attributes.get("unit");
		String functionName = (String)attributes.get("functionName");
		String option = (String)attributes.get("option");
		String param = (String)attributes.get("param");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			Context ctx = ClusterUtil.createContext(js, attributes, functionName, option);
			
			ResourceManager rm = js.getResourceManager();
			CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyId.intValue());
			FunctionLib.executeMemberFunction(cursor.getCursor(), functionName, param, option, ctx);
			return new Response();
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	/**
	 * 对集群游标做分组运算
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @param groupCount 结果集数量
	 * @param newNames 对分组结果产生新序表的字段名数组
	 * @param newExps 对分组结果产生新序表的表达式数组
	 * @return 分组结果
	 */
	public Object groups(Expression[] exps, String[] names, Expression[] calcExps, String[] calcNames, 
			String opt, Context ctx, int groupCount, String[] newNames, Expression[] newExps) {
		// @c：分机独立执行，结果集拼成集群内表，保持cs分布，可用作集群维表
		boolean copt = opt != null && opt.indexOf('c') != -1;
		
		int dcount = 0;
		int mcount = 0;
		if (exps != null) {
			// 如果省略了分组字段名则根据分组表达式自动生成
			dcount = exps.length;
			if (names == null) {
				names = new String[dcount];
			}
			
			for (int i = 0; i < dcount; ++i) {
				if (names[i] == null || names[i].length() == 0) {
					names[i] = exps[i].getFieldName();
				}
			}
		}

		if (calcExps != null) {			
			// 如果省略了汇总字段名则根据汇总表达式自动生成
			mcount = calcExps.length;
			if (calcNames == null) {
				calcNames = new String[mcount];
			}
			
			for (int i = 0; i < mcount; ++i) {
				if (calcNames[i] == null || calcNames[i].length() == 0) {
					calcNames[i] = calcExps[i].getFieldName();
				}
			}
		}
		
		String []dexps = null;
		String []mexps = null;
		Expression[] totalExps = new Expression[dcount + mcount];
		
		if (dcount > 0) {
			dexps = new String[dcount];
			for (int i = 0; i < dcount; ++i) {
				dexps[i] = exps[i].toString();
				totalExps[i] = exps[i];
			}
		}
		
		if (mcount > 0) {
			mexps = new String[mcount];
			for (int i = 0; i < mcount; ++i) {
				mexps[i] = calcExps[i].toString();
				totalExps[i + dcount] = calcExps[i];
			}
		}
		
		// @c选项时在节点机生产最终结果，返回集群内表
		String []newExpStrs = null;
		if (copt && newNames != null) {
			int count = newExps.length;
			newExpStrs = new String[count];
			
			for (int i = 0; i < count; ++i) {
				newExpStrs[i] = newExps[i].toString();
			}
		}
		
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		UnitJob []jobs = new UnitJob[count];
		ThreadPool pool = TaskManager.getPool();
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			UnitCommand command = new UnitCommand(UnitCommand.GROUPS);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[i]));
			
			command.setAttribute("dexps", dexps);
			command.setAttribute("names", names);
			command.setAttribute("mexps", mexps);
			command.setAttribute("calcNames", calcNames);
			command.setAttribute("option", opt);
			command.setAttribute("groupCount", groupCount);
			
			if (copt) {
				command.setAttribute("newNames", newNames);
				command.setAttribute("newExpStrs", newExpStrs);
			}
			
			ClusterUtil.setParams(command, totalExps, ctx);
			jobs[i] = new UnitJob(client, command);
			pool.submit(jobs[i]);
		}
		
		// @c：分机独立执行，结果集拼成集群内表，保持cs分布，可用作集群维表
		if (copt) {
			RemoteMemoryTable []tables = new RemoteMemoryTable[count];
			for (int i = 0; i < count; ++i) {
				// 等待任务执行完毕
				jobs[i].join();
				tables[i] = (RemoteMemoryTable)jobs[i].getResult();
			}
			
			return new ClusterMemoryTable(cluster, tables, true);
		}
		
		Sequence result = new Sequence();
		for (int i = 0; i < count; ++i) {
			// 等待任务执行完毕
			jobs[i].join();
			result.addAll((Sequence)jobs[i].getResult());
		}
		
		// 各路游标按分组字段拆分的
		if (opt != null && opt.indexOf('o') != -1) {
			if (newNames != null) {
				int groupFieldCount = 0;
				if (opt == null || opt.indexOf('b') == -1) {
					groupFieldCount = dcount;
				}
				
				return newGroupsResult(result, newNames, newExps, ctx, groupFieldCount);
			} else {
				return result.derive("o");
			}
		}
		
		Expression []exps2 = null;
		if (dcount > 0) {
			exps2 = new Expression[dcount];
			for (int i = 0, q = 1; i < dcount; ++i, ++q) {
				exps2[i] = new Expression(ctx, "#" + q);
			}
		}

		Expression []calcExps2 = null;
		if (mcount > 0) {
			calcExps2 = new Expression[mcount];
			for (int i = 0, q = dcount + 1; i < mcount; ++i, ++q) {
				Gather gather = (Gather)calcExps[i].getHome();
				gather.prepare(ctx);
				calcExps2[i] = gather.getRegatherExpression(q);
			}
		}

		Table table = result.groups(exps2, names, calcExps2, calcNames, opt, ctx);
		if (newNames != null) {
			int groupFieldCount = 0;
			if (opt == null || opt.indexOf('b') == -1) {
				groupFieldCount = dcount;
			}
			
			table = newGroupsResult(table, newNames, newExps, ctx, groupFieldCount);
		}
		
		if (opt == null || opt.indexOf('d') == -1) {
			return table;
		} else {
			// @d 结果集复制到分机上拼成集群复写内表
			return ClusterMemoryTable.dupLocal(cluster, table);
		}
	}	

	/**
	 * 当汇总字段不是单个汇总函数时，汇总字段会被拆成多个字段，最后需要对着分组结果再new一下
	 * @param result
	 * @param newNames
	 * @param newExps
	 * @param ctx
	 * @param groupFieldCount 分组字段数
	 * @return
	 */
	private static Table newGroupsResult(Sequence result, String []newNames, Expression []newExps, Context ctx, int groupFieldCount) {
		Table table = result.newTable(newNames, newExps, ctx);
		
		// 如果有分组字段则设置分组字段为主键
		if (groupFieldCount > 0) {
			String []pk = new String[groupFieldCount];
			for (int i = 1; i <= groupFieldCount; ++i) {
				pk[i - 1] = "#" + i;
			}
			
			table.setPrimary(pk);
		}
		
		return table;
	}
	
	/**
	 * 节点机上执行游标分组汇总运算
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeGroups(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer cursorProxyId = (Integer)attributes.get("cursorProxyId");
		String []dexps = (String[])attributes.get("dexps");
		String []names = (String[])attributes.get("names");
		String []mexps = (String[])attributes.get("mexps");
		String []calcNames = (String[])attributes.get("calcNames");
		String opt = (String)attributes.get("option");
		Integer groupCount = (Integer)attributes.get("groupCount");

		JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
		Context ctx = ClusterUtil.createContext(js, attributes);
		
		// @c：分机独立执行，结果集拼成集群内表，保持cs分布，可用作集群维表
		boolean copt = opt != null && opt.indexOf('c') != -1;
		String []newNames = null;
		String []newExpStrs = null;
		Expression[] newExps = null;
		if (copt) {
			newNames = (String[])attributes.get("newNames");
			newExpStrs = (String[])attributes.get("newExpStrs");
			
			if (newExpStrs != null) {
				int count = newExpStrs.length;
				newExps = new Expression[count];
				for (int i = 0; i < count; ++i) {
					newExps[i] = new Expression(ctx, newExpStrs[i]);
				}
			}
		}
		
		Expression[] exps = null;
		Expression[] calcExps = null;
		if (dexps != null) {
			int dcount = dexps.length;
			exps = new Expression[dcount];
			for (int i = 0; i < dcount; ++i) {
				exps[i] = new Expression(ctx, dexps[i]);
			}
		}
		
		if (mexps != null) {
			int mcount = mexps.length;
			calcExps = new Expression[mcount];
			for (int i = 0; i < mcount; ++i) {
				calcExps[i] = new Expression(ctx, mexps[i]);
			}
		}
		
		try {
			ResourceManager rm = js.getResourceManager();
			CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyId.intValue());
			ICursor cs = cursor.getCursor();
			Table result = null;
			if (cs != null) {
				result = cs.groups(exps, names, calcExps, calcNames, opt, ctx, groupCount);
			}
			
			if (copt) {
				// @c：分机独立执行，结果集拼成集群内表，保持cs分布，可用作集群维表
				if (newNames != null) {
					int groupFieldCount = 0;
					if ((opt == null || opt.indexOf('b') == -1) && exps != null) {
						groupFieldCount = exps.length;
					}
					
					result = newGroupsResult(result, newNames, newExps, ctx, groupFieldCount);
				}
				
				result = new MemoryTable(result);
				IProxy proxy = new TableProxy(result, cursor.getUnit());
				rm.addProxy(proxy);
				
				RemoteMemoryTable rmt = ClusterMemoryTable.newRemoteMemoryTable(proxy.getProxyId(), result);
				return new Response(rmt);
			} else {
				return new Response(result);
			}
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	/**
	 * 对游标进行汇总
	 * @param calcExps 汇总表达式数组
	 * @param ctx 计算上下文
	 * @return 如果只有一个汇总表达式返回汇总结果，否则返回汇总结果构成的序列
	 */
	public Object total(Expression[] calcExps, Context ctx) {
		int valCount = calcExps.length;
		String []expStrs = new String[valCount];
		for (int i = 0; i < valCount; ++i) {
			expStrs[i] = calcExps[i].toString();
		}
		
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		UnitJob []jobs = new UnitJob[count];
		ThreadPool pool = TaskManager.getPool();
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			UnitCommand command = new UnitCommand(UnitCommand.TOTAL);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[i]));
			
			command.setAttribute("expStrs", expStrs);
			ClusterUtil.setParams(command, calcExps, ctx);
			jobs[i] = new UnitJob(client, command);
			pool.submit(jobs[i]);
		}
		
		Table result;
		if (valCount == 1) {
			String []fnames = new String[]{"_1"};
			result = new Table(fnames, count);
			
			for (int i = 0; i < count; ++i) {
				// 等待任务执行完毕
				jobs[i].join();
				Record r = result.newLast();
				r.setNormalFieldValue(0, jobs[i].getResult());
			}
		} else {
			String []fnames = new String[valCount];
			for (int i = 1; i < valCount; ++i) {
				fnames[i - 1] = "_" + i;
			}
			
			result = new Table(fnames, count);
			for (int i = 0; i < count; ++i) {
				// 等待任务执行完毕
				jobs[i].join();
				Sequence seq = (Sequence)jobs[i].getResult();
				result.newLast(seq.toArray());
			}
		}
		
		// 生成二次汇总表达式
		Expression []valExps = new Expression[valCount];
		for (int i = 0; i < valCount; ++i) {
			Node gather = calcExps[i].getHome();
			gather.prepare(ctx);
			valExps[i] = gather.getRegatherExpression(i + 1);
		}
		
		// 进行二次汇总
		TotalResult total = new TotalResult(valExps, ctx);
		total.push(result, ctx);
		return total.result();
	}
	
	/**
	 * 节点机上执行游标汇总运算
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeTotal(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer cursorProxyId = (Integer)attributes.get("cursorProxyId");
		String []expStrs = (String[])attributes.get("expStrs");

		JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
		Context ctx = ClusterUtil.createContext(js, attributes);
		
		Expression[] calcExps = null;
		int valCount = expStrs.length;
		calcExps = new Expression[valCount];
		for (int i = 0; i < valCount; ++i) {
			calcExps[i] = new Expression(ctx, expStrs[i]);
		}
		
		try {
			ResourceManager rm = js.getResourceManager();
			CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyId.intValue());
			ICursor cs = cursor.getCursor();
			Object result = null;
			if (cs != null) {
				result = cs.total(calcExps, ctx);
			}
			
			return new Response(result);
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	/**
	 * 把集群游标读成集群内表
	 * @param fields 要读取的字段
	 * @param ctx 计算上下文
	 * @return 集群内表
	 */
	public ClusterMemoryTable memory(String []fields, Context ctx) {
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		UnitJob []jobs = new UnitJob[count];
		ThreadPool pool = TaskManager.getPool();
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			UnitCommand command = new UnitCommand(UnitCommand.MEMORY_CLUSTERCURSOR);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[i]));
			command.setAttribute("fields", fields);
			
			jobs[i] = new UnitJob(client, command);
			pool.submit(jobs[i]);
		}
		
		RemoteMemoryTable[] tables = new RemoteMemoryTable[count];
		for (int i = 0; i < count; ++i) {
			// 等待任务执行完毕
			jobs[i].join();
			tables[i] = (RemoteMemoryTable)jobs[i].getResult();
		}
		
		ClusterMemoryTable result = new ClusterMemoryTable(getCluster(), tables, isDistributed);
		result.setDistribute(distribute);
		result.setSortedColNames(sortedColNames);
		return result;
	}
	
	/**
	 * 节点机上执行把游标数据变成内表
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeMemory(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer cursorProxyId = (Integer)attributes.get("cursorProxyId");
		String []fields = (String[])attributes.get("fields");
		
		JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
				
		try {
			ResourceManager rm = js.getResourceManager();
			CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyId.intValue());
			ICursor cs = cursor.getCursor();

			Sequence seq = cs.fetch();
			Table table;
			if (seq instanceof Table) {
				table = (Table)seq;
			} else {
				table = seq.derive("o");
			}
			
			MemoryTable memoryTable = new MemoryTable(table);
			if (fields != null) {
				memoryTable.setPrimary(fields);
			}
			
			TableMetaData tmd = CursorUtil.getTableMetaData(cs);
			if (tmd != null) {
				String distribute = tmd.getDistribute();
				Integer partition = tmd.getGroupTable().getPartition();
				if (partition != null) {
					memoryTable.setDistribute(distribute);
					memoryTable.setPart(partition);
				}
			}

			IProxy proxy = new TableProxy(memoryTable, cursor.getUnit());
			rm.addProxy(proxy);
			
			RemoteMemoryTable rmt = ClusterMemoryTable.newRemoteMemoryTable(proxy.getProxyId(), memoryTable);
			return new Response(rmt);
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	/**
	 * 对集群游标做外存分组运算
	 * @param exps 分组表达式数组
	 * @param names	分组字段名数组
	 * @param calcExps 汇总表达式数组
	 * @param calcNames	汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @param capacity 内存能够存放的分组结果的数量
	 * @return 分组结果游标
	 */
	public ICursor groupx(Expression[] exps, String[] names,
			Expression[] calcExps, String[] calcNames, String opt, Context ctx, int capacity) {
		int dcount = 0;
		int mcount = 0;
		
		if (exps != null) {
			// 如果省略了分组字段名则根据分组表达式自动生成
			dcount = exps.length;
			if (names == null) {
				names = new String[dcount];
			}
			
			for (int i = 0; i < dcount; ++i) {
				if (names[i] == null || names[i].length() == 0) {
					names[i] = exps[i].getFieldName();
				}
			}
		}

		if (calcExps != null) {
			// 如果省略了汇总字段名则根据汇总表达式自动生成
			mcount = calcExps.length;
			if (calcNames == null) {
				calcNames = new String[mcount];
			}
			
			for (int i = 0; i < mcount; ++i) {
				if (calcNames[i] == null || calcNames[i].length() == 0) {
					calcNames[i] = calcExps[i].getFieldName();
				}
			}
		}

		String []dexps = null;
		String []mexps = null;
		Expression[] totalExps = new Expression[dcount + mcount];
		
		if (dcount > 0) {
			dexps = new String[dcount];
			for (int i = 0; i < dcount; ++i) {
				dexps[i] = exps[i].toString();
				totalExps[i] = exps[i];
			}
		}
		
		if (mcount > 0) {
			mexps = new String[mcount];
			for (int i = 0; i < mcount; ++i) {
				mexps[i] = calcExps[i].toString();
				totalExps[i + dcount] = calcExps[i];
			}
		}
				
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		UnitJob []jobs = new UnitJob[count];
		ThreadPool pool = TaskManager.getPool();
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			UnitCommand command = new UnitCommand(UnitCommand.GROUPX);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[i]));
			
			command.setAttribute("dexps", dexps);
			command.setAttribute("names", names);
			command.setAttribute("mexps", mexps);
			command.setAttribute("calcNames", calcNames);
			command.setAttribute("option", opt);
			command.setAttribute("capacity", new Integer(capacity));
			
			ClusterUtil.setParams(command, totalExps, ctx);
			jobs[i] = new UnitJob(client, command);
			pool.submit(jobs[i]);
		}
		
		// @c：分机独立执行，结果集拼成集群内表，保持cs分布，可用作集群维表
		if (opt != null && opt.indexOf('c') != -1) {
			int[] cursorProxyIds = new int[count];
			for (int i = 0; i < count; ++i) {
				// 等待任务执行完毕
				jobs[i].join();
				Integer id = (Integer)jobs[i].getResult();
				cursorProxyIds[i] = id.intValue();
			}
			
			return new ClusterCursor(this, cursorProxyIds, true);
		}
		
		ICursor []cursors = new ICursor[count];
		for (int i = 0; i < count; ++i) {
			// 等待任务执行完毕
			jobs[i].join();
			Integer id = (Integer)jobs[i].getResult();
			cursors[i] = new RemoteCursor(cluster.getHost(i), cluster.getPort(i), id.intValue());//, ctx);
		}
		
		Expression []exps2 = null;
		if (dcount > 0) {
			exps2 = new Expression[dcount];
			for (int i = 0, q = 1; i < dcount; ++i, ++q) {
				exps2[i] = new Expression(ctx, "#" + q);
			}
		}

		Expression []calcExps2 = null;
		if (mcount > 0) {
			calcExps2 = new Expression[mcount];
			for (int i = 0, q = dcount + 1; i < mcount; ++i, ++q) {
				Gather gather = (Gather)calcExps[i].getHome();
				gather.prepare(ctx);
				calcExps2[i] = gather.getRegatherExpression(q);
			}
		}

		// 对结果集进行二次分组
		MergesCursor mc = new MergesCursor(cursors, exps2, ctx);
		return new GroupmCursor(mc, exps2, names, calcExps2, calcNames, ctx);
	}

	/**
	 * 节点机上执行游标外存分组运算
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeGroupx(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer cursorProxyId = (Integer)attributes.get("cursorProxyId");
		String []dexps = (String[])attributes.get("dexps");
		String []names = (String[])attributes.get("names");
		String []mexps = (String[])attributes.get("mexps");
		String []calcNames = (String[])attributes.get("calcNames");
		String opt = (String)attributes.get("option");
		Integer capacity = (Integer)attributes.get("capacity");
		Expression[] exps = null;
		Expression[] calcExps = null;
		
		JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
		Context ctx = ClusterUtil.createContext(js, attributes);
		
		if (dexps != null) {
			int dcount = dexps.length;
			exps = new Expression[dcount];
			for (int i = 0; i < dcount; ++i) {
				exps[i] = new Expression(ctx, dexps[i]);
			}
		}
		
		if (mexps != null) {
			int mcount = mexps.length;
			calcExps = new Expression[mcount];
			for (int i = 0; i < mcount; ++i) {
				calcExps[i] = new Expression(ctx, mexps[i]);
			}
		}
		
		try {
			ResourceManager rm = js.getResourceManager();
			CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyId.intValue());
			ICursor result = cursor.getCursor().groupx(exps, names, 
					calcExps, calcNames, opt, ctx, capacity);
			
			if (opt == null || opt.indexOf('c') == -1) {
				RemoteCursorProxy rcp = new RemoteCursorProxy(result);
				return new Response(new Integer(rcp.getProxyID()));
			} else {
				// @c：分机结果间不再归并，返回成集群游标，继承分布方式
				IProxy proxy = new CursorProxy(result, cursor.getUnit());
				rm.addProxy(proxy);
				return new Response(new Integer(proxy.getProxyId()));
			}
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	/**
	 * 对集群游标执行外存排序
	 * @param exps 排序表达式数组
	 * @param ctx 计算上下文
	 * @param capacity 内存中能够保存的记录数，如果没有设置则自动估算一个
	 * @param opt 选项
	 * @return 排好序的游标
	 */
	public ICursor sortx(Expression[] exps, Context ctx, int capacity, String opt) {
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		UnitJob []jobs = new UnitJob[count];
		ThreadPool pool = TaskManager.getPool();
		
		int fcount = exps.length;
		String []fields = new String[fcount];
		for (int i = 0; i < fcount; ++i) {
			fields[i] = exps[i].toString();
		}
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			UnitCommand command = new UnitCommand(UnitCommand.SORTX);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[i]));
			command.setAttribute("fields", fields);
			command.setAttribute("capacity", new Integer(capacity));
			command.setAttribute("opt", opt);
			
			ClusterUtil.setParams(command, exps, ctx);
			jobs[i] = new UnitJob(client, command);
			pool.submit(jobs[i]);
		}
		
		ICursor []cursors = new ICursor[count];
		for (int i = 0; i < count; ++i) {
			// 等待任务执行完毕
			jobs[i].join();
			Integer id = (Integer)jobs[i].getResult();
			cursors[i] = new RemoteCursor(cluster.getHost(i), cluster.getPort(i), id.intValue());//, ctx);
		}

		return new MergesCursor(cursors, exps, ctx);
	}

	/**
	 * 节点机上执行游标外存排序
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeSortx(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer cursorProxyId = (Integer)attributes.get("cursorProxyId");
		String []fields = (String[])attributes.get("fields");
		Integer capacity = (Integer)attributes.get("capacity");
		String opt = (String)attributes.get("opt");
		Expression[] exps = null;
		
		JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
		Context ctx = ClusterUtil.createContext(js, attributes);
		
		int fcount = fields.length;
		exps = new Expression[fcount];
		for (int i = 0; i < fcount; ++i) {
			exps[i] = new Expression(ctx, fields[i]);
		}
		
		try {
			ResourceManager rm = js.getResourceManager();
			CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyId.intValue());
			ICursor result = CursorUtil.sortx(cursor.getCursor(), exps, ctx, capacity, opt);
			RemoteCursorProxy rcp = new RemoteCursorProxy(result);
			//IProxy proxy = new CursorProxy(result, cursor.getUnit());
			//rm.addProxy(proxy);
			return new Response(new Integer(rcp.getProxyID()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	// 取集群游标每个节点机相应的维字段的最小值，即起始值，dimCount小于1则取所有维字段的
	Object[][] getMinValues(int dimCount) {
		int count = cluster.getUnitCount();
		Object [][]minValues = new Object[count][];
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.CURSOR_GET_MINVALUES);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[i]));
				command.setAttribute("dimCount", new Integer(dimCount));
 				Response response = client.send(command);
 				minValues[i] = (Object[])response.checkResult();
			} finally {
				client.close();
			}
		}
		
		return minValues;
	}
	
	/**
	 * 节点机上执行取游标维字段的最小值
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeGetMinValues(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		int cursorProxyId = (Integer)attributes.get("cursorProxyId");
		int dimCount = (Integer)attributes.get("dimCount");
		
		JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
				
		try {
			ResourceManager rm = js.getResourceManager();
			CursorProxy cp = (CursorProxy)rm.getProxy(cursorProxyId);
			ICursor cs = cp.getCursor();
			if (cs instanceof IMultipath) {
				ICursor []cursors = ((IMultipath)cs).getParallelCursors();
				cs = cursors[0];
			}
			
			if (!(cs instanceof Cursor)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
			}
			
			Cursor cursor = (Cursor)cs;
			TableMetaData table = cursor.getTableMetaData();
			String []names = table.getSortedColNames();
			if (names == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.needMCursor"));
			}
			
			if (dimCount < 1) {
				dimCount = names.length;
			} else if (names.length < dimCount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
			}
			
			Sequence seq = cursor.peek(1);
			if (seq == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.needMCursor"));
			}
			
			Record r = (Record)seq.get(1);
			Object []vals = new Object[dimCount];
			for (int f = 0; f < dimCount; ++f) {
				vals[f] = r.getFieldValue(names[f]);
			}

			return new Response(vals);
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	/**
	 * 对集群游标做连接，游标在各节点机上同步分布，只需节点机各自做连接，然后合并关联结果即可
	 * @param cursors 集群游标数组
	 * @param exps 关联字段表达式数组
	 * @param names 结果集字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return 集群游标
	 */
	public static ClusterCursor joinx(ClusterCursor []cursors, Expression[][] exps, String []names, String opt, Context ctx) {
		int csCount = cursors.length;
		Cluster cluster = cursors[0].getCluster();
		
		for (int i = 1; i < csCount; ++i) {
			Cluster tmp = cursors[i].getCluster();
			if (!cluster.isEquals(tmp)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.clusterCSNotMatch"));
			}
		}
		
		int unitCount = cluster.getUnitCount();
		String []expStrs = null;
		Expression []totalExps = null;
		if (opt == null || opt.indexOf('p') == -1) {
			int fieldCount =  exps[0].length;
			int totalCount = csCount * fieldCount;
			expStrs = new String[totalCount];
			totalExps = new Expression[totalCount];
			for (int i = 0, q = 0; i < csCount; ++i) {
				Expression []tmp = exps[i];
				for (int f = 0; f < fieldCount; ++f, ++q) {
					expStrs[q] = tmp[f].toString();
					totalExps[q] = tmp[f];
				}
			}
		}
		
		int []newCursorProxyIds = new int[csCount];
		for (int i = 0; i < unitCount; ++i) {
			int []cursorProxyIds = new int[csCount];
			for (int j = 0; j < csCount; ++j) {
				cursorProxyIds[j] = cursors[j].getCursorProxyId(i);
			}
			
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			UnitCommand command = new UnitCommand(UnitCommand.JOINX);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("cursorProxyIds", cursorProxyIds);
			
			command.setAttribute("expStrs", expStrs);
			command.setAttribute("names", names);
			command.setAttribute("option", opt);
			command.setAttribute("unit", new Integer(i));
			
			if (totalExps != null) {
				ClusterUtil.setParams(command, totalExps, ctx);
			}
			
			Response response = client.send(command);
			Integer id = (Integer)response.checkResult();
			newCursorProxyIds[i] = id.intValue();
		}
		
		return new ClusterCursor(cluster, newCursorProxyIds, true);
	}
	
	/**
	 * 节点机上执行对游标做连接
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeJoinx(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		int []cursorProxyIds = (int[])attributes.get("cursorProxyIds");
		String []expStrs = (String[])attributes.get("expStrs");
		String []names = (String[])attributes.get("names");
		String opt = (String)attributes.get("option");
		Integer unit = (Integer) attributes.get("unit");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			Context ctx = ClusterUtil.createContext(js, attributes);
			int csCount = cursorProxyIds.length;
			ICursor []cursors = new ICursor[csCount];
			ResourceManager rm = js.getResourceManager();
			
			ICursor join;
			if (expStrs == null) {
				// @p按位置做连接
				for (int i = 0; i < csCount; ++i) {
					CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyIds[i]);
					cursors[i] = cursor.getCursor();
				}
				
				join = new PJoinCursor(cursors, names);
			} else {
				int totalCount = expStrs.length;
				int fieldCount = totalCount / csCount;
				Expression[][] exps = new Expression[csCount][];
				for (int i = 0, q = 0; i < csCount; ++i) {
					CursorProxy cursor = (CursorProxy)rm.getProxy(cursorProxyIds[i]);
					cursors[i] = cursor.getCursor();
					exps[i] = new Expression[fieldCount];
					for (int f = 0; f < fieldCount; ++f, ++q) {
						exps[i][f] = new Expression(ctx, expStrs[q]);
					}
				}
				
				join = CursorUtil.joinx(cursors, names, exps, opt, ctx);
			}
			
			IProxy proxy = new CursorProxy(join, unit);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	/**
	 * 多路游标接口，取路数
	 * @return int 路数
	 */
	public int getPathCount() {
		return cursorProxyIds.length;
	}
	
	/**
	 * 集群游标变成远程游标数组返回
	 * @return 远程游标数组
	 */
	public ICursor[] getParallelCursors() {
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		ICursor []cursors = new ICursor[count];
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.CURSOR_TO_REMOTE);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("cursorProxyId", new Integer(cursorProxyIds[i]));
				
 				Response response = client.send(command);
 				Integer id = (Integer)response.checkResult();
				cursors[i] = new RemoteCursor(cluster.getHost(i), cluster.getPort(i), id);
			} finally {
				client.close();
			}
		}
		
		return cursors;
	}
	
	/**
	 * 节点机上执行取远程游标
	 * @param attributes 属性
	 * @return Response 给主机的回应
	 */
	public static Response executeGetParallelCursors(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer cursorProxyId = (Integer)attributes.get("cursorProxyId");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);			
			ResourceManager rm = js.getResourceManager();
			CursorProxy cursorProxy = (CursorProxy)rm.getProxy(cursorProxyId.intValue());
			ICursor cursor = cursorProxy.getCursor();
			
			RemoteCursorProxy rcp = new RemoteCursorProxy(cursor);
			return new Response(new Integer(rcp.getProxyID()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
}