package com.raqsoft.lib.zip.function;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;  
import java.util.Collections;  
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raqsoft.common.Logger;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;  
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;  
 
/** 
* ZIP压缩文件操作工具类 
*/  
public class ImZipUtil {  
     
	/** 
    * 解压指定的ZIP压缩文件到当前目录
    * @param zip 指定的ZIP压缩文件 
    * @return  解压后文件数组 
    */  
   public static File [] unzip(String zip) throws ZipException {  
       return unzip(zip, null);  
   }
   
	/** 
    * 使用给定密码解压指定的ZIP压缩文件到当前目录 
    * @param zip 指定的ZIP压缩文件 
    * @param passwd ZIP文件的密码 
    * @return  解压后文件数组 
    * @throws ZipException 压缩文件有损坏或者解压缩失败抛出 
    */  
   public static File [] unzip(String zip, String passwd) throws ZipException {  
       File zipFile = new File(zip);  
       File parentDir = zipFile.getParentFile();  
       return unzip(zipFile, parentDir.getAbsolutePath(), passwd, null);  
   }
   	   
   /** 
    * 使用给定密码解压指定的ZIP压缩文件到指定目录 
    *  
    * 如果指定目录不存在,可以自动创建,不合法的路径将导致异常被抛出 
    * @param zip 指定的ZIP压缩文件 
    * @param dest 解压目录 
    * @param passwd ZIP文件的密码 
    * @return 解压后文件数组 
    * @throws ZipException 压缩文件有损坏或者解压缩失败抛出 
    */  
   public static File [] unzip(String zip, String dest, String passwd) throws ZipException {  
       File zipFile = new File(zip);  
       return unzip(zipFile, dest, passwd,null);  
   }  
     
        
   /** 
    * 使用给定密码解压指定的ZIP压缩文件到指定目录 
    *  
    * 如果指定目录不存在,可以自动创建,不合法的路径将导致异常被抛出 
    * @param zip 指定的ZIP压缩文件 
    * @param dest 解压目录 
    * @param passwd ZIP文件的密码 
    * @param charsetName 采用的编码
    * @return  解压后文件数组 
    * @throws ZipException 压缩文件有损坏或者解压缩失败抛出 
    */  
   public static File [] unzip(File zipFile, String dest, String passwd, String charsetName) throws ZipException {  
       ZipFile zFile = new ZipFile(zipFile);  
	   setZipParam(zFile, passwd, charsetName);
		  
       File destDir = new File(dest);  
       if (destDir.isDirectory() && !destDir.exists()) {  
           destDir.mkdir();  
       }  
       zFile.extractAll(dest);  
         
       List<FileHeader> headerList = zFile.getFileHeaders();  
       List<File> extractedFileList = new ArrayList<File>();  
       for(FileHeader fileHeader : headerList) {  
           if (!fileHeader.isDirectory()) {  
               extractedFileList.add(new File(destDir,fileHeader.getFileName()));  
           }  
       }  
       File [] extractedFiles = new File[extractedFileList.size()];  
       extractedFileList.toArray(extractedFiles);  
       return extractedFiles;  
   }  
   
   //解压到指定目录
   public static File [] unzip( ZipFile zFile, String dest) throws ZipException {  
       File destDir = new File(dest);  
       if ( !destDir.exists()) {  
           destDir.mkdir();  
       }  
     
       zFile.extractAll(dest);  
        
       List<FileHeader> headerList = zFile.getFileHeaders();  
       List<File> extractedFileList = new ArrayList<File>();  
       for(FileHeader fileHeader : headerList) {  
           if (!fileHeader.isDirectory()) {  
               extractedFileList.add(new File(destDir,fileHeader.getFileName()));  
           }  
       }  
       File [] extractedFiles = new File[extractedFileList.size()];  
       extractedFileList.toArray(extractedFiles);  
       return extractedFiles;  
   }  
   
   //解压给定的文件到指定目录
   public static File [] unzip(ZipFile zFile, ArrayList<String> files, String dest) throws ZipException {  
       File destDir = new File(dest);  
       if (destDir.isDirectory() && !destDir.exists()) {  
           destDir.mkdir();  
       }
  
       File [] extractedFiles = new File[files.size()];  
       int i = 0;
       for(String fileName:files){
    	   zFile.extractFile(fileName, dest);
    	   extractedFiles[i++] = new File(dest+File.separator+fileName);
       }
      
       return extractedFiles;  
   }  
   
