package com.raqsoft.lib.olap4j.function;

import java.util.List;

import org.olap4j.Cell;
import org.olap4j.CellSet;
import org.olap4j.Position;
import org.olap4j.metadata.Member;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;

public class ImCursor extends ICursor {
	private Context m_ctx;
	private CellSet m_cellSet;
	private String m_colNames[];
	private int m_nIndex = 0;

	public ImCursor(CellSet cellset, Context ctx) {
		this.m_cellSet = cellset;
		this.m_ctx = ctx;
		ctx.addResource(this);
		m_colNames = ImUtils.getColumnNames(cellset);
	}

	protected long skipOver(long n) {
		long count = 0;

		if (m_cellSet==null) {
			return 0;
		}
	
		if (m_cellSet.getAxes().size() == 2) {
			 m_nIndex += n;		 
			 count = n;
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
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	public Sequence get(int n) {
		Table table = getTable(n);
		if (table == null) {
			return null;
		}

		return table;
	}

	protected void finalize() throws Throwable {
		close();
	}

	private Table getTable(int n) {
		if (n < 0)
			return null;
		if (m_cellSet == null)
			return null;

		int nCount = 0, nCur = 0, k = 0;
		Table table = new Table(m_colNames);
		Object[] objs = new Object[m_colNames.length];

		if (m_cellSet.getAxes().size() == 2) {
			for (Position row : m_cellSet.getAxes().get(1)) {
				nCur++;
				if (m_nIndex > nCur){
					continue;
				}
				
				k = 0;
				for (Member member : row.getMembers()) {
					objs[k++] = member.getName();
				}

				for (Position column : m_cellSet.getAxes().get(0)) {
					final Cell cell = m_cellSet.getCell(column, row);
					objs[k++] = cell.getValue();
				}
				table.newLast(objs);
				if (nCount++ >= n - 1) {
					break;
				}
			}
			m_nIndex += nCount;
		} else if (m_cellSet.getAxes().size() == 1) {
			String line = null;
			for (Position row : m_cellSet.getAxes().get(0)) {
				nCur++;
				if (m_nIndex > nCur){
					continue;
				}
				
				k = 0;
				line = "";
				List<Member> members = row.getMembers();
	    		for (Member m : members) {
	    			if (line.isEmpty()) {
	    				line = m.getCaption();
	    			} else {
	    				line += "_" + m.getCaption();
	    			}
	    		}
	    		objs[k++] = line;
				final Cell cell = m_cellSet.getCell(row);
				objs[k++] = cell.getValue();
				table.newLast(objs);
				if (nCount++ >= n - 1) {
					break;
				}
			}
			m_nIndex += nCount;
		}

		return table;
	}
}
