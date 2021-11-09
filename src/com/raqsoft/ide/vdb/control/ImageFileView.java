package com.raqsoft.ide.vdb.control;

import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileView;

import com.raqsoft.ide.common.GC;
import com.raqsoft.ide.common.GM;

public class ImageFileView extends FileView {
	ImageIcon folderIcon = GM.getImageIcon(GC.IMAGES_PATH + "folder.png", false);
	ImageIcon fileIcon = GM.getImageIcon(GC.IMAGES_PATH + "file.png", false);

	public String getName(File f) {
		return null; // let the L&F FileView figure this out
	}

	public String getDescription(File f) {
		return null; // let the L&F FileView figure this out
	}

	public Boolean isTraversable(File f) {
		return null; // let the L&F FileView figure this out
	}

	public String getTypeDescription(File f) {
		return null;
	}

	public Icon getIcon(File f) {
		Icon icon = null;
		if (f.isDirectory()) {
			icon = folderIcon;
		} else {
			icon = fileIcon;
		}
		return icon;
	}
}
