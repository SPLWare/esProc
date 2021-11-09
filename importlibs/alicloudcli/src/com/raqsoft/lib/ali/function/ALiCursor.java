package com.raqsoft.lib.ali.function;

import java.util.ArrayList;
import java.util.Iterator;

import com.alicloud.openservices.tablestore.model.Row;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;

public class ALiCursor extends ICursor {
	private Iterator<Row> iterator;
	private DataStruct ds;
	
	private ALiClient client;
	
	public ALiCursor(ALiClient client, Iterator<Row> iterator, String []colNames, String opt, Context ctx) {
		this.iterator = iterator;
		
		if (colNames != null) {
			ds = new DataStruct(colNames);
		}
		
		if (opt != null && opt.indexOf('x') != -1) {
			this.client = client;
			this.ctx = ctx;
			ctx.addResource(this);
		}
	}

	protected long skipOver(long n) {
		Iterator<Row> iterator = this.iterator;
		if (iterator == null || n < 1) return 0;

		long count = 0;
		while (count < n && iterator.hasNext()) {
			count++;
			iterator.next();
		}
		
		if (count < n) {
			close();
		}
		
		return count;
	}

	public synchronized void close() {
		super.close();
		
		try {
			if (client != null) {
				ctx.removeResource(this);
				client.close();
				client = null;
				//ctx = null;
			}
			
			iterator = null;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	protected Sequence get(int n) {
		Iterator<Row> iterator = this.iterator;
		if (iterator == null || n < 1) return null;

		ArrayList<Row> list;
		if (n > INITSIZE) {
			list = new ArrayList<Row>(INITSIZE);
		} else {
			list = new ArrayList<Row>(n);
		}
		
		for (int i = 0; i < n && iterator.hasNext(); ++i) {
			list.add(iterator.next());
		}
		
		int size = list.size();
		if (size == 0) {
			close();
			return null;
		}
		
		Row []rows = new Row[size];
		list.toArray(rows);
		
		Table table = ALiClient.toTable(rows, ds);
		
		if (size < n) {
			close();
		}

		if (ds == null) {
			ds = table.dataStruct();
		}
		
		return table;
	}

	protected void finalize() throws Throwable {
		close();
	}
}
