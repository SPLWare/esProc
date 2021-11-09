package com.raqsoft.lib.extract.function;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

public class PagePipeline extends FilePersistentBase implements Pipeline {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private boolean m_bPostfix = true;
	public PagePipeline() {
		setPath("/data/webcrawl");
	}

	public PagePipeline(String path) {
		setPath(path);
	}
	
	public void setPostfilx(boolean bPostfix){
		m_bPostfix = bPostfix;
	}
	
	//加md5Hex为防止重名
	public void process(ResultItems resultItems, Task task) {
		String saveFile = null;
		Object o = null;
		String spath = "";
		if (path.endsWith(PATH_SEPERATOR)){
			spath = this.path + task.getUUID() + PATH_SEPERATOR;
		}else{
			spath = this.path + PATH_SEPERATOR + task.getUUID() + PATH_SEPERATOR;
		}
		try {
		do{
			boolean mMatch = false;
			String url = resultItems.getRequest().getUrl();
			String hex = DigestUtils.md5Hex(resultItems.getRequest().getUrl());
			
			o = resultItems.get("content");
			if (o == null){
				break;
			}
			Object[] ret = new Object[1];
			mMatch = ImUtils.hasMatch(url, "=((sz|sh|)\\d{6}$)", ret, 1);
			//1. 有股票代码的优先
			if (mMatch){
				if (m_bPostfix){
					saveFile = spath + ret[0].toString()+ "_"+hex+".txt";
				}else{
					saveFile = spath + ret[0].toString()+ ".txt";
				}
				break;
			}
			//2. 网页名
			int start = url.lastIndexOf("/");
    		int end = url.lastIndexOf("?");
    		if (end<0) {
    			end=url.length();
    		}
    		String link = url.substring(start+1, end);
    		//2. 带后缀名的文件的.
    		//mMatch = ImUtils.hasMatch(link, "\\.(xml|json|txt|html|htm|csv|dat|xls|xlsx|zip|rar|tar|tgz|tar\\.gz)");
    		end = link.indexOf(".");
    		if (end>=0){
    			link = link.replace(".", "_");
    		}
    		if (m_bPostfix){
    			saveFile = spath + link+"_"+hex+".txt";
			}else{
				saveFile = spath + link+".txt";
			}			
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
