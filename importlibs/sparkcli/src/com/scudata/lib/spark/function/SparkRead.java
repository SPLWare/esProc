package com.scudata.lib.spark.function;

import java.util.HashMap;
import java.util.Map;

import org.apache.spark.SparkConf;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.resources.EngineMessage;

public class SparkRead extends ImFunction {
	public Object doQuery(Object[] objs) {
		if (objs == null || objs.length < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_read1" + mm.getMessage(Integer.toString(param.getSubSize())));
		}

		if (!(objs[0] instanceof SparkCli)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_read2" + mm.getMessage("function.paramTypeError"));
		}
		
		SparkCli client = (SparkCli)objs[0];
		if (!(objs[1] instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_read3" + mm.getMessage("function.paramTypeError"));
		}
	    
	    Map<String,String> map = new HashMap<>(); //backup conf setting
	    if (option!=null && option.contains("t")){
	    	map.put("header","true");
	    }
	    for(int i=2; i<objs.length; i++){
	    	if (objs[i] instanceof Object[]){
	    		Object[] os = (Object[])objs[i];
	    		map.put(os[0].toString(), os[1].toString());
	    	} 
	    }
	    
	    Object ret = null;
	    if (option!=null && option.contains("c")){ //for cursor
	    	ret =  client.cursorRead((String)objs[1], map, m_ctx);
	    }else if(option!=null && option.contains("s")){	  
		    ret =  client.readSequenceFile((String)objs[1], map);
	    }else{
			ret =  client.read((String)objs[1], map);	    	
			if (option!=null && option.contains("x")){
				client.close();
			}	
		}
	    
	    return ret;
	}
}
