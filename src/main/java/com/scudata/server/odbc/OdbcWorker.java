package com.scudata.server.odbc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import com.esproc.jdbc.JDBCUtil;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.server.ConnectionProxyManager;
import com.scudata.util.CellSetUtil;

/**
 * ODBC工作任务线程
 * @author Joancy
 *
 */
class OdbcWorker extends Thread {
	static final int Buffer_Size = 1024 * 64; // 缓冲区大小
	
	private Socket sckt;
	private volatile boolean stop = false;
	OutputStream out;
	InputStream in;

	/**
	 * 创建一个工作任务
	 * @param tg 线程组
	 * @param name 名称
	 */
	public OdbcWorker(ThreadGroup tg, String name) {
		super(tg, name);
	}

	/**
	 * 设置通讯套接字
	 * @param socket 套接字
	 * @throws Exception
	 */
	public void setSocket(Socket socket) throws Exception{
			socket.setTcpNoDelay(true);
			socket.setKeepAlive(true);
			socket.setReceiveBufferSize(Buffer_Size);
			socket.setSendBufferSize(Buffer_Size);
			socket.setSoLinger(true, 1);
			socket.setReuseAddress(true);
			socket.setSoTimeout(3000);
			this.sckt = socket;
			holdBufferStream();
	}
	
	private void holdBufferStream() throws IOException{
		OutputStream os = sckt.getOutputStream();
		out = new BufferedOutputStream(os);
		out.flush();
		
		InputStream is = sckt.getInputStream();
		in = new BufferedInputStream(is);
	}

	private void writeOdbcResponse(OutputStream os, int code, String returnMsg)
			throws Exception {
		DataTypes.writeInt(os, code);
		if (returnMsg == null)
			return;
		if (code < 0) {
			DataTypes.writeString(os, returnMsg);
		}
	}

	private ConnectionProxy getConnectionProxy(ConnectionProxyManager cpm,int connId) throws Exception{
		return (ConnectionProxy)cpm.getConnectionProxy(connId);
	}

