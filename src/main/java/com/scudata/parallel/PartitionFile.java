package com.scudata.parallel;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileGroup;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dw.ColumnGroupTable;
import com.scudata.dw.GroupTable;
import com.scudata.dw.ITableMetaData;
import com.scudata.dw.RowGroupTable;
import com.scudata.dw.TableMetaData;
import com.scudata.resources.EngineMessage;

/**
 * 节点机分区文件
 * @author RunQian
 *
 */
class PartitionFile {
	private ClusterFile clusterFile; // 所属的集群文件
	private String host; // IP
	private int port; // 端口
	
	//private int partition; // 分区，-1表示直接用文件名取
	private int []parts; // 分表号数组，多个则为复组表，空则直接用文件名取
		
	/**
	 * 构建节点机分区文件
	 * @param clusterFile 所属的集群文件
	 * @param host IP
	 * @param port 端口
	 * @param partition 分区
	 */
	public PartitionFile(ClusterFile clusterFile, String host, int port, int partition) {
		this.clusterFile = clusterFile;
		this.host = host;
		this.port = port;
		
		//this.partition = partition;
		if (partition != -1) {
			parts = new int[] {partition};
		}
	}
	
	public PartitionFile(ClusterFile clusterFile, String host, int port, int []parts) {
		this.clusterFile = clusterFile;
		this.host = host;
		this.port = port;
		this.parts = parts;
	}
	
	/**
	 * 复制对象
	 * @param clusterFile
	 * @return
	 */
	public PartitionFile dup(ClusterFile clusterFile) {
		//return new PartitionFile(clusterFile, host, port, partition);
		return new PartitionFile(clusterFile, host, port, parts);
	}
	
	/**
	 * 创建组表
	 * @param colNames 字段名数组
	 * @param serialBytesLen
	 * @param segmentCol
	 * @param serialLen
	 * @param writePsw
	 * @param readPsw
	 * @param distribute
	 * @param opt
	 * @return
	 */
	public int createGroupTable(String []colNames, String distribute, String opt) {
		UnitClient client = new UnitClient(host, port);
		String fileName = clusterFile.getFileName();
		
		try {
			UnitCommand command = new UnitCommand(UnitCommand.CREATE_GT);
			command.setAttribute("fileName", fileName);
			//command.setAttribute("partition", new Integer(partition));
			command.setAttribute("parts", parts);
			command.setAttribute("jobSpaceId", clusterFile.getJobSpaceId());
			
			command.setAttribute("colNames", colNames);
			command.setAttribute("distribute", distribute);
			command.setAttribute("opt", opt);
			
			Response response = client.send(command);
			Integer id = (Integer)response.checkResult();
			return id.intValue();
		} finally {
			client.close();
		}
	}
	
	/**
	 * 在节点机上执行创建组表命令
	 * @param attributes
	 * @return
	 */
	public static Response executeCreateGroupTable(HashMap<String, Object> attributes) {
		String fileName = (String)attributes.get("fileName");
		//Integer partition = (Integer)attributes.get("partition");
		int []parts = (int[])attributes.get("parts");
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		String []colNames = (String [])attributes.get("colNames");
		String distribute = (String)attributes.get("distribute");
		String opt = (String)attributes.get("opt");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			Context ctx = ClusterUtil.createContext(js);
			FileObject fo = new FileObject(fileName);
			if (parts != null && parts.length == 1) {
				fo.setPartition(parts[0]);
			}
			
			File file = fo.getLocalFile().file();
			if ((opt == null || opt.indexOf('y') == -1) && file.exists()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("file.fileAlreadyExist", fo.getFileName()));
			} else if (opt != null && opt.indexOf('y') != -1 && file.exists()) {
				try {
					GroupTable table = GroupTable.open(file, ctx);
					table.delete();
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
			}
			
			
			GroupTable gt;
			if (opt != null && opt.indexOf('r') != -1) {
				gt = new RowGroupTable(file, colNames, distribute, opt, ctx);
			} else {
				gt = new ColumnGroupTable(file, colNames, distribute, opt, ctx);
			}
			
			if (parts != null && parts.length == 1) {
				gt.setPartition(parts[0]);
			}
			
			TableMetaData table = gt.getBaseTable();
			IProxy proxy = new TableMetaDataProxy(table);
			js.getResourceManager().addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}

	/**
	 * 打开组表
	 * @return 节点机组表代理ID
	 */
	public int openGroupTable() {
		UnitClient client = new UnitClient(host, port);
		
		try {
			UnitCommand command = new UnitCommand(UnitCommand.OPEN_GT);
			command.setAttribute("fileName", clusterFile.getFileName());
			//command.setAttribute("partition", new Integer(partition));
			command.setAttribute("parts", parts);
			command.setAttribute("jobSpaceId", clusterFile.getJobSpaceId());
			Response response = client.send(command);
			Integer id = (Integer)response.checkResult();
			return id.intValue();
		} finally {
			client.close();
		}
	}
	
