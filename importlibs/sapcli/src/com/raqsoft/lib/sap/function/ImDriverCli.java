package com.raqsoft.lib.sap.function;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.IResource;
import com.raqsoft.resources.EngineMessage;

/*
 *  jco_client@f(filename)  
	jco_client@f(filename:charset)
	jco_client@f(FileObject)
*/
public class ImDriverCli implements IResource{
	private RfcManager m_rfcManager;
	
	public ImDriverCli(Context ctx, Object[] params) {
		try {
			if (params.length>=6){
				String user = (String)params[0];
				String passwd= (String)params[1]; 
				String ashost= (String)params[2]; 
				String sysnr= (String)params[3];
				String client= (String)params[4]; 
				String lang= (String)params[5];
				String route="";
				if (params.length==7){
					route= (String)params[6];
				}
				init(user, passwd, ashost, sysnr,client, lang,route);
			}else{
				if (params.length==2){
					m_rfcManager = new RfcManager((String)params[0], (String)params[1]);
				}else if (params[0] instanceof FileObject){
					m_rfcManager = new RfcManager();
					m_rfcManager.init((FileObject)params[0]);
				}else{
					m_rfcManager = new RfcManager((String)params[0], null);
				}				
			}				
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		} finally{
			if (m_rfcManager==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("sap init false" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
	
	public void init(String user, String passwd, String ashost, String sysnr,String client, String lang, String route) {
		try {
			m_rfcManager = new RfcManager(user, passwd, ashost, sysnr,client, lang,route);		
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		} finally{
			if (m_rfcManager==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("sap init false" + mm.getMessage("function.paramTypeError"));
			}
		}
	}

	// 关闭连接释放资源
	public void close() {
		m_rfcManager.close();
	}
	
	public RfcManager getRfcManager(){
		return m_rfcManager;
	}
	
}