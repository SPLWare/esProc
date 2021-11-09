package com.raqsoft.lib.zip.function;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;

import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

//设置压缩参数：压缩方式 与压缩级别 
public class ImZipCompress extends ImFunction {	
	public Object doQuery(Object[] objs){
		boolean bRet = false;
		try {
			if (objs==null || objs.length<1){
				MessageManager mm = EngineMessage.get();
				throw new RQException("zip " + mm.getMessage("compress model error"));
			}
			
			if (objs.length>=1 && objs[0] instanceof Integer ){				
				int nMode = Integer.parseInt(objs[0].toString());
				// 压缩方式  
				if (nMode==0){
					m_parameters.setCompressionMethod(CompressionMethod.STORE); 
				}else if(nMode==1){
					m_parameters.setCompressionMethod(CompressionMethod.DEFLATE); 
				}else if(nMode==2){
					m_parameters.setCompressionMethod(CompressionMethod.AES_INTERNAL_ONLY); 
				}else{
					m_parameters.setCompressionMethod(CompressionMethod.DEFLATE); 
				}
			}
			
			if (objs.length>=2 && objs[1] instanceof Integer ){	
				int nLevel = Integer.parseInt(objs[1].toString());
				// 压缩级别  
				if (nLevel==1){
					m_parameters.setCompressionLevel(CompressionLevel.FASTER);
				}else if(nLevel==2){
					m_parameters.setCompressionLevel(CompressionLevel.FAST); 
				}else if(nLevel==3){
					m_parameters.setCompressionLevel(CompressionLevel.NORMAL); 
				}else if(nLevel==4){
					m_parameters.setCompressionLevel(CompressionLevel.MAXIMUM); 
				}else if(nLevel==5){
					m_parameters.setCompressionLevel(CompressionLevel.ULTRA); 
				}else {
					m_parameters.setCompressionLevel(CompressionLevel.NORMAL); 
				}
				bRet = true;
			}else{
				m_parameters.setCompressionLevel(CompressionLevel.NORMAL);
			}
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		} 
	    
		 return bRet;
	}
}
