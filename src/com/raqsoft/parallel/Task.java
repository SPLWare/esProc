package com.raqsoft.parallel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.raqsoft.cellset.datamodel.PgmCellSet;
import com.raqsoft.common.CellLocation;
import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.dm.CanceledException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DfxManager;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.IResource;
import com.raqsoft.dm.JobSpace;
import com.raqsoft.dm.JobSpaceManager;
import com.raqsoft.dm.ParallelCaller;
import com.raqsoft.dm.ParallelProcess;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.RetryException;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.MemoryCursor;
import com.raqsoft.ide.dfx.Esproc;
import com.raqsoft.resources.ParallelMessage;
import com.raqsoft.server.odbc.OdbcServer;
import com.raqsoft.server.unit.UnitServer;
import com.raqsoft.thread.Job;
import com.raqsoft.util.CellSetUtil;
import com.raqsoft.util.DatabaseUtil;

/**
 * 每一个作业的管理
 * 
 * 每个作业（也称为任务）产生的游标都记录在任务上，游标产生后，开始计时超时 
 * 游标的每一次访问过程，都不计超时。也即一次fetch过程如果可能超过时间，是不计时的；
 * 游标访问过后，重新开始计时； 如果一个任务产生了多个游标后，此时已经开始计时，
 * 如果dfx在挨个串行整理头几个游标，排在后面的游标仍然可能超时 任务
 * 
 * @author Joancy
 *
 */
public class Task extends Job implements IResource, ITask {
	Object dfxName;
	boolean isDfxFile = false;//记住当前dfx的来源，如果是从文件来，则使用缓存
	ArrayList args;
	String spaceId;
	boolean isProcessCaller = false;
	Object reduce;
	CellLocation accumulateLocation;
	CellLocation currentLocation;

	// 分进程端口号，可重算作业时，主控机直接将作业布置到分机的具体分进程
//	int subPort = 0;
	int processTaskId = 0;// 主进程的任务编号

	int taskId = -1;
	long callTime = -1;
	long finishTime = -1;

	RemoteCursorProxyManager rcpm = null;
	transient Object tasker = null;
	transient boolean isCanceled = false;
	transient Response res = null;

	private long lastAccessTime = -1;
	private static List connectedDsNames = null;// 如果是IDE端执行，由IDE设置该变量；否则从UnitContext中ConfigBean中取
	private String cancelCause = null;

	MessageManager mm = ParallelMessage.get();

	/**
	 * 构造一个任务
	 * @param dfxName 要执行的dfx
	 * @param argList 对应的参数
	 * @param taskId 任务号
	 * @param spaceId 空间号
	 */
	public Task(Object dfxName, List argList, int taskId, String spaceId) {
		this.dfxName = dfxName;
		this.args = (ArrayList) argList;
		this.taskId = taskId;
		this.spaceId = spaceId;

		JobSpaceManager.getSpace(spaceId).getResourceManager().add(this);
	}

	/**
	 * 构造一个任务
	 * @param dfxName 要执行的dfx
	 * @param argList 参数列表
	 * @param taskId 任务号
	 * @param spaceId 空间号
	 * @param isProcessCaller 是否主进程
	 * @param reduce reduce表达式
	 */
	public Task(Object dfxName, List argList, int taskId, String spaceId,
			boolean isProcessCaller, Object reduce,CellLocation accumulateLocation,
			CellLocation currentLocation) {
		this(dfxName, argList, taskId, spaceId);
		this.isProcessCaller = isProcessCaller;
		this.reduce = reduce;
		this.accumulateLocation = accumulateLocation;
		this.currentLocation = currentLocation;
	}

	/**
	 * 设置分进程的端口
	 * @param sub 端口号
	 */
//	public void setSubPort(int sub) {
//		this.subPort = sub;
//	}

	/**
	 * 设置主进程的任务号
	 * @param id
	 */
	public void setProcessTaskId(int id) {
		this.processTaskId = id;
	}

	/**
	 * 设置已经连接后的数据源名称 
	 * @param dsNames 数据源名称列表
	 */
	public static void setConnectedDsNames(List dsNames) {
		connectedDsNames = dsNames;
	}

	/**
	 * 任务代理的访问刷新
	 */
	public void access() {
		lastAccessTime = System.currentTimeMillis();
	}

