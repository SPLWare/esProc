package com.scudata.dm;

import java.text.DecimalFormat;
import java.util.*;

import com.scudata.common.*;
import com.scudata.expression.Expression;
import com.scudata.parallel.*;
import com.scudata.server.unit.UnitServer;
import com.scudata.thread.ThreadPool;

/**
 * 实现并发调用函数callx(dfx,…;hs;rdfx)的并发调用类
 * 向分机序列hs分配作业执行脚本dfx，继承任务空间；返回每个作业的返回值构成的序列。
 * 作业非正常结束(end msg)则将重新分配（避开已分配过分机），如果找不到可用分机则任务失败，作业分配到不可用分机时被认为非正常结束
 * 缺省将依次分配，即第i个作业分配到hs.m@r(i)上
 * rdfx是两个参数的脚本，用于reduce动作，可省略。其参数分别是当前累积值和当前返回值，rdfx返回值将作为新的累积值，初始累积值为null
 * 有rdfx时，返回值将是每个分机的返回值构成的序列，按hs的次序。
 * @author Joancy
 *
 */
public class ParallelCaller extends ParallelProcess {
	// 改为长连接后，node一旦产生就会首先占用连接；等量算法会直接用它；
	// 但少量算法，不直接使用nodes，会争用 ucList中的队列分机，少量算法的nodes会浪费初始连接
	private UnitClient[] nodes = null;

	private Context ctx;
	private int activeHostCount = 0;

	private final byte TYPE_DEFAULT = 0;//缺省顺次分配，多余的轮回
	private final byte TYPE_ONE = 1;//一个作业全部分配，最快的返回后，中断其他作业
	private final byte TYPE_RANDOM = 2;//随机分配
//	private final byte TYPE_MUCH = 3;//预分配然后抢作业

	private String opt = null;

	/**
	 * 构造一个并发调用对象
	 * @param dfx 计算对象
	 * @param hosts 分机
	 * @param ports 端口
	 */
	public ParallelCaller(Object dfx, String[] hosts, int[] ports) {
		super(dfx);
		if (hosts == null)
			return;// 本地线程执行时

		nodes = new UnitClient[hosts.length];
		for (int i = 0; i < hosts.length; i++) {
			UnitClient uc = new UnitClient(hosts[i], ports[i]);
			try {
				uc.connect();
			} catch (Exception x) {
				Logger.severe(uc + ":" + x.getMessage());
			}
			nodes[i] = uc;
		}
	}

	/**
	 * 设置计算选项
	 * @param ops 选项
	 */
	public void setOptions(String ops) {
		this.opt = ops;
	}

	/**
	 * 随机分配型
	 * @return 随机分配选项返回true
	 */
	public boolean isAOption() {
		return opt != null && opt.indexOf('a') != -1;
	}

	/**
	 * 只有一个作业也分配给所有可用分机，任何一个就算完成，中断其它任务
	 * @return 一个作业选项返回true
	 */
	public boolean is1Option() {
		return opt != null && opt.indexOf('1') != -1;
	}

	/**
	 * 缺省顺次分配
	 * @return 缺省顺次分配返回true
	 */
	public boolean isDefaultOption() {
		return opt == null;
	}
	/**
	 * 设置计算环境的上下文对象
	 * @param ctx 上下文
	 */
	public void setContext(Context ctx) {
		this.ctx = ctx;
		ctx.addResource(this);
	}

	// 该方法随机打乱分机队列
	private void randomUCList() {
		Sequence sortSeq = new Sequence();
		while(!ucList.isEmpty()){
			sortSeq.add(ucList.removeFirst());
		}
		Expression exp = new Expression("rand()");
		sortSeq.sort(exp, null, "o", new Context());
		for(int i=1;i<=sortSeq.length();i++){
			ucList.add((UnitClient)sortSeq.get(i));
		}
	}

	private byte getCalcType() {
		if (isAOption()) {
			return TYPE_RANDOM;
		}
		if (is1Option()) {
			return TYPE_ONE;
		}
		return TYPE_DEFAULT;
	}

	/**
	 * 执行作业，返回计算结果
	 * @return 计算结果
	 */
	public Object execute() {
		if (nodes == null) {
			//本机多线程执行
			return super.execute();
		}
		try {
			checkLiveNodes();
			checkCallerSize();

			byte calcType = getCalcType();
			Object result = null;
			switch (calcType) {
			case TYPE_ONE:
				result = executeOne();
				break;
			default:
				result = execute( calcType );
			}
			return result;
		} finally {
			closeConnects();
		}
	}
	
