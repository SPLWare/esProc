package com.web;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.utils.FilePersistentBase;
import org.apache.commons.codec.digest.DigestUtils;

public class StockPipeline extends FilePersistentBase implements StandPipeline {
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
	
	public StockPipeline() {
		m_path = "/data/webmagic";
	}
	
	public void setArgv(String argv) {
		m_argv = argv;
		if (m_argv.indexOf("save_path=")>=0){
			String[] ss = m_argv.split(", ");
			m_path = ss[0].replace("save_path=", "");
			m_argv = ss[1];
		}
	}
		
	//加md5Hex为防止重名
	public void process(ResultItems resultItems, Task task) {
		String saveFile = null;
		Object o = null;
		String spath = this.m_path + PATH_SEPERATOR + task.getUUID() + PATH_SEPERATOR;
		try {
		do{
			String url = resultItems.getRequest().getUrl();
			o = resultItems.get("content");
			if (o == null){
				break;
			}

			int start = url.lastIndexOf("/");
    		int end = url.lastIndexOf("?");
			if (end<0){
				end=url.length();
			}
	
    		String link = url.substring(start+1, end);
    		if (m_argv!=null && !m_argv.isEmpty()){
    			link = m_argv+"_"+link;
    		}

    		if (link.indexOf(".")>=0){
    			link = link.replace(".", "_");
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
