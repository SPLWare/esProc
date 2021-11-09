package com.raqsoft.ide.common;

/**
 * Custom menu handler
 *
 */
public interface IConfigMenuHandler {
	/**
	 * Send message
	 * 
	 * @param desc
	 *            Description
	 * @param argument
	 *            Argument
	 * @return
	 */
	public Object processMessage(String desc, Object argument);
}
