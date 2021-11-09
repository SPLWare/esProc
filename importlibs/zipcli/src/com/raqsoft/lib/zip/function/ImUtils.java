package com.raqsoft.lib.zip.function;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raqsoft.common.Logger;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.Sequence;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

public class ImUtils {
	/**
	 * 是否有特定字符*或?
	 * @param src
	 *            String
	 */
	private static String m_os = System.getProperty("os.name");  

	public static boolean isSpecialCharacters(String src) {
		boolean bRet = false;
		if (src.indexOf("*") != -1 || src.indexOf("?") != -1) {
			bRet = true;
		}
		return bRet;
	}

	/**
	 * 删除某个文件夹下的所有文件夹和文件
	 * 
	 * @param delpath
	 *            String
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @return boolean
	 */
	public static boolean deleteFile(String delpath) throws Exception {
		try {
			File file = new File(delpath);
			// 当且仅当此抽象路径名表示的文件存在且 是一个目录时，返回 true
			if (!file.isDirectory()) {
				file.delete();
			} else if (file.isDirectory()) {
				String[] filelist = file.list();
				for (int i = 0; i < filelist.length; i++) {
					File delfile = new File(delpath + "\\" + filelist[i]);
					if (!delfile.isDirectory()) {
						delfile.delete();
					} else if (delfile.isDirectory()) {
						deleteFile(delpath + "\\" + filelist[i]);
					}
				}
				file.delete();
			}
		} catch (FileNotFoundException e) {
			Logger.error("deletefile() Exception:" + e.getMessage());
		}
		
		return true;
	}

	/**
	 * 在s所在的文件夹下查找
	 * 
	 * @param s
	 *            String 文件名
	 * @return File[] 找到的文件
	 */
	public static List<File> getFiles(String s, boolean bRecursive) {
		File f = new File(s);
		String name = f.getName();
		String path = f.getParentFile().getAbsolutePath();
		return getFiles(path, name, bRecursive);
	}

	/**
	 * 获取文件 可以根据正则表达式查找
	 * 
	 * @param dir
	 *            String 文件夹名称
	 * @param s
	 *            String 查找文件名，可带*. 进行模糊查询
	 * @return File[] 找到的文件
	 */
	public static List<File> getFiles(String dir, String pattern, boolean bRecursive) {
		// 开始的文件夹
		File file = new File(dir);
		//支持汉字，空格，英文字母，如：teamview - 副本.java
		String s = replaceSpecialString(pattern);
		//System.out.println(pat);
		Pattern p = Pattern.compile(s);
		ArrayList<File> list = filePattern(file, p,bRecursive);

		return list;
	}

	/**
	 * @param file
	 *            File 起始文件夹
	 * @param p
	 *            Pattern 匹配类型
	 * @return ArrayList 其文件夹下的文件夹
	 */

