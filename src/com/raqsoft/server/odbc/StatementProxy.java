package com.raqsoft.server.odbc;

import java.util.ArrayList;
import java.util.List;

import com.raqsoft.app.common.AppUtil;
import com.raqsoft.common.ArgumentTokenizer;
import com.raqsoft.common.Escape;
import com.raqsoft.common.Logger;
import com.raqsoft.common.StringUtils;
import com.raqsoft.common.UUID;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.fn.Eval;
import com.raqsoft.parallel.Response;
import com.raqsoft.parallel.Task;
import com.raqsoft.server.IProxy;
import com.raqsoft.server.unit.UnitServer;
import com.raqsoft.util.DatabaseUtil;

/**
 * Statement代理器
 * 
 * @author Joancy
 *
 */
public class StatementProxy extends IProxy {
	String cmd = null;
	ArrayList params = null;
	String dfx = null;
	List args = null;
	Task task = null;

	/**
	 * 创建Statement代理器
	 * @param cp 连接代理
	 * @param id 代理编号
	 * @param cmd 查询命令
	 * @param params 参数数组
	 * @throws Exception
	 */
	public StatementProxy(ConnectionProxy cp, int id, String cmd,
			ArrayList<Object> params) throws Exception {
		super(cp, id);
		this.cmd = cmd;
		if (!StringUtils.isValidString(cmd)) {
			throw new Exception("Prepare statement cmd is empty!");
		}
		Logger.debug("StatementProxy cmd:\r\n"+cmd);
		this.params = params;
		if (isDfx()) {
			standardizeDfx();
			String spaceId = UUID.randomUUID().toString();
			task = new Task(dfx, args, id, spaceId);
		} else {//否则表达式
		}
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
	 * 获取命令,可能是call dfx: {call("a",2,?)}
	 * @return 要执行的查询命令
	 */
	public String getCmd() {
		return cmd;
	}

	private boolean isDfx() {
		cmd = cmd.trim();
		if (cmd.startsWith("{") && cmd.endsWith("}"))
			cmd = cmd.substring(1, cmd.length() - 1);
		
		String lower = cmd.toLowerCase();
		return lower.startsWith("call ");
	}

	private void standardizeDfx() {
		if (!isDfx())
			return;
		String tmp = cmd;
		
		int left = tmp.indexOf('(');
		if (left == -1)
			throw new RuntimeException(cmd +" must contain '()'");
		
		if (!tmp.endsWith(")"))
			throw new RuntimeException(cmd+" must end with ')'");

		String name = tmp.substring(5, left).trim();
		
		dfx = standardizeDfx(name);
		String strparams = tmp.substring(left+1,tmp.length()-1);
		args = standardizeArg(strparams);
	}

	private String standardizeDfx(String dfxName) {
		if (!dfxName.toLowerCase().endsWith(".dfx"))
			dfxName += ".dfx";
		return dfxName;
	}
	
//兼容单，双引号
	private static String adjustQuote(String args){
		ArgumentTokenizer at1 = new ArgumentTokenizer( args );
		StringBuffer buf = new StringBuffer();
		while(at1.hasMoreTokens()){
			String arg = at1.nextToken();
			String tmp = Escape.removeEscAndQuote(arg);
			if(tmp.equals(arg)){
				arg = tmp;
			}else{
				arg = Escape.addEscAndQuote(tmp);
			}
			if(buf.length()>0){
				buf.append(",");
			}
			buf.append(arg);
		}
		return buf.toString();
	}
	
	private List standardizeArg(String strParams) {
		if(params==null) throw new RuntimeException("You didn't bind any parameter for Call dfx()!");
		
		String exp = "["+adjustQuote(strParams)+"]";
		Context ctx = new Context();
		Sequence arg = new Sequence();
		for(Object o:params){
			arg.add(o);
		}
		Sequence o = (Sequence)Eval.calc(exp, arg, null, ctx);
		List cmpArgs = new ArrayList<Object>();
		int len = o.length();
		for(int i=1;i<=len; i++){
			cmpArgs.add(o.get(i));
		}
		return cmpArgs;
	}

	/**
	 * 取参数值
	 * @return 参数列表
	 */
	public List<String> getParams() {
		return params;
	}

	/**
	 * 获取结果集代理
	 * @param id 代理编号
	 * @return 结果集代理器
	 * @throws Exception
	 */
	public ResultSetProxy getResultSetProxy(int id) throws Exception{
		ResultSetProxy rsp = (ResultSetProxy)getProxy(id);
		if(rsp==null){
			throw new Exception("ResultSet "+id+" is not exist or out of time!");
		}
		
		return rsp;
	}

	/**
	 * 执行当前命令
	 * @return 结果集的代理号数组
	 * @throws Exception
	 */
	public int[] execute() throws Exception {
		int[] resultIds;
		if (task != null) {
			ICursor[] cursors = task.executeOdbc();
			int size = cursors.length;
			resultIds = new int[size];
			for (int i = 0; i < size; i++) {
				ICursor cursor = cursors[i];
				int resultId = OdbcServer.nextId();
				resultIds[i] = resultId;
				ResultSetProxy rsp = new ResultSetProxy(this, resultId, cursor);
				addProxy(rsp);
			}
		} else {
			Context context = Task.prepareEnv();

			try{
				ICursor cursor;
				Object obj;
				if(params!=null && params.size()>0){
					List<Object> args = new ArrayList<Object>();
					for(Object arg:params){
						args.add(arg);
					}
					obj = AppUtil.executeSql(cmd, args, context);
				}else{
					obj = AppUtil.executeCmd(cmd, context);
				}
				
				if(obj instanceof ICursor){
					cursor = (ICursor)obj;
				}else{
					cursor = Task.toCursor( obj );
				}
				
				resultIds = new int[1];
				int resultId = OdbcServer.nextId();
				resultIds[0] = resultId;
				ResultSetProxy rsp = new ResultSetProxy(this, resultId, cursor);
				addProxy(rsp);
			}finally{
				DatabaseUtil.closeAutoDBs(context);
			}
		}
		return resultIds;
	}

	/**
	 * 取消当前任务
	 * 若dfx运行中则dfx.interrupt()，否则啥也不干
	 * @return 取消成功，返回true
	 * @throws Exception
	 */
	public boolean cancel() throws Exception {
		Response res = task.cancel();
		if(res.getException()!=null){
			throw res.getException();
		}
		return true;
	}

	/**
	 * 取开始计算时间，未开始或有效被中断则返回-1
	 * @return 开始计算的时间，整数表示
	 */
	public long getStartTime() {
		return task.getCallTime();
	}

	/**
	 * 取计算结束时间，未完成或有效被中断则返回-1
	 * @return 结束时间
	 */
	public long getEndTime() {
		return task.getFinishTime();
	}

	// close时关闭所有ResultSetProxy，并从ConnectionProxy删除自己

	/**
	 * 设结果集返回的最大行数
	 * @param max
	 */
	public void setMaxRows(int max) {
	}

	/**
	 * 是否有下一个结果集
	 * @return false
	 */
	public boolean hasNextResultSet() {
		return false;
	}

	/**
	 * 下一结果集，dfx或dql返回的非ICursor需要封装成ICursor
	 * 单个普通对象先封装成序列
	 * 单个序列封装成MemoryCursor
	 * 向前台发送时若是普通序列构成的游标则返回列名为_1的结果集
	 * @return null
	 */
	public ICursor nextResultSet() {
		return null;
	}

	/**
	 * 关闭当前代理
	 */
	public void close() {
	}

	/**
	 * 实现toString方法
	 */
	public String toString() {
		return "Statement " + getId();
	}

}