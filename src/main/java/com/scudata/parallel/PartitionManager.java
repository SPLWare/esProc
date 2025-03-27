package com.scudata.parallel;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.*;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Env;
import com.scudata.dm.Machines;
import com.scudata.dw.BufferReader;
import com.scudata.dw.ComTable;
import com.scudata.resources.EngineMessage;

public class PartitionManager {
	static HostManager hm = HostManager.instance();
	
	public static Response execute(Request req, SocketData sd) {
		int cmd = req.getAction();
		Response res = new Response();
		String dstPath, path, option;
		Machines machines;
		boolean isY;
		try {
			switch (cmd) {
			case Request.PARTITION_MOVEFILE:
				String fileName = (String) req.getAttr(Request.MOVEFILE_Filename);
				Integer partition = (Integer) req.getAttr(Request.MOVEFILE_Partition);
				machines = (Machines) req.getAttr(Request.MOVEFILE_Machines);
				dstPath = (String) req.getAttr(Request.MOVEFILE_DstPath);
				option = (String) req.getAttr(Request.MOVEFILE_Option);
				PartitionUtil.moveFile(fileName,partition,machines, dstPath, option);
				res.setResult(Boolean.TRUE);
				break;
			case Request.PARTITION_LISTFILES:
				path = (String) req.getAttr(Request.LISTFILES_Path);
				res.setResult(listPathFiles(path, false));
				break;
			case Request.PARTITION_DELETE:
				dstPath = (String) req.getAttr(Request.DELETE_FileName);
				option = (String) req.getAttr(Request.DELETE_Option);
				delete( dstPath, option );
				break;
			case Request.PARTITION_UPLOAD:
				dstPath = (String) req.getAttr(Request.UPLOAD_DstPath);
				long lastModified = ((Number) req
						.getAttr(Request.UPLOAD_LastModified)).longValue();
				boolean isMove = (Boolean)req.getAttr(Request.UPLOAD_IsMove);
				isY = (Boolean)req.getAttr(Request.UPLOAD_IsY);
				if(isMove){
					boolean isNeedMove = isNeedMove(dstPath, isY);
					if(isNeedMove){
						res.setResult(isNeedMove);
						sd.write(res);
						upload( dstPath, sd, lastModified);
						res.setResult(Boolean.TRUE);
					}else{
						res.setException(new Exception("Move file failed for "+dstPath+" is exist, please use @y force move."));
						sd.write(res);
					}
				}else{
					boolean isNeedUpdate = isNeedUpdate(dstPath,lastModified);
					res.setResult(isNeedUpdate);
					if( isNeedUpdate ){
						sd.write(res);
						upload( dstPath, sd, lastModified);
						res.setResult(Boolean.TRUE);
					}
				}
				break;
			case Request.PARTITION_UPLOAD_DFX:
				dstPath = (String) req.getAttr(Request.UPLOAD_DFX_RelativePath);
				long lastModified2 = ((Number) req
						.getAttr(Request.UPLOAD_DFX_LastModified)).longValue();
//				boolean isNeedUpdate2 = isNeedUpdate(-1,dstPath,lastModified2);
//				res.setResult(isNeedUpdate2);
//				if( isNeedUpdate2 ){
//					sd.write(res);
//					upload(-1, dstPath, sd, lastModified2);
//					res.setResult(Boolean.TRUE);
//				}
				break;
			case Request.PARTITION_UPLOAD_CTX:
				//upload 组表
				dstPath = (String) req.getAttr(Request.UPLOAD_DstPath);
				long lastModified3 = ((Number) req.getAttr(Request.UPLOAD_LastModified)).longValue();
				Long fileSize = (Long) req.getAttr(Request.UPLOAD_FileSize);
				long []blockLinkInfo = (long[]) req.getAttr(Request.UPLOAD_BlockLinkInfo);
				Integer fileType = (Integer) req.getAttr(Request.UPLOAD_FileType);
				boolean hasExtFile = (Boolean) req.getAttr(Request.UPLOAD_HasExtFile);
				Long extFileLastModified = (Long) req.getAttr(Request.UPLOAD_ExtFileLastModified);
				if (fileType == 3) 
					upload_IDX(dstPath, sd, lastModified3, blockLinkInfo, fileSize);
				else 
					upload_CTX(dstPath, sd, lastModified3, blockLinkInfo, fileSize, hasExtFile, extFileLastModified);
				res.setResult(Boolean.TRUE);
				break;
			case Request.PARTITION_SYNCTO:
				machines = (Machines) req.getAttr(Request.SYNC_Machines);
				path = (String) req.getAttr(Request.SYNC_Path);
				PartitionUtil.syncTo(machines, path);
				res.setResult(Boolean.TRUE);
				break;
			}
		} catch (Exception x) {
			res.setException(x);
		}
		return res;
	}

/**
 * 列出当前机器上路径path下的文件信息，只能是相对于主路径的相对路径
 * @param path 绝对路径    或者    相对于主路径的相对路径,  path不能为空
 * @param isListSrc 同步时，先列出源机器的文件；然后列出目的机的文件，两相比较，去掉目的机的冗余文件
 * 所以列文件时， 对于源机器，给出的路径没有时，需要报错。而对于目的机，给出的路径没有文件时，返回空。
 * @return 文件信息列表
 */
	public static List<FileInfo> listPathFiles(String path, boolean isListSrc) {
		if( path==null ){
			throw new RQException("Path can not be empty.");
		}
		
		ArrayList<FileInfo> fileInfos = new ArrayList<FileInfo>();
		String parent = getAbsolutePath(path);
		
		File f = new File(parent);
		if (!f.exists()){
			 if( isListSrc ){
				MessageManager mm = EngineMessage.get();
				String node = hm.toString();
				throw new RQException(mm.getMessage("partitionmanager.lackfile",node,f.getAbsolutePath()));
			 }else{
				return fileInfos;
			 }
		}
		
		
		FileInfo fi = new FileInfo(parent, f.isDirectory());
		if(fi.isDir()){
			fi.setDirEmpty(f.listFiles()==null);
		}
		
		fi.setLastModified(f.lastModified());
		fileInfos.add(fi);

		if (!f.isDirectory()) {
			return fileInfos;
		}

		File[] subFiles = f.listFiles();
		if(subFiles==null){
			return fileInfos;
		}
		int beginIndex;
		if(parent.endsWith("/") || parent.endsWith("\\")){
			beginIndex = parent.length();
		}else{
			beginIndex = parent.length() + 1;
		}
		
		for (int i = 0; i < subFiles.length; i++) {
			File tmp = subFiles[i];
			
			String absPath = tmp.getAbsolutePath();
			String tmpPath = absPath.substring(beginIndex);
			fileInfos.addAll(listRelativePathFiles(parent, tmpPath));
		}
		Collections.sort(fileInfos);
//		GM.sort(fileInfos, true);
		return fileInfos;
	}

