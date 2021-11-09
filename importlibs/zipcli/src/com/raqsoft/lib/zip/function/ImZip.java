package com.raqsoft.lib.zip.function;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

	/***********************************
	 　　 1、zip(zipfile:encoding, password; path, files)
		zipfile为zip文件名或FileObject
		encoding为字符编码，省略则为utf-8
		password为密码,可省略
		path为文件所在根目录，省略或为null时为zipfile所在文件目录
		files为可包含通配符*和?的文件名(/和\等同)或文件名序列，也可以是FileObject或FileObject序列
		@u解压: path为输出路径
		@a追加： path为要压缩文件的路径
		@d删除  path为zip文件中的路径
		@n不递归子目录
		@f列出文件名
		@p列出目录名
	 *************************************************/

public class ImZip extends Function {
	private ZipFile m_zfile;
	private boolean m_bRecursive = true; 
	private ZipParameters m_parameters;
	public Node optimize(Context ctx) {
		return this;
	}

	//zip(zipfile:encoding,password; path, files)
	//解析传递参数，存入map
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("zip" + mm.getMessage("function.invalidParam"));
		}

		Map<String, Object> map = new HashMap<String, Object>();
		if (param.isLeaf()){
			map.put("zip", param.getLeafExpression().calculate(ctx));
		}else{
			int nSize = param.getSubSize();		
			char rootType = param.getType();
			for(int i=0; i<nSize; i++){
				ArrayList<Expression> ls = new ArrayList<Expression>();	
				param.getSub(i).getAllLeafExpression(ls);
				if(i==0){ //;前部分
					map.put("zip", ls.get(0).calculate(ctx));
					if (param.getSub(i).getType()==IParam.Comma){				
						if(ls.size()==3){					
							map.put("code", ls.get(1).calculate(ctx));
							map.put("pwd", ls.get(2).calculate(ctx));
						}else if(ls.size()==2){
							map.put("pwd", ls.get(1).calculate(ctx));
						}
					}else if(param.getSub(i).getType()==IParam.Colon){
						if(ls.size()==2){
							map.put("code", ls.get(1).calculate(ctx));
						}
					}
				} else { 
					if (rootType==IParam.Comma){
						map.put("pwd", param.getSub(i).getLeafExpression().calculate(ctx));
					}else{
						//;后部分								
						param.getSub(i).getAllLeafExpression(ls);	
						if (param.getSub(i).getType()==IParam.Comma){
							if (ls.get(0)!=null){
								map.put("path", ls.get(0).calculate(ctx));
							}
							map.put("files", ls.get(1).calculate(ctx));
						}else{
							map.put("path", ls.get(0).calculate(ctx));
						}
					}
				}
			}
		}
		