	/*
	 * 协议 int:high byte first char:unicode big endian 返回值： 正确时，4byte(>=0
	 * 相应请求的代理号)+[其他信息] 错误时，4byte(-1)+4byte(错误长度)+[错误信息]
	 */
	private boolean serveODBC(int reqType, InputStream is, OutputStream os) {
		try {
			OdbcServer server = OdbcServer.getInstance();
			OdbcContext context = server.getContext();
			ConnectionProxyManager cpm = ConnectionProxyManager.getInstance();
			switch (reqType) {
			// 1000、odbclogin:
			// 4byte(1000)+4byte(user长度) + [user] + 4byte(password长度) +
			// [password]
			// return: 正确时，4byte(连接号)
			// 错误时，4byte(-1)+4byte(错误长度)+[错误信息]
			case 1000:
				String user = DataTypes.readString(is);
				String password = DataTypes.readString(is);
				if (!context.isUserExist(user)) {
					writeOdbcResponse(os, -1, "Login error: invalid user "
							+ user);
				} else {
					try {
						boolean success = context.checkUser(user, password);
						if (success) {
							int connId = OdbcServer.nextId();
							ConnectionProxy connProxy = new ConnectionProxy(
									cpm, connId, user);
							cpm.addProxy(connProxy);
							writeOdbcResponse(os, connId,
									"Login OK, current odbc user: " + user);
						}
					} catch (Exception x) {
						writeOdbcResponse(os, -1, x.getMessage());
					}
				}
				break;
			// * 1001、prepare statement dfx or dql:
			// * 4byte(1001) + 4byte(连接号) + 4byte(dfx长度) + [dfx] +[ArgRowData]
			// * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
			// * 正确时, 4byte( Statement号 )
			case 1001:
				int connId = DataTypes.readInt(is);
				String dfx = DataTypes.readString(is);
				Object[] args = DataTypes.readRowData(is);
				ArrayList argList = new ArrayList();
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						argList.add(args[i]);
					}
				}
				ConnectionProxy connProxy = getConnectionProxy(cpm,connId);
				int stateId = OdbcServer.nextId();
				StatementProxy sp = new StatementProxy(connProxy, stateId, dfx,
						argList);
				connProxy.addProxy(sp);
				writeOdbcResponse(os, stateId, null);
				break;
			// * 1002、execute statement dfx or dql:
			// * 4byte(1002) + 4byte(连接号) + 4byte(statement号)
			// * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
			// * 正确时, 4byte( 结果集个数 )+ 4byte( 结果集号 )...
			case 1002:
				connId = DataTypes.readInt(is);
				stateId = DataTypes.readInt(is);
				connProxy = getConnectionProxy(cpm,connId);
				sp = connProxy.getStatementProxy(stateId);
				int[] resultIds = sp.execute();
				DataTypes.writeInt(os, resultIds.length);
				for (int i = 0; i < resultIds.length; i++) {
					DataTypes.writeInt(os, resultIds[i]);
				}
				break;
			// * 1003、cancel execute dfx:
			// * 4byte(1003) + 4byte(连接号) + 4byte(statement号)
			// * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
			// * 正确时, 4byte( 0 )
			case 1003:
				connId = DataTypes.readInt(is);
				stateId = DataTypes.readInt(is);
				connProxy = getConnectionProxy(cpm,connId);
				sp = connProxy.getStatementProxy(stateId);
				sp.cancel();
				DataTypes.writeInt(os, 0);
				break;
			// * 1010、获取结果集结构
			// * 4byte(1010) + 4byte(连接号) + 4byte(statement号) + 4byte(结果集标识号)
			// * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
			// * 正确时, 4byte(字段数目)+4byte(字段1长度)+[字段1信息]...
			// *
			case 1010:
				connId = DataTypes.readInt(is);
				stateId = DataTypes.readInt(is);
				int resultId = DataTypes.readInt(is);
				connProxy = getConnectionProxy(cpm,connId);
				sp = connProxy.getStatementProxy(stateId);
				ResultSetProxy rsp = sp.getResultSetProxy(resultId);
				String[] columns = rsp.getColumns();
				if (columns == null) {
					DataTypes.writeInt(os, 0);
					return true;
				}
				// 成功标志
				int size = columns.length;
				DataTypes.writeInt(os, size);
				for (int i = 0; i < size; i++) {
					DataTypes.writeString(os, columns[i]);
				}
				break;
			// * 1011、结果集取数
			// * 4byte(1011) + 4byte(连接号)+4byte(statement号)+4byte(结果集标识号) +
			// 4byte(fetchSize)
			// * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
			// * 正确时, 4byte(0)+[数据表]
			// *
			case 1011:
				connId = DataTypes.readInt(is);
				stateId = DataTypes.readInt(is);
				resultId = DataTypes.readInt(is);
				int n = DataTypes.readInt(is);
				connProxy = getConnectionProxy(cpm,connId);
				sp = connProxy.getStatementProxy(stateId);
				rsp = sp.getResultSetProxy(resultId);
				Sequence data = rsp.fetch(n);
				DataTypes.checkTable(data);
				
				// 成功标志
				DataTypes.writeInt(os, 0);
				DataTypes.writeTable(os, data);
				break;
//				设置sqlfirst属性用的cmd=1012(若改成其它也行)
//						内容为string(如:a=1;b=2),每个属性为k=v格式，用分号隔开。主要是考虑将来可设置更多的属性。
//						当前sqlfirst属性设置成sqlfirst=simple或sqlfirst=plus传递给server.
				// * 1012、设置属性
				// * 4byte(1012) + 4byte(分节串长度)+[分节串]
				// * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
				// * 设置成功, 4byte(0)
				// *
			case 1012://废弃sql+
				String properties = DataTypes.readString(is);
				// 成功标志
				DataTypes.writeInt(os, 0);
				break;
			// * 1018、关闭Statement
			// * 4byte(1018) + 4byte(连接号)+4byte(statement号)
			// * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
			// * 正确时, 4byte(0)
			// *
			case 1018:
				connId = DataTypes.readInt(is);
				stateId = DataTypes.readInt(is);
				connProxy = getConnectionProxy(cpm,connId);
				sp = connProxy.getStatementProxy(stateId);
				sp.destroy();
				writeOdbcResponse(os, 0, "Statement:" + stateId + " is closed.");
				break;
			// * 1020、关闭结果集
			// * 4byte(1020) + 4byte(连接号)+4byte(statement号)+4byte(结果集标识号)
			// * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
			// * 正确时, 4byte(0)
			// *
			case 1020:
				connId = DataTypes.readInt(is);
				stateId = DataTypes.readInt(is);
				resultId = DataTypes.readInt(is);
				connProxy = getConnectionProxy(cpm,connId);
				sp = connProxy.getStatementProxy(stateId);
				rsp = sp.getResultSetProxy(resultId);
				rsp.destroy();
				writeOdbcResponse(os, 0, "ResultSet:" + resultId
						+ " is closed.");
				break;
//				 * 1050、列出存储过程；根据通配符，搜索dfx名称
//				 * 4byte(1050) + 4byte(连接号) + 4byte(搜索串长度) + [搜索串]
//				 * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
//				 *         正确时, 4byte(0)+[固定格式存储过程列表信息表]
//				 *
			case 1050:
				connId = DataTypes.readInt(is);
				String filter = DataTypes.readString(is);
				Map<String,String> m = com.esproc.jdbc.JDBCUtil.getSplList(filter);
				String spCols = "PROCEDURE_CAT,PROCEDURE_SCHEM,PROCEDURE_NAME,NUM_INPUT_PARAMS,NUM_OUTPUT_PARAMS,NUM_RESULT_SETS,REMARKS,PROCEDURE_TYPE";
				StringTokenizer st = new StringTokenizer(spCols, ",");
				ArrayList<String> cols = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					String name = st.nextToken();
					cols.add(name);
				}

