package com.web;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;

public class StockFilePipeline2 implements StandPipeline {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private String m_argv;
	private String m_path;
	
	public static String PATH_SEPERATOR = "/";

	static {
	  String property = System.getProperties().getProperty("file.separator");
	  if (property != null) {
	    PATH_SEPERATOR = property;
	  }
	}
	
	public StockFilePipeline2() {
		setPath("/data/webmagic");
	}

	public StockFilePipeline2(String path) {
		setPath(path);
	}
	
	public void setArgv(String argv) {
		m_argv = argv;
	}
	
	public void setPath(String path) {
		  if (!path.endsWith(PATH_SEPERATOR)) {
		    path = path + PATH_SEPERATOR;
		  }
		  this.m_path = path;
		}

		public File getFile(String fullName) {
		  checkAndMakeParentDirecotry(fullName);
		  return new File(fullName);
		}

		public void checkAndMakeParentDirecotry(String fullName) {
		  int index = fullName.lastIndexOf(PATH_SEPERATOR);
		  if (index > 0) {
		    String path = fullName.substring(0, index);
		    File file = new File(path);
		    if (!file.exists()) {
		      file.mkdirs();
		    }
		  }
		}
	
	public boolean hasMatch(String url, String regex) {      
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        return matcher.find();
	}
	
	public boolean hasMatch(String url, String regex, Object[] ret, int idx) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(url);
		boolean bret = matcher.find();
		if (bret) {
			ret[0] = matcher.group(idx);
		}
		
		return bret;
	}
	
	//加md5Hex为防止重名
	public void process(ResultItems resultItems, Task task) {
		String saveFile = null;
		Object o = null;
		String spath = this.m_path + PATH_SEPERATOR + task.getUUID() + PATH_SEPERATOR;
		try {
		do{
			boolean mMatch = false;
			String url = resultItems.getRequest().getUrl();
			
			o = resultItems.get("content");
			if (o == null){
				break;
			}
			Object[] ret = new Object[1];
			mMatch = hasMatch(url, "=((sz|sh|)\\d{6}$)", ret, 1);
			//1. 有股票代码的优先
			if (mMatch){
				saveFile = spath + ret[0].toString()+ "_"+DigestUtils.md5Hex(resultItems.getRequest().getUrl())+".txt"; 
				break;
			}
			//2. 网页名
			int start = url.lastIndexOf("/");
    		int end = url.lastIndexOf("html");
    		if (end>0) {
    			end+=4;
    		} else{
    			end = url.lastIndexOf("?");
    			if (end<0){
    				end=url.length();
    			}
    		}
    		String link = url.substring(start+1, end);
    		//2. 带后缀名的文件的.
    		mMatch = hasMatch(link, "\\.(xml|json|txt|html|htm|csv|dat|xls|xlsx|zip|rar|tar|tgz|tar\\.gz)");
    		if (mMatch){
    			saveFile = spath + link;
    			break;
    		}
    		
			saveFile = spath + link+"_"+DigestUtils.md5Hex(resultItems.getRequest().getUrl())+".json";
		}while(false);
		if (saveFile!=null){
			PrintWriter printWriter = new PrintWriter(new FileWriter(getFile(saveFile )));
			printWriter.write(o.toString());
			printWriter.close();
		}
		} catch (IOException e) {
			logger.warn("write file error", e);
		}
	}
}
