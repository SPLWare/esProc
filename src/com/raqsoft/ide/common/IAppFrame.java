package com.raqsoft.ide.common;

import javax.swing.JInternalFrame;

/**
 * IDE main frame interface
 *
 */
public interface IAppFrame {
	/**
	 * Exit IDE
	 * 
	 * @return
	 */
	public boolean exit();

	/**
	 * Open the file with the specified name
	 * 
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public JInternalFrame openSheetFile(String fileName) throws Exception;

	/**
	 * Close all sheets
	 * 
	 * @return
	 */
	public boolean closeAll();
}
