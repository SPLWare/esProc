package com.raqsoft.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.raqsoft.common.IntArrayList;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.JobSpace;
import com.raqsoft.dm.JobSpaceManager;
import com.raqsoft.dm.ResourceManager;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dw.ITableMetaData;
import com.raqsoft.expression.Expression;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.resources.ParallelMessage;

/**
 * 集群文件
 * @author RunQian
 *
 */
public class ClusterFile implements IClusterObject {
	private Cluster cluster; // 节点机信息
	private String fileName; // 文件名（字符串）或者文件名数组（字符串数组）
	private String opt; // 选项
	
	private PartitionFile []pfs; // 每个分区对应的节点机
	private boolean isDistributedFile = true; // true：分布文件，false：复写文件
	
	private ClusterFile() {
	}
		
	/**
	 * 用于远程文件产生组表
	 * @param host 节点机IP地址
	 * @param port 节点机端口
	 * @param fileName 文件名
	 * @param part 分区
	 * @param ctx 计算上下文
	 */
	public ClusterFile(String host, int port, String fileName, int part, Context ctx) {
		cluster = new Cluster(new String[]{host}, new int[]{port}, ctx);
		pfs = new PartitionFile[1];
		pfs[0] = new PartitionFile(this, host, port, part);
		
		this.fileName = fileName;
		if (part > 0) {
			isDistributedFile = true;
		}
	}

	/**
	 * 构建和指定集群文件同分布的集群文件
	 * @param clusterFile 参照集群文件
	 * @param fileName 文件名
	 * @param parts 分区数组，省略则用参照的集群文件的分区
	 * @param opt 选项
	 */
	public ClusterFile(ClusterFile clusterFile, String fileName, int []parts, String opt) {
		this.fileName = fileName;
		this.opt = opt;
		
		if (parts == null || parts.length == 0) {
			isDistributedFile = clusterFile.isDistributedFile;
			cluster = clusterFile.cluster;
			int count = clusterFile.pfs.length;
			pfs = new PartitionFile[count];
			
			for (int i = 0; i < count; ++i) {
				pfs[i] = clusterFile.pfs[i].dup(this);
			}
		} else {
			isDistributedFile = true;
			int count = parts.length;
			String []hosts = new String[count];
			int []ports = new int[count];
			pfs = new PartitionFile[count];
			
			Next:
			for (int i = 0; i < count; ++i) {
				for (PartitionFile pf : clusterFile.pfs) {
					if (pf.getPartition() == parts[i]) {
						pfs[i] = pf.dup(this);
						hosts[i] = pf.getHost();
						ports[i] = pf.getPort();
						continue Next;
					}
				}
				
				MessageManager mm = ParallelMessage.get();
				throw new RQException(mm.getMessage("PartitionUtil.lackfile2", fileName, parts[i]));
			}
			
			Context ctx = clusterFile.cluster.getContext();
			cluster = new Cluster(hosts, ports, ctx);
		}
	}
	
