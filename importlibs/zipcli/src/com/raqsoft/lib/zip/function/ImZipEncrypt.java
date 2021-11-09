package com.raqsoft.lib.zip.function;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;

import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;

//设置加密方式
public class ImZipEncrypt extends ImFunction {	
	public Object doQuery(Object[] objs){
		boolean bRet = false;
		try {			
			if (objs.length==1 && objs[0] instanceof Integer){				
				int nMode = Integer.parseInt(objs[0].toString());
				
				EncryptionMethod mod = EncryptionMethod.NONE;
				if (nMode==1) {
					m_parameters.setEncryptFiles(true);
					mod = EncryptionMethod.ZIP_STANDARD;
				}else if(nMode>1 && nMode<5){//2,3,4
					mod = EncryptionMethod.AES;
					AesKeyStrength nKeyLen =  AesKeyStrength.KEY_STRENGTH_128;
					if(nMode==2) nKeyLen = AesKeyStrength.KEY_STRENGTH_128;
					else if(nMode==3) nKeyLen = AesKeyStrength.KEY_STRENGTH_192;
					else if(nMode==4) nKeyLen = AesKeyStrength.KEY_STRENGTH_256;
					m_parameters.setAesKeyStrength(nKeyLen); 
					m_parameters.setEncryptFiles(true);					
				}else{//other
					m_parameters.setEncryptFiles(false);
					mod = EncryptionMethod.NONE;
				}
				m_parameters.setEncryptionMethod(mod); // 加密方式  		
				
				bRet = true;
			}else{
				MessageManager mm = EngineMessage.get();
				throw new RQException("zip " + mm.getMessage("encrypt model error"));
			}					
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		} 
	    
		return bRet;
	}
}
