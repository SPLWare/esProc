package com.scudata.parallel;

import java.util.HashMap;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.Machines;
import com.scudata.dm.Record;
import com.scudata.dm.ResourceManager;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.Operable;
import com.scudata.dm.op.Operation;
import com.scudata.dw.pseudo.IPseudo;
import com.scudata.dw.pseudo.PseudoDefination;
import com.scudata.dw.pseudo.PseudoDerive;
import com.scudata.dw.pseudo.PseudoNew;
import com.scudata.dw.pseudo.PseudoNews;
import com.scudata.dw.pseudo.PseudoTable;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.FunctionLib;
import com.scudata.thread.ThreadPool;

public class ClusterPseudo implements IClusterObject, IPseudo {
	public static final int TYPE_TABLE = 0;
	public static final int TYPE_NEW = 1;
	public static final int TYPE_NEWS = 2;
	public static final int TYPE_DERIVE = 3;
	
	private ClusterFile clusterFile; // 节点机信息
	private int []pseudoProxyIds; // 对应的节点机虚表代理标识
	private Sequence cache;//对import的结果的cache
	private boolean hasZone;
	private ClusterTableMetaData table;
	
	public ClusterPseudo(ClusterFile clusterFile, boolean hasZone, int[] pseudoProxyIds) {
		this.clusterFile = clusterFile;
		this.pseudoProxyIds = pseudoProxyIds;
		this.hasZone = hasZone;
	}

	private static Sequence toSequence(int[] arr) {
		if (arr == null) return null;
		Sequence seq = new Sequence();
		for (int v : arr) {
			seq.add(v);
		}
		return seq;
	}
	