 	//解压给定的文件夹到指定目录
   public static File [] unzipDir(ZipFile zipFile, ArrayList<String> dirs, String dest) throws ZipException { 
	   if (!zipFile.isValidZipFile()) {  
		   return null;
       }  
       File destDir = new File(dest);  
       if (destDir.isDirectory() && !destDir.exists()) {  
           destDir.mkdir();  
       }
  
       List<String> extractNames = new ArrayList<String>();  
       for(String dir : dirs){
	       if (!dir.isEmpty()){
		    	if (!dir.endsWith(File.separator)) dir += File.separator;  
		    }
		    // 如果目录不存在则跳过  
		    FileHeader dirHeader = zipFile.getFileHeader(dir);  
		    if (null == dirHeader) continue;  		   
			
		    // 遍历压缩文件中所有的FileHeader, 将指定删除目录下的子文件名保存起来  
		    String subName = "";
		    List<FileHeader> headersList = zipFile.getFileHeaders();  
		    
		    for(int i=0, len = headersList.size(); i<len; i++) {  
		        FileHeader subHeader = (FileHeader) headersList.get(i);  
		        subName = subHeader.getFileName();
		        if (subName.startsWith(dirHeader.getFileName()) && 
		           !subName.equals(dirHeader.getFileName())) {
		        	extractNames.add(subName);
		        }  
		    }  
       }
       
       File fs[] = new File[extractNames.size()];
       int i = 0;
       for(String s : extractNames){
    	   zipFile.extractFile(s, dest);    	   
    	   fs[i++] = new File(dest+File.separator+s);
       }
       
	    return fs;
   }  
   
   //解压文件带特殊字符的
   public static File [] unzipFilter(ZipFile zFile, ArrayList<String> filter, String dest) throws Exception {  
       File destDir = new File(dest);  
       if (destDir.isDirectory() && !destDir.exists()) {  
           destDir.mkdir();  
       }
       
       File[] fs = listFiles(zFile);
       //filter fileName
       ArrayList<String> zipFileList = new ArrayList<String>();  
       if (filter!=null && filter.size()>0){
    	   Matcher m = null;
    	   Pattern p = null;
    	   for(String flt:filter){
    		   p = Pattern.compile(flt);
    		   for(int i=0; i<fs.length; i++){
    			   m = p.matcher(fs[i].getName());
    			   if(m.matches()){
        			   zipFileList.add(fs[i].getPath());
        		   }
    		   }    		   
	       }
    	   fs=null;
       }
       
       return unzip(zFile, zipFileList, dest);
   }  
     
   /***************************************
    * 不解压情况下，获取压缩文件zip中的文件名.
    * **************************************/
   public static File [] listFiles(ZipFile zFile) throws ZipException {
       List<FileHeader> headerList = zFile.getFileHeaders();  
       List<File> extractedFileList = new ArrayList<File>();  
       for(FileHeader fileHeader : headerList) {  
           if (!fileHeader.isDirectory()) {  
               extractedFileList.add(new File(fileHeader.getFileName()));  
           }  
       }  
       File [] extractedFiles = new File[extractedFileList.size()];  
       extractedFileList.toArray(extractedFiles);  
       return extractedFiles;  
   }  
   
   //获取zip文件中path下的文件，path为空则获取所有的文件
   public static File [] listFiles(ZipFile zFile, String path, String[] filter) throws ZipException {
	   List<FileHeader> headerList = zFile.getFileHeaders();  
       List<File> extractedFileList = new ArrayList<File>();  
       if (path==null){
	       for(FileHeader fileHeader : headerList) {  
	           if (!fileHeader.isDirectory()) {  
	               extractedFileList.add(new File(fileHeader.getFileName()));  
	           }  
	       }  
       } else{
    	   String subName = "";
    	   if (!path.endsWith(File.separator)) path += File.separator;  
    	   String sDir = path.replace("\\", "/");
    	   sDir = sDir.replace("//", "/");
    	   for(FileHeader fileHeader : headerList) {  
   		        subName = fileHeader.getFileName();
   		        if (subName.startsWith(sDir) && !subName.equals(sDir)) {
		           extractedFileList.add(new File(fileHeader.getFileName()));  
   		        }
	       }     
       }
       
       //filter fileName
       List<File> zipFileList = new ArrayList<File>();  
       if (filter!=null && filter.length>0){
    	   Matcher m = null;
    	   Pattern p = null;
    	   for(String flt:filter){
    		   p = Pattern.compile(flt);
    		   for(int i=0; i<extractedFileList.size(); i++){
    			   m = p.matcher(extractedFileList.get(i).getName());
    			   if(m.matches()){
        			   zipFileList.add(extractedFileList.get(i));
        		   }
    		   }    		   
	       }
    	   extractedFileList.clear();
    	   File [] extractedFiles = new File[zipFileList.size()];  
    	   zipFileList.toArray(extractedFiles);  
	       return extractedFiles; 
       }else{       
		   File [] extractedFiles = new File[extractedFileList.size()];  
	       extractedFileList.toArray(extractedFiles);  
	       return extractedFiles;  
       }
   }
   
