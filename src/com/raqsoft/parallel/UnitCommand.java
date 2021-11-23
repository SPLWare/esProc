package com.raqsoft.parallel;

import java.io.Serializable;
import java.util.HashMap;

public class UnitCommand implements Serializable {
	private static final long serialVersionUID = 559160970976735495L;

	//public static final int GET_UNITS = 0; // 取分机的所有进程
	public static final int CREATE_GT = 1; // 创建集群组表
	public static final int GET_TABLEMETADATA = 2; // 在分机上创建附表远程代理
	public static final int GET_GT_DISTRIBUTE = 3; // 取组表分布
	public static final int CLOSE_GT = 4; // 关闭集群组表
	public static final int OPEN_GT = 5; // 打开集群组表
	public static final int LIST_FILE_PARTS = 6; // 列出节点机上有哪些分表文件
	
	public static final int MEMORY_GT = 10; // 内存化组表
	public static final int MEMORY_CLUSTERCURSOR = 11; // 内存化集群游标
	public static final int MEMORY_TABLE = 12; // 把远程序表变成集群内表
	
	public static final int CREATE_MT_INDEX = 13; // 远程内表建索引
	public static final int GET_MT_ROW = 14; // 访问内表记录
	public static final int DUP_CLUSTER_MT = 15; // 将集群内表T拼成本地内表
	public static final int DUP_LOCAL_MT = 16; // 把本地内表复制成集群复写内表
	public static final int GET_MT_ROWS = 17; // 取多条内表记录
	public static final int MT_ADD_OPERATION = 18; // 为集群内表附件运算switch
	public static final int CLOSE_MT = 19; // 释放集群内表
	
	public static final int CREATE_BINARY_CURSOR = 20; //创建远程集文件游标
	public static final int CREATE_GT_CURSOR = 21; // 创建远程附表游标，非复写表时seg=-1
	public static final int CREATE_SYNC_GT_CURSOR = 22; // 创建同步分段集群游标
	public static final int CREATE_GT_ICURSOR = 23; // 创建索引游标
	public static final int CREATE_MT_CURSOR = 24; // 由集群内表创建集群游标
	
	public static final int CURSOR_ADD_OPERATION = 30; // 为游标附件运算
	public static final int CURSOR_FETCH = 31; // 游标取数
	public static final int CURSOR_SKIP = 32; // 游标跳过数据
	public static final int CURSOR_CLOSE = 33;
	public static final int CURSOR_GET_MINVALUES = 34;
	public static final int CURSOR_TO_REMOTE = 35; // 把集群游标变成多个远程游标
	
	public static final int GROUPS = 40;
	public static final int JOINX = 41;
	public static final int SORTX = 42;
	public static final int GROUPX = 43;
	public static final int TOTAL = 44;
	
	public static final int CHANNEL_CS = 70; // 由集群游标创建集群管道
	public static final int CHANNEL_CH = 71; // 由集群管道创建集群管道
	public static final int CHANNEL_ADD_OPERATION = 72; // 为管道附件运算
	public static final int CHANNEL_GROUPS = 73;
	public static final int CHANNEL_GROUPX = 74;
	public static final int CHANNEL_SORTX = 75;
	public static final int CHANNEL_FETCH = 76;
	public static final int CHANNEL_RESULT = 77;
	
	public static final int GT_APPEND_BY_CSID = 80; // 由同分布游标做append
	public static final int GT_APPEND_BY_DATA = 81; // 由本地游标做append，根据维值传送相应的数据到节点机做append
	public static final int GT_FIRST_KEY_VALUE = 82;
	public static final int GT_UPDATE = 83;
	public static final int GT_DELETE = 84;
	public static final int GT_DELETE_INDEX = 85;
	public static final int GT_INDEX = 86;
	public static final int GT_RESET = 87;
	public static final int GT_GET_PKEY = 88;
	public static final int GT_NEWS = 89;
	public static final int GT_CGROUPS = 90;//立方体查询
	public static final int GT_GET_COL_NAMES = 91;
	
	public static final int PSEUDO_CURSOR = 101;//虚表建立游标
	public static final int PSEUDO_ADD_COLNAME = 102;
	public static final int PSEUDO_ADD_OPERATION = 103;
	public static final int PSEUDO_CLONE = 104;
	public static final int PSEUDO_CREATE = 105;
	public static final int PSEUDO_JOINX = 106;
	public static final int PSEUDO_SET_MCS = 107;
	public static final int PSEUDO_SET_PATHCOUNT = 108;
	//创建远程临时文件
	public static final int CREATE_TMPFILE = 200;

	//将本机文件localFile传到即时创建的远程临时文件
	public static final int CREATE_TMPFILE_FROM = 201;
	
	//根据首键及各段最小值将各记录写到相应分机的远程临时文件，返回各分机相对应的临时文件名，
	//用RemoteFile直接写远程？还是先写到本地再传？
	//attr: int proxyId, String firstKey, Object[] segMinValues, String[] hosts, int[] ports
	//根据各分机传来的临时文件进行reduce，返回游标proxyId
	public static final int SHUFFLE = 202;
	
	//产生中间表，keys=null产生简表，否则组表
	//attr: int csProxyId, String[] keys
	//return: int 简表或附表proxyId
	public static final int INTERM = 205;
	
	
	//private String jobSpaceId; // 放attributes里
	private int command;
	private HashMap<String, Object> attributes = new HashMap<String, Object>();
	