	/**
	 * 重置代理的访问刷新
	 */
	public void resetAccess() {
		lastAccessTime = -1;
	}

	/**
	 * 销毁当前对象
	 */
	public void destroy() {
		if (rcpm != null) {
			rcpm.destroy();
			rcpm = null;
		}
		
		isClosed = true;
	}

	/**
	 * 计算前计时打卡
	 */
	private void beforeExecute() {
		callTime = System.currentTimeMillis();
	}

	/**
	 * 取游标管理器
	 * @return 游标代理管理器
	 */
	public RemoteCursorProxyManager getCursorManager() {
		if (rcpm == null) {
			rcpm = new RemoteCursorProxyManager(this);
		}
		return rcpm;
	}

	/**
	 * 判断当前作业是否运行状态
	 * @return 是运行状态返回true，否则返回false
	 */
	public boolean isRunning() {
		return tasker != null;
	}

	/**
	 * 将计算结果转换为游标访问对象
	 * @param result 计算结果
	 * @return 游标接口
	 */
	public static ICursor toCursor(Object result) {
		if (result instanceof Sequence) {
			Sequence t = (Sequence) result;
			if (t.length() > 0) {
				MemoryCursor mc = new MemoryCursor(t);
				return mc;
			} else {
				result = "";
			}
		}
		if (result instanceof ICursor) {
			return (ICursor) result;
		}
		String[] fields;
		Object[] values;
		if (result instanceof Record) {
			Record rec = (Record) result;
			fields = rec.getFieldNames();
			values = rec.getFieldValues();
		} else {
			fields = new String[] { "_1" };
			values = new Object[] { result };
		}

		Table table = new Table(fields);
		table.newLast(values);
		MemoryCursor mc = new MemoryCursor(table);
		return mc;
	}

	/**
	 * 该方法用于ODBC服务器执行dfx，需要检查Intergration
	 * @return 计算结果的游标组
	 * @throws Exception 计算出错时抛出异常
	 */
	public ICursor[] executeOdbc() throws Exception {
		Object obj = doExecute(true);
		if (obj instanceof Response) {
			Response res = (Response) obj;
			if (res.getError() != null) {
				throw res.getError();
			}
			if (res.getException() != null) {
				throw res.getException();
			}
			return null;
		}

		Sequence results = (Sequence) obj;
		int size = results.length();

		ICursor[] cursors = new ICursor[size];
		for (int i = 1; i <= size; i++) {
			cursors[i - 1] = toCursor(results.get(i));
		}
		return cursors;
	}

	/**
	 * 执行计算
	 * @return 计算完成后的响应
	 */
	public Response execute() {
		Object obj = doExecute(false);
		if (obj instanceof Response) {
			return (Response) obj;
		}

		Response res = new Response();
		res.setResult(obj);
		return res;
	}

	// 当取消是从Datastore来时，返回普通异常。目前的CanceledException是不需要中断别人的。
	// 但是DataStore发出的取消，需要中断别人。
	private Exception getCancelException() {
		if (cancelCause != null) {
			MessageManager mm = ParallelMessage.get();

			String status = mm.getMessage("Task.cancel", this, cancelCause);
			if (cancelCause.equalsIgnoreCase(CanceledException.TYPE_DATASTORE)) {
				return new Exception(status);
			}
		}
		return new CanceledException(cancelCause);
	}

	/**
	 * 准备计算前的上下文环境 
	 * @return 计算上下文
	 */
	public static Context prepareEnv() throws Exception{
		Context context = new Context();
		if (connectedDsNames == null) {
			UnitServer us = UnitServer.instance;
			OdbcServer os = OdbcServer.instance;
			if (us != null) {
				if (us.getRaqsoftConfig() != null) {
					connectedDsNames = us.getRaqsoftConfig()
							.getAutoConnectList();
				}
			} else if (os != null) {
				if (os.getRaqsoftConfig() != null) {
					connectedDsNames = os.getRaqsoftConfig()
							.getAutoConnectList();
				}
			}
		}
		DatabaseUtil.connectAutoDBs(context, connectedDsNames);
		Esproc.loadDataSource(context);
		return context;
	}

