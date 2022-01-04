package com.scudata.dm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import com.scudata.common.IOUtils;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.resources.EngineMessage;

/**
 * 本地文件
 * @author WangXiaoJun
 *
 */
public class LocalFile implements IFile {
	private String fileName;
	private String opt;
	private String parent; // 父路径
	private Context ctx;
	private Integer partition;
	
	public LocalFile(String fileName, String opt) {
		this.fileName = fileName;
		this.opt = opt;
	}

	/**
	 * 创建本地文件
	 * @param fileName 相对路径或绝对路径
	 * @param opt 
	 * @param ctx 不需要时传null
	 */
	public LocalFile(String fileName, String opt, Context ctx) {
		this.fileName = fileName;
		this.opt = opt;
		this.ctx = ctx;
	}

	/**
	 * 集群函数使用
	 * @param fileName
	 * @param opt
	 * @param partition
	 */
	public LocalFile(String fileName, String opt, Integer partition) {
		this.fileName = fileName;
		this.opt = opt;
		
		if (partition != null && partition.intValue() > 0) {
			this.partition = partition;
			//parent = Env.getMappingPath(partition);
			
			// 找出文件名的起始位置
			int index = fileName.lastIndexOf('\\');
			if (index == -1) {
				index = fileName.lastIndexOf('/');
			}
			
			if (index == -1) {
				this.fileName = partition.toString() + "." + fileName;
			} else {
				this.fileName = fileName.substring(0, index + 1) + 
						partition.toString() + "." + fileName.substring(index + 1);
			}
		}
	}

	/**
	 * 设置父文件夹
	 * @param parent
	 */
	public void setParent(String parent) {
		this.parent = parent;
	}
		
	/**
	 * 设置文件名
	 * @param fileName
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	private boolean isSearchPath() {
		return opt != null && opt.indexOf('s') != -1;
	}

	private File getAppHome() {
		if (ctx != null) {
			JobSpace js = ctx.getJobSpace();
			if (js != null) return js.getAppHome();
		}
		
		return null;
	}
	
	/**
	 * 生成文件，不找搜索路径
	 * @return File
	 */
	public File file() {
		if (parent != null) {
			return new File(parent, fileName);
		}
		
		// 如果设置了appHome，则fileName只能是相对路径
		File appHome = getAppHome();
		if (appHome != null) {
			String mainPath = Env.getMainPath();
			if (mainPath != null && mainPath.length() > 0) {
				File tmpFile = new File(appHome, mainPath);
				return new File(tmpFile, fileName);
			} else {
				return new File(appHome, fileName);
			}
		}
		
		if (IOUtils.isAbsolutePath(fileName)) {
			return new File(fileName);
		}

		String mainPath = Env.getMainPath();
		if (mainPath != null && mainPath.length() > 0) {
			return new File(mainPath, fileName);
		} else {
			return new File(fileName);
		}
	}
	
	/**
	 * 查找文件，如果文件不存在返回null
	 * @return
	 */
	public File getFile() {
		if (parent != null) {
			File file = new File(parent, fileName);
			if (file.exists()) {
				return file;
			}
		}
		
		// 如果设置了appHome，则fileName只能是相对路径
		File appHome = getAppHome();
		if (appHome != null) {
			// 带选项s时先找类路径，再找路径列表，最后找主目录
			if (isSearchPath() && Env.getPaths() != null) {
				for (String path : Env.getPaths()) {
					File tmpFile = new File(appHome, path);
					tmpFile = new File(tmpFile, fileName);
					if (tmpFile.exists()) return tmpFile;
				}
			}

			String mainPath = Env.getMainPath();
			if (mainPath != null && mainPath.length() > 0) {
				File tmpFile = new File(appHome, mainPath);
				tmpFile = new File(tmpFile, fileName);
				if (tmpFile.exists()) return tmpFile;
			} else {
				File tmpFile = new File(appHome, fileName);
				if (tmpFile.exists()) return tmpFile;
			}

			return null;
		}
		
		if (IOUtils.isAbsolutePath(fileName)) {
			File file = new File(fileName);
			if (file.exists()) {
				return file;
			} else {
				return null;
			}
		}

		// 带选项s时先找类路径，再找路径列表，最后找主目录
		if (isSearchPath()) {
			String []paths = Env.getPaths();
			if (paths != null) {
				for (int i = 0, count = paths.length; i < count; ++i) {
					File tmpFile = new File(paths[i], fileName);
					if (tmpFile.exists()) return tmpFile;
				}
			}
		}

		String mainPath = Env.getMainPath();
		if (mainPath != null && mainPath.length() > 0) {
			File tmpFile = new File(mainPath, fileName);
			if (tmpFile.exists()) return tmpFile;
		}

		File file = new File(fileName);
		if (file.exists()) {
			return file;
		} else {
			return null;
		}
	}

