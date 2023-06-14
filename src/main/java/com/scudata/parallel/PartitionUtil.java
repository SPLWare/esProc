package com.scudata.parallel;

import java.io.*;
import java.util.*;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.Machines;
import com.scudata.dm.RemoteFile;
import com.scudata.dw.BufferReader;
import com.scudata.dw.ComTable;
import com.scudata.resources.EngineMessage;
import com.scudata.resources.ParallelMessage;

public class PartitionUtil {
	static HostManager hm = HostManager.instance();
	static MessageManager pm = ParallelMessage.get();
	
	private static Object ask(String host, int port, Request req) {
		UnitClient uc = null;
		Response res = null;
		try {
			uc = new UnitClient(host, port);
			uc.connect();
			res = uc.send(req);
			if (res.getError() != null) {
				Error e = res.getError();
				throw new RQException("["+uc+"] "+e.getMessage(), e);// req+"  on ["+host+":"+port+"] error.",
			}
			if (res.getException() != null) {
				Exception x = res.getException();
				throw new RQException("["+uc+"] "+x.getMessage(), x);// req+"  on ["+host+":"+port+"] exception.",
			}
			return res.getResult();
		} catch (RQException rq) {
			throw rq;
		} catch (Exception x) {
			throw new RQException(pm.getMessage("PartitionUtil.askerror",host+":"+port),x);
//					"Request to node [" + host + ":" + port
//					+ "] failed.", x);
		} finally {
			if (uc != null) {
				uc.close();
			}
		}
	}

	/**
	 * 从分机序列mcs中找文件file，优先本地，hs大于1只读，否则可读写
	 * @param mcs 分机组
	 * @param file 文件名
	 * @return FileObject
	 */
	public static FileObject locate(Machines mcs, String file) {
		return locate(mcs,file,null);
	}

/**
 * 从分机序列hs中找文件fn，优先本地，hs大于1只读，否则可读写
 * @param mcs 分机组
 * @param file 文件名
 * @param z 分表区号
 * @return FileObject
 */
	public static FileObject locate(Machines mcs, String file, Integer z) {
		if (mcs == null)
			throw new RQException("hosts is null");
		FileObject fo = new FileObject(file);
		fo.setPartition(z);

		if(mcs.size()==1){
			String host = mcs.getHost(0);
			int port = mcs.getPort(0);
			fo = new FileObject(file, host, port);
			fo.setPartition(z);
			fo.setRemoteFileWritable();//仅指定唯一分机时，设置远程文件可写
			return fo;
		}else if (fo.isExists()) {//多个hs时，优先本地
			
			return fo;
		}
		
		HashMap<UnitClient, FileObject> existFileNodes = new HashMap<UnitClient, FileObject>();
		for (int i = 0; i < mcs.size(); i++) {
			String host = mcs.getHost(i);
			int port = mcs.getPort(i);
			try {
				RemoteFile rf = new RemoteFile(host, port, file, z);
				if (rf.exists()) {
					fo = new FileObject(file, host, port);
					fo.setPartition(z);
					existFileNodes.put(new UnitClient(host, port), fo);
				}
			} catch (RQException rx) {
			}
		}
		if (existFileNodes.isEmpty())
//			throw new RQException(pm.getMessage("PartitionUtil.lackfile2",file,partition));
			throw new RQException(file+" is not exists in node machines.");
		int taskCount = 0;
		UnitClient targetUC = null;
		Iterator it = existFileNodes.keySet().iterator();
		while (it.hasNext()) {
			UnitClient uc = (UnitClient) it.next();
			int c = uc.getCurrentTasks();
			if (taskCount == 0 || c < taskCount) {
				taskCount = c;
				targetUC = uc;
			}
		}
		return existFileNodes.get(targetUC);
	}

	/**
	 * 列出分机host的路径path下的所有文件信息
	 * @param host 分机IP
	 * @param port 分机端口号
	 * @param path 路径名称
	 * @return 文件信息的列表
	 */
	public static List<FileInfo> listFiles(String host, int port,String path) {
		Request req = new Request(Request.PARTITION_LISTFILES);
		req.setAttr(Request.LISTFILES_Path, path);
		return (List) ask(host, port, req);
	}

	/**
	 * 将本机文件fn移到hs分机的p路径下，hs可是序列；h省略本机
	 * @param fileName 源文件
	 * @param partition 分区表号
	 * @param hs 目标分机组
	 * @param dstPath 目标路径
	 * @param option 选项
	 */
	public static void moveFile(String fileName,int partition,
			Machines hs, String dstPath, String option) {
		moveFile(null,0,fileName,partition, hs, dstPath, option);
	}
	
