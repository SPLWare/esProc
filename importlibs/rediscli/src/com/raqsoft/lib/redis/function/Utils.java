package com.raqsoft.lib.redis.function;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;

public class Utils {
	public static enum JEDIS_CONNECT_TYPE{
		CONNECT_REDIS,
		CONNECT_POOL,
		CONNECT_SHARED,
		CONNECT_SHARED_POOL,
		CONNECT_SENTINEL,
		CONNECT_CLUSTER,
	};
	
	public static String[] objectArrayToStringArray(Object[] objs){
		return Arrays.asList( objs ).toArray( new String[0] );
	}
	
	public static List<Object> objectArrayToList(Object[] objs){
		return Arrays.asList(objs);
	}
	
	public static Object checkValidDataType(IParam sub, Context ctx, String type){
		Object retObj = null;
		do{
			if (sub == null ) {
				break;
			}
			//System.out.println("checkValidDataType::IParam= "+sub);
			Object o = sub.getLeafExpression().calculate(ctx);
			if (type.equalsIgnoreCase("int")){
				if (!(o instanceof Integer)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("long")){
				if (!(o instanceof Long)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("bool")){
				if (!(o instanceof Boolean)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			
			}else if (type.equalsIgnoreCase("double")){
				if (!(o instanceof Double)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("byte")){
				if (!(o instanceof byte[])) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("Sequence")){
				if (!(o instanceof Sequence)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else{
				if (o==null){
					break;
				}else if (!(o instanceof String)) {
					System.out.println("instanceof = " + o + " is not String");
					break;
				}
			}
			
			retObj = o;
		}while(false);
		
		return retObj;
	}
	
	// 检测数据类型无提示
	public static Object checkValidDataTypeWithoutPrompt(IParam sub, Context ctx, String type){
		Object retObj = null;
		do{
			if (sub == null ) {
				break;
			}
			//System.out.println("checkValidDataType::IParam= "+sub);
			Object o = sub.getLeafExpression().calculate(ctx);
			if (type.equalsIgnoreCase("int")){
				if (!(o instanceof Integer)) {
					break;
				}
			}else if (type.equalsIgnoreCase("long")){
				if (!(o instanceof Long)) {
					break;
				}
			}else if (type.equalsIgnoreCase("bool")){
				if (!(o instanceof Boolean)) {
					break;
				}
			}else if (type.equalsIgnoreCase("decimal")){
				if (!(o instanceof BigDecimal)) {
					break;
				}				
			}else if (type.equalsIgnoreCase("double")){
				if (!(o instanceof Double)) {
					break;
				}
			}else if (type.equalsIgnoreCase("byte")){
				if (!(o instanceof byte[])) {
					break;
				}
			}else{
				if (o==null){
					break;
				}else if (!(o instanceof String)) {
					break;
				}
			}
			
			retObj = o;
		}while(false);
		
		return retObj;
	}
	
	// 允许为空
	public static Boolean checkValidDataType(Object o, String type, Object ret){
		Boolean bRet = false;
		do{
			if (o == null ) {
				return true;
			}
			//System.out.println("checkValidDataType::IParam= "+sub);
			if (type.equalsIgnoreCase("int")){
				if (!(o instanceof Integer)) {
					break;
				}
			}else if (type.equalsIgnoreCase("long")){
				if (!(o instanceof Long)) {
					break;
				}
			}else if (type.equalsIgnoreCase("bool")){
				if (!(o instanceof Boolean)) {
					break;
				}
			}else if (type.equalsIgnoreCase("decimal")){
				if (!(o instanceof BigDecimal)) {
					break;
				}				
			}else if (type.equalsIgnoreCase("double")){
				if (!(o instanceof Double)) {
					break;
				}
			}else if (type.equalsIgnoreCase("byte")){
				if (!(o instanceof byte[])) {
					break;
				}
			}else{
				if (!(o instanceof String)) {
					break;
				}
			}
			ret = o;
			bRet = true;
		}while(false);
		
		return bRet;
	}
	
	// Object obj = param.getLeafExpression().calculate(ctx);
	// 不支持float, 只支持double;
	public static long objectToLong(Object Obj)  
	{  
	    //return Long.valueOf(String.valueOf(Obj)); 
		double d = Double.valueOf(String.valueOf(Obj));
		return (long)d;
	} 
	
	public static float objectToFloat(Object Obj)  
	{  
	    return Float.valueOf(String.valueOf(Obj));  
	} 
	
	public static double objectToDouble(Object Obj)  
	{  
	    return Double.valueOf(String.valueOf(Obj));  
	} 
	
	public static int objectToInt(Object obj, int defaultVal)  
	{  
		int iRet = -1;
		if (obj instanceof Integer){
			iRet = Integer.parseInt(String.valueOf(obj));
		}else{
			if (defaultVal != -1) {
		    	iRet = defaultVal;
		    }
		}
		
	    return iRet;
	} 
	
	public static boolean isRegExpMatch(String strSrc, String regExp)
	{
		Matcher retMatch;
		if (strSrc==null || strSrc.isEmpty()){
			throw new RQException("hive isMatch strUrl is empty");
		}
		
		if (regExp==null || regExp.isEmpty()){
			throw new RQException("hive isMatch regExp is empty");
		}
		
		Pattern p=Pattern.compile(regExp);
		retMatch = p.matcher(strSrc);
		
		return retMatch.find(); 
	}
	
	public static TimeUnit intToEnum(int value) {    //将数值转换成枚举值
        switch (value) {
        case 0:
            return TimeUnit.NANOSECONDS;
        case 1:
            return TimeUnit.MICROSECONDS;
        case 2:
            return TimeUnit.MILLISECONDS;
        case 3:
            return TimeUnit.SECONDS;
        case 4:
            return TimeUnit.MINUTES;
        case 5:
            return TimeUnit.HOURS;
        case 6:
            return TimeUnit.DAYS;
        default :
            return TimeUnit.MINUTES;
        }
    }
}