	/**
	 * 只有一个作业时，使用1选项，分配到所有分机，取最快的结果后返回
	 * @return 计算结果
	 */
	public Object executeOne() {
		Logger.debug("1个作业时，分给所有分机，取最快的结果");
		ThreadPool pool = null;
		try {
			List<ProcessCaller> pCallers = new ArrayList<ProcessCaller>();
			ucList = new LinkedList<UnitClient>();

			int size = nodes.length;
			// 需要容错，找出活动的分机，生成容错队列
			for (int i = 0; i < size; i++) {
				UnitClient uc = nodes[i];
				if (!uc.isConnected()) {
					continue;
				}
				appendClient(uc, false);
				activeHostCount++;
			}

			Sequence argPos = new Sequence();
			argPos.add(1);
			List<List> args = reserveResult(argPos);
//			将作业分给所有的分机
			for (int i = 1; i <= size; i++) {
				ProcessCaller pcaller = new ProcessCaller(args);
				pcaller.setPositions(argPos);
				pcaller.setOneOption();
				pCallers.add(pcaller);
			}

			callers.clear();
			for (ProcessCaller pc : pCallers) {
				callers.add(pc);
			}
			// 主机的工作池，直接是分机的数目
			int poolSize = ucList.size();
			pool = ThreadPool.newSpecifiedInstance(poolSize);
			for (int i = 0; i < size; i++) {
				ProcessCaller pcaller = (ProcessCaller) callers.get(i);
				UnitClient uc = getClient();
				pcaller.setUnitClient(uc);
				pool.submit(pcaller);
			}
			joinCallers();

			return result;
		} catch (Throwable x) {
			if (x instanceof OutOfMemoryError) {
				Logger.severe(x);
			}
			interruptAll(null, x);

			if(x.getMessage().equals(ParallelProcess.ONE_OPTION)){
//				1选项计算结束后，抛出ONE_OPTION异常，打断别的作业，然后直接返回结果。
				return result;
			}
			throw new RuntimeException(x);
		} finally {
			if (pool != null) {
				pool.shutdown();
			}
		}
	}

	/**
	 * 缺省顺次分配
	 * @return 计算结果
	 */
	public Object execute(byte calcType) {
		String msg;
		if(calcType==TYPE_DEFAULT){
			msg = "Dispatch jobs by sorting order.";
		}else{
			msg = "Dispatch jobs on random order.";
		}
		Logger.debug( msg );
		ThreadPool pool = null;
		try {
			List<ProcessCaller> pCallers = new ArrayList<ProcessCaller>();
			ucList = new LinkedList<UnitClient>();

			int size = nodes.length;
			int maxTaskNum = 0;
			HashMap<String, Integer> ucTaskMap = new HashMap<String, Integer>();
			// 需要容错，找出活动的分机，生成容错队列
			for (int i = 0; i < size; i++) {
				UnitClient uc = nodes[i];
				if (!uc.isConnected()) {
					continue;
				}
				int maxNum = uc.getUnitMaxNum();
				if (maxNum > maxTaskNum) {
					maxTaskNum = maxNum;
				}
				ucTaskMap.put(uc.toString(), maxNum);
				appendClient(uc, false);
				activeHostCount++;
			}

			// 根据分机的最大作业数，补齐分机的作业队列
			size = callers.size();
			for (int i = 1; i <= maxTaskNum; i++) {
				for (int n = 0; n < activeHostCount; n++) {
					UnitClient tmp = ucList.get(n);
					Integer tmpMax = ucTaskMap.get(tmp.toString());
					if (i < tmpMax) {
						// 补齐的分机，缺省克隆
						appendClient(tmp);
					}
				}
			}
			
			// 为了防止所有作业全部堆在第一台机器时，结果列表没法全部空出，做此动作以保证结果的个数绝对等于分机个数
			if (reduce != null) {
				int maxIndex = nodes.length;
				result.set(maxIndex, null);
			}

			for (int i = 1; i <= size; i++) {
				Sequence argPos = new Sequence();
				argPos.add(i);
				List<List> args;
				if (reduce != null) {
//					当前还没有分配分机，先用1号分机占返回分机占位。
					args = reserveResult(argPos,1);
				} else {
//					不需要reduce时，返回结果跟参数相等占位
					args = reserveResult(argPos);
				}
				
				ProcessCaller pcaller = new ProcessCaller(args);
				if (reduce == null) {
					pcaller.setPositions(argPos);
				}
				// 允许容错
				pcaller.setDispatchable(true);
				pCallers.add(pcaller);
			}

			callers.clear();
			for (ProcessCaller pc : pCallers) {
				callers.add(pc);
			}
			
			if(calcType==TYPE_RANDOM){
				randomUCList();
			}
			
			// 主机的工作池，得是作业数跟分机的最大作业总数，取小的分配
			int poolSize = Math.min(size, ucList.size());
			pool = ThreadPool.newSpecifiedInstance(poolSize);
			for (int i = 0; i < size; i++) {
				ProcessCaller pcaller = (ProcessCaller) callers.get(i);
				UnitClient uc = getClient();
				if (reduce != null) {
					pcaller.setReduce(reduce, accumulateLocation,currentLocation);//,ucIndex);
				}
				pcaller.setUnitClient(uc);
				pool.submit(pcaller);
			}
			joinCallers();

			if (reduce != null) {
//				有reduce时，最后从各分机将reduce的结果取回
				for(int i=0;i<nodes.length;i++){
					UnitClient uc = nodes[i];
					if(!uc.isAlive()){
						continue;
					}
					Object reduceResult;
					if( uc.isEqualToLocal() ) {
						reduceResult = reduceResults.get(spaceId);
					}else {
						reduceResult = uc.getReduceResult(spaceId);	
					}
					
					int ucIndex = indexOfUC(uc);
					result.set(ucIndex,reduceResult);
				}
			}
			return result;
		} catch (Throwable x) {
			if (x instanceof OutOfMemoryError) {
				Logger.severe(x);
			}
			interruptAll(null, x);

			if (x instanceof RQException) {
				throw (RQException) x;
			}
			throw new RuntimeException(x.getMessage(), x);
		} finally {
			if (pool != null) {
				pool.shutdown();
			}
		}
	}


