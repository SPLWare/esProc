package com.scudata.lib.chardet.function;

import com.scudata.common.*;

// encode(str, encodeName) 
// return byte[]
public class ImEncode extends ImFunction {
	
	protected Object doQuery(Object[] objs) {

		try {
			if (objs==null || objs.length<1){
				throw new Exception("chardet paramSize error!");
			}
			
			String encodeNames = "GBK";
			if (objs.length>=2 && objs[1] instanceof String){
				encodeNames = objs[1].toString();
			}
			
			if(objs[0] instanceof String ){
				String str = objs[0].toString();
				//str = new String(str.getBytes("gbk"),"UTF-16");	
//				encodeNames = "UTF-8";
//				encodeNames = "UTF-16";
//				encodeNames = "Unicode";
//				encodeNames = "GBK";
//				encodeNames = "ISO2022CN_GB";
				byte[] rawbyte = str.getBytes(encodeNames);
//				System.out.println(str+"\n"+rawbyte);
//				BytesEncodingDetect.toHex(encodeNames, rawbyte);
				
				return rawbyte;
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return null;
	}
	
}
