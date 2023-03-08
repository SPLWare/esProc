package com.scudata.lib.hdfs.function;

import com.scudata.common.RQException;
import com.scudata.common.Logger;
/*** @author 
 * hdfs_upload(hd, localFile, remoteFile/remotePath)
 * remotePath时，路径必须存在.
 * 选项d,要求源与目标都是文件夹
 */
public class HdfsUploadFile extends HdfsFunction {
	public Object doQuery( Object[] objs){
		try {
			if(objs==null ){
				throw new RQException("upload function.invalidParam");
			}else if (objs.length!=2){
				throw new RQException("upload ParamSize should have been 2");
			}else if (!(objs[0] instanceof String && objs[1] instanceof String)){
				throw new RQException("upload ParamType should have been String");
			}
			boolean bDirCopy = false;
			
			if (option!=null){
				if( option.contains("d")){
					bDirCopy = true;
				}				
			}
			String localFile = objs[0].toString();
			String hdFile = objs[1].toString();
			
			// for d, p, default option.
			HdfsFileImpl hfile = new HdfsFileImpl(m_hdfs);
			if (bDirCopy){
				hfile.uploadFiles(localFile, hdFile);
			}else{
				hfile.uploadFile(localFile, hdFile);
			}

			return true;
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		return false;
	}
	
}
