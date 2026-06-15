package com.scudata.lib.redis;

import java.io.InputStream;

import com.scudata.app.config.ConfigUtil;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.parallel.UnitContext;


public class ImMain {
	
	 public static void main(String[] args){
		try {
			System.setProperty("start.home","C:/Program Files/raqsoft/esProc");
			InputStream inputStream = UnitContext.getUnitInputStream("raqsoftConfig.xml");
			ConfigUtil.load(inputStream, true);//ConfigUtil.load(inputStream, true);
			inputStream.close();
			
			Context ctx = new com.scudata.dm.Context();		 
			String s = "call(\"D:/works/shell/redis/rediscli.dfx\")";
			s = "call(\"D:/works/shell/redis/redis2.dfx\")";
			s = "call(\"D:/works/shell/redis/sentinel.splx\")";
			s = "call(\"D:/works/shell/redis/cluster.splx\")";
			
			//s = "call(\"D:/works/shell/redis/start.splx\")";
			
			
			Expression exp = new com.scudata.expression.Expression(s);
			exp.calculate(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	 }

}