	public UnitCommand(int command) {
		this.command = command;
	}
		
	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	public Response execute() {
		switch (command) {
			case CREATE_BINARY_CURSOR:
				return PartitionFile.executeCreateBinaryCursor(attributes);
			case CURSOR_ADD_OPERATION:
				return ClusterCursor.executeAddOperation(attributes);
			case CURSOR_FETCH:
				return ClusterCursor.executeFetch(attributes);
			case CURSOR_SKIP:
				return ClusterCursor.executeSkip(attributes);
			case CURSOR_CLOSE:
				return ClusterCursor.executeClose(attributes);
			case CREATE_GT:
				return PartitionFile.executeCreateGroupTable(attributes);
			case OPEN_GT:
				return PartitionFile.executeOpenGroupTable(attributes);
			case LIST_FILE_PARTS:
				return ClusterUtil.executeListFileParts(attributes);
			case GET_GT_DISTRIBUTE:
				return ClusterFile.executeGetDistribute(attributes);
			case CLOSE_GT:
				return ClusterTableMetaData.executeCloseGroupTable(attributes);
			case GET_TABLEMETADATA:
				return ClusterTableMetaData.executeGetTableMetaData(attributes);
			case CREATE_GT_CURSOR:
				return ClusterTableMetaData.executeCreateCursor(attributes);
			case CREATE_SYNC_GT_CURSOR:
				return ClusterTableMetaData.executeCreateSyncCursor(attributes);
			case CREATE_GT_ICURSOR:
				return ClusterTableMetaData.executeICursor(attributes);
			case CREATE_MT_CURSOR:
				return ClusterMemoryTable.executeCreateCursor(attributes);
			case GROUPS:
				return ClusterCursor.executeGroups(attributes);
			case JOINX:
				return ClusterCursor.executeJoinx(attributes);
			case GROUPX:
				return ClusterCursor.executeGroupx(attributes);
			case SORTX:
				return ClusterCursor.executeSortx(attributes);
			case TOTAL:
				return ClusterCursor.executeTotal(attributes);
			case CURSOR_GET_MINVALUES:
				return ClusterCursor.executeGetMinValues(attributes);
			case CURSOR_TO_REMOTE:
				return ClusterCursor.executeGetParallelCursors(attributes);
			case CREATE_MT_INDEX:
				return ClusterMemoryTable.executeCreateIndex(attributes);
			case MEMORY_GT:
				return ClusterTableMetaData.executeMemory(attributes);
			case MEMORY_CLUSTERCURSOR:
				return ClusterCursor.executeMemory(attributes);
			case MEMORY_TABLE:
				return ClusterMemoryTable.executeMemory(attributes);
			case GET_MT_ROW:
				return ClusterMemoryTable.executeGetRow(attributes);
			case DUP_CLUSTER_MT:
				return ClusterMemoryTable.executeDup(attributes);
			case DUP_LOCAL_MT:
				return ClusterMemoryTable.executeDupLocal(attributes);
			case GET_MT_ROWS:
				return ClusterMemoryTable.executeGetRows(attributes);
			case MT_ADD_OPERATION:
				return ClusterMemoryTable.executeAddOperation(attributes);
			case CLOSE_MT:
				return ClusterMemoryTable.executeClose(attributes);
			case CHANNEL_CS:
				return ClusterChannel.executeCreateChannel_CS(attributes);
			case CHANNEL_CH:
				return ClusterChannel.executeCreateChannel_CH(attributes);
			case CHANNEL_ADD_OPERATION:
				return ClusterChannel.executeAddOperation(attributes);
			case CHANNEL_GROUPS:
				return ClusterChannel.executeGroups(attributes);
			case CHANNEL_GROUPX:
				return ClusterChannel.executeGroupx(attributes);
			case CHANNEL_SORTX:
				return ClusterChannel.executeSortx(attributes);
			case CHANNEL_FETCH:
				return ClusterChannel.executeFetch(attributes);
			case CHANNEL_RESULT:
				return ClusterChannel.executeResult(attributes);
			case GT_APPEND_BY_CSID:
				return ClusterTableMetaData.executeAppendByCSID(attributes);
			case GT_FIRST_KEY_VALUE:
				return ClusterTableMetaData.executeGetFirstKeyValue(attributes);
			case GT_APPEND_BY_DATA:
				return ClusterTableMetaData.executeAppendByData(attributes);
			case GT_UPDATE:
				return ClusterTableMetaData.executeUpdate(attributes);
			case GT_DELETE:
				return ClusterTableMetaData.executeDelete(attributes);
			case GT_RESET:
				return PartitionFile.executeResetGroupTable(attributes);
			case GT_INDEX:
				return ClusterTableMetaData.executeCreateIndex(attributes);
			case GT_DELETE_INDEX:
				return ClusterTableMetaData.executeDeleteIndex(attributes);
			case GT_GET_PKEY:
				return ClusterTableMetaData.executeGetPkey(attributes);
			case GT_CGROUPS:
				return ClusterTableMetaData.executeCgroups(attributes);
			case GT_NEWS:
				return ClusterTableMetaData.executeNews(attributes);
			case GT_GET_COL_NAMES:
				return ClusterTableMetaData.executeGetAllColNames(attributes);
			case PSEUDO_CURSOR:
				return ClusterPseudo.executeCreateCursor(attributes);
			case PSEUDO_ADD_OPERATION:
				return ClusterPseudo.executeAddOperation(attributes);
			case PSEUDO_ADD_COLNAME:
				return ClusterPseudo.executeAddColName(attributes);
			case PSEUDO_CLONE:
				return ClusterPseudo.executeClone(attributes);
			case PSEUDO_CREATE:
				return ClusterPseudo.executeCreateClusterPseudo(attributes);
			default:
				throw new RuntimeException();
		}
	}
}
