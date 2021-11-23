package com.scudata.lib.hive.function;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;

public class HiveICursor extends ICursor {
	private HiveBase m_hiveBase;
	private Context m_ctx;
	private boolean bEnd = false;
	
	public HiveICursor(HiveBase hiveBase, Context ctx) {
		this.m_hiveBase = hiveBase;
		this.m_ctx = ctx;
		ctx.addResource(this);		
	}

	protected long skipOver(long n) {
		int count = m_hiveBase.skipOver(n);
		
		return count;
	}

	public synchronized void close() {
		super.close();
		
		try {
			if (m_ctx != null) {
				m_ctx.removeResource(this);			
				m_ctx = null;
			}
			bEnd = true;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	public Sequence get(int n) {
		Table table = m_hiveBase.getTable(n);
				
		return table;
	}

	protected void finalize() throws Throwable {
		close();
	}
	
	public boolean isEnd()
	{
		return bEnd;
	}


}
