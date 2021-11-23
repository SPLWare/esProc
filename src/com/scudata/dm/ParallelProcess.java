package com.scudata.dm;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import com.scudata.cellset.INormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.*;
import com.scudata.expression.Expression;
import com.scudata.parallel.*;
import com.scudata.resources.*;
import com.scudata.server.unit.UnitServer;
import com.scudata.thread.Job;
import com.scudata.thread.ThreadPool;
import com.scudata.util.CellSetUtil;

/**
 * 并发进程
 * 对应于callx不带H的情形，在本机进程上，模拟线程执行任务
 * 分机接到的pCaller也是该类执行，pCaller可以一次处理一个或者多个参数
 * @author Joancy
 *
 */
public class ParallelProcess implements IResource{
//	当参数个数小于等于h的个数时，忽略reduce动作，此时传递给该类的reduce为下述值。
//	合并或者设置返回值时，需要根据REDUCE的类型做不同操作
//	public static String REDUCE_NULL = "NULL";
//	public static String REDUCE_MAIN = "MAIN";//只在主控端做reduce，分机等同于REDUCE_NULL
	public static String ONE_OPTION = "1选项计算已完成。";
	
	Object dfx;// dfx可以是1：dfx文件名字；2：一段代码；3：一个PgmCellSet对象
	// host可以 省略，省略时，从本机多线程执行
	String spaceId = null;
	Object reduce=null;// reduce 可以是一个串文件名，或者一个PgmCellSet
	CellLocation accumulateLocation=null;//如果参数位置为null时， 则使用第1个参数当累计值
	CellLocation currentLocation=null;// 如果参数位置为null时， 则使用第2个参数当当前值

//	transient Expression reduceExp = null;
//	transient Context reduceCtx = null;
	
	ArrayList<Caller> callers = new ArrayList<Caller>();
	Sequence result = new Sequence();
//	int subPort = 0;
	
//	Object lock = new Object();

	private volatile boolean interrupt = false;
	private Throwable interruptException;

	HostManager hostManager = HostManager.instance();
	
//	容错算法时，该队列会产生uc的适合作业数个副本，但不会初始就连接，用到它时才会真正连接
//	比如两个uc， 一个适合作业3，一个适合作业4，则会产生队列长度为7的分机副本
	LinkedList<UnitClient> ucList = null;
	
	transient volatile boolean isCanceled = false;
	String TERMINATE = "Terminated by user.";
	
	//主进程的任务Id
	private int processTaskId = 0;
	static MessageManager mm = ParallelMessage.get();
	static Map<String,Object> reduceResults = Collections.synchronizedMap(new HashMap<String,Object>());
	
	/**
	 * 指定计算对象的构造函数
	 * @param dfx 被计算的对象，  dfx可以是
	 * 1：dfx文件名字；
	 * 2：一段SPL代码；
	 * 3：一个PgmCellSet对象
	 */
	public ParallelProcess(Object dfx) {
		if (dfx instanceof String || dfx instanceof PgmCellSet) {
			this.dfx = dfx;
		} else {
			String className = dfx.getClass().getName();
			throw new RuntimeException(
					"ParallelCaller does not support class type:" + className);
		}
		TERMINATE = mm.getMessage("ParallelProcess.terminate");
	}
	
	/**
	 * 设置子端口号
	 * @param port 端口号
	 */
//	public void setSubPort(int port){
//		this.subPort = port;
//	}
	
	/**
	 * 设置当前进程的任务号
	 * @param pTaskId 任务号
	 */
	public void setProcessTaskId(int pTaskId){
		this.processTaskId = pTaskId;
	}
	
