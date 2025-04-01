package com.scudata.parallel;

import java.io.File;
import java.io.Serializable;

import com.scudata.dm.Env;

/**
 * 用于存储相对于某个路径下的文件信息
 * @author Joancy
 *
 */
public class FileInfo implements Serializable,Comparable<Object> {
	private static final long serialVersionUID = 3777477339763658303L;

//	public Integer partition;//当分区为-1时，表示fileName相对于Env.getMainPath()
	public String fileName; // 相对路径名，路径间用/分隔
	private boolean isDir; // 是否目录
	
	private boolean isDirEmpty = false; // 如果是目录，当前的目录是否为空

	private long lastModified = -1;

	public FileInfo( String fileName, boolean isDir) {
//		this.partition = partition;
		this.fileName = fileName;
		this.isDir = isDir;
	}

//	public Integer getPartition() {
//		return partition;
//	}

	public String getFileName() {
		return fileName;
	}

	public boolean isDir() {
		return isDir;
	}
	
	public boolean isDirEmpty() {
		return isDirEmpty;
	}
	
	public void setDirEmpty(boolean isEmpty){
		isDirEmpty = isEmpty;
	}

	public boolean isAbsoluteFile(){
		File f = new File(fileName);
		return f.isAbsolute();
	}
/**
 * 返回文件复制到目标机器的路径
 * @param parent 文件在本机的父路径
 * @return 目标机器的路径，如果parent是绝对路径，则目标机器使用相同的绝对路径
 * 如果parent是相对路径，则目标机器路径亦为去掉了本机主路径的相对路径
 */
	public String getDestPath(String parent){
		File p = new File(parent);
		String header;
		if(p.isAbsolute()){
			header = parent;
			return new File(header, fileName).getAbsolutePath();
		}else{
			String mainP = Env.getMainPath();
			header = new File( mainP,parent).getAbsolutePath();
			String tmp = new File(header, fileName).getAbsolutePath();
			return tmp.substring(mainP.length()+1);
		}
	}
	
	public File getFile(String parent) {
		File p = new File(parent);
		String header;
		if(p.isAbsolute()){
			header = parent;
		}else{
			header = new File(Env.getMainPath(),parent).getAbsolutePath();
		}
		return new File(header, fileName);
	}

	public void setLastModified(long m) {
		this.lastModified = m;
	}

	public long lastModified() {
		return lastModified;
	}

	public String toString() {
		return fileName;
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof FileInfo) {
			FileInfo otherFile = (FileInfo) obj;
				if (fileName != null
						&& fileName.equals(otherFile.getFileName()))
					if (isDir == otherFile.isDir())
						return true;
		}
		return false;
	}

	public int compareTo(Object o) {
		FileInfo other = (FileInfo)o;
		return fileName.compareTo(other.getFileName());
	}
}