	private static ArrayList<File> filePattern(File file, Pattern p, boolean bRecursive ) {
		if (file == null) {
			return null;
		} else if (file.isFile()) {
			Matcher fMatcher = p.matcher(file.getName());
			if (fMatcher.matches()) {
				ArrayList<File> list = new ArrayList<File>();
				list.add(file);
				return list;
			}
		} else if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null && files.length > 0) {
				ArrayList<File> list = new ArrayList<File>();
				for (int i = 0; i < files.length; i++) {
					if (bRecursive){
						ArrayList<File> rlist = filePattern(files[i], p, true);
						if (rlist != null) {
							list.addAll(rlist);
						}
					}else{
						if (files[i].isDirectory()) continue; //skip dir
						ArrayList<File> rlist = filePattern(files[i], p, false);
						if (rlist != null) {
							list.addAll(rlist);
						}
					}
				}
				return list;
			}
		}
		return null;
	}
	
	//返回带路径的文件名
	private static String getFullPathFileName(String path, String src){
		String sRet = new String(src);
		if(m_os.toLowerCase().startsWith("win")){ 
			if (sRet.length()>3 && sRet.indexOf(":"+File.separator)==1){ //有目录路径的fileName
				;//skip
			}else{
				sRet = path+File.separator+sRet;
			}
		}else{
			if (sRet.startsWith(File.separator)){ //有根目录路径的fileName
				;//skip
			}else{
				sRet = path+File.separator+sRet;
			}
		}
		return sRet;
	}
	
	//路径分隔符转换
	public static String replaceAllPathSeparator(String src){
		String sRet = new String(src);
		if (sRet.indexOf("*")!=-1){
			sRet = sRet.replaceAll("\\*", "#");
			if(m_os.toLowerCase().startsWith("win")){  
				sRet = sRet.replaceAll("/", "\\\\");
			}else{
				sRet = sRet.replaceAll("\\\\", "/");
			}
			sRet = sRet.replaceAll("#", "\\*");
		}else{
			if(m_os.toLowerCase().startsWith("win")){  
				sRet = sRet.replaceAll("/", "\\\\");
			}else{
				sRet = sRet.replaceAll("\\\\", "/");
			}
		}
		return sRet;
	}
	
	//获取不带路径的文件列表，fObjs可能是序列、文件对象、字符串
	public static List<String> getFilter(Object fObjs)
	{
		String fname = "";
		FileObject fo=null;
		Object o = null;
		List<String> files=new ArrayList<String>();
		if (fObjs instanceof Sequence){
			Sequence sq = (Sequence)fObjs;			
			for(int i=0; i<sq.length(); i++){
				o = sq.get(i+1);
				if (o instanceof FileObject){
					fo = (FileObject)o;
					fname = fo.getFileName();
				}else{
					fname = o.toString();
				}
				fname = replaceAllPathSeparator(fname);
				files.add(fname);
			}
		}else if(fObjs instanceof FileObject){
			fo = (FileObject)fObjs;
			fname = fo.getFileName();
			fname = replaceAllPathSeparator(fname);
			files.add(fname);		
		}else{
			fname = fObjs.toString();
			fname = replaceAllPathSeparator(fname);
			files.add(fname);
		}
		return files;		
	}
	
	//文件与路径合并
	public static List<String> getPathFilter(String path, Object fObjs)
	{
		if (path!=null && path.endsWith(File.separator)){
			path = path.substring(0, path.length()-1);
		}
		
		String fname = "";
		FileObject fo=null;
		Object o = null;
		List<String> files=new ArrayList<String>();
		if (fObjs instanceof Sequence){
			Sequence sq = (Sequence)fObjs;			
			for(int i=0; i<sq.length(); i++){
				o = sq.get(i+1);
				if (o instanceof FileObject){
					fo = (FileObject)o;
					fname = fo.getFileName();
				}else{
					fname = o.toString();
				}
				fname = replaceAllPathSeparator(fname);
				fname = getFullPathFileName(path, fname);
				
				files.add(fname);
			}
		}else {
			if(fObjs instanceof FileObject){
				fo = (FileObject)fObjs;
				fname = fo.getFileName();
			}else{
				fname = fObjs.toString();
			}
			
			fname = replaceAllPathSeparator(fname);
			fname = getFullPathFileName(path, fname);
			files.add(fname);	
		}
		
		return files;
	}
	
	//通过文件表达式筛选分流成文件列表、目录列表
	public static void getFiles(List<String> filter,ArrayList<File> rFile,
			ArrayList<File> rDir, boolean bRecursive){
		File f = null;
		String fname = null;
		
		for(int i=0; i<filter.size(); i++){
			fname = filter.get(i);
			if (ImUtils.isSpecialCharacters(fname)){ //处理特殊字符*,?
				String val = fname.substring(fname.lastIndexOf(File.separator)+1);
				if (val.equals("*")){
					val = fname.substring(0, fname.length()-2);
					rDir.add(new File(val));
				}else{
					List<File> fs = ImUtils.getFiles(fname, bRecursive);
					if (fs.size()>0){
						rFile.addAll(fs);
					}
				}
			}else{
				f = new File(fname);
				if (f.isDirectory()){
					if (bRecursive){ //递归时加入目录，否则跳过
						rDir.add(f);
					}else{
						File[] subFiles = f.listFiles();
						for(File line:subFiles){
							if (line.isDirectory()) {
								if(bRecursive){
									rDir.add(line.getParentFile());
								}
							}else{
								rFile.add(line);
							}
						}						
					}
				}else{
					rFile.add(f);
				}
			}
		}
	}
	
	public static String replaceSpecialString(String val){
		String pat = val.replace('.', '#');
		pat = pat.replace("#", "\\.");
		if(val.indexOf(".")==-1){ //不带后缀的表达式.
			pat = pat.replace("*", "[\\u4e00-\\u9fa5- /\\w\\.]*");
		}else{
			pat = pat.replace("*", "[\\u4e00-\\u9fa5- /\\w]*");	
		}
		
		pat = pat.replace("?", "\\w{1}");
		return pat;
	}
	
	/*
	 * 获取要处理的压缩文件或文件夹，无指定路径
	 * 
	 * *****/	
	public static void getZipFilterList(ZipFile zipFile, String path, Object fobjs,ArrayList<String> rFile,
			ArrayList<String> rDir,ArrayList<String> rPat) throws ZipException{
		String fname = null;
		List<String> filter = ImUtils.getFilter(fobjs);
		
		for(int i=0; i<filter.size(); i++){
			fname = filter.get(i);
			//System.out.println("file = "+fname);
			if (ImUtils.isSpecialCharacters(fname)){ //处理特殊字符*,?
				String pat = replaceSpecialString(fname);
				rPat.add(pat);
			}else{
 		        String sfile = "";
 		        if (path==null || path.isEmpty() || path.equals("\\")){
 		        	sfile = fname;
 		       }else {
 		        	sfile = path.replaceAll(File.separator, "/") + "/"+ fname;
 		        }

			    FileHeader dirHeader = zipFile.getFileHeader(sfile);  
				if (null == dirHeader){
					rDir.add(fname);
				}else{
					rFile.add(dirHeader.getFileName());
				}
			}
		}
	}
	
	//是否为根路径
	public static boolean isRootPathFile(String file){
		boolean bRet = false;
		String fname = file;
		String os = System.getProperty("os.name");  
		if(os.toLowerCase().startsWith("win")){  
			fname = fname.replaceAll("/", File.separator);
			if (fname.length()>3 && fname.indexOf(":"+File.separator)==1){ //有目录路径的fileName
				bRet = true;
			}
		}else{ //linux
			fname = fname.replaceAll("\\\\", File.separator);
			if (fname.startsWith(File.separator)){ //有目录路径的fileName
				bRet = true;
			}
		}
		
		return bRet;
	}
	
	//获取文件路径，windows下去掉盘符
	public static String getPathOfFile(File file) throws IOException{
		String parent = file.getParentFile().getCanonicalPath();
		String os = System.getProperty("os.name");  
		
		if(os.toLowerCase().startsWith("win")){  
			return parent.substring(3);
		}else{ //linux
			return parent;
		}
	}
}