	private Sequence executeTask() throws Exception {
		Context context = prepareEnv();
		PgmCellSet pcs = getPgmCellSet(context);
		tasker = pcs;

		JobSpace js = JobSpaceManager.getSpace(spaceId);
		context.setJobSpace(js);
		context.addResource(this);

		pcs.setContext(context);
		Object[] argsVal = null;
		if (args != null) {
			argsVal = args.toArray();
		}

		if (ParallelCaller.isScript(dfxName)) {
			// 语句中的参数，固定以"arg"开头
			if (argsVal != null && argsVal.length > 0) {
				for (int i = 0; i < argsVal.length; i++) {
					context.setParamValue("arg" + (i + 1), argsVal[i]);
				}
			}
		} else {
			CellSetUtil.putArgValue(pcs, argsVal);
		}
		pcs.calculateResult();

		if (pcs.getInterrupt()) {
			throw getCancelException();
		}

		Sequence results = new Sequence();
		UnitServer server = UnitServer.instance;
		boolean isLocalExecute = (server == null);
		UnitContext uc = null;
		if (!isLocalExecute) {
			uc = server.getUnitContext();
		}

		while (pcs.hasNextResult()) {
			Object tmp = pcs.nextResult();
			if (!isLocalExecute && (tmp instanceof ICursor)) {
				int proxyId = UnitServer.nextId();
				RemoteCursorProxyManager rcpm = getCursorManager();
				RemoteCursorProxy rcp = new RemoteCursorProxy(rcpm,
						(ICursor) tmp, proxyId);
				rcpm.addProxy(rcp);
				RemoteCursor rc = new RemoteCursor(uc.getLocalHost(),
						uc.getLocalPort(), taskId, proxyId);// , context);
				context.addResource(rc);
				results.add(rc);
			} else {
				results.add(tmp);
			}
		}
		return results;
	}

	long taskBegin = 0;

	private Object doExecute(boolean isODBC) {
		beforeExecute();
		try {
			if (isCanceled) {
				throw getCancelException();
			}

			taskBegin = System.currentTimeMillis();
			Object result = null;
			if (isProcessCaller) {
				ParallelProcess pp = new ParallelProcess(dfxName);
				tasker = pp;
				List<List> multiArgs = (List<List>) args;
				for (int i = 0; i < multiArgs.size(); i++) {
					pp.addCall(multiArgs.get(i));
				}
				pp.setJobSpaceId(spaceId);
				pp.setReduce(reduce,accumulateLocation,currentLocation);
//				pp.setSubPort(subPort);
				pp.setProcessTaskId(taskId);
				result = pp.execute();
			} else {
				Sequence seq = executeTask();
				if (isODBC) {
					result = seq;
				} else {
					if (seq.length() == 1) {
						result = seq.get(1);
					} else {
						result = seq;
					}
				}
			}
			return result;
		} catch (Throwable x) {
			// 主进程的错误信息不打印，已经在子进程打印过了
			if (!isProcessCaller) {
				Logger.debug(this, x);
			}

			Response res = new Response();
			if (x instanceof Error) {
				res.setError((Error) x);
			} else if (x instanceof Exception) {
				HostManager hm = HostManager.instance();
				String msg = "[" + hm + "] ";
				String causemsg = x.getMessage();
				if(causemsg!=null){
					if (causemsg.startsWith("[")) {
						msg = causemsg;
					} else {
						msg += causemsg;
					}
				}
				Exception ex;
				if (x instanceof RetryException) {
					ex = new RetryException(msg, x);
				} else {
					ex = new Exception(msg, x);
				}

				res.setException(ex);
			}
			return res;
		} finally {
			if (tasker != null) {
				if (tasker instanceof PgmCellSet) {
					finishTime = System.currentTimeMillis();
					((PgmCellSet) tasker).reset();
					 if(isDfxFile){
						 DfxManager.getInstance().putDfx((PgmCellSet) tasker);
					 }
				} else {
					ParallelProcess pp = (ParallelProcess) tasker;
					pp.close();
				}
				tasker = null;
			}

			if (rcpm == null) { // 计算结果有游标时，要等待游标都关闭时，才删除任务
				TaskManager.delTask(taskId);
			}
			access();
		}
	}

	/**
	 * 取消当前作业
	 * @return 取消结果的响应
	 */
	public Response cancel() {
		return cancel(null);
	}