   //获取zip文件中path下的目录，path为空则获取所有的目录
   public static File [] listDirs(ZipFile zFile, String path) throws ZipException {
       List<FileHeader> headerList = zFile.getFileHeaders();  
       List<String> extractedFileList = new ArrayList<String>();  
      
       if (path==null || path.isEmpty()){
	       for(FileHeader fileHeader : headerList) {  
	    	   if (fileHeader.isDirectory()) {
	    		   String spath = fileHeader.getFileName();
	    		   if (spath.endsWith("/")) spath=spath.substring(0, spath.length()-1);
	    		   if (!extractedFileList.contains(spath)){
	    			   extractedFileList.add(spath);  
	    		   }
	           } else {
	        	   int off=fileHeader.getFileName().lastIndexOf("/") ;
	        	   if(off>-1){
	        			String subStr = fileHeader.getFileName().substring(0, off);
	        			if (!extractedFileList.contains(subStr)){
	        				extractedFileList.add(subStr);  
	        			}
	        	   }
	           }
	       }  
       }else{
    	   if (!path.endsWith(File.separator)) path += File.separator;  
    	   String spath =  path.replace(File.separator, "/");
		    String subName = "";
		    for(FileHeader fileHeader : headerList) {   		    	
		        if (fileHeader.isDirectory()){
			        subName = fileHeader.getFileName();
			        if (subName.endsWith("/")) subName=subName.substring(0, subName.length()-1);
			        if (subName.startsWith(spath) && !subName.equals(spath)) {
			        	 if (!extractedFileList.contains(subName)){
			        		 extractedFileList.add(subName);  
			        	 }
			        }  
		        }else{
		        	int off=fileHeader.getFileName().lastIndexOf("/") ;
	        	    if(off>-1){
	        			String subStr = fileHeader.getFileName().substring(0, off);
	        			if (subStr.startsWith(path)){
		        			if (!extractedFileList.contains(subStr)){
		        				extractedFileList.add(subStr);  
		        			}
	        			}
	        	    }
		        }
		    }  
       }
     
	   File [] extractedFiles = new File[extractedFileList.size()];  
	   for(int n=0; n<extractedFileList.size(); n++){
		   extractedFiles[n] = new File( extractedFileList.get(n));  
	   }

	   return extractedFiles;  
   }
   
    //zip参数设置
	public static ZipParameters setZipParam(ZipFile zFile, String charsetName, String passwd) throws ZipException {
	   ZipParameters zRet = new ZipParameters();
	  
	   zRet.setCompressionMethod(CompressionMethod.DEFLATE);  
	   zRet.setCompressionLevel(CompressionLevel.NORMAL);
	   //设置密码
	   if(passwd!=null && !passwd.isEmpty()){
		   zRet.setEncryptFiles(true);  
		   zRet.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);   
		   zFile.setPassword(passwd.toCharArray());
	   }
	   
	   if(charsetName==null || charsetName.isEmpty()){
		   charsetName = "UTF8";
	   }
	   
	   zFile.setCharset(Charset.forName(charsetName));

