package com.scudata.lib.joinquant.function;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
/*
  * 每个用户每天�?个token
 * 
 */
import java.util.HashMap;
import java.util.Map;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImToken extends ImFunction {
	private static String mFile = null;
	private static String mMod = null;
	public Node optimize(Context ctx) {
		return this;
	}

	public Object doQuery(Object[] objs){
		try {
			if (objs.length!=2){
				MessageManager mm = EngineMessage.get();
				throw new RQException("ImToken " + mm.getMessage("add param error"));
			}
			String mod = null;
			String pwd = null;
			if (objs[0] instanceof String){
				mod = objs[0].toString();
			}
			if (objs[1] instanceof String){
				pwd = objs[1].toString();
			}
			
			if (mod==null || pwd == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ImToken " + mm.getMessage("function.missingParam"));
			}
			boolean bRequest = false;
			mFile = ImUtils.getTempTodateFile(mod);
			//System.out.println("tempFile = "+mFile);
			
			File f = new File(mFile);
			if (!f.exists()) {
				f.createNewFile();
				bRequest = true;
			}else if(this.option!=null && option.compareTo("r")==0) {
				bRequest = true;
			}else if (mMod != mod ) {
				String modifyDate = ImUtils.getModifiedTime(mFile);
				SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd");
				Date date = new Date(System.currentTimeMillis());
				String curDate = formatter.format(date);
				if (curDate.compareTo(modifyDate)==0) {
					JQNetWork.mToken = ImUtils.ReadStringOfFile(mFile);
					if (JQNetWork.mToken==null || JQNetWork.mToken.isEmpty()) {
						bRequest = true;
					}
				}else {
					bRequest = true;
				}
			}else if (JQNetWork.mToken!=null){
				return JQNetWork.mToken;
			}
			String ret = "";
			if (bRequest){
				//先获取是否已经存在，若不存在再生�?.
				Map<String, Object> paramMap = new HashMap<>();
				paramMap.put("method", "get_current_token");
				paramMap.put("mob", mod);
				paramMap.put("pwd", pwd);				
				ret = JQNetWork.SentPostBody(JQNetWork.mExecurl, paramMap);
				
				ImUtils.writeString(mFile, ret);
				JQNetWork.mToken = ret;
				System.out.println(ret);
			}else {
				ret = JQNetWork.mToken;
			}
			mMod = mod;
			return ret;
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
}
