package com.raqsoft.util;

import java.io.File;
import java.util.HashMap;

import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.LocalFile;


/**
 * 用于取文件的同步对象
 * 多线程修改文件时，如果想做同步，则用文件取出同步对象来做同步
 * @author RunQian
 *
 */
public final class FileSyncManager {
	// 文件和同步对象映射
	private static HashMap<File, File> fileMap = new HashMap<File, File>();
	
	/**
	 * 取得文件的同步对象，访问文件时在此对象上加锁
	 * @param fo 文件对象
	 * @return Object 用于同步对象
	 */
	public static Object getSyncObject(FileObject fo) {
		LocalFile lf = fo.getLocalFile();
		File file = lf.file();
		return getSyncObject(file);
	}
	
	/**
	 * 取得文件的同步对象，访问文件时在此对象上加锁
	 * @param file 文件
	 * @return Object 用于同步对象
	 */
	public static Object getSyncObject(File file) {
		synchronized(fileMap) {
			File f = fileMap.get(file);
			if (f == null) {
				fileMap.put(file, file);
				return file;
			} else {
				return f;
			}
		}
	}
}