	/**
	 * 取输入流
	 * @throws IOException
	 * @return InputStream
	 */
	public InputStream getInputStream() {
		try {
			if (parent != null) {
				File file = new File(parent, fileName);
				if (file.exists()) return new FileInputStream(file);
			}

			// 如果设置了appHome，则fileName只能是相对路径
			File appHome = getAppHome();
			if (appHome != null) {
				// 带选项s时先找类路径，再找路径列表，最后找主目录
				if (isSearchPath()) {
					InputStream in = IOUtils.findResource(fileName);
					if (in != null) return in;
					
					in = Env.getStreamFromApp(fileName);
					if (in != null) return in;

					String []paths = Env.getPaths();
					if (paths != null) {
						for (String path : paths) {
							File tmpFile = new File(appHome, path);
							tmpFile = new File(tmpFile, fileName);
							if (tmpFile.exists()) return new FileInputStream(tmpFile);
						}
					}
				}

				String mainPath = Env.getMainPath();
				if (mainPath != null && mainPath.length() > 0) {
					File tmpFile = new File(appHome, mainPath);
					tmpFile = new File(tmpFile, fileName);
					if (tmpFile.exists()) return new FileInputStream(tmpFile);
				} else {
					File tmpFile = new File(appHome, fileName);
					if (tmpFile.exists()) return new FileInputStream(tmpFile);
				}

				throw new FileNotFoundException(fileName);
			}

			if (IOUtils.isAbsolutePath(fileName)) {
				return new FileInputStream(fileName);
			}

			// 带选项s时先找类路径，再找路径列表，最后找主目录
			if (isSearchPath()) {
				InputStream in = IOUtils.findResource(fileName);
				if (in != null) return in;

				in = Env.getStreamFromApp(fileName);
				if (in != null) return in;
				
				String []paths = Env.getPaths();
				if (paths != null) {
					for (int i = 0, count = paths.length; i < count; ++i) {
						File tmpFile = new File(paths[i], fileName);
						if (tmpFile.exists()) return new FileInputStream(tmpFile);
					}
				}
			}

			String mainPath = Env.getMainPath();
			if (mainPath != null && mainPath.length() > 0) {
				File tmpFile = new File(mainPath, fileName);
				if (tmpFile.exists()) return new FileInputStream(tmpFile);
			}

			return new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 取输出流，文件不存在则创建
	 * @param isAppend boolean 是否追加
	 * @throws FileNotFoundException
	 * @return OutputStream
	 */
	public OutputStream getOutputStream(boolean isAppend) {
		try {
			File file = getFileForWrite();
			file.getParentFile().mkdirs();
			return new FileOutputStream(file, isAppend);
		} catch (FileNotFoundException e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 取能够随机写的输出流，文件不存在则创建
	 * @param isAppend boolean 是否追加
	 * @return RandomOutputStream
	 */
	public RandomOutputStream getRandomOutputStream(boolean isAppend) {
		try {
			File file = getFileForWrite();
			file.getParentFile().mkdirs();
			
			RandomAccessFile randomFile = new RandomAccessFile(file, "rw");
			if (!isAppend) {
				randomFile.setLength(0);
			} else {
				randomFile.seek(randomFile.length());
			}
		
			return new FileRandomOutputStream(randomFile);
		} catch (IOException e) {
			throw new RQException(e);
		}
		//return new FileRandomOutputStream(getFileOutputStream(isAppend));
	}

	private File getFileForWrite() {
		if (parent != null) {
			return new File(parent, fileName);
		}

		// 如果设置了appHome，则fileName只能是相对路径
		File appHome = getAppHome();
		if (appHome != null) {
			String mainPath = Env.getMainPath();
			if (mainPath != null && mainPath.length() > 0) {
				File tmpFile = new File(appHome, mainPath);
				return new File(tmpFile, fileName);
			} else {
				return new File(appHome, fileName);
			}
		}

		if (IOUtils.isAbsolutePath(fileName)) {
			return new File(fileName);
		}

		String mainPath = Env.getMainPath();
		if (mainPath != null && mainPath.length() > 0) {
			return new File(mainPath, fileName);
		} else {
			return new File(fileName);
		}
	}
	
	/**
	 * 返回文件是否存在
	 * @return boolean
	 */
	public boolean exists() {
		return getFile() != null;
	}

	/**
	 * 返回文件大小
	 * @return long
	 */
	public long size() {
		File file = getFile();
		if (file != null) {
			return file.length();
		} else {
			return 0;
		}
	}

	/**
	 * 返回最近修改时间
	 * @return long
	 */
	public long lastModified() {
		File file = getFile();
		if (file != null) {
			return file.lastModified();
		} else {
			return 0;
		}
	}

	/**
	 * 删除文件，返回是否成功
	 * @return boolean
	 */
	public boolean delete() {
		File file = getFile();
		if (file != null) {
			return file.delete();
		} else {
			return false;
		}
	}

	/**
	 * 删除路径及其下面的子文件和子路径
	 * @return
	 */
	public boolean deleteDir() {
		File file = getFile();
		if (file == null) {
			return false;
		}
		
		return deleteDir(file);
	}
	
	private static boolean deleteDir(File file) {
		File []subs = file.listFiles();
		if (subs != null) {
			for (File sub : subs) {
				deleteDir(sub);
			}
		}
		
		return file.delete();
	}
	
	/**
	 * 移动文件到path，path只有文件名则改名
	 * @param dest String 目标文件名或文件路径名
	 * @param opt String y：目标文件已存在时强行复制缺省将失败，c：复制，
	 * 					 p：目标文件是相对目录是相对于主目录，默认是相对于源文件的父目录
	 * @return boolean true：成功，false：失败
	 */
	public boolean move(String dest, String opt) {
		File file = getFile();
		if (file == null || !file.exists()) return false;

		boolean isCover = false, isCopy = false, isMain = false;
		if (opt != null) {
			if (opt.indexOf('y') != -1) isCover = true;
			if (opt.indexOf('c') != -1) isCopy = true;
			if (opt.indexOf('p') != -1) isMain = true;
		}

		File destFile = new File(dest);
		if (!destFile.isAbsolute()) {
			if (isMain) {
				File appHome = getAppHome();
				String mainPath = Env.getMainPath();
				if (appHome != null) {
					if (mainPath != null && mainPath.length() > 0) {
						destFile = new File(appHome, mainPath);
						destFile = new File(destFile, dest);
					} else {
						destFile = new File(appHome, dest);
					}
				} else if (mainPath != null && mainPath.length() > 0) {
					destFile = new File(mainPath, dest);
				}
			} else {
				destFile = new File(file.getParentFile(), dest);
			}
		} else if (getAppHome() != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("file.fileNotExist", dest));
		}

		// 如果不带文件名，自动用源文件名
		if (destFile.isDirectory() && !file.isDirectory()) {
			destFile = new File(destFile, file.getName());
		}
		
		if (!isCover && destFile.exists()) {
			return false;
		}

		File parent = destFile.getParentFile();
		if (parent != null) parent.mkdirs();

		if (isCopy) {
			if (file.isDirectory()) {
				return copyDirectory(file, destFile);
			} else {
				return copyFile(file, destFile);
			}
		} else {
			destFile.delete();
			return file.renameTo(destFile);
		}
	}

	/**
	 * 创建临时文件
	 * @param prefix String
	 * @return String 返回绝对路径文件名
	 */
	public String createTempFile(String prefix) {
		try {
			File file = getFile();
			if (file == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("file.fileNotExist", fileName));
			}
			
			if (file.isDirectory()) {
				File tmpFile = File.createTempFile(prefix, "", file);
				return tmpFile.getAbsolutePath();
			} else {
				String suffix = "";
				String name = file.getName();
				int index = name.lastIndexOf('.');
				if (index != -1) suffix = name.substring(index);

				file = file.getParentFile();
				File tmpFile = File.createTempFile(prefix, suffix, file);
				return tmpFile.getAbsolutePath();
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	/**
	 * 把给定的路径名删除前面的主目录，返回相对于主目录的路径
	 * @param pathName 文件路径名
	 * @param ctx 计算上下文
	 * @return 截取后的相对路径名
	 */
	public static String removeMainPath(String pathName, Context ctx) {
		File home = null;
		JobSpace js = ctx.getJobSpace();
		if (js != null) {
			home = js.getAppHome();
		}
		
		
		String main = Env.getMainPath();
		if (main != null && main.length() > 0) {
			if (home == null) {
				home = new File(main);
			} else {
				home = new File(home, main);
			}
		} else if (home == null) {
			return pathName;
		}
		
		String strHome = home.getAbsolutePath();
		int len = strHome.length();
		if (pathName.length() > len && pathName.substring(0, len).equalsIgnoreCase(strHome)) {
			// 去掉前面的斜杠或反斜杠
			char c = pathName.charAt(len);
			if (c == '\\' || c == '/') {
				len++;
				if (pathName.length() > len) {
					c = pathName.charAt(len);
					if (c == '\\' || c == '/') {
						len++;
					}
				}
			}
			
			return pathName.substring(len);
		} else {
			return pathName;
		}
	}
	
	/**
	 * 复制文件到指定文件夹下
	 * @param s 源文件
	 * @param t 目标文件夹
	 * @return
	 */
	public static boolean copyDirectory(File s, File t) {
		File destDir = new File(t, s.getName());
		destDir.mkdirs();
		
		boolean result = true;
		File[] files = s.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					if (!copyFile(file, new File(destDir, file.getName()))) {
						result = false;
					}
				} else if (file.isDirectory()) {
					if (!copyDirectory(file, destDir)) {
						result = false;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * 复制文件的内容到指定文件
	 * @param s 源文件
	 * @param t 目标文件
	 * @return true：成功
	 */
	public static boolean copyFile(File s, File t) {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		
		try {
			fis = new FileInputStream(s);
			fos = new FileOutputStream(t);
			FileChannel in = fis.getChannel();
			FileChannel out = fos.getChannel();
			in.transferTo(0, in.size(), out); // 连接两个通道，并且从in通道读取，然后写入out通道
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			IOException ie = null;
			try {
				fis.close();
			} catch (IOException e) {
				ie = e;
			}
			try {
				fos.close();
			} catch (IOException e) {
				ie = e;
			}
			
			if (ie != null) {
				throw new RQException(ie);
			}
		}
		
		return true;
	}
	
	/**
	 * 设置文件大小
	 * @param size 大小
	 */
	public void setFileSize(long size) {
		File file = getFile();
		if (file != null) {
			try {
				RandomAccessFile rf = new RandomAccessFile(file, "rw");
				rf.setLength(size);
				rf.close();
			} catch (IOException e) {
				throw new RQException(e);
			}
		}
	}
	
	/**
	 * 取分区
	 * @return Integer 如果没有设置分区则返回空
	 */
	public Integer getPartition() {
		return partition;
	}
}