	/**
	 * 列出root根路径下，相对于root的relativePath下的所有文件信息
	 * @param root 绝对路径的root路径
	 * @param relativePath 相对于root的相对路径
	 * @return 文件信息列表
	 */
	private static List<FileInfo> listRelativePathFiles(String root, String relativePath) {
		ArrayList<FileInfo> fileInfos = new ArrayList<FileInfo>();
		File f;
		if (relativePath == null) {
			f = new File(root);
		} else {
			f = new File(root, relativePath);
			if (!f.exists())
				return fileInfos;
			FileInfo fi = new FileInfo(relativePath, f.isDirectory());
			fi.setLastModified(f.lastModified());
			fileInfos.add(fi);
		}
//		Logger.debug("ListPathFiles: path="+f.getAbsolutePath());

		if (!f.isDirectory()) {
			return fileInfos;
		}

		File[] subFiles = f.listFiles();
		int beginIndex = root.length() + 1;
		for (int i = 0; i < subFiles.length; i++) {
			File tmp = subFiles[i];
			String absPath = tmp.getAbsolutePath();
			String tmpPath = absPath.substring(beginIndex);
			fileInfos.addAll(listRelativePathFiles(root,tmpPath));
		}
		Collections.sort(fileInfos);
//		GM.sort(fileInfos, true);
		return fileInfos;
	}