	/*
	 * 返回顺序号从1开始的分机序号
	 * */
	private int indexOfUC(UnitClient uc) {
		for (int i = 0; i < nodes.length; i++) {
			UnitClient tmp = nodes[i];
			if (uc.equals(tmp)) {
				return i + 1;
			}
		}
		return 0;
	}

	void closeConnects() {
		super.closeConnects();

		if (nodes == null)
			return;
		for (UnitClient uc : nodes) {
			if (uc == null)
				continue;
			uc.close();
		}
	}

	private void checkLiveNodes() {
		int len = nodes.length;
		boolean existLive = false;

		for (int i = 0; i < len; i++) {
			UnitClient uc = nodes[i];
			if (!uc.isConnected())
				continue;
			existLive = true;
		}

		if (!existLive) {
			throw new RQException(mm.getMessage("UnitTasks.noActiveHost"));
		}
	}

	private List<List> reserveResult(Sequence posIndexes) {
		return reserveResult(posIndexes, null);
	}

	private List<List> reserveResult(Sequence posIndexes, Integer ucIndex) {
		return reserveResult(callers, posIndexes, ucIndex);
	}

	private List<List> reserveResult(ArrayList<Caller> acs,
			Sequence posIndexes, Integer ucIndex) {
		List<List> args = new ArrayList<List>();
		for (int i = 1; i <= posIndexes.length(); i++) {
			int pos = (Integer) posIndexes.get(i);
			Caller c = acs.get(pos - 1);
			args.add(c.getArgs());
		}

		if (reduce != null) {
			if (ucIndex != null) {
				result.set(ucIndex, null);
			} else {
				result.add(null);
			}
		} else {
			for (int i = 0; i < posIndexes.length(); i++) {
				result.add(null);
			}
		}
		return args;
	}
	
//往分机分配的作业，一次作业可以有多个参数，可以reduce
	class ProcessCaller extends Caller implements IResource {
		private Object reduce = null;
		private CellLocation accumulateLocation = null;
		private CellLocation currentLocation = null;
		
		private Sequence argPositions = null;
		private boolean isDispatchable = false;

		Context pcCtx = null;
		Expression pcExp = null;

		public ProcessCaller(List<List> argList) {
			super(argList);
		}

		public void setDispatchable(boolean d) {
			isDispatchable = d;
		}
		
		public void setUnitClient(UnitClient uc) throws Exception {
			this.uc = uc;
			if (canRunOnLocal()) {
				// 如果可以本地执行时，不需要从分机获取任务号，也不建立跟分机的常连接
				taskId = UnitServer.nextId();
				return;
			}

			Request req = new Request(Request.DFX_TASK);
			req.setAttr(Request.TASK_DfxName, dfx);
			req.setAttr(Request.TASK_ArgList, argList);
			req.setAttr(Request.TASK_SpaceId, spaceId);
			req.setAttr(Request.TASK_IsProcessCaller, true);
			req.setAttr(Request.TASK_Reduce, reduce);
			req.setAttr(Request.TASK_AccumulateLocation, accumulateLocation);
			req.setAttr(Request.TASK_CurrentLocation, currentLocation);

			Response res = uc.send(req);
			if (res.getException() != null) {
				throw res.getException();
			}
			taskId = (Integer) res.getResult();
			registResource();
		}

