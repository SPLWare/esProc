package com.scudata.server.unit;

import java.util.ArrayList;
import java.util.Map;

import com.esproc.jdbc.JDBCUtil;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.parallel.Request;
import com.scudata.parallel.Response;
import com.scudata.parallel.TaskManager;
import com.scudata.server.ConnectionProxyManager;

/**
 * JDBC管理器
 * 
 * @author Joancy
 *
 */
public class JdbcManager {

	private static ConnectionProxy getConnectionProxy(int connId)
			throws Exception {
		ConnectionProxyManager cpm = ConnectionProxyManager.getInstance();
		return (ConnectionProxy) cpm.getConnectionProxy(connId);
	}

	/**
	 * 执行JDBC请求
	 * @param req 请求对象
	 * @return 响应结果
	 */
	public static Response execute(Request req) {
		ConnectionProxyManager cpm = ConnectionProxyManager.getInstance();
		Response res = new Response();
		int connId, stateId;
		ConnectionProxy connProxy;
		String tableName, columnName;
		Table table;
		try {
			switch (req.getAction()) {
			case Request.JDBC_CONNECT:
				connId = UnitServer.nextId();
				String spaceId = (String) req.getAttr(Request.CONNECT_spaceID);
				connProxy = new ConnectionProxy(cpm, connId, spaceId);
				cpm.addProxy(connProxy);
				res.setResult(connId);
				break;
			case Request.JDBC_GETTABLES:
				connId = (Integer) req.getAttr(Request.GETTABLES_connID);
				tableName = (String) req
						.getAttr(Request.GETTABLES_tableNamePattern);
				table = JDBCUtil.getTables(tableName);
				res.setResult(table);
				break;
			case Request.JDBC_GETCOLUMNS:
				connId = (Integer) req.getAttr(Request.GETCOLUMNS_connID);
				tableName = (String) req
						.getAttr(Request.GETCOLUMNS_tableNamePattern);
				columnName = (String) req
						.getAttr(Request.GETCOLUMNS_columnNamePattern);
				connProxy = getConnectionProxy(connId);
				table = JDBCUtil.getColumns(tableName, columnName,
						connProxy.getContext());
				res.setResult(table);
				break;
			case Request.JDBC_GETPROCEDURES:
				connId = (Integer) req.getAttr(Request.GETPROC_connID);
				tableName = (String) req
						.getAttr(Request.GETPROC_procedureNamePattern);
				table = JDBCUtil.getProcedures(tableName);
				res.setResult(table);
				break;
			case Request.JDBC_GETPROCECOLUMNS:
				connId = (Integer) req.getAttr(Request.GETPROCCOLUMNS_connID);
				tableName = (String) req
						.getAttr(Request.GETPROCCOLUMNS_procedureNamePattern);
				columnName = (String) req
						.getAttr(Request.GETPROCCOLUMNS_columnNamePattern);
				connProxy = getConnectionProxy(connId);
				table = JDBCUtil.getProcedureColumns(tableName, columnName);
				res.setResult(table);
				break;
			case Request.JDBC_PREPARE:
				connId = (Integer) req.getAttr(Request.PREPARE_connID);
				String dfx = (String) req.getAttr(Request.PREPARE_CMD);
				Object[] args = (Object[]) req.getAttr(Request.PREPARE_Args);
				Map<String, Object> envParams = (Map<String, Object>) req
						.getAttr(Request.PREPARE_ENV);
				ArrayList argList = new ArrayList();
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						argList.add(args[i]);
					}
				}
				connProxy = getConnectionProxy(connId);
				stateId = UnitServer.nextId();
				StatementProxy sp = new StatementProxy(connProxy, stateId, dfx,
						argList, envParams);
				// StatementProxy由于会产生游标，而游标的管理从属于TaskManager，所以该对象同时
				// 记在ConnectionProxy以及TaskManager里面
				connProxy.addProxy(sp);
				TaskManager.addTask(sp);
				res.setResult(stateId);
				break;
			case Request.JDBC_EXECUTE:
				connId = (Integer) req.getAttr(Request.EXECUTE_connID);
				stateId = (Integer) req.getAttr(Request.EXECUTE_stateID);
				connProxy = getConnectionProxy(connId);
				sp = connProxy.getStatementProxy(stateId);
				Sequence resultIds = sp.execute();
				res.setResult(resultIds);
				break;
			case Request.JDBC_CANCEL:
				connId = (Integer) req.getAttr(Request.CANCEL_connID);
				stateId = (Integer) req.getAttr(Request.CANCEL_stateID);
				connProxy = getConnectionProxy(connId);
				sp = connProxy.getStatementProxy(stateId);
				res.setResult(sp.cancel());
				break;
			case Request.JDBC_CLOSESTATEMENT:
				connId = (Integer) req.getAttr(Request.CLOSE_connID);
				stateId = (Integer) req.getAttr(Request.CLOSE_stateID);
				connProxy = getConnectionProxy(connId);
				sp = connProxy.getStatementProxy(stateId);
				sp.destroy();
				res.setResult(Boolean.TRUE);
				break;
			case Request.JDBC_CLOSECONNECTION:
				connId = (Integer) req.getAttr(Request.CLOSE_connID);
				connProxy = getConnectionProxy(connId);
				connProxy.destroy();
				res.setResult(Boolean.TRUE);
			}
		} catch (Exception x) {
			res.setException(x);
		}
		return res;
	}

}
