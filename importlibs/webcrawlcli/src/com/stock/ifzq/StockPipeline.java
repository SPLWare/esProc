package com.stock.ifzq;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.web.StandPipeline;
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
			if (o instanceof List){
				List<Object> ls = (List<Object>)o;
				if (ls.size()==1){
					o = ls.get(0);
				}
			}
			String stockCode = url.substring(url.indexOf("=")+1, url.indexOf(","));
			//JSONArray r = JSON.parseArray(o.toString());
			//List<Object> ls = new ArrayList<>(Arrays.asList("code", "date", "open", "close","max", "min", "column"));
			//r.add(0, new JSONArray(ls));
			
    		String link = m_argv+"_"+stockCode;
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
