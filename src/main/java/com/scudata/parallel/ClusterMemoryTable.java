package com.scudata.parallel;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Externalizable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.IResource;
import com.scudata.dm.IndexTable;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.ResourceManager;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Operable;
import com.scudata.dm.op.Operation;
import com.scudata.dm.op.Select;
import com.scudata.dw.MemoryTable;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.FunctionLib;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HashUtil;
import com.scudata.util.Variant;

/**
 * 集群内表
 * @author RunQian
 *
 */
public class ClusterMemoryTable extends Operable implements IClusterObject, IResource, Externalizable {
	private static final long serialVersionUID = 0x07015403;
	
	private Cluster cluster;
	
	// 数据要求按主键在节点机内、节点机间有序
	private RemoteMemoryTable []tables;
	private boolean isDistributed; // 是否分布内表，分布文件或者分段则为分布内表
	
	private Expression distribute; // 分布表达式，可以为空
	private String []sortedColNames; // 主键或排序字段
	private DataStruct distDs; // 计算分布时用的表达式
	
	private RemoteMemoryTable []sortedTables; // 按照主键或分区排序
	private int partCount; // 当前内表包含的最大分区数
	private int []partIndex; // 每个分区对应的远程内表的索引
	
	public ClusterMemoryTable() {
	}
	
	public ClusterMemoryTable(Cluster cluster, RemoteMemoryTable []tables, boolean isDistributed) {
		this.cluster = cluster;
		this.tables = tables;
		this.isDistributed = isDistributed;
	}
	
	// 当维表被switch或join时，先对节点机按分区或主键值排序
	private void prepareFind(Context ctx) {
		if (sortedTables != null) {
			return;
		}
		
		RemoteMemoryTable []tables = this.tables;
		if (distribute == null) {
			String str = tables[0].getDistribute();
			if (str != null && str.length() > 0) {
				distribute = new Expression(str);
			}
		}
		
		if (distribute != null) {
			for (RemoteMemoryTable table : tables) {
				if (table.getPart() < 1) {
					distribute = null;
					break;
				}
			}
		}
		
		int count = tables.length;
		sortedTables = new RemoteMemoryTable[count];
		System.arraycopy(tables, 0, sortedTables, 0, count);
		
		if (distribute == null) {
			// 按主键值排序
			Comparator<RemoteMemoryTable> cmp = new Comparator<RemoteMemoryTable>() {
				public int compare(RemoteMemoryTable o1, RemoteMemoryTable o2) {
					return Variant.compare(o1.getStartKeyValue(), o2.getStartKeyValue());
				}
			};
			
			Arrays.sort(sortedTables, cmp);
		} else {
			// 按分区排序
			Comparator<RemoteMemoryTable> cmp = new Comparator<RemoteMemoryTable>() {
				public int compare(RemoteMemoryTable o1, RemoteMemoryTable o2) {
					return o1.getPart() <= o2.getPart() ? -1 : 1;
				}
			};
			
			Arrays.sort(sortedTables, cmp);
			partCount = sortedTables[count - 1].getPart();
			partIndex = new int[partCount + 1];
			for (int i = 0; i < partCount; ++i) {
				partIndex[i] = -1;
			}
			
			for (int i = 0; i < count; ++i) {
				partIndex[sortedTables[i].getPart()] = i;
			}

			// 生成计算分区用的数据结构
			if (sortedColNames != null) {
				distDs = new DataStruct(sortedColNames);
			} else {
				ArrayList<String> list = new ArrayList<String>();
				distribute.getUsedFields(ctx, list);
				if (list.size() != 0) {
					String []names = new String[list.size()];
					list.toArray(names);
					distDs = new DataStruct(names);
				}
			}
		}
	}
	
	public boolean isDistributed() {
		return isDistributed;
	}
	
	public Expression getDistribute() {
		return distribute;
	}

	public void setDistribute(Expression distribute) {
		this.distribute = distribute;
	}
	
	public String[] getSortedColNames() {
		return sortedColNames;
	}

	public void setSortedColNames(String[] sortedColNames) {
		this.sortedColNames = sortedColNames;
	}
	
	public int getProxyId(int seq) {
		return tables[seq].getProxyId();
	}
	
