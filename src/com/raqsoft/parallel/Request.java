package com.raqsoft.parallel;

import java.util.*;
import java.io.*;

/**
 * 请求命令
 * 命令分为n类,命令为全部大写，前缀为类型，后缀为命令标识 命令参数有命令标识做前缀，后缀为首字母大写的参数标识
 */
public class Request implements Serializable {
	private static final long serialVersionUID = 559160970976632495L;

	// 命令类型定义
	public static final int TYPE_SERVER = 0; // 服务器相关命令
	public static final int TYPE_DFX = 10000; // 执行dfx
	public static final int TYPE_CURSOR = 20000; // 游标服务
	public static final int TYPE_FILE = 30000; // 远程文件服务
	public static final int TYPE_PARTITION = 40000; // 远程分区文件服务
	public static final int TYPE_ZONE = 50000; // 内存加载区服务
	public static final int TYPE_UNITCOMMAND = 60000; // 王调用的UnitCommand
	public static final int TYPE_JDBC = 70000; // JDBC接口

	// 服务器相关命令
	public static final int SERVER_SHUTDOWN = 1 + TYPE_SERVER; // 停止服务器
	public static final int SERVER_LISTTASK = 2 + TYPE_SERVER; // 列出任务列表,应答Table
	public static final int SERVER_CANACCEPTTASK = 3 + TYPE_SERVER; // 探测分机能否接受任务，应答boolean
	public static final int SERVER_GETTASKNUMS = 4 + TYPE_SERVER; //获取适合作业数和当前作业数，应答int[2]
	public static final String CANACCEPTTASK_DispatchedCount = "Dispatched count";// 已经在主叫机器上分配了的任务数，后台的最优并行数必须大于已经分配的

	public static final int SERVER_LISTPARAM = 5 + TYPE_SERVER; // 列出任务机的全局变量表,应答HashMap:
																// String
																// spaceId-
																// Param[]
	public static final int SERVER_GETCONCURRENTCOUNT = 6 + TYPE_SERVER; // 列出任务机的任务数目,应答Integer
	
//清除服务器端的垃圾代理器(用户没有正常关闭的代理或者网络断掉后的代理)
	public static final int SERVER_CLOSESPACE = 10 + TYPE_SERVER; // 关闭分机的变量空间
	public static final String CLOSESPACE_SpaceId = "Space id";

	public static final int SERVER_FETCHDIMS = 20 + TYPE_SERVER; // 从分机获取维表,应答
																	// Sequence
	public static final String FETCHDIMS_SpaceId = "Space id";// String
	public static final String FETCHDIMS_DimVarName = "Dim var name";// String
	public static final String FETCHDIMS_KeySequence = "Key sequence";// Sequence
	public static final String FETCHDIMS_NewExps = "New exps";// String[]
	public static final String FETCHDIMS_NewNames = "New names";// String[]

	public static final int SERVER_FETCHCLUSTERTABLE = 21 + TYPE_SERVER; // 从分机获取集群表,应答   Sequence
	public static final String FETCHCLUSTERTABLE_SpaceId = "Space id";// String
	public static final String FETCHCLUSTERTABLE_TableName = "Table name";// String
	public static final String FETCHCLUSTERTABLE_Seqs = "Seqs";// int[],表的记录序号
	public static final String FETCHCLUSTERTABLE_KeySequence = "Key sequence";// Sequence
	public static final String FETCHCLUSTERTABLE_NewExps = "New exps";// String[]
	public static final String FETCHCLUSTERTABLE_NewNames = "New names";// String[]
	public static final String FETCHCLUSTERTABLE_Filter = "Filter";// String

	public static final int SERVER_GETTABLEMEMBERS = 22 + TYPE_SERVER; // 获取分机上内存表的成员个数,应答   int
	public static final String GETTABLEMEMBERS_SpaceId = "Space id";// String
	public static final String GETTABLEMEMBERS_TableName = "Table name";// String

	public static final int SERVER_GETUNITS_MAXNUM = 50 + TYPE_SERVER; // 列出当前分机的最大作业数
	