	public static boolean isCOption(String option){
		return option!=null && option.indexOf("c")>-1;
	}
	public static boolean isYOption(String option){
		return option!=null && option.indexOf("y")>-1;
	}
	/**
	 * movefile(fn:z,h;p,hs)	将分机h上的文件fn移到hs分机的p路径下，hs可是序列；h省略本机
	 * hs省略为改名
	 * p:hs省略删除
	 * h和p省略但hs不空则删除hs下的文件
	 * @param host 源机IP
	 * @param port 源机端口号
	 * @param fileName 源文件
	 * @param partition 分区表号
	 * @param hs 目标分机组
	 * @param dstPath 目标路径
	 * @param option 选项
	 */
	public static boolean moveFile(String host, int port, String fileName,int partition,
			Machines hs, String dstPath, String option) {
		// 1:如果同步的host不是本地机器，发送消息到host，从host机器执行本地操作。
		if (host != null) {
			boolean isLocal = host.equals(hm.getHost()) && port == hm.getPort();
			if (!isLocal) {
				Request req = new Request(Request.PARTITION_MOVEFILE);
				req.setAttr(Request.MOVEFILE_Machines, hs);
				req.setAttr(Request.MOVEFILE_Filename, fileName);
				req.setAttr(Request.MOVEFILE_Partition, partition);
				req.setAttr(Request.MOVEFILE_DstPath, dstPath);
				req.setAttr(Request.MOVEFILE_Option, option);
				ask(host, port, req);
				return true;
			}
		}
		
		//2:改名或者删除
		String absolute = PartitionManager.getAbsolutePath(fileName);
		File file = new File( absolute );
		
		if (hs == null) {//hs省略时
			if(!file.exists()){//只能在本地操作时，判断该文件是否存在
				throw new RQException( absolute + " is not exist.");
			}
			if(!StringUtils.isValidString(dstPath)){//p也省略时，则删除本地文件
				file.delete();
			}else{
			//改名
				if (file.exists()) {
					File newFile = new File(file.getParent(), dstPath);
					file.renameTo(newFile);
				}
			}
			return true;
		}else if(!StringUtils.isValidString(dstPath)){//p也省略时，则删除hs上的相关文件
			//此处操作的是远程分机上的文件，不能判断file是否存在
			for(int n=0;n<hs.size();n++){
				String tmpHost = hs.getHost(n);
				int tmpPort = hs.getPort(n);
				Request req = new Request(Request.PARTITION_DELETE);
				req.setAttr(Request.DELETE_FileName, fileName);
				req.setAttr(Request.DELETE_Option, option);
				try{
					ask(tmpHost, tmpPort, req);
				}catch(Exception x){//捕获异常以阻止一台分机报错，影响其他分机执行
					Logger.warn(x);
				}
			}
			return true;
		}
		
		if(file.isDirectory()){
			throw new RQException( absolute + " is not a file.");
		}
		if(!file.exists()){
			throw new RQException( absolute + " is not exist.");
		}

		// 4:依次上传到所有分机
		File dstFile = new File(dstPath);
		boolean isDstAbsolute = dstFile.isAbsolute();
		for (int n = 0; n < hs.size(); n++) {
			ArrayList<String> tmpUpFiles = new ArrayList<String>();
			tmpUpFiles.add( absolute );
			String targetPath;
			if( isDstAbsolute ){
				targetPath = new File(dstPath, file.getName()).getAbsolutePath();
			}else{
				targetPath = new File(dstPath, file.getName()).getPath();
			}
			upload(hs.getHost(n), hs.getPort(n), tmpUpFiles, targetPath, true, isYOption(option));
		}
		
		if(!isCOption(option)){//不是复制模式时，最后删除本机文件
			file.delete();
		}
		
		return true;
	}
	
