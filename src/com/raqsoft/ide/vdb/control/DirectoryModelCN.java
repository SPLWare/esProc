package com.raqsoft.ide.vdb.control;

import java.io.File;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.plaf.basic.BasicDirectoryModel;

import com.raqsoft.dm.Env;

class DirectoryModelCN extends BasicDirectoryModel {
	private static final long serialVersionUID = 1L;

	public DirectoryModelCN(JFileChooser filechooser) {
		super(filechooser);
	}

	protected void sort(Vector v) {
		int l = v.size();
		if (l < 2)
			return;

		String names[] = new String[l];
		for (int i = 0; i < names.length; i++) {
			names[i] = getName((File) v.get(i));
		}
		Arrays.sort(names, Env.getCollator());
		File[] sortedFiles = new File[names.length];
		for (int i = 0; i < names.length; i++) {
			File f = searchFile(names[i], v);
			sortedFiles[i] = f;
		}
		for (int i = 0; i < sortedFiles.length; i++) {
			v.setElementAt(sortedFiles[i], i);
		}
	}

	private File searchFile(String name, Vector files) {
		for (int i = 0; i < files.size(); i++) {
			File f = (File) files.get(i);
			if (getName(f).equals(name)) {
				return f;
			}
		}
		return null;
	}

	private String getName(File f) {
		if (f.isDirectory()) {
			return f.getPath();
		} else {
			return f.getName();
		}
	}

}
