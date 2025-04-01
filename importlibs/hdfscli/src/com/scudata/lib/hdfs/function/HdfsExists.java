package com.scudata.lib.hdfs.function;

import com.scudata.common.RQException;
import com.scudata.common.Logger;
/*** @author 
 * hdfs_exists(hd, dst)
 * dst: remoteFile/remotePath
 * 选项d,dst:remotePath文件夹
 */
public class HdfsExists extends HdfsFunction {
	public Object doQuery( Object[] objs){
		try {
			if(objs==null || objs.length==0 ){
				throw new RQException("upload function.invalidParam");
			}else if (!(objs[0] instanceof String)){
				throw new RQException("exists ParamType should have been String");
			}

			String hdFile = objs[0].toString();			
			HdfsFileImpl hfile = new HdfsFileImpl(m_hdfs, hdFile);
			
			return hfile.exists();			
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		return false;
	}
	
}
