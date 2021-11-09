package com.raqsoft.lib.extract.function;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Sequence;
import com.raqsoft.resources.EngineMessage;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.scheduler.QueueScheduler;

public class ImWebCrawl extends ImFunction {

	public Object doQuery(Object[] objs){
		try {
		do{
			if (objs==null || objs.length!=1 ){
				MessageManager mm = EngineMessage.get();
				throw new RQException("crawl " + mm.getMessage(" param error"));
			}
						
			if (objs[0] instanceof String ){
				String sVal = doPreviewParam(objs[0].toString());
				
				loadProcessor(sVal);
			}else if(objs[0] instanceof Sequence){
				StringBuffer buf = new StringBuffer("[");
				Sequence sq= (Sequence)objs[0];
				for(int i=1; i<=sq.length(); i++){
					Object o = sq.get(i);
					buf.append("," + o);
				}
				buf.append("]");
				String sVal = doPreviewParam(buf.toString());
				sVal = sVal.replaceFirst("\\[,", "\\[");

				loadProcessor(sVal);
			}
			
			return true;
		}while(false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.getMessage();
		} 
	    
		return false;
	}
	
	private String doPreviewParam(String p){
		String sVal = p;
		//sVal = sVal.replace("\"", "");
		if (sVal.charAt(0)=='"'){
			sVal = sVal.substring(1, sVal.length()-1);
		}
		sVal = sVal.replace("\n", "");
		sVal = sVal.replace("\r", "");
		return sVal;
	}
	
	private void loadProcessor(String param){
		//System.out.println(param);
		UrlsProcessor cls = new UrlsProcessor(param);
		int thread_size = 4;
		String pipeLine = "";
    	String pipeArgv = "";
    	String output = "d:/tmp/data/webcrawl/sina";
    	Map<String, Object> m = cls.getObjMap();
    	if (m.containsKey("thread_size")){
    		thread_size = Integer.parseInt(m.get("thread_size").toString());
    	}
    	
    	if (m.containsKey("save_path")){
    		output = m.get("save_path").toString();
    	}else if (ImUtils.getOsSystem().equals("win")){
			File f = new File(this.getClass().getResource("").getPath());
			if (f.getPath().startsWith("file:")){
				String path = f.getPath();
				path = path.substring(path.indexOf(":")+1, path.indexOf("!"));
				int end = path.lastIndexOf(File.separator);
				output = path.substring(0, end);
			}else{
				output = System.getProperty("user.dir");
			}
		}else{
			output = "/data/webcrawl";
		}
    	
    	Pipeline clsPipeline = null;
    	if (m.containsKey("class_name")){
    		pipeLine = m.get("class_name").toString();
    	
	    	if (m.containsKey("class_argv")){
	    		pipeArgv = m.get("class_argv").toString();
	    	}
	    	if (!output.isEmpty()){
	    		pipeArgv = "save_path="+output+", "+pipeArgv;
	    	}
	    	clsPipeline = cls.pipelineLoader(pipeLine, pipeArgv);
    	}else{
    		clsPipeline = new PagePipeline(output);
    		if (m.containsKey("save_post")){
	    		if (m.get("save_post").toString().equalsIgnoreCase("true")){
	    			((PagePipeline)clsPipeline).setPostfilx(true);
	    		}else{
	    			((PagePipeline)clsPipeline).setPostfilx(false);
	    		}
	    	}	    		
    	}
    	//获取初始化的url.
    	List<String> urls = new ArrayList<String>();
    	for(String k:m.keySet()){
    		if (k.startsWith("init_url_")){
    			String val = m.get(k).toString();
    			urls.add(val);
    		}
    	}
    	
    	if (urls.size()==1){
	        Spider spider = Spider.create(cls)
	        		.addUrl(urls.get(0))
	        		.addPipeline(clsPipeline);
	        spider.thread(4).run();
    	}else if(urls.size()>1){
    		QueueScheduler scheduler = new QueueScheduler();
    		Spider spider = Spider.create(cls)
	        		.scheduler(scheduler)
	        		.addUrl(urls.get(0))
	        		.addPipeline(clsPipeline);
    		
    		for(int i=1; i<urls.size(); i++){					
	            Request request = new Request();
	            
	            request.setUrl(urls.get(i));
	            scheduler.push(request, spider);
	        }
    		spider.thread(thread_size).run();
    	}
	}
}
