package com.scudata.expression.mfn.string;

import java.util.Properties;

import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.expression.IParam;
import com.scudata.expression.StringFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 读出指定属性或者把所有属性读成以{"name", "value"}为结构的序表
 * xs.property(n,v) xs.property()
 * @author RunQian
 *
 */
public class Property extends StringFunction {
	public Object calculate(Context ctx) {
		if (srcStr.length() == 0) {
			return null;
		}
		
		StrProperties sts = new StrProperties();
		if(option != null){
			if( option.indexOf('c') != -1){
				sts.setSeperator(",");
			}
			if( option.indexOf('j') != -1){
				sts.setConnector(":");
			}
		}
		
		sts.load(srcStr);
		if (param == null) {
			return FileObject.getProperties(sts, option);
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			return getValue(sts,o,option);
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("property" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("property" + mm.getMessage("function.invalidParam")+" n can not be null.");
			}
			
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			String key = o1.toString();
			if (sub2 == null) {
				sts.remove(key);
				return sts.getString();
			}
			
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			if (o2 == null) {
				sts.remove(key);
				return sts.getString();
			} else {
				sts.put(key, o2);
				return sts.getString();
			}
		}
	}
	
	private static Object getValue(Properties properties,Object o,String option) {
		boolean isValue = option != null && option.indexOf('v') != -1;
		boolean isQuote = option != null && option.indexOf('q') != -1;
		String key = o.toString();
		String str = properties.getProperty(key);
		
		if (isValue) {
			return Variant.parse(str);
		} else if(isQuote){
			return Escape.addEscAndQuote(str);
		} else {
			return str;
		}
	}
}