	public static final int SERVER_GETAREANO = 54 + TYPE_SERVER; // 获取分机的内存区号
	public static final String GETAREANO_TaskName = "Task name";// String
	
	// DFX相关
	public static final int DFX_TASK = 1 + TYPE_DFX; // 创建一个dfx任务，应答Integer，任务号
	public static final String TASK_DfxName = "Dfx name";
	public static final String TASK_ArgList = "Arg list";
	public static final String TASK_SpaceId = "Space id";
	public static final String TASK_IsProcessCaller = "TASK_IsProcessCaller";
	public static final String TASK_ProcessTaskId = "Process task id";
	public static final String TASK_Reduce = "Reduce";
	public static final String TASK_AccumulateLocation = "AccumulateLocation";
	public static final String TASK_CurrentLocation = "CurrentLocation";

	public static final int DFX_CALCULATE = 2 + TYPE_DFX; // 计算一个任务，应答Object，任务的执行结果
	public static final String CALCULATE_TaskId = "Task id";

	public static final int DFX_CANCEL = 3 + TYPE_DFX; // 取消一个正在执行的dfx文件，应答boolean，取消是否成功
	public static final String CANCEL_TaskId = "Task id";
	public static final String CANCEL_Reason = "Task reason";

	public static final int DFX_GET_REDUCE = 4 + TYPE_DFX; //获取任务在分机上的reduce结果，应答Object
	public static final String GET_REDUCE_SpaceId = "Space id";

	// 游标服务
	public static final int CURSOR_METHOD = 1 + TYPE_CURSOR;// 执行游标的一个代理方法，应答:Object方法的返回值
	public static final String METHOD_TaskId = "Task id";
	public static final String METHOD_ProxyId = "Proxy id";
	public static final String METHOD_MethodName = "Method name";
	public static final String METHOD_ArgValues = "Arg values";

	// 远程文件服务
	public static final int FILE_GETPROPERTY = 1 + TYPE_FILE; // 获取文件的相关属性，应答：HashMap文件的相应属性
	public static final String GETPROPERTY_FileName = "File name";
	public static final String GETPROPERTY_Opt = "Options";

	public static final int FILE_DELETE = 2 + TYPE_FILE; // 删除指定文件，应答：boolean
	public static final String DELETE_FileName = "File name";

	public static final int FILE_OPEN = 3 + TYPE_FILE; // 打开远程文件，应答：int 文件句柄
	public static final String OPEN_Partition = "Partition";
	public static final String OPEN_FileName = "File name";
	public static final String OPEN_Opt = "Opt";
	public static final String OPEN_IsAppend = "Is append";

	public static final int FILE_READ = 4 + TYPE_FILE; // 读取文件的一块数据，应答：byte[]
	public static final String READ_Handle = "handle";
	public static final String READ_BufferSize = "Buffer size";

	public static final int FILE_CLOSE = 5 + TYPE_FILE; // 关闭文件句柄，应答：boolean
	public static final String CLOSE_Handle = "handle";

	public static final int FILE_WRITE = 6 + TYPE_FILE; // 写文件的一块数据，应答：无
	public static final String WRITE_Handle = "handle";
	public static final String WRITE_Bytes = "bytes";

	public static final int FILE_POSITION = 7 + TYPE_FILE; // 获取随机输出文件的当前位置，应答：long，位置
	public static final String POSITION_Handle = "handle";

	public static final int FILE_SETPOSITION = 8 + TYPE_FILE; // 设置随机输出文件的当前位置，应答：无
	public static final String SETPOSITION_Handle = "handle";
	public static final String SETPOSITION_Position = "position";

	public static final int FILE_TRYLOCK = 9 + TYPE_FILE; // 尝试锁定文件，应答：布尔值
	public static final String TRYLOCK_Handle = "handle";

	public static final int FILE_FROM_HANDLE = 10 + TYPE_FILE; // 从远程输出流打开的文件中，打开输入流
	public static final String FROM_Handle = "From handle";
	public static final String FROM_Pos = "From position";
	
	public static final int FILE_LOCK = 11 + TYPE_FILE; // 锁定文件，应答：布尔值
	public static final String LOCK_Handle = "handle";