//		Iterator<String> it =map.keySet().iterator();
//        while(it.hasNext()){
//            //得到每一个key
//            String key = it.next();
//            //通过key获取对应的value
//            Object value = map.get(key);
//            System.out.println("kv:: "+key+"=>"+value);
//        }

		return doZip(option, map);
	}	
	
	//zip功能选项
	private Object doZip(String opt, Map<String, Object> map){
		try {
			String sfile, path=null, code=null,pwd=null;
			
			// for zipFile
			Object o = map.get("zip");
			if (o==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("zip" + mm.getMessage("zipFile is null"));
			}
			if (o instanceof FileObject){
				sfile = ((FileObject)o).getFileName();
			}else{
				sfile =  o.toString();
			}
			// for code
			o = map.get("code");
			if (o!=null){ code = o.toString(); }
			o = map.get("pwd");			
			if (o!=null){ pwd = o.toString(); }
			o = map.get("path");			
			if (o!=null){ 
				path = o.toString(); 
				path = ImUtils.replaceAllPathSeparator(path);
			}			
			
			m_zfile = ImZipUtil.resetZipFile(sfile);
			m_parameters = ImZipUtil.setZipParam(m_zfile, code, pwd);
			
			if (opt!=null){
				if (opt.indexOf("n")!=-1){ //@n不递归子目录
					m_bRecursive = false;
				}
				if (opt.indexOf("u")!=-1){ //@u解压
					if (path==null){
						path = getZipParentPath();
						path = ImUtils.replaceAllPathSeparator(path);
					}
					
					return doUnzipFiles( path, map.get("files"));
				}else if (opt.indexOf("a")!=-1){ //@a追加
					if (path==null){
						path = getZipParentPath();
						path = ImUtils.replaceAllPathSeparator(path);
					}					
					return doZipFiles( path, map.get("files"));
				}else if (opt.indexOf("d")!=-1){ //@d删除
					return delZipFiles(path, map.get("files"));
				}else if (opt.indexOf("f")!=-1){ //@f列出文件名
					return getZipFileNames(path, map.get("files"));
				}else if (opt.indexOf("p")!=-1){ //@p列出目录名
					return getZipDirs( path);
				}
			}
			// 缺省调用
			if (path==null){
				path = getZipParentPath();
				path = ImUtils.replaceAllPathSeparator(path);
			}	
			return doZipFiles(path, map.get("files"));
			
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
		return null;
	}
	
	//获取zipfile所在路径
	private String getZipParentPath() throws Exception{
		return m_zfile.getFile().getParentFile().getCanonicalPath();
	}
	
	//获取zip目录
	private Object getZipDirs( String path){
		Table tbl = null;
		try {				
			File[] fs = ImZipUtil.listDirs(m_zfile, path);
			if (fs.length>0){
				tbl = new Table(new String[]{"DirName"});
				Object[] objs=new Object[1];
				for(File line : fs ){
					objs[0] = line.getPath();
					tbl.newLast(objs);
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return tbl;
	}
	
	//获取zip文件列表
	private Object getZipFileNames( String path, Object fobjs){
		try {	
			String[] filter = null;
			if (fobjs!=null){
				filter = doFileFilter(fobjs);
			}
			File[] fs = ImZipUtil.listFiles(m_zfile, path, filter);
			if (fs==null) return null;
			
			Table tbl = new Table(new String[]{"FileName"});
			Object[] objs=new Object[1];
			for(File line : fs ){
				objs[0] = line.getPath();
				tbl.newLast(objs);
			}
			
			return tbl;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//过滤参数处理
	private String[] doFileFilter(Object fObjs){		
		String fname = "";
		List<String> files=new ArrayList<String>();
		if (fObjs instanceof Sequence){
			Sequence sq = (Sequence)fObjs;
			FileObject fo=null;
			Object o = null;
			
			for(int i=0; i<sq.length(); i++){
				o = sq.get(i+1);
				if (o instanceof FileObject){
					fo = (FileObject)o;
					fname = fo.getFileName();
				}else{
					fname = o.toString();
				}
				fname = fname.replaceAll("/", "\\\\");
				String pat = fname.replace('.', '#');
			    pat = pat.replace("#", "\\.");
			    pat = pat.replace("*", "\\w*");
				pat = pat.replace("?", "\\w{1}");
				files.add(pat);
			}
		}else{
			fname = fObjs.toString();
			String pat = fname.replace('.', '#');
		    pat = pat.replace("#", "\\.");
		    pat = pat.replace("*", "\\w*");
			pat = pat.replace("?", "\\w{1}");
			files.add(pat);
		}
		String fs[] = new String[files.size()];
		files.toArray(fs);
		return fs;
	}
	
	/**
	 * 获取压缩文件或文件夹
	 * 将文件夹或文件拼成带绝对路径
	 * 处理特殊字符*,?
	 * path：压缩文件所在的路径
	 * fobjs：要处理的文件或文件表达式
	 * rFile: 返回的文件列表
	 * rDir： 返回的文件目录
	 * ***/
	
	private void getFileList(String path, Object fobjs,ArrayList<File> rFile,ArrayList<File> rDir){
		Object o = fobjs;
		List<String> filter=null;
		if (o==null && path == null){ 
			MessageManager mm = EngineMessage.get();
			throw new RQException("zip" + mm.getMessage("add src file is null"));
		}else if(o!=null){
			if (path!=null && !path.isEmpty()){
				filter = ImUtils.getPathFilter(path, o);
			}else{
				filter = ImUtils.getFilter(o);
			}
			ImUtils.getFiles(filter, rFile, rDir, m_bRecursive);
		}else if(path!=null){ //(o==null && path!=null)
			if (m_bRecursive){
				rDir.add(new File(path));
			}else{
				filter = new ArrayList<String>();
				filter.add(path);
				ImUtils.getFiles(filter, rFile, rDir, m_bRecursive);
			}
		}
	}
	
	/**
	 * 解压文件处理
	 * path解压输出文件的路径，若为空则与zfile路径所在的路径一致，若是相对路径，则path追加到zfile路径.
	 * fobjs为要解压的文件(列表)
	 *
	 * ***********************************************/
	private Object doUnzipFiles(String path, Object fobjs) throws Exception{
		File [] fs = null;
		Table tbl = new Table(new String[]{"File"});
		Object objs[] = new Object[1];
		
		String rootPath = getZipParentPath();
		if (!ImUtils.isRootPathFile(path)){
			path = rootPath+File.separator+path;
		}
		
		if(fobjs==null || fobjs.toString().isEmpty()){
			fs = ImZipUtil.unzip(m_zfile, path);
			for(File f:fs){
				objs[0] = f;
				tbl.newLast(objs);
			}
		}else{
			ArrayList<String> lfile = new ArrayList<String>();
			ArrayList<String> ldir = new ArrayList<String>();
			ArrayList<String> lpat = new ArrayList<String>(); //特殊字符
			ArrayList<File> ls = new ArrayList<File>();

			ImUtils.getZipFilterList(m_zfile, null, fobjs, lfile, ldir, lpat);
		
			//2.1 解压正常文件			
			if (lfile.size()>0){
				fs = ImZipUtil.unzip(m_zfile, lfile, path);
				ls.addAll(Arrays.asList(fs));
			}
			
			//2.2 解压文件夹			
			if (ldir.size()>0){
				fs = ImZipUtil.unzipDir(m_zfile, ldir, path);
				ls.addAll(Arrays.asList(fs));
			}
			//2.3 解压特殊字符文件。
			if (lpat.size()>0){
				fs = ImZipUtil.unzipFilter(m_zfile, lpat, path);
				ls.addAll(Arrays.asList(fs));
			}
			
			//2.4去重处理:
			HashSet<File> h = new HashSet<File>(ls);   
		    ls.clear();   
		    ls.addAll(h);   
		    Collections.sort(ls);
		    //System.out.println(ls.toString());
		    for(File f:ls){
				objs[0] = f;
				tbl.newLast(objs);
			}
		}
		return tbl;
	}
	
	/**********************************************
	 * 压缩文件处理，也包括追加文件
	 * path要压缩文件的路径，若为空则与zfile路径所在的路径一致.
	 * fobjs为要压缩的文件(列表)
	 * 
	 * ***********************************************/
	private Object doZipFiles( String path, Object fobjs) throws Exception{
		ArrayList<File> lfile = new ArrayList<File>();
		ArrayList<File> ldir = new ArrayList<File>();
		
		String rootPath = getZipParentPath();
		if (!ImUtils.isRootPathFile(path)){
			path = rootPath+File.separator+path;
		}
		if (!(path==null || path.isEmpty())){
			rootPath = path;
		}
		rootPath = rootPath.toLowerCase();
		getFileList(path, fobjs, lfile, ldir);

		String dir = "";		
		for(File f:lfile){		
			String sfile = f.getCanonicalPath().toLowerCase();
			if (sfile.startsWith(rootPath)){
				dir = sfile.substring(0, sfile.lastIndexOf(File.separator));
				dir = dir.replace(rootPath+File.separator, "");
				dir = dir.replace(rootPath, "");
				ImZipUtil.zip(m_zfile, f, dir, m_parameters);
			}else{
				dir = ImUtils.getPathOfFile(f);
				ImZipUtil.zip(m_zfile, f, dir, m_parameters);
			}
		}
		
		if(ldir.size()>0){
			ImZipUtil.zip(m_zfile, m_parameters, null, ldir);
		}
		
		return true;
	}
	/**
	 * 删除压缩文件处理，也包括文件或目录
	 * path: zip文件中要删除文件的路径，从根路径开始的文件夹.
	 * fobjs: 为要压缩的文件(列表)
	 * ***********************************************/
	private Object delZipFiles(String path, Object fobjs){
		boolean bRet = false;
		try {
			//A. 删除目录
			if (fobjs==null || fobjs.toString().isEmpty() || fobjs.toString().equals("*")){
				if (path!=null){
					ImZipUtil.removeDirFromZipArchive(m_zfile, path);
				}
			}else if( path==null || (path!=null &&(path.equals(".") || path.equals("./") || path.equals(".\\") 
					|| path.equals("\\") || path.equals("/"))) ){ //B. 删除文件(区分带特殊字符与非特殊字符情况)：
				ArrayList<String> lfile = new ArrayList<String>();
				ArrayList<String> ldir = new ArrayList<String>();
				ArrayList<String> lpat = new ArrayList<String>(); //特殊字符
				ImUtils.getZipFilterList(m_zfile, null, fobjs, lfile, ldir, lpat);
				int i = 0;
				//2.1 删除正常文件
				if (lfile.size()>0){
					ImZipUtil.removeFilesFromZipArchive(m_zfile, (List<String>)lfile);
				}
				//2.1 删除目录
				if (ldir.size()>0){
					for(String dir:ldir){
						ImZipUtil.removeDirFromZipArchive(m_zfile, dir);
					}
				}
				//2.2 删除特殊字符文件。
				for(i = 0; i<lpat.size(); i++){
					if (path==null){
						ImZipUtil.removePathFilePatternFromZip(m_zfile, lpat.get(i));
					}else{
						ImZipUtil.removeFilePatternFromPathZip(m_zfile, path, lpat.get(i));
					}
				}
			}else{ //C 删除目录与文件
				ArrayList<String> lfile = new ArrayList<String>();
				ArrayList<String> ldir = new ArrayList<String>();
				ArrayList<String> lpat = new ArrayList<String>(); //特殊字符
				ImUtils.getZipFilterList(m_zfile, path, fobjs, lfile, ldir, lpat);
				int i = 0;
				//2.1 删除正常文件
				if (lfile.size()>0){ //file与path合并后再删除.
					String file="";
					ArrayList<String> ls = new ArrayList<String>();
					for(String f:lfile){
						if (path.equals("\\")){
							file = f;
						}else{
							file = path+"/"+f;
						}
						ls.add(file);
					}
					lfile.clear();
					ImZipUtil.removeFilesFromZipArchive(m_zfile, ls);
				}
				
				//2.1 删除目录
				if (ldir.size()>0){
					for(String dir:ldir){
						ImZipUtil.removeDirFromZipArchive(m_zfile, dir);
					}
				}
				//2.2 删除特殊字符文件。
				for(i = 0; i<lpat.size(); i++){
					ImZipUtil.removeFilePatternFromPathZip(m_zfile, path, lpat.get(i));
				}				
			}
			bRet = true;
		} catch (ZipException e) {
			Logger.error(e.getStackTrace());
		}
		
		return bRet;
	}
}
