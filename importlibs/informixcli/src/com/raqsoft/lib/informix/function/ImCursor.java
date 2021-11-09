package com.raqsoft.lib.informix.function;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.raqsoft.common.Logger;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.lib.informix.helper.Fragment;
import com.raqsoft.lib.informix.helper.IfxConn;
import com.raqsoft.lib.informix.helper.ImCommand;
import com.raqsoft.lib.informix.helper.ImTableInfo;
import com.raqsoft.lib.informix.function.multiCursor.BPipeReader;

public class ImCursor extends ICursor {
	private Context m_ctx;	
	private IfxConn m_ifxConn = null;
	private BPipeReader m_read = null;
	private Process m_process = null;
	private static int m_nShellSerial = 1;
	private ImTableInfo m_tblInfo;
	private boolean m_bEnd = false;
	private static long m_count = 0;
	//private static Object m_lock = 0;
	private Fragment m_frag;
	private String m_sql;
	private String m_sPipeName = null;
	private Fragment.ORDER_TYPE m_nOrderby;	

	public ImCursor(Context ctx, IfxConn conn,String sql, Fragment frag, Fragment.ORDER_TYPE bOrderby) {
		this.m_ifxConn = conn;
		this.m_ctx = ctx;
		this.m_sql = sql;
		this.m_frag = frag;
		m_nOrderby = bOrderby;
		ctx.addResource(this);
		m_ifxConn.addCursor(this);
		threadRead(ctx);
	}
	
	public String getTableName(){
		if (m_frag==null){
			return null;
		}else{
			return m_frag.getTableName();
		}
	}
	public String getFieldName(){
		if (m_frag==null){
			return null;
		}else{
			return m_frag.getFieldName();
		}
	}
	public Object getMinValue(){			//分片字段最小值(包含)
		if (m_frag==null){
			return null;
		}else{
			return m_frag.getMinValue();
		}
	}
	public Object getMaxValue(){			//分段字段最大值(不包含)
		if (m_frag==null){
			return null;
		}else{
			return m_frag.getMaxValue();
		}
	}
	
	protected long skipOver(long n) {
		long count = 0;// m_hiveBase.skipOver(n);

		if (count < n) {
			close();
		}

		return count;
	}

	public synchronized void close() {
		super.close();

		try {
			if (m_ctx != null) {
				m_ctx.removeResource(this);
				m_ctx = null;
			}
			
			if (m_read!=null){
				m_read.close();
				m_read = null;
			}
			if (m_sPipeName!=null){
				if (ImCommand.isLinuxOS()){
					ImCommand.killShell(m_ifxConn.getDbName(), m_sPipeName);
				}
				m_sPipeName = null;
			}
			if (m_process!=null){
				m_process.destroyForcibly();
				m_process = null;
			}
			
			m_bEnd = true;
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
	}

	public Sequence get(int n) {
		if(m_read==null) return null;
		Table table = m_read.getData(n);
		if (table == null) {
			System.out.println("cursor getX1 = " + m_count);
			close();
			return null;
		}
		//synchronized(m_lock)
		{
			m_count += table.length();
		}
		if (table.length() < n && n < ICursor.INITSIZE) {
			System.out.println("cursor getX2 = " + m_count);
			close();
		}

		return table;
	}

	protected void finalize() throws Throwable {
		close();
	}

	public boolean isEnd() {
		return m_bEnd;
	}

	public void threadRead(Context ctx) {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");// 设置日期格式
		String sDate = df.format(new Date());
		
		m_tblInfo = new ImTableInfo();
		m_tblInfo.initTableInfo(m_ifxConn.m_connect.conn, m_sql); // getMetadata
		m_sPipeName = m_tblInfo.m_tableName + "_" + sDate + "_" + m_nShellSerial++;
		String sPipeName="";
		boolean bLocal = false;
		// 1. 启动脚本程序
		if (!bLocal) {
			int nPdqNum = 0;
			/* split.sh 处理多管道数据输出脚本
			 * split.sh dbName pipeName sql pipeNum pdq
			 * echo "set pdqpriority ${pdq};insert into ext_${pipe_name} $sql " |dbaccess $dbname  >/dev/null 2>&1 
			*/
			String[] shellName = new String[] { "/informix/vsettan/split.sh", m_ifxConn.getDbName(), m_sPipeName, m_sql,
					String.valueOf(1), String.valueOf(nPdqNum) };
			m_process = ImCommand.exeShellCmd(shellName);
			String c = "";
			for (int i = 0; i < shellName.length; i++) {
				c += shellName[i] + " ";
			}
			//System.out.println("cmd1000 after=" + c);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Logger.error(e.getStackTrace());
			}
			sPipeName = "/tmp/pipe_" + m_sPipeName + "1.p";
			
			// m_oldThreadNum = Env.getCallxParallelNum();
			// Env.setCallxParallelNum(nThreadNum);
		} else { //文件
			if(m_nShellSerial==200){
				sPipeName = "d:/tmp/ifx/orders2.txt";
				sPipeName = "d:/tmp/ifx/mycol.txt";
			}else{
				sPipeName = "d:/tmp/ifx/lineitem600w.txt";
			}
			//System.out.println("sPipeName2000 = " + sPipeName );
			File file = new File(sPipeName);
	    	if (!file.exists()) {
	    		Logger.warn(sPipeName + " is not existed");
	    	}
		}
		//System.out.println("sPipeName5000 = " + sPipeName );
		m_read = new BPipeReader(m_tblInfo, sPipeName);
	}
}
