package com.scudata.dm;

import java.io.File;

/**
 * 非本地File类的子类，把远程文件映射为本地File类
 * @author LW
 *
 */
public class NonLocalFile extends File {
	private static final long serialVersionUID = 1L;

	private FileObject fo;
	
	public NonLocalFile(String pathname, FileObject fo) {
		super(pathname);
		this.fo = fo;
	}

	public String getAbsolutePath() {
        return fo.getFileName();
    }
	
	public String getName() {
		return fo.getFileName();
	}
	
	public boolean exists() {
		return fo.isExists();
	}
	
	public boolean delete() {
		return fo.delete();
	}
}
