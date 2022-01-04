package com.esproc.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The CallableStatement list
 */
public class StatementList {
	/**
	 * The CallableStatement list
	 */
	private List<InternalCStatement> stats = new ArrayList<InternalCStatement>();

	/**
	 * Number of statements
	 * 
	 * @return
	 */
	public synchronized int count() {
		return stats.size();
	}

	/**
	 * 
	 * @param sid
	 * @return
	 */
	public synchronized InternalCStatement getByID(int sid) {
		for (int i = 0; i < stats.size(); i++) {
			InternalCStatement stat = (InternalCStatement) stats.get(i);
			if (stat.getID() == sid) {
				return stat;
			}
		}
		return null;
	}

	/**
	 * Get statement by serial number
	 * 
	 * @param index
	 * @return
	 */
	public synchronized InternalCStatement get(int index) {
		return stats.get(index);
	}

	/**
	 * Add a statement
	 * 
	 * @param ic
	 */
	public synchronized void add(InternalCStatement ic) {
		stats.add(ic);
	}

	/**
	 * Remove the statement of the specified serial number
	 * 
	 * @param index
	 */
	public synchronized void remove(int index) {
		stats.remove(index);
	}

	/**
	 * Remove statement according to statement ID
	 * 
	 * @param sid
	 */
	public synchronized void removeByID(int sid) {
		InternalCStatement ic = getByID(sid);
		stats.remove(ic);
	}

	/**
	 * Clean up statements
	 */
	public synchronized void clear() {
		while (stats.size() > 0) {
			try {
				stats.get(0).close();
				stats.remove(0);
			} catch (SQLException e) {
				JDBCUtil.log(e.getMessage());
			}
		}
	}
}
