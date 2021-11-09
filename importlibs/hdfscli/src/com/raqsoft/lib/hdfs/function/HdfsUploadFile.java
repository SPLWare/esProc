package com.raqsoft.lib.hdfs.function;

import com.raqsoft.common.RQException;

/*** @author 
 * hdfs_upload(hd, localFile, removeFile/removePath)
 * removePath时，路径必须存在.
 * 选项d,要求源与目标都是文件夹
 */
public class HdfsUploadFile extends HdfsFunction {
	public Object doQuery( Object[] objs){
		try {
			// System.out.println("upload start....");
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
			//System.out.println("upload start100....");
			if (bDirCopy){
				hfile.uploadFiles(localFile, hdFile);
			}else{
				hfile.uploadFile(localFile, hdFile);
			}
			//System.out.println("upload start200....");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
}