	/**
	 * 实现toString方法，方便显示当前进程的参数内容
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(dfxDelegate(dfx));
		sb.append("   ");
		int size = callers.size();
		List<Object> args = new ArrayList<Object>();
		for(int i=0;i<size; i++){
			args.addAll(callers.get(i).getArgs());
		}
		sb.append(args2String(args));
		return sb.toString();
	}

	private static String getListString(List list){
		StringBuffer sb = new StringBuffer();
		sb.append("[ ");
		int size = list.size();
		int min = 2;
		min = Math.min(min, size);
		for (int i = 0; i < min; i++) {
			if (i > 0) {
				sb.append(",");
			}
			Object mem = list.get(i);
			if(mem instanceof List){
				sb.append(getListString((List)mem));
			}else{
				sb.append( mem );
			}
		}
		if(min<size){
			sb.append("...");
			Object mem = list.get(size-1);
			if(mem instanceof List){
				sb.append(getListString((List)mem));
			}else{
				sb.append( mem );
			}
		}
		sb.append(" ]");
		return sb.toString();
	}
	
	void closeConnects(){
		if(ucList==null) return;
		for(UnitClient uc:ucList){
			uc.close();
		}
	}
	
	/**
	 * 将参数列表的多个参数转化为可读串
	 * @param argList 参数列表
	 * @return 转化的串
	 */
	public static String args2String(List argList) {
		StringBuffer sb = new StringBuffer();
		if (argList != null) {
			MessageManager mm = EngineMessage.get();
			sb.append(mm.getMessage("callx.arg"));
			sb.append(" = ");
			List sub = argList;
			if(argList.size()==1 && argList.get(0) instanceof List){
				sub = (List)argList.get(0);
			}

			sb.append( getListString(sub) );
		}
		return sb.toString();
	}

	/**
	 * 设置任务空间号
	 * @param id 任务空间
	 */
	public void setJobSpaceId(String id) {
		this.spaceId = id;
	}
	
	/**
	 * 设置任务执行完的reduce表达式
	 * @param reduce reduce表达式或者reduce网格对象
	 */
	public void setReduce(Object reduce){
		if(reduce==null){
			return;
		}
		if(reduce instanceof PgmCellSet){
			this.reduce = (PgmCellSet)reduce;
		}else{
			String dfx = (String)reduce;
			DfxManager dfxManager = DfxManager.getInstance();
			FileObject fo = new FileObject(dfx, "s");
			PgmCellSet pcs = dfxManager.removeDfx(fo, new Context());
			this.reduce = pcs;
		}
//		this.reduce = reduce;
//		if(needReduce() && isExpReduce() ){
//			reduceCtx = new Context();
//			reduceExp = new Expression(reduceCtx, (String)reduce);
//		}
	}
	
	/**
	 * 设置网格reduce对象
	 * @param reduce 网格计算reduce对象
	 * @param accumulateLocation 累计值位置
	 * @param currentLocation    当前值位置
	 */
	public void setReduce(Object reduce,CellLocation accumulateLocation, CellLocation currentLocation){
		setReduce(reduce);
		this.accumulateLocation = accumulateLocation;
		this.currentLocation = currentLocation;
	}

	/**
	 * 判断当前的dfxarg是一个文件名还是一串代码
	 * 
	 * @param dfxarg dfx对象
	 * @return 如果是一段脚本，返回true，否则返回false
	 */
	public static boolean isScript(Object dfxarg) {
		if (!(dfxarg instanceof String))
			return false;
		String dfx = (String) dfxarg;
		boolean b = dfx.indexOf('\t') > 0 || dfx.indexOf('\n') > 0;
		if (b)
			return true;// 多行代码肯定是脚本
		b = dfx.toLowerCase().startsWith("return ");// 只有一行代码时，写了返回的也是脚本
		if (b)
			return true;
		b = dfx.toLowerCase().endsWith(".dfx");
		return !b;// 形如 123 串当脚本处理
	}

	/**
	 * 获取dfx对象的缩写表示，用于打出提示或者调试信息
	 * 当dfx是一串代码时，提示信息只显示头十个字母长度
	 * @param dfx 计算对象
	 * @return 缩写表示或对象描述串
	 */
	public static String dfxDelegate(Object dfx) {
		boolean isScript = isScript(dfx);
		if (isScript) {
			String str = (String) dfx;
			if (str.length() > 10) {
				return str.substring(0, 10) + "...";
			}
		} else if (dfx instanceof PgmCellSet) {
			PgmCellSet pcs = (PgmCellSet) dfx;
			if(pcs.getName()==null) return "PgmCellSet";
			return "PgmCellSet[" + pcs.getName() + "]";
		}
		return (String) dfx;
	}

	/**
	 * 设置计算对象
	 * @param dfx
	 */
	public void setDfx(Object dfx) {
		this.dfx = dfx;
	}

