package com.raqsoft.ide.common;

import javax.swing.JFileChooser;
import javax.swing.plaf.basic.BasicDirectoryModel;
import javax.swing.plaf.metal.MetalFileChooserUI;

/**
 * FileChooser that supports Chinese sorting
 *
 */
class FileChooserUICN extends MetalFileChooserUI {
	private BasicDirectoryModel model = null;

	/**
	 * Constructor
	 * 
	 * @param filechooser
	 */
	public FileChooserUICN(JFileChooser filechooser) {
		super(filechooser);
	}

	/**
	 * Create model
	 */
	protected void createModel() {
		model = new DirectoryModelCN(getFileChooser());
	}

	/**
	 * Get BasicDirectoryModel
	 */
	public BasicDirectoryModel getModel() {
		return model;
	}

}