	public static ClusterPseudo createClusterPseudo(Record rec, Machines hs, int n, Context ctx) {
		Sequence zone = (Sequence) PseudoDefination.getFieldValue(rec, PseudoDefination.PD_ZONE);
		String file = (String) PseudoDefination.getFieldValue(rec, PseudoDefination.PD_FILE);
		ClusterFile clusterFile;
		boolean hasZone;
		
		Cluster cluster = new Cluster(hs.getHosts(), hs.getPorts(), ctx);
		if (zone != null) {
			hasZone = true;
			clusterFile = new ClusterFile(cluster, file, zone, null);
		} else {
			hasZone = false;
			clusterFile = new ClusterFile(cluster, file, null);
		}
		
		ClusterTableMetaData table = clusterFile.openGroupTable(ctx);
		
		int count = clusterFile.getUnitCount();
		int[] newPseudoProxyIds = new int[count];
		Record record = new Record(rec.dataStruct(), rec.getFieldValues());
		PartitionFile[] pfs = clusterFile.getPartitionFiles();
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(clusterFile.getHost(i), clusterFile.getPort(i));

			try {
				UnitCommand command = new UnitCommand(UnitCommand.PSEUDO_CREATE);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				Sequence z = toSequence(pfs[i].getParts());
				if (z == null) {
					PseudoDefination.setFieldValue(record, PseudoDefination.PD_ZONE, null);
				} else {
					PseudoDefination.setFieldValue(record, PseudoDefination.PD_ZONE, z);
				}
				command.setAttribute("rec", record);
				command.setAttribute("n", new Integer(n));
				
				Response response = client.send(command);
				Integer id = (Integer) response.checkResult();
				newPseudoProxyIds[i] = id.intValue();
			} finally {
				client.close();
			}
		}
		ClusterPseudo result = new ClusterPseudo(clusterFile, hasZone, newPseudoProxyIds);
		result.table = table;
		return result;
	}
	
	
	public static Response executeCreateClusterPseudo(HashMap<String, Object> attributes) {
		String jobSpaceID = (String) attributes.get("jobSpaceId");
		IPseudo pseudo = null;
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			Context ctx = ClusterUtil.createContext(js, attributes);
			
			Record rec = (Record) attributes.get("rec");
			Integer n = (Integer) attributes.get("n");
			Integer ptableId = (Integer) attributes.get("ptableId");
			String option = (String) attributes.get("option");
			Integer type = (Integer) attributes.get("type");
			
			if (type == null) {
				pseudo = PseudoTable.create(rec, n, ctx);
			} else {
				PseudoProxy pseudoProxy = (PseudoProxy) rm.getProxy(ptableId.intValue());
				IPseudo ptable = pseudoProxy.getPseudo();
				PseudoDefination pd = new PseudoDefination(rec, ctx);
				if (type == TYPE_NEW) {
					pseudo = new PseudoNew(pd, ptable, option);
				} else if (type == TYPE_NEWS) {
					pseudo = new PseudoNews(pd, ptable, option);
				} else if (type == TYPE_DERIVE) {
					pseudo = new PseudoDerive(pd, ptable, option);
				}
			}
			
			IProxy proxy = new PseudoProxy(pseudo);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public Cluster getCluster() {
		return clusterFile.getCluster();
	}

	public void addColNames(String[] nameArray) {
		for (String name : nameArray) {
			addColName(name);
		}
	}

	public void addColName(String name) {
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.PSEUDO_ADD_COLNAME);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("pseudoProxyId", new Integer(pseudoProxyIds[i]));
				command.setAttribute("name", name);
				
 				Response response = client.send(command);
				response.checkResult();
			} finally {
				client.close();
			}
		}
	}
	
	public static Response executeAddColName(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer pseudoProxyId = (Integer)attributes.get("pseudoProxyId");
		String name = (String)attributes.get("name");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			
			ResourceManager rm = js.getResourceManager();
			PseudoProxy pseudo = (PseudoProxy)rm.getProxy(pseudoProxyId.intValue());
			pseudo.getPseudo().addColName(name);
			return new Response();
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public ICursor cursor(Expression[] exps, String[] names) {
		return cursor(exps, names, false);
	}
	
	public ICursor cursor(Expression[] exps, String[] names, boolean isColumn) {
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		int[] cursorProxyIds = new int[count];
		
		String []expStrs = null;
		if (exps != null) {
			int len = exps.length;
			expStrs = new String[len];
			for (int i = 0; i < len; ++i) {
				if (exps[i] != null) {
					expStrs[i] = exps[i].toString();
				}
			}
		}
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));

			try {
				UnitCommand command = new UnitCommand(UnitCommand.PSEUDO_CURSOR);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("pseudoProxyId", new Integer(pseudoProxyIds[i]));
				
				command.setAttribute("expStrs",  expStrs);
				command.setAttribute("names", names);
				command.setAttribute("isColumn", isColumn);
				command.setAttribute("unit", new Integer(i));
				
				Response response = client.send(command);
				Integer id = (Integer) response.checkResult();
				cursorProxyIds[i] = id.intValue();
			} finally {
				client.close();
			}
		}

		ClusterCursor result = new ClusterCursor(this, cursorProxyIds, true);
		result.setDistribute(table.getDistribute());
		result.setSortedColNames(getAllSortedColNames());
		return result;
	}

	public static Response executeCreateCursor(HashMap<String, Object> attributes) {
		String jobSpaceID = (String) attributes.get("jobSpaceId");
		Integer pseudoProxyId = (Integer) attributes.get("pseudoProxyId");
		String []expStrs = (String[]) attributes.get("expStrs");
		String []names = (String[]) attributes.get("names");
		Boolean isColumn = (Boolean) attributes.get("isColumn");
		Integer unit = (Integer) attributes.get("unit");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			PseudoProxy pseudoProxy = (PseudoProxy) rm.getProxy(pseudoProxyId.intValue());
			IPseudo pseudo = pseudoProxy.getPseudo();
			Context ctx = ClusterUtil.createContext(js, attributes, "cursor", null);
			
			Expression []exps = null;
			if (expStrs != null) {
				int len = expStrs.length;
				exps = new Expression[len];
				for (int i = 0; i < len; ++i) {
					exps[i] = new Expression(ctx, expStrs[i]);
				}
			}
			
			ICursor cursor = pseudo.cursor(exps, names, isColumn);
			IProxy proxy = new CursorProxy(cursor, unit);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public void addPKeyNames() {
		throw new RQException("never run to here");
	}

	public boolean isColumn(String col) {
		throw new RQException("never run to here");
	}

	public Context getContext() {
		throw new RQException("never run to here");
	}

	public Object clone(Context ctx) throws CloneNotSupportedException {
		int count = clusterFile.getUnitCount();
		int[] newPseudoProxyIds = new int[count];
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(clusterFile.getHost(i), clusterFile.getPort(i));

			try {
				UnitCommand command = new UnitCommand(UnitCommand.PSEUDO_CLONE);
				command.setAttribute("jobSpaceId", clusterFile.getJobSpaceId());
				command.setAttribute("pseudoProxyId", new Integer(pseudoProxyIds[i]));
				
				Response response = client.send(command);
				Integer id = (Integer) response.checkResult();
				newPseudoProxyIds[i] = id.intValue();
			} finally {
				client.close();
			}
		}

		ClusterPseudo result = new ClusterPseudo(clusterFile, hasZone, newPseudoProxyIds);
		return result;
	}
	
	public static Response executeClone(HashMap<String, Object> attributes) {
		String jobSpaceID = (String) attributes.get("jobSpaceId");
		Integer pseudoProxyId = (Integer) attributes.get("pseudoProxyId");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			PseudoProxy pseudoProxy = (PseudoProxy) rm.getProxy(pseudoProxyId.intValue());
			IPseudo pseudo = pseudoProxy.getPseudo();
			Context ctx = ClusterUtil.createContext(js, attributes, "cursor", null);
			
			Object obj = pseudo.clone(ctx);
			IProxy proxy = new PseudoProxy((IPseudo) obj);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public void append(ICursor cursor, String option) {
		// TODO Auto-generated method stub
		
	}

	public Sequence update(Sequence data, String opt) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Sequence delete(Sequence data, String opt) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Operable addOperation(Operation op, Context ctx) {
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		Function function = op.getFunction();
		String functionName = function.getFunctionName();
		String option = function.getOption();
		String param = function.getParamString();
		int[] newPseudoProxyIds = new int[count];
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.PSEUDO_ADD_OPERATION);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("pseudoProxyId", new Integer(pseudoProxyIds[i]));
				command.setAttribute("functionName", functionName);
				command.setAttribute("option", option);
				command.setAttribute("param", param);
				ClusterUtil.setParams(command, function, ctx);
				
 				Response response = client.send(command);
 				Integer id = (Integer) response.checkResult();
				newPseudoProxyIds[i] = id.intValue();
			} finally {
				client.close();
			}
		}
		
		ClusterPseudo result = new ClusterPseudo(clusterFile, hasZone, newPseudoProxyIds);
		return result;
	}
	
	public static Response executeAddOperation(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer pseudoProxyId = (Integer)attributes.get("pseudoProxyId");
		String functionName = (String)attributes.get("functionName");
		String option = (String)attributes.get("option");
		String param = (String)attributes.get("param");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			Context ctx = ClusterUtil.createContext(js, attributes, functionName, option);
			
			ResourceManager rm = js.getResourceManager();
			PseudoProxy pseudo = (PseudoProxy)rm.getProxy(pseudoProxyId.intValue());
			Object obj = FunctionLib.executeMemberFunction(pseudo.getPseudo(), functionName, param, option, ctx);
			
			IProxy proxy = new PseudoProxy((IPseudo) obj);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));		
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public void setCache(Sequence cache) {
		this.cache = cache;
	}
	
	public Sequence getCache() {
		return cache;
	}
	
	/**
	 * 返回所有的维字段名
	 * @return 字段名数组
	 */
	public String[] getAllSortedColNames() {
		return table.getAllSortedColNames();
	}
	
	/**
	 * 读出组表字段成集群内表
	 * @param fields 要读出的字段名数组
	 * @param filter 过滤条件
	 * @param ctx 计算上下文
	 * @return 集群内表
	 */
	public ClusterMemoryTable memory(String []fields, Expression filter, String option, Context ctx) {
		boolean hasV = (option != null && option.indexOf('v') != -1);
		if (hasV) {
			
		}
		
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		RemoteMemoryTable[] tables = new RemoteMemoryTable[count];
		
		UnitJob []jobs = new UnitJob[count];
		ThreadPool pool = TaskManager.getPool();
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			UnitCommand command = new UnitCommand(UnitCommand.MEMORY_GT);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("tmdProxyId", new Integer(pseudoProxyIds[i]));
			
			command.setAttribute("fields", fields);
			command.setAttribute("option", option);
			command.setAttribute("filter", filter == null ? null : filter.toString());
			command.setAttribute("unit", new Integer(i));
			
			ClusterUtil.setParams(command, filter, ctx);
			jobs[i] = new UnitJob(client, command);
			pool.submit(jobs[i]);
		}

		for (int i = 0; i < count; ++i) {
			// 等待任务执行完毕
			jobs[i].join();
			tables[i] = (RemoteMemoryTable)jobs[i].getResult();
		}

		ClusterMemoryTable result = new ClusterMemoryTable(getCluster(), tables, hasZone);
		result.setDistribute(table.getDistribute());
		result.setSortedColNames(getAllSortedColNames());
		return result;
	}
}
