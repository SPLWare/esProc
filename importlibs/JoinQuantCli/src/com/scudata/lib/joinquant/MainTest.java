package com.scudata.lib.joinquant;

import java.io.InputStream;
import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.parallel.UnitContext;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;

public class MainTest {
	
	 public static void main(String[] args){
		 InputStream inputStream;
		try {
			inputStream = UnitContext.getUnitInputStream("raqsoftConfig.xml");
			ConfigUtil.load(inputStream, true);
			inputStream.close();
		
			Context ctx = new Context();		 
			String s = "call(\"D:/works/shell/joinquant/download.dfx\")";
			s = "call(\"D:/works/shell/joinquant/fq.dfx\")";
			s = "call(\"D:/works/shell/joinquant/ticks.dfx\")";
			Expression exp = new Expression(s);
			exp.calculate(ctx);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	 }
	 

	

}
