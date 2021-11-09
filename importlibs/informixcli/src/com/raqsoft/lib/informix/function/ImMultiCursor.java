package com.raqsoft.lib.informix.function;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.raqsoft.common.Logger;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.lib.informix.helper.Fragment;
import com.raqsoft.lib.informix.helper.IfxConn;
import com.raqsoft.lib.informix.helper.ImCommand;
import com.raqsoft.lib.informix.helper.ImTableInfo;
import com.raqsoft.lib.informix.function.multiCursor.BPipeCursor;
import com.raqsoft.lib.informix.function.multiCursor.MultiCursors;

public class ImMultiCursor extends ICursor {
	private Context m_ctx;
	private IfxConn m_ifxConn = null;
	private Process m_process = null;
	private MultiCursors m_cursors = null;
	
	private static int m_nShellSerial = 1;
	private ImTableInfo m_tblInfo;
	private boolean m_bEnd = false;
	private static long m_count = 0;
	private Fragment m_frag;
	private String m_sql;
	private String m_sPipeName = null;
	private Fragment.ORDER_TYPE m_nOrderby;

	public ImMultiCursor(Context ctx, IfxConn conn,String sql,Fragment frag,Fragment.ORDER_TYPE bOrderby) {
		this.m_ifxConn = conn;
		this.m_ctx = ctx;
		m_sql = sql;
		m_frag = frag;
		m_nOrderby = bOrderby;
		ctx.addResource(this);
		m_ifxConn.addCursor(this);
		threadRead(ctx);
	}
		
	protected long skipOver(long n) {
		long count = 0;
		count = m_cursors.skip(n);
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
			if (m_cursors!=null){
				m_cursors.close();
			}
			if (m_sPipeName!=null){
				if (ImCommand.isLinuxOS()){
					ImCommand.killShell(m_ifxConn.getDbName(), m_sPipeName);
				}
				m_sPipeName = null;
			}
			if (m_process!=null){
				//System.out.println("split close ");
				m_process.destroyForcibly();
				m_process = null;
			}
			
			m_bEnd = true;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	public Sequence get(int n) {
		Sequence table = m_cursors.get(n);		
		if (table == null) {
			//System.out.println("cursor getX1 = " + m_count);
			close();
			return null;
		}
		m_count+=table.length();
		if (table.length() < n && n < ICursor.INITSIZE) {
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
		String fileName = "";
		m_sPipeName = m_tblInfo.m_tableName + "_" + sDate + "_" + m_nShellSerial++;
		int nCursorNum = 2;
		boolean bLocal = false;
		// 1. 启动脚本程序
		if (!bLocal) {
			//int nPdqNum = 0;			
			// System.out.println("cmd before ..... ");
			String[] shellName = new String[] {"/informix/vsettan/split.sh", m_ifxConn.getDbName(), m_sPipeName, m_sql,
			String.valueOf(nCursorNum), String.valueOf(0) };
			// shellName = "d:/tmp/test.bat";
			m_process = ImCommand.exeShellCmd(shellName);
			String c = "";
			for (int i = 0; i < shellName.length; i++) {
				c += shellName[i] + " ";
			}
			System.out.println("cmd after=" + c);
			try {
				Thread.sleep(1000 * 3);
			} catch (InterruptedException e) {
				Logger.error(e.getStackTrace());
			}
		}
		
		ICursor []cursors = new ICursor[nCursorNum];
		for (int i = 0; i < nCursorNum; i++) 
		{    			
			if (bLocal){
				fileName = "d:/tmp/ifx/lineitem600w.txt";
			}else{
				fileName =  "/tmp/pipe_"+ m_sPipeName + (i+1)+".p";
			}
	    			
			cursors[i] = new BPipeCursor(m_tblInfo, fileName, ctx);
			System.out.println("end fileName_"+i+"=" + fileName);
		}
		this.m_cursors = new MultiCursors(cursors, ctx);  
	}
}