	/**
	 * 从给出的节点机列表中选出含有指定分表文件的节点机，创建集群文件
	 * @param cluster 节点机信息
	 * @param fileName 文件名
	 * @param parts 分区数组
	 * @param opt 选项
	 */
	public ClusterFile(Cluster cluster, String fileName, int []parts, String opt) {
		this.fileName = fileName;
		this.opt = opt;
		isDistributedFile = true;
		
		String []hosts = cluster.getHosts();
		int []ports = cluster.getPorts();
		int hcount = hosts.length;
		int pcount = parts.length;
		pfs = new PartitionFile[pcount];

		// 集群可写入文件，z和hs一一对应
		if (opt != null && opt.indexOf('w') != -1) {
			if (hcount != pcount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("file" + mm.getMessage("function.paramCountNotMatch"));
			}
			
			for (int i = 0; i < hcount; ++i) {
				pfs[i] = new PartitionFile(this, hosts[i], ports[i], parts[i]);
			}
			
			this.cluster = cluster;
			return;
		}
		
		// 列出每个分区都有哪些节点机有
		List<IntArrayList> hostList = new ArrayList<IntArrayList>();
		for (int i = 0; i < hcount; ++i) {
			try {
				int []partList = ClusterUtil.listFileParts(hosts[i], ports[i], fileName, parts);
				for (int p : partList) {
					for (int size = hostList.size(); size <= p; ++size) {
						hostList.add(new IntArrayList());
					}
					
					hostList.get(p).addInt(i);
				}
			} catch (Exception e) {
				// 有异常发生时不再使用此节点机，继续循环
			}
		}
		
		// 每个分区选择一个节点机，平均分配
		int []weights = new int[hcount];
		String []useHosts = new String[pcount];
		int []usePorts = new int[pcount];
		
		for (int i = 0; i < pcount; ++i) {
			int p = parts[i];
			if (hostList.size() <= p) {
				MessageManager mm = ParallelMessage.get();
				throw new RQException(mm.getMessage("PartitionUtil.lackfile2", fileName, p));
			}
			
			IntArrayList list = hostList.get(p);
			int size = list.size();
			if (size == 0) {
				MessageManager mm = ParallelMessage.get();
				throw new RQException(mm.getMessage("PartitionUtil.lackfile2", fileName, p));
			}
			
			int h = list.getInt(0);
			for (int j = 1; j < list.size(); ++j) {
				int cur = list.getInt(j);
				if (weights[cur] < weights[h]) {
					h = cur;
				}
			}
			
			weights[h]++;
			pfs[i] = new PartitionFile(this, hosts[h], ports[h], p);
			useHosts[i] = hosts[h];
			usePorts[i] = ports[h];
		}
		
		this.cluster = new Cluster(useHosts, usePorts, cluster.getContext());
	}
	
	/**
	 * 构建集群分布写文件
	 * @param cluster 节点机信息
	 * @param fileName 文件名
	 * @param opt 选项，w：创建分布写文件
	 */
	public ClusterFile(Cluster cluster, String fileName, String opt) {
		this.cluster = cluster;
		this.fileName = fileName;
		this.opt = opt;
		isDistributedFile = true;
		
		String []hosts = cluster.getHosts();
		int []ports = cluster.getPorts();
		int hcount = hosts.length;
		pfs = new PartitionFile[hcount];
		
		for (int i = 0; i < hcount; ++i) {
			pfs[i] = new PartitionFile(this, hosts[i], ports[i], i + 1);
		}
	}
	
	/**
	 * 由指定文件名创建与当前集群文件同分布的集群文件
	 * @param pathName 文件名
	 * @return 集群文件
	 */
	public ClusterFile newFile(String pathName) {
		ClusterFile clusterFile = new ClusterFile();
		clusterFile.cluster = cluster;
		clusterFile.fileName = pathName;
		clusterFile.isDistributedFile = isDistributedFile;
		clusterFile.opt = opt;
		
		PartitionFile []pfs = this.pfs;
		int count = pfs.length;
		clusterFile.pfs = new PartitionFile[count];
		for (int i = 0; i < count; ++i) {
			clusterFile.pfs[i] = pfs[i].dup(clusterFile);
		}
		
		return clusterFile;
	}
	
	/**
	 * 返回是否是分布文件
	 * @return true：分布文件，false：复写文件
	 */
	public boolean isDistributedFile() {
		return isDistributedFile;
	}
	
	/**
	 * 取节点机数
	 * @return
	 */
	public int getUnitCount() {
		return cluster.getUnitCount();
	}
	
	/**
	 * 取集群文件对应的每个节点机的分表文件
	 * @return
	 */
	public PartitionFile[] getPartitionFiles() {
		return pfs;
	}
	
	//根据序号取分机
	public String getHost(int unit) {
		return cluster.getHost(unit);
	}
	
	/**
	 * 取指定节点机的端口
	 * @param unit
	 * @return
	 */
	public int getPort(int unit) {
		return cluster.getPort(unit);
	}
	
	// 文件名（字符串）或者文件名数组（字符串数组）
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * 取选项
	 * @return
	 */
	public String getOption() {
		return opt;
	}
	
	/**
	 * 取计算上下文
	 * @return
	 */
	public Context getContext() {
		return cluster.getContext();
	}
	
