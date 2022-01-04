package com.scudata.ide.common;

/**
 * IDE atomic command interface
 *
 */
public interface IAtomicCmd {
	/**
	 * Execute command
	 * 
	 * @return
	 */
	public IAtomicCmd execute();

	/**
	 * Convert command to string
	 * 
	 * @return
	 */
	public String toString();
}
