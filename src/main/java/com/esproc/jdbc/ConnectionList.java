package com.esproc.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * List of all connections. Used to manage connections.
 *
 */
public class ConnectionList {
	/**
	 * List of all connections
	 */
	private List<InternalConnection> cons = new ArrayList<InternalConnection>();

	/**
	 * Number of connections
	 * 
	 * @return int
	 */
	public synchronized int count() {
		return cons.size();
	}

	/**
	 * Get connection by ID
	 * 
	 * @param The
	 *            ID of the connection
	 * @return Return the connection object. Return null when not found.
	 */
	public synchronized InternalConnection getByID(int cid) {
		for (int i = 0; i < cons.size(); i++) {
			InternalConnection con = cons.get(i);
			if (con.getID() == cid) {
				return con;
			}
		}
		return null;
	}

	/**
	 * Return connection by sequence number. Start from 0.
	 * 
	 * @param index
	 * @return InternalConnection
	 */
	public synchronized InternalConnection get(int index) {
		return cons.get(index);
	}

	/**
	 * Add a connection.
	 * 
	 * @param ic
	 */
	public synchronized void add(InternalConnection ic) {
		cons.add(ic);
	}

	/**
	 * Add a connection to the specified sequence number.
	 * 
	 * @param index
	 * @param ic
	 */
	public synchronized void add(int index, InternalConnection ic) {
		cons.add(index, ic);
	}

	/**
	 * Remove a connection by serial number
	 * 
	 * @param index
	 */
	public synchronized void remove(int index) {
		cons.remove(index);
	}

	/**
	 * Remove a connection by ID
	 * 
	 * @param cid
	 */
	public synchronized void removeByID(int cid) {
		InternalConnection ic = getByID(cid);
		cons.remove(ic);
	}

	/**
	 * Clean up all connections
	 */
	public synchronized void clear() {
		while (cons.size() > 0) {
			try {
				((InternalConnection) cons.get(0)).close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

}
