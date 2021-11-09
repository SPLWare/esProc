package com.raqsoft.lib.redis.function;

import java.io.File;
import java.net.URL;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.lib.redis.function.Utils.JEDIS_CONNECT_TYPE;

//解析xml文件
public class JedisOpen extends Function {
	
	protected JEDIS_CONNECT_TYPE connectType = JEDIS_CONNECT_TYPE.CONNECT_POOL;
	protected String []m_paramTypes; //除去redis句柄参数类型
	protected String []m_confParam = null;
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		Object objs[] = new Object[1];	
		if (param != null) {
			if (param.isLeaf()){			
				objs[0] = param.getLeafExpression().calculate(ctx);
				if (!(objs[0] instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("redis_open2" + mm.getMessage("function.paramTypeError"));
				}			
			}
		}
		
		return doConnect(ctx, objs);
	}
	/***************
	 *url = new URL("file:D:\\Program Files\\raqsoft\\esProc\\extlib\\rediscli\\raq-redis-cli-3.6.jar!/com/raqsoft/lib/redis/function/");
	 *xml文件位置在外部库下。
	 **/
	protected Object doConnect(Context ctx, Object[] objs){	
		if (objs.length<1){
			throw new RQException("redis param " + objs.length + "is too litter");
		}
		
		ApplicationContext appCtx=null;
		try {
			String appHome = "D:\\Program Files\\raqsoft\\esProc\\extlib\\rediscli";
			URL url = JedisOpen.this.getClass().getResource("");
			if (url!=null){				
				File f = new File(url.getPath());
				if (f.getPath().startsWith("file:")){
					String path = f.getPath();
					path = path.substring(path.indexOf(":")+1, path.indexOf("!"));
					int end = path.lastIndexOf(File.separator);
					appHome = path.substring(0, end);
				}
			}
			// classLoader of currentThreadd退出时还原
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader loader = JedisOpen.class.getClassLoader();
			Thread.currentThread().setContextClassLoader(loader);
			//System.out.println("init Loader = "+classLoader+"; currentThread="+loader);
			ctx.setParamValue("classLoader", classLoader);
			String sfile = "";
			StringRedisTemplate stringRedisTemplate = null;
			if (objs[0] == null || objs[0].toString().isEmpty()){
				if (option!=null && option.contains("c")){
					 sfile = "file:"+appHome+File.separator+"spring-cluster.xml";
				}else if (option!=null && option.contains("s")){
					 sfile = "file:"+appHome+File.separator+"spring-sentinel.xml";
				}else{
					sfile = "file:"+appHome+File.separator+"spring-redis.xml";
				}
			}else{
				sfile = "file:"+appHome+File.separator+objs[0].toString();
			}
			
			appCtx = new ClassPathXmlApplicationContext(sfile);
	        //从容器中获得JedisClient对象
			stringRedisTemplate =(StringRedisTemplate)appCtx.getBean("stringRedisTemplate");
			
			return new RedisTool(appCtx, stringRedisTemplate);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
	
}
