package com.raqsoft.lib.hdfs.function;

import com.raqsoft.common.RQException;

/*** @author 
 * hdfs_exists(hd, dst)
 * dst: removeFile/removePath
 * 选项d,dst:removePath文件夹
 */
public class HdfsExists extends HdfsFunction {
	public Object doQuery( Object[] objs){
		try {
			if(objs==null || objs.length==0 ){
				throw new RQException("upload function.invalidParam");
			}else if (!(objs[0] instanceof String)){
				throw new RQException("exists ParamType should have been String");
			}
			
			boolean bDir = false;
			if (option!=null){
				if( option.contains("d")){
					bDir = true;
				}				
			}

			String hdFile = objs[0].toString();			
			HdfsFileImpl hfile = new HdfsFileImpl(m_hdfs, hdFile);
			
			return hfile.exists();			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
}
