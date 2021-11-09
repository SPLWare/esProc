package com.raqsoft.lib.spark.function;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;

public class ImCursor extends ICursor {

	private SparkCli client;
	private Context ctx;
	private boolean bEnd = false;
	
	public ImCursor(SparkCli client, Context ctx) {
		this.client = client;
		this.ctx = ctx;
		ctx.addResource(this);		
	}

	protected long skipOver(long n) {
		long count = client.skipOver(n);
				
		return count;
	}

	public synchronized void close() {
		super.close();
		
		try {
			if (client != null) {
				ctx.removeResource(this);
				client.close();
				client = null;
				ctx = null;
			}
			bEnd = true;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	public Sequence get(int n) {
		Table table = client.getTable(n);
		
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