	public static final int FILE_DIRECTREAD = 100 + TYPE_FILE; // 直接快速读取文件
	public static final String DIRECTREAD_FileName = "fileName";
	public static final String DIRECTREAD_Partition = "partition";

	// 远程分区文件服务
	public static final int PARTITION_LISTFILES = 2 + TYPE_PARTITION; // 列出分机的文件信息，应答List<FileInfo>,符合条件的文件列表
	public static final String LISTFILES_Path = "path";

	public static final int PARTITION_DELETE = 3 + TYPE_PARTITION; // 删除分机的文件
//	public static final String DELETE_FileName = "FILE NAME";//以及有同名定义
	public static final String DELETE_Option = "Option";

	public static final int PARTITION_UPLOAD = 8 + TYPE_PARTITION; // 往分机上传一个文件
	public static final String UPLOAD_DstPath = "Dest path";
	public static final String UPLOAD_LastModified = "Last Modified";
	public static final String UPLOAD_IsMove = "Is move";//如果是移动模式，则不比较LastModified
	public static final String UPLOAD_IsY = "Is y";//IsY状态时，强制覆盖，否则报错

	public static final int PARTITION_SYNCTO = 11 + TYPE_PARTITION; // 将本地路径p下的文件同步到分机组machines
	public static final String SYNC_Machines = "Machines";
	public static final String SYNC_Path = "Path";

	public static final int PARTITION_MOVEFILE = 14 + TYPE_PARTITION; // MOVEFILE
	public static final String MOVEFILE_Machines = "machines";
	public static final String MOVEFILE_Filename = "file name";
	public static final String MOVEFILE_Partition = "Partition";
	public static final String MOVEFILE_DstPath = "Dest path";
	public static final String MOVEFILE_Option = "Option";

	public static final int PARTITION_UPLOAD_DFX = 20 + TYPE_PARTITION; 
	// 该命令上传一个文件到分机的主路径Env.getMainPath()下，跟分区无关，但是也是跟分区一样的上传文件，参数不一致，放在分区管理里面
	// 该命令的上传动作基本等同于PARTITION_UPLOAD
	public static final String UPLOAD_DFX_RelativePath = "Relative Path";//上传到目的机的该路径下，直接相对于主路径时，该参数为空
	public static final String UPLOAD_DFX_LastModified = UPLOAD_LastModified;

	public static final int PARTITION_UPLOAD_CTX = 30 + TYPE_PARTITION; 
	// 该命令上传一个组表、索引文件到分机的主路径；上传中进行优化，只同步增量和修改的部分
	public static final String UPLOAD_FileSize = "File size";
	public static final String UPLOAD_FileType = "File_type";
	public static final String UPLOAD_BlockLinkInfo = "Block link info";
	public static final String UPLOAD_HasExtFile = "Has Ext File";
	public static final String UPLOAD_ExtFileLastModified = "Ext File Last Modified";
	
	// 内存加载区服务
	public static final int ZONE_INITDFX = 2 + TYPE_ZONE; // 在分机上执行一个加载内存区的dfx程序,返回boolean，执行是否完成
	public static final String EXECDFX_ArgList = "Arg list";
	public static final String EXECDFX_SpaceId = "Space id";

	// 王的UnitCommand
	public static final int UNITCOMMAND_EXE = 1+ TYPE_UNITCOMMAND; // UnitCommand相关命令
	public static final String EXE_Object = "Command Object";
	
//	JDBC接口
	public static final int JDBC_CONNECT = 1+ TYPE_JDBC; // 获取连接号，产生连接代理，返回值：整数，连接号
	public static final String CONNECT_spaceID = "connect spaceId";//spaceId
	
	public static final int JDBC_PREPARE = 2+ TYPE_JDBC; // prepareStatement,返回值：整数，statement号
	public static final String PREPARE_connID = "prepare connId";//connId
	public static final String PREPARE_CMD = "prepare cmd";//JDBC cmds
	public static final String PREPARE_Args = "prepare args";//Object[]
	public static final String PREPARE_ENV = "prepare env";//Map<String, Object> envParams
	public static final String PREPARE_ENV_SQLFIRST = "sqlfirst";
	
