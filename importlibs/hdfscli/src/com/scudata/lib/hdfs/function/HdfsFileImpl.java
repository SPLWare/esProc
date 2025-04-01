package com.scudata.lib.hdfs.function;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileRandomOutputStream;
import com.scudata.dm.IFile;
import com.scudata.dm.RandomOutputStream;

public class HdfsFileImpl implements IFile {
	private FileSystem hdfs;
	private String fileName;
	private CompressionCodec codec;
	private ImRandomOutputStream ost = null;
	
	public HdfsFileImpl() {}
	
	public HdfsFileImpl( String fileName ) {
		initFile( fileName );
	}
	
	public HdfsFileImpl(FileSystem fs)
	{
		hdfs = fs;
	}
	
	public HdfsFileImpl(FileSystem fs, String fileName)
	{
		hdfs = fs;
		initFile(fileName);
	}
	
	private void initFile( String fileName ) {		
		this.fileName = fileName;
        Path inputPath = new Path( fileName );
        CompressionCodecFactory factory = new CompressionCodecFactory( hdfs.getConf() );
        codec = factory.getCodec( inputPath );
	}
	
	public boolean deleteDir(){
		return true;
	}
	
	public void setFileName( String fileName ) {
		initFile( fileName );
	}
	
	public boolean exists(String fileName) {
		try {
			return hdfs.exists( new Path( fileName ) );
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}
	
	public boolean exists() {
		try {
			return hdfs.exists( new Path( fileName ) );
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}
	
	/**
	 * 返回文件长度
	 * @return
	 */
	public long size() {
		try {
			if( !hdfs.exists( new Path( fileName ) ) ) return 0;
			return hdfs.getFileStatus( new Path( fileName ) ).getLen();
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}

	public long lastModified() {
		try {
			return hdfs.getFileStatus( new Path( fileName ) ).getModificationTime();
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}
	
	public boolean move( String newName, String opt ) {
		try {
			if( opt != null && opt.trim().length() > 0 ) {
				throw new Exception( "Not support the option " + opt );
			}
			return hdfs.rename( new Path( fileName ), new Path( newName ) );
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}
	
	public boolean delete() {
		try {
			return hdfs.delete( new Path( fileName ), true );
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}
	
	/**
	 * 打开文件输入流
	 * @return 返回org.apache.hadoop.fs.FSDataInputStream,此流对象有seek(long desired)方法定位指针位置 
	 */
	public InputStream getInputStream() {
   		try {
   			if( codec != null ) {
   				return codec.createInputStream( hdfs.open( new Path( fileName ) ) );
   			}
   			Path pt = new Path( fileName );
   			InputStream ism = hdfs.open( pt);
			return ism;
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}
	
	/**
	 * 打开文件输出流，如果文件不存在则创建并打开，存在则打开
	 * @param isAppend 是否以追加方式打开
	 * @return
	 */
	public OutputStream getOutputStream( boolean isAppend ) {
		try {
			Path p = new Path( fileName ).getParent();
			if( !hdfs.exists( p ) ) hdfs.mkdirs( p );
			OutputStream os = null;
			if( !isAppend ) os = hdfs.create( new Path( fileName ) );
			else os = hdfs.append( new Path( fileName ) );
			if( codec != null ) {
				os = codec.createOutputStream( os );
			}
			return os;
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}
	
	public String createTempFile( String prefix ) {
		try {
			Path path = new Path( fileName );
			Path p = path;
			String suffix = "";
			int pos = path.getName().lastIndexOf( "." );
			if( pos > 0 ) suffix = path.getName().substring( pos + 1 );
			if( suffix.length() > 0 ) {
				p = path.getParent();
			}
			while( true ) {
				long sj = System.currentTimeMillis() % 10000;
				String fn = p.toString() + "/" + prefix + sj;
				if( suffix.length() > 0 ) fn = fn + "." + suffix;
				if( !hdfs.exists( new Path( fn ) ) ) return fn;
			}
		}catch( Exception e ) {
			throw new RQException( e );
		}		
	}
	// fs, src, dest
	public void uploadFile( String localFile, String hdFile ) throws Exception {
		try {
			//System.out.println("upload localFile="+localFile+"  hdFile="+hdFile);
			String remoteFile = new String(hdFile);
			boolean bDir = isRemoveDir(hdFile);
			if (bDir){
				File f = new File(localFile);
				String name = f.getName();
				if (hdFile.endsWith("/")){
					remoteFile += name;
				}else{
					remoteFile += "/"+name;
				}				
			}
			System.out.println("upload2 localFile="+localFile+"  remoteFile="+remoteFile);
			hdfs.copyFromLocalFile(new Path(localFile), new Path(remoteFile));
//			initFile(remoteFile);
//
//			OutputStream output = getOutputStream( false );
//			InputStream input = new FileInputStream(localFile);
//			//System.out.println("upload3 input="+input+"  output="+output);
//			//copyByte
//		    IOUtils.copyBytes(input,output,1024,true);
		
		    //System.out.println("upload4 input="+input+"  output="+output);
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
	}
	
	// fs, src, dest
	public void uploadFiles( String localDir, String hdDir) throws Exception {
		try {
			List<String> lsDir = new ArrayList<String>();
	        List<String> lsFile = new ArrayList<String>();
	        folderLocalTaversal(localDir, lsDir, lsFile); 
	        
	        String sDir = localDir.replaceAll("\\\\", "/");
	        if (!sDir.endsWith("/")){
	        	sDir = sDir+"/";
	        }
	        hdDir = hdDir.replaceAll("\\\\", "/");
	        if (hdDir.endsWith("/")){
	        	hdDir = hdDir.substring(0, hdDir.length()-1);
	        }
	        String rDir = null;
	        for(String dir: lsDir){
	        	dir = dir.replaceAll("\\\\", "/");
	        	rDir = dir.replace(sDir, "");
	        	rDir = hdDir + "/"+rDir;
	        	if (!exists(rDir)){
	        		boolean bRet = hdfs.mkdirs(new Path(rDir));
	        		if (!bRet){
	        			Logger.error("create path:" + rDir+ " false.");
	        		}
	        	}
	        }
	        
	        String rFile = null;
	        String sTmp = null;
	        
	        for(String f: lsFile){
	        	sTmp = f.replaceAll("\\\\", "/");
	        	rFile = sTmp.replace(sDir, "");
	        	rFile = hdDir + "/"+rFile;
	        	initFile(rFile);
				OutputStream output = getOutputStream( false );
				InputStream input = new FileInputStream(f);
				//copyByte
			    IOUtils.copyBytes(input,output,1024,true);
	        }
		}catch(Exception e){
			Logger.error(e.getMessage());
		}		
	}
	
	// fs, src, dest
	public void downloadFile( String hdFile, String localFile ) throws Exception {
		try {
			String sFile = new String(localFile);
			if (isLocalDir(localFile)){
				java.io.File f = new java.io.File(hdFile); 
				if (localFile.endsWith("/")){
					sFile += f.getName();
				}else{
					sFile += "/"+f.getName();
				}	
			}
			initFile(hdFile);
			InputStream input = getInputStream();
			OutputStream out = new FileOutputStream(sFile);
			IOUtils.copyBytes(input,out,1024,true);
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
	}

	// fs, src, dest
	public void downloadFiles( String hdDir, String localDir) throws Exception {
		try {
			List<String> lsDir = new ArrayList<String>();
	        List<String> lsFile = new ArrayList<String>();
	        folderHdfsTaversal(hdDir, lsDir, lsFile); 
	        
	        String sDir = hdDir.replaceAll("\\\\", "/");
	        if (!sDir.endsWith("/")){
	        	sDir = sDir+"/";
	        }
	        localDir = localDir.replaceAll("\\\\", "/");
	        if (localDir.endsWith("/")){
	        	localDir = localDir.substring(0, localDir.length()-1);
	        }
	        
	        // 目录列表
	        File fp = null;
	        String lDir = null;
	        for(String dir: lsDir){
	        	dir = dir.replaceAll("\\\\", "/");
	        	lDir = dir.replace(sDir, "");
	        	lDir = localDir + "/"+lDir;
	        	fp = new File(lDir);
	        	if (!fp.exists()){
	        		boolean bRet = fp.mkdirs();
	        		if (!bRet){
	        			Logger.error("create localPath: " + lDir+ " false.");
	        		}
	        	}
	        }
	        
	        // 文件列表
	        String lFile = null;
	        String sTmp = null;
	        for(String f: lsFile){
	        	sTmp = f.replaceAll("\\\\", "/");
	        	lFile = sTmp.replace(sDir, "");
	        	lFile = localDir + "/"+lFile;
	        	initFile(f);
	        	InputStream input = getInputStream();
	        	OutputStream output = new FileOutputStream(lFile);
				//copyByte
			    IOUtils.copyBytes(input,output,1024,true);
	        }
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
	}
		
	private void readFile(String fname ) throws Exception {
		BufferedReader br = null;
		try {
			initFile(fname);
			br = new BufferedReader( new InputStreamReader( getInputStream() ) );
			String tmp = null;
			while( ( tmp = br.readLine() ) != null ) {
				System.out.println( tmp );
			}
		}
		finally {
			br.close();
		}
	}

	public RandomOutputStream getRandomOutputStream(boolean isAppend) {
		return new ImRandomOutputStream(isAppend);
	}
	
	class ImRandomOutputStream extends RandomOutputStream{
		Path path = null;
		FSDataOutputStream fsDataOutputStream = null;
		Object lock = null;
		
		public ImRandomOutputStream(boolean isAppend){
			try{
				lock = new Object();
				path = new Path(fileName);				
				if(hdfs.exists(path) && isAppend)
	            {
		           	 long size = hdfs.getFileStatus(path).getLen();
		           	 fsDataOutputStream = hdfs.append(path, (int)size);
	            }else{
		            //输出流对象，将数据输出到HDFS文件系统
		            fsDataOutputStream = hdfs.create(path);
	            }
			}catch(Exception e){
				Logger.error(e.getMessage());
			}
		}
		
		public void close(){
			try{
				if (fsDataOutputStream != null){
					fsDataOutputStream.close();
				}
			}catch(Exception e){
				
			}
		}
		
		@Override
		public long position() throws IOException {
			// TODO Auto-generated method stub
			long size = fsDataOutputStream.getPos();
			return size;
		}

		@Override
		public void position(long l) throws IOException {
			//由于hdfs不支持随机写，因此不能定位
		}

		@Override
		public boolean tryLock() throws IOException {
			// TODO Auto-generated method stub
			
			return true;
		}

		@Override
		public void write(int i) throws IOException {
			// TODO Auto-generated method stub
			fsDataOutputStream.write(i);
		}
		
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			fsDataOutputStream.write(b, off, len);
		}		
	}
	
	//list all files
    public FileStatus[] listFiles(String dirName) {
        try {
        	String sPath = fileName + "/" + dirName;
        	Path f = new Path(sPath);
        	
			return hdfs.listStatus(f);
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
        
        return null;         
    }
    
    public boolean isRemoveDir( String hdFolder){
    	boolean ret = false;
    	try {
    		File fp = new File(hdFolder);
    		String sParent = fp.getParent();
    		sParent = sParent.replaceAll("\\\\", "/");
    		System.out.println("hdFolder="+hdFolder+"; sParent="+sParent);
    		Path path = new Path(sParent);
			if (!hdfs.exists(path)){
				Logger.warn("path: "+hdFolder+" is not existed.");
				return false;
			}
			FileStatus[] list = hdfs.listStatus(path);
			if (list==null) return false;
			
			for (FileStatus f : list) {	
				String name = f.getPath().getName();
				if (hdFolder.endsWith(name)){
					if (f.isDirectory()){
						ret = true;
					}	
					break;
				}
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}		
		
		return ret;
    }

    public boolean isLocalDir(String hdFolder){
    	boolean ret = false;
    	try {
    		File fp = new File(hdFolder);
    		if (fp.exists()){
    			ret = fp.isDirectory();
    		}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}		
		
		return ret;
    }
    
    //遍历本地目录
    public void folderLocalTaversal(String path, List<String> lsDir, List<String> lsFile) {
        File file = new File(path);
        LinkedList<File> list = new LinkedList<>();

        try{
	        if (file.exists()) {
	            if (null == file.listFiles()) {
	                return;
	            }
	            list.addAll(Arrays.asList(file.listFiles()));
	            while (!list.isEmpty()) {
	            	File fp = list.removeFirst();
	            	if (fp.isDirectory()){
	            		lsDir.add(fp.getPath());
	            	}else if(fp.isFile()){
	            		lsFile.add(fp.getAbsolutePath());
	            	}
	                File[] files = fp.listFiles();
	                if (files==null || files.length==0) {
	                    continue;
	                }
	                for (File f : files) {
	                    if (f.isDirectory()) {
	                    	//System.out.println("dir:" + f.getAbsolutePath());
	                        list.add(f);
	                    } else {
	                        //System.out.println("file:" + f.getAbsolutePath());
	                        lsFile.add(f.getAbsolutePath());
	                    }
	                }
	            }
	        } else {
	            System.out.println("file not existed!");
	        }
	        //System.out.println("文件夹数量:" + lsDir.size() + ",文件数量:" + lsFile.size());
        }catch(Exception e){
        	Logger.error(e.getMessage());
        }
    }
    
    //遍历hdfs目录
    public void folderHdfsTaversal(String hdFile, List<String> lsDir, List<String> lsFile) {
        LinkedList<FileStatus> list = new LinkedList<>();

        try{
	        if (exists(hdFile)) {
	        	URI uri = hdfs.getUri();
	        	String sUrl =  uri.toString();
	        	FileStatus[] ls = hdfs.listStatus(new Path(hdFile));
	            if (null == ls) {
	                return;
	            }
	            String sTmp = null;
	            list.addAll(Arrays.asList(ls));
	            while (!list.isEmpty()) {
	            	FileStatus fp = list.removeFirst();
	            	if (fp.isDirectory()){
	            		sTmp = fp.getPath().toString();
	            		sTmp = sTmp.replace(sUrl, "");
	            		lsDir.add(sTmp);
	            	}else if(fp.isFile()){
	            		sTmp = fp.getPath().toString();
	            		sTmp = sTmp.replace(sUrl, "");
	            		lsFile.add(sTmp);
	            	}
	            	FileStatus[] files = hdfs.listStatus(fp.getPath());
	                if (files==null || files.length == 0) {
	                    continue;
	                }
	                for (FileStatus f : files) {
	                    if (f.isDirectory()) {
	                        //System.out.println("dir:" + f.getPath());
	                        list.add(f);
	                    } else {
	                        sTmp = f.getPath().toString();
		            		sTmp = sTmp.replace(sUrl, "");
		            		//System.out.println("file:" + sTmp);
		            		lsFile.add(sTmp);
	                    }
	                }
	            }
	        } else {
	        	System.out.println("file not existed!");
	        }
	       // System.out.println("文件夹数量:" + lsDir.size() + ",文件数量:" + lsFile.size());
        }catch(Exception e){
        	Logger.error(e.getMessage());
        }
    }
    
    public static void main( String[] args ) {
		try{
			String sUrl = "hdfs://192.168.0.76:9000";
			HdfsClient client = new HdfsClient(new Context(), null, sUrl, "root");
			HdfsFileImpl hfile = new HdfsFileImpl( client.getFileSystem() );
			if (1==12){
				hfile.readFile( "/tmp/lxw1234.txt");
			}else if(1==12){
				hfile.downloadFile( "/tmp/influx.info", "d:/tmp");
			}else if(1==1){
				hfile.downloadFiles( "/user/webmagic", "d:/tmp/data/backup");
				
			}else if(1==12){
				hfile.uploadFile( "d:/tmp/influx.info", "/tmp");
			}else{
				
				hfile.uploadFiles("d:/tmp/data", "/user/webmagic");
			}
		}
		catch( Exception e ) {
			Logger.error(e.getMessage());
		}
	}
   
}
