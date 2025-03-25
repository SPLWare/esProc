package com.scudata.ide.common;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.ide.common.swing.JTableEx;

/**
 * Thread to append data to the table
 * 
 */
public class AppendDataThread extends Thread {
	/**
	 * Whether to stop appending data
	 */
	private boolean isStopped = false;
	/**
	 * Sequence for display
	 */
	private Sequence pmt;
	/**
	 * Table component
	 */
	private JTableEx table;

	/**
	 * Constructor
	 * 
	 * @param pmt
	 *            Sequence for display
	 * @param table
	 *            Table component
	 */
	public AppendDataThread(Sequence pmt, JTableEx table) {
		this.pmt = pmt;
		this.table = table;
	}

	/**
	 * Implementation
	 */
	public void run() {
		int size = pmt.length();
		table.data.setRowCount(0);
		boolean isPmt = pmt.isPmt();
		for (int i = 1; i <= size; i++) {
			if (isStopped) {
				break;
			}
			Object record = pmt.get(i);
			try {
				if (isPmt && record instanceof BaseRecord) {
					addRecordRow(table, (BaseRecord) record);
				} else {
					addObjectRow(record);
				}
			} catch (Exception e) {
			}
		}

	}

	/**
	 * Stop thread
	 */
	public void stopThread() {
		isStopped = true;
	}

	/**
	 * Append a record to the table component
	 * 
	 * @param table
	 * @param record
	 * @throws Exception
	 */
	public static void addRecordRow(JTableEx table, BaseRecord record)
			throws Exception {
		if (record == null) {
			return;
		}
		DataStruct ds = record.dataStruct();
		int r = table.addRow();
		String nNames[] = ds.getFieldNames();
		if (nNames != null) {
			Object val;
			for (int j = 0; j < nNames.length; j++) {
				val = null;
				try {
					val = record.getFieldValue(nNames[j]);
				} catch (Exception e) {
					// 取不到的显示空
					continue;
				}
				int col = table.getColumnIndex(nNames[j]);
				if (col > -1) {
					table.data.setValueAt(val, r, col);
				}
			}
		}
	}

	/**
	 * When the sequence member is not a record, add a row value to an object
	 * 
	 * @param object
	 * @throws Exception
	 */
	private void addObjectRow(Object object) throws Exception {
		table.addRow(new Object[] { object });
	}

}