	/**
	 * 在节点机上执行打开组表命令
	 * @param attributes
	 * @return
	 */
	public static Response executeOpenGroupTable(HashMap<String, Object> attributes) {
		String fileName = (String)attributes.get("fileName");
		//Integer partition = (Integer)attributes.get("partition");
		int []parts = (int[])attributes.get("parts");
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			Context ctx = ClusterUtil.createContext(js);
			ITableMetaData table;
			
			if (parts == null || parts.length <= 1) {
				FileObject fo = new FileObject(fileName);
				if (parts != null && parts.length == 1) {
					fo.setPartition(parts[0]);
				}
	
				File file = fo.getLocalFile().file();
				TableMetaData tmd = GroupTable.openBaseTable(file, ctx);
				table = tmd;
				
				if (parts != null && parts.length == 1) {
					tmd.getGroupTable().setPartition(parts[0]);
				}
			} else {
				FileGroup fileGroup = new FileGroup(fileName, parts);
				table = fileGroup.open(null, ctx);
			}
			
			IProxy proxy = new TableMetaDataProxy(table);
			js.getResourceManager().addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	/**
	 * 创建集群集文件游标
	 * @param fields 选出字段名数组
	 * @param opt 选项
	 * @param segSeq 分段号
	 * @param segCount 分段数
	 * @param unit 节点机序号
	 * @return 节点机游标代理号
	 */
	public int createBinaryCursor(String []fields, String opt, int segSeq, int segCount, int unit) {
		UnitClient client = new UnitClient(host, port);
		String fileName = clusterFile.getFileName();
		
		try {
			UnitCommand command = new UnitCommand(UnitCommand.CREATE_BINARY_CURSOR);
			command.setAttribute("fileName", fileName);
			//command.setAttribute("partition", new Integer(partition));
			command.setAttribute("segSeq", new Integer(segSeq));
			command.setAttribute("segCount", new Integer(segCount));
			command.setAttribute("unit", new Integer(unit));
			command.setAttribute("jobSpaceId", clusterFile.getJobSpaceId());
			command.setAttribute("fields", fields);
			command.setAttribute("opt", opt);
			
			Response response = client.send(command);
			Integer id = (Integer)response.checkResult();
			return id.intValue();
		} finally {
			client.close();
		}
	}
	
	/**
	 * 在节点机上执行创建集文件游标命令
	 * @param attributes
	 * @return
	 */
	public static Response executeCreateBinaryCursor(HashMap<String, Object> attributes) {
		String fileName = (String)attributes.get("fileName");
		//Integer partition = (Integer)attributes.get("partition");
		Integer segSeq = (Integer)attributes.get("segSeq");
		Integer segCount = (Integer)attributes.get("segCount");
		Integer unit = (Integer)attributes.get("unit");
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		String []fields = (String[])attributes.get("fields");
		String opt = (String)attributes.get("opt");
		
		FileObject fo = new FileObject(fileName);
		//if (partition.intValue() > 0) {
		//	fo.setPartition(partition);
		//}
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			Context ctx = ClusterUtil.createContext(js, attributes);
			BFileCursor cursor;
			if (segCount > 1) {
				cursor = new BFileCursor(fo, fields, segSeq, segCount, opt, ctx);
			} else {
				cursor = new BFileCursor(fo, fields, opt, ctx);
			}
			
			IProxy proxy = new CursorProxy(cursor, unit);
			js.getResourceManager().addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
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
	 * @return true：成功，false：失败
	 */
	public boolean resetGroupTable(String file, String option, String distribute) {
		UnitClient client = new UnitClient(host, port);
		
		try {
			UnitCommand command = new UnitCommand(UnitCommand.GT_RESET);
			command.setAttribute("fileName", clusterFile.getFileName());
			//command.setAttribute("partition", new Integer(partition));
			command.setAttribute("parts", parts);
			command.setAttribute("file", file);
			command.setAttribute("option", option);
			command.setAttribute("jobSpaceId", clusterFile.getJobSpaceId());
			command.setAttribute("distribute", distribute);
			
			Response response = client.send(command);
			Boolean result = (Boolean)response.checkResult();
			return result;
		} finally {
			client.close();
		}
	}
	
	/**
	 * 在节点机上执行重设组表命令
	 * @param attributes
	 * @return
	 */
	public static Response executeResetGroupTable(HashMap<String, Object> attributes) {
		String fileName = (String)attributes.get("fileName");
		//Integer partition = (Integer)attributes.get("partition");
		int []parts = (int[])attributes.get("parts");
		String file = (String)attributes.get("file");//new file name
		String option = (String)attributes.get("option");
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		String distribute = (String)attributes.get("distribute");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			Context ctx = ClusterUtil.createContext(js);
			boolean result;
			
			if (parts == null || parts.length <= 1) {
				FileObject fo = new FileObject(fileName);
				if (parts != null && parts.length == 1) {
					fo.setPartition(parts[0]);
				}
				
				GroupTable table = GroupTable.open(fo.getLocalFile().file(), ctx);
				if (file == null) {
					result = table.reset(null, option, ctx, distribute);
				} else {
					FileObject newFile = new FileObject(file);
					if (parts != null && parts.length == 1) {
						newFile.setPartition(parts[0]);
					}
					
					result = table.reset(newFile.getLocalFile().file(), option, ctx, distribute);
				}
			} else {
				FileGroup fileGroup = new FileGroup(fileName, parts);
				if (file == null) {
					result = fileGroup.resetGroupTable(option, ctx);
				} else {
					FileGroup newFileGroup = new FileGroup(file, parts);
					result = fileGroup.resetGroupTable(newFileGroup, option, distribute, ctx);
				}
			}
			
			return new Response(Boolean.valueOf(result));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}

	/**
	 * 取节点机IP
	 * @return String
	 */
	public String getHost() {
		return host;
	}

	/**
	 * 取节点机端口
	 * @return int
	 */
	public int getPort() {
		return port;
	}

	/**
	 * 取分区
	 * @return int
	 */
	public int getPartition() {
		//return partition;
		if (parts != null && parts.length > 0) {
			return parts[0];
		} else {
			return -1;
		}
	}
}
