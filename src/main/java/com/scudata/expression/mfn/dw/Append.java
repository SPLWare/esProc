package com.scudata.expression.mfn.dw;

import java.io.File;
import java.io.IOException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dw.ColPhyTable;
import com.scudata.dw.ComTable;
import com.scudata.dw.IPhyTable;
import com.scudata.dw.PhyTableGroup;
import com.scudata.expression.PhyTableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 追加游标数据到组表中
 * T.append(cs)
 * @author RunQian
 *
 */
public class Append extends PhyTableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("append" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("append" + mm.getMessage("function.invalidParam"));
		}
		
		if (table instanceof ColPhyTable) {
			ColPhyTable colTable = (ColPhyTable) table;
			if (!colTable.getGroupTable().isPureFormat()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.oldVersion2"));
			}
		}
		
		Object obj = param.getLeafExpression().calculate(ctx);
		ICursor cursor;
		if (obj instanceof ICursor) {
			cursor = (ICursor)obj;
		} else if (obj instanceof Sequence) {
			cursor = ((Sequence)obj).cursor();
		} else if (obj == null) {
			return table;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("append" + mm.getMessage("function.paramTypeError"));
		}
		
		try {
			synchronized(table) {
				if (option != null && option.indexOf('y') != -1) {
					PhyTableGroup tg = new PhyTableGroup(null, new IPhyTable[] {table}, null, null, ctx);
					Sequence seq = cursor.fetch();
					tg.setMemoryTable(seq);
					return tg;
				}
				
				if (cursor instanceof MultipathCursors) {
					parallelAppend((MultipathCursors) cursor, ctx);
				} else {
					table.append(cursor, option);
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		return table;
	}
	
	private void parallelAppend(MultipathCursors mcs, Context ctx) throws IOException {
		int num = mcs.getPathCount();
		ColPhyTable[] tables = new ColPhyTable[num];
		File[] files = new File[num];
		Thread[] threads = new Thread[num];
		
		tables[0] = (ColPhyTable) table;
		for (int i = 1; i < num; i++) {
			FileObject tmp = FileObject.createTempFileObject();
			files[i] = tmp.getLocalFile().file();
			((ColPhyTable)table).getGroupTable().reset(files[i], "S", null, null);
			tables[i] = (ColPhyTable) ComTable.openBaseTable(files[i], ctx);
		}
		
		for (int i = 0; i < num; i++) {
			threads[i] = newAppendThread(tables[i], mcs.getPathCursor(i), option);
			threads[i].run();
		}
		for (int i = 0; i < num; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
		
		for (int i = 1; i < num; i++) {
			table.append(tables[i]);
			tables[i].getGroupTable().delete();
		}
	}
	
	private static Thread newAppendThread(final ColPhyTable table, ICursor cs, String option) {
		return new Thread() {
			public void run() {
				try {
					table.append(cs, option);
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
			}
		};
	}
}