	/**
	 * 增加一个子作业
	 * @param argList 子作业的执行参数
	 * @throws RQException 由于参数需要被序列化后发送到别的分机去执行，
	 * 所以没实现序列化接口的参数会抛出异常
	 */
	public void addCall(List<Object> argList) throws RQException {
		for (Object obj : argList) {
			if(obj==null){
				continue;
			}
			if (obj instanceof Serializable) {
				continue;
			}
			throw new RQException(mm.getMessage("ParallelProcess.invalidarg",obj));
		}
		Caller caller = new Caller(argList);
		callers.add(caller);
	}

	private int indexOf(Caller caller) {
		for (int i = 0; i < callers.size(); i++) {
			if (callers.get(i).equals(caller)) {
				return i + 1;
			}
		}
		return 0;
	}

	void setResult(int index, Object val) {
		synchronized ( reduceResults ) {
			result.set(index, val);
		}
	}

	/**
	 * 取消执行当前作业
	 * @param reason 取消作业的原因
	 */
	public void cancel(String reason){
		isCanceled = true;
		if(reason!=null){
			TERMINATE = reason;
		}
		int size = callers.size();
		for (int i = 0; i < size; i++) {
			Caller caller = callers.get(i);
			caller.cancel();
		}
	}
	
	void joinCallers() throws Throwable{
		try {
			int size = callers.size();
			for (int i = 0; i < size; i++) {
				Caller caller = callers.get(i);
				caller.join();
			}
		} catch (Exception x) {
			interruptAll(null, x);
			throw x;
		}

		if (interrupt) {
			throw interruptException;
		}
	}

	void checkCallerSize() {
		int size = callers.size();
		if (size == 0) {
//		没有参数也能调用callx，用于仅仅需要执行分机的一个dfx，但不需要参数的情形。
			Caller caller = new Caller(new ArrayList());
			callers.add(caller);
		}
	}
	
	/**
	 * 获取当前作业下的线程数目，用于调试
	 * @return 线程计数
	 */
	public static int threadCount() {
		return threadCount(false);
	}
	
	/**
	 * 获取线程数目
	 * @param showName 将当前线程的名字同时打印到控制台，用以调试
	 * @return 线程计数
	 */
	public static int threadCount(boolean showName) {
		Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
		if(showName){
		 Iterator<Thread> it = map.keySet().iterator();
		 while(it.hasNext())
		 System.out.println( it.next() );
		}

		return map.size();
	}
	
	/**
	 * 执行当前作业
	 * @return 计算结果
	 */
	public Object execute(){
		checkCallerSize();
		int size = callers.size();
		
		int min = Env.getParallelNum();
/**		在主控端本地执行
		if( subPort==0 ){
			if(hostManager.getHost()!=null){
				LinkedList<UnitClient>  tmpList = (LinkedList<UnitClient>)hostManager.listUnits(size);
				ucList = new LinkedList<UnitClient>();
				while(tmpList.size()>0){
					appendClient(tmpList.removeFirst());
				}
				min = Math.min(size, hostManager.getPreferredTaskNum());
	//			进程数目少于适合作业数时，要补足进程的多线程并发
				if(ucList.size()<min){
					int cha = min - ucList.size();
					for(int i=0;i<cha; i++){
						appendClient(ucList.get(i));
					}
				}
			}
		}else{
			min = Math.min(size, min);
		}
**/		
		ThreadPool pool = ThreadPool.newSpecifiedInstance( min );
		try {
			for (int i = 0; i < size; i++) {
				if( isCanceled ) continue;
				if( !needReduce() ){
					result.add(null);
				}
				Caller caller = callers.get(i);
//				每个分进程的UC只有一个，且是共享的，所以每个分进程执行的作业只能是一个，也就是此处保证了分进程的串行作业
//				UnitClient uc = getClient();
				caller.setUnitClient( null );//全部为本机线程执行
				pool.submit(caller);
			}
			joinCallers();
			return getResult();
		}catch(RetryException re){
			throw re;
		}catch (Throwable x) {
			interruptAll(null, x);
			throw new RuntimeException(x);
		}finally{
			pool.shutdown();
			closeConnects();
		}
	}
	
	boolean needReduce(){
		return !(reduce==null);// || reduce.equals(REDUCE_NULL) || reduce.equals(REDUCE_MAIN));
	}
	
	/**
	 * reduce动作分两种，一种直接表达式计算
	 * 另一种使用网格设置参数来计算
	 * @return 如果是表达式计算的reduce返回true
	 */
	boolean isExpReduce(){
		return (reduce instanceof String);
	}
	