				Table storeInfos = new Table(StringUtils.toStringArray(cols));
				Iterator<String> files = m.keySet().iterator();
				while (files.hasNext()){
					String path = files.next();
					String dfxName = m.get(path);
					int paramCount = getParamCount(path);
					storeInfos.newLast(new Object[]{"","",dfxName,paramCount,-1,-1,"",2});
				}
				// 成功标志
				DataTypes.writeInt(os, 0);
				DataTypes.writeTable(os, storeInfos);
				break;
//				 * 1051、获取存储过程详细信息表
//				 * 4byte(1051) + 4byte(存储过程名长度) + [名称]
//				 * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
//				 *         正确时, 4byte(0)+[固定格式存储过程信息表]
//				 *
				
				
//				 * 1060、列出表信息；
//				 * 4byte(1060) + 4byte(连接号) + 4byte(表名称长度) + [表名字]
//				 * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
//				 *         正确时, 4byte(字段数目)+4byte(字段1长度)+[字段1信息]...+[表信息数据表]
			case 1060:
				connId = DataTypes.readInt(is);
				String tableName = DataTypes.readString(is);
				connProxy = getConnectionProxy(cpm,connId);
				Table table = JDBCUtil.getTables(tableName);
				DataTypes.writeDatastructAndData(os, table);
				break;
				
//				 * 1061、列出字段信息；
//				 * 4byte(1061) + 4byte(连接号) + 4byte(表名称长度) + [表名字]+ 4byte(字段长度) + [字段]
//				 * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
//				 *         正确时, 4byte(字段数目)+4byte(字段1长度)+[字段1信息]...+[表信息数据表]
			case 1061:
				connId = DataTypes.readInt(is);
				tableName = DataTypes.readString(is);
				String columnName = DataTypes.readString(is);
				connProxy = getConnectionProxy(cpm,connId);
				table = JDBCUtil.getColumns(tableName, columnName, new Context());
				DataTypes.writeDatastructAndData(os, table);
				break;
				
			// * 1111、关闭连接
			// * 4byte(1111) + 4byte(连接号)
			// * return: 错误时, 4byte(-1:计算出错)+4byte(错误长度)+[错误信息]
			// * 正确时, 4byte(0)
			// *
			case 1111:
				connId = DataTypes.readInt(is);
				connProxy = getConnectionProxy(cpm,connId);
				writeOdbcResponse(os, 0, null);
				os.flush();
				connProxy.destroy();
				return false;
				// 关闭Socket
			case 2222:
				return false;
			}
		} catch (Throwable x) {
			String msg = x.getMessage();
			while(msg==null && x.getCause()!=null){
				 x = x.getCause();
				 msg = x.getMessage();
			}
			try {
				writeOdbcResponse(os, -1, msg);
			} catch (Exception e) {
			}
			x.printStackTrace();
			Logger.debug("Service exception:"+msg);
		}
		return true;
	}
	
	private int getParamCount(String dfx) throws Exception{
		int c = -1;
		FileInputStream in=null;
		try{
			in = new FileInputStream(dfx);
			PgmCellSet cs = CellSetUtil.readPgmCellSet(in);
			ParamList pl = cs.getParamList();
			if(pl!=null){
				c = pl.count();
			}
		}finally{
			if(in!=null) in.close();
		}
		return c;
	}

	public void shutDown() {
		stop = true;
	}

	/**
	 * 运行工作任务
	 */
	public void run() {
		try {
			InputStream is = in;
			OutputStream os = out;
			while (!stop) {
				int reqType = 0;
				try{
					reqType = DataTypes.readInt(is);
				}catch (java.net.SocketTimeoutException e) {
					continue;
				}

				if (reqType == -1) {
					// 关闭服务器
					OdbcServer.getInstance().shutDown();
					return;
				}

				// 关闭服务线程
				if (reqType == -2) {
					break;
				}

				if (reqType > 0) {
					if (serveODBC(reqType, is, os)) {
						os.flush();
						continue;
					}
					break;
				}
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			in.close();
			out.close();
			sckt.close();
		} catch (IOException e) {
		}
	}
}