	private static void uploadFile(UnitClient uc, File file,
			String dstPathName, boolean isMove, boolean isY) throws Exception {
//		if (file.isDirectory()) {
//			File[] subFiles = file.listFiles();
//			for (int i = 0; i < subFiles.length; i++) {
//				File subFile = subFiles[i];
//				uploadFile(uc, subFile, dstPathName
//						+ File.separator + subFile.getName());
//			}
//			return;
//		}
		int type = getFileType(file);
		if (type > 0) {
			uploadCtxFile(uc, file, dstPathName);
			return;
		}
		Request req;
//		if(dstPartition==-1){
//			req = new Request(Request.PARTITION_UPLOAD_DFX);
//			req.setAttr(Request.UPLOAD_DFX_RelativePath, dstPathName);
//			req.setAttr(Request.UPLOAD_DFX_LastModified, new Long(file.lastModified()));
//		}else{
			req = new Request(Request.PARTITION_UPLOAD);
			req.setAttr(Request.UPLOAD_DstPath, dstPathName);
			req.setAttr(Request.UPLOAD_LastModified, new Long(file.lastModified()));
			req.setAttr(Request.UPLOAD_IsMove, isMove);
			req.setAttr(Request.UPLOAD_IsY, isY);
//		}

		uc.write(req);
		Response res = (Response) uc.read();
		if (res.getException() != null) {
			throw res.getException();
		}
		
		boolean isNeedUpdate = (Boolean)res.getResult();
		if(!isNeedUpdate){
			return;
		}
		
		FileInputStream fis = new FileInputStream(file);
		byte[] fileBuf = RemoteFileProxyManager.read(fis, Env.FILE_BUFSIZE);
		uc.write(fileBuf);
		while (fileBuf != null) {// Request.EOF) {
			fileBuf = RemoteFileProxyManager.read(fis, Env.FILE_BUFSIZE);
			uc.write(fileBuf);
		}
		fis.close();

		res = (Response) uc.read();
		if (res.getException() != null)
			throw res.getException();

		Logger.debug("upload: " + file.getAbsolutePath() + " OK.");
	}

	// 上载
	/**
	 * 往分机host上上传一个文件或者文件夹
	 * @param host 分机的IP地址
	 * @param port 分机的端口号
	 * @param localFile
	 *            本地文件或者文件夹
	 * @param dstPath
	 *            上载到目标的文件名，null时与localFile同名
	 * @throws Exception
	 */
	public static void upload(String host, int port, List localFiles, String dstPath) {
		upload(host,port,localFiles,dstPath,false,false);
	}
	
	/**
	 * 往分机host上上传一个移动模式的文件，即不比较LastModified值来决定是否移动
	 * @param host 分机的IP地址
	 * @param port 分机的端口号
	 * @param localFile
	 *            本地文件或者文件夹
	 * @param dstPath
	 *            上载到目标的文件名，null时与localFile同名
	 * @param isMove 是否为移动模式的上传文件
	 * @param isY 移动模式时，是否强制覆盖目标文件，否的话，存在目标时，报错
	 */
	public static void upload(String host, int port, List localFiles, String dstPath, boolean isMove, boolean isY) {
		MessageManager mm = EngineMessage.get();

		UnitClient uc = new UnitClient(host, port);
		try {
			uc.connect();
			for (int i = 0; i < localFiles.size(); i++) {
				String localFile = (String) localFiles.get(i);
				File f = new File(localFile);
				if (!f.exists()){
					Logger.warning(mm.getMessage("partitionutil.filenotexist",localFile));
					continue;
				}
				uploadFile(uc, f, dstPath, isMove, isY);
			}
		} catch (Exception x) {
			throw new RQException("["+uc+"] "+x.getMessage(), x);
		} finally {
			if (uc != null) {
				uc.close();
			}
		}
	}