	public static final int JDBC_EXECUTE = 3+ TYPE_JDBC; // 执行dfx,返回值：整数组，结果集号
	public static final String EXECUTE_connID = "execute connId";//connId
	public static final String EXECUTE_stateID = "execute stateId";//stateId
	
	public static final int JDBC_CANCEL = 4+ TYPE_JDBC; // 取消执行dfx,返回值：无
	public static final String CANCEL_connID = EXECUTE_connID;//connId
	public static final String CANCEL_stateID = EXECUTE_stateID;//stateId

	public static final String JDBC_ISPLUS = "isplus";

	public static final int JDBC_GETTABLES = 5+ TYPE_JDBC; // 读取表信息
	public static final String GETTABLES_connID = "getTables connId";//connId
	public static final String GETTABLES_tableNamePattern = "tableNamePattern";

	public static final int JDBC_GETCOLUMNS = 6+ TYPE_JDBC; // 读取字段信息
	public static final String GETCOLUMNS_connID = "getTables connId";//connId
	public static final String GETCOLUMNS_tableNamePattern = "tableNamePattern";
	public static final String GETCOLUMNS_columnNamePattern = "columnNamePattern";

	public static final int JDBC_CLOSESTATEMENT = 11+ TYPE_JDBC; //关闭Statement
	public static final int JDBC_CLOSECONNECTION = 12+ TYPE_JDBC; //关闭Connection
	public static final String CLOSE_connID = EXECUTE_connID;//connId
	public static final String CLOSE_stateID = EXECUTE_stateID;//stateId

	private int action;
	private Map attrs = new HashMap();

	/**
	 * 创建一个请求
	 * @param action 请求命令
	 */
	public Request(int action) {
		this.action = action;
	}

	/**
	 * 请求是否短连接
	 * @return true
	 */
	public boolean isShortConnectCmd() {
		return true;
	}

	/**
	 * 根据当前的请求编号，取出请求类型
	 * @return 请求类型
	 */
	public int getActionType() {
		if (action > TYPE_JDBC) {
			return TYPE_JDBC;
		}
		if (action > TYPE_UNITCOMMAND) {
			return TYPE_UNITCOMMAND;
		}
		if (action > TYPE_ZONE) {
			return TYPE_ZONE;
		}
		if (action > TYPE_PARTITION) {
			return TYPE_PARTITION;
		}
		if (action > TYPE_FILE) {
			return TYPE_FILE;
		}
		if (action > TYPE_CURSOR) {
			return TYPE_CURSOR;
		}
		if (action > TYPE_DFX) {
			return TYPE_DFX;
		}
		return TYPE_SERVER;
	}

	/**
	 * 取请求命令
	 * @return 命令编号
	 */
	public int getAction() {
		return action;
	}

	/**
	 * 设置请求命令
	 * @param action 命令编号
	 */
	public void setAction(int action) {
		this.action = action;
	}

	/**
	 * 取请求的属性表
	 * @return 属性表
	 */
	public Map getAttrs() {
		return attrs;
	}

	/**
	 * 取请求的属性值
	 * @param attr 属性
	 * @return 属性值
	 */
	public Object getAttr(String attr) {
		return attrs.get(attr);
	}

	/**
	 * 设置属性的值
	 * @param attr 属性
	 * @param value 属性值
	 */
	public void setAttr(String attr, Object value) {
		attrs.put(attr, value);
	}

	private String getAttrString(){
		if(attrs.isEmpty()) return "Attr is empty/";
		Iterator it = attrs.keySet().iterator();
		StringBuffer sb = new StringBuffer();
		while(it.hasNext()){
			sb.append("\r\n");
			String key = (String)it.next();
			sb.append(key);
			sb.append("=");
			Object val = attrs.get(key);
			sb.append(val);
		}
		return sb.toString();
	}
	
	/**
	 * 实现toString描述信息
	 */
	public String toString() {
		return "Request action:" + action+getAttrString();
	}
}