	/**
	 * 取消当前作业
	 * @param reason 将取消信息写入原因
	 * @return 取消结果的响应
	 */
	public Response cancel(String reason) {
		cancelCause = reason;
		Response res = new Response();
		if (tasker == null) {
			if (callTime == -1) {
				res.setResult(Boolean.TRUE);
				isCanceled = true;
			} else {
				res.setException(new Exception("Task is finished."));
			}
		} else {
			if (tasker instanceof PgmCellSet) {
				((PgmCellSet) tasker).interrupt();
			} else {
				((ParallelProcess) tasker).cancel(reason);
			}

			res.setResult(Boolean.TRUE);
			Logger.debug(this + " is canceled.");
		}
		return res;
	}

	/**
	 * 获取dfx名称
	 * @return 名称
	 */
	public String getDfxName() {
		return ParallelCaller.dfxDelegate(dfxName);
	}

	/**
	 * 获取参数列表
	 * @return 参数列表
	 */
	public List getArgList() {
		return args;
	}

	/**
	 * 获取任务编号
	 * @return 任务编号
	 */
	public int getTaskID() {
		return taskId;
	}

	/**
	 * 取任务开始计算的时刻
	 * @return 任务开始时刻(缺省-1)
	 */
	public long getCallTime() {
		return callTime;
	}

	/**
	 * 设置任务开始计算的时刻
	 * @param callTime 时刻
	 */
	public void setCallTime(long callTime) {
		this.callTime = callTime;
	}

	/**
	 * 取任务计算完成后的时刻
	 * @return 任务完成时刻(缺省-1)
	 */
	public long getFinishTime() {
		return finishTime;
	}

	/**
	 * 设置任务计算完成时的时刻
	 * @param finishTime 完成时刻
	 */
	public void setFinishTime(long finishTime) {
		this.finishTime = finishTime;
	}

	PgmCellSet getPgmCellSet(Context ctx) {
		DfxManager dfxManager = DfxManager.getInstance();
		PgmCellSet pcs;
		if (ParallelCaller.isScript(dfxName)) {
			String dfx = (String) dfxName;
			pcs = CellSetUtil.toPgmCellSet(dfx);
		} else if (dfxName instanceof String) {
			String dfx = (String) dfxName;
			FileObject fo = new FileObject(dfx, "s");
			isDfxFile = true;
			pcs = dfxManager.removeDfx(fo, ctx);
		} else {
			pcs = (PgmCellSet) dfxName;
		}
		return pcs;
	}

	/**
	 * 检查代理对象的访问超时
	 */
	public boolean checkTimeOut(int timeOut) {
		if (lastAccessTime < 0) {
			return false; // 还没计算的任务不能检查过期
		}
		// 换算成秒，timeOut单位为秒
		long unvisit = (System.currentTimeMillis() - lastAccessTime) / 1000;
		if (unvisit > timeOut) {
			Logger.info(this + " is timeout.");
			destroy();
			return true;
		}
		return false;
	}

	/**
	 * 任务对象的toString实现
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();

		if (processTaskId == 0) {
			sb.append(" [" + ParallelCaller.dfxDelegate(dfxName) + "] ");
			sb.append(ParallelCaller.args2String(args));
			sb.append(mm.getMessage("Task.taskid", taskId));
		} else {
			sb.append(mm
					.getMessage("Task.taskAndMainId", taskId, processTaskId));
		}

		System.out.println(sb);
		return sb.toString();
	}

	/**
	 * 开始运行
	 */
	public void run() {
		long l1 = System.currentTimeMillis();
//		作业调度有两个层级，ProcessCaller接到一堆作业后，再逐个转化为本机单个作业线程
//		两个层级都会走这个run方法，第一层的ProcessCaller的消息忽略
		if (!(isProcessCaller && args.size()<2)) {
			Logger.debug(mm.getMessage("Task.taskBegin", this));
		}
		res = execute();
		long l2 = System.currentTimeMillis();
		DecimalFormat df = new DecimalFormat("###,###");
		long lastTime = l2 - l1;
		if (!(isProcessCaller && args.size()<2)) {
			Logger.debug(mm.getMessage("Task.taskEnd", this, df.format(lastTime)));
		}
	}

	/**
	 * 获取计算完成的响应
	 * @return 响应对象
	 */
	public Response getResponse() {
		return res;
	}

	boolean isClosed = false;

	/**
	 * 关闭当前作业，释放相关资源
	 */
	public void close() {
		if (!isClosed) {
			destroy();
			TaskManager.delTask(taskId);
		}
	}

}
