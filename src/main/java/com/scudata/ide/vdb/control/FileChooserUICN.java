package com.scudata.ide.vdb.control;

import javax.swing.JFileChooser;
import javax.swing.plaf.basic.BasicDirectoryModel;
import javax.swing.plaf.metal.MetalFileChooserUI;

public class FileChooserUICN extends MetalFileChooserUI {
	private BasicDirectoryModel model = null;

	public FileChooserUICN(JFileChooser filechooser) {
		super(filechooser);
	}

	protected void createModel() {
		model = new DirectoryModelCN(getFileChooser());
	}

	public BasicDirectoryModel getModel() {
		return model;
	}

}
