package com.esproc.jdbc;

import java.util.Locale;

import com.scudata.common.MessageManager;

/**
 * Message Manager of the esProc JDBC
 *
 */
public class JDBCMessage {

	/**
	 * Private constructor
	 */
	private JDBCMessage() {
	}

	/**
	 * Get MessageManager object
	 * 
	 * @return MessageManager
	 */
	public static MessageManager get() {
		return MessageManager.getManager("com.esproc.jdbc.jdbcMessage");
	}

	/**
	 * Get MessageManager object according to Locale
	 * 
	 * @param locale
	 * @return MessageManager
	 */
	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.esproc.jdbc.jdbcMessage", locale);
	}
}
