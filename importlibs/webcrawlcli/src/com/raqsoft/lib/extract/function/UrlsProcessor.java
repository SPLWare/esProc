package com.raqsoft.lib.extract.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.web.StandPageItem;
import com.web.StandPipeline;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.QueueScheduler;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.JsonPathSelector;
import us.codecraft.webmagic.selector.Selectable;

public class UrlsProcessor implements PageProcessor {
	private JSONArray m_json;
	private Map<String, Object> m_map_of_object;	//遍历的url规则
	
	private Site site = Site.me().setDomain("raqsoft.com").setSleepTime(2000)
			.setUserAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0");
	
	public UrlsProcessor(String s){
		m_map_of_object=new HashMap<String, Object>();
		init(s);
	}
	
	private void init(String s){
		try{
			 String ss = s.replace("\\\"","##");
			 ss = ss.replace("\\","#");
			 ss = ss.replace("##","\"");
			// System.out.println(ss);
			 JSONArray r = JSON.parseArray(ss);
			 m_json = r;
			 String key = "web_info";
			 Map<String, Object> m = ImUtils.getJsonMap(r, key);
			 
			 key = "init_url";
			 m_map_of_object = ImUtils.getJsonMap(r, key);
			 //System.out.println(key+"=="+m.size());
			 if (m.size()>0){
				 Map<String, String> subObj = (Map<String, String>)m.get("web_info_1");
				 for(Map.Entry<String ,String> line: subObj.entrySet()){
					 m_map_of_object.put(line.getKey(), line.getValue());
				 }
				 if (subObj.containsKey("domain")){
					 site.setDomain(subObj.get("domain"));
				 }
				 
				 if (subObj.containsKey("user_agent")){
					 site.setUserAgent((String)subObj.get("user_agent"));
				 }
				 if (subObj.containsKey("sleep_time")){
					 site.setSleepTime(Integer.parseInt(subObj.get("sleep_time")));
				 }
				 if (subObj.containsKey("cycle_retry_times")){
					 site.setCycleRetryTimes(Integer.parseInt(subObj.get("cycle_retry_times")));
				 }
				 if (subObj.containsKey("charset")){
					 site.setCharset(subObj.get("charset"));
				 }
				 if (subObj.containsKey("use_gzip")){
					 site.setUseGzip(subObj.get("use_gzip").equalsIgnoreCase("true")?true:false);
				 }
				 if (subObj.containsKey("time_out")){
					 site.setTimeOut(Integer.parseInt(subObj.get("time_out")));
				 }
				 if (subObj.containsKey("cookie")){
					 for(String k : subObj.keySet()){
						 if (k.startsWith("cookie_")){
							 String nk = k.replace("cookie_", "");
							 site.addCookie(nk,subObj.get(k));
						 }
					 }
				 }
			 }
			 
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
    public Site getSite() {
        return site;
    }
	
	public Map<String, Object> getObjMap(){
		return m_map_of_object;
	}
		
    @Override
    public void process(Page page) {
    	String url = page.getUrl().toString();    	
    	//System.out.println("res = "+url);
    	//1. HelpUrl
    	String key = "help_url";
    	m_map_of_object = ImUtils.getJsonMap(m_json, key);
    	
    	if (ImUtils.hasValueOfMap(url, m_map_of_object)) {
    		System.out.println("helpUrl = "+url);
    		parseHelpUrls(page, m_map_of_object);    	
    	}
    	//2. 收集targetUrl
    	key = "target_url";
    	List<Object> regContent= new ArrayList<Object>();
    	m_map_of_object = ImUtils.getJsonMap(m_json, key);
    	if(ImUtils.hasKeyMap(url, m_map_of_object, regContent)){
    		parseTargetUrls(page, regContent);
    	}
    	// 3. 保存target页面数据.
    	if(page.getRequest().getExtra("target_page")!=null){
    		page.getRequest().putExtra("target_page", null);
    		System.out.println("TargetUrl = "+url);
    		
    		regContent.clear();
        	key = "page_url";
        	m_map_of_object = ImUtils.getJsonMap(m_json, key);
    		ImUtils.hasKeyMap(url, m_map_of_object, regContent);
    		savePage(page, regContent);        	
    	}else if(m_map_of_object.size()==0){
    		regContent.clear();
        	key = "page_url";
        	m_map_of_object = ImUtils.getJsonMap(m_json, key);
    		ImUtils.hasKeyMap(url, m_map_of_object, regContent);
    		savePage(page, regContent);  
    	}
    }

	private void savePage(Page page, List<Object> mReg){
		if (mReg.size()<1){
			String content = page.getHtml().toString();
			if(content!=null){
				page.putField("content", content);
			}
			return;
		}
		
		for (Object line : mReg) {
			Map<String, String> sObj = (Map<String, String>) line;
			String reg = sObj.get("extractby");
			String rawText = page.getRawText();
			List<String> content = null;
			if (ImUtils.isJSONValid(rawText)){
				content = new JsonPathSelector(reg).selectList(rawText);
			}else{
				content = page.getHtml().xpath(reg).all();
			}
			
			if(!content.isEmpty()){
				if (sObj.containsKey("class")){
					Html html = new Html(content.toString());
					page.setHtml(html);
					String cls = sObj.get("class").toString();
					if (cls.equalsIgnoreCase("default")){ //1. 缺省采集??
						catchTable(page);
					}else{								  //2. 自定义采集器
						loader(page, cls);
					}	    				
				}else{ 									  //3. 不用采集??		
					page.putField("content", content);
				}
				break;
			}
		}
	}
    
    private void loader(Page p, String clasName){
    	Class<?> b = null;
    	try {
	    	b = Class.forName(clasName);
	    	StandPageItem stand = (StandPageItem)b.newInstance();
	    	stand.parse(p);
    	} catch (Exception e) {
	    	e.printStackTrace();
    	}
    }
    
    public StandPipeline pipelineLoader(String clasName,String argv){
    	StandPipeline stand = null;
    	try {
    		Class<?> b = Class.forName(clasName);
	    	stand = (StandPipeline)b.newInstance();
	    	stand.setArgv(argv);
    	} catch (Exception e) {
	    	e.printStackTrace();
    	}
    	
    	return stand;
    }
    
	private void catchTable(Page page) {
		StringBuilder buf = new StringBuilder();
		List<Selectable> nodes = page.getHtml().xpath("table/tbody/").nodes();
		for (Selectable node : nodes) {
			//String day = node.xpath("//a/text()").get();
			List<String> title = node.xpath("//a/text() | tr/td/text() ").all();
			if (title.size() < 5)
				continue;
			String line = title.toString().replaceFirst(", , ", ", ");
			line = line.replaceFirst("\\[", "");
			line = line.replaceFirst("\\]", "");

			// System.out.println("title: "+title);
			buf.append(line + "\n");
		}
		page.putField("content", buf.toString());
	}
    
    private void parseHelpUrls(Page page, Map<String, Object> mReg){
    	Matcher matcher = null;
    	List<Object> selectLinks = new ArrayList<Object>();
    	List<String> links = page.getHtml().links().all();
    	
    	for (Object regUrl : mReg.values()){	    	
	    	Pattern pattern = Pattern.compile(regUrl.toString());
	    	//过滤url
	    	for(String link:links){
	    		matcher = pattern.matcher(link);
	    		if (matcher.find()){
	    			int end = link.lastIndexOf("#");
	    			if (end>-1){
	    				link = link.substring(0, end);
	    			}
	    			selectLinks.add(link);
	    		}
	    	}
    	}
    	//url去重处理.
    	LinkedHashSet<Object> hashSet = new LinkedHashSet<>(selectLinks);
    	selectLinks = new ArrayList<>(hashSet);
    	
    	for(Object link:selectLinks){
            page.addTargetRequest(new Request(link.toString()).setPriority(2)
					.putExtra("url_list", link));
    	}

    }
    //http://www.aigaogao.com/tools/history.html?s=
	private void parseTargetUrls(Page page, List<Object> mReg) {
		Matcher matcher = null;
		List<Object> selectLinks = new ArrayList<Object>();
		
		// regUrls
		String regUrl = "";
		for (Object line : mReg) {
			Map<String, String> sObj = (Map<String, String>) line;
			regUrl = sObj.get("reg_url");
			
			if (sObj.containsKey("new_url")) {
				String newurl = ((String) sObj.get("new_url"));
			
				String sNew = "";
				List<String> links2 = page.getHtml().links().regex(regUrl).all();
				for (String link : links2) {
					sNew = String.format(newurl, link);
					selectLinks.add(sNew);
				}
			} else {
				Pattern pattern = Pattern.compile(regUrl);
				List<String> links = page.getHtml().links().all();
				// 过滤url
				for (String link : links) {
					matcher = pattern.matcher(link);
					if (matcher.find()) {
						int end = link.lastIndexOf("#");
						if (end > -1) {
							link = link.substring(0, end);
						}
						selectLinks.add(link);
					}
				}
			}
		}
		// url去重处理.
		LinkedHashSet<Object> hashSet = new LinkedHashSet<>(selectLinks);
    	selectLinks = new ArrayList<>(hashSet);
		for (Object link : selectLinks) {
			page.addTargetRequest(new Request(link.toString()).setPriority(2).putExtra("target_page", true));
		}
	}
    
    public static void main(String[] args) {
    	UrlsProcessor cls = null;
    	String str = "https://www.banban.cn/gupiao/list_cyb.html";
    	{
			//说明??. init_url, help_url返回为list;
			//		2. target_url, page_url返回为k:v
			String s = "[{web_info:{domain:\"www.banban.cn\", save_path:\"d:/tmp/data/webcrawl\",save_post:\"true\", thread_size:2,"+
						"user_agent:\"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0\","+
						"pipe_line:\"com.web.StockPipeline\",pipe_argv:\"d:/tmp/data/webcrawl/test\"}},"+
				 	"{init_url:[\"https://www.banban.cn/gupiao/list_cybs.html\", \"https://www.banban.cn/gupiao/list_sh.html\"]},"+
				    "{help_url:[\"gupiao/list_(sh|sz|cyb)\\.html\", \"/shujv/zhangting/\", \"/agu/$\"]},"+
				    "{target_url:{reg_url:'/agu/365\\d'}},"+
				    "{target_url:{filter:\"gupiao/list_(sh|sz|cyb)\\.html\", reg_url:'gupiao/[sz|sh]?(60000\\d)/',new_url:\"http://www.aigaogao.com/tools/history.html?s=%s\"}},"+
    				"{page_url:{filter:\"history.html\\?s=\\d{6}\", extractby:\"//div[@id='ctl16_contentdiv']/\","+
    				     "class:\"com.web.StockHistoryData\"}},"+
				    "{page_url:{filter:\"history.html\\?s=[sz|sh]?\\d{6}\", extractby:\"//div[@id='contentdiv']/\"}},"+
				    "{page_url:{extractby:\"//div[@id='content_all']/\"}},"+
				    "{page_url:{filter:\"/agu/365\\d\", extractby:\"//div[@id='content']/\"}}]";
			 s = "[{init_url:[\"http://www.aigaogao.com/tools/history.html?s=600000\"]}]";
			 s = "[{web_info:{domain:'www.banban.cn', save_path:'d:/tmp/data/webmagic', thread_size:2,"+
				 "cookie:{name:'jacker', laster:'2011'},user_agent:'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:39.0) "+
				 "Gecko/20100101 Firefox/39.0',class_name:'com.web.StockPipeline',class_argv:'stock'}},"+
				 "{init_url:[ 'https://www.banban.cn/gupiao/list_sh.html']},"+
				 "{help_url:['gupiao/list_(sh|sz|cyb)\\.html', '/shujv/zhangting/', '/agu/$']},{target_url:{reg_url:'/agu/365\\d'}},"
				 + "{target_url:{filter:'gupiao/list_(sh|sz|cyb)\\.html', reg_url:'gupiao/[sz|sh]?(60000\\d)/',"
				 + "new_url:'https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param=sh%s,day,2021-8-1,2021-8-5,500,qfq'}},"
				 + "{page_url:{filter:'history.html\\?s=\\d{6}', extractby: \"//div[@id='ctl16_contentdiv']/\",class:'com.web.StockHistoryData'}},"
				 + "{page_url:{extractby: \"$.data\"}},"
				 + "{page_url:{filter:'/agu/365\\d', extractby: \"//div[@id='content']/\"}}]";
			cls = new UrlsProcessor(s);
    		
    		int thread_size = 1;
	    	String output = "d:/tmp/data/webcrawl";
	    	String pipeLine = "";
	    	String pipeArgv = "";
	    	Map<String, Object> m = cls.getObjMap();
	    	if (m.containsKey("save_path")){
	    		output = m.get("save_path").toString();
	    	}
	    	if (m.containsKey("thread_size")){
	    		thread_size = Integer.parseInt(m.get("thread_size").toString());
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
	    	str = urls.get(0);
	    	if (urls.size()==1){
		        Spider spider = Spider.create(cls)
		        		.addUrl(str)
		        		.addPipeline(clsPipeline);
		        spider.thread(4).run();
	    	}else if(urls.size()>1){
	    		QueueScheduler scheduler = new QueueScheduler();
	    		Spider spider = Spider.create(cls)
		        		.scheduler(scheduler)
		        		.addUrl(str)
		        		.addPipeline(clsPipeline);
	    		
	    		for(int i=0; i<urls.size(); i++){					
		            Request request = new Request();
		            
		            request.setUrl(urls.get(i));
		            scheduler.push(request, spider);
		        }
	    		spider.thread(thread_size).run();
	    	}
    	}
    }
    
    
}
