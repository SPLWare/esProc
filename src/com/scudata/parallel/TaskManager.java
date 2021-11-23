package com.scudata.parallel;

import java.util.*;

import com.scudata.common.CellLocation;
import com.scudata.dm.ParallelProcess;
import com.scudata.server.unit.UnitServer;
import com.scudata.thread.ThreadPool;

/**
 * 作业管理器(包含所有完成、未完成、取消的作业)，主要用于监控
 * 
 * @author Joancy
 *
 */
public class TaskManager {
	static ArrayList<ITask> tasks = new ArrayList<ITask>();

//	分机最多能跑最大任务数
	static ThreadPool pool=null;
	public static ThreadPool getPool(){
		if(pool==null){
			HostManager hostManager = HostManager.instance();
			int size = hostManager.getMaxTaskNum();
			pool = ThreadPool.newSpecifiedInstance(size); 
		}
		return pool;
	}
	
	/**
	 * 执行计算请求
	 * @param req 请求
	 * @return 响应
	 */
	public static Response execute(Request req) {
		int cmd = req.getAction();
		int taskId;
		Task task;
		Response res = new Response();
		try {
			switch (cmd) {
			case Request.DFX_TASK:
				Object dfx = req.getAttr(Request.TASK_DfxName);
				List args = (List) req.getAttr(Request.TASK_ArgList);
				String spaceId = (String) req.getAttr(Request.TASK_SpaceId);
				boolean isProcessCaller = false;
				Boolean B = (Boolean) req.getAttr(Request.TASK_IsProcessCaller);
				if(B!=null){
					isProcessCaller = B.booleanValue();
				}
				Object reduce = req.getAttr(Request.TASK_Reduce);
				CellLocation accumulateLocation = (CellLocation)req.getAttr(Request.TASK_AccumulateLocation);
				CellLocation currentLocation = (CellLocation)req.getAttr(Request.TASK_CurrentLocation);
				taskId = UnitServer.nextId();
				task = new Task(dfx, args, taskId, spaceId, isProcessCaller,reduce,accumulateLocation,currentLocation);
				Integer processTaskId = (Integer) req.getAttr(Request.TASK_ProcessTaskId);
				if(processTaskId!=null){
					task.setProcessTaskId(processTaskId);
				}
				
				addTask(task);
				res.setResult(new Integer(taskId));
				break;
			case Request.DFX_CALCULATE:
				taskId = ((Number) req.getAttr(Request.CALCULATE_TaskId))
						.intValue();
				task = (Task)getTask(taskId);
				
				getPool().submit( task );
				task.join();
				res = task.getResponse();
				break;
			case Request.DFX_CANCEL:
				taskId = ((Number) req.getAttr(Request.CANCEL_TaskId))
						.intValue();
				String cancelReason = (String)req.getAttr(Request.CANCEL_Reason);
				task = (Task)getTask(taskId);
				res = task.cancel( cancelReason );
				break;
			case Request.DFX_GET_REDUCE:
				spaceId = (String) req.getAttr(Request.GET_REDUCE_SpaceId);
				res.setResult(ParallelProcess.getReduceResult(spaceId));
				break;
			}
		} catch (Exception x) {
			res.setException(x);
		}

		return res;
	}

	/**
	 * 增加任务
	 * @param t 任务
	 */
	public synchronized static void addTask(ITask t) {
		tasks.add(t);
	}

	/**
	 * 删除任务
	 * @param taskId 任务号
	 */
	public synchronized static void delTask(int taskId) {
		for (int i = 0; i < tasks.size(); i++) {
			ITask t = tasks.get(i);
			if (t.getTaskID() == taskId) {
				tasks.remove(i);
				break;
			}
		}
	}

	/**
	 * 根据任务号取出任务对象
	 * @param taskId 任务号 
	 * @return 恩物对象
	 * @throws Exception 出错时抛出异常
	 */
	public synchronized static ITask getTask(int taskId) throws Exception {
		for (int i = 0; i < tasks.size(); i++) {
			ITask t = tasks.get(i);
			if (t.getTaskID() == taskId) {
				return t;
			}
		}
		throw new Exception("Task:" + taskId + " is timeout.");
	}

	/**
	 * 取任务列表的复制
	 * @return 任务列表
	 */
	public synchronized static List<ITask> getTaskList() {
		ArrayList<ITask> al = new ArrayList<ITask>();
		al.addAll(tasks);
		return al;
	}

	/**
	 * 检查代理超时
	 */
	public synchronized static void checkTimeOut(int proxyTimeOut) {
		for (int i = tasks.size() - 1; i >= 0; i--) {
			ITask t = tasks.get(i);
			if (t.checkTimeOut(proxyTimeOut)) {
				t.close();
				tasks.remove(t);
			}
		}
	}

}
