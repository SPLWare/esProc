package com.scudata.ide.common;

import java.awt.event.ActionListener;

/**
 * Customize menu commands
 *
 */
public abstract class ConfigMenuAction implements ActionListener {
	/**
	 * Arguments
	 */
	protected String argument;
	/**
	 * Handler
	 */
	protected IConfigMenuHandler handler;

	/**
	 * Set argument
	 * 
	 * @param arg
	 */
	public void setConfigArgument(String arg) {
		this.argument = arg;
	}

	/**
	 * Get argument
	 * 
	 * @return
	 */
	public String getConfigArgument() {
		return this.argument;
	}

	/**
	 * Set handler
	 * 
	 * @param handler
	 */
	public void setConfigMenuHandler(IConfigMenuHandler handler) {
		this.handler = handler;
	}

	/**
	 * Get handler
	 * 
	 * @return
	 */
	public IConfigMenuHandler getConfigMenuHandler() {
		return this.handler;
	}
}
