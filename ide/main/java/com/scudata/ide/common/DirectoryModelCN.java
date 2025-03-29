package com.scudata.ide.common;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.plaf.basic.BasicDirectoryModel;

import com.scudata.dm.Env;

/**
 * Used to support file names sorted by language.
 *
 */
class DirectoryModelCN extends BasicDirectoryModel {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 * 
	 * @param filechooser
	 */
	public DirectoryModelCN(JFileChooser filechooser) {
		super(filechooser);
	}

	/**
	 * Sort file names
	 */
	protected void sort(Vector<? extends File> v) {
		int l = v.size();
		if (l < 2)
			return;
		Collections.sort(v, new Comparator<File>() {

			public int compare(File o1, File o2) {
				return Env.getCollator().compare(getName(o1), getName(o2));
			}

		});
	}

	/**
	 * Get file name/path
	 * 
	 * @param f
	 * @return
	 */
	private String getName(File f) {
		if (f.isDirectory()) {
			return f.getPath();
		} else {
			return f.getName();
		}
	}

}
