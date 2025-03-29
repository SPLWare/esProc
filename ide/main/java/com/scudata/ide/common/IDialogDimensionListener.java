package com.scudata.ide.common;

import java.awt.Component;

/**
 * Interface for setting window position and size
 *
 */
public interface IDialogDimensionListener {
	/**
	 * Save window position and size
	 * 
	 * @param dlg
	 */
	public void saveWindowDimension(Component dlg);

	/**
	 * Load window position and size
	 * 
	 * @param dlg
	 * @return
	 */
	public boolean loadWindowDimension(Component dlg);
}
