package com.scudata.server.unit;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import com.esproc.jdbc.JDBCUtil;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.parallel.Request;

/**
 * JDBC执行任务
 * 
 * @author Joancy
 *
 */
public class JdbcTask {
	String cmd;
	ArrayList args;
	Context context;
	Map<String, Object> envParams;
	Thread execThread = null;

	/**
	 * 创建一个JDBC任务
	 * @param cmd 任务执行的命令
	 * @param args 参数列表
	 * @param ctx 计算上下文
	 * @param envParams 环境参数
	 */
	public JdbcTask(String cmd, ArrayList args, Context ctx,
			Map<String, Object> envParams) {
		this.cmd = cmd;
		this.args = (ArrayList) args;
		this.context = ctx;
		this.envParams = envParams;
	}

	/**
	 * 执行当前任务
	 * @return 结果序表
	 * @throws Exception
	 */
	public Sequence execute() throws Exception {
		Object result = executeJDBC();
		if (result == null)
			return null;
		Sequence seq = new Sequence();
		if (result instanceof PgmCellSet) {
			PgmCellSet cs = (PgmCellSet) result;
			while (cs.hasNextResult()) {
				seq.add(checkResult(cs.nextResult()));
			}
		} else {
			seq.add(checkResult(result));
		}
		return seq;
	}

	public static Object checkResult(Object r) throws Exception{
		if(!(r instanceof Serializable)) {
			throw new Exception("Return result "+r.getClass().getName()+" is not supportted.");
		}
		return r;
	}

	
	/**
	 * 取消当前任务
	 * @return 成功取消返回true
	 * @throws Exception
	 */
	public boolean cancel() throws Exception {
		if (execThread != null) {
			try {
				execThread.stop();
			} catch (Throwable t1) {
			}
			try {
				execThread.destroy();
			} catch (Throwable t1) {
			}
			execThread = null;
		}
		return true;
	}

	/**
	 * 获取执行命令，比如dfx名称
	 * @return 命令
	 */
	public String getCmd() {
		return cmd;
	}

	/**
	 * 获取参数列表
	 * @return dfx参数
	 */
	public ArrayList getArgs() {
		return args;
	}

	private Exception ex;
	private Boolean execFinished = false;
	private boolean isCanceled = false;
	private Object result;

	private Object executeJDBC() throws Exception {
		try {
			ex = null;
			result = null;
			execFinished = false;
			isCanceled = false;
			execThread = new Thread() {
				public void run() {
					try {
						result = JDBCUtil.execute(cmd, args, context);
					} catch (ThreadDeath td) {
						isCanceled = true;
					} catch (SQLException e) {
						ex = e;
					} catch (Exception e) {
						Throwable t = e;
						while (t != null) {
							if (t instanceof ThreadDeath) {
								ex = new InterruptedException();
								break;
							}
							t = t.getCause();
						}
						if (ex == null)
							ex = new SQLException(e.getMessage(), e);
					} finally {
						execFinished = true;
					}
				}
			};
			execThread.start();
			synchronized (execFinished) {
				while (!execFinished.booleanValue())
					try {
						Thread.sleep(5);
					} catch (ThreadDeath td) {
					} catch (InterruptedException e) {
					}
			}
			if (ex != null) {
				throw ex;
			}
		} catch (ThreadDeath td) {
			isCanceled = true;
		}
		if (isCanceled)
			throw new InterruptedException();
		return result;
	}
}
