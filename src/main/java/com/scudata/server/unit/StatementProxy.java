package com.scudata.server.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.parallel.ITask;
import com.scudata.parallel.RemoteCursor;
import com.scudata.parallel.RemoteCursorProxy;
import com.scudata.parallel.RemoteCursorProxyManager;
import com.scudata.parallel.TaskManager;
import com.scudata.parallel.UnitContext;
import com.scudata.server.IProxy;

/**
 * Statement 代理器
 * 
 * @author Joancy
 *
 */
public class StatementProxy extends IProxy implements ITask {
	String cmd = null;
	ArrayList params = null;

	JdbcTask task = null;
	Context ctx;
	RemoteCursorProxyManager rcpm = null;

	/**
	 * 创建一个Statement代理器
	 * @param cp 连接代理
	 * @param id 代理编号
	 * @param cmd 执行的命令
	 * @param params 参数列表
	 * @param envParams 环境参数
	 * @throws Exception
	 */
	public StatementProxy(ConnectionProxy cp, int id, String cmd,
			ArrayList<Object> params, Map<String, Object> envParams)
			throws Exception {
		super(cp, id);
		this.cmd = cmd;
		if (!StringUtils.isValidString(cmd)) {
			throw new Exception("Prepare statement cmd is empty!");
		}
		Logger.debug("StatementProxy cmd:\r\n" + cmd);
		this.params = params;
		this.ctx = cp.getContext();
		task = new JdbcTask(cmd, params, ctx, envParams);
		access();
	}

	/**
	 * 获取连接代理器
	 * @return 连接代理器
	 */
	public ConnectionProxy getConnectionProxy() {
		return (ConnectionProxy) getParent();
	}

	/**
	 * 获取要执行的命令
	 * @return 命令
	 */
	public String getCmd() {
		return cmd;
	}

	/**
	 * 获取参数值
	 * @return 参数列表
	 */
	public List<String> getParams() {
		return params;
	}

	/**
	 * 获取结果集的游标代理管理器
	 */
	public RemoteCursorProxyManager getCursorManager() {
		if (rcpm == null) {
			rcpm = new RemoteCursorProxyManager(this);
		}
		return rcpm;
	}

	/**
	 * 执行当前命令
	 * @return 结果序表
	 * @throws Exception
	 */
	public Sequence execute() throws Exception {
		Sequence seq = task.execute();
		if (seq == null) {
			return null;
		}
		Sequence results = new Sequence();
		UnitServer server = UnitServer.instance;
		UnitContext uc = server.getUnitContext();

		for (int i = 1; i <= seq.length(); i++) {
			Object tmp = seq.get(i);
			if (tmp instanceof ICursor) {
				int proxyId = UnitServer.nextId();
				rcpm = getCursorManager();
				RemoteCursorProxy rcp = new RemoteCursorProxy(rcpm,
						(ICursor) tmp, proxyId);
				rcpm.addProxy(rcp);
				RemoteCursor rc = new RemoteCursor(uc.getLocalHost(),
						uc.getLocalPort(), getId(), proxyId);
				ctx.addResource(rc);
				results.add(rc);
			} else {
				results.add(tmp);
			}
		}

		return results;
	}

	/**
	 * 取消当前任务
	 * @return 成功取消返回true
	 * @throws Exception
	 */
	public boolean cancel() throws Exception {
		task.cancel();
		return true;
	}

	/**
	 * 关闭当前代理器
	 */
	public void close() {
		TaskManager.delTask(getId());
	}

	/**
	 * 实现toString接口
	 */
	public String toString() {
		return "Statement " + getId();
	}

	/**
	 * 获取任务ID
	 */
	public int getTaskID() {
		return getId();
	}

}