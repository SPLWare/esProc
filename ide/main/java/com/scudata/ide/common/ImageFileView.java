package com.scudata.ide.common;

import java.io.File;
import java.io.InputStream;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileView;

/**
 * Used to display files with icons
 *
 */
public class ImageFileView extends FileView {
	/**
	 * Folder icon
	 */
	ImageIcon folderIcon = createImageIcon("folder.png");
	/**
	 * File icon
	 */
	ImageIcon fileIcon = createImageIcon("file.png");

	/**
	 * Get the icon from the file
	 */
	public Icon getIcon(File f) {
		Icon icon = null;
		if (f.isDirectory()) {
			icon = folderIcon;
		} else {
			icon = fileIcon;
		}
		return icon;
	}

	/**
	 * Get the icon based on the icon file name
	 * 
	 * @param fileName
	 * @return
	 */
	private ImageIcon createImageIcon(String fileName) {
		InputStream is = ImageFileView.class.getResourceAsStream(fileName
				.toLowerCase());
		try {
			byte[] bt = GM.inputStream2Bytes(is);
			is.close();
			ImageIcon ii = new ImageIcon(bt);
			return ii;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