	/**
	 * 取任务空间标识
	 * @return
	 */
	public String getJobSpaceId() {
		return cluster.getJobSpaceId();
	}
	
	/**
	 * 取集群节点机
	 */
	public Cluster getCluster() {
		return cluster;
	}
	
	/**
	 * 产生基于集文件的集群游标
	 * @param fields 要读取的字段
	 * @param opt
	 * @return
	 */
	public ClusterCursor createBinaryCursor(String []fields, String opt) {
		int count = pfs.length;
		int []proxyIds = new int[count];
		boolean isDistributed = isDistributedFile;
		if (!isDistributed && opt != null && opt.indexOf('z') != -1) {
			isDistributed = true;
			for (int i = 0; i < count; ++i) {
				proxyIds[i] = pfs[i].createBinaryCursor(fields, opt, i + 1, count, i);
			}
		} else {
			for (int i = 0; i < count; ++i) {
				proxyIds[i] = pfs[i].createBinaryCursor(fields, opt, 0, 0, i);
			}
		}
		
		return new ClusterCursor(this, proxyIds, isDistributed);
	}
	
	/**
	 * 创建集群组表
	 * @param colNames 字段名数组
	 * @param serialBytesLen 排号键字段长度数组
	 * @param segmentCol 分段字段
	 * @param serialLen 如果分段字段是排号，则指定采用的排号长度
	 * @param writePsw 写密码
	 * @param readPsw 读密码
	 * @param distribute 分区表达式
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return 集群组表
	 */
	public ClusterTableMetaData createGroupTable(String []colNames, Expression distribute, String opt, Context ctx) {
		int count = pfs.length;
		int []proxyIds = new int[count];
		String dis = distribute == null ? null : distribute.toString();
		
		for (int i = 0; i < count; ++i) {
			proxyIds[i] = pfs[i].createGroupTable(colNames, dis, opt);
		}
		
		ClusterTableMetaData table = new ClusterTableMetaData(this, proxyIds, ctx);
		table.setDistribute(distribute);
		return table;
	}
	
	/**
	 * 打开组表
	 * @param ctx 计算上下文
	 * @return 集群组表
	 */
	public ClusterTableMetaData openGroupTable(Context ctx) {
		int count = pfs.length;
		int []proxyIds = new int[count];
		for (int i = 0; i < count; ++i) {
			proxyIds[i] = pfs[i].openGroupTable();
		}
		
		UnitClient client = new UnitClient(cluster.getHost(0), cluster.getPort(0));
		Expression distribute = null;
		
		try {
			UnitCommand command = new UnitCommand(UnitCommand.GET_GT_DISTRIBUTE);
			command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
			command.setAttribute("tmdProxyId", new Integer(proxyIds[0]));
			
			Response response = client.send(command);
			String str = (String)response.checkResult();
			if (str != null) {
				distribute = new Expression(ctx, str);
			}
		} finally {
			client.close();
		}
		
		ClusterTableMetaData table = new ClusterTableMetaData(this, proxyIds, ctx);
		table.setDistribute(distribute);
		return table;
	}
	
	/**
	 * 在节点机上执行取分布表达式
	 * @param attributes
	 * @return
	 */
	public static Response executeGetDistribute(HashMap<String, Object> attributes) {
		String jobSpaceID = (String) attributes.get("jobSpaceId");
		Integer tmdProxyId = (Integer) attributes.get("tmdProxyId");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			TableMetaDataProxy tmd = (TableMetaDataProxy) rm.getProxy(tmdProxyId.intValue());
			ITableMetaData table = tmd.getTableMetaData();
			String distribute = table.getDistribute();
			return new Response(distribute);
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	/**
	 * 整理组表
	 * @param file 新组表对应的文件，省略则覆盖源文件
	 * @param option 选项
	 * @param distribute 新分布表达式
	 * @return 结果序列
	 */
	public Sequence resetGroupTable(String file, String option, String distribute) {
		if (!isDistributedFile()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.needDistributed"));
		}
		
		Sequence result = new Sequence();
		int count = pfs.length;
		for (int i = 0; i < count; ++i) {
			result.add(pfs[i].resetGroupTable(file, option, distribute));
		}
		
		return result;
	}
}