	private static void delete(String path, String option) {
		String absolute = getAbsolutePath( path );
		File file = new File( absolute );
		if (file.exists()) {
			deleteFile(file, option);
		}else{
			throw new RQException(file+" is not exist.");
		}
	}

	private static boolean deleteFile(File file) {
		return deleteFile(file,null);
	}
	//感觉option没有意义，文件被锁定时， 强制y也没法删除
	private static boolean deleteFile(File file, String option) {
		if (!file.isDirectory()){
			boolean b = file.delete();
			Logger.debug("Delete file:"+file.getAbsolutePath()+" "+ b);
			return b;
		}
		File[] subFiles = file.listFiles();
		for (int i = 0; i < subFiles.length; i++) {
			deleteFile(subFiles[i], option);
		}
		return file.delete();
	}

	static File getTargetFile(String tarPathName,boolean autoRename) {
		File f = new File(tarPathName);
		if (!f.exists()) {
			return f;
		}
		if( !autoRename ){
			f.delete();
			return f;
		}
		int c = 1;
		String p = f.getParent();
		String tmp = f.getName();
		int dot = tmp.lastIndexOf('.');
		String n,ext="";
		if (dot > 0) {
			n = tmp.substring(0, dot);
			ext = tmp.substring(dot + 1);
			if (!ext.isEmpty()) {
				ext = "." + ext;
			}
		}else{
			n = tmp;
		}
		
		File tmpf = new File(p, n + "(" + c + ")" + ext);
		while (tmpf.exists()) {
			c++;
			tmpf = new File(p, n + "(" + c + ")" + ext);
		}
		return tmpf;
	}

	/**
	 * 将相对于主路径的文件名，或者已经是绝对路径的文件名
	 * 转换为本地绝对路径
	 * @param relativeOrAbsolute 相对或绝对的路径名
	 * @return 绝对路径名
	 */
	public static String getAbsolutePath(String relativeOrAbsolute){
		File f = new File(relativeOrAbsolute);
		if(!f.isAbsolute()){
			String main = Env.getMainPath();
			f = new File(main,relativeOrAbsolute);
		}
		return f.getAbsolutePath();
	}
	
	private static boolean isNeedMove( String dstPath,boolean isY){
		String absolutePath = getAbsolutePath( dstPath );
		File f = new File( absolutePath );
		if(!f.exists()){
			return true;
		}
		return isY;//文件存在时，使用y选项强制move
	}
	
	private static boolean isNeedUpdate( String dstPath,long lastModified){
		String absolutePath = getAbsolutePath( dstPath );
		File f = new File( absolutePath );
		if(!f.exists()){
			return true;
		}
		boolean isNeed = f.lastModified()<lastModified;
		return isNeed;
	}
	
	private static void upload(String dstPath,SocketData sd, long lastModified) throws Exception {
		String absolutePath = getAbsolutePath( dstPath );
		File f = new File( absolutePath );

		File uploadFile = getTargetFile(absolutePath,false);
		File p = uploadFile.getParentFile();
		if(!p.exists()){
			p.mkdirs();
		}

		BufferedOutputStream outBuff = null;
		outBuff = new BufferedOutputStream(new FileOutputStream(uploadFile));
		
		try {
			byte[] data = (byte[])sd.read();
			while( data!=null){
				outBuff.write(data);
				data = (byte[])sd.read();
			}
			// 刷新此缓冲的输出流
			outBuff.flush();
		} finally {
			// 关闭流
			if (outBuff != null)
				outBuff.close();
			uploadFile.setLastModified(lastModified);//同步上载文件的修改时间
		}
		Logger.debug("Receive file:"+f.getAbsolutePath()+" OK.");

	}

