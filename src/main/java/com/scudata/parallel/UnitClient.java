package com.scudata.parallel;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Env;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.Param;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;

/**
 * 分机的访问客户端
 * @author Joancy
 *
 */
public class UnitClient implements Serializable {
	private static final long serialVersionUID = 1L;

	String host = null;
	int port = 0;

	boolean isDispatchUC = false;
	// 取消长
	transient SocketData socketData = null;

	/**
	 * 根据地址和端口构造一个分机客户端
	 * @param host 主机IP
	 * @param port 端口号
	 */
	public UnitClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * 根据主机描述串(形如：  host:ip)构造分机客户端
	 * @param add 主机描述串
	 */
	public UnitClient(String add) {
		if (add != null) {
			int index = add.lastIndexOf(':');
			if (index > 0) {
				host = add.substring(0, index).trim();
				port = Integer.parseInt(add.substring(index + 1).trim());
			}
		}
	}

	/**
	 * 设置分机为可重分配的客户端
	 * 注意：只有通过getClient或者getRandomClient方法获取的UC才允许为true，
	 * 为true表示该uc是在队列里面维持生产消费者模型，分配时减少队列，计算完成再释放到队列。
	 */
	public void setDispatchable() {
		isDispatchUC = true;
	}

	/**
	 * 判断分机是否可重分配作业
	 * @return 允许返回true，否则返回false
	 */
	public boolean isDispatchable() {
		return isDispatchUC;
	}

	/**
	 * 获取主机IP
	 * @return IP串
	 */
	public String getHost() {
		return host;
	}

	/**
	 * 获取主机端口号
	 * @return 端口号
	 */
	public int getPort() {
		return port;
	}

	/**
	 * 克隆一个分机客户端
	 * @return 克隆后的分机客户端
	 */
	public UnitClient clone() {
		UnitClient uc = new UnitClient(host, port);
		return uc;
	}

	/**
	 * 判断两个分机客户端是否相等
	 * @param nodeHost 另一个分机IP
	 * @param nodePort 另一个分机端口号
	 * @return 相同时返回true，否则返回false
	 */
	public boolean equals(String nodeHost, int nodePort) {
		return host.equalsIgnoreCase(nodeHost) && port == nodePort;
	}

	/**
	 * 判断当前分机是否已经启动，可访问
	 * @return 可访问返回true，否则返回false
	 */
	public boolean isReachable() {
		try {
			InetAddress address = InetAddress.getByName(host);
			boolean isArrived = address.isReachable(2000);
			return isArrived;
		} catch (Exception x) {
			Logger.debug("Ping " + host + " failure.", x);
			return false;
		}
	}

	/**
	 * 检查当前分机是否存活
	 * @return 存活时返回true，否则返回false
	 */
	public boolean isAlive() {
		return isAlive(null);
	}

	/**
	 * 判断当前分机是否位于本机
	 * @return 是本地机返回true，否则返回false
	 */
	public boolean isEqualToLocal() {
		// callx支持 :, ""写法代表使用本地线程
		if (host == null)
			return true;

		HostManager hostManager = HostManager.instance();
		return host.equals(hostManager.getHost())
				&& port == hostManager.getPort();
	}

	/**
	 * 判断分机是否存活，如果异常，将异常原因写入原因缓冲reason
	 * @param reason 原因缓冲
	 * @return 存活时返回true，否则返回false
	 */
	public boolean isAlive(StringBuffer reason) {
		if (isEqualToLocal()) {
			return true;
		}
		SocketData sd = null;

		try {
			sd = newSocketData();
		} catch (Exception x) {
			if (reason != null) {
				if (!isReachable()) {
					reason.append(this + " is not exist.\n");
				} else {
					reason.append("UnitServer or UnitServerConsole is not started on "
							+ this + "\n");
				}
			}
			return false;
		} finally {
			if (sd != null) {
				try {
					sd.clientClose();
				} catch (Exception x) {
				}
				sd = null;
			}
		}
		return true;
	}

	/**
	 * 连接到分机
	 * @throws Exception 连接出错时抛出异常
	 */
	public void connect() throws Exception {
		if (!isEqualToLocal()) {
			socketData = newSocketData();
		}
	}

	/**
	 * 判断是否已经连接到分机
	 * @return 连接好后返回true，否则返回false
	 */
	public boolean isConnected() {
		return !isClosed() || isEqualToLocal();// 本机线程也算联机状态
	}