	/**
	 * 将本机路径path下的文件同步到machines
	 * @param machines 目的分机群
	 * @param path 路径名称
	 */
	public static void syncTo(Machines machines, String path) {
		syncTo(null,0,machines,path);
	}
	/**
	 * 将host机器路径p下的文件同步到machines的对应p路径
	 * @param host 同步的机器IP
	 * @param port 同步的机器端口
	 * @param machines 待同步的目的机器群
	 * @param path，要同步的路径名
	 */
	public static void syncTo(String host, int port, Machines machines, String path) {
		MessageManager mm = EngineMessage.get();
		// 0:检查目的分机
		if (machines == null || machines.size() == 0) {
			throw new RQException(mm.getMessage("partitionutil.notarget"));
		}
		if(path==null){
			throw new RQException("Path can not be empty.");
		}
		String absPath = PartitionManager.getAbsolutePath(path);
		File file = new File(absPath);
		if( !file.isDirectory() ){
			throw new RQException( absPath +" is not a directory!");
		}
		
		
		// 1:如果同步的host不是本地机器，发送消息到host，从host机器执行本地操作。
		if (host != null) {
			boolean isLocal = host.equals(hm.getHost()) && port == hm.getPort();
			if (!isLocal) {
				Request req = new Request(Request.PARTITION_SYNCTO);
				req.setAttr(Request.SYNC_Machines, machines);
				req.setAttr(Request.SYNC_Path, path);
				ask(host, port, req);
				return;
			}
		}
		
		// 2:列出目标机器上的所有文件
		List<FileInfo>[] machineFileList = new List[machines.size()];
		for (int i = 0; i < machines.size(); i++) {
			List<FileInfo> fileInfosN = listFiles(machines.getHost(i), machines.getPort(i),path);
			machineFileList[i] = fileInfosN;
		}
		
		// 3:列出本机文件列表
		List<FileInfo> localFiles = PartitionManager.listPathFiles( path, true);
		
		// 4:按本机文件列表，依次更新到所有分机
		for (int i = 0; i < localFiles.size(); i++) {
			FileInfo syncFi = (FileInfo) localFiles.get(i);// 待同步文件
			if(syncFi.isDir()){
				continue;
			}
			for (int n = 0; n < machineFileList.length; n++) {
				List<FileInfo> fileInfosN = machineFileList[n];
				int index = fileInfosN.indexOf(syncFi);
				if (index >= 0) {
					FileInfo fiN = (FileInfo) fileInfosN.get(index);
					if (fiN.lastModified() > syncFi.lastModified())
						continue;// 分机的文件更新一些
				}
				ArrayList<String> tmpUpFiles = new ArrayList<String>();
				File absFile = syncFi.getFile(path);
				tmpUpFiles.add( absFile.getAbsolutePath());
				String dstPath = syncFi.getDestPath(path);
				upload(machines.getHost(n), machines.getPort(n), tmpUpFiles, dstPath);
			}
		}
	}
	

	//上载组表文件
	private static void uploadCtxFile(UnitClient uc, File file, String dstPathName) throws Exception {
		Request req;
		if (!file.exists()) {
			return;
		}
		if (file.getName().indexOf(ComTable.SF_SUFFIX) != -1) {
			return;
		}
		
		ComTable table = ComTable.open(file, null);
		File extFile = ComTable.getSupplementFile(file);
		
		req = new Request(Request.PARTITION_UPLOAD_CTX);
		req.setAttr(Request.UPLOAD_DstPath, dstPathName);
		req.setAttr(Request.UPLOAD_LastModified, Long.valueOf(file.lastModified()));
		req.setAttr(Request.UPLOAD_BlockLinkInfo, table.getBlockLinkInfo());
		req.setAttr(Request.UPLOAD_FileSize, file.length());
		req.setAttr(Request.UPLOAD_FileType, Integer.valueOf(1));
		
		if (extFile.exists()) {
			req.setAttr(Request.UPLOAD_HasExtFile, Boolean.TRUE);
			req.setAttr(Request.UPLOAD_ExtFileLastModified, Long.valueOf(extFile.lastModified()));
		} else {
			req.setAttr(Request.UPLOAD_HasExtFile, Boolean.FALSE);
		}
		uc.write(req);
		
		long [] modifyPositions = table.getModifyPosition();
		long []positions = (long[]) uc.read();
		long remoteFileSize = (Long) uc.read();
		int blockSize = table.getBlockSize();
		byte []buf = new byte[blockSize];
		
		if (remoteFileSize == 0) {
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			try {
				int size;
				raf.seek(0);
				size = raf.read(buf);
				while (size != -1) {
					uc.write(buf);
					size = raf.read(buf);
				}
				uc.write(null);
			} finally {
				raf.close();
				table.close();
			}
			
			if (extFile.exists()) {
				raf = new RandomAccessFile(extFile, "rw");
				try {
					int size;
					raf.seek(0);
					size = raf.read(buf);
					while (size != -1) {
						uc.write(buf);
						size = raf.read(buf);
					}
					uc.write(null);
				} finally {
					raf.close();
				}
			}
			Response res = (Response) uc.read();
			if (res.getException() != null)
				throw res.getException();

			Logger.debug("upload: " + file.getAbsolutePath() + " OK.");
			return;
		}
		
		if (positions != null && positions.length > 0) {
			positions = table.getSyncPosition(positions);
		}
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		
		try {
			if (modifyPositions != null) {
				for (long pos : modifyPositions) {
					if (pos >= remoteFileSize) 
						continue;
					raf.seek(pos);
					raf.read(buf);
					uc.write("m");//表示是补块
					uc.write(pos);
					uc.write(buf);
				}
			}
			
			if (positions != null) {
				for (long pos : positions) {
					if (pos >= remoteFileSize) 
						continue;
					raf.seek(pos);
					raf.read(buf);
					uc.write("n");//表示是正常块
					uc.write(pos);
					uc.write(buf);
				}
			}

			long fileSize = file.length();
			while (remoteFileSize < fileSize) {
				raf.seek(remoteFileSize);
				raf.read(buf);
				uc.write("a");//表示是增量块
				uc.write(remoteFileSize);
				uc.write(buf);
				remoteFileSize += blockSize;
			}
			
			positions = table.getHeaderPosition();
			for (long pos : positions) {
				if (pos >= remoteFileSize) 
					continue;
				raf.seek(pos);
				raf.read(buf);
				uc.write("h");//表示是header块
				uc.write(pos);
				uc.write(buf);
			}
			
			uc.write(null);//end
		} finally {
			table.close();
			raf.close();
		}

		if (extFile.exists()) {
			raf = new RandomAccessFile(extFile, "rw");
			try {
				int size;
				raf.seek(0);
				size = raf.read(buf);
				while (size != -1) {
					uc.write(buf);
					size = raf.read(buf);
				}
				uc.write(null);
			} finally {
				raf.close();
			}
		}
		
		Response res = (Response) uc.read();
		if (res.getException() != null)
			throw res.getException();

		Logger.debug("upload: " + file.getAbsolutePath() + " OK.");
	}
	