	/**
	 * 
	 * @param dstPath
	 * @param sd
	 * @param lastModified
	 * @param blockLinkInfo
	 * @param fileSize 远程发送过来的文件大小
	 * @param hasExtFile 有补文件
	 * @throws Exception
	 */
	private static void upload_CTX(String dstPath, SocketData sd, Long lastModified, long[] blockLinkInfo, 
			Long fileSize, Boolean hasExtFile, Long extFileLastModified) throws Exception {
		String absolutePath = getAbsolutePath(dstPath);
		File f = new File(absolutePath);

		File uploadFile = getTargetFile(absolutePath, false);
		File p = uploadFile.getParentFile();
		if(!p.exists()){
			p.mkdirs();
		}

		long uploadFileSize = uploadFile.length();
		if (fileSize < uploadFileSize) {
			//如果节点机本地文件更大，或者更新
			uploadFileSize = 0;
		}
		
		if (uploadFileSize == 0) {
			sd.write(new long[0]);
			sd.write(uploadFileSize);
			RandomAccessFile raf = new RandomAccessFile(uploadFile, "rw");
			raf.setLength(fileSize);
			raf.seek(0);
			try {
				byte []data = (byte[])sd.read();
				while(data != null){
					raf.write(data);
					data = (byte[])sd.read();
				}
				// 刷新此缓冲的输出流
				raf.getChannel().force(false);
			} finally {
				// 关闭流
				if (raf != null)
					raf.close();
				uploadFile.setLastModified(lastModified);//同步上载文件的修改时间
			}
			
			if (hasExtFile) {
				uploadFile = getTargetFile(absolutePath + ComTable.SF_SUFFIX, false);
				raf = new RandomAccessFile(uploadFile, "rw");
				raf.setLength(fileSize);
				raf.seek(0);
				try {
					byte []data = (byte[])sd.read();
					while(data != null){
						raf.write(data);
						data = (byte[])sd.read();
					}
					// 刷新此缓冲的输出流
					raf.getChannel().force(false);
				} finally {
					// 关闭流
					if (raf != null)
						raf.close();
					uploadFile.setLastModified(extFileLastModified);//同步上载文件的修改时间
				}
				Logger.debug("Receive file:"+uploadFile.getAbsolutePath()+" OK.");
			} else {
				uploadFile = getTargetFile(absolutePath + ComTable.SF_SUFFIX, false);
				if (uploadFile.exists()) {
					deleteFile(uploadFile);
				}
			}
			Logger.debug("Receive file:"+f.getAbsolutePath()+" OK.");
			return;
		}
		ComTable table = ComTable.open(uploadFile, null);
		
		long []positions = table.cmpBlockLinkInfo(blockLinkInfo);
		sd.write(positions);
		sd.write(uploadFile.length());
		
		RandomAccessFile raf = new RandomAccessFile(uploadFile, "rw");
		raf.setLength(fileSize);
		
		int blockSize = table.getBlockSize();
		byte[] buf = new byte[blockSize];
		try {
			String type = (String)sd.read();
			while( type != null){
				char ch = type.charAt(0);
				long pos = (Long) sd.read();
				byte []data = (byte[])sd.read();
				raf.seek(pos);
				switch (ch) {
				case 'a':
					raf.write(data);
					break;
				case 'h':
					raf.write(data);
					break;
				case 'm':
					raf.read(buf);
					if (Arrays.equals(buf, data)) {
						break;
					}
					raf.seek(pos);
					raf.write(data);
					break;
				case 'n':
					raf.write(data);
					break;
				}
				type = (String)sd.read();
			}
			// 刷新此缓冲的输出流
			raf.getChannel().force(false);
		} finally {
			// 关闭流
			if (raf != null)
				raf.close();
			table.close();
			uploadFile.setLastModified(lastModified);//同步上载文件的修改时间
		}
		
		if (hasExtFile) {
			uploadFile = getTargetFile(absolutePath + ComTable.SF_SUFFIX, false);
			raf = new RandomAccessFile(uploadFile, "rw");
			raf.setLength(fileSize);
			raf.seek(0);
			try {
				byte []data = (byte[])sd.read();
				while(data != null){
					raf.write(data);
					data = (byte[])sd.read();
				}
				// 刷新此缓冲的输出流
				raf.getChannel().force(false);
			} finally {
				// 关闭流
				if (raf != null)
					raf.close();
				uploadFile.setLastModified(extFileLastModified);//同步上载文件的修改时间
			}
			Logger.debug("Receive file:"+uploadFile.getAbsolutePath()+" OK.");
		} else {
			uploadFile = getTargetFile(absolutePath + ComTable.SF_SUFFIX, false);
			if (uploadFile.exists()) {
				deleteFile(uploadFile);
			}
		}
		Logger.debug("Receive file:"+f.getAbsolutePath()+" OK.");

	}