	/**
	 * 主机在所有作业都计算完成后，根据SPACEID来获取分机上的reduce的最终结果
	 * 结果取完后，同时清空分机的结果缓存。
	 * @param SPACEID 任务的空间ID
	 * @return 任务在某分机上的reduce结果
	 */
	public static Object getReduceResult(String SPACEID){
		Object val = reduceResults.get(SPACEID);
		reduceResults.remove(SPACEID);
		return val;
	}
	
	Object getResult(){
		if( needReduce() ){
//			reduce的单步计算结果不在作业中返回，而是记在reduceResults中，单步作业直接返回true告诉主机reduce的结果计算完成
			return true;
//			Object val = reduceResults.get(spaceId);
//			reduceResults.remove(spaceId);
//			return val;
		}else{
			return result;
		}
	}
	
	Object reducePgmCellSet(Object prevValue, Object curValue)throws Exception{
		Context context = Task.prepareEnv();

		PgmCellSet pcs = (PgmCellSet)reduce;
		pcs.setContext(context);
		if(accumulateLocation==null){//没有位置信息时，则使用参数位置
			CellSetUtil.putArgValue(pcs, new Object[]{prevValue,curValue});
		}else{
			int row = accumulateLocation.getRow();
			int col = accumulateLocation.getCol();
			INormalCell nc = pcs.getCell(row, col);
			nc.setValue(prevValue);

			row = currentLocation.getRow();
			col = currentLocation.getCol();
			nc = pcs.getCell(row, col);
			nc.setValue(curValue);
		}
		pcs.calculateResult();
		return pcs.nextResult();
	}
	
	Object reduce(Object prevValue, Object curValue, Expression exp, Context ctx){
		Param param = ctx.getIterateParam();
		Object oldVal = param.getValue();
		param.setValue(prevValue);
		
		Sequence tmp = new Sequence(1);
		tmp.add(curValue);
		ComputeStack stack = ctx.getComputeStack();
		stack.push(tmp.new Current(1));
		
		try {
			Object val = exp.calculate(ctx);
			return val;
		} finally {
			param.setValue(oldVal);
			stack.pop();
		}
	}

	// 当某分错误时打断其它的
	void interruptAll(Caller master,Throwable x) {
		isCanceled = true;
		if (interrupt) {
			return; // 后续线程有错时，一旦打断过，后续线程错误都忽略
		}
		interrupt = true;
		interruptException = x;
		TERMINATE = x.getMessage();
		
		int size = callers.size();
		
//Dispatchable Execute时，每个分机只会有一个作业，此时虽然也有打断，但其实就是打断自己
//不用执行后面打断其他caller的代码		
		if(size==1){
			return;
		}
		boolean needCancel = false;
		for (int i = 0; i < size; i++) {
			Caller caller = callers.get(i);
				if(caller.cancel()){
					needCancel = true;
				}
		}
		
		if( !needCancel ) return;
		Logger.info(mm.getMessage("ParallelProcess.cancelfor",this,TERMINATE));
		
		while(true){
			boolean isAllFinished = false;
			for (int i = 0; i < size; i++) {
				Caller caller = callers.get(i);
//				发起者自身的代码要等待继续，所以此处的发起Caller要跳过
				if(caller!=master){
					if(caller.isRunning()){
						isAllFinished = false;
						break;
					}
				}
				isAllFinished = true;
			}
			if(isAllFinished){
				break;
			}
		}
	}

/**
 * 追加一个分客户端uc，放入到队列的uc，得有dispatchable标志
 * @param uc 分机客户端
 */
	public void appendClient(UnitClient uc){
		appendClient(uc,true);
	}
	
	/**
	 * 追加一个计算的分机客户端，
	 * 需要容错的客户端，相当于另一个uc备份，也有根据适合作业数扩展的uc，需要克隆出来
	 * @param uc 客户端
	 * @param needClone 是否需要克隆客户端
	 */
	public void appendClient(UnitClient uc,boolean needClone){
		UnitClient ucClone = uc;
		//
		if(needClone){
			ucClone = uc.clone();
		}
		try{
			ucClone.setDispatchable();
			ucList.add(ucClone);
		}catch(Exception x){
//		由于socket受到机器资源限制，所以即使是已经连接上的node，也仍然可能建立新连接出错
		}
	}
	