	//上载组表索引文件
	private static void uploadIdxFile(UnitClient uc, File file, String dstPathName) throws Exception {
		Request req;
		if (!file.exists()) {
			return;
		}

		FileInputStream fis = new FileInputStream(file);
		byte[] header = RemoteFileProxyManager.read(fis, 1024);
		BufferReader reader = new BufferReader(null, header, 39, 1024);
		long indexPos1, indexPos2, index1EndPos;
		reader.readLong64();
		index1EndPos = reader.readLong64();
		reader.readLong64();
		reader.readLong64();
		reader.readLong64();
		indexPos1 = reader.readLong64();
		indexPos2 = reader.readLong64();
		fis.close();

		long []pos = new long[]{indexPos1, indexPos2, index1EndPos}; 
		req = new Request(Request.PARTITION_UPLOAD_CTX);
//		req.setAttr(Request.UPLOAD_DstPartition, dstPartition);
		req.setAttr(Request.UPLOAD_DstPath, dstPathName);
		req.setAttr(Request.UPLOAD_LastModified, new Long(file.lastModified()));
		req.setAttr(Request.UPLOAD_FileSize, file.length());
		req.setAttr(Request.UPLOAD_BlockLinkInfo, pos);
		req.setAttr(Request.UPLOAD_FileType, new Integer(3));
		uc.write(req);
		
		int syncType = (Integer) uc.read();

		if (syncType == 0) {
			fis = new FileInputStream(file);
			byte[] fileBuf = RemoteFileProxyManager.read(fis, Env.FILE_BUFSIZE);
			uc.write(fileBuf);
			while (fileBuf != null) {// Request.EOF) {
				fileBuf = RemoteFileProxyManager.read(fis, Env.FILE_BUFSIZE);
				uc.write(fileBuf);
			}
			fis.close();
		} else {
			fis = new FileInputStream(file);
			byte[] fileBuf = RemoteFileProxyManager.read(fis, (int) indexPos1);
			uc.write(fileBuf);
			fis.skip(indexPos2 - indexPos1);
			while (fileBuf != null) {// Request.EOF) {
				fileBuf = RemoteFileProxyManager.read(fis, Env.FILE_BUFSIZE);
				uc.write(fileBuf);
			}
			fis.close();
		}

		Response res = (Response) uc.read();
		if (res.getException() != null)
			throw res.getException();

		Logger.debug("upload: " + file.getAbsolutePath() + " OK.");
	}

	private static int getFileType(File file) {
		RandomAccessFile raf = null;
		if (!file.exists()) {
			return 0;
		}
		try {
			raf = new RandomAccessFile(file, "rw");
			raf.seek(0);
			byte []bytes = new byte[32];
			raf.read(bytes);
			if (bytes[0] != 'r' || bytes[1] != 'q' || bytes[2] != 'd' || bytes[3] != 'w') {
				return 0;
			}
			
			if (bytes[4] == 'g' && bytes[5] == 't') {
				if (bytes[6] == 'c') {
					return 1;
				} else if (bytes[6] == 'r') {
					return 2;
				}
			}
//			if (bytes[4] == 'i' && bytes[5] == 'd') {
//				if (bytes[6] == 'x') {
//					return 3;
//				}
//			}
			return 0;
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				raf.close();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}
}