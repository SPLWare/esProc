package com.raqsoft.lib.informix.function.multiCursor;

import com.raqsoft.common.*;
import com.raqsoft.dm.*;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.lib.informix.helper.ImTableInfo;

public class BPipeCursor extends ICursor {
	private String m_fileName;
	private ImTableInfo m_tblInfo;
	
	private int fileBufSize = Env.FILE_BUFSIZE;
	private BPipeReader reader;

	public BPipeCursor(ImTableInfo tbl, String fullName, Context ctx) {
		m_tblInfo = tbl;
		this.m_fileName = fullName;		
		this.ctx = ctx;
		reader = new BPipeReader(tbl, m_fileName);
		ctx.addResource(this);
	}
	
	public void setFileBufferSize(int size) {
		this.fileBufSize = size;
	}

	protected Sequence get(int n) {
		if (n < 1 || reader == null) {
			Logger.warn("PipeCursor getData finished");
			return null;
		}
		
		try {
			Sequence seq = reader.getData(n);
			if (seq == null || seq.length() < n) {
				close();
			}
			return seq;
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}finally{
			close();
		}
		
		return null;
	}

	protected long skipOver(long n) {
		if (n < 1 || reader == null) {
			return 0;
		}
		
		try {
			long count = reader.skip(n);
			if (count < n) {
				close();
			}
			
			return count;
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}finally{
			close();
		}
		return 0;
	}

	public synchronized void close() {
		super.close();
		
		if (reader != null) {
			if (ctx != null) ctx.removeResource(this);
			try {
				reader.close();
			} catch (Exception e) {
			}
			
			reader = null;
		}
	}

	protected void finalize() throws Throwable {
		close();
	}
	
	public boolean reset() {
		close();
		
		reader = new BPipeReader(m_tblInfo, m_fileName);
		return true;
		
	}
}