	/**
	 * 作业计算完成后，释放客户端
	 * @param uc 客户端
	 */
	public void releaseClient(UnitClient uc) {
		if(uc==null || ucList==null){
			return;
		}
//		如果不是从队列里面获取到的UC，释放时不加入到队列
		if(!uc.isDispatchable()){
			return;
		}
		synchronized(ucList) {
			ucList.add(uc);
			ucList.notify();
		}
	}
	
	/**
	 * 客户端队列里面获取一个可以计算的对象实例
	 * getClient方法必须与releaseClient成对使用
	 * @return 如果有空闲uc对象，则返回它，否则会等待uc被释放
	 */
	public UnitClient getClient(){
//		没有写host参数时，直接在本地多线程执行。直接返回null客户端
		if(ucList==null){
			if(hostManager.getHost()==null){//如果不是在分机中执行时，都是本地线程
				return null;
			}
//			if(subPort>0){
//				return new UnitClient(hostManager.getHost(),subPort);
//			}
		}
		
		synchronized(ucList) {
			if (ucList.size() == 0) {
				try {
					ucList.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return null;
				}
			}
			return  ucList.removeFirst();
		}
		
	}
	
	
	class Caller extends Job{
		List<?> argList;
		UnitClient uc = null;
		Integer taskId = null;
		private boolean isOneOption = false;//如果是1选项，则当前作业计算完成后，就立刻中断其他的作业
		
		transient boolean isRunning = false;
		public Caller(List<?> argList) {
			this.argList = argList;
		}
		
		public void setOneOption(){
			isOneOption = true;
		}

		
		boolean isRunning(){
			return isRunning;
		}
		
		public List<?> getArgs(){
			return argList;
		}

		public void setUnitClient(UnitClient uc) throws Exception {
			this.uc = uc;
			if (canRunOnLocal()) {
				// 如果可以本地执行时，不需要从分机获取任务号，也不建立跟分机的常连接
				taskId = UnitServer.nextId();
				return;
			}

			Request req = new Request(Request.DFX_TASK);
			req.setAttr(Request.TASK_DfxName, getDfxObject());
			req.setAttr(Request.TASK_ArgList, argList);
			req.setAttr(Request.TASK_SpaceId, spaceId);
			req.setAttr(Request.TASK_ProcessTaskId, processTaskId);
			
			Response res = uc.send(req);
			if (res.getException() != null) {
				throw res.getException();
			}
			taskId = (Integer) res.getResult();
		}

		public boolean cancel() {
			isCanceled = true;
			if(!isRunning) return false;
			if (canRunOnLocal()) {
				try {
					Task t = (Task)TaskManager.getTask(taskId);
					t.cancel( TERMINATE );
				} catch (Exception x) {
				}
			} else {
				uc.cancel(taskId, TERMINATE );
			}
			return true;
		}

		public void breakOff() { // 网络断开时，关闭相关通信流
			try {
				uc.close();
			} catch (Exception x) {
			}
		}

		String getErrorDesc(String errMsg) {
			MessageManager mm = EngineMessage.get();
//			由于errMsg在后续堆栈会打出来，改为在message资源中忽略errMsg消息。也即如下参数errMsg传进去，但无效
			return mm.getMessage("callx.error", dfxDelegate(dfx),
					errMsg + "\r\n", this);
		}

		void runOnNode() throws Throwable{
			try {
				Request req = new Request(Request.DFX_CALCULATE);
				req.setAttr(Request.CALCULATE_TaskId, taskId);
				// 分机如果断网时，说不定会阻塞在哪，所以要时刻保持ois以及oos；以便监控线程判断出断网时，可以强行
				// 关闭读写流。
				Response res = uc.send(req);

				if (res.getException() != null) {
					Exception ex = res.getException();
					if (ex instanceof RetryException) {
							throw ex;
					}
//					取消操作也可以能是从DataStoreconsole发来的，此时仍然需要中断别人。
					if (ex instanceof CanceledException) {
						// 已经是被别人取消的任务，不需要再调用interruptAll
					} else {
						throw ex;
					}
				} else if (res.getError() != null) { // 一般为内存溢出错误
					Error err = res.getError();
					throw err;
				} else {
					Object result = res.getResult();
					setResponseValue(result);
					if( isOneOption ){
						throw new Exception(ONE_OPTION);
					}
				}
			} catch (Throwable t) {
				throw t;
			}
		}

