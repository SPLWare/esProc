package com.scudata.lib.hbase.function;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;

public class HbaseCursor extends ICursor {

	private HbaseDriverCli client;
	private TableInfo m_tbInfo;
	private boolean bEnd = false;
	
	public HbaseCursor(HbaseDriverCli client, Context ctx, TableInfo tbInfo) {
		this.client = client;
		this.ctx = ctx;
		m_tbInfo = tbInfo;
		ctx.addResource(this);		
	}

	protected long skipOver(long  n) {
		return client.skipOver(n);
	}

	public synchronized void close() {
		super.close();
	}

	public Sequence get(int n) {
		Table table = client.getTable(n, m_tbInfo);
		
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