	public int[] getProxyIds() {
		int len = tables.length;
		int ids[] = new int[len];
		for (int i = 0; i < len; i++) {
			ids[i] = tables[i].getProxyId();
		}
		return ids;
	}
	
	// 取集群内表在当前节点机上对应的内表的proxyId
	public int getCurrentClusterProxyId() {
		HostManager hm = HostManager.instance();
		String host = hm.getHost();
		int port = hm.getPort();
		int count = cluster.getUnitCount();
		
		for (int i = 0; i < count; ++i) {
			if (cluster.getHost(i).equals(host) && cluster.getPort(i) == port) {
				return tables[i].getProxyId();
			}
		}
		
		return -1;
	}
	
	public Cluster getCluster() {
		return cluster;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(cluster);
		out.writeObject(tables);
		out.writeBoolean(isDistributed);
		
		String exp = null;
		if (distribute != null) {
			exp = distribute.toString();
		}
		
		out.writeObject(exp);
		out.writeObject(sortedColNames);
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		cluster = (Cluster)in.readObject();
		tables = (RemoteMemoryTable [])in.readObject();
		isDistributed = in.readBoolean();
		
		String exp = (String)in.readObject();
		if (exp != null) {
			distribute = new Expression(exp);
		}
		
		sortedColNames = (String[])in.readObject();
	}
	