		void setResponseValue(Object val) throws Exception{
			if( needReduce() ){
				synchronized ( reduceResults ) {
					Object accumulateResult = reduceResults.get(spaceId);
					if(accumulateResult==null){
						accumulateResult = val;
					}else{
//						if( isExpReduce() ){
//							accumulateResult = reduce( accumulateResult, val, reduceExp, reduceCtx);
//						}else{
							accumulateResult = reducePgmCellSet( accumulateResult, val);
//						}
					}
					reduceResults.put(spaceId, accumulateResult);
				}
			}else{
				int index = indexOf(this);
				setResult( index, val );
			}
		}
		
		/**
		 * 程序网不能并发，所以每个实例一个克隆
		 */
		private Object getDfxObject(){
			Object dfxObj = dfx;
			if (dfx instanceof PgmCellSet) {
				PgmCellSet pcs = (PgmCellSet)dfx;
				dfxObj = pcs.deepClone();
			}
			return dfxObj;
		}
		
		private void runOnLocal() throws Throwable{//RedispatchableException {
			try {
				Task task = new Task(getDfxObject(), argList, taskId, spaceId);
				TaskManager.addTask(task);
				
				Response res = task.execute();
				if (res.getException() != null) {
					Exception ex = res.getException();
					if (ex instanceof RetryException) {
							throw ex;
					}
					
					if (ex instanceof CanceledException) {
//						 已经是被别人取消的任务，不需要再调用interruptAll
					} else {
						throw ex;
					}
				} else if (res.getError() != null) { // 一般为内存溢出错误
					Error err = res.getError();
					throw err;
				} else {
					Object result = res.getResult();
					setResponseValue(result);
				}
			} catch (CanceledException ce) {
				Logger.info(mm.getMessage("ParallelProcess.cancelfor",this,ce.getMessage()));
			} catch (Throwable t) {
				throw t;
			}
		}

		private boolean canRunOnLocal() {
			if (uc == null) {
				return true;
			}
			// 如果分机的host和IP跟IDE设置的环境变量中的本地地址相同，则也是本地线程执行 2013.10.22
			return uc.isEqualToLocal();
		}

		public void run() {
			isRunning = true;
			try {
				PerfMonitor.enterProcess();
				
				boolean cancel = isCanceled;
				if( !cancel ){
					long l1 = System.currentTimeMillis();
					Logger.debug( mm.getMessage("Task.taskBegin",this));//this+" 开始计算。");
					if (canRunOnLocal()) {
						runOnLocal();
					}else{
						runOnNode();
					}
					long l2 = System.currentTimeMillis();
					DecimalFormat df = new DecimalFormat("###,###");
					long lastTime = l2 - l1;
					Logger.debug(mm.getMessage("Task.taskEnd",this,df.format(lastTime)));//this+" 计算完成，耗时："+df.format(lastTime)+ " 毫秒。 ");
				}else{
					Logger.info(mm.getMessage("ParallelProcess.cancelfor",this,TERMINATE));
				}
			} catch (Throwable t) {
				interruptAll(this, t);
			}finally {
				PerfMonitor.leaveProcess();
				releaseClient(uc);
				isRunning = false;
			}
		}

		public String getArgDesc(){
			return args2String(argList);
		}
		
		public String toString() {
			MessageManager mm = EngineMessage.get();

			StringBuffer sb = new StringBuffer();
			if (uc != null) {
				sb.append(uc);
			} else {
				sb.append(dfxDelegate(getDfxObject()));
//				sb.append(mm.getMessage("callx.local"));
			}
			sb.append("  ");
			sb.append( getArgDesc()+" " );
//			MessageManager pm = ParallelMessage.get();
//			if(canRunOnLocal() || UnitServer.instance==null ){//客户机上或者主进程时
//				sb.append(pm.getMessage("Task.taskid",taskId));//" 任务号=[ "+ taskId+" ]"
//			}else{
//				sb.append(pm.getMessage("Task.taskid2",taskId));//" 子任务号=[ "+ taskId+" ]"
//			}
			return sb.toString();
		}

	}


	public void close() {
		cancel(null);
	}

}