	private static void upload_IDX(String dstPath, SocketData sd, long lastModified, 
			long[] blockLinkInfo, long fileSize) throws Exception {
		String absolutePath = getAbsolutePath(dstPath);
		File f = new File(absolutePath);

		File uploadFile = getTargetFile(absolutePath, false);
		File p = uploadFile.getParentFile();
		if(!p.exists()){
			p.mkdirs();
		}

		long indexPos1 = 0, indexPos2 = 0, index1EndPos = 0;
		long uploadFileSize = uploadFile.length();
		if (uploadFileSize > 0) {
			//如果文件存在
			FileInputStream fis = new FileInputStream(uploadFile);
			byte[] header = RemoteFileProxyManager.read(fis, 1024);
			BufferReader reader = new BufferReader(null, header, 39, 1024);
			reader.readLong64();
			index1EndPos = reader.readLong64();
			reader.readLong64();
			reader.readLong64();
			reader.readLong64();
			indexPos1 = reader.readLong64();
			indexPos2 = reader.readLong64();
			fis.close();
		}
		
		if (blockLinkInfo[0] != indexPos1 
				|| blockLinkInfo[1] != indexPos2
				|| blockLinkInfo[2] != index1EndPos
				|| fileSize < uploadFileSize//如果分机的文件更大，或者更新
				|| 0 == uploadFileSize) {
			sd.write(new Integer(0));
			
			BufferedOutputStream outBuff = null;
			outBuff = new BufferedOutputStream(new FileOutputStream(uploadFile));
			
			try {
				byte[] data = (byte[])sd.read();
				while (data!=null) {
					outBuff.write(data);
					data = (byte[])sd.read();
				}
				// 刷新此缓冲的输出流
				outBuff.flush();
			} finally {
				// 关闭流
				if (outBuff != null)
					outBuff.close();
				uploadFile.setLastModified(lastModified);//同步上载文件的修改时间
			}
		} else {
			//只同步增量部分
			sd.write(new Integer(1));
			RandomAccessFile raf = new RandomAccessFile(uploadFile, "rw");
			raf.setLength(fileSize);
			
			try {
				byte []header = (byte[])sd.read();//暂存一下header，最后写入
				
				raf.seek(indexPos2);
				byte []data = (byte[])sd.read();
				while(data != null){
					raf.write(data);
					data = (byte[])sd.read();
				}
				
				raf.seek(0);
				raf.write(header);
				
				// 刷新此缓冲的输出流
				raf.getChannel().force(false);
			} finally {
				// 关闭流
				if (raf != null)
					raf.close();
				uploadFile.setLastModified(lastModified);//同步上载文件的修改时间
			}
		}
		Logger.debug("Receive file:" + uploadFile.getAbsolutePath()+" OK.");
	}
}