	/**
	 * 往分机写出一个对象
	 * @param obj 数据对象
	 * @throws Exception 写出错时抛出异常
	 */
	public void write(Object obj) throws Exception {
		socketData.write(obj);
	}

	/**
	 * 从分机读入一个对象
	 * @return 对象数据
	 * @throws Exception 读出错时抛出异常
	 */
	public Object read() throws Exception {
		return socketData.read();
	}

	/**
	 * 创建一个新的通讯套接字
	 * @return 通讯套接字
	 * @throws Exception 出错时抛出异常
	 */
	public SocketData newSocketData() throws Exception {
		Socket s = new Socket();
		SocketData sd = new SocketData(s);
		InetSocketAddress isa = new InetSocketAddress(host, port);
		sd.connect(isa, 5000);

		return sd;
	}

	/**
	 * 取消当前任务
	 * @param taskId 任务编号
	 * @param reason 取消原因
	 */
	public void cancel(Integer taskId, String reason) {
		Request req = new Request(Request.DFX_CANCEL);
		req.setAttr(Request.CANCEL_TaskId, taskId);
		req.setAttr(Request.CANCEL_Reason, reason);
		try {
			sendByNewSocket(req);
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	/**
	 * 停止服务器
	 */
	public void shutDown() {
		Request req = new Request(Request.SERVER_SHUTDOWN);
		try {
			sendByNewSocket(req);
		} catch (Exception x) {
		}
	}

	/**
	 * 列出当前分机的最大作业数
	 * @return 最大作业数
	 */
	public int getUnitMaxNum() {
		if (isEqualToLocal()) {
			return HostManager.maxTaskNum;
		}
		Request req = new Request(Request.SERVER_GETUNITS_MAXNUM);
		try {
			Response res = send(req);
			return (Integer) res.getResult();
		} catch (Exception x) {
			throw new RQException(x);
		}
	}

	/**
	 * 获取分机上任务J的内存区号
	 * @param J 任务名称
	 * @return 内存区号
	 */
	public Integer getAreaNo(String J) {
		Request req = new Request(Request.SERVER_GETAREANO);
		req.setAttr(Request.GETAREANO_TaskName, J);
		try {
			Response res = sendByNewSocket(req);
			return (Integer) res.getResult();
		} catch (Exception x) {
			throw new RQException(x);
		}
	}

	/**
	 * spaceId - Param[]
	 * 
	 * @return HashMap
	 */
	public Table getEnvParamList() {
		Request req = new Request(Request.SERVER_LISTPARAM);
		try {
			Response res = sendByNewSocket(req);
			if (res.getException() != null) {
				throw res.getException();
			}
			return (Table) res.getResult();
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}

	public Table getTaskList() {
		Request req = new Request(Request.SERVER_LISTTASK);
		try {
			Response res = sendByNewSocket(req);
			if (res.getException() != null) {
				throw res.getException();
			}
			return (Table) res.getResult();
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}

	public void closeSpace(String spaceId) {
		if (!isAlive()) {
			return;
		}
		Request req = new Request(Request.SERVER_CLOSESPACE);
		req.setAttr(Request.CLOSESPACE_SpaceId, spaceId);
		try {
			Response res = sendByNewSocket(req);
			if (res.getException() != null) {
				throw res.getException();
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	public void initNode(int i, int N, String j) throws Exception {
		Logger.debug("Before init zone: " + i + " on " + this);

		Request req = new Request(Request.ZONE_INITDFX);
		// req.setAttr(Request.EXECDFX_DfxName, "init.dfx");
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(i);
		args.add(N);
		args.add(j);
		req.setAttr(Request.EXECDFX_ArgList, args);
		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		Logger.debug("Init zone: " + i + " on " + this + " OK.");
	}

	public int getMemoryTableLength(String spaceId, String table)
			throws Exception {
		Request req = new Request(Request.SERVER_GETTABLEMEMBERS);
		req.setAttr(Request.GETTABLEMEMBERS_SpaceId, spaceId);
		req.setAttr(Request.GETTABLEMEMBERS_TableName, table);
		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return ((Integer) res.getResult()).intValue();
	}

	/**
	 * 获取任务空间号在分机上的reduce结果
	 * @param spaceId 任务空间号
	 * @return reduce后的计算结果
	 * @throws Exception
	 */
	public Object getReduceResult(String spaceId) throws Exception {
		Request req = new Request(Request.DFX_GET_REDUCE);
		req.setAttr(Request.GET_REDUCE_SpaceId, spaceId);
		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return res.getResult();
	}

	/**
	 * 能否接受任务，保持连接时，才能调用 connect(); canAcceptTask(); close();
	 * 
	 * @param count
	 *            Integer
	 * @throws Exception
	 * @return boolean
	 */
	private transient Request accTask = null;

	public int[] getTaskNums() throws Exception {
		if (accTask == null) {
			accTask = new Request(Request.SERVER_GETTASKNUMS);
		} else {
			accTask.setAction(Request.SERVER_GETTASKNUMS);
		}
		Response res = send(accTask);

		if (res.getException() != null) {
			throw res.getException();
		}
		return (int[]) res.getResult();
	}

	public static Sequence getMemoryTable(String spaceId, String tableName,
			String nodeDesc) throws Exception {
		JobSpace space = JobSpaceManager.getSpace(spaceId);
		Param param = space.getParam(tableName);
		if (param == null) {
			param = Env.getParam(tableName);
		}
		if (param == null) {
			throw new Exception("Table:" + tableName
					+ " is not exist in space:" + spaceId
					+ " or Env of machine:" + nodeDesc);
		}
		Sequence dimTable = (Sequence) param.getValue();
		if (dimTable == null) {
			throw new Exception("Table:" + tableName
					+ " can not be null in space:" + spaceId
					+ " or Env of machine:" + nodeDesc);
		}
		return dimTable;
	}

	/**
	 * 获取指定分机上正在运行的任务数目
	 * 
	 * @return
	 */
	public int getCurrentTasks() {
		Request req = new Request(Request.SERVER_GETCONCURRENTCOUNT);

		try {
			Response res = sendByNewSocket(req);
			if (res.getException() != null) {
				throw res.getException();
			}
			return ((Integer) res.getResult()).intValue();
		} catch (Exception x) {
			throw new RQException(x);
		}
	}

	public Response sendByNewSocket(Request req) throws Exception {
		SocketData tmp = null;
		try {
			tmp = newSocketData();
			tmp.write(req);
			Response res = (Response) tmp.read();
			res.setFromHost(this.toString());
			return res;
		} finally {
			if (tmp != null) {
				tmp.clientClose();
			}
		}
	}

	public Response send(Request req) throws Exception {
		// Logger.debug(req);
		if (socketData == null) {
			connect();
		}
		socketData.write(req);
		Response res = (Response) socketData.read();
		res.setFromHost(this.toString());
		return res;
	}

	public Response send(UnitCommand command) {
		try {
			Request req = new Request(Request.UNITCOMMAND_EXE);
			req.setAttr(Request.EXE_Object, command);
			Response res = sendByNewSocket(req);
			// Logger.debug("after unitcmd");
			return res;
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	public boolean isClosed() {
		return (socketData == null || socketData.isClosed());
	}

	public void close() {
		if (socketData != null) {
			try {
				socketData.clientClose();
			} catch (Exception x) {
			}
			socketData = null;
		}
	}

	public static String parseHost(String hostDesc) {
		boolean isIp4 = (hostDesc.indexOf(".") > 0);
		int colon;
		if (isIp4) {
			colon = hostDesc.indexOf(":");
		} else {
			colon = hostDesc.lastIndexOf(":");
		}
		if (colon < 0) {
			return null;
		}
		return hostDesc.substring(0, colon);
	}

	public static int parsePort(String hostDesc) {
		boolean isIp4 = (hostDesc.indexOf(".") > 0);
		int colon;
		if (isIp4) {
			colon = hostDesc.indexOf(":");
		} else {
			colon = hostDesc.lastIndexOf(":");
		}
		if (colon < 0) {
			return -1;
		}
		int cPort = Integer.parseInt(hostDesc.substring(colon + 1));
		return cPort;
	}

	private transient String tmpString = null;

	public String toString() {
		if (host == null) {
			return "Local";
		}
		if (tmpString == null) {
			tmpString = host + ":" + port;
		}
		return tmpString;
	}

	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof UnitClient)) {
			return false;
		}
		UnitClient otherUc = (UnitClient) other;
		if (otherUc.getHost() == null) {// 都是本机时
			return (host == null);
		}
		return otherUc.getHost().equalsIgnoreCase(host)
				&& otherUc.getPort() == port;
	}

	// JDBC函数接口部分
	public int JDBCConnect(String spaceId) throws Exception {
		Request req = new Request(Request.JDBC_CONNECT);
		req.setAttr(Request.CONNECT_spaceID, spaceId);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return (Integer) res.getResult();
	}

	public Table JDBCGetTables(int connId, String tableNamePattern,
			boolean isPlus) throws Exception {
		Request req = new Request(Request.JDBC_GETTABLES);
		req.setAttr(Request.GETTABLES_connID, connId);
		req.setAttr(Request.GETTABLES_tableNamePattern, tableNamePattern);
		req.setAttr(Request.JDBC_ISPLUS, isPlus);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return (Table) res.getResult();
	}

	public Table JDBCGetColumns(int connId, String tableNamePattern,
			String columnNamePattern, boolean isPlus) throws Exception {
		Request req = new Request(Request.JDBC_GETCOLUMNS);
		req.setAttr(Request.GETCOLUMNS_connID, connId);
		req.setAttr(Request.GETCOLUMNS_tableNamePattern, tableNamePattern);
		req.setAttr(Request.GETCOLUMNS_columnNamePattern, columnNamePattern);
		req.setAttr(Request.JDBC_ISPLUS, isPlus);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return (Table) res.getResult();
	}

	public Table JDBCGetProcedures(int connId, String procedureNamePattern,
			boolean isPlus) throws Exception {
		Request req = new Request(Request.JDBC_GETPROCEDURES);
		req.setAttr(Request.GETPROC_connID, connId);
		req.setAttr(Request.GETPROC_procedureNamePattern, procedureNamePattern);
		req.setAttr(Request.JDBC_ISPLUS, isPlus);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return (Table) res.getResult();
	}

	public Table JDBCGetProcedureColumns(int connId,
			String procedureNamePattern, String columnNamePattern,
			boolean isPlus) throws Exception {
		Request req = new Request(Request.JDBC_GETPROCECOLUMNS);
		req.setAttr(Request.GETPROCCOLUMNS_connID, connId);
		req.setAttr(Request.GETPROCCOLUMNS_procedureNamePattern,
				procedureNamePattern);
		req.setAttr(Request.GETPROCCOLUMNS_columnNamePattern, columnNamePattern);
		req.setAttr(Request.JDBC_ISPLUS, isPlus);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return (Table) res.getResult();
	}

	public Table JDBCGetSplParams(int connId, String procedureNamePattern,
			boolean isPlus) throws Exception {
		Request req = new Request(Request.JDBC_GETSPLPARAMS);
		req.setAttr(Request.GETSPLPARAMS_connID, connId);
		req.setAttr(Request.GETSPLPARAMS_splPath, procedureNamePattern);
		req.setAttr(Request.JDBC_ISPLUS, isPlus);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return (Table) res.getResult();
	}

	public int JDBCPrepare(int connId, String cmd, Object[] args,
			Map<String, Object> envParams) throws Exception {
		Request req = new Request(Request.JDBC_PREPARE);
		req.setAttr(Request.PREPARE_connID, connId);
		req.setAttr(Request.PREPARE_CMD, cmd);
		req.setAttr(Request.PREPARE_Args, args);
		req.setAttr(Request.PREPARE_ENV, envParams);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return (Integer) res.getResult();
	}

	public Sequence JDBCExecute(int connId, int stateId) throws Exception {
		Request req = new Request(Request.JDBC_EXECUTE);
		req.setAttr(Request.EXECUTE_connID, connId);
		req.setAttr(Request.EXECUTE_stateID, stateId);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		Sequence results = (Sequence) res.getResult();
		return results;
	}

	public boolean JDBCCancel(int connId, int stateId) throws Exception {
		Request req = new Request(Request.JDBC_CANCEL);
		req.setAttr(Request.CANCEL_connID, connId);
		req.setAttr(Request.CANCEL_stateID, stateId);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return (Boolean) res.getResult();
	}

	public boolean JDBCCloseStatement(int connId, int stateId) throws Exception {
		Request req = new Request(Request.JDBC_CLOSESTATEMENT);
		req.setAttr(Request.CLOSE_connID, connId);
		req.setAttr(Request.CLOSE_stateID, stateId);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return (Boolean) res.getResult();
	}

	public boolean JDBCCloseConnection(int connId) throws Exception {
		Request req = new Request(Request.JDBC_CLOSECONNECTION);
		req.setAttr(Request.CLOSE_connID, connId);

		Response res = sendByNewSocket(req);
		if (res.getException() != null) {
			throw res.getException();
		}
		return (Boolean) res.getResult();
	}

	public static String getHostPath(String host) {
		String path = host.replaceAll("::", ".");
		path = path.replaceAll(":", ".");// 将ipv6的冒号换成点
		return path;
	}

}
