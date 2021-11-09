package com.raqsoft.lib.zip.function;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;

//向zip追加文件进行压缩处理
public class ImZipAdd extends ImFunction {	
	public Object doQuery(Object[] objs){
		try {
			if (objs.length==1){
				ArrayList<File> rFile=new ArrayList<File>();
				ArrayList<File> rDir=new ArrayList<File>();
				List<String> ls=new ArrayList<String>();
				List<String> filters = ImUtils.getFilter(objs[0]);
				String rootPath = m_zipfile.getFile().getParentFile().getCanonicalPath();
				for(String line:filters){
					if(!ImUtils.isRootPathFile(line)){
						ls.add(rootPath+File.separator+line);
					}else{
						ls.add(line);
					}
				}
				ImUtils.getFiles(ls, rFile, rDir, true);
				
				ImZipUtil.zip(m_zipfile, m_parameters, rFile, rDir);
			}else{
				MessageManager mm = EngineMessage.get();
				throw new RQException("zip" + mm.getMessage("zipadd param error"));
			}
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		} 
	    
		 return true;
	}
}
