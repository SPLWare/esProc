package com.scudata.expression.fn;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scudata.common.*;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import org.mozilla.universalchardet.UniversalDetector;

/***************************************
 * 
 * chardetect@v(p)
 * 参数p，分为字符串，二进制值；URL；文件型
 * 返回值：单个编码值时为String, 否则为序列
 * 
 * */

public class CharDetect extends CharFunction {
	List<String> m_codes = new ArrayList<String>();
	protected Object doQuery(Object[] objs) {
		List<String> result = null; 
		try {
			m_codes.clear();
			if (objs==null || objs.length<1){
				throw new Exception("chardet paramSize error!");
			}
			
			if (objs.length>=2){
				if (objs[1] instanceof Sequence){
					Sequence seq = (Sequence)objs[1];
					for(int i=1; i<=seq.length(); i++){
						m_codes.add(seq.get(i).toString().toUpperCase());
					}
				}
			}
			
			// check encoding for string
			if(option!=null && option.contains("v")){
				byte[] buf = null;
				CharEncodingDetect detector = new CharEncodingDetect();
				if(objs[0] instanceof String){					
					String str = objs[0].toString();
					buf = str.getBytes();
					result = detector.autoDetectEncoding(buf);					
				}else if(objs[0] instanceof byte[]){
					buf = (byte[])objs[0];
					result = detector.autoDetectEncoding(buf);
				}	
				
				if (result==null){
					String encoding = CharEncodingDetectEx.getJavaEncode(buf);
					result = new ArrayList<String>();
					result.add(encoding);
				}

				List<String> rep = new ArrayList<String>();
				if (m_codes.size()>0){						
					for(String item:result){
						//System.out.println("val = "+item);
						if (m_codes.indexOf(item.toUpperCase())>-1){
							rep.add(item);
						}
					}
					if (rep.size()>0){
						return new Sequence(rep.toArray(new String[rep.size()]));
					}
				}else{
					return new Sequence(result.toArray(new String[result.size()]));
				}				
			}else if(objs[0] instanceof String){ 
				String sTmp = objs[0].toString();				
				String reg = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
				if (isMatch(sTmp, reg)){ // for url
					return detectEncoding(new URL(sTmp));
				}else{ // for file
					return detectCharsetFile(sTmp);
				}
			}else if(objs[0] instanceof FileObject){
				FileObject fo = (FileObject)objs[0];
				
				return detectCharsetFile(fo.getFileName());
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return null;
	}
	
	private Object detectCharsetFile(String sfile) throws IOException
	{
		File file = new File(sfile);
		if (file.exists()){						
			return getFileCharset(file);
		}
		String fullFile = null;
		
		// 1. 用户设置的main
		String path = Env.getMainPath();
		if (path!=null){
			fullFile = path+File.separatorChar+sfile;
			file = new File(fullFile);
			if (file.exists()){	
				return getFileCharset(file);
			}
		}
		
		// 2. 系统自带的main
		path = System.getProperty("start.home");
		fullFile = path+File.separatorChar+"main"+File.separatorChar+sfile;
		file = new File(fullFile);
		if (file.exists()){	
			return getFileCharset(file);
		}
		// 3. 系统自带的demo
		fullFile = path+File.separatorChar+"demo"+File.separatorChar+sfile;
		file = new File(fullFile);
		if (file.exists()){	
			return getFileCharset(file);
		}else{
			Logger.info("File: "+ sfile +" not existed.");
		}
		
		return null;
	}
		
    private String detectEncoding(InputStream stream, Object obj) throws IOException {
        UniversalDetector detector = new UniversalDetector(null);

        int read;
        byte[] buffer = new byte[4096];
        while ((read = stream.read(buffer)) > 0 && !detector.isDone()) {
            detector.handleData(buffer, 0, read);
        }
        detector.dataEnd();

        String encoding = detector.getDetectedCharset();
        detector.reset();
        
	    if (encoding ==null){
	    	encoding = CharEncodingDetectEx.getJavaEncode(obj);
	    }else{
	    	BytesEncodingDetect s = new BytesEncodingDetect(); 
	    	List<String> list= Arrays.asList(EncodingEx.javaname);
    		if(list.indexOf(encoding)==-1){
    			encoding = CharEncodingDetectEx.getJavaEncode(obj);
    		}	    	
	    }
	    
	    if (m_codes!=null){	
	    	if (m_codes.indexOf(encoding)!=-1){
	    		return encoding;
	    	}else{
	    		return null;
	    	}
	    }
        return encoding;
    }
    
    private String detectEncoding(URL url) {
		InputStream istream;
		try {
			istream = url.openStream();
			String code = detectEncoding(istream, url);
			istream.close();
			return code;			
		} catch (Exception e) {
			Logger.error("Error loading or using URL " + e.toString());			
		}
		
		return null;
	}
	
	// 通过Url获取主机名，port, warehouse
	private boolean isMatch(String strUrl, String regExp)
	{
		if (strUrl==null || strUrl.isEmpty()){
			throw new RQException("spark isMatch strUrl is empty");
		}
		
		if (regExp==null || regExp.isEmpty()){
			throw new RQException("spark isMatch regExp is empty");
		}
		
		Pattern p=Pattern.compile(regExp);
		Matcher m = p.matcher(strUrl);
		
		return m.matches();
	}
	
	private String getFileCharset(File file) throws IOException {
		FileInputStream ins = new FileInputStream(file);
	    String code = detectEncoding(ins, file);
	    ins.close();
	    
	    return code;
	}
}