	   return zRet;
	   
   }
   /** 
    * 压缩指定文件到当前文件夹 
    * @param src 要压缩的指定文件 
    * @return 最终的压缩文件存放的绝对路径,如果为null则说明压缩失败. 
    */  
   public static String zip(String src) {  
       return zip(src,null);  
   }
 
   /** 
    * 使用给定密码压缩指定文件或文件夹到当前目录 
    * @param src 要压缩的文件 
    * @param passwd 压缩使用的密码 
    * @return 最终的压缩文件存放的绝对路径,如果为null则说明压缩失败. 
    */  
   public static String zip(String src, String passwd) {  
       return zip(src, null, passwd);  
   }  
   
  
   /** 
    * 使用给定密码压缩指定文件或文件夹到当前目录 
    * @param src 要压缩的文件 
    * @param dest 压缩文件存放路径 
    * @param passwd 压缩使用的密码 
    * @return 最终的压缩文件存放的绝对路径,如果为null则说明压缩失败. 
    */  
   public static String zip(String src, String dest, String passwd) {  
       return zip(src, dest, true, passwd);  
   }  
    
   /** 
    * 使用给定密码压缩指定文件或文件夹到指定位置. 
    *  
    * dest可传最终压缩文件存放的绝对路径,也可以传存放目录,也可以传null或者"". 
    * 如果传null或者""则将压缩文件存放在当前目录,即跟源文件同目录,压缩文件名取源文件名,以.zip为后缀; 
    * 如果以路径分隔符(File.separator)结尾,则视为目录,压缩文件名取源文件名,以.zip为后缀,否则视为文件名. 
    * @param src 要压缩的文件或文件夹路径 
    * @param dest 压缩文件存放路径 
    * @param isCreateDir 是否在压缩文件里创建目录,仅在压缩文件为目录时有效. 
    * 如果为false,将直接压缩目录下文件到压缩文件. 
    * @param passwd 压缩使用的密码 
    * @return 最终的压缩文件存放的绝对路径,如果为null则说明压缩失败. 
    */  
   
   public static String zip(String src, String dest, boolean isCreateDir, String passwd) {  
	   return zip(src, dest, isCreateDir, passwd, null);
   }
   
   /** 
    * 使用给定密码压缩指定文件或文件夹到指定位置. 
    *  
    * @param files 要压缩的文件或文件夹路径 
    * @param rootFolder 压缩文件存放路径 
    * @param zFile 生成的压缩文件名，以.zip为后缀; 
    * @param passwd 压缩使用的密码
    * @param charsetName 采用的编码  
    * @return 最终的压缩文件存放的绝对路径,如果为null则说明压缩失败. 
    */  

   // 压缩文件时带路径,
   public static String zips(ArrayList<File> files, String rootFolder, String zFile, String passwd, String charsetName) {  
	   ZipFile zipFile = null;
	   ZipParameters parameters = new ZipParameters();  
       createDestDirectoryIfNecessary(zFile);  
       parameters.setCompressionMethod(CompressionMethod.DEFLATE);           // 压缩方式  
       parameters.setCompressionLevel(CompressionLevel.NORMAL);    			 // 压缩级别  
       try { 
	       if (!(passwd==null || passwd.length()==0)) {  
	           parameters.setEncryptFiles(true);  
	           parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD); // 加密方式  
	           zipFile = new ZipFile(zFile, passwd.toCharArray());  
	       }else{        
	    	   zipFile = new ZipFile(zFile);  
	       }
	       
           if (charsetName==null ||charsetName.length()==0){
        	   zipFile.setCharset(Charset.forName("UTF8")); 
           }else{
        	   zipFile.setCharset(Charset.forName(charsetName)); 
           }

           parameters.setRootFolderNameInZip(rootFolder);
           if (files!=null && files.size()>0){
        	   zipFile.addFiles(files, parameters);  
           }
           zipFile.close();
           return zFile;  
       } catch (Exception e) {  
           e.printStackTrace();  
       }  
       return null;  
   }  
   /* 单文件追加到给定的dir
    * zFile zip文件名
    * file 要压缩的文件
    * dir 压缩包的文件夹
    */
   public static boolean zip(ZipFile zipFile, File file, String zipDir, ZipParameters parameters) throws IOException { 
	   try {  
    	   String sFile = zipFile.getFile().getCanonicalPath();
           createDestDirectoryIfNecessary(sFile);  
           parameters.setRootFolderNameInZip(zipDir);
           
           zipFile.addFile(file, parameters);  
           
           return true;  
       } catch (Exception e) {  
    	   Logger.error("zip zipDir="+zipDir+";File="+file.getCanonicalPath() + " false");
       }  
       return false;  
   }  
   
   //对给定的files，　dirs进行压缩处理
   public static String zip(ZipFile zipFile, ZipParameters parameters, ArrayList<File> files, ArrayList<File> dirs) {
	   if (parameters.getCompressionLevel()!=CompressionLevel.ULTRA && parameters.getCompressionMethod()!=CompressionMethod.STORE){
		   parameters.setCompressionMethod(CompressionMethod.DEFLATE);           // 压缩方式  
	       parameters.setCompressionLevel(CompressionLevel.NORMAL);    			 // 压缩级别  
       }
       
       try {           
           if (files!=null && files.size()>0){
        	   zipFile.addFiles(files, parameters);  
           }
           if (dirs!=null && dirs.size()>0){
        	   for(int i=0; i<dirs.size(); i++){
        		   zipFile.addFolder(dirs.get(i), parameters);
        	   }
           }
           return zipFile.getFile().getName();  
       } catch (ZipException e) {  
    	   Logger.error(e.getStackTrace());
       }  
       return null;  
   }  
   
   //将src压缩成dest文件返回
   public static String zip(String src, String dest, boolean isCreateDir, String passwd, String charsetName) {  
	   ZipFile zipFile = null;
	   File srcFile = new File(src);  
       dest = buildDestinationZipFilePath(srcFile, dest);  
       ZipParameters parameters = new ZipParameters();  
       parameters.setCompressionMethod(CompressionMethod.DEFLATE);           // 压缩方式  
       parameters.setCompressionLevel(CompressionLevel.NORMAL);    			 // 压缩级别  
       
       try {  
    	   if (!(passwd==null || passwd.length()==0)) {  
               parameters.setEncryptFiles(true);  
               parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD); // 加密方式  
               zipFile = new ZipFile(dest, passwd.toCharArray());  
           } else {
        	   zipFile = new ZipFile(dest);  
           }
    	   
    	   if (charsetName==null ||charsetName.length()==0){
        	   zipFile.setCharset(Charset.forName("UTF8")); 
           }else{
        	   zipFile.setCharset(Charset.forName(charsetName)); 
           }
    	   
           if (srcFile.isDirectory()) {  
               // 如果不创建目录的话,将直接把给定目录下的文件压缩到压缩文件,即没有目录结构  
               if (!isCreateDir) {  
                   File [] subFiles = srcFile.listFiles();  
                   ArrayList<File> temp = new ArrayList<File>();  
                   Collections.addAll(temp, subFiles);  
                   zipFile.addFiles(temp, parameters);  
                   return dest;  
               }  
               zipFile.addFolder(srcFile, parameters);  
           } else {  
               zipFile.addFile(srcFile, parameters);  
           }  
           
           return dest;  
       } catch (Exception e) {  
           Logger.error(e.getStackTrace()); 
       }  finally{
    	   try {
				zipFile.close();
    	   } catch (IOException e) {
				Logger.error(e.getStackTrace());
    	   }
       }
       return null;  
   }  
   
   /** 
    * 构建压缩文件存放路径,如果不存在将会创建 
    * 传入的可能是文件名或者目录,也可能不传,此方法用以转换最终压缩文件的存放路径 
    * @param srcFile 源文件 
    * @param destParam 压缩目标路径 
    * @return 正确的压缩文件存放路径 
    */  
   private static String buildDestinationZipFilePath(File srcFile,String destParam) { 
	   if (destParam==null || destParam.length()==0) {
           if (srcFile.isDirectory()) {  
               destParam = srcFile.getParent() + File.separator + srcFile.getName() + ".zip";  
           } else {  
               String fileName = srcFile.getName().substring(0, srcFile.getName().lastIndexOf("."));  
               destParam = srcFile.getParent() + File.separator + fileName + ".zip";  
           }  
       } else {  
           createDestDirectoryIfNecessary(destParam);  // 在指定路径不存在的情况下将其创建出来  
           if (destParam.endsWith(File.separator)) {  
               String fileName = "";  
               if (srcFile.isDirectory()) {  
                   fileName = srcFile.getName();  
               } else {  
                   fileName = srcFile.getName().substring(0, srcFile.getName().lastIndexOf("."));  
               }  
               destParam += fileName + ".zip";  
           }  
       }  
       return destParam;  
   }  
     
   /** 
    * 在必要的情况下创建压缩文件存放目录,比如指定的存放路径并没有被创建 
    * @param destParam 指定的存放路径,有可能该路径并没有被创建 
    */  
   private static void createDestDirectoryIfNecessary(String destParam) {  
       File destDir = null;  
       String dest = destParam.replace("/", File.separator);
       dest = dest.replace("\\", File.separator);
       if (dest.endsWith(File.separator)) {  
           destDir = new File(dest);  
       } else {  
           destDir = new File(dest.substring(0, dest.lastIndexOf(File.separator)));  
       }  
       if (!destDir.exists()) {  
           destDir.mkdirs();  
       }  
   }  
   
   // 删除目录.
   public static boolean removeDirFromZipArchive(ZipFile zipFile, String removeDir) throws ZipException {  
	   boolean bRet = false;
	   try{
		    // 给要删除的目录加上路径分隔符  
		   if (!removeDir.endsWith(File.separator)) removeDir += File.separator;  
		   String rDir = removeDir.replace(File.separator, "/");
		   rDir = rDir.replace("//", "/");
		  
		    // 遍历压缩文件中所有的FileHeader, 将指定删除目录下的子文件名保存起来  
		    List<FileHeader> headersList = zipFile.getFileHeaders();  
		    List<String> removeHeaderNames = new ArrayList<String>();  
		    for(int i=0, len = headersList.size(); i<len; i++) {  
		        FileHeader subHeader = (FileHeader) headersList.get(i);  
		        //System.out.println(subHeader.getFileName()+"=="+subHeader.isDirectory());
		        if (subHeader.getFileName().startsWith(rDir)  
		                && !subHeader.getFileName().equals(rDir)) {  
		            removeHeaderNames.add(subHeader.getFileName());  
		        }  
		    }  
		    // 遍历删除指定目录下的所有子文件, 最后删除指定目录(此时已为空目录)  
		    for(String headerNameString : removeHeaderNames) {  
		        zipFile.removeFile(headerNameString);  
		    }  
	   }catch(Exception e){
		   Logger.error(e.getStackTrace());
	   }
	   
	   return bRet;
	}  
   
   // 删除条件为正常文件.
   public static boolean removeFilesFromZipArchive(ZipFile zipFile,  List<String> files) throws ZipException {
	   boolean bRet = false;
	   try{
		    // 遍历删除指定目录下的所有子文件, 最后删除指定目录(此时已为空目录)  
		    for(String headerNameString : files) {  
		    	try{
		    		zipFile.removeFile(headerNameString);  
		    	}catch(Exception e){
		    		;
		    	}
		    	bRet = true;
		    }
		    
	   }catch(Exception e){
		   Logger.error(e.getStackTrace());
	   }
	   
	   return bRet;
	}  
   
   // 删除条件带特殊字符文件.
   // pattern是纯表达式，如：*.xml
   public static boolean removePathFilePatternFromZip(ZipFile zipFile, String filePattern) throws ZipException {
	   if (filePattern.indexOf(File.separator)!=-1){
		   String removeDir=filePattern.substring(0, filePattern.indexOf(File.separator)+1);
		   String pat=filePattern.substring( filePattern.indexOf(File.separator)+1);
		   if(pat.equals("*")){
			   return removeDirFromZipArchive(zipFile, removeDir); 
		   }else if(removeDir.equals("."+File.separator)){
			   return removeFilePatternFromZip(zipFile, pat); 
		   }else{
			   return removeFilePatternFromPathZip(zipFile, removeDir, pat);
		   }
	   }else{
		   String pat = ImUtils.replaceSpecialString(filePattern);
		   return removeFilePatternFromZip(zipFile, pat);
	   }
   }
   
   //对符合pattern条件的文件进行删除
   private static boolean removeFilePatternFromZip(ZipFile zipFile, String pattern) throws ZipException {
	   boolean bRet = false;
	   try{
		    // 遍历压缩文件中所有的FileHeader, 将指定删除目录下的子文件名保存起来  
		    String subName = "", subOrg="";
		    List<FileHeader> headersList = zipFile.getFileHeaders();  
		    List<String> removeHeaderNames = new ArrayList<String>();  
		    
		    for(int i=0, len = headersList.size(); i<len; i++) {  
		        FileHeader subHeader = (FileHeader) headersList.get(i);  
		        subOrg = subName = subHeader.getFileName();	
		        int start = subName.indexOf("/",1);
		        if (start>0){
		        	subName = subName.substring(start+1);	
		        }
	        	//System.out.println(subName);
	        	if (subName.matches(pattern)){
	        		removeHeaderNames.add(subOrg);  
	        	}		          
		    }  

		    for(String headerNameString : removeHeaderNames) {  
		        zipFile.removeFile(headerNameString);  
		    }
	   }catch(Exception e){
		   Logger.error(e.getStackTrace());
	   }
	   
	   return bRet;
	}  
   
   ////对给定目录removeDir下的符合pattern条件的文件进行删除
   public static boolean removeFilePatternFromPathZip(ZipFile zipFile, String removeDir, String pattern) throws ZipException {
	   boolean bRet = false;
	   try{
		   // 给要删除的目录加上路径分隔符  
		   if (!removeDir.endsWith(File.separator)) removeDir += File.separator;  
		   String rDir = removeDir.replace(File.separator, "/");
		   rDir = rDir.replace("//", "/");
		    // 遍历压缩文件中所有的FileHeader, 将指定删除目录下的子文件名保存起来  
		    String subName = "";
		    List<FileHeader> headersList = zipFile.getFileHeaders();  
		    List<String> removeHeaderNames = new ArrayList<String>();  

		    for(int i=0, len = headersList.size(); i<len; i++) {  
		        FileHeader subHeader = (FileHeader) headersList.get(i);  
		        subName = subHeader.getFileName();
		        //System.out.println("sub="+subName);
		        if(rDir.equals("/")){ //根目录下的文件才有效
		        	if (subName.indexOf("/")==-1){
			        	if (subName.matches(pattern)){
			        		removeHeaderNames.add(subName);  
			        	}
		        	}
		        }else if (subName.startsWith(rDir) && !subName.equals(rDir)) {
		        	String sub = subName.replaceFirst(rDir, "");
		        	
		        	if (sub.matches(pattern)){
		        		removeHeaderNames.add(subName);  
		        	}
		        }  
		    }  
		    // 遍历删除指定目录下的所有子文件, 最后删除指定目录(此时已为空目录)  
		    for(String headerNameString : removeHeaderNames) {  
		        zipFile.removeFile(headerNameString);  
		    }
	   }catch(Exception e){
		   Logger.error(e.getStackTrace());
	   }
	   
	   return bRet;
	}  
   
   //对空内容的zip文件处理.
   public static ZipFile resetZipFile(String sfile) throws ZipException{
	   if(!isValidZipFile(sfile)){
			File f = new File(sfile);
			if(f.exists()){
				f.delete();
			}
	   }
	   return new ZipFile(sfile);
   }
   
   
   //检测是否为有效的zip文件
   private static boolean isValidZipFile(String sfile ){
		boolean bRet = false;
		try {
			ZipFile zfile = new ZipFile(sfile);
			zfile.getFileHeaders();
			bRet = true;
			zfile.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		return bRet;
	}
   
   public static void main(String[] args) {  
     
     try {  
         //File[] files = unzip("d:\\tmp\\zdata汉字.zip", "111");
    	 //File[] files = unzip("d:\\tmp\\一个例子100.zip", "222");
    	 //File[] files = unzip("d:\\tmp\\zdata\\zdat.zip",null);
//    	 File[] files = unzip("d:\\tmp\\zdata\\emp.zip");
//         for (int i = 0; i < files.length; i++) {  
//             System.out.println(files[i]);  
//         }  
    	 if (1==2){
    		 zip("d:\\tmp\\zdata\\dir", "d:\\tmp\\zdata\\emp_1007.zip"); 
    	 }else if(1==12){
    		 ArrayList<String> files=new ArrayList<String>();
    		 files.add("dir");
    		 unzip(new ZipFile("d:/tmp/zdata/java_1004.zip"), "d:/tmp/zout");
    	 }else if(1==1){
    		 ArrayList<String> files=new ArrayList<String>();
    		 files.add("dir");
    		 ZipFile zipFile = new ZipFile("d:/tmp/zdata/emp_1002.zip");
    		 List<FileHeader> headersList = zipFile.getFileHeaders();  
    		 unzipDir(zipFile,files,"d:/tmp/zout4");
    	 }else if(1==1){
	    	 String zfile = "d:/tmp/zdata/emp_1006.zip";
	    	
	    	 String pwd=null;
	    	 String code=null;
	    	 ArrayList<File> files=new ArrayList<File>();
	    	 files.add(new File("d:/tmp/zdata/aat.xlsx"));
	    	 files.add(new File("d:/tmp/zdata/cout.xlsx"));
	    	//ImZipUtil.removeFilesFromZipArchive(zfile, files, pwd, code);
	    	 zips(files, "ddd2", zfile, pwd, code);
    	 }
    	 
    	 
     } catch (Exception e) {  
         e.printStackTrace();  
     }  
   }  
}  