		private void registResource() {
			ctx.getJobSpace().addHosts(uc.getHost(), uc.getPort());
			ctx.addResource(this);
		}

		public void setReduce(Object reduce, CellLocation accumulateLocation, CellLocation currentLocation){//, int argPos) {
			this.reduce = reduce;
			this.accumulateLocation = accumulateLocation;
			this.currentLocation = currentLocation;
		}

		public void setPositions(Sequence argPos) {
			this.argPositions = argPos;
		}

		void setResponseValue(Object rVal) {
			int index = 0;
			if (reduce != null) {
			} else {
				Sequence pos = argPositions;
				Sequence val = null;
				if (rVal instanceof Sequence) {
					val = (Sequence) rVal;
				} else {
					// 当参数小于host.length时，也是采用多个机器里面找最大算力机器，但由于此时每台分机最多一个作业，
					// 所以返回值不是Sequence，此时的argPositions只能为一个成员的序列，此处将result也转换为
					// 只有一个成员的序列。
					val = new Sequence();
					val.add(rVal);
				}

				for (int i = 1; i <= pos.length(); i++) {
					index = (Integer) pos.get(i);
					Object tmp = null;
					if (i <= val.length()) {
						tmp = val.get(i);
					} else {
						Logger.severe(mm.getMessage("ParallelCaller.emptysub"));//"子程序返回的值为空！");
					}
					setResult(index, tmp);
				}
			}
		}

		public void close() {
			interruptAll(this, new Exception(TERMINATE));
			if (uc != null) {
				uc.close();
			}
			ctx.removeResource(this);
		}

		// 出过错的分机，不能再重新分配
		private transient HashSet<UnitClient> errorNodes = new HashSet<UnitClient>();

		public void run() {
			if (!isDispatchable) {
				super.run();
				return;
			}

			isRunning = true;
			try {
				while (true) {
					if (isCanceled) {
						Logger.debug(mm.getMessage("ParallelProcess.canceled",
								this));
						break;
					}
					try {
						long l1 = System.currentTimeMillis();
						Logger.debug(mm.getMessage("Task.taskBegin", this));
						if (canRunOnLocal()) {
							runOnLocal( true );
						}else{
							runOnNode();
						}
						long l2 = System.currentTimeMillis();
						DecimalFormat df = new DecimalFormat("###,###");
						long lastTime = l2 - l1;
						Logger.debug(mm.getMessage("Task.taskEnd", this,
								df.format(lastTime)));// this+" 开始计算。");
						break;
					} catch (RetryException re) { // 需要重新分配分机异常时
						releaseClient(uc);
						if (!errorNodes.contains(uc)) {
							errorNodes.add(uc.clone());
						}
						try {
							UnitClient tmpuc = getDispatchNode(errorNodes,
									args2String(argList), re.getMessage());
							setUnitClient(tmpuc);
							Logger.debug(mm.getMessage(
									"ParallelProcess.reassign", this));
						} catch (Exception ex) {
							interruptAll(this, ex);
							break;
						}
					}
				}
			} catch (Throwable t) {
				interruptAll(this, t);
			} finally {
				releaseClient(uc);
				isRunning = false;
			}
		}
	}

	/**
	 * 获取一个可以执行的分机,用于容错某个执行中的任务因网络中断或者分机死机等， 重新分配分机
	 * 
	 * @return UnitClient 分机客户端
	 */
	public UnitClient getDispatchNode(HashSet<UnitClient> errorNodes,
			String argString, String cause) throws Exception {
		if (errorNodes.size() == activeHostCount) {
			throw new Exception(mm.getMessage("ParallelProcess.exeFail",
					argString, cause));
		}
		UnitClient uc = getClient();
		while (contains(errorNodes, uc)) {
			releaseClient(uc);
			uc = getClient();
		}
		return uc;
	}

	// 该方法只根据主进程端口号来比较，所以同一个分机的子进程，是相同的。
	// errorNodes.contains会比较hashKey，会将子进程端口也算进去
	private boolean contains(HashSet<UnitClient> errorNodes, UnitClient uc) {
		Iterator<UnitClient> nodes = errorNodes.iterator();
		while (nodes.hasNext()) {
			UnitClient tmp = nodes.next();
			if (tmp.equals(uc)) {
				return true;
			}
		}
		return false;
	}

}