	/**
	 * k.row(T)
	 * @param key 待查找的名字字段的值
	 * @return
	 */
	public BaseRecord getRow(Object key) {
		BaseRecord r = null;
		Cluster cluster = this.cluster;
		int count = cluster.getUnitCount();
		if (!isDistributed) {
			count = 1;
		}
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.GET_MT_ROW);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("proxyId", getProxyId(i));
				command.setAttribute("key", key);
				Response response = client.send(command);
				r = (BaseRecord)response.checkResult();
				if (r != null) break;//只要找到了就返回
			} finally {
				client.close();
			}
		}
		
		return r;
	}

	public static Response executeGetRow(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer proxyId = (Integer)attributes.get("proxyId");
		Object key = attributes.get("key");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			TableProxy table = (TableProxy)rm.getProxy(proxyId.intValue());
			return new Response(table.getRow(key, null));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
		
	//如果有某个分机没有成功创建则如何处理？
	public boolean createIndex(Integer capacity, String opt) {
		Cluster cluster = this.cluster;
		int count = cluster.getUnitCount();
		boolean result = true;
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.CREATE_MT_INDEX);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("proxyId", getProxyId(i));
				command.setAttribute("capacity", capacity);
				command.setAttribute("opt", opt);
				Response response = client.send(command);
				result &= (Boolean)response.checkResult();
			} finally {
				client.close();
			}
		}		
		return result;
	}
	
	public static Response executeCreateIndex(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer proxyId = (Integer)attributes.get("proxyId");
		Integer capacity = (Integer)attributes.get("capacity");
		String opt = (String)attributes.get("opt");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			TableProxy table = (TableProxy)rm.getProxy(proxyId.intValue());
			table.createIndex(capacity, opt);
			return new Response(Boolean.TRUE);
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public Table dup() {
		Cluster cluster = this.cluster;
		int count = cluster.getUnitCount();
		if (!isDistributed) {
			count = 1;
		}
		
		Table mt = null;
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.DUP_CLUSTER_MT);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("proxyId", getProxyId(i));
				Response response = client.send(command);
				Table cur = (Table)response.checkResult();
				if (mt != null) {
					mt.append(cur, null);
				} else {
					mt = cur;
				}
			} finally {
				client.close();
			}
		}
		
		return mt;
	}

	public static Response executeDup(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer proxyId = (Integer)attributes.get("proxyId");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			TableProxy table = (TableProxy)rm.getProxy(proxyId.intValue());
			return new Response(table.getTable());
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public static ClusterMemoryTable dupLocal(Cluster cluster, Table table) {
		int count = cluster.getUnitCount();
		RemoteMemoryTable[] tables = new RemoteMemoryTable[count];
		int recordCount = table.length();
		int []seqs = table.dataStruct().getPKIndex();
		int keyCount = seqs == null ? 0 : seqs.length;
		
		Object keyValue = null;
		if (keyCount == 1) {
			if(recordCount > 0) {
				BaseRecord r = table.getRecord(1);
				keyValue = r.getNormalFieldValue(seqs[0]);
			}
		} else if (keyCount > 1) {
			Object []vals = new Object[keyCount];
			keyValue = vals;
			if(recordCount > 0) {
				BaseRecord r = table.getRecord(1);
				for (int i = 0; i < keyCount; ++i) {
					vals[i] = r.getNormalFieldValue(seqs[i]);
				}
			}
		}
		
		for (int i = 0; i < count; ++i) {
			String host = cluster.getHost(i);
			int port = cluster.getPort(i);
			UnitClient client = new UnitClient(host, port);			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.DUP_LOCAL_MT);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("table", table);
				command.setAttribute("unit", new Integer(i));
				Response response = client.send(command);
				Integer proxyId = (Integer)response.checkResult();
				tables[i] = new RemoteMemoryTable(host, port, proxyId, recordCount);

				if (keyCount > 0) {
					tables[i].setStartKeyValue(keyValue, keyCount);
				}
				
				if (table instanceof MemoryTable) {
					MemoryTable mt = (MemoryTable)table;
					tables[i].setDistribute(mt.getDistribute(), mt.getPart());
				}
			} finally {
				client.close();
			}
		}
		
		return new ClusterMemoryTable(cluster, tables, false);
	}
	
	public static Response executeDupLocal(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Table table = (Table)attributes.get("table");
		Integer unit = (Integer)attributes.get("unit");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			IProxy proxy = new TableProxy(table, unit);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public ClusterCursor cursor(Expression []exps, String []names, Expression filter, int segCount, String opt, Context ctx) {
		Cluster cluster = this.cluster;
		int count = cluster.getUnitCount();
		int[] cursorProxyIds = new int[count];
		boolean isSeg = !isDistributed;
		
		String []fieldExps = null;
		if (exps != null) {
			int fcount = exps.length;
			fieldExps = new String[fcount];
			for (int i = 0; i < fcount; ++i) {
				fieldExps[i] = exps[i].toString();
			}
		}
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			try {
				UnitCommand command = new UnitCommand(UnitCommand.CREATE_MT_CURSOR);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("proxyId", getProxyId(i));
				
				command.setAttribute("fieldExps", fieldExps);
				command.setAttribute("names", names);
				command.setAttribute("filter", filter == null ? null : filter.toString());
				command.setAttribute("option", opt);
				
				command.setAttribute("unit", new Integer(i));
				command.setAttribute("unitCount", new Integer(count));
				command.setAttribute("isSeg", isSeg);
				command.setAttribute("segCount", new Integer(segCount));
				
				ClusterUtil.setParams(command, exps, ctx);
				Response response = client.send(command);
				Integer id = (Integer) response.checkResult();
				cursorProxyIds[i] = id.intValue();
			} finally {
				client.close();
			}
		}
		
		ClusterCursor result = new ClusterCursor(this, cursorProxyIds, isDistributed);
		result.setDistribute(distribute);
		result.setSortedColNames(sortedColNames);
		return result;
	}
		
	public static Response executeCreateCursor(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer proxyId = (Integer)attributes.get("proxyId");
		String []fieldExps = (String[])attributes.get("fieldExps");
		String []names = (String[])attributes.get("names");
		String filter = (String)attributes.get("filter");
		String option = (String)attributes.get("option");
		
		Integer unit = (Integer) attributes.get("unit");
		int unitCount = (Integer)attributes.get("unitCount");
		boolean isSeg = (Boolean)attributes.get("isSeg");
		int segCount = (Integer)attributes.get("segCount");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			Context ctx = ClusterUtil.createContext(js, attributes);
			ResourceManager rm = js.getResourceManager();
			TableProxy tp = (TableProxy)rm.getProxy(proxyId.intValue());
			Table table = tp.getTable();
			
			ICursor cursor;
			int len = table.length();
			int start = 1;
			int end = len + 1; // 不包含
			
			// 节点机间进行切分
			if (isSeg) {
				int blockSize = len / unitCount;
				start = blockSize * unit + 1;
				if (unit + 1 != unitCount) {
					end = blockSize + start;
				}
			}
			
			if (option != null && option.indexOf('m') != -1) {
				if (segCount < 2) {
					segCount = Env.getCursorParallelNum();
				}
				
				len = end - start;
				if (segCount > 1 && segCount < len) {
					// 节点机上产生多路游标
					int blockSize = len / segCount;
					ICursor []cursors = new ICursor[segCount];
					for (int i = 1; i <= segCount; ++i) {
						int start2 = blockSize * (i - 1) + start;
						int end2;
						
						if (i == segCount) {
							end2 = end;
						} else {
							end2 = start2 + blockSize;
						}
						
						cursors[i - 1] = new MemoryCursor(table, start2, end2);
					}
					
					cursor = new MultipathCursors(cursors, ctx);
				} else {
					cursor = new MemoryCursor(table, start, end);
				}
			} else {
				cursor = new MemoryCursor(table, start, end);
			}
			
			if (names != null) {
				int fcount = names.length;
				Expression []exps = new Expression[fcount];
				for (int i = 0; i < fcount; ++i) {
					exps[i] = new Expression(ctx, fieldExps[i]);
				}
				
				New newOp = new New(exps, names, null);
				cursor.addOperation(newOp, ctx);
			}
			
			if (filter != null) {
				Expression filterExp = new Expression(ctx, filter);
				Select select = new Select(filterExp, null);
				cursor.addOperation(select, ctx);
			}
			
			IProxy proxy = new CursorProxy(cursor, unit);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));			
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public static ClusterMemoryTable memory(Cluster cluster, String varName) {
		int count = cluster.getUnitCount();
		RemoteMemoryTable[] tables = new RemoteMemoryTable[count];
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			UnitCommand command = new UnitCommand(UnitCommand.MEMORY_TABLE);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("varName", varName);
			command.setAttribute("unit", new Integer(i));
			
			Response response = client.send(command);
			tables[i] = (RemoteMemoryTable)response.checkResult();
		}

		// 如果首条记录的主键值不同则认为是分布的
		Object keyValue1 = tables[0].getStartKeyValue();
		Object keyValue2 = tables[1].getStartKeyValue();
		boolean b = count > 1 && !Variant.isEquals(keyValue1, keyValue2);
		return new ClusterMemoryTable(cluster, tables, b);
	}
	
	public static Response executeMemory(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		String varName = (String)attributes.get("varName");
		Integer unit = (Integer)attributes.get("unit");
		
		// 把varName当表达式计算，结果应该为排列
		JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
		Context ctx = new Context();
		ctx.setJobSpace(js);
		
		try {
			Expression exp = new Expression(ctx, varName);
			Object val = exp.calculate(ctx);
			MemoryTable mt;
			
			if (val instanceof MemoryTable) {
				mt = (MemoryTable)val;
			} else if (val instanceof Table) {
				mt = new MemoryTable((Table)val);
			} else if (val instanceof Sequence) {
				Sequence seq = (Sequence)val;
				Table table = seq.derive("o");
				mt = new MemoryTable(table);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("memory" + mm.getMessage("function.paramTypeError"));
			}
			
			ResourceManager rm = js.getResourceManager();
			IProxy proxy = new TableProxy(mt, unit);
			rm.addProxy(proxy);
			
			RemoteMemoryTable rmt = newRemoteMemoryTable(proxy.getProxyId(), mt);
			return new Response(rmt);
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}

	// 取主键个数
	private int getKeyCount() {
		return tables[0].getKeyCount();
	}
	
	private Sequence[] getRows(Sequence[] keyValues, String[] fieldExps, String[] fieldNames) {
		RemoteMemoryTable []sortedTables = this.sortedTables;
		int count = sortedTables.length;
		Sequence []result = new Sequence[count];
		
		for (int i = 0; i < count; ++i) {
			RemoteMemoryTable rmt = sortedTables[i];
			UnitClient client = new UnitClient(rmt.getHost(), rmt.getPort());
			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.GET_MT_ROWS);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("proxyId", rmt.getProxyId());
				command.setAttribute("keyValues", keyValues[i]);
				command.setAttribute("fieldExps", fieldExps);
				command.setAttribute("fieldNames", fieldNames);
				
				Response response = client.send(command);
				result[i] = (Sequence)response.checkResult();
			} finally {
				client.close();
			}
		}
		
		return result;
	}
	
	public static Response executeGetRows(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer proxyId = (Integer)attributes.get("proxyId");
		Sequence keyValues = (Sequence)attributes.get("keyValues");
		String []fieldExps = (String [])attributes.get("fieldExps");
		String []fieldNames = (String [])attributes.get("fieldNames");
		
		JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
		Context ctx = ClusterUtil.createContext(js);
		Expression []newExps = null;
		if (fieldExps != null) {
			int count = fieldExps.length;
			newExps = new Expression[count];
			for (int i = 0; i < count; ++i) {
				newExps[i] = new Expression(ctx, fieldExps[i]);
			}
		}
		
		try {
			ResourceManager rm = js.getResourceManager();
			TableProxy tableProxy = (TableProxy)rm.getProxy(proxyId.intValue());
			Table table = tableProxy.getTable();
			Sequence result = getRows(table, keyValues, newExps, fieldNames, ctx);
			return new Response(result);
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	private static Sequence getRows(Table table, Sequence keySeq,
			Expression[] newExps, String[] newNames, Context ctx) {
		if (keySeq == null || keySeq.length() == 0) {
			return new Sequence(0);
		}

		String[] pkNames = table.getPrimary();
		int pkCount = pkNames.length;
		IndexTable indexTable = table.getIndexTable();
		if (indexTable == null) {
			table.createIndexTable(null);
			indexTable = table.getIndexTable();
		}

		int len = keySeq.length();
		Sequence result = new Sequence(len);

		if (pkCount == 1) {
			for (int i = 1; i <= len; ++i) {
				Object pk = keySeq.getMem(i);
				BaseRecord r = (BaseRecord) indexTable.find(pk);
				result.add(r);
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				Object[] pks = (Object[])keySeq.getMem(i);
				BaseRecord r = (BaseRecord) indexTable.find(pks);
				result.add(r);
			}
		}

		if (newExps == null || newExps.length == 0) {
			return result;
		}
		
		IArray mems = result.getMems();
		int colCount = newExps.length;
		DataStruct ds = new DataStruct(newNames);
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(result);
		stack.push(current);
		
		try {
			for (int i = 1; i <= len; ++i) {
				if (mems.get(i) != null) {
					current.setCurrent(i);
					Record r = new Record(ds);
					
					for (int c = 0; c < colCount; ++c) {
						r.setNormalFieldValue(c, newExps[c].calculate(ctx));
					}
					
					mems.set(i, r);
				} else {
					mems.set(i, null);
				}
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}
	
	/**
	 * 按主键值取记录
	 * @param keySeq 主键值序列
	 * @param newExps 返回的计算字段表达式，空则返回源记录
	 * @param newNames
	 * @param ctx
	 * @return
	 */
	public Sequence getRows(Sequence keySeq, String[] newExps, String[] newNames, Context ctx) {
		prepareFind(ctx);
		
		IArray mems = keySeq.getMems();
		int srcLen = mems.size();
		HashUtil hashUtil = new HashUtil(srcLen);
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];
		Sequence ids = new Sequence(hashUtil.getCapacity());
		
		int keyCount = getKeyCount();
		if (keyCount == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("ds.lessKey"));
		}
		
		if (keyCount == 1) {
			for (int i = 1; i <= srcLen; ++i) {
				Object mem = mems.get(i);
				int hash = hashUtil.hashCode(mem);
				if (groups[hash] == null) {
					Object []objs = new Object[2];
					objs[0] = mem;
					ids.add(objs);
	
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					groups[hash].add(objs);
				} else {
					int index = HashUtil.bsearch_a(groups[hash], mem);
					if (index < 1) {
						Object []objs = new Object[2];
						objs[0] = mem;
						ids.add(objs);
	
						groups[hash].add(-index, objs);
					}
				}
			}
		} else {
			int count = keyCount + 1;
			for (int i = 1; i <= srcLen; ++i) {
				Object []mem = (Object[])mems.get(i);
				int hash = hashUtil.hashCode(mem);
				if (groups[hash] == null) {
					Object []objs = new Object[count];
					System.arraycopy(mem, 0, objs, 0, keyCount);
					objs[keyCount] = mem;
					ids.add(objs);

					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					groups[hash].add(objs);
				} else {
					int index = HashUtil.bsearch_a(groups[hash], mem, keyCount);
					if (index < 1) {
						Object []objs = new Object[count];
						System.arraycopy(mem, 0, objs, 0, keyCount);
						objs[keyCount] = mem;
						ids.add(objs);

						groups[hash].add(-index, objs);
					}
				}
			}
		}

		// 对主键序列进行分组
		Sequence []seqs = group(ids, ctx);
		
		int gcount = seqs.length;
		Sequence []keySeqs = new Sequence[gcount];
		for (int g = 0; g < gcount; ++g) {
			Sequence seq = seqs[g];
			IArray curMems = seq.getMems();
			int len = curMems.size();
			Sequence tmp = new Sequence(len);
			keySeqs[g] = tmp;
			
			int keyIndex = keyCount == 1 ? 0 : keyCount;
			for (int i = 1; i <= len; ++i) {
				Object []objs = (Object[])curMems.get(i);
				tmp.add(objs[keyIndex]);
			}
		}

		Sequence []tables = getRows(keySeqs, newExps, newNames);

		// 把返回的tables与主键序列关联起来，并合并到同一个table上
		//DataStruct ds = new DataStruct(newNames);
		for (int g = 0; g < gcount; ++g) {
			Sequence seq = seqs[g];
			IArray curMems = seq.getMems();
			int len = curMems.size();
			Sequence curTable = tables[g];
			for (int i = 1; i <= len; ++i) {
				Object []objs = (Object[])curMems.get(i);
				BaseRecord r = (BaseRecord)curTable.get(i);
				objs[keyCount] = r;

				//if (r != null) r.setDataStruct(ds);
			}
		}

		Sequence result = new Sequence(srcLen);
		if (keyCount == 1) {
			for (int i = 1; i <= srcLen; ++i) {
				Object mem = mems.get(i);
				int hash = hashUtil.hashCode(mem);
				int index = HashUtil.bsearch_a(groups[hash], mem);
				Object []objs = (Object[])groups[hash].get(index);
				result.add(objs[1]);
			}
		} else {
			for (int i = 1; i <= srcLen; ++i) {
				Object []mem = (Object[])mems.get(i);
				int hash = hashUtil.hashCode(mem);
				int index = HashUtil.bsearch_a(groups[hash], mem, keyCount);
				Object []objs = (Object[])groups[hash].get(index);
				result.add(objs[keyCount]);
			}
		}

		return result;
	}
	
	// 对主键序列按节点机进行分组
	private Sequence[] group(Sequence result, Context ctx) {
		RemoteMemoryTable []tables = this.sortedTables;
		int groupCount = tables.length;
		
		// 如果不是分布内表只从第一个节点机找？
		if (!isDistributed) {
			Sequence []seqs = new Sequence[groupCount];
			seqs[0] = new Sequence(result);
			for (int i = 1; i < groupCount; ++i) {
				seqs[i] = new Sequence(0);
			}
			
			return seqs;
		}
		
		IArray mems = result.getMems();
		int len = mems.size();
		
		Sequence []seqs = new Sequence[groupCount];
		int initCount = len / groupCount + 10;
		for (int i = 0; i < groupCount; ++i) {
			seqs[i] = new Sequence(initCount);
		}

		if (distDs == null) {
			int keyCount = getKeyCount();
			int last = groupCount - 1;
			if (keyCount == 1) {
				Object []startVals = new Object[groupCount];
				for (int i = 0; i < groupCount; ++i) {
					startVals[i] = tables[i].getStartKeyValue();
				}
				
				Next:
				for (int i = 1; i <= len; ++i) {
					Object []objs = (Object[])mems.get(i);
					for (int g = 1; g < groupCount; ++g) {
						int cmp = Variant.compare(startVals[g], objs[0]);
						if (cmp > 0) {
							seqs[g - 1].add(objs);
							continue Next;
						} else if (cmp == 0) {
							seqs[g].add(objs);
							continue Next;
						}
					}
					
					seqs[last].add(objs);
				}
			} else {
				Object [][]startVals = new Object[groupCount][];
				for (int i = 0; i < groupCount; ++i) {
					startVals[i] = (Object [])tables[i].getStartKeyValue();
				}
						
				Next:
				for (int i = 1; i <= len; ++i) {
					Object []objs = (Object[])mems.get(i);
					for (int g = 1; g < groupCount; ++g) {
						int cmp = Variant.compareArrays(startVals[g], objs, keyCount);
						if (cmp > 0) {
							seqs[g - 1].add(objs);
							continue Next;
						} else if (cmp == 0) {
							seqs[g].add(objs);
							continue Next;
						}
					}
					
					seqs[last].add(objs);
				}
			}
		} else {
			int partCount = this.partCount; // 当前内表包含的最大分区数
			int []partIndex = this.partIndex; // 每个分区对应的远程内表的索引
			Expression distribute = this.distribute;
			int keyCount = distDs.getFieldCount();
			Record r = new Record(distDs);
			ComputeStack stack = ctx.getComputeStack();
			stack.push(r);
			
			try {
				for (int i = 1; i <= len; ++i) {
					Object []objs = (Object[])mems.get(i);
					for (int k = 0; k < keyCount; ++k) {
						r.setNormalFieldValue(k, objs[k]);
					}
					
					Object obj = distribute.calculate(ctx);
					if (obj instanceof Number) {
						int n = ((Number)obj).intValue();
						if (n > 0 && n <= partCount) {
							seqs[partIndex[n]].add(objs);
						}
					}
				}
			} finally {
				stack.pop();
			}
		}
		
		return seqs;
	}
	
	public static RemoteMemoryTable newRemoteMemoryTable(int proxy, Table table) {
		int recordCount = table.length();
		int []seqs = table.dataStruct().getPKIndex();
		int keyCount = seqs == null ? 0 : seqs.length;
		
		Object keyValue = null;
		if (keyCount == 1) {
			if(recordCount > 0) {
				BaseRecord r = table.getRecord(1);
				keyValue = r.getNormalFieldValue(seqs[0]);
			}
		} else if (keyCount > 1) {
			Object []vals = new Object[keyCount];
			keyValue = vals;
			if(recordCount > 0) {
				BaseRecord r = table.getRecord(1);
				for (int i = 0; i < keyCount; ++i) {
					vals[i] = r.getNormalFieldValue(seqs[i]);
				}
			}
		}
		
		HostManager hm = HostManager.instance();
		String host = hm.getHost();
		int port = hm.getPort();
		RemoteMemoryTable rmt = new RemoteMemoryTable(host, port, proxy, recordCount);
		
		if (keyCount > 0) {
			rmt.setStartKeyValue(keyValue, keyCount);
		}
		
		if (table instanceof MemoryTable) {
			MemoryTable mt = (MemoryTable)table;
			rmt.setDistribute(mt.getDistribute(), mt.getPart());
		}
		
		return rmt;
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
				UnitCommand command = new UnitCommand(UnitCommand.MT_ADD_OPERATION);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("proxyId", new Integer(getProxyId(i)));
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
	
	public static Response executeAddOperation(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer proxyId = (Integer)attributes.get("proxyId");
		//Integer unit = (Integer)attributes.get("unit");
		String functionName = (String)attributes.get("functionName");
		String option = (String)attributes.get("option");
		String param = (String)attributes.get("param");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			Context ctx = ClusterUtil.createContext(js, attributes, functionName, option);
			
			ResourceManager rm = js.getResourceManager();
			TableProxy table = (TableProxy)rm.getProxy(proxyId.intValue());
			Object obj = FunctionLib.executeMemberFunction(table.getTable(), functionName, param, option, ctx);
			if (obj instanceof Table) {
				table.setTable((Table)obj);
			}
			
			return new Response();
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
		
	// 关闭集群内表释放节点机资源
	public void close() {
		Cluster cluster = this.cluster;
		int count = cluster.getUnitCount();
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.CLOSE_MT);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("proxyId", getProxyId(i));
				client.send(command);
			} finally {
				client.close();
			}
		}
	}

	public static Response executeClose(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer proxyId = (Integer)attributes.get("proxyId");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			boolean b = rm.closeProxy(proxyId);
			return new Response(b);
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
}